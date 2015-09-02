package com.psddev.cms.view;

import java.lang.annotation.Annotation;

/**
 * Processes an annotation in order to create a ViewRenderer.
 *
 * @param <A> the annotation type to process.
 */
public interface ViewRendererAnnotationProcessor<A extends Annotation> {

    /**
     * Creates a ViewRenderer based on the specified annotation.
     *
     * @param viewClass the class the annotation is being applied to.
     * @param annotation the annotation to process.
     * @return the ViewRenderer created based on the annotation.
     */
    ViewRenderer createRenderer(Class<?> viewClass, A annotation);
}
