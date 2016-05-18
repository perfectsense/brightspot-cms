package com.psddev.cms.db;

import com.google.common.base.Preconditions;
import com.psddev.dari.db.ObjectIndex;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.db.State;
import com.psddev.dari.db.Trigger;
import com.psddev.dari.util.Settings;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Defines interface hook {@link #onCopy} for custom behavior when copying objects.
 */
public interface Copyable extends Recordable {

    String PRESERVE_OWNING_SITE_SETTING = "cms/tool/copiedObjectInheritsSourceObjectsSiteOwner";

    /**
     * Hook for defining custom behavior during object copy.  Each of the object's implementation
     * and its {@link com.psddev.dari.db.Modification Modifications'} implementations of {@code onCopy}
     * are invoked.  The invocations can occur in any order, so {@code onCopy} definitions should
     * not be interdependent.
     *
     * The code defined within {@code onCopy} is executed on the copied {@link State} before it
     * is returned from {@link #copy}.
     * @param source the Object to copy
     */
    void onCopy(Object source);

    /**
     * Copies a source object and sets the copy to be owned by the specified {@link Site}.
     *
     * If a target {@link ObjectType} is specified, the copied object will be converted
     * to the specified type, otherwise it will be of the same type as the object identified
     * by {@code source}.
     *
     * @param source the source object to be copied
     * @param site the {@link Site} to be set as the {@link Site.ObjectModification#owner}
     * @param targetType the {@link ObjectType} to which the copy should be converted
     * @return the copy {@link State} after application of {@link #onCopy}
     */
    static State copy(Object source, Site site, ObjectType targetType) {

        UUID sourceId = State.getInstance(source).getId();

        Preconditions.checkNotNull(sourceId, "Can't copy without a source! No source object was supplied!");

        // Query source object including invisible references.  Cache is prevented which insures both that invisibles
        // are properly resolved and no existing instance of the source object becomes linked to the copy.
        // This prevents mutations to the new copy from affecting the original source object if it is subsequently saved.
        source = Query.fromAll().where("id = ?", sourceId).resolveInvisible().noCache().first();

        State sourceState = State.getInstance(source);

        if (targetType == null) {
            targetType = sourceState.getType();
        }

        Preconditions.checkState(targetType != null, "Copy failed! Could not determine copy target type!");

        State destinationState = State.getInstance(targetType.createObject(null));
        Content.ObjectModification destinationContent = destinationState.as(Content.ObjectModification.class);

        // State#getRawValues must be used or invisible objects will not be included.
        destinationState.putAll(sourceState.getRawValues());
        destinationState.setId(null);
        destinationState.setStatus(null);
        destinationState.setType(targetType);

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

        // Clear publishUser, updateUser, publishDate, and updateDate.
        destinationContent.setPublishUser(null);
        destinationContent.setUpdateUser(null);
        destinationContent.setUpdateDate(null);
        destinationContent.setPublishDate(null);

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
