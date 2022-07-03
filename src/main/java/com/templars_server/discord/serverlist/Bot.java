package com.templars_server.discord.serverlist;

import com.templars_server.discord.serverlist.api.ServerData;
import com.templars_server.discord.serverlist.api.ServerListAPI;
import com.templars_server.discord.serverlist.command.LocalCommand;
import com.templars_server.discord.serverlist.command.LocalCommandUnwatch;
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
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class Bot extends ListenerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(Bot.class);
    private static final int MAX_EMBED_TABLE_SIZE = 12;
    private static final int EMBED_COLOR = 0x35ed47;
    private static final int MAX_RETRIES_BEFORE_PANIC = 5;
    private static final int MAX_RETRY_INTERVAL = 30 * 60 * 1000;

    private final int checkInterval;
    private final Map<String, LocalCommand> commandList;
    private final Set<String> guildIds;
    private final Thread queryThread;
    private final WatchList watchList;
    private final ServerListAPI api;
    private JDA jda;
    private List<ServerData> lastServerData;
    private int lastPopulated;
    private int lastTotalPlaying;

    public Bot(int checkIntervalMinutes) {
        checkInterval = checkIntervalMinutes * 60 * 1000;
        commandList = new HashMap<>();
        guildIds = new HashSet<>();
        queryThread = new Thread(this::queryRun, "QueryThread");
        watchList = new WatchList();
        api = new ServerListAPI();
        lastServerData = new ArrayList<>();
        lastPopulated = 0;
        lastTotalPlaying = 0;
        registerCommand(new LocalCommandWatch());
        registerCommand(new LocalCommandUnwatch());
    }

    public void onReady(@Nonnull ReadyEvent event) {
        jda = event.getJDA();
        LOG.info("Loading watch list");
        watchList.loadFromFile();
        cleanup(guildIds);
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
        guildIds.add(guild.getId());
        event.getJDA().getPresence().setActivity(Activity.watching(guildIds.size() + " " + (guildIds.size() > 1 ? "sectors" : "sector")));
    }

    @Override
    public void onGuildLeave(@NotNull GuildLeaveEvent event) {
        Guild guild = event.getGuild();
        LOG.info("Leaving guild " + guild.getName());
        guildIds.remove(guild.getId());
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
        int retry = 0;
        while(true) {
            LOG.info("Updating server list");
            try {
                List<ServerData> serverDataList = api.getList();
                LOG.info(serverDataList.size() + " servers found, updating embeds");
                lastPopulated = 0;
                lastTotalPlaying = 0;
                for (ServerData server : serverDataList) {
                    int players = server.getNumPlayers();
                    if (players > 0) {
                        lastPopulated++;
                    }

                    lastTotalPlaying += players;
                }

                sendEmbeds(fetchGuilds(guildIds), serverDataList, lastPopulated, lastTotalPlaying);
                lastServerData = serverDataList;
                if (retry > MAX_RETRIES_BEFORE_PANIC) {
                    Thread.sleep(MAX_RETRY_INTERVAL);
                } else {
                    Thread.sleep(checkInterval);
                }

                retry = 0;
            } catch (InterruptedException e) {
                LOG.error("Server list thread interrupted", e);
                return;
            } catch (Exception e) {
                retry++;
                LOG.error("Error encountered updating server list, retry " + retry, e);
            }
        }
    }

    public WatchList getWatchList() {
        return watchList;
    }

    private void registerCommand(LocalCommand command) {
        commandList.put(command.getName(), command);
    }

    private void cleanup(Set<String> usedGuildIds) {
        LOG.info("Cleaning up watch list");
        int before = watchList.getGuildIds().size();
        watchList.getGuildIds().retainAll(usedGuildIds);
        watchList.saveToFile();
        LOG.info("Cleaned up " + (before - watchList.getGuildIds().size()) + " guilds");
    }

    private List<Guild> fetchGuilds(Set<String> guildIds) {
        List<Guild> guilds = new ArrayList<>();
        for (String id : guildIds) {
            guilds.add(jda.getGuildById(id));
        }

        return guilds;
    }

    private void sendEmbeds(List<Guild> guilds, List<ServerData> serverData, int populated, int totalPlaying) {
        for (Guild guild : guilds) {
            String guildId = guild.getId();
            for (Map.Entry<String, WatchedChannel> entry : watchList.get(guildId).entrySet()) {
                String channelId = entry.getKey();
                WatchedChannel watchedChannel = entry.getValue();
                TextChannel channel = guild.getTextChannelById(channelId);
                if (channel == null) {
                    LOG.info("Can't find channel for id " + channelId);
                    return;
                }

                sendChannelEmbed(channel, watchedChannel, serverData, populated, totalPlaying);
            }
        }
    }

    public void sendChannelEmbed(TextChannel channel, WatchedChannel watchedChannel) {
        if (lastServerData == null) {
            return;
        }

        sendChannelEmbed(channel, watchedChannel, lastServerData, lastPopulated, lastTotalPlaying);
    }

    private void sendChannelEmbed(TextChannel channel, WatchedChannel watchedChannel, List<ServerData> serverData, int populated, int totalPlaying) {
        String messageId = watchedChannel.getMessageId();
        MessageEmbed messageEmbed = buildEmbed(watchedChannel, serverData, populated, totalPlaying);
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

    private MessageEmbed buildEmbed(WatchedChannel watchedChannel, List<ServerData> serverData, int populated, int totalPlaying) {
        List<ServerData> sortedData = serverData.stream()
                .filter(server -> server.getNumPlayers() >= watchedChannel.getMinPlayers())
                .filter(server -> watchedChannel.getRegion() == Region.ALL || watchedChannel.getRegion().checkRegionCode(server.getRegionCode()))
                .filter(server -> watchedChannel.getMbMode() == MBMode.ALL || watchedChannel.getMbMode().checkMBMode(server.getMBMode()))
                .sorted(Comparator.comparingInt(ServerData::getNumPlayers).reversed())
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
                    server.getRegionCode() + " ",
                    server.getHostnameNoColor() + " ",
                    server.getMapName(),
                    server.getNumPlayers() + "/" + server.getMaxClients(),
                    server.getMBMode().equals("Authentic") ? "FA" : server.getMBMode()
            );
            table.addRule();
        }

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("MBII Server List - (Region: " + watchedChannel.getRegion() + ", Mode: " + watchedChannel.getMbMode() + ", Min Slots: " + watchedChannel.getMinPlayers() + ")");
        builder.setDescription("```" + table.render() + "```");
        builder.setColor(EMBED_COLOR);
        builder.setTimestamp(Instant.now());
        builder.setFooter("There are " + populated + " out of " + serverData.size() + " servers populated with " + totalPlaying + " players total.");
        return builder.build();
    }

}
