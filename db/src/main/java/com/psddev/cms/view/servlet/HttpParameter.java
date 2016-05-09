package com.psddev.cms.view.servlet;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

import com.psddev.dari.util.StringUtils;

/**
 * Populates a field with the query parameter value from an HTTP request. The
 * parameter fetched has the same name as the field it populates unless
 * otherwise specified.
 */
@ServletViewRequestAnnotationProcessorClass(HttpParameterProcessor.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface HttpParameter {
    String value() default "";
}

class HttpParameterProcessor implements ServletViewRequestAnnotationProcessor<HttpParameter> {

    @Override
    public Object getValue(HttpServletRequest request, String fieldName, HttpParameter annotation) {

        String parameterName = annotation.value();
        if (StringUtils.isBlank(parameterName)) {
            parameterName = fieldName;
        }

        String[] parameterValues = request.getParameterValues(parameterName);

        return parameterValues != null ? Arrays.asList(parameterValues) : Collections.emptyList();
    }
}
