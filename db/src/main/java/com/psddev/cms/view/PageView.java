package com.psddev.cms.view;

/**
 * A base definition for a top level view responsible for rendering a page.
 */
public interface PageView {

    /**
     * Gets the main view for the given {@code model} based on the
     * {@link MainViewClass} annotation.
     *
     * @param model the model to check.
     * @param request the current view request
     * @return the main view.
     */
    default Object createMainView(Object model, ViewRequest request) {
        MainViewClass annotation = model.getClass().getAnnotation(MainViewClass.class);
        return annotation != null ? request.createView(annotation.value(), model) : null;
    }
}
