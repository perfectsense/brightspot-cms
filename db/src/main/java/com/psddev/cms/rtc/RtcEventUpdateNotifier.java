package com.psddev.cms.rtc;

import com.google.common.collect.ImmutableMap;
import com.psddev.dari.db.UpdateNotifier;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

class RtcEventUpdateNotifier implements UpdateNotifier<RtcEvent> {

    private final ConcurrentMap<UUID, RtcAsyncContext> contexts;

    public RtcEventUpdateNotifier(ConcurrentMap<UUID, RtcAsyncContext> contexts) {
        this.contexts = contexts;
    }

    @Override
    public void onUpdate(RtcEvent event) {
        RtcBroadcast.forEachBroadcast(event, (broadcast, data) -> {
            String broadcastClassName = broadcast.getClass().getName();

            contexts.values().forEach(context -> {
                if (broadcast.shouldBroadcast(data, context.getUserId())) {
                    context.writeEvent(ImmutableMap.of(
                            "broadcast", broadcastClassName,
                            "data", data));
                }
            });
        });
    }
}
