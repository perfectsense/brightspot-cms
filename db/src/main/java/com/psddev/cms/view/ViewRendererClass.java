package com.psddev.cms.view;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for view types that specifies the ViewRenderer type that
 * should be used to render the view.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ViewRendererClass {

    /**
     * @return the view renderer class.
     */
    Class<? extends ViewRenderer> value();
}
