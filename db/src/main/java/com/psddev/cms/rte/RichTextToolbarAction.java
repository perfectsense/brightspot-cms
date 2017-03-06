package com.psddev.cms.rte;

import java.util.Map;
import java.util.Objects;

import com.psddev.dari.util.CompactMap;

public class RichTextToolbarAction extends RichTextToolbarButton {

    public static final RichTextToolbarAction CLEAR = builder()
            .action("clear")
            .text("Clear")
            .cssClass("rte2-toolbar-clear")
            .tooltip("Clear Formatting")
            .inline(true)
            .build();

    public static final RichTextToolbarAction ENHANCEMENT = builder()
            .action("enhancement")
            .text("Enhancement")
            .cssClass("rte2-toolbar-enhancement")
            .tooltip("Add Block Enhancement")
            .build();

    public static final RichTextToolbarAction MARKER = builder()
            .action("marker")
            .text("Marker")
            .cssClass("rte2-toolbar-marker")
            .tooltip("Add Marker")
            .build();

    public static final RichTextToolbarAction TABLE = builder()
            .action("table")
            .text("Table")
            .cssClass("rte2-toolbar-noicon")
            .tooltip("Add Table")
            .build();

    public static final RichTextToolbarAction TRACK_CHANGES = builder()
            .action("trackChangesToggle")
            .text("Track Changes")
            .cssClass("rte2-toolbar-track-changes")
            .tooltip("Toggle Track Changes")
            .inline(true)
            .build();

    public static final RichTextToolbarAction TRACK_CHANGES_ACCEPT = builder()
            .action("trackChangesAccept")
            .text("Accept")
            .cssClass("rte2-toolbar-track-changes-accept")
            .tooltip("Accept Change")
            .inline(true)
            .build();

    public static final RichTextToolbarAction TRACK_CHANGES_REJECT = builder()
            .action("trackChangesReject")
            .text("Reject")
            .cssClass("rte2-toolbar-track-changes-reject")
            .tooltip("Reject Change")
            .inline(true)
            .build();

    public static final RichTextToolbarAction TRACK_CHANGES_PREVIEW = builder()
            .action("trackChangesShowFinalToggle")
            .text("Show Final")
            .cssClass("rte2-toolbar-track-changes-show-final")
            .tooltip("Toggle Preview")
            .inline(true)
            .build();

    public static final RichTextToolbarAction COMMENT_REMOVE = new RichTextToolbarAction() {
        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> map = new CompactMap<>();
            map.putAll(RichTextToolbarAction.builder()
                    .action("cleartext")
                    .text("Remove Comment")
                    .cssClass("rte2-toolbar-comment-remove")
                    .tooltip("Remove Comment")
                    .inline(true)
                    .build().toMap());
            map.put("cleartextStyle", "comment");
            return map;
        }
    };

    public static final RichTextToolbarAction COMMENT_COLLAPSE = new RichTextToolbarAction() {
        @Override
        public Map<String, Object> toMap() {
            Map<String, Object> map = new CompactMap<>();
            map.putAll(RichTextToolbarAction.builder()
                    .action("collapse")
                    .text("Toggle comment collapse")
                    .cssClass("rte2-toolbar-comment-collapse")
                    .tooltip("Toggle comment collapse")
                    .inline(true)
                    .build().toMap());
            map.put("collapseStyle", "comment");
            return map;
        }
    };

    public static final RichTextToolbarAction FULLSCREEN = builder()
            .action("fullscreen")
            .text("Fullscreen")
            .cssClass("rte2-toolbar-fullscreen")
            .tooltip("Toggle Fullscreen Editing")
            .inline(true)
            .build();

    public static final RichTextToolbarAction MODE = builder()
            .action("modeToggle")
            .text("HTML")
            .cssClass("rte2-toolbar-noicon")
            .tooltip("Toggle HTML Mode")
            .inline(true)
            .build();

    private String action;

    public static Builder builder() {
        return new Builder();
    }

    protected RichTextToolbarAction() {
    }

    public String getAction() {
        return action;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("action", getAction());
        return map;
    }

    public static class Builder extends ButtonBuilder<Builder, RichTextToolbarAction> {

        protected Builder() {
            super(new RichTextToolbarAction());
        }

        public Builder action(String action) {
            item.action = action;
            return this;
        }

        @Override
        protected void verify() {
            super.verify();
            Objects.requireNonNull(item.action);
        }
    }
}
