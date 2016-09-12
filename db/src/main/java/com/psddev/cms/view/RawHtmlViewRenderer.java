package com.psddev.cms.view;

public class RawHtmlViewRenderer implements ViewRenderer {

    @Override
    public ViewOutput render(Object view, ViewTemplateLoader templateLoader) {

        if (view instanceof ViewMap) {
            view = ((ViewMap) view).getView();
        }

        String html;

        if (view instanceof RawHtmlView) {
            RawHtmlView rawHtmlView = (RawHtmlView) view;
            html = rawHtmlView.getHtml();

        } else {
            html = null;
        }

        return () -> html;
    }
}
