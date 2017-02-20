package com.psddev.cms.db;

import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Record;
import com.psddev.dari.util.CodeUtils;
import com.psddev.dari.util.Lazy;

import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RichTextElement extends Record {

    private static final Logger LOGGER = LoggerFactory.getLogger(RichTextElement.class);

    private static final Lazy<Map<String, ObjectType>> CONCRETE_TAG_TYPES = new Lazy<Map<String, ObjectType>>() {

        @Override
        protected Map<String, ObjectType> create() throws Exception {
            Map<String, ObjectType> tagTypes = new LinkedHashMap<>();

            ObjectType.getInstance(RichTextElement.class).findConcreteTypes().forEach(type -> {
                String tagName = type.as(ToolUi.class).getRichTextElementTagName();

                if (tagName != null && type.getObjectClass() != null) {
                    ObjectType existingType = tagTypes.putIfAbsent(tagName, type);

                    if (existingType != null) {
                        LOGGER.warn("Ignoring [{}] because its tag name, [{}], conflicts with [{}]",
                                new Object[] {
                                        type.getInternalName(),
                                        tagName,
                                        existingType.getInternalName()
                                });
                    }
                }
            });

            return Collections.unmodifiableMap(tagTypes);
        }
    };

    static {
        CodeUtils.addRedefineClassesListener(classes -> CONCRETE_TAG_TYPES.reset());
    }

    /**
     * Returns all concrete types that extend {@link RichTextElement}, keyed
     * by their {@linkplain Tag#value() tag names}.
     *
     * @return Nonnull. Immutable.
     */
    public static Map<String, ObjectType> getConcreteTagTypes() {
        return CONCRETE_TAG_TYPES.get();
    }

    public abstract void fromAttributes(Map<String, String> attributes);

    public void fromBody(String body) {
    }

    public abstract Map<String, String> toAttributes();

    public String toBody() {
        return null;
    }

    public boolean shouldCloseOnSave() {
        return true;
    }

    public void writePreviewHtml(ToolPageContext page) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Documented
    @ObjectType.AnnotationProcessorClass(TagProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Tag {

        String value();
        String initialBody() default "";
        boolean block() default false;
        boolean preview() default false;
        boolean readOnly() default false;
        boolean root() default false;
        Class<?>[] children() default { };
        String menu() default "";
        String tooltip() default "";
        String[] keymaps() default { };
        double position() default 0d;
    }

    private static class TagProcessor implements ObjectType.AnnotationProcessor<Tag> {

        @Override
        public void process(ObjectType type, Tag annotation) {
            type.as(ToolUi.class).setRichTextElementTagName(annotation.value());
        }
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Exclusive {

    }

    @Documented
    @ObjectField.AnnotationProcessorClass(TagsProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Tags {

        Class<?>[] value();
    }

    private static class TagsProcessor implements ObjectField.AnnotationProcessor<Tags> {

        @Override
        public void process(ObjectType type, ObjectField field, Tags annotation) {
            field.as(ToolUi.class).setRichTextElementClassNames(
                    Stream.of(annotation.value())
                            .map(Class::getName)
                            .collect(Collectors.toCollection(LinkedHashSet::new)));
        }
    }
}
