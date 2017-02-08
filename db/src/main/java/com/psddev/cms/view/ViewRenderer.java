package com.psddev.cms.view;

import com.psddev.cms.db.PageFilter;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.State;
import com.psddev.dari.util.PageContextFilter;
import com.psddev.dari.util.StringUtils;
import com.psddev.dari.util.TypeDefinition;

import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A renderer of views.
 */
public interface ViewRenderer {

    /**
     * @deprecated Use {@link ViewRenderer#render(Object, ViewTemplateLoader)} instead.
     */
    @Deprecated
    default ViewOutput render(Object view) {
        throw new UnsupportedOperationException("Must implement ViewRenderer#render(Object, ViewTemplateLoader)!");
    }

    /**
     * Renders a view, storing the result.
     *
     * @param view the view to render.
     * @param templateLoader the template loader.
     * @return the result of rendering a view.
     */
    default ViewOutput render(Object view, ViewTemplateLoader templateLoader) {
        return render(view);
    }

    /**
     * Creates an appropriate ViewRenderer based on the specified view.
     *
     * @param view the view from which to create a view renderer.
     * @return the view renderer for the specified view.
     */
    static ViewRenderer createRenderer(Object view) {

        if (view == null) {
            return null;
        }

        if (view instanceof ViewMap) {
            view = ((ViewMap) view).toView();
        }

        // we expect a list of size 1
        List<ViewRenderer> renderers = new ArrayList<>();

        for (Class<?> viewClass : ViewUtils.getAnnotatableClasses(view.getClass())) {

            ViewRendererClass rendererAnnotation = viewClass.getAnnotation(ViewRendererClass.class);
            if (rendererAnnotation != null) {
                Class<? extends ViewRenderer> rendererClass = rendererAnnotation.value();

                if (rendererClass != null) {

                    try {
                        ViewRenderer renderer = TypeDefinition.getInstance(rendererClass).newInstance();

                        if (renderer != null) {
                            renderers.add(renderer);
                        }

                    } catch (Exception e) {
                        LoggerFactory.getLogger(ViewRenderer.class)
                                .warn("Unable to create instance of renderer of type ["
                                        + rendererClass.getName() + "]");
                    }
                }
            }

            // check for annotation processors.
            for (Annotation viewAnnotation : viewClass.getAnnotations()) {

                Class<?> annotationClass = viewAnnotation.annotationType();

                ViewRendererAnnotationProcessorClass annotation = annotationClass.getAnnotation(
                        ViewRendererAnnotationProcessorClass.class);

                if (annotation != null) {

                    Class<? extends ViewRendererAnnotationProcessor<? extends Annotation>> annotationProcessorClass = annotation.value();

                    if (annotationProcessorClass != null) {

                        @SuppressWarnings("unchecked")
                        ViewRendererAnnotationProcessor<Annotation> annotationProcessor
                                = (ViewRendererAnnotationProcessor<Annotation>) TypeDefinition.getInstance(annotationProcessorClass).newInstance();

                        ViewRenderer renderer = annotationProcessor.createRenderer(view.getClass(), viewAnnotation);

                        if (renderer != null) {
                            renderers.add(renderer);
                        }
                    }
                }
            }
        }

        if (!renderers.isEmpty()) {

            if (renderers.size() == 1) {
                ViewRenderer renderer = renderers.get(0);

                // wrap the view renderer so that it always converts the view to a ViewMap
                // before delegating to the actual renderer if it's not already a map.
                return new ViewRenderer() {

                    @Deprecated
                    @Override
                    public ViewOutput render(Object view) {
                        return createViewOutput(
                                view,
                                () -> view instanceof Map
                                        ? renderer.render(view)
                                        : renderer.render(new ViewMap(view)));
                    }

                    @Override
                    public ViewOutput render(Object view, ViewTemplateLoader loader) {
                        return createViewOutput(
                                view,
                                () -> view instanceof Map
                                        ? renderer.render(view, loader)
                                        : renderer.render(new ViewMap(view), loader));
                    }

                    private ViewOutput createViewOutput(Object view, Supplier<ViewOutput> viewOutputSupplier) {
                        HttpServletRequest request = PageContextFilter.Static.getRequestOrNull();
                        HttpServletResponse response = PageContextFilter.Static.getResponseOrNull();
                        String contentType = response != null ? response.getContentType() : null;

                        if (request == null
                                || !PageFilter.Static.isInlineEditingAllContents(request)
                                || (contentType != null
                                    && !StringUtils.ensureEnd(contentType, ";").startsWith("text/html;"))) {

                            return viewOutputSupplier.get();
                        }

                        if (view instanceof ViewMap) {
                            view = ((ViewMap) view).toView();
                        }

                        if (!(view instanceof ViewModel)) {
                            return viewOutputSupplier.get();
                        }

                        Object model = ((ViewModel) view).model;

                        PageFilter.Static.pushObject(request, model);

                        try {
                            Map<String, String> map = new HashMap<>();
                            Object concrete = PageFilter.Static.peekConcreteObject(request);

                            if (concrete != null) {
                                State state = State.getInstance(concrete);
                                ObjectType stateType = state.getType();

                                map.put("id", state.getId().toString());

                                if (stateType != null) {
                                    map.put("typeLabel", stateType.getLabel());
                                }

                                try {
                                    map.put("label", state.getLabel());

                                } catch (RuntimeException error) {
                                    // Not a big deal if label can't be retrieved.
                                }
                            }

                            String viewOutput = viewOutputSupplier.get().get();

                            return () -> PageFilter.createMarkerHtml("BrightspotCmsObjectBegin", map)
                                    + (viewOutput != null ? viewOutput : "")
                                    + PageFilter.createMarkerHtml("BrightspotCmsObjectEnd", null);

                        } finally {
                            PageFilter.Static.popObject(request);

                        }
                    }
                };

            } else {
                LoggerFactory.getLogger(ViewRenderer.class)
                        .warn("Found multiple renderers for view of type [" + view.getClass().getName() + "]!");
                return null;
            }

        } else {
            return null;
        }
    }
}
