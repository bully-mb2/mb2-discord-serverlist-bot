package com.templars_server.discord.serverlist.api;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ServerListAPI {

    private static final Logger LOG = LoggerFactory.getLogger(ServerListAPI.class);
    private static final String API_URL = "https://servers.moviebattles.org";

    private final Gson gson;

    public ServerListAPI() {
        gson = new Gson();
    }

    public List<ServerData> getList() throws IOException {
        URL url = new URL(API_URL + "/api/get/list");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
            List<ServerData> serverData = gson.fromJson(reader, new TypeToken<List<ServerData>>(){}.getType());
            LOG.info(serverData.size() + " servers found, updating embeds");
            return serverData;
        }
    }

}
