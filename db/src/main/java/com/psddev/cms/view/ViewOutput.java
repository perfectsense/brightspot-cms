package com.psddev.cms.view;

/**
 * The result of {@link com.psddev.cms.view.ViewRenderer#render(Object)
 * rendering} a view.
 */
public interface ViewOutput {

    /**
     * Fetches the rendered view's output.
     *
     * @return the text output from a rendered view.
     */
    String get();
}
