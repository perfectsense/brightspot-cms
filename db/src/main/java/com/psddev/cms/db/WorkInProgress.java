package com.psddev.cms.db;

import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Record;
import com.psddev.dari.util.CompactMap;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class WorkInProgress extends Record {

    @Indexed
    private ToolUser owner;

    @Indexed
    private ObjectType contentType;

    @Indexed
    private UUID contentId;

    private String contentLabel;

    private Date createDate;

    @Indexed
    private Date updateDate;

    private Map<String, Map<String, Object>> differences;

    public ToolUser getOwner() {
        return owner;
    }

    public void setOwner(ToolUser owner) {
        this.owner = owner;
    }

    public ObjectType getContentType() {
        return contentType;
    }

    public void setContentType(ObjectType contentType) {
        this.contentType = contentType;
    }

    public UUID getContentId() {
        return contentId;
    }

    public void setContentId(UUID contentId) {
        this.contentId = contentId;
    }

    public String getContentLabel() {
        return contentLabel;
    }

    public void setContentLabel(String contentLabel) {
        this.contentLabel = contentLabel;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public Date getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(Date updateDate) {
        this.updateDate = updateDate;
    }

    public Map<String, Map<String, Object>> getDifferences() {
        if (differences == null) {
            differences = new CompactMap<>();
        }
        return differences;
    }

    public void setDifferences(Map<String, Map<String, Object>> differences) {
        this.differences = differences;
    }
}
