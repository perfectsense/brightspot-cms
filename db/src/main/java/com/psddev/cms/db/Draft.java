package com.psddev.cms.db;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.DatabaseEnvironment;
import com.psddev.dari.db.DistributedLock;
import com.psddev.dari.db.Modification;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.UuidUtils;

/** Unpublished object or unsaved changes to an existing object. */
@ToolUi.Hidden
public class Draft extends Content {

    private static final String OLD_VALUES_EXTRA = "cms.draft.oldValues";
    private static final Object REMOVED = new Object();

    @Indexed
    private DraftStatus status;

    @Indexed
    private Schedule schedule;

    private String name;

    @Indexed
    private ToolUser owner;

    @Indexed
    @Required
    private ObjectType objectType;

    @Indexed
    @Required
    private UUID objectId;

    @Deprecated
    private Map<String, Object> objectChanges;

    @Indexed
    private boolean newContent;

    private Map<String, Map<String, Object>> differences;

    /**
     * Finds the differences between the given {@code oldValues} and
     * {@code newValues}.
     *
     * @param environment
     *        Can't be {@code null}.
     *
     * @param oldValues
     *        May be {@code null}.
     *
     * @param newValues
     *        May be {@code null}.
     *
     * @return Never {@code null}.
     */
    public static Map<String, Map<String, Object>> findDifferences(
            DatabaseEnvironment environment,
            Map<String, Object> oldValues,
            Map<String, Object> newValues) {

        Map<String, Map<String, Object>> newIdMaps = newValues != null
                ? findIdMaps(newValues)
                : new CompactMap<>();

        if (oldValues == null) {
            return newIdMaps;
        }

        Map<String, Map<String, Object>> oldIdMaps = findIdMaps(oldValues);
        Map<String, Map<String, Object>> differences = new CompactMap<>();

        newIdMaps.keySet().stream().forEach(id -> {
            Map<String, Object> oldIdMap = oldIdMaps.get(id);
            Map<String, Object> newIdMap = newIdMaps.get(id);
            Map<String, Object> changes = new CompactMap<>();
            ObjectType type = environment.getTypeById(ObjectUtils.to(UUID.class, newIdMap.get(State.TYPE_KEY)));
            Set<String> keys = new LinkedHashSet<>(newIdMap.keySet());

            if (oldIdMap != null) {
                keys.addAll(oldIdMap.keySet());
            }

            keys.forEach(key -> {
                Object oldValue = oldIdMap != null ? oldIdMap.get(key) : null;
                Object newValue = newIdMap.get(key);

                if (ObjectUtils.equals(oldValue, newValue)) {
                    return;
                }

                if (ObjectUtils.isBlank(oldValue)
                        && ObjectUtils.isBlank(newValue)) {

                    return;
                }

                if (type != null) {
                    ObjectField field = type.getField(key);

                    if (field == null) {
                        field = environment.getField(key);
                    }

                    if (field != null) {
                        String fieldType = field.getInternalType();

                        if (ObjectField.BOOLEAN_TYPE.equals(fieldType)) {
                            if ((oldValue == null || Boolean.FALSE.equals(oldValue))
                                    && (newValue == null || Boolean.FALSE.equals(newValue))) {

                                return;
                            }

                        } else if (ObjectField.NUMBER_TYPE.equals(fieldType)) {
                            Double oldValueDouble = ObjectUtils.to(Double.class, oldValue);

                            if (oldValueDouble != null) {
                                Double newValueDouble = ObjectUtils.to(Double.class, newValue);

                                if (newValueDouble != null && oldValueDouble.equals(newValueDouble)) {
                                    return;
                                }
                            }

                        } else if (fieldType.startsWith(ObjectField.SET_TYPE + "/")) {
                            if (ObjectUtils.equals(ObjectUtils.to(Set.class, oldValue), ObjectUtils.to(Set.class, newValue))) {
                                return;
                            }
                        }
                    }
                }

                changes.put(key, newValue);
            });

            if (!changes.isEmpty()) {
                differences.put(id, changes);
            }
        });

        return differences;
    }

    private static Map<String, Map<String, Object>> findIdMaps(Object value) {
        Map<String, Map<String, Object>> valuesById = new CompactMap<>();

        addIdMaps(valuesById, value);

        for (Map.Entry<String, Map<String, Object>> entry : valuesById.entrySet()) {
            entry.setValue(minify(entry.getValue()));
        }

        return valuesById;
    }

    private static void addIdMaps(Map<String, Map<String, Object>> valuesById, Object value) {
        Collection<?> collection = null;

        if (value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) value;
            String id = ObjectUtils.to(String.class, map.get(State.ID_KEY));

            if (id != null) {
                if (valuesById.containsKey(id)) {
                    id = UuidUtils.createSequentialUuid().toString();

                    map.put(State.ID_KEY, id);
                }

                valuesById.put(id, new CompactMap<>(map));
            }

            collection = map.values();

        } else if (value instanceof Collection) {
            collection = (Collection<?>) value;
        }

