package com.templars_server.discord.serverlist.command;

import com.templars_server.discord.serverlist.Bot;
import com.templars_server.discord.serverlist.store.WatchedChannel;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

public class LocalCommandUnwatch extends LocalCommand {

    public LocalCommandUnwatch() {
        super("unwatch", "Unwatch the MBII server list");
    }

    @Override
    public String execute(Bot bot, SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();
        if (guild == null) {
            return "This command has to be executed in a guild channel";
        }

        String channelId = event.getChannel().getId();
        WatchedChannel channel = bot.getWatchList().remove(guild.getId(), channelId);
        if (channel == null) {
            return "Not watching anything here!";
        }

        try {
            event.getTextChannel().deleteMessageById(channel.getMessageId()).complete();
        } catch (ErrorResponseException ignored) {
            // Message couldn't be deleted
        }

        bot.getWatchList().saveToFile();
        return "Removed!";
    }

}
