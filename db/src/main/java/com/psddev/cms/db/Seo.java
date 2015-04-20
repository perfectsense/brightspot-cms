package com.psddev.cms.db;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.google.common.base.Joiner;
import com.psddev.dari.db.Modification;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.db.ReferentialText;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;

/**
 * SEO-related functions for a piece of content.
 */
public final class Seo {

    /**
     * Finds the most appropriate page title for the given {@code object}.
     *
     * @param object If {@code null}, returns {@code null}.
     * @deprecated Use {@link ObjectModification#findTitle} instead.
     */
    @Deprecated
    public static String findTitle(Object object) {
        return Static.findTitle(object);
    }

    /**
     * Finds the most appropriate page description for the given
     * {@code object}.
     *
     * @param object If {@code null}, returns {@code null}.
     * @deprecated Use {@link ObjectModification#findDescription} instead.
     */
    @Deprecated
    public static String findDescription(Object object) {
        return Static.findDescription(object);
    }

    /**
     * Finds the most appropriate page keywords for the given
     * {@code object}.
     *
     * @param object If {@code null}, returns {@code null}.
     * @deprecated Use {@link ObjectModification#findKeywords} instead.
     */
    @Deprecated
    public static Set<String> findKeywords(Object object) {
        return Static.findKeywords(object);
    }

    /**
     * {@link Seo} utility methods.
     *
     * @deprecated Use the {@code find*} methods {@link ObjectModification}
     * instead.
     */
    @Deprecated
    public static final class Static {

        /**
         * Finds the most appropriate page title for the given {@code object}.
         *
         * @param object If {@code null}, returns {@code null}.
         * @deprecated Use {@link ObjectModification#findTitle} instead.
         */
        @Deprecated
        public static String findTitle(Object object) {
            return object != null ?
                    State.getInstance(object).as(ObjectModification.class).findTitle() :
                    null;
        }

        /**
         * Finds the most appropriate page description for the given
         * {@code object}.
         *
         * @param object If {@code null}, returns {@code null}.
         * @deprecated Use {@link ObjectModification#findDescription} instead.
         */
        @Deprecated
        public static String findDescription(Object object) {
            return object != null ?
                    State.getInstance(object).as(ObjectModification.class).findDescription() :
                    null;
        }

        /**
         * Finds the most appropriate page keywords for the given
         * {@code object}.
         *
         * @param object If {@code null}, returns {@code null}.
         * @deprecated Use {@link ObjectModification#findKeywords} instead.
         */
        @Deprecated
        public static Set<String> findKeywords(Object object) {
            return object != null ?
                    State.getInstance(object).as(ObjectModification.class).findKeywords() :
                    null;
        }
    }

    /**
     * All possible robots meta tag values.
     *
     * @see <a href="https://developers.google.com/webmasters/control-crawl-index/docs/robots_meta_tag">Robots meta tag documentation</a>
     */
    public enum RobotsValue {
        NOINDEX,
        NOFOLLOW,
        NOARCHIVE,
        NOSNIPPET,
        NOODP,
        NOTRANSLATE,
        NOIMAGEINDEX;
    }

    /**
     * Object modification for specifying SEO-related overrides.
     */
    @Recordable.BeanProperty("seo")
    @Modification.FieldInternalNamePrefix("cms.seo.")
    public static final class ObjectModification extends Modification<Object> {

        @ToolUi.Hidden(false)
        @ToolUi.Placeholder(dynamicText = "${content.seo.findTitle()}", editable = true)
        @ToolUi.Tab("SEO")
        private String title;

        @ToolUi.Hidden(false)
        @ToolUi.Placeholder(dynamicText = "${content.seo.findDescription()}", editable = true)
        @ToolUi.Tab("SEO")
        private String description;

        @ToolUi.Hidden(false)
        @ToolUi.Tab("SEO")
        private Set<String> keywords;

        @ToolUi.Hidden(false)
        @ToolUi.NoteHtml("See <a href=\"https://developers.google.com/webmasters/control-crawl-index/docs/robots_meta_tag\" target=\"_blank\">robots meta tag documentation</a> for more information.")
        @ToolUi.Tab("SEO")
        private Set<RobotsValue> robots;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        /**
         * @return Never {@code null}. Mutable.
         */
        public Set<String> getKeywords() {
            if (keywords == null) {
                keywords = new LinkedHashSet<String>();
            }
            return keywords;
        }

