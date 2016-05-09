package com.psddev.cms.view;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @deprecated Use {@link ViewBinding} instead.
 */
@Deprecated
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(ViewMappings.class)
public @interface ViewMapping {

    /**
     * @return the ViewCreator class that can be used to create views from the
     * annotated model class.
     */
    Class<? extends ViewCreator> value();

    String[] types() default { };
}
