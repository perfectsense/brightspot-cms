package com.psddev.cms.tool.page;

import com.psddev.dari.util.CompactMap;

import java.util.Map;
import java.util.UUID;

class RtcSession {

    private UUID currentUserId;
    private Map<String, RtcAction> actions;

    public UUID getCurrentUserId() {
        return currentUserId;
    }

    public void setCurrentUserId(UUID currentUserId) {
        this.currentUserId = currentUserId;
    }

    public Map<String, RtcAction> getActions() {
        if (actions == null) {
            actions = new CompactMap<>();
        }
        return actions;
    }

    public void setActions(Map<String, RtcAction> actions) {
        this.actions = actions;
    }
}
