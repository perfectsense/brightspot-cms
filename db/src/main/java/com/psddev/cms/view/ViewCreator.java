package com.psddev.cms.view;

import com.psddev.dari.util.TypeDefinition;

import org.slf4j.LoggerFactory;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @deprecated Use {@link ViewModel} instead.
 */
@Deprecated
public interface ViewCreator<M, V, VR> {

    /**
     * @deprecated Use {@link ViewModel#onCreate(ViewResponse)} instead.
     */
    @Deprecated
    V createView(M model, VR request);

    /**
     * @deprecated Use {@link ViewModel#onCreate(ViewResponse)} instead.
     */
    @Deprecated
    default boolean processRequest(VR request, ViewResponse response) {
        return true;
    }

    /**
     * @deprecated Use {@link ViewModel#findViewModelClass(Class, String, Object)} instead.
     */
    @Deprecated
    static <M, V, VR> Class<? extends ViewCreator<? super M, V, ? super VR>> findCreatorClass(M model, Class<V> viewClass, String viewType, VR viewRequest) {

        if (model == null) {
            return null;
        }

        Class<?> modelClass = model.getClass();

        Map<Class<?>, List<Class<? extends ViewCreator>>> modelToCreatorClassMap = new HashMap<>();

        Set<Class<? extends ViewCreator>> allCreatorClasses = new LinkedHashSet<>();

        for (Class<?> annotatableClass : ViewUtils.getAnnotatableClasses(modelClass)) {

            allCreatorClasses.addAll(
                    Arrays.stream(annotatableClass.getAnnotationsByType(ViewMapping.class))
                            // check that it matches the view type if it exists
                            .filter(viewMapping -> viewType == null || Arrays.asList(viewMapping.types()).contains(viewType))
                            // get the annotation's view creator class
                            .map(ViewMapping::value)
                            // make sure it's a concrete class
                            .filter(klass -> !Modifier.isAbstract(klass.getModifiers()) && !Modifier.isInterface(klass.getModifiers()))
                            .collect(Collectors.toList()));

            if (viewType == null && annotatableClass.isAnnotationPresent(StaticNestedViewMappings.class)) {
                allCreatorClasses.addAll(
                        Arrays.stream(annotatableClass.getClasses())
                                // make sure it's a view creator class
                                .filter(ViewCreator.class::isAssignableFrom)
                                // make sure it's a concrete class
                                .filter(klass -> !Modifier.isAbstract(klass.getModifiers()) && !Modifier.isInterface(klass.getModifiers()))
                                // cast it
                                .map(klass -> (Class<? extends ViewCreator>) klass)
                                .collect(Collectors.toList()));
            }
        }

        allCreatorClasses.forEach(creatorClass -> {

            TypeDefinition<? extends ViewCreator> typeDef = TypeDefinition.getInstance(creatorClass);
            Class<?> declaredModelClass = typeDef.getInferredGenericTypeArgumentClass(ViewCreator.class, 0);
            Class<?> declaredViewClass = typeDef.getInferredGenericTypeArgumentClass(ViewCreator.class, 1);
            Class<?> declaredViewRequestClass = typeDef.getInferredGenericTypeArgumentClass(ViewCreator.class, 2);

            if ((viewClass == null || (declaredViewClass != null && viewClass.isAssignableFrom(declaredViewClass)) || viewClass.isAssignableFrom(creatorClass))
                    && declaredModelClass != null && declaredModelClass.isAssignableFrom(modelClass)
                    && (viewRequest == null || declaredViewRequestClass != null && declaredViewRequestClass.isAssignableFrom(viewRequest.getClass()))) {

                List<Class<? extends ViewCreator>> creatorClasses = modelToCreatorClassMap.get(declaredModelClass);
                if (creatorClasses == null) {
                    creatorClasses = new ArrayList<>();
                    modelToCreatorClassMap.put(declaredModelClass, creatorClasses);
                }
                creatorClasses.add(creatorClass);
            }
        });

        if (!modelToCreatorClassMap.isEmpty()) {

            Set<Class<?>> nearestModelClasses = ViewUtils.getNearestSuperClassesInSet(model.getClass(), modelToCreatorClassMap.keySet());
            if (nearestModelClasses.size() == 1) {

                List<Class<? extends ViewCreator>> creatorClasses = modelToCreatorClassMap.get(nearestModelClasses.iterator().next());
                if (creatorClasses.size() == 1) {
                    @SuppressWarnings("unchecked")
                    Class<? extends ViewCreator<? super M, V, ? super VR>> creatorClass = (Class<? extends ViewCreator<? super M, V, ? super VR>>) (Object) creatorClasses.get(0);

                    return creatorClass;
                } else {
                    LoggerFactory.getLogger(ViewCreator.class)
                            .warn("Found " + creatorClasses.size()
                                    + " conflicting view creator mappings for model type ["
                                    + model.getClass() + "] and view type [" + viewClass + "]: "
                                    + creatorClasses);
                }
            } else {
                Set<Class<? extends ViewCreator>> conflictingCreatorClasses = new LinkedHashSet<>();
                for (Class<?> nearestModelClass : nearestModelClasses) {
                    conflictingCreatorClasses.addAll(modelToCreatorClassMap.get(nearestModelClass));
                }
                LoggerFactory.getLogger(ViewCreator.class)
                        .warn("Found " + conflictingCreatorClasses.size()
                                + " conflicting view creator mappings for model type ["
                                + model.getClass() + "] and view type [" + viewClass + "]: "
                                + conflictingCreatorClasses);
            }
        }

        return null;
    }

