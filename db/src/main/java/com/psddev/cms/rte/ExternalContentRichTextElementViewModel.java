package com.psddev.cms.rte;

import java.io.IOException;
import java.io.StringWriter;

import com.psddev.cms.db.ExternalContent;
import com.psddev.cms.view.RawHtmlView;
import com.psddev.cms.view.ViewModel;
import com.psddev.dari.util.HtmlWriter;

class ExternalContentRichTextElementViewModel extends ViewModel<ExternalContentRichTextElement> implements RawHtmlView {

    @Override
    public String getHtml() {

        ExternalContent externalContent = model.getContent();
        if (externalContent != null) {

            StringWriter html = new StringWriter();
            try {
                externalContent.renderObject(null, null, new HtmlWriter(html));
            } catch (IOException e) {
                // should never happen
                throw new IllegalStateException(e);
            }

            return html.toString();
        }

        return null;
    }
}
