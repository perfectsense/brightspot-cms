package com.psddev.cms.db;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.psddev.dari.db.DatabaseEnvironment;
import com.psddev.dari.db.Modification;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectMethod;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Reference;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;
import com.psddev.dari.util.TypeDefinition;

/** Controls the tool UI display. */
@ToolUi.FieldInternalNamePrefix("cms.ui.")
@Modification.Classes({ ObjectField.class, ObjectType.class })
public class ToolUi extends Modification<Object> {

    private Boolean bulkUpload;
    private String codeType;
    private Boolean colorPicker;
    private String cssClass;
    private Set<String> displayAfter;
    private Set<String> displayBefore;
    private boolean displayFirst;
    private boolean displayGlobalFilters;
    private Boolean displayGrid;
    private boolean displayLast;
    private boolean dropDown;
    private boolean dropDownSortDescending;
    private String dropDownSortField;
    private Boolean expanded;
    private List<String> fieldDisplayOrder;
    private Boolean filterable;
    private boolean globalFilter;
    private String heading;
    private Boolean hidden;
    private String iconName;
    private String inputProcessorApplication;
    private String inputProcessorPath;
    private String inputSearcherPath;
    private String storagePreviewProcessorApplication;
    private String storagePreviewProcessorPath;
    private String languageTag;
    private ToolUiLayoutElement layoutField;
    private List<ToolUiLayoutElement> layoutPlaceholders;
    private String noteHtml;
    private String noteRendererClassName;
    private String placeholder;
    private String placeholderDynamicText;
    private Boolean placeholderEditable;
    private String publishButtonText;
    private Boolean referenceable;
    private String referenceableViaClassName;
    private Boolean readOnly;
    private boolean richText;
    private boolean secret;
    private Boolean sortable;
    private Set<String> standardImageSizes;
    private Boolean suggestions;
    private Number suggestedMaximum;
    private Number suggestedMinimum;
    private String tab;
    private String storageSetting;
    private String defaultSortField;

    public boolean isBulkUpload() {
        return Boolean.TRUE.equals(bulkUpload);
    }

    public void setBulkUpload(boolean bulkUpload) {
        this.bulkUpload = bulkUpload ? Boolean.TRUE : null;
    }

    public String getCodeType() {
        return codeType;
    }

    public void setCodeType(String codeType) {
        this.codeType = codeType;
    }

    public boolean isColorPicker() {
        return Boolean.TRUE.equals(colorPicker);
    }

    public void setColorPicker(boolean colorPicker) {
        this.colorPicker = colorPicker ? Boolean.TRUE : null;
    }

    public String getCssClass() {
        return cssClass;
    }

    public void setCssClass(String cssClass) {
        this.cssClass = cssClass;
    }

    public Set<String> getDisplayAfter() {
        if (displayAfter == null) {
            displayAfter = new LinkedHashSet<>();
        }
        return displayAfter;
    }

    public void setDisplayAfter(Set<String> displayAfter) {
        this.displayAfter = displayAfter;
    }

    public Set<String> getDisplayBefore() {
        if (displayBefore == null) {
            displayBefore = new LinkedHashSet<>();
        }
        return displayBefore;
    }

    public void setDisplayBefore(Set<String> displayBefore) {
        this.displayBefore = displayBefore;
    }

    public boolean isDisplayFirst() {
        return displayFirst;
    }

    public void setDisplayFirst(boolean displayFirst) {
        this.displayFirst = displayFirst;
    }

    public boolean isDisplayGlobalFilters() {
        return displayGlobalFilters;
    }

    public void setDisplayGlobalFilters(boolean displayGlobalFilters) {
        this.displayGlobalFilters = displayGlobalFilters;
    }

    public Boolean getDisplayGrid() {
        return displayGrid;
    }

    public void setDisplayGrid(boolean displayGrid) {
        this.displayGrid = displayGrid;
    }

    public boolean isDisplayGrid() {
        Boolean displayGrid = getDisplayGrid();

        if (displayGrid != null) {
            return displayGrid;
        }

        Object object = getOriginalObject();

        if (!(object instanceof ObjectField)) {
            return false;
        }

        ObjectField field = (ObjectField) object;

        final List<ObjectType> validTypes = field.as(ToolUi.class).findDisplayTypes();

        if (isValueExternal(field)) {

            // all display types must be previewable
            for (ObjectType displayType : field.as(ToolUi.class).findDisplayTypes()) {
                if (ObjectUtils.isBlank(displayType.getPreviewField())) {
                    return false;
                }
            }

            return true;
        } else {

            // always show grid view by default for types with bulkUpload capability
            Set<ObjectType> bulkUploadTypes = new HashSet<ObjectType>();

            for (ObjectType t : validTypes) {
                for (ObjectField f : t.getFields()) {
                    if (f.as(ToolUi.class).isBulkUpload()) {
                        for (ObjectType ft : f.getTypes()) {
                            bulkUploadTypes.add(ft);
                        }
                    }
                }
            }

            return !bulkUploadTypes.isEmpty();
        }
    }

    public boolean isDisplayLast() {
        return displayLast;
    }

    public void setDisplayLast(boolean displayLast) {
        this.displayLast = displayLast;
    }

    public boolean isDropDown() {
        return dropDown;
    }

    public void setDropDown(boolean dropDown) {
        this.dropDown = dropDown;
    }

    public boolean isDropDownSortDescending() {
        return dropDownSortDescending;
    }

    public void setDropDownSortDescending(boolean dropDownSortDescending) {
        this.dropDownSortDescending = dropDownSortDescending;
    }

    public String getDropDownSortField() {
        return dropDownSortField;
    }

    public void setDropDownSortField(String dropDownSortField) {
        this.dropDownSortField = dropDownSortField;
    }

    public boolean isExpanded() {
        return Boolean.TRUE.equals(expanded);
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded ? Boolean.TRUE : null;
    }

    public List<String> getFieldDisplayOrder() {
        if (fieldDisplayOrder == null) {
            fieldDisplayOrder = new ArrayList<>();
        }
        return fieldDisplayOrder;
    }

    public void setFieldDisplayOrder(List<String> fieldDisplayOrder) {
        this.fieldDisplayOrder = fieldDisplayOrder;
    }

    public Boolean getFilterable() {
        return filterable;
    }

    public void setFilterable(Boolean filterable) {
        this.filterable = filterable;
    }

