package com.psddev.cms.tool.page.content;

import com.psddev.cms.db.ToolUser;
import com.psddev.cms.rtc.RtcBroadcast;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.CompactMap;

import java.util.Map;
import java.util.UUID;

class OpenContentBroadcast implements RtcBroadcast<OpenContent> {

    @Override
    public boolean shouldBroadcast(Map<String, Object> data, UUID currentUserId) {
        return !currentUserId.toString().equals(data.get("userId"));
    }

    @Override
    public Map<String, Object> create(OpenContent open) {
        ToolUser user = Query.from(ToolUser.class).where("_id = ?", open.getUserId()).first();

        if (user == null) {
            return null;
        }

        Map<String, Object> data = new CompactMap<>();

        data.put("userId", user.getId().toString());
        data.put("avatarHtml", user.createAvatarHtml());
        data.put("contentId", open.getContentId().toString());
        data.put("closed", open.isClosed());

        return data;
    }
}
