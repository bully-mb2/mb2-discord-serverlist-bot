package com.templars_server.discord.serverlist.store;

public enum MBMode {

    ALL,
    OPEN,
    LEGENDS,
    DUEL,
    AUTHENTIC;

    public static MBMode fromValue(String string) {
        try {
            return MBMode.valueOf(string.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return MBMode.ALL;
        }
    }

    public boolean checkMBMode(String mbMode) {
        MBMode mode = MBMode.fromValue(mbMode);
        if (mode == null) {
            return false;
        }

        return mode == this;
    }

}