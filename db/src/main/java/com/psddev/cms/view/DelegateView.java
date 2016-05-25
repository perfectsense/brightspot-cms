package com.psddev.cms.view;

/**
 * A view that delegates its rendering logic to another.
 */
@ViewInterface
@ViewRendererClass(DelegateViewRenderer.class)
public interface DelegateView {

    /**
     * Gets the delegate view.
     *
     * @return the delegate view.
     */
    Object getDelegate();
}
