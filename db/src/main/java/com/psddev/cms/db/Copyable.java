package com.psddev.cms.db;

import com.psddev.dari.db.ObjectIndex;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.db.State;
import com.psddev.dari.db.Trigger;
import com.psddev.dari.util.Settings;
import com.psddev.dari.util.UuidUtils;

import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Defines interface hook {@link #onCopy} for custom behavior when copying objects.
 */
public interface Copyable extends Recordable {

    String PRESERVE_OWNING_SITE_SETTING = "cms/tool/copiedObjectInheritsSourceObjectsSiteOwner";

    String COPY_SOURCE_ID_STATE_EXTRA = "cms.copySourceId";
    String COPY_SOURCE_TYPE_ID_STATE_EXTRA = "cms.copySourceTypeId";

    /**
     * Returns {@code true} if the specified object was generated by {@link #copy}
     * @param object the object to check
     * @return {@code true} if the specified object was generated by {@link #copy}
     */
    static boolean isCopying(Object object) {
        return State.getInstance(object).getExtras().containsKey(COPY_SOURCE_ID_STATE_EXTRA);
    }

    /**
     * Hook for defining custom behavior during object copy.  Each of the object's implementation
     * and its {@link com.psddev.dari.db.Modification Modifications'} implementations are invoked
     * individually.  The invocation can occur in any order, so {@code onCopy} definitions should
     * not be inter-dependent.
     *
     * The code defined within {@code onCopy} is executed on the copied {@link State} before it
     * is returned from {@link #copy}.
     * @param source the Object to copy
     */
    void onCopy(Object source);

    /**
     * Copies a source object where the copy is to be owned by the specified {@link Site}
     * with specified {@link ToolUser} as both {@link Content.ObjectModification#publishUser} and
     * {@link Content.ObjectModification#updateUser}.
     *
     * If a {@code targetTypeId} is specified, the copied object will be of the specified type, otherwise
     * it will be of the same type as the object identified by {@code sourceId}.
     *
     * @param source the source object to be copied
     * @param site the {@link Site} to be set as the {@link Site.ObjectModification#owner}
     * @param user the {@link ToolUser} to be set as {@link Content.ObjectModification#publishUser} and {@link Content.ObjectModification#updateUser}
     * @param targetTypeId the {@link State#id id} of the {@link ObjectType} to which the copy should be converted
     * @return the copy {@link State} after application of {@link #onCopy}
     */
    static State copy(Object source, Site site, ToolUser user, UUID targetTypeId) {

        if (source == null) {
            throw new IllegalArgumentException("Can't copy without a source! No source object was supplied!");
        }

        return copy(State.getInstance(source).getId(), site, user, targetTypeId);
    }

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
     * @return the copy {@link State} after application of {@link #onCopy}
     */
    static State copy(UUID sourceId, Site site, ToolUser user, UUID targetTypeId) {

        if (sourceId == null) {
            throw new IllegalArgumentException("Can't copy without a source! \"sourceId\" was missing!");
        }

        // Query source object including invisible references.  Cache is prevented which insures both that invisibles
        // are properly resolved and no existing instance of the source object becomes linked to the copy.
        // This prevents mutations to the new copy from affecting the original source object if it is subsequently saved.
        Object source = Query.fromAll().where("id = ?", sourceId).resolveInvisible().noCache().first();

        State sourceState = State.getInstance(source);

        ObjectType targetType = targetTypeId != null ? ObjectType.getInstance(targetTypeId) : sourceState.getType();

        if (targetType == null) {
            throw new IllegalStateException("Copy failed! Could not determine copy target type!");
        }

        UUID destinationId = UuidUtils.createSequentialUuid();
        Object destination = targetType.createObject(destinationId);
        State destinationState = State.getInstance(destination);
        Content.ObjectModification destinationContent = destinationState.as(Content.ObjectModification.class);
        Date now = new Date();

        // State#getRawValues must be used or invisible objects will not be included.
        destinationState.putAll(sourceState.getRawValues());
        destinationState.setId(destinationId);
        destinationState.setStatus(null);
        destinationState.setType(targetType);
        destinationState.setTypeId(targetType.getId());

        // Clear existing paths
        destinationState.as(Directory.ObjectModification.class).clearPaths();
        // Clear existing consumer Sites
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
            destinationState.getDatabase().getEnvironment().getIndexes().stream()
        )
        .filter(ObjectIndex::isVisibility)
        .map(ObjectIndex::getFields)
        .flatMap(Collection::stream)
        .forEach(destinationState::remove);

        // Set publishUser, updateUser, publishDate, and updateDate
        destinationContent.setPublishUser(user);
        destinationContent.setUpdateUser(user);
        destinationContent.setUpdateDate(now);
        destinationContent.setPublishDate(now);

        // If it or any of its modifications are copyable, fire onCopy()
        destinationState.fireTrigger(new CopyTrigger(source));

        return destinationState;
    }

    /**
     * Executes {@link #onCopy} on the object and for each {@link com.psddev.dari.db.Modification}.
     */
    class CopyTrigger implements Trigger {

        private Object source;

        public CopyTrigger(Object source) {
            this.source = source;
        }

        @Override
        public void execute(Object object) {
            if (object instanceof Copyable) {
                ((Copyable) object).onCopy(source);
            }
        }
    }
}