        /**
         * @param keywords May be {@code null} to clear the set.
         */
        public void setKeywords(Set<String> keywords) {
            this.keywords = keywords;
        }

        /**
         * @return Never {@code null}.
         */
        public Set<RobotsValue> getRobots() {
            if (robots == null) {
                robots = new LinkedHashSet<RobotsValue>();
            }
            return robots;
        }

        /**
         * @param robots May be {@code null} to clear the set.
         */
        public void setRobots(Set<RobotsValue> robots) {
            this.robots = robots;
        }

        /**
         * Finds the most appropriate page title.
         *
         * @return Never {@code null}.
         */
        public String findTitle() {
            String title = getTitle();

            if (!ObjectUtils.isBlank(title)) {
                return title;
            }

            State state = getState();
            ObjectType type = state.getType();

            if (type != null) {
                for (String field : type.as(TypeModification.class).getTitleFields()) {
                    Object fieldTitle = state.getByPath(field);

                    if (fieldTitle != null) {
                        title = toMetaTagString(fieldTitle);

                        if (title != null) {
                            return title;
                        }
                    }
                }
            }

            return getState().getLabel();
        }

        // Converts the given object into a plain string that's usable
        // inside a meta tag.
        private static String toMetaTagString(Object object) {
            String string;

            if (object instanceof ReferentialText) {
                StringBuilder sb = new StringBuilder();

                for (Object item : (ReferentialText) object) {
                    if (item instanceof String) {
                        sb.append((String) item);
                    }
                }

                string = StringUtils.unescapeHtml(
                        sb.toString().replaceAll("<[^>]+>", ""));

            } else if (object instanceof Recordable) {
                string = ((Recordable) object).getState().getLabel();

            } else {
                string = object.toString();
            }

            if (string != null) {
                string = string.trim();

                if (!string.isEmpty()) {
                    return string;
                }
            }

            return null;
        }

        /**
         * Finds the most appropriate page description.
         *
         * @return May be {@code null}.
         */
        public String findDescription() {
            String description = getDescription();

            if (!ObjectUtils.isBlank(description)) {
                return description;
            }

            State state = getState();
            ObjectType type = state.getType();

            if (type != null) {
                for (String field : type.as(TypeModification.class).getDescriptionFields()) {
                    Object fieldDescription = state.getByPath(field);

                    if (fieldDescription != null) {
                        description = toMetaTagString(fieldDescription);

                        if (description != null) {
                            return description;
                        }
                    }
                }
            }

            return null;
        }

        /**
         * Finds the most appropriate page keywords.
         *
         * @return May be {@code null}. The set is ordered, and its
         * {@link #toString} will return a comma-delimited string.
         */
        public Set<String> findKeywords() {
            @SuppressWarnings("serial")
            Set<String> keywords = new LinkedHashSet<String>() {
                @Override
                public String toString() {
                    return Joiner.on(',').skipNulls().join(this);
                }
            };

            keywords.addAll(getKeywords());

            State state = getState();
            ObjectType type = state.getType();

            if (type != null) {
                for (String field : type.as(TypeModification.class).getKeywordsFields()) {
                    Iterable<?> fieldKeywords = ObjectUtils.to(Iterable.class, state.getByPath(field));

                    if (fieldKeywords != null) {
                        for (Object item : fieldKeywords) {
                            if (item != null) {
                                keywords.add(toMetaTagString(item));
                            }
                        }
                    }
                }
            }

            if (!keywords.isEmpty()) {
                return keywords;
            }

            return null;
        }

        /**
         * Finds the most appropriate robots string.
         *
         * @return May be {@code null}.
         * @see <a href="https://developers.google.com/webmasters/control-crawl-index/docs/robots_meta_tag">Robots meta tag documentation</a>
         */
        public String findRobotsString() {
            Set<RobotsValue> robots = getRobots();

            if (robots == null || robots.isEmpty()) {
                return null;
            }

            StringBuilder string = new StringBuilder();

            for (Iterator<Seo.RobotsValue> i = getRobots().iterator(); i.hasNext();) {
                Seo.RobotsValue value = i.next();

                string.append(value.name().toLowerCase(Locale.ENGLISH));

                if (i.hasNext()) {
                    string.append(",");
                }
            }

            return string.toString();
        }
    }

