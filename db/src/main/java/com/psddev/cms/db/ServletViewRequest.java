package com.psddev.cms.db;

import java.util.Arrays;
import java.util.stream.Stream;

import com.psddev.cms.view.ViewCreator;
import com.psddev.cms.view.ViewRequest;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.TypeDefinition;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @deprecated no replacement.
 */
@Deprecated
class ServletViewRequest implements ViewRequest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServletViewRequest.class);

    private HttpServletRequest request;

    public ServletViewRequest(HttpServletRequest request) {
        this.request = request;
    }

    @Override
    public <V> V createView(Class<V> viewClass, Object model) {

        Class<? extends ViewCreator<? super Object, V, ? super ServletViewRequest>> viewCreatorClass = ViewCreator.findCreatorClass(model, viewClass, null, this);
        if (viewCreatorClass != null) {
            ViewCreator<? super Object, ? extends V, ? super ServletViewRequest> vc = TypeDefinition.getInstance(viewCreatorClass).newInstance();
            return vc.createView(model, this);

        } else {
            LOGGER.warn("Could not find view creator for view of type [" + viewClass.getName()
                    + "] and object of type [" + (model != null ? model.getClass() : "null") + "]!");
            return null;
        }
    }

    @Override
    public Object createView(String viewType, Object model) {

        Class<? extends ViewCreator<? super Object, ?, ? super ServletViewRequest>> viewCreatorClass = ViewCreator.findCreatorClass(model, null, viewType, this);
        if (viewCreatorClass != null) {
            ViewCreator<? super Object, ?, ? super ServletViewRequest> vc = TypeDefinition.getInstance(viewCreatorClass).newInstance();
            return vc.createView(model, this);

        } else {
            LOGGER.warn("Could not find view creator for view of type [" + viewType
                    + "] and object of type [" + (model != null ? model.getClass() : "null") + "]!");
            return null;
        }
    }

    // To support backward compatibility from 3.1 when the concept of namespace
    // had not yet been introduced.
    @Deprecated
    public <T> Stream<T> getParameter(Class<T> returnType, String name) {
        String[] values = request.getParameterValues(name);
        return values != null ? Arrays.asList(values).stream()
                .map(rawItem -> ObjectUtils.to(returnType, rawItem))
                .filter(item -> item != null) : Stream.empty();
    }
}
