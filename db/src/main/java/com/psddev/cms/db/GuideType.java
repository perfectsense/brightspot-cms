package com.psddev.cms.db;

import com.psddev.dari.db.CachingDatabase;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.ReferentialText;
import com.psddev.dari.db.State;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Production Guide class to hold information about Content types and their associated fields
 *
 */
@Record.LabelFields({ "documentedType/name" })
@Record.BootstrapPackages("Production Guides")
public class GuideType extends Record {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(GuideType.class);

    @ToolUi.Note("Content type for Production Guide information. After selecting, click Save to automatically generate field entries")
    @Required
    @Indexed
    ObjectType documentedType;

    @ToolUi.Note("Production Guide information about this content type")
    @ToolUi.Hidden
    // No plan for this yet - may not be needed
    ReferentialText description;

    @ToolUi.Note("Production Guide field descriptions for this content type (Note that any fields within an embedded type should be defined separately in a Guide for the embedded type)")
    private List<GuideField> fieldDescriptions;

    public ObjectType getDocumentedType() {
        return documentedType;
    }

    public void setDocumentedType(ObjectType documentedType) {
        this.documentedType = documentedType;
    }

    public ReferentialText getDescription() {
        return description;
    }

    public void setDescription(ReferentialText description) {
        this.description = description;
    }

    public List<GuideField> getFieldDescriptions() {
        return fieldDescriptions;
    }

    /*
     * Add a field description entry. Assumes that entry doesn't already exist.
     */
    public void addFieldDescription(GuideField fieldDescription) {
        if (fieldDescriptions == null) {
            fieldDescriptions = new ArrayList<GuideField>();
        }
        fieldDescriptions.add(fieldDescription);
    }

    public void setFieldDescriptions(List<GuideField> fieldDescriptions) {
        this.fieldDescriptions = fieldDescriptions;
    }

    public ReferentialText getFieldDescription(String fieldName, String fieldDisplayName,
            boolean createIfMissing) {
        ReferentialText desc = null;
        if (fieldDescriptions != null) {
            for (GuideField gf : fieldDescriptions) {
                if (gf.getFieldName().equals(fieldName)) {
                    // we take this opportunity to synch the display name
                    if (createIfMissing && fieldDisplayName != null) {
                        gf.setDisplayName(fieldDisplayName);
                    }
                    return gf.getDescription();
                }
            }
        }
        if (createIfMissing) {
            setFieldDescription(fieldName, fieldDisplayName, null);
        }
        return desc;
    }

    public GuideField getGuideField(String fieldName) {
        if (fieldDescriptions != null) {
            for (GuideField gf : fieldDescriptions) {
                if (gf.getFieldName().equals(fieldName)) {
                    return gf;
                }
            }
        }
        return null;
    }

    public void setFieldDescription(String fieldName, String fieldDisplayName,
            ReferentialText description) {
        ReferentialText desc = null;
        if (fieldDescriptions != null) {
            for (GuideField gf : fieldDescriptions) {
                if (gf.getFieldName().equals(fieldName)) {
                    if (fieldDisplayName != null) {
                        gf.setDisplayName(fieldDisplayName);
                    }
                    gf.setDescription(description);
                    return;
                }
            }
        }
        // if didn't already exist
        GuideField gf = new GuideField();
        gf.setFieldName(fieldName);
        if (fieldDisplayName != null) {
            gf.setDisplayName(fieldDisplayName);
        }
        gf.setDescription(description);
        addFieldDescription(gf);

    }

    public void generateFieldDescriptionList() {
        // Create an entry for each field
        ObjectType type = getDocumentedType();
        if (type != null) {
            List<ObjectField> fields = type.getFields();
            for (ObjectField field : fields) {
                getFieldDescription(field.getInternalName(), field.getDisplayName(), true);
            }
            // Now identify any potentially out of date fields
            if (this.getFieldDescriptions() != null && !this.getFieldDescriptions().isEmpty()) {
                for (GuideField fieldDescription : this.getFieldDescriptions()) {
                    if (fieldDescription.getFieldName() != null && !fieldDescription.getFieldName().isEmpty()) {
                        if (type.getField(fieldDescription.getFieldName()) == null) {
                            fieldDescription.setDisplayName(fieldDescription.getFieldName() + " - INTERNAL FIELDNAME NO LONGER FOUND IN OBJECT TYPE. EVALUATE FOR CONTENT TRANSFER/REMOVAL.");
                        }
                    }
                }
            }
        }

    }

    /*
     * (non-Javadoc)
     *
     * @see com.psddev.dari.db.Record#beforeSave()
     */
    public void beforeSave() {
        generateFieldDescriptionList();
    }

    @Record.Embedded
    @Record.LabelFields({ "displayName" })
    public static class GuideField extends Record {

        @Required
        @Indexed
        @ToolUi.Note("Internal fieldname in this object type. This is used to query the object and must exactly match the internal name. Extreme care should be taken if changing.")
        String fieldName;

        @ToolUi.Note("Displayed fieldname (note that changing this value does not change the value on the edit form)")
        String displayName;

        @ToolUi.Note("Production Guide information about this field")
        ReferentialText description;

        public String getFieldName() {
            return fieldName;
        }

        public void setFieldName(String fieldName) {
            this.fieldName = fieldName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public ReferentialText getDescription() {
            return description;
        }

        public void setDescription(ReferentialText description) {
            this.description = description;
        }

    }

    public static final class Static {

        public static ReferentialText getFieldDescription(State state,
                String fieldName) {
            if (state != null) {
                ObjectType typeDefinition = state.getType();
                GuideType guide = getGuideType(typeDefinition);
                if (guide != null) {
                    return guide.getFieldDescription(fieldName, null, false);
                }
            }
            return null;
        }

