package com.psddev.cms.tool;

import com.psddev.cms.db.ToolUi;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.db.State;
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
        ObjectType type = State.getInstance(content).getType();
        return type != null && type.as(ToolUi.class).isPublishable();
    }

    public abstract ContentEditSection getSection(ToolPageContext page, Object content);

    public double getPosition(ToolPageContext page, Object content, ContentEditSection section) {
        return 0.0;
    }

    public abstract String getHeading(ToolPageContext page, Object content);

    public abstract void display(ToolPageContext page, Object content, ContentEditSection section) throws IOException;

    @Override
    public String getLabel() {
        return StringUtils.toLabel(getClass().getSimpleName());
    }
}
