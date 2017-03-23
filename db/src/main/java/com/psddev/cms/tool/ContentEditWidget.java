package com.psddev.cms.tool;

import com.psddev.dari.db.Record;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.util.StringUtils;

import java.io.IOException;

@Recordable.Embedded
public abstract class ContentEditWidget extends Record {

    private ContentEditSection sectionOverride;

    public ContentEditSection getSectionOverride() {
        return sectionOverride;
    }

    public void setSectionOverride(ContentEditSection sectionOverride) {
        this.sectionOverride = sectionOverride;
    }

    public boolean shouldDisplay(ToolPageContext page, Object content) {
        return true;
    }

    public abstract ContentEditSection getSection(ToolPageContext page, Object content);

    public abstract String getHeading(ToolPageContext page, Object content);

    public abstract void display(ToolPageContext page, Object content, ContentEditSection section) throws IOException;

    public void update(ToolPageContext page, Object content) {
    }

    @Override
    public String getLabel() {
        return StringUtils.toLabel(getClass().getSimpleName());
    }
}