    public boolean isEffectivelyFilterable() {
        Boolean filterable = getFilterable();

        if (filterable != null) {
            return filterable;
        }

        Object object = getOriginalObject();

        if (!(object instanceof ObjectField)) {
            return false;
        }

        ObjectField field = (ObjectField) object;

        if (field.isDeprecated()
                || isHidden()
                || !ObjectField.RECORD_TYPE.equals(field.getInternalItemType())
                || field.isEmbedded()) {
            return false;
        }

        for (ObjectType type : field.getTypes()) {
            if (type.isEmbedded()) {
                return false;
            }
        }

        return true;
    }

    public boolean isGlobalFilter() {
        return globalFilter;
    }

    public void setGlobalFilter(boolean globalFilter) {
        this.globalFilter = globalFilter;
    }

    /** Returns the heading to display before this object. */
    public String getHeading() {
        return heading;
    }

    /** Sets the heading to display before this object. */
    public void setHeading(String heading) {
        this.heading = heading;
    }

    public boolean isHidden() {
        if (hidden == null) {
            hidden = ObjectUtils.to(Boolean.class, getState().get("cms.ui.isHidden"));
        }
        return hidden != null ? hidden : false;
    }

    public Boolean getHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public String getIconName() {
        return iconName;
    }

    public String getIconNameOrDefault(String defaultIconName) {
        String iconName = getIconName();
        return ObjectUtils.isBlank(iconName) ? defaultIconName : iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    public String getInputProcessorApplication() {
        return inputProcessorApplication;
    }

    public void setInputProcessorApplication(String inputProcessorApplication) {
        this.inputProcessorApplication = inputProcessorApplication;
    }

    public String getInputProcessorPath() {
        return inputProcessorPath;
    }

    public void setInputProcessorPath(String inputProcessorPath) {
        this.inputProcessorPath = inputProcessorPath;
    }

    public String getInputSearcherPath() {
        return inputSearcherPath;
    }

    public void setInputSearcherPath(String inputSearcherPath) {
        this.inputSearcherPath = inputSearcherPath;
    }

    public String getStoragePreviewProcessorPath() {
        return storagePreviewProcessorPath;
    }

    public void setStoragePreviewProcessorPath(String storagePreviewProcessorPath) {
        this.storagePreviewProcessorPath = storagePreviewProcessorPath;
    }

    public String getStoragePreviewProcessorApplication() {
        return storagePreviewProcessorApplication;
    }

    public void setStoragePreviewProcessorApplication(String storagePreviewProcessorApplication) {
        this.storagePreviewProcessorApplication = storagePreviewProcessorApplication;
    }

    public String getLanguageTag() {
        return languageTag;
    }

    public void setLanguageTag(String languageTag) {
        this.languageTag = languageTag;
    }

    public ToolUiLayoutElement getLayoutField() {
        return layoutField;
    }

    public void setLayoutField(ToolUiLayoutElement layoutField) {
        this.layoutField = layoutField;
    }

    public List<ToolUiLayoutElement> getLayoutPlaceholders() {
        if (layoutPlaceholders == null) {
            layoutPlaceholders = new ArrayList<ToolUiLayoutElement>();
        }
        return layoutPlaceholders;
    }

    public void setLayoutPlaceholders(List<ToolUiLayoutElement> layoutPlaceholders) {
        this.layoutPlaceholders = layoutPlaceholders;
    }

    public String getNoteHtml() {
        if (noteHtml == null) {
            setNoteHtml(StringUtils.escapeHtml(ObjectUtils.to(String.class, getState().get("cms.ui.note"))));
        }
        return noteHtml;
    }

    public void setNoteHtml(String noteHtml) {
        this.noteHtml = noteHtml;
    }

    @SuppressWarnings("unchecked")
    public Class<? extends NoteRenderer> getNoteRendererClass() {
        Class<?> c = ObjectUtils.getClassByName(noteRendererClassName);
        return c != null && NoteRenderer.class.isAssignableFrom(c) ? (Class<? extends NoteRenderer>) c : null;
    }

    public void setNoteRendererClass(Class<? extends NoteRenderer> noteRendererClass) {
        this.noteRendererClassName = noteRendererClass != null ? noteRendererClass.getName() : null;
    }

    public String getEffectiveNoteHtml(Object object) {
        Class<? extends NoteRenderer> noteRendererClass = getNoteRendererClass();

        if (noteRendererClass == null) {
            return getNoteHtml();

        } else {
            NoteRenderer renderer = TypeDefinition.getInstance(noteRendererClass).newInstance();
            return renderer.render(object);
        }
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public String getPublishButtonText() {
        return publishButtonText;
    }

    public void setPublishButtonText(String publishButtonText) {
        this.publishButtonText = publishButtonText;
    }

    public boolean isReadOnly() {
        if (readOnly == null) {
            readOnly = ObjectUtils.to(Boolean.class, getState().get("cms.ui.isReadOnly"));
        }
        if (getOriginalObject() instanceof ObjectMethod) {
            return true;
        }
        return readOnly != null ? readOnly : false;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public boolean isPlaceholderEditable() {
        return Boolean.TRUE.equals(placeholderEditable);
    }

    public void setPlaceholderEditable(boolean placeholderEditable) {
        this.placeholderEditable = Boolean.TRUE.equals(placeholderEditable) ? Boolean.TRUE : null;
    }

    public String getPlaceholderDynamicText() {
        return placeholderDynamicText;
    }

    public void setPlaceholderDynamicText(String placeholderDynamicText) {
        this.placeholderDynamicText = placeholderDynamicText;
    }

    public boolean isRichText() {
        return richText;
    }

    public void setRichText(boolean richText) {
        this.richText = richText;
    }

    public boolean isReferenceable() {
        if (referenceable == null) {
            referenceable = ObjectUtils.to(Boolean.class, getState().get("cms.ui.isReferenceable"));
        }
        return referenceable != null ? referenceable : false;
    }

    public void setReferenceable(boolean referenceable) {
        this.referenceable = referenceable;
    }

    @SuppressWarnings("unchecked")
    public Class<? extends Reference> getReferenceableViaClass() {
        Class<?> c = ObjectUtils.getClassByName(referenceableViaClassName);
        return c != null && Reference.class.isAssignableFrom(c) ? (Class<? extends Reference>) c : null;
    }

    public void setReferenceableViaClass(Class<? extends Reference> referenceableViaClass) {
        this.referenceableViaClassName = referenceableViaClass != null ? referenceableViaClass.getName() : null;
    }

    public boolean isSecret() {
        return secret;
    }

    public void setSecret(boolean secret) {
        this.secret = secret;
    }

    public Boolean getSortable() {
        return sortable;
    }

    public void setSortable(Boolean sortable) {
        this.sortable = sortable;
    }

    public boolean isEffectivelySortable() {
        Boolean sortable = getSortable();

        if (sortable != null) {
            return sortable;
        }

        Object object = getOriginalObject();

        if (!(object instanceof ObjectField)) {
            return false;
        }

        ObjectField field = (ObjectField) object;

        if (isHidden()) {
            return false;
        }

        String fieldType = field.getInternalType();

        return ObjectField.DATE_TYPE.equals(fieldType)
                || ObjectField.NUMBER_TYPE.equals(fieldType)
                || ObjectField.TEXT_TYPE.equals(fieldType)
                || field.isMetric();
    }

    public Set<String> getStandardImageSizes() {
        if (standardImageSizes == null) {
            standardImageSizes = new LinkedHashSet<String>();
        }
        return standardImageSizes;
    }

    public void setStandardImageSizes(Set<String> standardImageSizes) {
        this.standardImageSizes = standardImageSizes;
    }

    public Boolean getSuggestions() {
        return suggestions;
    }

    public boolean isEffectivelySuggestions() {
        return !Boolean.FALSE.equals(suggestions);
    }

    public void setSuggestions(Boolean suggestions) {
        this.suggestions = suggestions;
    }

    public Number getSuggestedMaximum() {
        return suggestedMaximum;
    }

    public void setSuggestedMaximum(Number suggestedMaximum) {
        this.suggestedMaximum = suggestedMaximum;
    }

    public Number getSuggestedMinimum() {
        return suggestedMinimum;
    }

    public void setSuggestedMinimum(Number suggestedMinimum) {
        this.suggestedMinimum = suggestedMinimum;
    }

    public String getTab() {
        return tab;
    }

    public void setTab(String tab) {
        this.tab = tab;
    }

    public String getStorageSetting() {
        return storageSetting;
    }

    public void setStorageSetting(String storageSetting) {
        this.storageSetting = storageSetting;
    }

    public String getDefaultSortField() {
        return defaultSortField;
    }

    public void setDefaultSortField(String defaultSortField) {
        this.defaultSortField = defaultSortField;
    }

    /**
     * Finds a list of all concrete types that can be displayed in the
     * context of this type or field.
     *
     * @return Never {@code null}.
     */
    public List<ObjectType> findDisplayTypes() {
        Object object = getOriginalObject();
        List<ObjectType> displayTypes = new ArrayList<ObjectType>();
        Set<ObjectType> concreteTypes;

        if (object instanceof ObjectType) {
            concreteTypes = ((ObjectType) object).findConcreteTypes();

        } else if (object instanceof ObjectField) {
            concreteTypes = ((ObjectField) object).findConcreteTypes();

        } else {
            concreteTypes = null;
        }

        if (concreteTypes != null) {
            for (ObjectType t : concreteTypes) {
                if (!t.as(ToolUi.class).isHidden()) {
                    if (t.getObjectClassName() == null) {
                        displayTypes.add(t);

                    } else if (t.getObjectClass() != null) {
                        displayTypes.add(t);
                    }
                }
            }
        }

        return displayTypes;
    }

    public static boolean isValueExternal(ObjectField field) {

        final List<ObjectType> validTypes = field.as(ToolUi.class).findDisplayTypes();
        boolean isValueExternal = !field.isEmbedded();
        if (isValueExternal && validTypes != null && validTypes.size() > 0) {
            for (ObjectType type : validTypes) {
                if (!type.isEmbedded()) {
                    // value is external if any of the valid types must be externally referenced.
                    return true;
                }
            }

            return false;
        }

        return isValueExternal;
    }

    /**
     * Specifies whether the target field should enable and accept files
     * from the bulk upload feature.
     */
    @Documented
    @ObjectField.AnnotationProcessorClass(BulkUploadProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface BulkUpload {

        boolean value() default true;
    }

    private static class BulkUploadProcessor implements ObjectField.AnnotationProcessor<BulkUpload> {

        @Override
        public void process(ObjectType type, ObjectField field, BulkUpload annotation) {
            field.as(ToolUi.class).setBulkUpload(annotation.value());
        }
    }

    /**
     * Specifies that the target field should be displayed as code of the
     * given type {@code value}.
     */
    @Documented
    @ObjectField.AnnotationProcessorClass(CodeTypeProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.METHOD })
    public @interface CodeType {
        String value();
    }

    private static class CodeTypeProcessor implements ObjectField.AnnotationProcessor<CodeType> {

        @Override
        public void process(ObjectType type, ObjectField field, CodeType annotation) {
            field.as(ToolUi.class).setCodeType(annotation.value());
        }
    }

    /**
     * Specifies whether the target field should display the color picker.
     */
    @Documented
    @ObjectField.AnnotationProcessorClass(ColorPickerProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ColorPicker {

        boolean value() default true;
    }

    private static class ColorPickerProcessor implements ObjectField.AnnotationProcessor<ColorPicker> {

        @Override
        public void process(ObjectType type, ObjectField field, ColorPicker annotation) {
            field.as(ToolUi.class).setColorPicker(annotation.value());
        }
    }

    /**
     * Specifies the CSS class to add to {@code .inputContainer} when
     * displaying the target field.
     */
    @Documented
    @ObjectField.AnnotationProcessorClass(CssClassProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.METHOD })
    public @interface CssClass {
        String value();
    }

    private static class CssClassProcessor implements ObjectField.AnnotationProcessor<CssClass> {

        @Override
        public void process(ObjectType type, ObjectField field, CssClass annotation) {
            field.as(ToolUi.class).setCssClass(annotation.value());
        }
    }

    @Documented
    @ObjectField.AnnotationProcessorClass(DisplayAfterProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.METHOD })
    public @interface DisplayAfter {
        String[] value();
    }

    private static class DisplayAfterProcessor implements ObjectField.AnnotationProcessor<DisplayAfter> {
        @Override
        public void process(ObjectType type, ObjectField field, DisplayAfter annotation) {
            Collections.addAll(field.as(ToolUi.class).getDisplayAfter(), annotation.value());
        }
    }

    @Documented
    @ObjectField.AnnotationProcessorClass(DisplayBeforeProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.METHOD })
    public @interface DisplayBefore {
        String[] value();
    }

    private static class DisplayBeforeProcessor implements ObjectField.AnnotationProcessor<DisplayBefore> {
        @Override
        public void process(ObjectType type, ObjectField field, DisplayBefore annotation) {
            Collections.addAll(field.as(ToolUi.class).getDisplayBefore(), annotation.value());
        }
    }

    /**
     * Specifies that the target field should be displayed before any other
     * fields.
     */
    @Documented
    @ObjectField.AnnotationProcessorClass(DisplayFirstProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.METHOD })
    public @interface DisplayFirst {
        boolean value() default true;
    }

