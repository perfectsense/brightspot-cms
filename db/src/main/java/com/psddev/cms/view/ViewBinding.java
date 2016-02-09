package com.psddev.cms.view;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a ViewModel class that can be used to bind to views from the
 * annotated model class. The ViewModel class implicitly specifies the View
 * type that it can be bound to as well as the model it is bound from. Multiple
 * bindings can be placed on the model.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Repeatable(ViewBindings.class)
public @interface ViewBinding {

    /**
     * @return the ViewModel class that can be bound to views from the
     * annotated model class.
     */
    Class<? extends ViewModel> value();

    String[] types() default { };
}
