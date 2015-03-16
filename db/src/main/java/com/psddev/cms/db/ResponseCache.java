package com.psddev.cms.db;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.psddev.dari.db.ObjectType;

@ObjectType.AnnotationProcessorClass(ResponseCacheAnnotationProcessor.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ResponseCache {
    int timeout() default 0;
}

class ResponseCacheAnnotationProcessor implements ObjectType.AnnotationProcessor<ResponseCache> {

    @Override
    public void process(ObjectType type, ResponseCache annotation) {
        type.as(ResponseCacheTypeModification.class).setTimeout(annotation.timeout());
    }
}
