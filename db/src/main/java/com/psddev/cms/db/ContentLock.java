package com.psddev.cms.db;

import java.util.Date;
import java.util.UUID;

import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.UuidUtils;

public class ContentLock extends Record {

    private Date createDate;
    private UUID contentId;
    private Recordable owner;

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public UUID getContentId() {
        return contentId;
    }

    public void setContentId(UUID contentId) {
        this.contentId = contentId;
    }

    public Object getOwner() {
        return owner;
    }

    public void setOwner(Object owner) {
        this.owner = (Recordable) owner;
    }

    /**
     * {@link ContentLock} utility methods.
     */
    public static class Static {

        private static UUID createLockId(Object content, String aspect) {
            return UuidUtils.createVersion3Uuid(
                    "cms.contentLock/"
                            + State.getInstance(content).getId() + "/"
                            + ObjectUtils.firstNonNull(aspect, ""));
        }

        /**
         * Returns the lock associated with the given {@code aspect} of the
         * given {@code content}.
         *
         * @param content Can't be {@code null}.
         * @param aspect If {@code null}, it's equivalent to an empty string.
         * @return May be {@code null} if there is no lock associated, or if
         * the lock's owner is either archived or deleted.
         */
        public static ContentLock findLock(Object content, String aspect) {
            ContentLock lock =  Query
                    .from(ContentLock.class)
                    .where("_id = ?", createLockId(content, aspect))
                    .master()
                    .noCache()
                    .first();

            if (lock != null) {
                Object owner = lock.getOwner();

                // Owner is deleted.
                if (owner == null) {
                    unlock(content, null, null);
                    return null;

                // Owner is archived.
                } else if (State.getInstance(owner).as(Content.ObjectModification.class).isTrash()) {
                    return null;
                }
            }

            return lock;
        }

        /**
         * Tries to lock the given {@code aspect} of the given {@code content}
         * and associate it to the given {@code newOwner}.
         *
         * @param content Can't be {@code null}.
         * @param aspect If {@code null}, it's equivalent to an empty string.
         * @param newOwner Can't be {@code null}.
         * @return Never {@code null}.
         */
        public static ContentLock lock(Object content, String aspect, Object newOwner) {
            UUID lockId = createLockId(content, aspect);

            while (true) {
                ContentLock lock = Query
                        .from(ContentLock.class)
                        .where("_id = ?", lockId)
                        .master()
                        .noCache()
                        .first();

                if (lock != null) {
                    Object owner = lock.getOwner();

                    // Unlock if owner doesn't exist or is archived
                    if (owner == null || State.getInstance(owner).as(Content.ObjectModification.class).isTrash()) {
                        unlock(content, null, owner);
                    } else {
                        return lock;
                    }
                }

                lock = new ContentLock();
                lock.getState().setId(lockId);
                lock.setCreateDate(new Date());
                lock.setContentId(State.getInstance(content).getId());
                lock.setOwner(newOwner);
                lock.saveImmediately();
            }
        }

        /**
         * Unlocks the given {@code aspect} of the given {@code content}
         * if it's associated with the given {@code owner}.
         *
         * @param content Can't be {@code null}.
         * @param aspect If {@code null}, it's equivalent to an empty string.
         * @param owner If {@code null}, always unlocks.
         */
        public static void unlock(Object content, String aspect, Object owner) {
            ContentLock lock = Query
                    .from(ContentLock.class)
                    .where("_id = ?", createLockId(content, aspect))
                    .first();

            if (lock != null
                    && (owner == null
                    || owner.equals(lock.getOwner()))) {
                lock.delete();
            }
        }
    }
}