        /**
         * Retrieve the field's minimum value for Production Guide display. For
         * these purposes, we suppress some of the "less-useful" max/min values
         * as they can be confusing to the editors
         *
         */
        public static Object getFieldMinimumValue(ObjectField field) {
            Object minVal = field.getMinimum();
            if (minVal != null) {
                Class javaTypeClass = minVal.getClass();
                if (javaTypeClass.equals(long.class)
                        || javaTypeClass.equals(Long.class)
                        && (Long) minVal == Long.MIN_VALUE) {
                    return null;
                }
                if (javaTypeClass.equals(int.class)
                        || javaTypeClass.equals(Integer.class)
                        && (Integer) minVal == Integer.MIN_VALUE) {
                    return null;
                }
                if (javaTypeClass.equals(short.class)
                        || javaTypeClass.equals(Short.class)
                        && (Short) minVal == Short.MIN_VALUE) {
                    return null;
                }
                if (javaTypeClass.equals(byte.class)
                        || javaTypeClass.equals(Byte.class)
                        && (Byte) minVal == Byte.MIN_VALUE) {
                    return null;
                }
            }
            return minVal;
        }

        /**
         * Retrieve the field's maximum value for Production Guide display. For
         * these purposes, we suppress some of the "less-useful" max/min values
         * as they can be confusing to the editors
         *
         */
        public static Object getFieldMaximumValue(ObjectField field) {
            Object maxVal = field.getMaximum();
            if (maxVal != null) {
                Class javaTypeClass = maxVal.getClass();
                if (javaTypeClass.equals(long.class)
                        || javaTypeClass.equals(Long.class)
                        && (Long) maxVal == Long.MAX_VALUE) {
                    return null;
                }
                if (javaTypeClass.equals(int.class)
                        || javaTypeClass.equals(Integer.class)
                        && (Integer) maxVal == Integer.MAX_VALUE) {
                    return null;
                }
                if (javaTypeClass.equals(short.class)
                        || javaTypeClass.equals(Short.class)
                        && (Short) maxVal == Short.MAX_VALUE) {
                    return null;
                }
                if (javaTypeClass.equals(byte.class)
                        || javaTypeClass.equals(Byte.class)
                        && (Byte) maxVal == Byte.MAX_VALUE) {
                    return null;
                }
            }
            return maxVal;
        }

        /**
         * Query to get a T/F as to whether the given {@code fieldName} has any
         * information we include in the field description (e.g. used to
         * determine whether ? link is displayed in UI
         */
        public static boolean hasFieldGuideInfo(State state, String fieldName) {
            ObjectField field = state.getField(fieldName);
            if (field.isRequired()) {
                return true;
            } else if (getFieldMaximumValue(field) != null) {
                return true;
            } else if (getFieldMinimumValue(field) != null) {
                return true;
            }
            ReferentialText desc = getFieldDescription(state, fieldName);
            if (desc != null && !desc.isEmpty()) {
                return true;
            }

            return false;
        }

        /**
         * Retrieve the existing GuideType instance for a given {@code objectType}.
         * If none exists, null is returned
         */
        public static GuideType getGuideType(ObjectType objectType) {
            return Query.from(GuideType.class)
                    .where("documentedType = ?", objectType.getId()).first();
        }

        /**
         * Retrieve a GuideType instance for the parent type of a given {@code field}, creating one if it
         * doesn't already exist.
         */
        public static GuideType findOrCreateGuide(ObjectField field) {
            GuideType guide = Query.from(GuideType.class)
                    .where("documentedType = ?", field.getParentType().getId()).first();
            if (guide == null) {
                guide = createGuide(field.getParentType());
            }
            return guide;
        }

        /**
         * Retrieve a GuideType instance for the given {@code documentedType}, creating one if it
         * doesn't already exist.
         */
        public static GuideType findOrCreateGuide(ObjectType documentedType) {
            GuideType guide = Query.from(GuideType.class)
                    .where("documentedType = ?", documentedType.getId()).first();
            if (guide == null) {
                guide = createGuide(documentedType);
            }
            return guide;
        }

        /**
         * Create a GuideType instance for the given {@code documentedType}. To allow for thread/transaction safety, this
         * is synchronized first queries to ensure it hasn't already been created.
         */
        public static synchronized GuideType createGuide(
                ObjectType documentedType) {
            Query<GuideType> query = Query.from(GuideType.class)
                    .where("documentedType = ?", documentedType.getId());
            query.as(CachingDatabase.QueryOptions.class).setDisabled(true);
            GuideType guide = query.first();
            if (guide == null) {
                LOGGER.info("Creating a production guide instance for type: " + documentedType);
                guide = new GuideType();
                guide.setDocumentedType(documentedType);
                guide.saveImmediately();
            }
            return guide;
        }

        public static void setDescription(ObjectField field,
                ReferentialText descText) {
            GuideType guide = Static.findOrCreateGuide(field);
            guide.setFieldDescription(field.getInternalName(), field.getDisplayName(), descText);
            guide.saveImmediately();
        }

        /**
         * Generate a GuideType instance for every content type usable in the
         * templates
         */
        public static void createDefaultTypeGuides() {
            List<Template> templates = Query.from(Template.class).selectAll();
            for (Template template : templates) {
                for (ObjectType t : template.getContentTypes()) {
                    findOrCreateGuide(t);
                    // Create guides for the types referenced within this type
                    if (t.getFields() != null) {
                        for (ObjectField field : t.getFields()) {
                            Set<ObjectType> types = field.getTypes();
                            if (types != null) {
                                for (ObjectType type : types) {
                                    if (type.getFields() != null && !type.getFields().isEmpty()) {
                                        findOrCreateGuide(type);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
