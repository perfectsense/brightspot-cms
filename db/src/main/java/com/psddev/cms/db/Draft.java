package com.psddev.cms.db;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.psddev.dari.db.Database;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ErrorUtils;
import com.psddev.dari.util.ObjectUtils;

/** Unpublished object or unsaved changes to an existing object. */
@Draft.DisplayName("Content Update")
@ToolUi.Hidden
public class Draft extends Content {

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

    private Map<String, Object> objectChanges;

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
     */
    public Map<String, Object> getObjectChanges() {
        if (objectChanges == null) {
            objectChanges = new LinkedHashMap<String, Object>();
        }
        return objectChanges;
    }

    /**
     * Sets the map of all the values to be changed on the originating
     * object.
     */
    public void setObjectChanges(Map<String, Object> values) {
        this.objectChanges = values;
    }

    /**
     * Returns a copy of the originating object with all the changes
     * applied.
     *
     * @return {@code null} if {@code objectType} isn't set.
     */
    public Object getObject() {
        ObjectType type = getObjectType();

        if (type == null) {
            return null;
        }

        UUID id = getObjectId();
        Object object = Query.fromAll().
                where("_id = ?", id).
                using(getState().getDatabase()).
                master().
                noCache().
                resolveInvisible().
                first();

        if (object == null) {
            object = type.createObject(id);
        }

        State.getInstance(object).putAll(getObjectChanges());
        return object;
    }

    /**
     * Sets all the field values based on the given {@code object}.
     *
     * @param object Can't be {@code null}.
     */
    public void setObject(Object object) {
        ErrorUtils.errorIfNull(object, "object");

        State newState = State.getInstance(object);
        Database db = newState.getRealDatabase();

        getState().setDatabase(db);
        setObjectType(newState.getType());
        setObjectId(newState.getId());

        Object oldObject = Query.from(Object.class).where("_id = ?", object).using(db).noCache().first();
        Map<String, Object> newValues = newState.getSimpleValues();

        if (oldObject != null) {
            Map<String, Object> oldValues = State.getInstance(oldObject).getSimpleValues();
            Set<String> keys = new HashSet<String>();

            keys.addAll(oldValues.keySet());
            keys.addAll(newValues.keySet());

            for (String key : keys) {
                Object newValue = newValues.get(key);

                if (ObjectUtils.equals(oldValues.get(key), newValue)) {
                    newValues.remove(key);

                } else if (newValue == null) {
                    newValues.put(key, null);
                }
            }
        }

        setObjectChanges(newValues);
    }

    @Override
    public String getLabel() {
        Object object = getObject();

        if (object != null) {
            State state = State.getInstance(object);
            ObjectType type = state.getType();
            StringBuilder label = new StringBuilder();

            if (type != null) {
                label.append(type.getLabel());
                label.append(": ");
            }

            label.append(state.getLabel());
            return label.toString();

        } else {
            return super.getLabel();
        }
    }
}