    private static class DisplayFirstProcessor implements ObjectField.AnnotationProcessor<DisplayFirst> {

        @Override
        public void process(ObjectType type, ObjectField field, DisplayFirst annotation) {
            field.as(ToolUi.class).setDisplayFirst(annotation.value());
        }
    }

    /**
     * Specifies that the target type displays global search filters.
     */
    @Documented
    @Inherited
    @ObjectType.AnnotationProcessorClass(DisplayGlobalFiltersProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface DisplayGlobalFilters {

        boolean value() default true;
    }

    private static class DisplayGlobalFiltersProcessor implements ObjectType.AnnotationProcessor<DisplayGlobalFilters> {

        @Override
        public void process(ObjectType type, DisplayGlobalFilters annotation) {
            type.as(ToolUi.class).setDisplayGlobalFilters(annotation.value());
        }
    }

    /**
     * Specifies that the target field should be displayed in a grid layout.
     */
    @Documented
    @ObjectField.AnnotationProcessorClass(DisplayGridProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.METHOD })
    public @interface DisplayGrid {
        boolean value() default true;
    }

    private static class DisplayGridProcessor implements ObjectField.AnnotationProcessor<DisplayGrid> {

        @Override
        public void process(ObjectType type, ObjectField field, DisplayGrid annotation) {
            field.as(ToolUi.class).setDisplayGrid(annotation.value());
        }
    }

