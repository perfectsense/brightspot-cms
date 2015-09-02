package com.psddev.cms.rtc;

import com.psddev.dari.db.Modification;
import com.psddev.dari.db.Recordable;

import java.util.UUID;

/**
 * @since 3.1
 */
public interface RtcEvent extends Recordable {

    /**
     * Called when a user disconnects from a session.
     */
    void onDisconnect();

    @FieldInternalNamePrefix("cms.rtc.event.")
    class Data extends Modification<RtcEvent> {

        @Indexed
        @Required
        private UUID sessionId;

        public UUID getSessionId() {
            return sessionId;
        }

        public void setSessionId(UUID sessionId) {
            this.sessionId = sessionId;
        }
    }
}
