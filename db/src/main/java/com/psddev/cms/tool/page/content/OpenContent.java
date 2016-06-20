package com.psddev.cms.tool.page.content;

import com.google.common.base.Preconditions;
import com.psddev.cms.rtc.RtcEvent;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.Record;
import com.psddev.dari.util.UuidUtils;

import java.util.UUID;

public class OpenContent extends Record implements RtcEvent {

    @Indexed
    private UUID userId;

    @Indexed
    private UUID contentId;

    private boolean closed = false;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getContentId() {
        return contentId;
    }

    public void setContentId(UUID contentId) {
        this.contentId = contentId;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public static void save(UUID userId, UUID sessionId, UUID contentId) {
        Preconditions.checkNotNull(userId);
        Preconditions.checkNotNull(sessionId);
        Preconditions.checkNotNull(contentId);

        OpenContent open = new OpenContent();

        open.getState().setId(UuidUtils.createVersion3Uuid(OpenContent.class.getName() + "/" + sessionId + "/" + contentId));
        open.setUserId(userId);
        open.setContentId(contentId);
        open.as(RtcEvent.Data.class).setSessionId(sessionId);
        open.saveImmediately();
    }

    @Override
    public void onDisconnect() {

        setClosed(true);
        saveImmediately();

        Database db = Database.Static.getDefault();

        db.beginIsolatedWrites();

        try {
            delete();
            db.commitWrites();

        } finally {
            db.endWrites();
        }
    }
}
