package com.psddev.cms.rtc;

import com.psddev.dari.db.Record;

import java.util.UUID;

/**
 * @since 3.1
 */
public class RtcSession extends Record {

    private UUID userId;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }
}
