package com.psddev.cms.tool.page.content;

import com.psddev.cms.rtc.RtcBroadcast;

import java.util.Map;
import java.util.UUID;

class PublishBroadcast implements RtcBroadcast<Publish> {

    @Override
    public boolean shouldBroadcast(Map<String, Object> data, UUID currentUserId) {
        return true;
    }

    @Override
    public Map<String, Object> create(Publish publish) {
        return publish.getState().getSimpleValues();
    }
}
