package com.psddev.cms.view.servlet;

import java.lang.annotation.Annotation;

import javax.servlet.http.HttpServletRequest;

/**
 * Processes an annotation in order to produce a value that is populated on a
 * view request field.
 * @param <A>
 */
public interface ServletViewRequestAnnotationProcessor<A extends Annotation> {

    /**
     * Gets a value to be populated in the field with name {@code fieldName} on
     * a view request object.
     *
     * @param request the current request.
     * @param fieldName the name of the field to be populated.
     * @param annotation the annotation that lives on the field.
     * @return the value that will be populated in the field.
     */
    Object getValue(HttpServletRequest request, String fieldName, A annotation);
}
