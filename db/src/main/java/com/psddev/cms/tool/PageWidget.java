package com.psddev.cms.tool;

import java.io.StringWriter;
import java.util.UUID;

import com.psddev.dari.db.State;
import com.psddev.dari.util.HtmlWriter;

/** Widget driven by a self-contained page. */
public class PageWidget extends Widget {

    private String path;
    private boolean displayInNonPublishable;

    /** Returns the path. */
    public String getPath() {
        return path;
    }

    /** Sets the path. */
    public void setPath(String path) {
        this.path = path;
    }

    public boolean isDisplayInNonPublishable() {
        return displayInNonPublishable;
    }

    public void setDisplayInNonPublishable(boolean displayInNonPublishable) {
        this.displayInNonPublishable = displayInNonPublishable;
    }

    // --- Widget support ---

    @Override
    public boolean shouldDisplayInNonPublishable() {
        return isDisplayInNonPublishable();
    }

    @Override
    @SuppressWarnings("resource")
    public String createDisplayHtml(ToolPageContext page, Object object) throws Exception {
        StringWriter sw = new StringWriter();
        HtmlWriter writer = new HtmlWriter(sw);
        State state = State.getInstance(object);

        writer.writeStart("div", "class", "frame");
            writer.writeStart("a",
                    "href", page.toolUrl(getTool(), path,
                            "id", state != null ? state.getId() : null,
                            "historyId", page.param(UUID.class, "historyId")));
            writer.writeEnd();
        writer.writeEnd();

        return sw.toString();
    }

    @Override
    public void update(ToolPageContext page, Object object) throws Exception {
    }
}
