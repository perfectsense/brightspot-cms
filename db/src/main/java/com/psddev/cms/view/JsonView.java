package com.psddev.cms.view;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker for view classes that render JSON output using {@link com.psddev.cms.view.JsonViewRenderer}
 */
@ViewRendererAnnotationProcessorClass(JsonViewRendererAnnotationProcessor.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface JsonView {
}