    /**
     * Type modification for specifying various fields that are checked to
     * find SEO-related data.
     */
    @Modification.FieldInternalNamePrefix("cms.seo.")
    public static final class TypeModification extends Modification<ObjectType> {

        private List<String> titleFields;
        private List<String> descriptionFields;
        private List<String> keywordsFields;
        private String openGraphType;

        /**
         * @return Never {@code null}. Mutable.
         */
        public List<String> getTitleFields() {
            if (titleFields == null) {
                titleFields = new ArrayList<String>();
            }
            return titleFields;
        }

        /**
         * @param titleFields May be {@code null} to clear the list.
         */
        public void setTitleFields(List<String> titleFields) {
            this.titleFields = titleFields;
        }

        /**
         * @return Never {@code null}. Mutable.
         */
        public List<String> getDescriptionFields() {
            if (descriptionFields == null) {
                descriptionFields = new ArrayList<String>();
            }
            return descriptionFields;
        }

        /**
         * @param descriptionFields May be {@code null} to clear the list.
         */
        public void setDescriptionFields(List<String> descriptionFields) {
            this.descriptionFields = descriptionFields;
        }

        /**
         * @return Never {@code null}. Mutable.
         */
        public List<String> getKeywordsFields() {
            if (keywordsFields == null) {
                keywordsFields = new ArrayList<String>();
            }
            return keywordsFields;
        }

        /**
         * @param keywordsFields May be {@code null} to clear the list.
         */
        public void setKeywordsFields(List<String> keywordsFields) {
            this.keywordsFields = keywordsFields;
        }

        public String getOpenGraphType() {
            return openGraphType;
        }

        public void setOpenGraphType(String openGraphType) {
            this.openGraphType = openGraphType;
        }
    }

    /**
     * Specifies an array of field paths that are checked to find the page
     * title from an instance of the target type.
     */
    @Documented
    @Inherited
    @ObjectType.AnnotationProcessorClass(TitleFieldsProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface TitleFields {
        String[] value();
    }

    private static class TitleFieldsProcessor implements ObjectType.AnnotationProcessor<TitleFields> {
        @Override
        public void process(ObjectType type, TitleFields annotation) {
            Collections.addAll(
                      type.as(TypeModification.class).getTitleFields(),
                      annotation.value());
        }
    }

    /**
     * Specifies an array of field paths that are checked to find the page
     * description from an instance of the target type.
     */
    @Documented
    @Inherited
    @ObjectType.AnnotationProcessorClass(DescriptionFieldsProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface DescriptionFields {
        String[] value();
    }

    private static class DescriptionFieldsProcessor implements ObjectType.AnnotationProcessor<DescriptionFields> {
        @Override
        public void process(ObjectType type, DescriptionFields annotation) {
            Collections.addAll(
                    type.as(TypeModification.class).getDescriptionFields(),
                    annotation.value());
        }
    }

    /**
     * Specifies an array of field paths that are checked to find the page
     * keywords from an instance of the target type.
     */
    @Documented
    @Inherited
    @ObjectType.AnnotationProcessorClass(KeywordsFieldsProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface KeywordsFields {
        String[] value();
    }

    private static class KeywordsFieldsProcessor implements ObjectType.AnnotationProcessor<KeywordsFields> {
        @Override
        public void process(ObjectType type, KeywordsFields annotation) {
            Collections.addAll(
                    type.as(TypeModification.class).getKeywordsFields(),
                    annotation.value());
        }
    }

    /**
     * Specifies the Open Graph type of the target type.
     *
     * @see <a href="http://ogp.me/">Open Graph protocol</a>
     */
    @Documented
    @Inherited
    @ObjectType.AnnotationProcessorClass(OpenGraphTypeProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface OpenGraphType {
        String value();
    }

    private static class OpenGraphTypeProcessor implements ObjectType.AnnotationProcessor<OpenGraphType> {
        @Override
        public void process(ObjectType type, OpenGraphType annotation) {
            type.as(TypeModification.class).setOpenGraphType(annotation.value());
        }
    }
}
