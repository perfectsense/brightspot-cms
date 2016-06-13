package com.psddev.cms.db;

import com.google.common.base.Preconditions;
import com.psddev.dari.db.ObjectIndex;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.db.State;
import com.psddev.dari.db.Trigger;

import java.util.Collection;
import java.util.stream.Stream;

/**
 * Interface for defining custom behavior when copying objects through {@link #onCopy}.
 */
public interface Copyable extends Recordable {

    /**
     * Hook for defining custom behavior during object copy.  Each of the object's implementation
     * and its {@link com.psddev.dari.db.Modification Modifications'} implementations of {@code onCopy}
     * are invoked.  The invocations can occur in any order, so {@code onCopy} definitions should
     * not be interdependent.
     * <p>
     * The code defined within {@code onCopy} is executed on the copied {@link State} before it
     * is returned from {@link #copy}.
     *
     * @param source the Object to copy
     */
    void onCopy(Object source);

    /**
     * Copies the given {@code source} object into an instance of the given
     * {@code targetType}.
     * <p>
     * If a target {@link ObjectType} is specified, the copied object will be converted
     * to the specified type, otherwise it will be of the same type as the object identified
     * by {@code source}.
     *
     * @param targetClass the class to which the copy should be converted
     * @param source     the source object to be copied
     * @return the copy {@link State} after application of {@link #onCopy}
     */
    @SuppressWarnings("unchecked")
    static <T> T copy(Class<T> targetClass, Object source) {
        Preconditions.checkNotNull(targetClass, "targetClass");
        Preconditions.checkNotNull(source, "source");

        // Query source object including invisible references.  Cache is prevented which insures both that invisibles
        // are properly resolved and no existing instance of the source object becomes linked to the copy.
        // This prevents mutations to the new copy from affecting the original source object if it is subsequently saved.
        source = Query.fromAll().where("id = ?", source).resolveInvisible().noCache().first();

        State sourceState = State.getInstance(source);
        ObjectType targetType = sourceState.getDatabase().getEnvironment().getTypeByClass(targetClass);

        Preconditions.checkState(targetType != null, "Copy failed! Could not determine copy target type!");

        Object destination = targetType.createObject(null);
        State destinationState = State.getInstance(destination);
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

        // Unset all visibility indexes
        Stream.concat(
                destinationState.getIndexes().stream(),
                destinationState.getDatabase().getEnvironment().getIndexes().stream())
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

        return (T) destination;
    }

    /**
     * Copies the given {@code source} object.
     */
    @SuppressWarnings("unchecked")
    static <T> T copy(T source) {
        return (T) copy(source.getClass(), source);
    }
}

/**
 * Executes {@link Copyable#onCopy} on the object and for each {@link com.psddev.dari.db.Modification}.
 */
@SuppressWarnings("unchecked")
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