    /**
     * @deprecated Use {@link #findCreatorClass(Object, Class, String, Object)} instead.
     */
    @Deprecated
    static <M, V> ViewCreator<M, V, Object> createCreator(M model, Class<V> viewClass) {

        if (model == null) {
            return null;
        }

        Class<?> modelClass = model.getClass();

        Map<Class<?>, List<Class<? extends ViewCreator>>> modelToCreatorClassMap = new HashMap<>();

        Set<Class<? extends ViewCreator>> allCreatorClasses = new LinkedHashSet<>();

        for (Class<?> annotatableClass : ViewUtils.getAnnotatableClasses(modelClass)) {

            allCreatorClasses.addAll(
                    Arrays.stream(annotatableClass.getAnnotationsByType(ViewMapping.class))
                            .map(ViewMapping::value)
                            .collect(Collectors.toList()));

            if (annotatableClass.isAnnotationPresent(StaticNestedViewMappings.class)) {
                allCreatorClasses.addAll(
                        Arrays.stream(annotatableClass.getClasses())
                                .filter(ViewCreator.class::isAssignableFrom)
                                .map((klass) -> (Class<? extends ViewCreator>) klass)
                                .collect(Collectors.toList()));
            }
        }

        allCreatorClasses.forEach(creatorClass -> {

            TypeDefinition<? extends ViewCreator> typeDef = TypeDefinition.getInstance(creatorClass);
            Class<?> declaredModelClass = typeDef.getInferredGenericTypeArgumentClass(ViewCreator.class, 0);
            Class<?> declaredViewClass = typeDef.getInferredGenericTypeArgumentClass(ViewCreator.class, 1);

            if (((declaredViewClass != null && viewClass.isAssignableFrom(declaredViewClass)) || viewClass.isAssignableFrom(creatorClass))
                    && declaredModelClass != null && declaredModelClass.isAssignableFrom(modelClass)) {

                List<Class<? extends ViewCreator>> creatorClasses = modelToCreatorClassMap.get(declaredModelClass);
                if (creatorClasses == null) {
                    creatorClasses = new ArrayList<>();
                    modelToCreatorClassMap.put(declaredModelClass, creatorClasses);
                }
                creatorClasses.add(creatorClass);
            }
        });

        if (!modelToCreatorClassMap.isEmpty()) {

            Set<Class<?>> nearestModelClasses = ViewUtils.getNearestSuperClassesInSet(model.getClass(), modelToCreatorClassMap.keySet());
            if (nearestModelClasses.size() == 1) {

                List<Class<? extends ViewCreator>> creatorClasses = modelToCreatorClassMap.get(nearestModelClasses.iterator().next());
                if (creatorClasses.size() == 1) {

                    Class<? extends ViewCreator> creatorClass = creatorClasses.get(0);
                    try {
                        @SuppressWarnings("unchecked")
                        ViewCreator<M, V, Object> creator = (ViewCreator<M, V, Object>) TypeDefinition.getInstance(creatorClass).newInstance();

                        return creator;
                    } catch (Exception e) {
                        LoggerFactory.getLogger(ViewCreator.class)
                                .warn("Unable to create view creator of type [" + creatorClass.getName() + "]");
                    }
                } else {
                    LoggerFactory.getLogger(ViewCreator.class)
                            .warn("Found " + creatorClasses.size()
                                    + " conflicting view creator mappings for model type ["
                                    + model.getClass() + "] and view type [" + viewClass + "]: "
                                    + creatorClasses);
                }
            } else {
                Set<Class<? extends ViewCreator>> conflictingCreatorClasses = new LinkedHashSet<>();
                for (Class<?> nearestModelClass : nearestModelClasses) {
                    conflictingCreatorClasses.addAll(modelToCreatorClassMap.get(nearestModelClass));
                }
                LoggerFactory.getLogger(ViewCreator.class)
                        .warn("Found " + conflictingCreatorClasses.size()
                                + " conflicting view creator mappings for model type ["
                                + model.getClass() + "] and view type [" + viewClass + "]: "
                                + conflictingCreatorClasses);
            }
        } else {
            LoggerFactory.getLogger(ViewCreator.class)
                    .warn("No view creator mappings found for model type ["
                            + model.getClass() + "] and view type [" + viewClass + "]");
        }
        return null;
    }

    /**
     * @deprecated Use {@link #findCreatorClass(Object, Class, String, Object)} instead.
     */
    @Deprecated
    static <M> ViewCreator<M, ?, Object> createCreator(M model, String viewType) {
        for (Class<?> c : ViewUtils.getAnnotatableClasses(model.getClass())) {
            for (ViewMapping mapping : c.getAnnotationsByType(ViewMapping.class)) {
                Optional<String> name = Arrays.stream(mapping.types())
                        .filter(n -> n.equals(viewType))
                        .findFirst();

                if (name.isPresent()) {
                    return (ViewCreator<M, ?, Object>) TypeDefinition.getInstance(mapping.value()).newInstance();
                }
            }
        }

        return null;
    }
}
