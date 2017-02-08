package com.psddev.cms.rtc;

import com.psddev.dari.db.UpdateNotifier;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

class RtcSessionUpdateNotifier implements UpdateNotifier<RtcSession> {

    private final ConcurrentMap<UUID, RtcAsyncContext> contexts;

    public RtcSessionUpdateNotifier(ConcurrentMap<UUID, RtcAsyncContext> contexts) {
        this.contexts = contexts;
    }

    @Override
    public void onUpdate(RtcSession session) {
        if (session.isClosed()) {
            RtcAsyncContext context = contexts.remove(session.getId());

            if (context != null) {
                context.disconnect();
            }
        }
    }
}
