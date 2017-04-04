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

    private ContentEditWidgetPlacement placementOverride;

    public ContentEditWidgetPlacement getPlacementOverride() {
        return placementOverride;
    }

    public void setPlacementOverride(ContentEditWidgetPlacement placementOverride) {
        this.placementOverride = placementOverride;
    }

    public boolean shouldDisplay(ToolPageContext page, Object content) {
        ObjectType type = State.getInstance(content).getType();
        return type != null && type.as(ToolUi.class).isPublishable();
    }

    public abstract ContentEditWidgetPlacement getPlacement(ToolPageContext page, Object content);

    public double getPosition(ToolPageContext page, Object content, ContentEditWidgetPlacement placement) {
        return 0.0;
    }

    public abstract String getHeading(ToolPageContext page, Object content);

    public abstract void display(ToolPageContext page, Object content, ContentEditWidgetPlacement placement) throws IOException;

    @Override
    public String getLabel() {
        return StringUtils.toLabel(getClass().getSimpleName());
    }
}
