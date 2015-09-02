package com.psddev.cms.rtc;

import com.psddev.dari.db.UpdateNotifier;
import org.atmosphere.cpr.Broadcaster;

class RtcObjectUpdateNotifier implements UpdateNotifier<Object> {

    private final Broadcaster broadcaster;

    public RtcObjectUpdateNotifier(Broadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @Override
    public void onUpdate(Object object) {
        RtcBroadcast.forEachBroadcast(object, (broadcast, data) ->
                broadcaster.broadcast(new RtcBroadcastMessage(broadcast, data)));
    }
}
