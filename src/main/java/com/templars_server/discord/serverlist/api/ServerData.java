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

    public String getId() {
        return id;
    }

    public String getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public String getSv_hostname() {
        return sv_hostname;
    }

    public String getSv_hostname_nocolor() {
        return sv_hostname_nocolor;
    }

    public String getMapname() {
        return mapname;
    }

    public int getNumplayers() {
        return numplayers;
    }

    public int getNumbots() {
        return numbots;
    }

    public int getSv_maxclients() {
        return sv_maxclients;
    }

    public int getG_gametype() {
        return g_gametype;
    }

    public int getG_needpass() {
        return g_needpass;
    }

    public String getCountry() {
        return country;
    }

    public String getCountry_code() {
        return country_code;
    }

    public String getRegion() {
        return region;
    }

    public String getRegion_code() {
        return region_code;
    }

    public String getDiscord() {
        return discord;
    }

    public String getFirst_seen() {
        return first_seen;
    }

    public String getMbversion() {
        return mbversion;
    }

    public String getMbmode() {
        return mbmode;
    }

    public String getRtvrtm_state() {
        return rtvrtm_state;
    }

}
