package com.psddev.cms.tool.page.content;

import com.google.common.base.Preconditions;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.Query;
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
public class EditFieldUpdate extends Record {

    @Indexed
    private UUID userId;

    @Indexed
    private UUID contentId;

    private Map<String, Set<String>> fieldNamesByObjectId;

    private static UUID createId(UUID userId, UUID contentId) {
        return UuidUtils.createVersion3Uuid(EditFieldUpdate.class.getName() + "/" + userId + "/" + contentId);
    }

    /**
     * Saves the given {@code fieldNamesByObjectId} update information and
     * associates it to the given {@code userId} and {@code contentId}.
     *
     * @param userId
     *        Can't be {@code null}.
     *
     * @param contentId
     *        Can't be {@code null}.
     *
     * @param fieldNamesByObjectId
     *        If {@code null}, equivalent to an empty map.
     */
    public static void save(UUID userId, UUID contentId, Map<String, Set<String>> fieldNamesByObjectId) {
        Preconditions.checkNotNull(userId);
        Preconditions.checkNotNull(contentId);

        EditFieldUpdate update = new EditFieldUpdate();

        update.getState().setId(createId(userId, contentId));
        update.setUserId(userId);
        update.setContentId(contentId);
        update.setFieldNamesByObjectId(fieldNamesByObjectId);
        update.saveImmediately();
    }

    /**
     * Deletes the update associated with the given {@code userId} and
     * {@code contentId}.
     *
     * @param userId
     *        Can't be {@code null}.
     *
     * @param contentId
     *        Can't be {@code null}.
     */
    public static void delete(UUID userId, UUID contentId) {
        Preconditions.checkNotNull(userId);
        Preconditions.checkNotNull(contentId);

        for (EditFieldUpdate update : Query.from(EditFieldUpdate.class)
                .where("_id = ?", createId(userId, contentId))
                .selectAll()) {

            update.setFieldNamesByObjectId(null);
            update.saveImmediately();

            Database db = Database.Static.getDefault();

            db.beginIsolatedWrites();

            try {
                update.delete();
                db.commitWrites();

            } finally {
                db.endWrites();
            }
        }
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
}
