package com.psddev.cms.db;

import java.io.IOException;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.psddev.dari.db.Modification;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.HtmlWriter;
import com.psddev.dari.util.ObjectUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface Renderer extends Recordable {

    public static final String DEFAULT_CONTEXT = "";

    public void renderObject(
            HttpServletRequest request,
            HttpServletResponse response,
            HtmlWriter writer)
            throws IOException, ServletException;

    /**
     * Resolves the path to be used to render an object for a given HTTP
     * request.
     */
    public static interface PathResolver {

        /**
         * Gets the renderer path for this object in context of the
         * {@code request}.
         *
         * @param request the current HTTP request.
         * @return the path to the script that should be used to render this
         * object.
         */
        String getRendererPath(HttpServletRequest request);
    }

    /**
     * Resolves the RendererView class to be used to when rendering an object
     * for a given HTTP request.
     */
    public static interface ViewClassResolver {

        /**
         * Gets the renderer view class for this object in context of the
         * {@code request}.
         *
         * @param request the current HTTP request.
         * @return the RendererView class that should be used when renderering
         * this object.
         */
        Class<? extends RendererView> getRendererViewClass(HttpServletRequest request);
    }

    /**
     * Global modification that stores rendering hints.
     */
    @FieldInternalNamePrefix("cms.renderable.")
    public static class Data extends Modification<Object> {

        public Map<String, List<String>> listLayouts;

        public Map<String, List<String>> getListLayouts() {
            if (listLayouts == null) {
                listLayouts = new HashMap<String, List<String>>();
            }
            return listLayouts;
        }

        public void setListLayouts(Map<String, List<String>> listLayouts) {
            this.listLayouts = listLayouts;
        }
    }

    /**
     * Type modification that stores how objects should be rendered.
     */
    @FieldInternalNamePrefix("cms.render.")
    public static class TypeModification extends Modification<ObjectType> {

        private static final Logger LOGGER = LoggerFactory.getLogger(Renderer.class);

        @InternalName("renderScript")
        private String path;

        private Map<String, String> paths;
        private String layoutPath;
        private String embedPath;
        private int embedPreviewWidth;

        private Map<String, String> viewClassNames;

        // Returns the legacy rendering JSP.
        private String getDefaultRecordJsp() {
            return (String) getState().get("cms.defaultRecordJsp");
        }

        /**
         * Returns the default servlet path used to render instances of this
         * type.
         *
         * @return May be {@code null}.
         */
        public String getPath() {
            return getEffectivePaths().get(DEFAULT_CONTEXT);
        }

        /**
         * Returns the default servlet path used to render instances of this
         * type.
         *
         * @param path May be {@code nul}.
         */
        public void setPath(String path) {
            this.path = path;
        }

        /**
         * Returns all servlet paths associated with rendering instances of
         * this type in a specific context.
         *
         * @return Never {@code null}.
         * @see ContextTag
         */
        public Map<String, String> getPaths() {
            Map<String, String> paths = getEffectivePaths();
            paths.remove(DEFAULT_CONTEXT);
            return paths;
        }

        /**
         * Sets all servlet paths associated with rendering instances of
         * this type in a specific context.
         *
         * @param paths May be {@code null} to remove all associations.
         * @see ContextTag
         */
        public void setPaths(Map<String, String> paths) {
            this.paths = paths;
        }

        /**
         * Returns the raw default servlet path used to render instances of
         * this type. Raw in this case means the data stored directly on this
         * type definition. Additional data may be used when resolving the path
         * based on other related type definitions.
         *
         * @return May be {@code null}.
         * @see #getPath()
         */
        String getRawPath() {
            if (ObjectUtils.isBlank(path)) {
                String jsp = getDefaultRecordJsp();

                if (!ObjectUtils.isBlank(jsp)) {
                    path = jsp;
                }
            }

            return path;
        }

        /**
         * Returns all the raw servlet paths associated with rendering
         * instances of this type in a specific context.  Raw in this case
         * means the data stored directly on this type definition. Additional
         * data may be used when resolving the path based on other related type
         * definitions.
         *
         * @return Never {@code null}.
         * @see ContextTag
         * @see #getRawPaths()
         */
        Map<String, String> getRawPaths() {
            if (paths == null) {
                paths = new CompactMap<>();
            }

            return paths;
        }

        /**
         * Returns the servlet path used to render the layout around the
         * instances of this type.
         *
         * @return May be {@code null}.
         */
        public String getLayoutPath() {
            return layoutPath;
        }

        /**
         * Sets the servlet path used to render the layout around the
         * instances of this type.
         *
         * @param layoutPath May be {@code null}.
         */
        public void setLayoutPath(String layoutPath) {
            this.layoutPath = layoutPath;
        }

        public String getEmbedPath() {
            return embedPath;
        }

        public void setEmbedPath(String embedPath) {
            this.embedPath = embedPath;
        }

        public int getEmbedPreviewWidth() {
            return embedPreviewWidth;
        }

        public void setEmbedPreviewWidth(int embedPreviewWidth) {
            this.embedPreviewWidth = embedPreviewWidth;
        }

        /**
         * Returns the RendererView class names associated with rendering
         * instances of this type in a specific context.
         *
         * @return Never {@code null}.
         */
        public Map<String, String> getViewClassNames() {
            if (viewClassNames == null) {
                viewClassNames = new CompactMap<>();
            }
            return viewClassNames;
        }

        /**
         * Sets RendererView class names associated with rendering instances
         * of this type in a specific context.
         *
         * @param viewClassNames May be {@code null} to remove all associations.
         * @see ContextTag
         */
        public void setViewClassNames(Map<String, String> viewClassNames) {
            this.viewClassNames = viewClassNames;
        }

        /**
         * Returns all RendererView classes associated with rendering instances
         * of this type in a specific context.
         *
         * @return Never {@code null}.
         * @see ContextTag
         */
        public Map<String, Class<? extends RendererView>> getViewClasses() {

            Map<String, Class<? extends RendererView>> viewClasses = new CompactMap<>();

            viewClasses.putAll(getInheritedViewClasses());

            // only add contexts that don't already exist.
            for (Map.Entry<String, Class<? extends RendererView>> entry : getInterfaceViewClasses().entrySet()) {

                String context = entry.getKey();
                Class<? extends RendererView> viewClass = entry.getValue();

                if (!viewClasses.containsKey(context)) {
                    viewClasses.put(context, viewClass);

                } else {
                    LOGGER.warn("A renderer mapping for context [" + context +
                            "] already exists, skipping view class [" + viewClass + "].");
                }
            }

            return viewClasses;
        }

        /**
         * Finds the servlet path that should be used to render the instances
         * of this type in the current context of the given {@code request}.
         *
         * @param request Can't be {@code null}.
         * @return May be {@code null}.
         */
        public String findContextualPath(ServletRequest request) {
            Map<String, String> paths = getPaths();

            for (Iterator<String> i = ContextTag.Static.getContexts(request).descendingIterator(); i.hasNext();) {
                String context = i.next();
                String path = paths.get(context);

                if (!ObjectUtils.isBlank(path)) {
                    return path;
                }
            }

            return getPath();
        }

        /**
         * Finds the RendererView class that should be used to render the
         * instances of this type in the current context of the given
         * {@code request}.
         *
         * @param request Can't be {@code null}.
         * @return May be {@code null}.
         */
        public Class<? extends RendererView> findContextualViewClass(ServletRequest request) {
            Map<String, Class<? extends RendererView>> viewClasses = getViewClasses();

            for (Iterator<String> i = ContextTag.Static.getContexts(request).descendingIterator(); i.hasNext();) {
                String context = i.next();
                Class<? extends RendererView> viewClass = viewClasses.get(context);

                if (viewClass != null) {
                    return viewClass;
                }
            }

            return viewClasses.get(DEFAULT_CONTEXT);
        }

        private Map<String, String> getEffectiveRawPaths() {
            Map<String, String> paths = new CompactMap<>();

            paths.putAll(getRawPaths());

            String defaultPath = getRawPath();
            if (!ObjectUtils.isBlank(defaultPath)) {
                paths.put(DEFAULT_CONTEXT, defaultPath);
            }

            return paths;
        }

        private Map<String, String> getInheritedPaths() {
            Map<String, String> paths = new CompactMap<>();

            ObjectType originalType = getOriginalObject();

            List<ObjectType> superTypes = new ArrayList<>();

            superTypes.add(originalType);

            for (String className : getOriginalObject().getSuperClassNames()) {

                Class<?> klass = ObjectUtils.getClassByName(className);
                if (klass != null) {

                    ObjectType type = ObjectType.getInstance(klass);
                    if (type != null && !type.equals(originalType)) {
                        superTypes.add(type);
                    }
                }
            }

            Collections.reverse(superTypes);

            for (ObjectType type : superTypes) {
                paths.putAll(type.as(Renderer.TypeModification.class).getEffectiveRawPaths());
            }

            return paths;
        }

        private Map<String, String> getInterfacePaths() {
            Map<String, String> paths = new CompactMap<>();

            Set<String> seenContexts = new HashSet<>();

            ObjectType originalType = getOriginalObject();

            for (String group : originalType.getGroups()) {

                Class<?> klass = ObjectUtils.getClassByName(group);
                if (klass != null && klass.isInterface()) {

                    ObjectType type = ObjectType.getInstance(klass);

                    if (type != null && !type.equals(originalType)) {

                        for (Map.Entry<String, String> entry : type.as(Renderer.TypeModification.class).getEffectiveRawPaths().entrySet()) {

                            String context = entry.getKey();
                            String path = entry.getValue();
                            if (context != null) {

                                if (seenContexts.add(context)) {
                                    paths.put(context, path);

                                } else {
                                    paths.remove(context);
                                    LOGGER.warn("More than one renderer mapping for context [" +
                                            context + "], skipping path [" + path + "].");
                                }
                            }
                        }
                    }
                }
            }

            return paths;
        }

        private Map<String, String> getEffectivePaths() {
            Map<String, String> paths = new CompactMap<>();

            paths.putAll(getInheritedPaths());

            // only add contexts that don't already exist.
            for (Map.Entry<String, String> entry : getInterfacePaths().entrySet()) {

                String context = entry.getKey();
                String path = entry.getValue();

                if (!paths.containsKey(context)) {
                    paths.put(context, path);

                } else {
                    LOGGER.warn("A renderer mapping for context [" + context +
                            "] already exists, skipping path [" + path + "].");
                }
            }

            return paths;
        }

        private Map<String, Class<? extends RendererView>> getRawViewClasses() {
            Map<String, Class<? extends RendererView>> viewClasses = new CompactMap<>();

            for (Map.Entry<String, String> entry : getViewClassNames().entrySet()) {

                Class<?> klass = ObjectUtils.getClassByName(entry.getValue());

                if (klass != null && RendererView.class.isAssignableFrom(klass)) {

                    @SuppressWarnings("unchecked")
                    Class<? extends RendererView> viewClass = (Class<? extends RendererView>) klass;
                    viewClasses.put(entry.getKey(), viewClass);
                }
            }

            return viewClasses;
        }

        private Map<String, Class<? extends RendererView>> getInheritedViewClasses() {

            Map<String, Class<? extends RendererView>> viewClasses = new CompactMap<>();

            ObjectType originalType = getOriginalObject();

            List<ObjectType> superTypes = new ArrayList<>();

            superTypes.add(originalType);

            for (String className : getOriginalObject().getSuperClassNames()) {

                Class<?> klass = ObjectUtils.getClassByName(className);
                if (klass != null) {

                    ObjectType type = ObjectType.getInstance(klass);
                    if (type != null && !type.equals(originalType)) {
                        superTypes.add(type);
                    }
                }
            }

            Collections.reverse(superTypes);

            for (ObjectType type : superTypes) {
                viewClasses.putAll(type.as(Renderer.TypeModification.class).getRawViewClasses());
            }

            return viewClasses;
        }

        private Map<String, Class<? extends RendererView>> getInterfaceViewClasses() {

            Map<String, Class<? extends RendererView>> viewClasses = new CompactMap<>();

            Set<String> seenContexts = new HashSet<>();

            ObjectType originalType = getOriginalObject();

            for (String group : originalType.getGroups()) {

                Class<?> klass = ObjectUtils.getClassByName(group);
                if (klass != null && klass.isInterface()) {

                    ObjectType type = ObjectType.getInstance(klass);

                    if (type != null && !type.equals(originalType)) {

                        for (Map.Entry<String, Class<? extends RendererView>> entry : type.as(Renderer.TypeModification.class).getRawViewClasses().entrySet()) {

                            String context = entry.getKey();
                            Class<? extends RendererView> viewClass = entry.getValue();
                            if (context != null) {

                                if (seenContexts.add(context)) {
                                    viewClasses.put(context, viewClass);

                                } else {
                                    viewClasses.remove(context);
                                    LOGGER.warn("More than one renderer mapping for context [" +
                                            context + "], skipping view class [" + viewClass + "].");
                                }
                            }
                        }
                    }
                }
            }

            return viewClasses;
        }

        // --- Deprecated ---

        private static final String FIELD_PREFIX = "cms.render.";

        /** @deprecated No replacement. */
        @Deprecated
        public static final String ENGINE_FIELD = FIELD_PREFIX + "renderEngine";

        /** @deprecated No replacement. */
        @Deprecated
        public static final String SCRIPT_FIELD = FIELD_PREFIX + "renderScript";

        @Deprecated
        @InternalName(ENGINE_FIELD)
        private String engine;

        /** @deprecated No replacement. */
        @Deprecated
        public String getEngine() {
            if (ObjectUtils.isBlank(engine)) {
                String jsp = getDefaultRecordJsp();
                if (!ObjectUtils.isBlank(jsp)) {
                    setEngine("JSP");
                }
            }
            return engine;
        }

        /** @deprecated No replacement. */
        @Deprecated
        public void setEngine(String engine) {
            this.engine = engine;
        }

        /** @deprecated Use {@link #getPath} instead. */
        @Deprecated
        public String getScript() {
            return getPath();
        }

        /** @deprecated Use {@link #setPath} instead. */
        @Deprecated
        public void setScript(String script) {
            setPath(script);
        }
    }

    /**
     * Field modification that stores how field values should be
     * rendered.
     */
    public static class FieldData extends Modification<ObjectField> {

        private Map<String, List<String>> listLayouts;

        public Map<String, List<String>> getListLayouts() {
            if (listLayouts == null) {
                listLayouts = new HashMap<String, List<String>>();
            }
            return listLayouts;
        }

        public void setListLayouts(Map<String, List<String>> listLayouts) {
            this.listLayouts = listLayouts;
        }
    }

    /**
     * Specifies the servlet path used to render instances of the target type
     * as a module.
     */
    @Documented
    @Inherited
    @ObjectType.AnnotationProcessorClass(PathProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Repeatable(Paths.class)
    public @interface Path {
        String value();
        String context() default Renderer.DEFAULT_CONTEXT;
    }

    @Documented
    @Inherited
    @ObjectType.AnnotationProcessorClass(PathsProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Paths {
        Path[] value();
    }

    /**
     * Specifies the servlet path used to render instances of the target type
     * as a page.
     */
    @Documented
    @Inherited
    @ObjectType.AnnotationProcessorClass(LayoutPathProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface LayoutPath {
        String value();
    }

    /**
     * Specifies the servlet path used to render instances of the target type
     * when embedded in another page.
     */
    @Documented
    @Inherited
    @ObjectType.AnnotationProcessorClass(EmbedPathProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface EmbedPath {
        String value();
    }

    /**
     * Specifies the width (in pixels) of the preview for the instances of
     * the target type.
     */
    @Documented
    @Inherited
    @ObjectType.AnnotationProcessorClass(EmbedPreviewWidthProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface EmbedPreviewWidth {
        int value();
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ListLayout {
        String name();
        Class<?>[] itemClasses();
    }

    @Documented
    @Inherited
    @ObjectField.AnnotationProcessorClass(ListLayoutsProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ListLayouts {
        String[] value() default { };
        ListLayout[] map() default { };
    }

    /**
     * Specifies the list of {@link com.psddev.cms.db.Renderer.ViewClass}
     * annotations to be applied.
     */
    @Documented
    @ObjectType.AnnotationProcessorClass(ViewClassesProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface ViewClasses {
        ViewClass[] value();
    }

    /**
     * Specifies the class of the RendererView object that will be placed on
     * the request in a "view" attribute when rendering the annotated object in
     * the specified context.
     */
    @Documented
    @ObjectType.AnnotationProcessorClass(ViewClassProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Repeatable(ViewClasses.class)
    public @interface ViewClass {
        String context() default Renderer.DEFAULT_CONTEXT;
        Class<? extends RendererView> value();
    }

    // --- Deprecated ---

    /** @deprecated No replacement. */
    @Deprecated
    @Documented
    @Inherited
    @ObjectType.AnnotationProcessorClass(EngineProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Engine {
        String value();
    }

    /** @deprecated Use {@link Path} instead. */
    @Deprecated
    @Documented
    @Inherited
    @ObjectType.AnnotationProcessorClass(ScriptProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Script {
        String value();
    }
}

class PathProcessor implements ObjectType.AnnotationProcessor<Renderer.Path> {
    @Override
    public void process(ObjectType type, Renderer.Path annotation) {
        Renderer.TypeModification rendererData = type.as(Renderer.TypeModification.class);
        Map<String, String> paths = rendererData.getRawPaths();
        String value = annotation.value();
        String context = annotation.context();

        if (ObjectUtils.isBlank(context)) {
            rendererData.setPath(value);

        } else {
            paths.put(context, value);
        }
    }
}

class PathsProcessor implements ObjectType.AnnotationProcessor<Renderer.Paths> {
    @Override
    public void process(ObjectType type, Renderer.Paths annotation) {
        PathProcessor pathProcessor = new PathProcessor();

        for (Renderer.Path pathAnnotation : annotation.value()) {
            pathProcessor.process(type, pathAnnotation);
        }
    }
}

class LayoutPathProcessor implements ObjectType.AnnotationProcessor<Renderer.LayoutPath> {
    @Override
    public void process(ObjectType type, Renderer.LayoutPath annotation) {
        type.as(Renderer.TypeModification.class).setLayoutPath(annotation.value());
    }
}

class EmbedPathProcessor implements ObjectType.AnnotationProcessor<Renderer.EmbedPath> {
    @Override
    public void process(ObjectType type, Renderer.EmbedPath annotation) {
        type.as(Renderer.TypeModification.class).setEmbedPath(annotation.value());
    }
}

class EmbedPreviewWidthProcessor implements ObjectType.AnnotationProcessor<Renderer.EmbedPreviewWidth> {
    @Override
    public void process(ObjectType type, Renderer.EmbedPreviewWidth annotation) {
        type.as(Renderer.TypeModification.class).setEmbedPreviewWidth(annotation.value());
    }
}

class ListLayoutsProcessor implements ObjectField.AnnotationProcessor<Renderer.ListLayouts> {
    @Override
    public void process(ObjectType type, ObjectField field, Renderer.ListLayouts annotation) {
        String[] value = annotation.value();
        Renderer.ListLayout[] map = annotation.map();

        Map<String, List<String>> listLayouts = field.as(Renderer.FieldData.class).getListLayouts();

        for (String layoutName : value) {
            listLayouts.put(layoutName, new ArrayList<String>());
        }

        for (Renderer.ListLayout layout : map) {
            List<String> layoutItems = new ArrayList<String>();
            listLayouts.put(layout.name(), layoutItems);

            for (Class<?> itemClass : layout.itemClasses()) {
                layoutItems.add(itemClass.getName());
            }
        }
    }
}

class ViewClassesProcessor implements ObjectType.AnnotationProcessor<Renderer.ViewClasses> {
    @Override
    public void process(ObjectType type, Renderer.ViewClasses annotation) {
        ViewClassProcessor viewProcessor = new ViewClassProcessor();

        for (Renderer.ViewClass viewAnnotation : annotation.value()) {
            viewProcessor.process(type, viewAnnotation);
        }
    }
}

class ViewClassProcessor implements ObjectType.AnnotationProcessor<Renderer.ViewClass> {
    @Override
    public void process(ObjectType type, Renderer.ViewClass annotation) {
        Renderer.TypeModification rendererData = type.as(Renderer.TypeModification.class);

        String context = annotation.context();
        Class<? extends RendererView> viewClass = annotation.value();

        if (ObjectUtils.isBlank(context)) {
            context = Renderer.DEFAULT_CONTEXT;
        }

        rendererData.getViewClassNames().put(context, viewClass.getName());
    }
}

@Deprecated
class EngineProcessor implements ObjectType.AnnotationProcessor<Renderer.Engine> {
    @Override
    public void process(ObjectType type, Renderer.Engine annotation) {
        type.as(Renderer.TypeModification.class).setEngine(annotation.value());
    }
}

@Deprecated
class ScriptProcessor implements ObjectType.AnnotationProcessor<Renderer.Script> {
    @Override
    public void process(ObjectType type, Renderer.Script annotation) {
        type.as(Renderer.TypeModification.class).setScript(annotation.value());
    }
}
