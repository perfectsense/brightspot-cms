package com.psddev.cms.tool.page.content;

import com.google.common.base.Preconditions;
import com.psddev.cms.rtc.RtcEvent;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.Record;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.UuidUtils;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Live field update data when a user is editing something in the tool.
 *
 * @since 3.1
 */
public class EditFieldUpdate extends Record implements RtcEvent {

    @Indexed
    private UUID userId;

    @Indexed
    private UUID contentId;

    private Map<String, Set<String>> fieldNamesByObjectId;

    /**
     * Saves the given {@code fieldNamesByObjectId} update information and
     * associates it to the given {@code userId}, {@code sessionId},
     * and {@code contentId}.
     *
     * @param userId
     *        Can't be {@code null}.
     *
     * @param sessionId
     *        Can't be {@code null}.
     *
     * @param contentId
     *        Can't be {@code null}.
     *
     * @param fieldNamesByObjectId
     *        If {@code null}, equivalent to an empty map.
     */
    public static void save(UUID userId, UUID sessionId, UUID contentId, Map<String, Set<String>> fieldNamesByObjectId) {
        Preconditions.checkNotNull(userId);
        Preconditions.checkNotNull(sessionId);
        Preconditions.checkNotNull(contentId);

        EditFieldUpdate update = new EditFieldUpdate();

        update.getState().setId(UuidUtils.createVersion3Uuid(EditFieldUpdate.class.getName() + "/" + sessionId + "/" + contentId));
        update.setUserId(userId);
        update.as(RtcEvent.Data.class).setSessionId(sessionId);
        update.setContentId(contentId);
        update.setFieldNamesByObjectId(fieldNamesByObjectId);
        update.saveImmediately();
    }

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

    /**
     * @return Never {@code null}.
     */
    public Map<String, Set<String>> getFieldNamesByObjectId() {
        if (fieldNamesByObjectId == null) {
            fieldNamesByObjectId = new CompactMap<>();
        }
        return fieldNamesByObjectId;
    }

    public void setFieldNamesByObjectId(Map<String, Set<String>> fieldsByObjectId) {
        this.fieldNamesByObjectId = fieldsByObjectId;
    }

    @Override
    public void onDisconnect() {
        setFieldNamesByObjectId(null);
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
