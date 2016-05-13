package com.psddev.cms.db;

import com.psddev.dari.db.ObjectIndex;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.db.State;
import com.psddev.dari.db.Trigger;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.Settings;
import com.psddev.dari.util.UuidUtils;

import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Defines interface hook {@link #beforeCopy} for custom behavior when copying objects.
 *
 * Provides trigger to be fired by {@link CopyFilter} upon copying
 * content in the CMS.
 */
public interface Copyable extends Recordable {

    String PRESERVE_OWNING_SITE_SETTING = "cms/tool/copiedObjectInheritsSourceObjectsSiteOwner";

    String COPY_SOURCE_ID_STATE_EXTRA = "cms.copySourceId";
    String COPY_SOURCE_TYPE_ID_STATE_EXTRA = "cms.copySourceTypeId";
    String IS_COPYABLE_STATE_EXTRA = "cms.isCopyable";

    /**
     * Returns {@code true} if the specified object was generated by {@link #copy}
     * @param object the object to check
     * @return {@code true} if the specified object was generated by {@link #copy}
     */
    static boolean isCopying(Object object) {
        return State.getInstance(object).getExtras().containsKey(COPY_SOURCE_ID_STATE_EXTRA);
    }

    /**
     * Returns {@code true} if the specified object or any of its {@link com.psddev.dari.db.Modification Modifications}
     * implements {@link Copyable}.
     * @param object the object to check
     * @return {@code true} if the specified object or any of its {@link com.psddev.dari.db.Modification Modifications}
     * implements {@link Copyable}.
     */
    static boolean isCopyable(Object object) {

        State state = State.getInstance(object);
        state.getExtras().remove(IS_COPYABLE_STATE_EXTRA);
        state.fireTrigger(new CopyCapabilityTrigger());
        return ObjectUtils.to(boolean.class, state.getExtra(IS_COPYABLE_STATE_EXTRA));
    }

    /**
     * Gets the {@link State#id} of the source object if the specified object is being copied
     * or {@code null} if the specified object is not being copied.
     * @param object the object from which to get the source {@link UUID}
     * @return the {@link State#id} of the source object for the specified copied object.
     */
    static UUID getCopySourceId(Object object) {
        return (UUID) State.getInstance(object).getExtra(COPY_SOURCE_ID_STATE_EXTRA);
    }

    /**
     * Gets the {@link State#typeId} of the source object if the specified object is being copied
     * or {@code null} if the specified object is not being copied.
     * @param object the object from which to get the source {@link State#typeId}
     * @return the {@link State#typeId} of the source object for the specified copied object.
     */
    static UUID getCopyTypeId(Object object) {
        return (UUID) State.getInstance(object).getExtra(COPY_SOURCE_TYPE_ID_STATE_EXTRA);
    }

    /**
     * Hook for defining custom behavior during object copy.  Each of the object's implementation
     * and its {@link com.psddev.dari.db.Modification Modifications'} implementations are invoked
     * individually.  The invocation can occur in any order, so {@code beforeCopy} definitions should
     * not be inter-dependent.
     *
     * The code defined within {@code beforeCopy} is executed on the copied {@link State} before it
     * is returned from {@link #copy}.
     */
    void beforeCopy();

    /**
     * Copies a source object specified by {@code sourceId} where the copy is to be owned by the
     * specified {@link Site} with specified {@link ToolUser} as both {@link Content.ObjectModification#publishUser}
     * and {@link Content.ObjectModification#updateUser}.
     *
     * If a {@code targetTypeId} is specified, the copied object will be of the specified type, otherwise
     * it will be of the same type as the object identified by {@code sourceId}.
     *
     * @param sourceId the {@link State#id} of the source object to be copied
     * @param site the {@link Site} to be set as the {@link Site.ObjectModification#owner}
     * @param user the {@link ToolUser} to be set as {@link Content.ObjectModification#publishUser} and {@link Content.ObjectModification#updateUser}
     * @param targetTypeId the {@link State#id id} of the {@link ObjectType} to which the copy should be converted
     * @return the copy {@link State} after application of {@link #beforeCopy}
     */
    static State copy(UUID sourceId, Site site, ToolUser user, UUID targetTypeId) {

        if (sourceId == null) {
            throw new IllegalArgumentException("Can't copy without a source! \"sourceId\" was missing!");
        }

        // Query source object including invisible references.  Cache is prevented which insures both that invisibles
        // are properly resolved and no existing instance of the source object becomes linked to the copy.
        // This prevents mutations to the new copy from affecting the original source object if it is subsequently saved.
        Object sourceObject = Query.fromAll().where("id = ?", sourceId).resolveInvisible().noCache().first();
        return copy(sourceObject, site, user, targetTypeId);
    }

    /**
     * Copies a source object where the copy is to be owned by the specified {@link Site}
     * with specified {@link ToolUser} as both {@link Content.ObjectModification#publishUser} and
     * {@link Content.ObjectModification#updateUser}.
     *
     * If a {@code targetTypeId} is specified, the copied object will be of the specified type, otherwise
     * it will be of the same type as the object identified by {@code sourceId}.
     *
     * WARNING: It is recommended to use the {@link #copy(UUID, Site, ToolUser, UUID)} signature to safeguard
     * against mutation in the {@code sourceObject}.
     *
     * By nature of the underlying copy mechanism (copy of a Map), the copy itself is not a deep copy.
     * The {@code sourceObject}'s {@link State} will contain values (e.g. collections) that will
     * exist in both the source and the copied {@link State}.
     *
     * {@link #copy(UUID, Site, ToolUser, UUID)} generates a new instance of the source object by UUID
     * which is discarded after copy, which avoids any scenarios where {@link #beforeCopy} might mutate
     * values in both the copied and source {@link State States}.
     *
     * @param sourceObject the source object to be copied
     * @param site the {@link Site} to be set as the {@link Site.ObjectModification#owner}
     * @param user the {@link ToolUser} to be set as {@link Content.ObjectModification#publishUser} and {@link Content.ObjectModification#updateUser}
     * @param targetTypeId the {@link State#id id} of the {@link ObjectType} to which the copy should be converted
     * @return the copy {@link State} after application of {@link #beforeCopy}
     */
    static State copy(Object sourceObject, Site site, ToolUser user, UUID targetTypeId) {

        if (sourceObject == null) {
            throw new IllegalArgumentException("Can't copy without a source! No source object was supplied!");
        }

        State sourceState = State.getInstance(sourceObject);

        ObjectType targetType = targetTypeId != null ? ObjectType.getInstance(targetTypeId) : sourceState.getType();

        if (targetType == null) {
            throw new IllegalStateException("Copy failed! Could not determine copy target type!");
        }

        UUID sourceTypeId = sourceState.getTypeId();
        UUID destinationId = UuidUtils.createSequentialUuid();
        Object destination = targetType.createObject(destinationId);
        State destinationState = State.getInstance(destination);
        Content.ObjectModification destinationContent = destinationState.as(Content.ObjectModification.class);
        Date now = new Date();

        // State#getRawValues must be used or invisible objects will not be included.
        destinationState.putAll(sourceState.getRawValues());
        destinationState.setId(destinationId);
        destinationState.setType(targetType);
        destinationState.setTypeId(targetType.getId());

        // Behavior copied from edit.jsp's copy routine
        destinationState.as(Directory.ObjectModification.class).clearPaths();
        for (Site consumer : destinationState.as(Site.ObjectModification.class).getConsumers()) {
            destinationState.as(Directory.ObjectModification.class).clearSitePaths(consumer);
        }
        if (site != null
            && !Settings.get(boolean.class, PRESERVE_OWNING_SITE_SETTING)) {
            // Only set the owner to current site if not on global and no setting to dictate otherwise.
            destinationState.as(Site.ObjectModification.class).setOwner(site);
        }

        // Unset all visibility indexes
        Stream.concat(
            destinationState.getIndexes().stream(),
            destinationState.getDatabase().getEnvironment().getIndexes().stream())
            .filter(ObjectIndex::isVisibility)
            .forEach(index -> destinationState.remove(index.getField()));

        // Set publishUser, updateUser, publishDate, and updateDate
        destinationContent.setPublishUser(user);
        destinationContent.setUpdateUser(user);
        destinationContent.setUpdateDate(now);
        destinationContent.setPublishDate(now);

        // Set cms.copySourceId to the source ID to provide additional behavior in beforeSave/afterSave.
        destinationState.getExtras().put(COPY_SOURCE_ID_STATE_EXTRA, sourceState.getId());
        destinationState.getExtras().put(COPY_SOURCE_TYPE_ID_STATE_EXTRA, sourceTypeId);

        // If it or any of its modifications are copyable, fire onCopy()
        destinationState.fireTrigger(new CopyTrigger());

        return destinationState;
    }

    /**
     * Executes {@link #beforeCopy} on the object and for each {@link com.psddev.dari.db.Modification}.
     */
    class CopyTrigger implements Trigger {

        @Override
        public void execute(Object object) {
            if (object instanceof Copyable) {
                ((Copyable) object).beforeCopy();
            }
        }
    }

    /**
     * Sets {@link #IS_COPYABLE_STATE_EXTRA} {@link State#extras} flag if this
     * object or any of its {@link com.psddev.dari.db.Modification Modifications}
     * implement {@link Copyable}.
     */
    class CopyCapabilityTrigger implements Trigger {

        @Override
        public void execute(Object object) {
            if (object instanceof Copyable) {
                State.getInstance(object).getExtras().put(IS_COPYABLE_STATE_EXTRA, true);
            }
        }
    }
}
