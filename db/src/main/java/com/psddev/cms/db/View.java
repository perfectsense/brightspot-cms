package com.psddev.cms.db;

import com.psddev.dari.db.Modification;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.TypeDefinition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Helps render an object in the context of an HTTP request.
 *
 * @param <T> the type of object being rendered.
 */
public abstract class View<T extends Recordable> {

    public static final String DEFAULT_CONTEXT = "";

    private T object;

    private HttpServletRequest request;

    private HttpServletResponse response;

    /**
     * @return the object being rendered.
     */
    protected final T getObject() {
        return object;
    }

    /**
     * @return the current HTTP request.
     */
    protected final HttpServletRequest getRequest() {
        return request;
    }

    /**
     * @return the current HTTP response.
     */
    protected final HttpServletResponse getResponse() {
        return response;
    }

    /**
     * Creates an instance of View of type {@code viewClass} with the
     * given arguments.
     *
     * @param viewClass the class of the View to be created.
     * @param object the associated object for the view.
     * @param request the associated request for the view.
     * @param response the associated response for the view.
     * @param <V> the View type.
     * @param <T> the Recordable type.
     * @return a newly created View object based on the given arguments.
     */
    public static <V extends View<T>, T extends Recordable> View<T> create(
            Class<V> viewClass, T object, HttpServletRequest request, HttpServletResponse response) {

        V view = TypeDefinition.getInstance(viewClass).newInstance();
        ((View<T>) view).object = object;
        ((View<T>) view).request = request;
        ((View<T>) view).response = response;
        return view;
    }

    /**
     * Type modification that stores views associated with objects of a certain
     * type.
     */
    @Recordable.FieldInternalNamePrefix("cms.view.")
    public static class TypeModification extends Modification<ObjectType> {

        private static final Logger LOGGER = LoggerFactory.getLogger(View.class);

        private Map<String, String> viewClassNames;

        /**
         * Returns the View class names associated with rendering
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
         * Sets View class names associated with rendering instances
         * of this type in a specific context.
         *
         * @param viewClassNames May be {@code null} to remove all associations.
         * @see ContextTag
         */
        public void setViewClassNames(Map<String, String> viewClassNames) {
            this.viewClassNames = viewClassNames;
        }

        /**
         * Returns all View classes associated with rendering instances
         * of this type in a specific context.
         *
         * @return Never {@code null}.
         * @see ContextTag
         */
        public Map<String, Class<? extends View>> getViewClasses() {

            Map<String, Class<? extends View>> viewClasses = new CompactMap<>();

            viewClasses.putAll(getInheritedViewClasses());

            // only add contexts that don't already exist.
            for (Map.Entry<String, Class<? extends View>> entry : getInterfaceViewClasses().entrySet()) {

                String context = entry.getKey();
                Class<? extends View> viewClass = entry.getValue();

                if (!viewClasses.containsKey(context)) {
                    viewClasses.put(context, viewClass);

                } else {
                    LOGGER.warn("A view mapping for context [" + context +
                            "] already exists, skipping view class [" + viewClass + "].");
                }
            }

            return viewClasses;
        }

        /**
         * Finds the View class that should be used while rendering
         * instances of this type in the current context of the given
         * {@code request}.
         *
         * @param request Can't be {@code null}.
         * @return May be {@code null}.
         */
        public Class<? extends View> findContextualViewClass(ServletRequest request) {
            Map<String, Class<? extends View>> viewClasses = getViewClasses();

            for (Iterator<String> i = ContextTag.Static.getContexts(request).descendingIterator(); i.hasNext();) {
                String context = i.next();
                Class<? extends View> viewClass = viewClasses.get(context);

                if (viewClass != null) {
                    return viewClass;
                }
            }

            return viewClasses.get(DEFAULT_CONTEXT);
        }

        private Map<String, Class<? extends View>> getRawViewClasses() {
            Map<String, Class<? extends View>> viewClasses = new CompactMap<>();

            for (Map.Entry<String, String> entry : getViewClassNames().entrySet()) {

                Class<?> klass = ObjectUtils.getClassByName(entry.getValue());

                if (klass != null && View.class.isAssignableFrom(klass)) {

                    @SuppressWarnings("unchecked")
                    Class<? extends View> viewClass = (Class<? extends View>) klass;
                    viewClasses.put(entry.getKey(), viewClass);
                }
            }

            return viewClasses;
        }

        private Map<String, Class<? extends View>> getInheritedViewClasses() {

            Map<String, Class<? extends View>> viewClasses = new CompactMap<>();

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
                viewClasses.putAll(type.as(TypeModification.class).getRawViewClasses());
            }

            return viewClasses;
        }

        private Map<String, Class<? extends View>> getInterfaceViewClasses() {

            Map<String, Class<? extends View>> viewClasses = new CompactMap<>();

            Set<String> seenContexts = new HashSet<>();

            ObjectType originalType = getOriginalObject();

            for (String group : originalType.getGroups()) {

                Class<?> klass = ObjectUtils.getClassByName(group);
                if (klass != null && klass.isInterface()) {

                    ObjectType type = ObjectType.getInstance(klass);

                    if (type != null && !type.equals(originalType)) {

                        for (Map.Entry<String, Class<? extends View>> entry : type.as(TypeModification.class).getRawViewClasses().entrySet()) {

                            String context = entry.getKey();
                            Class<? extends View> viewClass = entry.getValue();
                            if (context != null) {

                                if (seenContexts.add(context)) {
                                    viewClasses.put(context, viewClass);

                                } else {
                                    viewClasses.remove(context);
                                    LOGGER.warn("More than one view mapping for context [" +
                                            context + "], skipping view class [" + viewClass + "].");
                                }
                            }
                        }
                    }
                }
            }

            return viewClasses;
        }
    }

    /**
     * Specifies the list of {@link com.psddev.cms.db.View.SetClass}
     * annotations to be applied.
     */
    @Documented
    @ObjectType.AnnotationProcessorClass(SetClassesProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface SetClasses {
        SetClass[] value();
    }

    /**
     * Specifies the class of the View object that will be placed on
     * the request in a "view" attribute when rendering the annotated object in
     * the specified context.
     */
    @Documented
    @ObjectType.AnnotationProcessorClass(SetClassProcessor.class)
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Repeatable(SetClasses.class)
    public @interface SetClass {
        String context() default View.DEFAULT_CONTEXT;
        Class<? extends View> value();
    }
}

class SetClassesProcessor implements ObjectType.AnnotationProcessor<View.SetClasses> {
    @Override
    public void process(ObjectType type, View.SetClasses annotation) {
        SetClassProcessor setClassProcessor = new SetClassProcessor();

        for (View.SetClass viewAnnotation : annotation.value()) {
            setClassProcessor.process(type, viewAnnotation);
        }
    }
}

class SetClassProcessor implements ObjectType.AnnotationProcessor<View.SetClass> {
    @Override
    public void process(ObjectType type, View.SetClass annotation) {
        View.TypeModification viewData = type.as(View.TypeModification.class);

        String context = annotation.context();
        Class<? extends View> viewClass = annotation.value();

        if (ObjectUtils.isBlank(context)) {
            context = View.DEFAULT_CONTEXT;
        }

        viewData.getViewClassNames().put(context, viewClass.getName());
    }
}
