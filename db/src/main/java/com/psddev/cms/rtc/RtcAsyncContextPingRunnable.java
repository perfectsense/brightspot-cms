package com.psddev.cms.rtc;

import com.google.common.collect.ImmutableMap;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

class RtcAsyncContextPingRunnable implements Runnable {

    private final ConcurrentMap<UUID, RtcAsyncContext> contexts;
    private volatile boolean stopped;

    public RtcAsyncContextPingRunnable(ConcurrentMap<UUID, RtcAsyncContext> contexts) {
        this.contexts = contexts;
    }

    public void stop() {
        stopped = true;
    }

    @Override
    public void run() {
        for (RtcAsyncContext context : contexts.values()) {
            if (stopped) {
                return;
            }

            context.writeEvent(ImmutableMap.of("_ping", true));
        }
    }
}
