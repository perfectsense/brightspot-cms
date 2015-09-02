package com.psddev.cms.db;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import com.psddev.cms.tool.AuthenticationFilter;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.DistributedLock;
import com.psddev.dari.db.Modification;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.State;
import com.psddev.dari.db.VisibilityLabel;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.PageContextFilter;

/** Represents a generic content. */
@Content.Searchable
public abstract class Content extends Record {

    private static final String PREFIX = "cms.content.";
    private static final String NOTIFICATIONS_EXTRA = PREFIX + "notifications";

    public static final String PUBLISH_DATE_FIELD = PREFIX + "publishDate";
    public static final String PUBLISH_USER_FIELD = PREFIX + "publishUser";
    public static final String UPDATE_DATE_FIELD = PREFIX + "updateDate";
    public static final String UPDATE_USER_FIELD = PREFIX  + "updateUser";

    public static final String SEARCHABLE_GROUP = PREFIX + "searchable";

    /** Returns the best permalink for this {@code object}. */
    public String getPermalink() {
        return as(Directory.ObjectModification.class).getPermalink();
    }

    /** Returns the date when this object was published. */
    public Date getPublishDate() {
        return as(ObjectModification.class).getPublishDate();
    }

    /** Returns the tool user that published this object. */
    public ToolUser getPublishUser() {
        return as(ObjectModification.class).getPublishUser();
    }

    /** Returns the date when this object was last updated. */
    public Date getUpdateDate() {
        return as(ObjectModification.class).getUpdateDate();
    }

    /** Returns the tool user that last updated this object. */
    public ToolUser getUpdateUser() {
        return as(ObjectModification.class).getUpdateUser();
    }

    /**
     * Returns the tool user that's currently logged in.
     *
     * @return May be {@code null}.
     */
    protected ToolUser getCurrentToolUser() {
        HttpServletRequest request = PageContextFilter.Static.getRequestOrNull();

        return request != null ? AuthenticationFilter.Static.getUser(request) : null;
    }

    /** Modification that adds CMS content information. */
    public static final class ObjectModification extends Modification<Object> implements VisibilityLabel {

        private ObjectModification() {
        }

        @DisplayName("Initial Draft")
        @Indexed(visibility = true)
        @InternalName("cms.content.draft")
        private Boolean draft;

        @DisplayName("Archived")
        @Indexed(visibility = true)
        @InternalName("cms.content.trashed")
        private Boolean trash;

        @Indexed
        @InternalName(PUBLISH_DATE_FIELD)
        @ToolUi.Filterable
        private Date publishDate;

        @Indexed
        @InternalName(PUBLISH_USER_FIELD)
        @ToolUi.Filterable(false)
        private ToolUser publishUser;

        @Indexed
        @InternalName(UPDATE_DATE_FIELD)
        private Date updateDate;

        @Indexed
        @InternalName(UPDATE_USER_FIELD)
        @ToolUi.Filterable(false)
        private ToolUser updateUser;

        @InternalName("cms.content.scheduleDate")
        private Date scheduleDate;

        /**
         * Returns {@code true} if this content is a draft.
         */
        public boolean isDraft() {
            return Boolean.TRUE.equals(draft);
        }

        /**
         * Sets whether this content is a draft.
         */
        public void setDraft(boolean draft) {
            this.draft = draft ? Boolean.TRUE : null;
        }

        /** Returns {@code true} if this content is a trash. */
        public boolean isTrash() {
            return Boolean.TRUE.equals(trash);
        }

        /** Sets whether this content is a trash. */
        public void setTrash(boolean trash) {
            this.trash = trash ? Boolean.TRUE : null;
        }

        /** Returns the date when the given {@code object} was published. */
        public Date getPublishDate() {
            return publishDate;
        }

        /** Sets the date when the given {@code object} was published. */
        public void setPublishDate(Date publishDate) {
            this.publishDate = publishDate;
        }