        if (collection != null) {
            collection.forEach(item -> addIdMaps(valuesById, item));
        }
    }

    private static Map<String, Object> minify(Map<String, Object> map) {
        Map<String, Object> minified = new CompactMap<>();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            minified.put(entry.getKey(), minifyValue(entry.getValue()));
        }

        return minified;
    }

    @SuppressWarnings("unchecked")
    private static Object minifyValue(Object value) {
        if (value instanceof Map) {
            Map<String, Object> valueMap = (Map<String, Object>) value;
            String id = ObjectUtils.to(String.class, valueMap.get(State.ID_KEY));

            if (id != null) {
                return ImmutableMap.of(State.ID_KEY, id);

            } else {
                return minify((Map<String, Object>) value);
            }

        } else if (value instanceof Collection) {
            return ((Collection<Object>) value)
                    .stream()
                    .map(Draft::minifyValue)
                    .collect(Collectors.toList());

        } else {
            return value;
        }
    }

    /**
     * Merges the given {@code differences} into the given {@code oldValues}.
     *
     * @param environment
     *        Can't be {@code null}.
     *
     * @param oldValues
     *        Can't be {@code null}.
     *
     * @param differences
     *        If blank, returns the given {@code oldValues} as is.
     *
     * @return Never {@code null}.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> mergeDifferences(
            DatabaseEnvironment environment,
            Map<String, Object> oldValues,
            Map<String, Map<String, Object>> differences) {

        Preconditions.checkNotNull(oldValues);

        return differences != null && !differences.isEmpty()
                ? (Map<String, Object>) mergeValue(environment, findIdMaps(oldValues), differences, oldValues)
                : oldValues;
    }

    @SuppressWarnings("unchecked")
    private static Object mergeValue(
            DatabaseEnvironment environment,
            Map<String, Map<String, Object>> oldIdMaps,
            Map<String, Map<String, Object>> differences,
            Object value) {

        if (value instanceof Map) {
            Map<String, Object> valueMap = (Map<String, Object>) value;
            Map<String, Object> newIdMap = new CompactMap<>();
            String valueId = ObjectUtils.to(String.class, valueMap.get(State.ID_KEY));

            if (valueId != null) {
                Map<String, Object> oldIdMap = oldIdMaps.get(valueId);
                Map<String, Object> changes = differences.get(valueId);

                if (oldIdMap != null) {
                    newIdMap.putAll(oldIdMap);
                }

                if (changes != null) {
                    newIdMap.putAll(changes);
                }

                for (Map.Entry<String, Object> entry : newIdMap.entrySet()) {
                    entry.setValue(mergeValue(environment, oldIdMaps, differences, entry.getValue()));
                }

                if (newIdMap.get(State.ID_KEY) == null) {
                    return REMOVED;
                }

            } else {
                valueMap.forEach((k, v) -> newIdMap.put(k, mergeValue(environment, oldIdMaps, differences, v)));
            }

            return newIdMap;

        } else if (value instanceof List) {
            return ((List<Object>) value)
                    .stream()
                    .map(item -> mergeValue(environment, oldIdMaps, differences, item))
                    .filter(item -> item != REMOVED)
                    .collect(Collectors.toList());
        }

        return value;
    }

    /**
     * Finds the old values of the given {@code object} before the draft
     * differences were merged.
     *
     * @param object
     *        Can't be {@code null}.
     *
     * @return Never {@code null}.
     */
    public static Map<String, Object> findOldValues(Object object) {
        Preconditions.checkNotNull(object);

        State state = State.getInstance(object);
        @SuppressWarnings("unchecked")
        Map<String, Object> oldValues = (Map<String, Object>) state.getExtra(OLD_VALUES_EXTRA);

        return oldValues != null ? oldValues : state.getSimpleValues();
    }

    /** Returns the status. */
    public DraftStatus getStatus() {
        return status;
    }

    /** Sets the status. */
    public void setStatus(DraftStatus status) {
        this.status = status;
    }

    /** Returns the schedule. */
    public Schedule getSchedule() {
        return schedule;
    }

    /** Sets the schedule. */
    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }

    /** Returns the name. */
    public String getName() {
        return name;
    }

    /** Sets the name. */
    public void setName(String name) {
        this.name = name;
    }

    /** Returns the owner. */
    public ToolUser getOwner() {
        return owner;
    }

    /** Sets the owner. */
    public void setOwner(ToolUser owner) {
        this.owner = owner;
    }

    /** Returns the originating object's type. */
    public ObjectType getObjectType() {
        return objectType;
    }

    /** Sets the originating object's type ID. */
    public void setObjectType(ObjectType type) {
        this.objectType = type;
    }

    /** Returns the originating object's ID. */
    public UUID getObjectId() {
        return objectId;
    }

    /** Sets the originating object's ID. */
    public void setObjectId(UUID objectId) {
        this.objectId = objectId;
    }

    /**
     * Returns the map of all the values to be changed on the originating
     * object.
     *
     * @deprecated Use {@link #getDifferences()} instead.
     */
    @Deprecated
    public Map<String, Object> getObjectChanges() {
        if (objectChanges == null) {
            objectChanges = new LinkedHashMap<String, Object>();
        }
        return objectChanges;
    }

    /**
     * Sets the map of all the values to be changed on the originating
     * object.
     *
     * @deprecated Use {@link #setDifferences(Map)} instead.
     */
    @Deprecated
    public void setObjectChanges(Map<String, Object> values) {
        this.objectChanges = values;
    }

    public boolean isNewContent() {
        return newContent;
    }

    public void setNewContent(boolean newContent) {
        this.newContent = newContent;
    }

    /**
     * @return Never {@code null}.
     */
    @SuppressWarnings("deprecation")
    public Map<String, Map<String, Object>> getDifferences() {
        if ((differences == null
                || differences.isEmpty())
                && objectChanges != null
                && !objectChanges.isEmpty()) {

            ObjectType type = getObjectType();

            if (type != null) {
                UUID id = getObjectId();

                if (id != null) {
                    Map<String, Object> values = new CompactMap<>(objectChanges);

                    values.put(State.ID_KEY, id.toString());
                    values.put(State.TYPE_KEY, type.getId().toString());

                    return findIdMaps(values);
                }
            }
        }

        if (differences == null) {
            differences = new CompactMap<>();
        }

        return differences;
    }

    public void setDifferences(Map<String, Map<String, Object>> differences) {
        this.differences = differences;
    }

    /**
     * @deprecated Use {@link #recreate()} instead.
     */
    @Deprecated
    public Object getObject() {
        return recreate();
    }

    /**
     * Recreates the originating object with the differences merged.
     *
     * @return {@code null} if the object type is {@code null}.
     */
    @SuppressWarnings("deprecation")
    public Object recreate() {
        ObjectType type = getObjectType();

        if (type == null) {
            return null;
        }

        UUID id = getObjectId();
        Object object = Query.fromAll()
                .where("_id = ?", id)
                .noCache()
                .resolveInvisible()
                .first();

        if (object == null) {
            object = type.createObject(id);
        }

        merge(object);

        return object;
    }

    /**
     * @deprecated Use {@link #findOldValues(Object)} and
     *             {@link #update(Map, Object)} instead.
     */
    @Deprecated
    public void setObject(Object object) {
        update(findOldValues(object), object);
    }

    /**
     * Updates all necessary fields to recreate the object later using
     * the differences between the given {@code oldValues} and
     * {@code newObject}.
     *
     * @param oldValues
     *        May be {@code null}.
     *
     * @param newObject
     *        Can't be {@code null}.
     */
    public void update(Map<String, Object> oldValues, Object newObject) {
        Preconditions.checkNotNull(newObject);

        State newState = State.getInstance(newObject);
        UUID newId = newState.getId();

        setObjectType(newState.getType());
        setObjectId(newId);
        setDifferences(findDifferences(
                newState.getDatabase().getEnvironment(),
                oldValues,
                newState.getSimpleValues()));

        State newStateCopy = State.getInstance(Query.fromAll()
                .where("_id = ?", newId)
                .noCache()
                .first());

        if (newStateCopy == null) {
            setName("#1");
            newState.as(NameData.class).setIndex(1);

        } else {
            DistributedLock lock = DistributedLock.Static.getInstance(
                    Database.Static.getDefault(),
                    getClass().getName() + "/" + newId);

            lock.lock();

            try {
                NameData nameData = newStateCopy.as(NameData.class);

                Integer index = nameData.getIndex();
                index = index != null ? index + 1 : 1;

                if (ObjectUtils.isBlank(getName())) {
                    setName("#" + index);
                }

                nameData.setIndex(index);
                nameData.save();

            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * Merges the differences into the given {@code object}.
     *
     * @param object
     *        Can't be {@code null}.
     */
    @SuppressWarnings("deprecation")
    public void merge(Object object) {
        Preconditions.checkNotNull(object);

        State state = State.getInstance(object);
        Map<String, Object> oldValues = state.getSimpleValues();

        state.getExtras().put(OLD_VALUES_EXTRA, oldValues);
        state.setValues(mergeDifferences(
                state.getDatabase().getEnvironment(),
                oldValues,
                getDifferences()));
    }

    @Override
    public String getLabel() {
        Object object = recreate();

        if (object != null) {
            return State.getInstance(object).getLabel();

        } else {
            return super.getLabel();
        }
    }

    @FieldInternalNamePrefix("cms.draft.name.")
    public static class NameData extends Modification<Object> {

        private Integer index;

        public Integer getIndex() {
            return index;
        }

        public void setIndex(Integer index) {
            this.index = index;
        }
    }
}
