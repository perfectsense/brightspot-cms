package com.psddev.cms.tool.page.content;

import com.psddev.cms.rtc.RtcAction;
import com.psddev.dari.util.ObjectUtils;

import java.util.Map;
import java.util.UUID;

class OpenContentAction implements RtcAction {

    @Override
    public void execute(Map<String, Object> data, UUID userId, UUID sessionId) {

        UUID contentId = ObjectUtils.to(UUID.class, data.get("contentId"));

        OpenContent.save(userId, sessionId, contentId);
    }
}