    /**
     * Specifies that the target field should be displayed after all other
     * fields.
     */
    @Documented
    @ObjectField.AnnotationProcessorClass(DisplayLastProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.METHOD })
    public @interface DisplayLast {
        boolean value() default true;
    }

    private static class DisplayLastProcessor implements ObjectField.AnnotationProcessor<DisplayLast> {

        @Override
        public void process(ObjectType type, ObjectField field, DisplayLast annotation) {
            field.as(ToolUi.class).setDisplayLast(annotation.value());
        }
    }

    @Documented
    @ObjectType.AnnotationProcessorClass(FieldDisplayOrderProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface FieldDisplayOrder {
        String[] value();
    }

    private static class FieldDisplayOrderProcessor implements ObjectType.AnnotationProcessor<FieldDisplayOrder> {
        @Override
        public void process(ObjectType type, FieldDisplayOrder annotation) {
            Collections.addAll(type.as(ToolUi.class).getFieldDisplayOrder(), annotation.value());
        }
    }

    /**
     * Specifies whether the target field should be displayed as a drop-down
     * menu.
     */
    @Documented
    @ObjectField.AnnotationProcessorClass(DropDownProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.METHOD })
    public @interface DropDown {
        boolean value() default true;
        String sortField() default "";
        boolean sortDescending() default false;
    }

    private static class DropDownProcessor implements ObjectField.AnnotationProcessor<DropDown> {

        @Override
        public void process(ObjectType type, ObjectField field, DropDown annotation) {
            field.as(ToolUi.class).setDropDown(annotation.value());
            field.as(ToolUi.class).setDropDownSortField(StringUtils.isBlank(annotation.sortField()) ? null : annotation.sortField());
            field.as(ToolUi.class).setDropDownSortDescending(annotation.sortDescending());
        }
    }

    /**
     * Specifies whether the target field should always be expanded in an
     * embedded display.
     */
    @Documented
    @ObjectField.AnnotationProcessorClass(ExpandedProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Expanded {

        boolean value() default true;
    }

    private static class ExpandedProcessor implements ObjectField.AnnotationProcessor<Expanded> {

        @Override
        public void process(ObjectType type, ObjectField field, Expanded annotation) {
            field.as(ToolUi.class).setExpanded(annotation.value());
        }
    }

    /**
     * Specifies whether the target field should be offered as a filterable
     * field in search.
     */
    @Documented
    @ObjectField.AnnotationProcessorClass(FilterableProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.METHOD })
    public @interface Filterable {
        boolean value() default true;
    }

    private static class FilterableProcessor implements ObjectField.AnnotationProcessor<Filterable> {

        @Override
        public void process(ObjectType type, ObjectField field, Filterable annotation) {
            field.as(ToolUi.class).setFilterable(annotation.value());
        }
    }

    /**
     * Specifies whether the target type shows up as a filter that can be
     * applied to any types in search.
     */
    @Documented
    @Inherited
    @ObjectType.AnnotationProcessorClass(GlobalFilterProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.TYPE })
    public @interface GlobalFilter {
        boolean value() default true;
    }

    private static class GlobalFilterProcessor implements ObjectType.AnnotationProcessor<GlobalFilter> {
        @Override
        public void process(ObjectType type, GlobalFilter annotation) {
            type.as(ToolUi.class).setGlobalFilter(annotation.value());
        }
    }

    /** Specifies the text to display before the target field. */
    @Documented
    @Inherited
    @ObjectField.AnnotationProcessorClass(HeadingProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.METHOD })
    public @interface Heading {
        String value();
    }

    private static class HeadingProcessor implements ObjectField.AnnotationProcessor<Heading> {
        @Override
        public void process(ObjectType type, ObjectField field, Heading annotation) {
            field.as(ToolUi.class).setHeading(annotation.value());
        }
    }

    /** Specifies whether the target is hidden in the UI. */
    @Documented
    @Inherited
    @ObjectField.AnnotationProcessorClass(HiddenProcessor.class)
    @ObjectType.AnnotationProcessorClass(HiddenProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.TYPE, ElementType.METHOD })
    public @interface Hidden {
        boolean value() default true;
    }

    private static class HiddenProcessor implements
            ObjectField.AnnotationProcessor<Hidden>,
            ObjectType.AnnotationProcessor<Hidden> {

        @Override
        public void process(ObjectType type, ObjectField field, Hidden annotation) {
            field.as(ToolUi.class).setHidden(annotation.value());
        }

        @Override
        public void process(ObjectType type, Hidden annotation) {
            type.as(ToolUi.class).setHidden(annotation.value());
        }
    }

    /** Specifies the name of the icon that represents the target type. */
    @Documented
    @Inherited
    @ObjectType.AnnotationProcessorClass(IconNameProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface IconName {
        String value();
    }

    private static class IconNameProcessor implements ObjectType.AnnotationProcessor<IconName> {
        @Override
        public void process(ObjectType type, IconName annotation) {
            type.as(ToolUi.class).setIconName(annotation.value());
        }
    }

    /**
     * Specifies the path to the processor used to render and update
     * the target field.
     */
    @Documented
    @Inherited
    @ObjectField.AnnotationProcessorClass(InputProcessorPathProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.METHOD })
    public @interface InputProcessorPath {
        String application() default "";
        String value();
    }

    private static class InputProcessorPathProcessor implements ObjectField.AnnotationProcessor<InputProcessorPath> {
        @Override
        public void process(ObjectType type, ObjectField field, InputProcessorPath annotation) {
            field.as(ToolUi.class).setInputProcessorApplication(annotation.application());
            field.as(ToolUi.class).setInputProcessorPath(annotation.value());
        }
    }

    /**
     * Specifies the path to the processor used to render previews of StorageItems fields.
     */
    @Documented
    @Inherited
    @ObjectField.AnnotationProcessorClass(StoragePreviewProcessorPathProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.METHOD })
    public @interface StoragePreviewProcessorPath {
        String application() default "";
        String value();
    }

    private static class StoragePreviewProcessorPathProcessor implements ObjectField.AnnotationProcessor<StoragePreviewProcessorPath> {
        @Override
        public void process(ObjectType type, ObjectField field, StoragePreviewProcessorPath annotation) {
            field.as(ToolUi.class).setStoragePreviewProcessorApplication(annotation.application());
            field.as(ToolUi.class).setStoragePreviewProcessorPath(annotation.value());
        }
    }

    /**
     * Specifies the path to the searcher used to find a value for
     * the target field.
     */
    @Documented
    @Inherited
    @ObjectField.AnnotationProcessorClass(InputSearcherPathProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface InputSearcherPath {
        String value();
    }

    private static class InputSearcherPathProcessor implements ObjectField.AnnotationProcessor<InputSearcherPath> {
        @Override
        public void process(ObjectType type, ObjectField field, InputSearcherPath annotation) {
            field.as(ToolUi.class).setInputSearcherPath(annotation.value());
        }
    }

    /**
     * Specifies the language of the text in the target type or field.
     */
    @Documented
    @Inherited
    @ObjectField.AnnotationProcessorClass(LanguageTagProcessor.class)
    @ObjectType.AnnotationProcessorClass(LanguageTagProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.TYPE })
    public @interface LanguageTag {

        public String value();
    }

    private static class LanguageTagProcessor implements ObjectField.AnnotationProcessor<LanguageTag>, ObjectType.AnnotationProcessor<LanguageTag> {

        @Override
        public void process(ObjectType type, ObjectField field, LanguageTag annotation) {
            field.as(ToolUi.class).setLanguageTag(annotation.value());
        }

        @Override
        public void process(ObjectType type, LanguageTag annotation) {
            type.as(ToolUi.class).setLanguageTag(annotation.value());
        }
    }

    @Documented
    @ObjectField.AnnotationProcessorClass(LayoutFieldProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface LayoutField {

        public int left();
        public int top();
        public int width();
        public int height();
        public String dynamicText() default "";
    }

    private static class LayoutFieldProcessor implements ObjectField.AnnotationProcessor<LayoutField> {

        @Override
        public void process(ObjectType type, ObjectField field, LayoutField annotation) {
            ToolUiLayoutElement element = new ToolUiLayoutElement();

            element.setLeft(annotation.left());
            element.setTop(annotation.top());
            element.setWidth(annotation.width());
            element.setHeight(annotation.height());
            element.setDynamicText(annotation.dynamicText());

            field.as(ToolUi.class).setLayoutField(element);
        }
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    public @interface LayoutPlaceholder {

        public String name();
        public int left();
        public int top();
        public int width();
        public int height();
        public String dynamicText() default "";
    }

    @Documented
    @Inherited
    @ObjectType.AnnotationProcessorClass(LayoutPlaceholdersProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface LayoutPlaceholders {

        public LayoutPlaceholder[] value();
    }

    private static class LayoutPlaceholdersProcessor implements ObjectType.AnnotationProcessor<LayoutPlaceholders> {

        @Override
        public void process(ObjectType type, LayoutPlaceholders annotation) {
            for (LayoutPlaceholder placeholder : annotation.value()) {
                ToolUiLayoutElement element = new ToolUiLayoutElement();

                element.setName(placeholder.name());
                element.setLeft(placeholder.left());
                element.setTop(placeholder.top());
                element.setWidth(placeholder.width());
                element.setHeight(placeholder.height());
                element.setDynamicText(placeholder.dynamicText());

                type.as(ToolUi.class).getLayoutPlaceholders().add(element);
            }
        }
    }

    /** Specifies the note displayed along with the target in the UI. */
    @Documented
    @Inherited
    @ObjectField.AnnotationProcessorClass(NoteProcessor.class)
    @ObjectType.AnnotationProcessorClass(NoteProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.TYPE, ElementType.METHOD })
    public @interface Note {
        String value();
    }

    private static class NoteProcessor implements
            ObjectField.AnnotationProcessor<Note>,
            ObjectType.AnnotationProcessor<Note> {

        @Override
        public void process(ObjectType type, ObjectField field, Note annotation) {
            field.as(ToolUi.class).setNoteHtml(StringUtils.escapeHtml(annotation.value()));
        }

        @Override
        public void process(ObjectType type, Note annotation) {
            type.as(ToolUi.class).setNoteHtml(StringUtils.escapeHtml(annotation.value()));
        }
    }

    /** Renders the note displayed along with a type or a field. */
    public static interface NoteRenderer {

        /** Renders the note for the given {@code object}. */
        public String render(Object object);
    }

    /**
     * Specifies the class that can render the note displayed along with
     * the target in the UI.
     */
    @Documented
    @Inherited
    @ObjectField.AnnotationProcessorClass(NoteRendererClassProcessor.class)
    @ObjectType.AnnotationProcessorClass(NoteRendererClassProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.TYPE, ElementType.METHOD })
    public @interface NoteRendererClass {
        Class<? extends NoteRenderer> value();
    }

    private static class NoteRendererClassProcessor implements
            ObjectField.AnnotationProcessor<NoteRendererClass>,
            ObjectType.AnnotationProcessor<NoteRendererClass> {

        @Override
        public void process(ObjectType type, ObjectField field, NoteRendererClass annotation) {
            field.as(ToolUi.class).setNoteRendererClass(annotation.value());
        }

        @Override
        public void process(ObjectType type, NoteRendererClass annotation) {
            type.as(ToolUi.class).setNoteRendererClass(annotation.value());
        }
    }

    /**
     * Specifies the note, in raw HTML, displayed along with the target
     * in the UI.
     */
    @Documented
    @Inherited
    @ObjectField.AnnotationProcessorClass(NoteHtmlProcessor.class)
    @ObjectType.AnnotationProcessorClass(NoteHtmlProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.TYPE, ElementType.METHOD })
    public @interface NoteHtml {
        String value();
    }

    private static class NoteHtmlProcessor implements
            ObjectField.AnnotationProcessor<NoteHtml>,
            ObjectType.AnnotationProcessor<NoteHtml> {

        @Override
        public void process(ObjectType type, ObjectField field, NoteHtml annotation) {
            field.as(ToolUi.class).setNoteHtml(annotation.value());
        }

        @Override
        public void process(ObjectType type, NoteHtml annotation) {
            type.as(ToolUi.class).setNoteHtml(annotation.value());
        }
    }

    /**
     * Specifies the target field's placeholder text.
     */
    @Documented
    @ObjectField.AnnotationProcessorClass(PlaceholderProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.METHOD })
    public @interface Placeholder {

        /**
         * Static placeholder text.
         */
        String value() default "";

        /**
         * Dynamic placeholder text.
         */
        String dynamicText() default "";

        /**
         * {@code true} if the placeholder text should remain and be editable
         * when the user clicks into the input.
         */
        boolean editable() default false;
    }

    private static class PlaceholderProcessor implements ObjectField.AnnotationProcessor<Annotation> {

        @Override
        public void process(ObjectType type, ObjectField field, Annotation annotation) {
            ToolUi ui = field.as(ToolUi.class);

            if (annotation instanceof FieldPlaceholder) {
                ui.setPlaceholder(((FieldPlaceholder) annotation).value());

            } else {
                Placeholder placeholder = (Placeholder) annotation;

                ui.setPlaceholder(placeholder.value());
                ui.setPlaceholderDynamicText(placeholder.dynamicText());
                ui.setPlaceholderEditable(placeholder.editable());
            }
        }
    }

    /**
     * Specifies the note displayed along with the target in the UI.
     */
    @Documented
    @Inherited
    @ObjectType.AnnotationProcessorClass(PublishButtonTextProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface PublishButtonText {
        String value();
    }

    private static class PublishButtonTextProcessor implements ObjectType.AnnotationProcessor<PublishButtonText> {

        @Override
        public void process(ObjectType objectType, PublishButtonText annotation) {
            objectType.as(ToolUi.class).setPublishButtonText(annotation.value());
        }
    }

    /** Specifies whether the target is read-only. */
    @Documented
    @ObjectField.AnnotationProcessorClass(ReadOnlyProcessor.class)
    @ObjectType.AnnotationProcessorClass(ReadOnlyProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ReadOnly {
        boolean value() default true;
    }

    private static class ReadOnlyProcessor implements
            ObjectField.AnnotationProcessor<ReadOnly>,
            ObjectType.AnnotationProcessor<ReadOnly> {

        @Override
        public void process(ObjectType type, ObjectField field, ReadOnly annotation) {
            field.as(ToolUi.class).setReadOnly(annotation.value());
        }

        @Override
        public void process(ObjectType type, ReadOnly annotation) {
            type.as(ToolUi.class).setReadOnly(annotation.value());
        }
    }

    /**
     * Specifies whether the instances of the target type can be referenced
     * by a {@linkplain com.psddev.dari.db.ReferentialText referential text}
     * object.
     */
    @Documented
    @Inherited
    @ObjectType.AnnotationProcessorClass(ReferenceableProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Referenceable {
        boolean value() default true;
        Class<? extends Reference> via() default Reference.class;
    }

    private static class ReferenceableProcessor implements ObjectType.AnnotationProcessor<Referenceable> {
        @Override
        public void process(ObjectType type, Referenceable annotation) {
            type.as(ToolUi.class).setReferenceable(annotation.value());
            type.as(ToolUi.class).setReferenceableViaClass(annotation.via());
        }
    }

    /**
     * Specifies whether the target field should offer rich-text editing
     * options.
     */
    @Documented
    @ObjectField.AnnotationProcessorClass(RichTextProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.METHOD })
    public @interface RichText {
        boolean value() default true;
    }

    private static class RichTextProcessor implements ObjectField.AnnotationProcessor<RichText> {

        @Override
        public void process(ObjectType type, ObjectField field, RichText annotation) {
            field.as(ToolUi.class).setRichText(annotation.value());
        }
    }

    /**
     * Specifies whether the target field display should be scrambled.
     */
    @Documented
    @ObjectField.AnnotationProcessorClass(SecretProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.METHOD })
    public @interface Secret {
        boolean value() default true;
    }

    private static class SecretProcessor implements ObjectField.AnnotationProcessor<Secret> {

        @Override
        public void process(ObjectType type, ObjectField field, Secret annotation) {
            field.as(ToolUi.class).setSecret(annotation.value());
        }
    }

    /**
     * Specifies whether the target field should be offered as a sortable
     * field in search.
     */
    @Documented
    @ObjectField.AnnotationProcessorClass(SortableProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.METHOD })
    public @interface Sortable {
        boolean value() default true;
    }

    private static class SortableProcessor implements ObjectField.AnnotationProcessor<Sortable> {

        @Override
        public void process(ObjectType type, ObjectField field, Sortable annotation) {
            field.as(ToolUi.class).setSortable(annotation.value());
        }
    }

    /**
     * Specifies the standard image sizes that would be applied to the target
     * field.
     */
    @Documented
    @ObjectField.AnnotationProcessorClass(StandardImageSizesProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface StandardImageSizes {
        String[] value();
    }

    private static class StandardImageSizesProcessor implements ObjectField.AnnotationProcessor<StandardImageSizes> {

        @Override
        public void process(ObjectType type, ObjectField field, StandardImageSizes annotation) {
            Collections.addAll(field.as(ToolUi.class).getStandardImageSizes(), annotation.value());
        }
    }

    /** Specifies whether the target field should offer suggestions. */
    @Documented
    @ObjectField.AnnotationProcessorClass(SuggestionsProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Suggestions {
        boolean value() default true;
    }

    private static class SuggestionsProcessor implements ObjectField.AnnotationProcessor<Suggestions> {

        @Override
        public void process(ObjectType type, ObjectField field, Suggestions annotation) {
            field.as(ToolUi.class).setSuggestions(annotation.value());
        }
    }

    /** Specifies the suggested maximum size of the target field value. */
    @Documented
    @ObjectField.AnnotationProcessorClass(SuggestedMaximumProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface SuggestedMaximum {
        double value();
    }

    private static class SuggestedMaximumProcessor implements ObjectField.AnnotationProcessor<Annotation> {
        @Override
        public void process(ObjectType type, ObjectField field, Annotation annotation) {
            field.as(ToolUi.class).setSuggestedMaximum(annotation instanceof FieldSuggestedMaximum
                    ? ((FieldSuggestedMaximum) annotation).value()
                    : ((SuggestedMaximum) annotation).value());
        }
    }

    /** Specifies the suggested minimum size of the target field value. */
    @Documented
    @ObjectField.AnnotationProcessorClass(SuggestedMinimumProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface SuggestedMinimum {
        double value();
    }

    private static class SuggestedMinimumProcessor implements ObjectField.AnnotationProcessor<Annotation> {
        @Override
        public void process(ObjectType type, ObjectField field, Annotation annotation) {
            field.as(ToolUi.class).setSuggestedMinimum(annotation instanceof FieldSuggestedMinimum
                    ? ((FieldSuggestedMinimum) annotation).value()
                    : ((SuggestedMinimum) annotation).value());
        }
    }

    /**
     * Specifies the tab that the target field belongs to.
     */
    @Documented
    @ObjectField.AnnotationProcessorClass(TabProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.METHOD })
    public @interface Tab {
        String value();
    }

    private static class TabProcessor implements ObjectField.AnnotationProcessor<Tab> {
        @Override
        public void process(ObjectType type, ObjectField field, Tab annotation) {
            field.as(ToolUi.class).setTab(annotation.value());
        }
    }

    // --- Legacy ---

    private static final String FIELD_PREFIX = "cms.ui.";
    private static final String OPTION_PREFIX = "cms.ui.";

    // --- Type annotations ---

    /**
     * Specifies an array of compatible types that the target type may
     * switch to.
     */
    @Documented
    @Inherited
    @ObjectType.AnnotationProcessorClass(CompatibleTypesProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface CompatibleTypes {

        Class<?>[] value();
    }

    private static class CompatibleTypesProcessor implements ObjectType.AnnotationProcessor<CompatibleTypes> {

        @Override
        public void process(ObjectType type, CompatibleTypes annotation) {
            Set<ObjectType> compatibleTypes = new HashSet<ObjectType>();
            DatabaseEnvironment environment = type.getState().getDatabase().getEnvironment();
            for (Class<?> typeClass : annotation.value()) {
                ObjectType compatibleType = environment.getTypeByClass(typeClass);
                if (compatibleType != null) {
                    compatibleTypes.add(compatibleType);
                }
            }
            setCompatibleTypes(type, compatibleTypes);
        }
    }

    private static final String COMPATIBLE_TYPES_FIELD = FIELD_PREFIX + "compatibleTypes";

    /** Returns the set of types that the given {@code type} may switch to. */
    public static Set<ObjectType> getCompatibleTypes(ObjectType type) {
        Set<ObjectType> types = new HashSet<ObjectType>();

        if (type != null) {
            Collection<?> ids = (Collection<?>) type.getState().get(COMPATIBLE_TYPES_FIELD);

            if (!ObjectUtils.isBlank(ids)) {
                DatabaseEnvironment environment = type.getState().getDatabase().getEnvironment();

                for (Object idObject : ids) {
                    ObjectType compatibleType = environment.getTypeById(ObjectUtils.to(UUID.class, idObject));

                    if (compatibleType != null) {
                        types.add(compatibleType);
                    }
                }
            }
        }

        return types;
    }

    /** Sets the set of types that the given {@code type} may switch to. */
    public static void setCompatibleTypes(ObjectType type, Iterable<ObjectType> compatibleTypes) {
        Set<UUID> compatibleTypeIds;
        if (ObjectUtils.isBlank(compatibleTypes)) {
            compatibleTypeIds = null;
        } else {
            compatibleTypeIds = new HashSet<UUID>();
            for (ObjectType compatibleType : compatibleTypes) {
                if (compatibleType != null) {
                    compatibleTypeIds.add(compatibleType.getId());
                }
            }
        }
        type.getState().put(COMPATIBLE_TYPES_FIELD, compatibleTypeIds);
    }

    // --- Field annotations ---

    /** Specifies the internal type used to render the target field. */
    @Documented
    @ObjectField.AnnotationProcessorClass(FieldDisplayTypeProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ ElementType.FIELD, ElementType.METHOD })
    public @interface FieldDisplayType {

        String value();
    }

    private static class FieldDisplayTypeProcessor implements ObjectField.AnnotationProcessor<FieldDisplayType> {

        @Override
        public void process(ObjectType type, ObjectField field, FieldDisplayType annotation) {
            setFieldDisplayType(field, annotation.value());
        }
    }

    private static final String FIELD_INTERNAL_TYPE_OPTION = OPTION_PREFIX + "internalType";

    /** Returns the internal type used to render the given {@code field}. */
    public static String getFieldDisplayType(ObjectField field) {
        return (String) field.getOptions().get(FIELD_INTERNAL_TYPE_OPTION);
    }

    /** Sets the internal type used to render the given {@code field}. */
    public static void setFieldDisplayType(ObjectField field, String type) {
        field.getOptions().put(FIELD_INTERNAL_TYPE_OPTION, type);
    }

    // ---

    /**
     * Specifies whether the values in the target field should be
     * sorted before being saved.
     */
    @Documented
    @ObjectField.AnnotationProcessorClass(FieldSortedProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface FieldSorted {
        boolean value() default true;
    }

    private static class FieldSortedProcessor implements ObjectField.AnnotationProcessor<FieldSorted> {
        @Override
        public void process(ObjectType type, ObjectField field, FieldSorted annotation) {
            setFieldSorted(field, annotation.value());
        }
    }

    private static final String FIELD_SORTED_OPTION = OPTION_PREFIX + "sorted";

    /**
     * Returns {@code true} if the values in the given {@code field}
     * should be sorted before being saved.
     */
    public static boolean isFieldSorted(ObjectField field) {
        return Boolean.TRUE.equals(field.getOptions().get(FIELD_SORTED_OPTION));
    }

    /**
     * Sets whether the values in the given {@code field} should be
     * sorted before being saved.
     */
    public static void setFieldSorted(ObjectField field, boolean isSorted) {
        field.getOptions().put(FIELD_SORTED_OPTION, isSorted);
    }

    // ---

    /**
     * Specifies whether the target field may only contain objects with
     * paths.
     */
    @Documented
    @ObjectField.AnnotationProcessorClass(OnlyPathedProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface OnlyPathed {

        boolean value() default true;
    }

    private static class OnlyPathedProcessor implements ObjectField.AnnotationProcessor<OnlyPathed> {

        @Override
        public void process(ObjectType type, ObjectField field, OnlyPathed annotation) {
            setOnlyPathed(field, annotation.value());
        }
    }

    private static final String IS_ONLY_PATHED_OPTION = OPTION_PREFIX + "isOnlyPathed";

    /**
     * Returns {@code true} if the given {@code field} only allows objects
     * with paths to be stored.
     */
    public static boolean isOnlyPathed(ObjectField field) {
        return Boolean.TRUE.equals(field.getOptions().get(IS_ONLY_PATHED_OPTION));
    }

    /**
     * Sets whether the given {@code field} only allows objects with paths
     * to be stored.
     */
    public static void setOnlyPathed(ObjectField field, boolean isOnlyPathed) {
        Map<String, Object> options = field.getOptions();
        if (isOnlyPathed) {
            options.put(IS_ONLY_PATHED_OPTION, Boolean.TRUE);
        } else {
            options.remove(IS_ONLY_PATHED_OPTION);
        }
    }

    @ObjectField.AnnotationProcessorClass(StorageSettingProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface StorageSetting {
        String value();
    }

    private static class StorageSettingProcessor implements ObjectField.AnnotationProcessor<StorageSetting> {
        @Override
        public void process(ObjectType type, ObjectField field, StorageSetting annotation) {
            field.as(ToolUi.class).setStorageSetting(annotation.value());
        }
    }

    /**
     * Specifies which field should be used as the default sorter.
     */
    @ObjectType.AnnotationProcessorClass(DefaultSortFieldProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface DefaultSortField {
        String value();
    }

    private static class DefaultSortFieldProcessor implements ObjectType.AnnotationProcessor<DefaultSortField> {
        @Override
        public void process(ObjectType type, DefaultSortField annotation) {
            type.as(ToolUi.class).setDefaultSortField(annotation.value());
        }
    }

    // --- Deprecated ---

    /** @deprecated Use {@link #isHidden()} instead. */
    @Deprecated
    public static boolean isHidden(ObjectField field) {
        return field.as(ToolUi.class).isHidden();
    }

    /** @deprecated Use {@link #setHidden(boolean)} instead. */
    @Deprecated
    public static void setHidden(ObjectField field, boolean isHidden) {
        field.as(ToolUi.class).setHidden(isHidden);
    }

    /** @deprecated Use {@link #getNoteHtml} instead. */
    @Deprecated
    public static String getNote(ObjectField field) {
        return ObjectUtils.to(String.class, field.getState().get("cms.ui.note"));
    }

    /** @deprecated Use {@link #setNoteHtml} instead. */
    @Deprecated
    public static void setNote(ObjectField field, String note) {
        field.as(ToolUi.class).setNoteHtml(StringUtils.escapeHtml(note));
    }

    /** @deprecated Use {@link Placeholder} instead. */
    @Deprecated
    @Documented
    @ObjectField.AnnotationProcessorClass(PlaceholderProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface FieldPlaceholder {
        String value();
    }

    /** @deprecated Use {@link #getPlaceholder()} instead. */
    @Deprecated
    public static String getFieldPlaceholder(ObjectField field) {
        return field.as(ToolUi.class).getPlaceholder();
    }

    /** @deprecated Use {@link #setPlaceholder(String)} instead. */
    @Deprecated
    public static void setFieldPlaceholder(ObjectField field, String placeholder) {
        field.as(ToolUi.class).setPlaceholder(placeholder);
    }

    /** @deprecated Use {@link #isReadOnly()} instead. */
    @Deprecated
    public static boolean isReadOnly(ObjectField field) {
        return field.as(ToolUi.class).isReadOnly();
    }

    /** @deprecated Use {@link #setReadOnly(boolean)} instead. */
    @Deprecated
    public static void setReadOnly(ObjectField field, boolean isReadOnly) {
        field.as(ToolUi.class).setReadOnly(isReadOnly);
    }

    /** @deprecated Use {@link #isReferenceable()} instead. */
    @Deprecated
    public static boolean isReferenceable(ObjectType type) {
        return type.as(ToolUi.class).isReferenceable();
    }

    /** @deprecated Use {@link #setReferenceable(boolean)} instead. */
    @Deprecated
    public static void setReferenceable(ObjectType type, boolean isReferenceable) {
        type.as(ToolUi.class).setReferenceable(isReferenceable);
    }

    /** @deprecated Use {@link SuggestedMaximum} instead. */
    @Deprecated
    @Documented
    @ObjectField.AnnotationProcessorClass(SuggestedMaximumProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface FieldSuggestedMaximum {
        double value();
    }

    /** @deprecated Use {@link #getSuggestedMaximum()} instead. */
    @Deprecated
    public static Number getFieldSuggestedMaximum(ObjectField field) {
        return field.as(ToolUi.class).getSuggestedMaximum();
    }

    /** @deprecated Use {@link #setSuggestedMaximum(Number)} instead. */
    @Deprecated
    public static void setFieldSuggestedMaximum(ObjectField field, Number maximum) {
        field.as(ToolUi.class).setSuggestedMaximum(maximum);
    }

    /** @deprecated Use {@link SuggestedMinimum} instead. */
    @Deprecated
    @Documented
    @ObjectField.AnnotationProcessorClass(SuggestedMinimumProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface FieldSuggestedMinimum {
        double value();
    }

    /** @deprecated Use {@link #getSuggestedMinimum()} instead. */
    @Deprecated
    public static Number getFieldSuggestedMinimum(ObjectField field) {
        return field.as(ToolUi.class).getSuggestedMinimum();
    }

    /** @deprecated Use {@link #setSuggestedMinimum(Number)} instead. */
    @Deprecated
    public static void setFieldSuggestedMinimum(ObjectField field, Number minimum) {
        field.as(ToolUi.class).setSuggestedMinimum(minimum);
    }
}
