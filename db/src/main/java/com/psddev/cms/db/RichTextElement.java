package com.psddev.cms.db;

import com.psddev.dari.db.Record;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

public abstract class RichTextElement extends Record {

    public abstract void fromAttributes(Map<String, String> attributes);

    public abstract Map<String, String> toAttributes();

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Tag {

        String value();
        boolean empty() default false;
        boolean root() default true;
        String[] parents() default { };
        String menu() default "";
    }
}
