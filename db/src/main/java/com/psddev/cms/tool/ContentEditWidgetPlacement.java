package com.psddev.cms.tool;

import com.psddev.cms.db.Localization;

import java.io.IOException;

public enum ContentEditWidgetPlacement {

    TOP(null),

    TAB(null) {

        @Override
        public void displayBefore(ToolPageContext page, Object content, ContentEditWidget widget) throws IOException {
            page.writeStart("div",
                    "class", "Tab",
                    "data-tab", widget.getHeading(page, content),
                    "data-tab-class", widget.getClass().getName());
        }

        @Override
        public void displayAfter(ToolPageContext page) throws IOException {
            page.writeEnd();
        }
    },

    BOTTOM(Tool.CONTENT_BOTTOM_WIDGET_POSITION),
    RIGHT(Tool.CONTENT_RIGHT_WIDGET_POSITION);

    private final String legacyPosition;

    ContentEditWidgetPlacement(String legacyPosition) {
        this.legacyPosition = legacyPosition;
    }

    public String getLegacyPosition() {
        return legacyPosition;
    }

    public void displayBefore(ToolPageContext page, Object content, ContentEditWidget widget) throws IOException {
        page.writeStart("div", "class", "widget");
        page.writeStart("h1");
        page.writeHtml(widget.getHeading(page, content));
        page.writeEnd();
    }

    public void displayAfter(ToolPageContext page) throws IOException {
        page.writeEnd();
    }

    @Override
    public String toString() {
        return Localization.currentUserText(ContentEditWidgetPlacement.class, name());
    }
}
