package com.psddev.cms.db;

import com.psddev.cms.view.ViewCreator;
import com.psddev.cms.view.ViewRequest;
import com.psddev.dari.util.ObjectUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

/**
 * ViewRequest implementation that uses the Java Servlet Spec for handling HTTP
 * requests.
 */
class ServletViewRequest implements ViewRequest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServletViewRequest.class);

    private HttpServletRequest request;

    public ServletViewRequest(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public <V> V createView(Class<V> viewClass, Object model) {

        ViewCreator<Object, V> vc = ViewCreator.createCreator(model, viewClass);

        if (vc != null) {
            return vc.createView(model, this);

        } else {
            LOGGER.warn("Could not find view creator of [" + viewClass
                    + "] for object of type [" + (model != null ? model.getClass() : "null") + "]!");
            return null;
        }
    }

    @Override
    public <T> Stream<T> getParameter(Class<T> returnType, String name) {
        String[] values = request.getParameterValues(name);
        return values != null ? Arrays.stream(values).map((param) -> ObjectUtils.to(returnType, param)) : Stream.empty();
    }
}
