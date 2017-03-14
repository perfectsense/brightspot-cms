package com.psddev.cms.view;

/**
 * Indicates an object that is wrapping another. Classes implementing this
 * interface are detected during calls to {@link ViewModel#createView(Class, Object)}
 * and {@link ViewModel#createView(String, Object)} and will recursively have
 * their {@link #unwrap()} method called to obtain the actual model object used
 * for {@linkplain ViewModel} / {@linkplain ViewBinding} look up. This API
 * serves as a convenience to {@linkplain ViewModel} implementations to simplify
 * their invocations of {@code createView} when dealing with models that merely
 * delegate to others.
 */
public interface ModelWrapper {

    /**
     * @return The object being wrapped.
     */
    Object unwrap();
}
