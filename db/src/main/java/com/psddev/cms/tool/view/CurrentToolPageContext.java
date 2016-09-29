package com.psddev.cms.tool.view;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.servlet.http.HttpServletRequest;

import com.psddev.cms.tool.ToolPageContext;
import com.psddev.cms.view.servlet.ServletViewRequestAnnotationProcessor;
import com.psddev.cms.view.servlet.ServletViewRequestAnnotationProcessorClass;
import com.psddev.dari.util.PageContextFilter;

/**
 * Populates a field with the {@link ToolPageContext} of the request.
 */
@ServletViewRequestAnnotationProcessorClass(CurrentToolPageContextProcessor.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CurrentToolPageContext {
}

class CurrentToolPageContextProcessor implements ServletViewRequestAnnotationProcessor<CurrentToolPageContext> {

    @Override
    public ToolPageContext getValue(HttpServletRequest request, String fieldName, CurrentToolPageContext annotation) {
        return new ToolPageContext(request.getServletContext(), request, PageContextFilter.Static.getResponse());
    }
}
