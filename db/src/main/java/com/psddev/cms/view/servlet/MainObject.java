package com.psddev.cms.view.servlet;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.servlet.http.HttpServletRequest;

import com.psddev.cms.db.PageFilter;

/**
 * Populates a field with the "main object" of the request as defined by
 * {@link com.psddev.cms.db.PageFilter.Static#getMainObject(javax.servlet.http.HttpServletRequest)}.
 */
@ServletViewRequestAnnotationProcessorClass(MainObjectProcessor.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MainObject {
}

class MainObjectProcessor implements ServletViewRequestAnnotationProcessor<MainObject> {

    @Override
    public Object getValue(HttpServletRequest request, String fieldName, MainObject annotation) {
        return PageFilter.Static.getMainObject(request);
    }
}