        /** Returns the tool user that published the given {@code object}. */
        public ToolUser getPublishUser() {
            return publishUser;
        }

        /** Sets the tool user that published the given {@code object}. */
        public void setPublishUser(ToolUser publishUser) {
            this.publishUser = publishUser;
        }

        /** Returns the date when the given {@code object} was last updated. */
        public Date getUpdateDate() {
            return updateDate;
        }

        /** Sets the date when the given {@code object} was last updated. */
        public void setUpdateDate(Date updateDate) {
            this.updateDate = updateDate;
        }

        /** Returns the tool user that last updated the given {@code object}. */
        public ToolUser getUpdateUser() {
            return updateUser;
        }

        /** Sets the tool user that last updated the given {@code object}. */
        public void setUpdateUser(ToolUser updateUser) {
            this.updateUser = updateUser;
        }

        public Date getScheduleDate() {
            return scheduleDate;
        }

        public void setScheduleDate(Date scheduleDate) {
            this.scheduleDate = scheduleDate;
        }

        /**
         * Adds the given {@code notification} to be processed on save.
         *
         * @param notification If {@code null}, does nothing.
         * @deprecated Use {@link Record#beforeSave} or {@link Record#afterSave} instead.
         */
        @Deprecated
        public void addNotification(Notification notification) {
            if (notification != null) {
                Map<String, Object> extras = getState().getExtras();
                @SuppressWarnings("unchecked")
                List<Notification> notifications = (List<Notification>) extras.get(NOTIFICATIONS_EXTRA);

                if (notifications == null) {
                    notifications = new ArrayList<Notification>();
                    extras.put(NOTIFICATIONS_EXTRA, notifications);
                }

                notifications.add(notification);
            }
        }

        // --- VisibilityLabel support ---

        @Override
        public String createVisibilityLabel(ObjectField field) {
            if (field.getInternalName().equals("cms.content.draft")) {
                return isDraft() ? "Initial Draft" : null;
            } else {
                return isTrash() ? "Archived" : null;
            }
        }
    }

    /** Static utility methods. */
    public static final class Static {

        private Static() {
        }

        /**
         * Deletes the given {@code object}, and returns a trash object
         * that can be used later to restore it.
         *
         * @deprecated Use {@link #trash} instead.
         */
        @Deprecated
        public static Trash deleteSoftly(Object object, Site site, ToolUser user) {
            State objectState = State.getInstance(object);
            Site.ObjectModification objectSiteMod = objectState.as(Site.ObjectModification.class);

            if (site == null || ObjectUtils.equals(objectSiteMod.getOwner(), site)) {
                Trash trash = new Trash(user, object);
                try {
                    trash.beginWrites();
                    objectState.delete();
                    trash.save();
                    trash.commitWrites();
                    return trash;
                } finally {
                    trash.endWrites();
                }

            } else {
                objectSiteMod.getConsumers().remove(site);
                if (objectSiteMod.isGlobal()) {
                    objectSiteMod.getBlacklist().add(site);
                }
                objectState.save();
                return null;
            }
        }

        /**
         * Returns {@code true} if the instances of the given {@code type}
         * is searchable.
         */
        public static boolean isSearchableType(ObjectType type) {
            return type != null
                    && type.getGroups().contains(SEARCHABLE_GROUP);
        }

        /** Returns {@code true} if the given {@code object} is searchable. */
        public static boolean isSearchable(Object object) {
            return object != null
                    && isSearchableType(State.getInstance(object).getType());
        }

