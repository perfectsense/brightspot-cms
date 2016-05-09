package com.psddev.cms.view.servlet;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.servlet.http.HttpServletRequest;

/**
 * Populates a field with the HTTP method value used to make an HTTP request.
 * Typically, GET or POST.
 */
@ServletViewRequestAnnotationProcessorClass(HttpMethodProcessor.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface HttpMethod {
}

class HttpMethodProcessor implements ServletViewRequestAnnotationProcessor<HttpMethod> {

    @Override
    public Object getValue(HttpServletRequest request, String fieldName, HttpMethod annotation) {
        return request.getMethod();
    }
}
