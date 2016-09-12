package com.psddev.cms.db;

import java.io.IOException;
import java.io.StringWriter;

import com.psddev.cms.view.RawHtmlView;
import com.psddev.cms.view.ViewModel;
import com.psddev.dari.util.HtmlWriter;

class ExternalContentViewModel extends ViewModel<ExternalContent> implements RawHtmlView {

    @Override
    public String getHtml() {

        StringWriter html = new StringWriter();
        try {
            model.renderObject(null, null, new HtmlWriter(html));
        } catch (IOException e) {
            // should never happen
            throw new IllegalStateException(e);
        }

        return html.toString();
    }
}
