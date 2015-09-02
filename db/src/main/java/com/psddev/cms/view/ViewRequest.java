package com.psddev.cms.view;

import java.util.stream.Stream;

/**
 * The context in which views are created while serving an HTTP request.
 */
public interface ViewRequest {

    /**
     * Creates a view of the specified {@code viewClass} based on the given
     * {@code model}.
     *
     * @param viewClass the view type to create.
     * @param model the model to create the view from.
     * @param <V> the view type to be created.
     * @return the created view.
     */
    <V> V createView(Class<V> viewClass, Object model);

    /**
     * Gets a stream of the HTTP request parameter values for the given {@code name}.
     *
     * @param returnType the stream type.
     * @param name the parameter name.
     * @param <T> the stream type.
     * @return a stream of the parameter values.
     */
    <T> Stream<T> getParameter(Class<T> returnType, String name);
}
