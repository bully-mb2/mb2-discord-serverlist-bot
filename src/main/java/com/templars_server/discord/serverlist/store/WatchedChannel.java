package com.templars_server.discord.serverlist.store;

public class WatchedChannel {

    private final Region region;
    private final MBMode mbMode;
    private final int minPlayers;
    private String messageId;

    public WatchedChannel(Region region, MBMode mbMode, int minPlayers) {
        this.region = region;
        this.mbMode = mbMode;
        this.minPlayers = minPlayers;
        this.messageId = null;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public Region getRegion() {
        return region;
    }

    public MBMode getMbMode() {
        return mbMode;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

}