        /**
         * Publishes the given {@code object} in the given {@code site}
         * as the given {@code user}.
         *
         * @param object Can't be {@code null}.
         * @param site May be {@code null}.
         * @param user May be {@code null}.
         * @return Can be used to revert all changes. May be {@code null}.
         */
        public static History publish(Object object, Site site, ToolUser user) {
            State state = State.getInstance(object);
            ObjectModification contentData = state.as(ObjectModification.class);
            Site.ObjectModification siteData = state.as(Site.ObjectModification.class);

            if (object instanceof Site) {
                site = (Site) object;
                siteData.setOwner(site);

            } else if (state.isNew()
                    && siteData.getOwner() == null) {
                siteData.setOwner(site);
            }

            Date now = new Date();
            Date publishDate = contentData.getPublishDate();
            ToolUser publishUser = contentData.getPublishUser();

            if (publishDate == null) {
                contentData.setPublishDate(now);
            }

            if (publishUser == null) {
                contentData.setPublishUser(user);
            }

            contentData.setUpdateDate(now);
            contentData.setUpdateUser(user);

            if (object instanceof Draft) {
                state.save();
                return null;

            } else {
                try {
                    state.beginWrites();
                    state.save();

                    History history = new History(user, object);

                    history.save();
                    state.commitWrites();
                    return history;

                } finally {
                    state.endWrites();
                }
            }
        }

        public static History publishDifferences(
                Object object,
                Map<String, Map<String, Object>> differences,
                Site site,
                ToolUser user) {

            State state = State.getInstance(object);
            UUID id = state.getId();
            DistributedLock lock = DistributedLock.Static.getInstance(
                    Database.Static.getDefault(),
                    Content.class.getName() + "/publish/" + id);

            lock.lock();

            try {
                Object oldObject = Query.fromAll().where("_id = ?", id).noCache().first();

                if (oldObject != null) {
                    state.setValues(Draft.mergeDifferences(
                            state.getDatabase().getEnvironment(),
                            State.getInstance(oldObject).getSimpleValues(),
                            differences));
                }

                return publish(object, site, user);

            } finally {
                lock.unlock();
            }
        }

        /**
         * Trashes the given {@code object} so that it's not usable in
         * the given {@code site}.  If the {@link Site} is the {@link com.psddev.cms.db.Site.ObjectModification#owner},
         * then the object will be trashed for all {@link com.psddev.cms.db.Site.ObjectModification#consumers}.
         * @param object the object for which trash status will be set
         * @param site the site within which the trash status will be reflected
         * @param user the user for whom this change is made
         */
        public static void trash(Object object, Site site, ToolUser user) {
            toggleTrash(object, site, user, true);
        }

        /**
         * Restores the given {@code object} so that it's usable in
         * the given {@code site}.  If the {@link Site} is the {@link com.psddev.cms.db.Site.ObjectModification#owner},
         * then the object will be restored for all {@link com.psddev.cms.db.Site.ObjectModification#consumers}.
         * @param object the object for which trash status will be removed
         * @param site the site within which the trash status will be reflected
         * @param user the user for whom this change is made
         */
        public static void restore(Object object, Site site, ToolUser user) {
            toggleTrash(object, site, user, false);
        }

        /**
         * Trashes or restores the given {@code object} for the given {@code site} based
         * on the value of {@code trash}.  If the {@link Site} is the {@link com.psddev.cms.db.Site.ObjectModification#owner},
         * then the change in trash status will be reflected for all {@link com.psddev.cms.db.Site.ObjectModification#consumers}.
         * @param object the object for which trash status will be changed
         * @param site the site within which the trash status will be reflected
         * @param user the user for whom this change is made
         * @param trash {@code true} to set status to trashed, {@code false} to un-set trashed status
         */
        private static void toggleTrash(Object object, Site site, ToolUser user, boolean trash) {

            State state = State.getInstance(object);
            Site.ObjectModification siteData = state.as(Site.ObjectModification.class);

            if (object instanceof ToolEntity
                    || site == null
                    || ObjectUtils.equals(siteData.getOwner(), site)) {
                ObjectModification contentData = state.as(ObjectModification.class);

                contentData.setTrash(trash);
                contentData.setUpdateDate(new Date());
                contentData.setUpdateUser(user);
                state.save();

            } else {

                if (trash) {
                    siteData.getConsumers().remove(site);
                } else {
                    siteData.getConsumers().add(site);
                }

                if (siteData.isGlobal()) {

                    if (trash) {
                        siteData.getBlacklist().add(site);
                    } else {
                        siteData.getBlacklist().remove(site);
                    }
                }
            }
        }

