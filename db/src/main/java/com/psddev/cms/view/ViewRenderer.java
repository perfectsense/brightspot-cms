package com.psddev.cms.view;

import com.psddev.dari.util.TypeDefinition;

import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A renderer of views.
 */
public interface ViewRenderer {

    /**
     * Renders a view, storing the result.
     *
     * @param view the view to render.
     * @return the result of rendering a view.
     */
    ViewOutput render(Object view);

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
            view = ((ViewMap) view).getView();
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
                    @Override
                    public ViewOutput render(Object view) {
                        return view instanceof Map ? renderer.render(view) : renderer.render(new ViewMap(view));
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
