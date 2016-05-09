package com.psddev.cms.view.servlet;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.psddev.dari.util.JspUtils;
import com.psddev.dari.util.StringUtils;

/**
 * Populates a field with the cookie value from an HTTP request. The cookie
 * fetched has the same name as the field it populates unless otherwise
 * specified.
 */
@ServletViewRequestAnnotationProcessorClass(HttpCookieProcessor.class)
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface HttpCookie {
    String value() default "";
}

class HttpCookieProcessor implements ServletViewRequestAnnotationProcessor<HttpCookie> {

    @Override
    public Object getValue(HttpServletRequest request, String fieldName, HttpCookie annotation) {

        String cookieName = annotation.value();
        if (StringUtils.isBlank(cookieName)) {
            cookieName = fieldName;
        }

        Cookie cookie = JspUtils.getCookie(request, cookieName);
        if (cookie != null) {
            return cookie.getValue();
        }

        return null;
    }
}