        /**
         * Purges the given {@code object} completely, including all of
         * its drafts, histories, and trashes.
         */
        public static void purge(Object object, Site site, ToolUser user) {
            State objectState = State.getInstance(object);
            Site.ObjectModification objectSiteMod = objectState.as(Site.ObjectModification.class);

            if (!ObjectUtils.equals(objectSiteMod.getOwner(), site)) {
                trash(object, site, user);
                return;
            }

            Database database = objectState.getDatabase();
            UUID objectId = objectState.getId();

            try {
                database.beginWrites();
                database.deleteByQuery(Query.from(Draft.class).where("objectId = ?", objectId));
                database.deleteByQuery(Query.from(History.class).where("objectId = ?", objectId));
                database.deleteByQuery(Query.from(Trash.class).where("objectId = ?", objectId));
                objectState.delete();
                database.commitWrites();
            } finally {
                database.endWrites();
            }
        }
    }

    /**
     * Specifies whether the instances of the target type should be
     * searchable.
     */
    @Documented
    @Inherited
    @ObjectType.AnnotationProcessorClass(SearchableProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Searchable {
        boolean value() default true;
    }

    private static class SearchableProcessor implements ObjectType.AnnotationProcessor<Searchable> {
        @Override
        public void process(ObjectType type, Searchable annotation) {
            Set<String> groups = type.getGroups();
            if (annotation.value()) {
                groups.add(SEARCHABLE_GROUP);
            } else {
                groups.remove(SEARCHABLE_GROUP);
            }
        }
    }

    /** @deprecated Use {@link ObjectModification} or {@link Static} instead. */
    @Deprecated
    public abstract static class Global extends Record {

        /** @deprecated Use {@link ObjectModification#getPublishDate} instead. */
        @Deprecated
        public static Date getPublishDate(Object object) {
            return State.getInstance(object).as(ObjectModification.class).getPublishDate();
        }

        /** @deprecated Use {@link ObjectModification#getPublishUser} instead. */
        @Deprecated
        public static ToolUser getPublishUser(Object object) {
            return State.getInstance(object).as(ObjectModification.class).getPublishUser();
        }

        /** @deprecated Use {@link ObjectModification#getUpdateDate} instead. */
        @Deprecated
        public static Date getUpdateDate(Object object) {
            return State.getInstance(object).as(ObjectModification.class).getUpdateDate();
        }

        /** @deprecated Use {@link ObjectModification#getUpdateUser} instead. */
        @Deprecated
        public static ToolUser getUpdateUser(Object object) {
            return State.getInstance(object).as(ObjectModification.class).getUpdateUser();
        }

        /** @deprecated Use {@link Static#publish} instead. */
        @Deprecated
        public static History publish(ToolUser user, Object object) {
            return Static.publish(object, null, user);
        }

        /** @deprecated Use {@link Static#deleteSoftly} instead. */
        @Deprecated
        public static Trash deleteSoftly(ToolUser user, Object object) {
            return Static.deleteSoftly(object, null, user);
        }

        /** @deprecated Use {@link Static#purge} instead. */
        @Deprecated
        public static void purge(ToolUser user, Object object) {
            Static.purge(object, null, user);
        }

        /** @deprecated Use {@link Static#isSearchableType} instead. */
        @Deprecated
        public static boolean isSearchableType(ObjectType type) {
            return Static.isSearchableType(type);
        }

        /** @deprecated Use {@link Static#isSearchable} instead. */
        @Deprecated
        public static boolean isSearchable(Object object) {
            return Static.isSearchable(object);
        }
    }
}
