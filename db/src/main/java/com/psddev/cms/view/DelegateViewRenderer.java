package com.psddev.cms.view;

/**
 * A renderer for {@link DelegateView delegate views} that forwards the
 * rendering logic to another view.
 */
public final class DelegateViewRenderer implements ViewRenderer {

    private static final String DELEGATE_VIEW_KEY = "delegate";

    @Override
    public ViewOutput render(Object view, ViewTemplateLoader templateLoader) {

        ViewMap viewMap = null;

        if (view instanceof ViewMap) {
            viewMap = (ViewMap) view;

        } else if (view != null) {
            viewMap = new ViewMap(view);
        }

        if (viewMap != null) {

            Object delegateView = viewMap.get(DELEGATE_VIEW_KEY);
            if (delegateView != null) {

                ViewRenderer delegateViewRenderer = ViewRenderer.createRenderer(delegateView);
                if (delegateViewRenderer != null) {

                    return delegateViewRenderer.render(delegateView, templateLoader);
                }
            }
        }

        return () -> null;
    }
}
