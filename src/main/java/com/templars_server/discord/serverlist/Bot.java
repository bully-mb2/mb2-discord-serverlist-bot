package com.templars_server.discord.serverlist;

import com.templars_server.discord.serverlist.api.ServerData;
import com.templars_server.discord.serverlist.api.ServerListAPI;
import com.templars_server.discord.serverlist.command.LocalCommand;
import com.templars_server.discord.serverlist.command.LocalCommandWatch;
import com.templars_server.discord.serverlist.store.MBMode;
import com.templars_server.discord.serverlist.store.Region;
import com.templars_server.discord.serverlist.store.WatchList;
import com.templars_server.discord.serverlist.store.WatchedChannel;
import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciitable.CWC_LongestLine;
import de.vandermeer.asciithemes.TA_GridThemes;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class Bot extends ListenerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(Bot.class);
    private static final String API_URL = "https://servers.moviebattles.org/api/get/list";
    private static final int MAX_EMBED_TABLE_SIZE = 12;

    private final int checkInterval;
    private final Map<String, LocalCommand> commandList;
    private final Map<String, Guild> guilds;
    private final Thread queryThread;
    private final WatchList watchList;
    private final ServerListAPI api;
    private List<ServerData> lastServerData;

    public Bot(int checkIntervalMinutes) {
        checkInterval = checkIntervalMinutes * 60 * 1000;
        commandList = new HashMap<>();
        guilds = new HashMap<>();
        queryThread = new Thread(this::queryRun, "QueryThread");
        watchList = new WatchList();
        api = new ServerListAPI();
        registerCommand(new LocalCommandWatch());
    }

    public void onReady(@Nonnull ReadyEvent event) {
        LOG.info("Loading watch list");
        watchList.loadFromFile();
        LOG.info("Loaded, watching " + watchList.size() + " channels");
        LOG.info("Starting query thread");
        queryThread.start();
    }

    public void onGuildReady(@Nonnull GuildReadyEvent event) {
        Guild guild = event.getGuild();
        LOG.info("Registering commands for guild " + guild.getName());
        commandList.forEach((name, localCommand) -> guild.upsertCommand(name, localCommand.getDescription())
                .addOptions(localCommand.getOptionData())
                .queue()
        );
        guilds.put(guild.getId(), guild);
        event.getJDA().getPresence().setActivity(Activity.watching(guilds.size() + " " + (guilds.size() > 1 ? "sectors" : "sector")));
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        LocalCommand localCommand = commandList.get(event.getName());
        if (localCommand == null) {
            event.reply("Command not found? Somehow? Contact my creator please this shouldn't happen!").queue();
            return;
        }

        Member member = event.getMember();
        if (member == null) {
            event.reply("Only Guild members may command me!").queue();
            return;
        }

        if (!member.hasPermission(Permission.ADMINISTRATOR)) {
            event.reply("Only members with the administrator permission may command me!").queue();
            return;
        }

        LOG.info("Executing " + localCommand.getName());
        String reply = localCommand.execute(this, event);
        LOG.info("Result: " + reply);
        event.reply(reply).queue();
    }

    @SuppressWarnings("BusyWait")
    private void queryRun() {
        while(true) {
            LOG.info("Updating server list");
            try {
                List<ServerData> serverDataList = api.getList();
                sendEmbeds(serverDataList);
                lastServerData = serverDataList;
                Thread.sleep(checkInterval);
            } catch (IOException | InterruptedException e) {
                LOG.info("Error encountered updating server list", e);
                return;
            }
        }
    }

    public WatchList getWatchList() {
        return watchList;
    }

    private void registerCommand(LocalCommand command) {
        commandList.put(command.getName(), command);
    }

    private void sendEmbeds(List<ServerData> serverData) {
        for (Map.Entry<String, Guild> guildEntry : guilds.entrySet()) {
            String guildId = guildEntry.getKey();
            Guild guild = guildEntry.getValue();
            for (Map.Entry<String, WatchedChannel> entry : watchList.get(guildId).entrySet()) {
                String channelId = entry.getKey();
                WatchedChannel watchedChannel = entry.getValue();
                TextChannel channel = guild.getTextChannelById(channelId);
                if (channel == null) {
                    LOG.info("Can't find channel for id " + channelId);
                    return;
                }

                sendChannelEmbed(channel, watchedChannel, serverData);
            }
        }
    }

    public void sendChannelEmbed(TextChannel channel, WatchedChannel watchedChannel) {
        if (lastServerData == null) {
            return;
        }

        sendChannelEmbed(channel, watchedChannel, lastServerData);
    }

    private void sendChannelEmbed(TextChannel channel, WatchedChannel watchedChannel, List<ServerData> serverData) {
        String messageId = watchedChannel.getMessageId();
        MessageEmbed messageEmbed = buildEmbed(watchedChannel, serverData);
        boolean sent = false;
        if (messageId != null) {
            try {
                channel.editMessageEmbedsById(messageId, messageEmbed).complete();
                sent = true;
            } catch (ErrorResponseException ignored) {
                // Message couldn't be edited
            }
        }

        if (!sent) {
            messageId = channel.sendMessageEmbeds(messageEmbed).complete().getId();
            watchedChannel.setMessageId(messageId);
            watchList.saveToFile();
        }
    }

    private MessageEmbed buildEmbed(WatchedChannel watchedChannel, List<ServerData> serverData) {
        int populated = 0;
        int totalPlaying = 0;
        for (ServerData server : serverData) {
            int players = server.getNumplayers();
            if (players > 0) {
                populated++;
            }

            totalPlaying += players;
        }
        List<ServerData> sortedData = serverData.stream()
                .filter(server -> server.getNumplayers() >= watchedChannel.getMinPlayers())
                .filter(server -> watchedChannel.getRegion() == Region.ALL || watchedChannel.getRegion().checkRegionCode(server.getRegion_code()))
                .filter(server -> watchedChannel.getMbMode() == MBMode.ALL || watchedChannel.getMbMode().checkMBMode(server.getMbmode()))
                .sorted(Comparator.comparingInt(ServerData::getNumplayers).reversed())
                .limit(MAX_EMBED_TABLE_SIZE)
                .collect(Collectors.toList());

        AsciiTable table = new AsciiTable();
        table.setPadding(0);
        table.setTextAlignment(TextAlignment.LEFT);
        table.getContext().setGridTheme(TA_GridThemes.HORIZONTAL);
        table.getRenderer().setCWC(new CWC_LongestLine());
        table.addRule();
        table.addRow("🗺", "Server Name", "Map", "Slots", "Mode");
        table.addRule();
        for (ServerData server : sortedData) {
            table.addRow(
                    server.getRegion_code() + " ",
                    server.getSv_hostname_nocolor() + " ",
                    server.getMapname(),
                    server.getNumplayers() + "/" + server.getSv_maxclients(),
                    server.getMbmode().equals("Authentic") ? "FA" : server.getMbmode()
            );
            table.addRule();
        }

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("MBII Server List - (Region: " + watchedChannel.getRegion() + ", Mode: " + watchedChannel.getMbMode() + ", Min Slots: " + watchedChannel.getMinPlayers() + ")");
        builder.setDescription("```" + table.render() + "```");
        builder.setColor(0x35ed47);
        builder.setTimestamp(Instant.now());
        builder.setFooter("There are " + populated + " out of " + serverData.size() + " servers populated with " + totalPlaying + " players total.");
        return builder.build();
    }

}
