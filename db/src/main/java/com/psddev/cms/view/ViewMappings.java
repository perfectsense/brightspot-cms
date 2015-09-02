package com.psddev.cms.view;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The wrapper annotation for the repeatable {@link com.psddev.cms.view.ViewMapping}
 * annotation.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ViewMappings {

    /**
     * Specifies the list of {@link com.psddev.cms.view.ViewMapping}
     * annotations to be applied.
     */
    ViewMapping[] value();
}
