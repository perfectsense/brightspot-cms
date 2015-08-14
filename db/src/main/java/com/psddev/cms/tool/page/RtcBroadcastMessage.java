package com.psddev.cms.tool.page;

import java.util.Map;

class RtcBroadcastMessage {

    private final RtcBroadcast<Object> broadcast;
    private final Map<String, Object> data;

    public RtcBroadcastMessage(RtcBroadcast<Object> broadcast, Map<String, Object> data) {
        this.broadcast = broadcast;
        this.data = data;
    }

    public RtcBroadcast<Object> getBroadcast() {
        return broadcast;
    }

    public Map<String, Object> getData() {
        return data;
    }
}
