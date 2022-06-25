package com.templars_server.discord.serverlist;

import com.templars_server.util.settings.Settings;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.*;


public class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) throws IOException, LoginException {
        LOG.info("======== Starting mb2-discord-serverlist-bot ========");
        LOG.info("Loading settings");
        Settings settings = new Settings();
        settings.load("application.properties");

        LOG.info("Reading properties");
        String token = settings.get("discord.token");
        int checkInterval = settings.getInt("serverlist.queryintervalminutes");

        LOG.info("Logging in discord");
        JDABuilder builder = JDABuilder.createDefault(token);
        builder.disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE);
        builder.setBulkDeleteSplittingEnabled(false);
        builder.addEventListeners(new Bot(checkInterval));
        builder.setActivity(Activity.watching("MBII Server List"));
        JDA jda = builder.build();
        jda.retrieveCommands().complete().forEach(command -> {
            jda.deleteCommandById(command.getId()).queue();
        });
    }

}
