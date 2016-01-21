package com.psddev.cms.db;

import com.psddev.dari.db.Record;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

public abstract class RichTextElement extends Record {

    public static final String ROOT_CONTEXT = "rte.root.context";

    public abstract void fromAttributes(Map<String, String> attributes);

    public abstract Map<String, String> toAttributes();

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Tag {

        String value();
        boolean voidElement() default false;
        boolean editForm() default true;
        String[] allowedContexts() default { };
        String subMenu() default "";
    }
}
