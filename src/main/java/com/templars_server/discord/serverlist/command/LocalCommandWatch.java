package com.templars_server.discord.serverlist.command;

import com.templars_server.discord.serverlist.Bot;
import com.templars_server.discord.serverlist.store.MBMode;
import com.templars_server.discord.serverlist.store.Region;
import com.templars_server.discord.serverlist.store.WatchedChannel;
import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciitable.CWC_LongestLine;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class LocalCommandWatch extends LocalCommand {

    public LocalCommandWatch() {
        super("watch", "Watch the MBII server list, with options");
        OptionData optionRegion = new OptionData(OptionType.STRING, "region", "Specify what region you want to watch", false, false);
        optionRegion.addChoice("Europe", "EU");
        optionRegion.addChoice("North America", "NA");
        optionRegion.addChoice("South America", "SA");
        optionRegion.addChoice("Oceania", "OC");
        addOption(optionRegion);

        OptionData optionMode = new OptionData(OptionType.STRING, "mode", "Specify what mode you want to watch", false, false);
        optionMode.addChoice("Open", "OPEN");
        optionMode.addChoice("Duel", "DUEL");
        optionMode.addChoice("Legends", "LEGENDS");
        optionMode.addChoice("Full Authentic", "AUTHENTIC");
        addOption(optionMode);

        OptionData optionMinimumPlayerCount = new OptionData(OptionType.INTEGER, "minimum-player-count", "Specify how many players a server needs to get shown", false, false);
        optionMinimumPlayerCount.setMaxValue(32);
        optionMinimumPlayerCount.setMinValue(0);
        addOption(optionMinimumPlayerCount);
    }

    @Override
    public String execute(Bot bot, SlashCommandInteractionEvent event) {
        // Read options, defaults can be found in WatchedChannel
        WatchedChannel watchedChannel = new WatchedChannel(
                Region.fromValue(readOption(event, "region")),
                MBMode.fromValue(readOption(event, "mode")),
                readOptionAsInt(event, "minimum-player-count")
        );

        Guild guild = event.getGuild();
        if (guild == null) {
            return "This command has to be executed in a guild channel";
        }

        WatchedChannel previous = bot.getWatchList().put(guild.getId(), event.getChannel().getId(), watchedChannel);
        if (previous != null) {
            watchedChannel.setMessageId(previous.getMessageId());
        }

        bot.getWatchList().saveToFile();
        bot.sendChannelEmbed(event.getTextChannel(), watchedChannel);

        AsciiTable table = new AsciiTable();
        table.getRenderer().setCWC(new CWC_LongestLine());
        table.addRule();
        table.addRow("Region", "Mode", "Minimum Player Count");
        table.addRule();
        table.addRow(watchedChannel.getRegion(), watchedChannel.getMbMode(), watchedChannel.getMinPlayers());
        table.addRule();
        return "Now watching this channel with the following options:\n```" + table.render() + "```";

    }

}
