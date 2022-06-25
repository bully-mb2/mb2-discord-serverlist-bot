package com.templars_server.discord.serverlist.api;

public class ServerData {

    private String id;
    private String address;
    private int port;
    private String sv_hostname;
    private String sv_hostname_nocolor;
    private String mapname;
    private int numplayers;
    private int numbots;
    private int sv_maxclients;
    private int g_gametype;
    private int g_needpass;
    private String country;
    private String country_code;
    private String region;
    private String region_code;
    private String discord;
    private String first_seen;
    private String mbversion;
    private String mbmode;
    private String rtvrtm_state;

    public String getHostnameNoColor() {
        return sv_hostname_nocolor;
    }

    public String getMapName() {
        return mapname;
    }

    public int getNumPlayers() {
        return numplayers;
    }

    public int getMaxClients() {
        return sv_maxclients;
    }

    public String getRegionCode() {
        return region_code;
    }

    public String getMBMode() {
        return mbmode;
    }

}
