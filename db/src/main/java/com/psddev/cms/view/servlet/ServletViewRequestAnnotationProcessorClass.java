package com.psddev.cms.view.servlet;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that is placed on the annotations of view request fields.
 * Specifies the class that will read the view request annotation and produce
 * a value to be populated on the view request field based on an
 * HttpServletRequest.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface ServletViewRequestAnnotationProcessorClass {

    /**
     * @return the class that will process the annotation on which this
     * annotation lives.
     */
    Class<? extends ServletViewRequestAnnotationProcessor<? extends Annotation>> value();
}
