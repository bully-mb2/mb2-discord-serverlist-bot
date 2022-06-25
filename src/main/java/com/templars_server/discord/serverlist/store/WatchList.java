package com.templars_server.discord.serverlist.store;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class WatchList {

    private static final Logger LOG = LoggerFactory.getLogger(WatchList.class);
    private static final String FILE_NAME = "watchlist.json";
    private static final Type TYPE = new TypeToken<Map<String, Map<String, WatchedChannel>>>(){}.getType();

    private final Map<String, Map<String, WatchedChannel>> watchMap;
    private final Gson gson;

    public WatchList() {
        watchMap = new HashMap<>();
        gson = new Gson();
    }

    public WatchedChannel put(String guildId, String channelId, WatchedChannel watchedChannel) {
        Map<String, WatchedChannel> channelMap = watchMap.computeIfAbsent(guildId, map -> new HashMap<>());
        return channelMap.put(channelId, watchedChannel);
    }

    public Map<String, WatchedChannel> get(String guildId) {
        return watchMap.computeIfAbsent(guildId, map -> new HashMap<>());
    }

    public int size() {
        return watchMap.values().stream()
                .map(Map::size)
                .reduce(0, Integer::sum);
    }

    public void loadFromFile() {
        LOG.info("Loading from file " + FILE_NAME);
        try (Reader reader = new FileReader(FILE_NAME)) {
            Map<String, Map<String, WatchedChannel>> watchList = gson.fromJson(reader, TYPE);
            if (watchList != null) {
                this.watchMap.putAll(watchList);
            }
        } catch (IOException e) {
            LOG.error("Couldn't load watch list", e);
        }
    }

    public void saveToFile() {
        LOG.info("Saving to file " + FILE_NAME);
        try {
            try (Writer writer = new FileWriter(FILE_NAME)) {
                gson.toJson(watchMap, writer);
            }
        } catch (IOException e) {
            LOG.error("Couldn't save watch list", e);
        }
    }

}
