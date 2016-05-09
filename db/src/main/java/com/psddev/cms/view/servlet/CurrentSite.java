package com.psddev.cms.view.servlet;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.servlet.http.HttpServletRequest;

import com.psddev.cms.db.PageFilter;

/**
 * Populates a field with the "current site" of the request as defined by
 * {@link com.psddev.cms.db.PageFilter.Static#getSite(javax.servlet.http.HttpServletRequest)}.
 */
@ServletViewRequestAnnotationProcessorClass(CurrentSiteProcessor.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CurrentSite {
}

class CurrentSiteProcessor implements ServletViewRequestAnnotationProcessor<CurrentSite> {

    @Override
    public Object getValue(HttpServletRequest request, String fieldName, CurrentSite annotation) {
        return PageFilter.Static.getSite(request);
    }
}
