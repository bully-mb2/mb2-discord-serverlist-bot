package com.templars_server.discord.serverlist.store;

public enum Region {

    ALL,
    EU,
    NA,
    SA,
    OC;

    public static Region fromValue(String string) {
        try {
            return Region.valueOf(string.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return Region.ALL;
        }
    }

    public boolean checkRegionCode(String regionCode) {
        Region region = Region.fromValue(regionCode);
        if (region == null) {
            return false;
        }

        return region == this;
    }

}
