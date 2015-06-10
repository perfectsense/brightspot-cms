package com.psddev.cms.db;

import com.psddev.dari.db.Recordable;
import com.psddev.dari.util.TypeDefinition;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Helps render a Recordable object in the context of an HTTP request.
 *
 * @param <T> the type of object being rendered.
 */
public abstract class RendererView<T extends Recordable> {

    private T object;

    private HttpServletRequest request;

    private HttpServletResponse response;

    /**
     * @return the object being rendered.
     */
    protected final T getObject() {
        return object;
    }

    /**
     * @return the current HTTP request.
     */
    protected final HttpServletRequest getRequest() {
        return request;
    }

    /**
     * @return the current HTTP response.
     */
    protected final HttpServletResponse getResponse() {
        return response;
    }

    /**
     * Creates an instance of RendererView of type {@code viewClass} with the
     * given arguments.
     *
     * @param viewClass the class of the RendererView to be created.
     * @param object the associated object for the renderer view.
     * @param request the associated request for the renderer view.
     * @param response the associated response for the renderer view.
     * @param <V> the RendererView type.
     * @param <T> the Recordable type.
     * @return a newly created RendererView object based on the given arguments.
     */
    public static <V extends RendererView<T>, T extends Recordable> RendererView<T> create(
            Class<V> viewClass, T object, HttpServletRequest request, HttpServletResponse response) {

        V view = TypeDefinition.getInstance(viewClass).newInstance();
        ((RendererView<T>) view).object = object;
        ((RendererView<T>) view).request = request;
        ((RendererView<T>) view).response = response;
        return view;
    }
}
