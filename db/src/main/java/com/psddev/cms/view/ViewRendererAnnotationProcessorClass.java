package com.psddev.cms.view;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that is placed on the annotations of views. Specifies the
 * class that that will read the view annotation and create a ViewRenderer
 * that will be used to render the view. This is a hook to create custom
 * annotations that define ViewRenderers as opposed to the more direct
 * {@link com.psddev.cms.view.ViewRendererClass} annotation.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface ViewRendererAnnotationProcessorClass {

    /**
     * @return the class that will process annotation on which this
     * annotation lives.
     */
    Class<? extends ViewRendererAnnotationProcessor<? extends Annotation>> value();
}
