package com.psddev.cms.view;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.psddev.dari.util.ClassFinder;
import com.psddev.dari.util.CodeUtils;
import com.psddev.dari.util.TypeDefinition;

/**
 * Binds a model with a view model to produce a view.
 *
 * @param <M> the model type to bind with the view model.
 */
public abstract class ViewModel<M> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ViewModel.class);

    private static final LoadingCache<Class<?>, LoadingCache<Object, Optional<Class<?>>>> VIEW_BINDINGS = CacheBuilder.newBuilder()
            .weakKeys()
            .build(new CacheLoader<Class<?>, LoadingCache<Object, Optional<Class<?>>>>() {

                @Override
                @ParametersAreNonnullByDefault
                public LoadingCache<Object, Optional<Class<?>>> load(Class<?> modelClass) {
                    return CacheBuilder.newBuilder()
                            .build(new CacheLoader<Object, Optional<Class<?>>>() {

                                @Override
                                @ParametersAreNonnullByDefault
                                public Optional<Class<?>> load(final Object viewTypeObject) {

                                    Class<?> viewClass = viewTypeObject instanceof Class ? (Class<?>) viewTypeObject : null;
                                    String viewType = viewTypeObject instanceof String ? (String) viewTypeObject : null;

                                    return Optional.ofNullable(findViewModelClassCacheHelper(viewClass, viewType, modelClass));
                                }
                            });
                }
            });

    static {
        CodeUtils.addRedefineClassesListener(classes -> VIEW_BINDINGS.invalidateAll());
    }

    private ViewModelCreator viewModelCreator;

    private ViewResponse viewResponse;

    protected M model;

    /**
     * Called during creation of this view model before {@link #onCreate(ViewResponse)}.
     * The object is fully initialized at this point so it is safe to utilize
     * full functionality. The default implementation always returns {@code true}.
     * Sub-classes may override this method to return {@code false} if the
     * ViewModel creation should not be completed, causing {@code null} to be
     * returned from the upstream caller.
     */
    protected boolean shouldCreate() {
        return true;
    }

    /**
     * Called during creation of this view model. The object is fully initialized
     * at this point so it is safe to utilize full functionality.
     *
     * @param response the current view response.
     */
    protected void onCreate(ViewResponse response) {
        // do nothing by default
    }

    /**
     * Returns a iterable of views of type {@code viewClass} that are bound to
     * the given {@code object}. If the object is itself an iterable, then each
     * item is evaluated and the returned iterable of views will have at most
     * the same number of items, otherwise the single object will be evaluated
     * and the returned iterable will have at most one item.
     *
     * @param viewClass the type of views to create.
     * @param object the object used to create the views.
     * @param <V> the view type.
     * @return Never {@code null}.
     */
    protected final <V> Iterable<V> createViews(Class<V> viewClass, Object object) {

        Iterable<?> models;

        if (object instanceof Iterable) {
            models = (Iterable<?>) object;

        } else if (object != null) {
            models = Collections.singleton(object);

        } else {
            models = Collections.emptyList();
        }

        return StreamSupport.stream(models.spliterator(), false)
                .map(model -> unwrapModel(model, new HashSet<>()))
                .map(model -> {
                    Class<? extends ViewModel<? super Object>> viewModelClass = findViewModelClassHelper(viewClass, null, model, true);
                    if (viewModelClass != null) {

                        ViewModel<? super Object> viewModel = viewModelCreator.createViewModel(viewModelClass, model, viewResponse);

                        if (viewModel != null && viewClass.isAssignableFrom(viewModel.getClass())) {

                            @SuppressWarnings("unchecked")
                            V view = (V) viewModel;
                            return view;
                        }
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Creates a view of type {@code viewClass} that is bound to the given
     * {@code model}.
     *
     * @param viewClass the type of view to create.
     * @param model the model used to create the view.
     * @param <V> the view type.
     * @return a newly created view.
     */
    protected final <V> V createView(Class<V> viewClass, Object model) {
        Iterator<V> views = createViews(viewClass, model).iterator();
        return views.hasNext() ? views.next() : null;
    }

    /**
     * Creates a view that is bound to the given {@code viewType} and
     * {@code model}.
     *
     * @param viewType the view type key bound to the view and model.
     * @param model the model used to create the view.
     * @return a newly created view.
     */
    protected final Object createView(String viewType, Object model) {

        model = unwrapModel(model, new HashSet<>());

        Class<? extends ViewModel<? super Object>> viewModelClass = findViewModelClassHelper(null, viewType, model, true);
        if (viewModelClass != null) {

            return viewModelCreator.createViewModel(viewModelClass, model, viewResponse);
        }

        return null;
    }

    // Recursively unwraps a ModelWrapper while detecting cyclic references.
    private Object unwrapModel(Object model, Set<Object> unwrapped) {
        if (model instanceof ModelWrapper && unwrapped.add(model)) {
            return unwrapModel(((ModelWrapper) model).unwrap(), unwrapped);
        } else {
            return model;
        }
    }

    /**
     * The default view model creator.
     */
    public static class DefaultCreator implements ViewModelCreator {

        @Override
        public final <M, VM extends ViewModel<? super M>> VM createViewModel(Class<VM> viewModelClass, M model, ViewResponse viewResponse) {

            if (findViewModelClassHelper(viewModelClass, null, model, false) != null) {

                VM viewModel = TypeDefinition.getInstance(viewModelClass).newInstance();

                ((ViewModel<? super M>) viewModel).viewModelCreator = this;
                ((ViewModel<? super M>) viewModel).viewResponse = viewResponse;

                viewModel.model = model;

                beforeViewModelOnCreate(viewModel);

                if (!viewModel.shouldCreate()) {
                    return null;
                }

                viewModel.onCreate(viewResponse);

                return viewModel;
            }

            return null;
        }

        /**
         * Called immediately before {@link ViewModel#onCreate(ViewResponse)}
         * is invoked. Sub-classes may override this method to further
         * initialize the {@link ViewModel}.
         *
         * @param viewModel the viewModel to modify.
         * @param <M> the model type for the ViewModel.
         * @param <VM> the ViewModel type.
         */
        protected <M, VM extends ViewModel<? super M>> void beforeViewModelOnCreate(VM viewModel) {
            // do nothing by default
        }
    }

    /**
     * Finds an appropriate ViewModel class based on the given view class, and
     * model. If more than one class is found, the result is ambiguous and null
     * is returned.
     *
     * @param viewClass the desired compatible class of the returned view model.
     * @param model the model used to look up available view model classes, that is also compatible with the returned view model class.
     * @param <M> the model type
     * @param <V> the view type
     * @return the view model class that matches the bounds of the arguments.
     */
    public static <M, V> Class<? extends ViewModel<? super M>> findViewModelClass(Class<V> viewClass, M model) {
        return findViewModelClassHelper(viewClass, null, model, false);
    }

    /**
     * Finds an appropriate ViewModel class based on the given view type, and
     * model. If more than one class is found, the result is ambiguous and null
     * is returned.
     *
     * @param viewType the desired view type that is bound to the returned view model.
     * @param model the model used to look up available view model classes, that is also compatible with the returned view model class.
     * @param <M> the model type
     * @return the view model class that matches the bounds of the arguments.
     */
    public static <M> Class<? extends ViewModel<? super M>> findViewModelClass(String viewType, M model) {
        return findViewModelClassHelper(null, viewType, model, false);
    }

    private static <M, V> Class<? extends ViewModel<? super M>> findViewModelClassHelper(Class<V> viewClass, String viewType, M model, boolean logFailure) {

        if (model == null) {
            return null;
        }

        Class<? extends ViewModel<? super M>> viewModelClass = findViewModelClassHelper(viewClass, viewType, model);

        if (viewModelClass == null && logFailure) {
            String message = String.format("Could not find ViewModel class for model of type [%s] and view of type [%s].",
                    model.getClass().getName(),
                    Stream.of(viewClass != null ? viewClass.getName() : null, viewType).filter(Objects::nonNull).collect(Collectors.joining(" and")));

            LOGGER.warn(message, new IllegalArgumentException());
        }

        return viewModelClass;
    }

    private static <M, V> Class<? extends ViewModel<? super M>> findViewModelClassHelper(Class<V> viewClass, String viewType, M model) {

        if (model == null) {
            return null;
        }

        LoadingCache<Object, Optional<Class<?>>> viewTypes = VIEW_BINDINGS.getUnchecked(model.getClass());

        if (viewTypes != null) {
            Object viewTypeObject = null;

            if (viewClass != null) {
                viewTypeObject = viewClass;

            } else if (viewType != null) {
                viewTypeObject = viewType;
            }

            if (viewTypeObject != null) {
                Optional<Class<?>> viewModelClass = viewTypes.getUnchecked(viewTypeObject);

                if (viewModelClass.isPresent()) {
                    return (Class<? extends ViewModel<? super M>>) viewModelClass.get();
                }
            }
        }

        return null;
    }

    private static Class<? extends ViewModel<?>> findViewModelClassCacheHelper(Class<?> viewClass, String viewType, Class<?> modelClass) {

        if (modelClass == null) {
            return null;
        }

        // if it's a concrete view model class, with no type specified, then just verify that the model types match.
        if (viewClass != null && viewType == null
                && ViewModel.class.isAssignableFrom(viewClass)
                && !Modifier.isAbstract(viewClass.getModifiers())) {

            Class<?> declaredModelClass = TypeDefinition.getInstance(viewClass).getInferredGenericTypeArgumentClass(ViewModel.class, 0);

            if (declaredModelClass != null && declaredModelClass.isAssignableFrom(modelClass)) {

                @SuppressWarnings("unchecked")
                Class<? extends ViewModel<?>> viewModelClass = (Class<? extends ViewModel<?>>) viewClass;

                return viewModelClass;

            } else {
                return null;
            }
        }

        // Attempt automatic ViewBinding.
        Class<?> concreteViewModelClass = null;

        // If it's a class with no type specified, try to find a single
        // compatible concrete ViewModel class using the following rules,
        // otherwise, do a lookup of the view bindings.
        // Rules:
        // 1. Do NOT include ViewModels that implement UnboundView.
        // 2. Do NOT include ViewModels whose generic type argument (Model) has
        //    @ViewBinding annotations set DIRECTLY on it.
        // 3. If there are multiple valid ViewModel classes, check their
        //    inheritance hierarchy and the one that extends the rest should
        //    win. If they're not related, move to step 4.
        // 4. If there are multiple valid ViewModel classes with differing
        //    inheritance hierarchies then choose the one whose generic type
        //    argument (the model) is "closest" to the model class argument
        //    passed to this method, where "closest" is defined by the sorting
        //    the model class hierarchy using the C3 linearization algorithm
        //    (https://en.wikipedia.org/wiki/C3_linearization) and finding the
        //    one earliest in the list that matches. If there is still no
        //    winner OR the class hierarchy cannot be linearized a warning
        //    will be logged due to an ambiguous result.
        if (viewClass != null && viewType == null) {

            Set<Class<?>> concreteViewClasses = new HashSet<>(ClassFinder.findConcreteClasses(viewClass));

            // ClassFinder only finds sub-classes, so if the current viewClass is also concrete, add it to the set.
            if (!viewClass.isInterface() && !Modifier.isAbstract(viewClass.getModifiers())) {
                concreteViewClasses.add(viewClass);
            }

            Set<Class<?>> concreteViewModelClasses = concreteViewClasses
                    .stream()
                    // It must be a sub-class of ViewModel
                    .filter(ViewModel.class::isAssignableFrom)
                    // It should NOT implement UnboundView (Rule #1)
                    .filter(concreteClass -> !UnboundView.class.isAssignableFrom(concreteClass))
                    // It must have the correct generic type argument for its model, and that model must not have any ViewBindings (Rule #2).
                    .filter(concreteClass -> {
                        Class<?> declaredModelClass = TypeDefinition.getInstance(concreteClass).getInferredGenericTypeArgumentClass(ViewModel.class, 0);
                        return declaredModelClass != null
                                && declaredModelClass.isAssignableFrom(modelClass)
                                && declaredModelClass.getAnnotationsByType(ViewBinding.class).length == 0;
                    })
                    .collect(Collectors.toSet());

            // Eliminate any super classes if there are sub-class / super-class
            // combinations in the set since the sub-class takes precedence (Rule #3).
            Set<Class<?>> superClassesToRemove = new HashSet<>();

            for (Class<?> concreteClass : concreteViewModelClasses) {

                Set<Class<?>> superClasses = new HashSet<>();

                Class<?> superClass = concreteClass.getSuperclass();

                while (superClass != null) {
                    superClasses.add(superClass);
                    superClass = superClass.getSuperclass();
                }

                superClassesToRemove.addAll(superClasses);
            }

            concreteViewModelClasses.removeAll(superClassesToRemove);

            // If there is exactly one concrete view model class left, then it is automatically bound.
            if (concreteViewModelClasses.size() == 1) {
                concreteViewModelClass = concreteViewModelClasses.iterator().next();

            } else if (concreteViewModelClasses.size() > 1) {

                // If there is still more than 1, calculate the C3 linearization
                // of the model class hierarchy and choose the ViewModel(s) whose
                // generic type argument appears first in the list (Rule #4).

                // Collect the view model classes based on their generic model class.
                Map<Class<?>, Set<Class<?>>> modelToViewModels = new HashMap<>();

                for (Class<?> viewModelClass : concreteViewModelClasses) {

                    Class<?> genericModelClass = TypeDefinition.getInstance(viewModelClass).getInferredGenericTypeArgumentClass(ViewModel.class, 0);

                    if (genericModelClass != null) {
                        modelToViewModels.computeIfAbsent(genericModelClass, k -> new HashSet<>()).add(viewModelClass);
                    }
                }

                // Loop through the C3 linearized modelClass hierarchy and find
                // the first view model(s) that match
                try {
                    for (Class<?> next : c3LinearizeClass(modelClass)) {
                        Set<Class<?>> viewModelClasses = modelToViewModels.get(next);

                        if (viewModelClasses != null) {

                            if (viewModelClasses.size() == 1) {
                                concreteViewModelClass = viewModelClasses.iterator().next();
                            }

                            concreteViewModelClasses = viewModelClasses;
                            break;
                        }
                    }

                } catch (RuntimeException e) {
                    LOGGER.warn("Could not linearize the class hierarchy for model type [{}] to disambiguate view model bindings.");
                }
            }

            if (concreteViewModelClasses.size() > 1) {
                // More than one valid class found, log a warning and short circuit (Rule #3).
                LOGGER.warn("Found [{}] conflicting view model bindings for model type [{}] and view type [{}]: [{}]",
                        new Object[] {
                                concreteViewModelClasses.size(),
                                modelClass,
                                viewClass.getName(),
                                concreteViewModelClasses.stream().map(Class::getName).collect(Collectors.joining(", "))
                        });
                return null;
            }
        }

        // if a single concrete view model class was found, then return.
        if (concreteViewModelClass != null) {

            @SuppressWarnings("unchecked")
            Class<? extends ViewModel<?>> viewModelClass = (Class<? extends ViewModel<?>>) concreteViewModelClass;
            return viewModelClass;

        } else { // do a lookup of the view bindings on the model.

            Map<Class<?>, List<Class<? extends ViewModel>>> modelToViewModelClassMap = new HashMap<>();

            Set<Class<? extends ViewModel>> allViewModelClasses = new LinkedHashSet<>();

            for (Class<?> annotatableClass : ViewUtils.getAnnotatableClasses(modelClass)) {

                allViewModelClasses.addAll(Arrays.stream(annotatableClass.getAnnotationsByType(ViewBinding.class))

                        // check that it matches the view type if it exists
                        .filter(viewBinding -> viewType == null || Arrays.asList(viewBinding.types()).contains(viewType))

                        // get the annotation's view model class
                        .map(ViewBinding::value)

                        .collect(Collectors.toList()));
            }

            allViewModelClasses.forEach(viewModelClass -> {

                TypeDefinition<? extends ViewModel> typeDef = TypeDefinition.getInstance(viewModelClass);

                Class<?> declaredModelClass = typeDef.getInferredGenericTypeArgumentClass(ViewModel.class, 0);

                if (declaredModelClass != null && declaredModelClass.isAssignableFrom(modelClass)
                        && (viewClass == null || viewClass.isAssignableFrom(viewModelClass))) {

                    List<Class<? extends ViewModel>> viewModelClasses = modelToViewModelClassMap.get(declaredModelClass);
                    if (viewModelClasses == null) {
                        viewModelClasses = new ArrayList<>();
                        modelToViewModelClassMap.put(declaredModelClass, viewModelClasses);
                    }
                    viewModelClasses.add(viewModelClass);
                }
            });

            if (!modelToViewModelClassMap.isEmpty()) {

                Set<Class<?>> nearestModelClasses = ViewUtils.getNearestSuperClassesInSet(modelClass, modelToViewModelClassMap.keySet());
                if (nearestModelClasses.size() == 1) {

                    List<Class<? extends ViewModel>> viewModelClasses = modelToViewModelClassMap.get(nearestModelClasses.iterator().next());
                    if (viewModelClasses.size() == 1) {
                        @SuppressWarnings("unchecked")
                        Class<? extends ViewModel<?>> viewModelClass = (Class<? extends ViewModel<?>>) viewModelClasses.get(0);

                        return viewModelClass;
                    } else {
                        LOGGER.warn("Found [{}] conflicting view model bindings for model type [{}] and view type [{}]: [{}]",
                                new Object[] {
                                        viewModelClasses.size(),
                                        modelClass,
                                        viewClass != null ? viewClass.getName() : null,
                                        viewModelClasses.stream().map(Class::getName).collect(Collectors.joining(", "))
                                });
                    }
                } else {
                    Set<Class<? extends ViewModel>> conflictingViewModelClasses = new LinkedHashSet<>();
                    for (Class<?> nearestModelClass : nearestModelClasses) {
                        conflictingViewModelClasses.addAll(modelToViewModelClassMap.get(nearestModelClass));
                    }
                    LOGGER.warn("Found [{}] conflicting view model bindings for model type [{}] and view type [{}]: [{}]",
                            new Object[] {
                                    conflictingViewModelClasses.size(),
                                    modelClass.getName(),
                                    viewClass != null ? viewClass.getName() : null,
                                    conflictingViewModelClasses.stream().map(Class::getName).collect(Collectors.joining(", "))
                            });
                }
            }
        }

        return null;
    }

    // https://en.wikipedia.org/wiki/C3_linearization
    private static List<Class<?>> c3LinearizeClass(Class<?> source) {
        return c3Linearize(source, child -> {

            List<Class<?>> parents = new ArrayList<>();

            // super class first...
            Class<?> superClass = child.getSuperclass();
            if (superClass != null && superClass != Object.class) {
                parents.add(superClass);
            }

            // interfaces second...
            parents.addAll(Arrays.asList(child.getInterfaces()));

            // and Object last for all top level classes and interfaces.
            // This is to ensure Object always ends up last.
            if (child != Object.class && (superClass == null || superClass == Object.class)) {
                parents.add(Object.class);
            }

            return parents;

        }, new HashSet<>());
    }

    private static <T> List<T> c3Linearize(T source, Function<T, List<T>> parentsFunction, Set<T> visited) {

        // Guard against stack overflow.
        if (!visited.add(source)) {
            throw new IllegalStateException("Cyclic hierarchy detected.");
        }

        // Store the linearization result.
        List<T> result = new ArrayList<>();

        // The source is always first.
        result.add(source);

        // Collect the source's direct parents.
        List<T> sourceParents = new ArrayList<>(parentsFunction.apply(source));

        if (!sourceParents.isEmpty()) {

            // Linearize each parent and add the result to merge list.
            List<List<T>> toMerge = sourceParents.stream()
                    .map(parent -> c3Linearize(parent, parentsFunction, new HashSet<>(visited)))
                    .collect(Collectors.toCollection(ArrayList::new));

            // Add the source parents as the last item in the merge list.
            toMerge.add(sourceParents);

            // Merge and add to result.
            result.addAll(c3merge(toMerge));
        }

        return result;
    }

    private static <T> List<T> c3merge(List<List<T>> lists) {

        List<T> merged = new ArrayList<>();

        // while the lists are not empty
        while (lists.stream().map(List::size).mapToInt(i -> i).sum() > 0) {

            // grab the first item from each list
            List<T> candidates = new ArrayList<>(lists.stream()
                    .filter(list -> !list.isEmpty())
                    .map(list -> list.get(0))
                    .collect(Collectors.toCollection(LinkedHashSet::new)));

            // find the first candidate that is not present in the tail of any of the lists
            T candidate = candidates.stream()
                    .filter(c -> lists.stream()
                            .allMatch(list -> list.size() <= 1 || !list.subList(1, list.size()).contains(c)))
                    .findFirst().orElse(null);

            if (candidate != null) {
                // remove the candidate from each list and add it the merge list.
                lists.forEach(list -> list.remove(candidate));
                merged.add(candidate);

            } else {
                throw new IllegalStateException("Cyclic hierarchy detected.");
            }
        }

        return merged;
    }

    /**
     * @deprecated Use {@link #findViewModelClass(Class, Object)} or {@link #findViewModelClass(String, Object)} instead.
     */
    @Deprecated
    public static <M, V> Class<? extends ViewModel<? super M>> findViewModelClass(Class<V> viewClass, String viewType, M model) {
        return findViewModelClassHelper(viewClass, viewType, model, false);
    }
}
