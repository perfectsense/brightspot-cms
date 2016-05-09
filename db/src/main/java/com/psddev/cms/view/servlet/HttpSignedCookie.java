package com.psddev.cms.view.servlet;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.servlet.http.HttpServletRequest;

import com.psddev.dari.util.JspUtils;
import com.psddev.dari.util.StringUtils;

/**
 * Populates a field with the signed cookie value from an HTTP request. The
 * cookie fetched has the same name as the field it populates unless otherwise
 * specified. An optional expiration duration in milliseconds can be defined
 * such that if the specified duration has passed the field value will be null.
 */
@ServletViewRequestAnnotationProcessorClass(HttpSignedCookieProcessor.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface HttpSignedCookie {
    String value() default "";
    long expiry() default 0L;
}

class HttpSignedCookieProcessor implements ServletViewRequestAnnotationProcessor<HttpSignedCookie> {

    @Override
    public Object getValue(HttpServletRequest request, String fieldName, HttpSignedCookie annotation) {

        String cookieName = annotation.value();
        if (StringUtils.isBlank(cookieName)) {
            cookieName = fieldName;
        }

        String value;
        if (annotation.expiry() > 0) {
            value = JspUtils.getSignedCookieWithExpiry(request, cookieName, annotation.expiry());
        } else {
            value = JspUtils.getSignedCookie(request, cookieName);
        }

        return value;
    }
}
