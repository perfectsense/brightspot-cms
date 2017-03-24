package com.psddev.cms.tool.page;

import com.psddev.cms.tool.CmsTool;
import com.psddev.cms.tool.ContentEditSection;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.cms.tool.Widget;
import com.psddev.cms.tool.content.RevisionsWidget;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * @deprecated Use {@link RevisionsWidget} instead.
 */
@Deprecated
public class ContentRevisions extends Widget {

    {
        setDisplayName("Revisions");
        setInternalName("cms.contentRevision");
        addPosition(CmsTool.CONTENT_RIGHT_WIDGET_POSITION, 0, 3);
    }

    @Override
    public boolean shouldDisplayInNonPublishable() {
        return true;
    }

    @Override
    public String createDisplayHtml(ToolPageContext page, Object object) throws IOException {
        Writer oldDelegate = page.getDelegate();
        StringWriter newDelegate = new StringWriter();

        try {
            page.setDelegate(newDelegate);
            new RevisionsWidget().display(page, object, ContentEditSection.RIGHT);
            return newDelegate.toString();

        } finally {
            page.setDelegate(oldDelegate);
        }
    }

    @Override
    public void update(ToolPageContext page, Object object) {
    }
}
