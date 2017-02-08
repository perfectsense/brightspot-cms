package com.psddev.cms.tool.page.content;

import com.psddev.cms.rtc.RtcEvent;
import com.psddev.dari.db.Record;

import java.util.Map;
import java.util.UUID;

public class Publish extends Record implements RtcEvent {

    @Required
    private UUID userId;

    @Required
    private String userName;

    @Required
    private long date;

    @Required
    private Map<String, Object> values;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public void setValues(Map<String, Object> values) {
        this.values = values;
    }

    @Override
    public void onDisconnect() {
    }
}
