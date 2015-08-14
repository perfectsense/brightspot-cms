package com.psddev.cms.db;

import java.util.UUID;

import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;

/**
 * Action taken by a user in the tool.
 */
public class ToolUserAction extends Record {

    @Indexed
    private ToolUserDevice device;

    @Indexed
    private long time;

    private UUID contentId;
    private String url;

    public ToolUserDevice getDevice() {
        return device;
    }

    public void setDevice(ToolUserDevice device) {
        this.device = device;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public UUID getContentId() {
        return contentId;
    }

    public void setContentId(UUID contentId) {
        this.contentId = contentId;
    }

    public Object getContent() {
        return Query
                .from(Object.class)
                .where("_id = ?", getContentId())
                .first();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
