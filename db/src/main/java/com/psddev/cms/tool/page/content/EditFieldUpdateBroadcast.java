package com.psddev.cms.tool.page.content;

import com.psddev.cms.db.ToolUser;
import com.psddev.cms.rtc.RtcBroadcast;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.CompactMap;

import java.util.Map;
import java.util.UUID;

class EditFieldUpdateBroadcast implements RtcBroadcast<EditFieldUpdate> {

    @Override
    public boolean shouldBroadcast(Map<String, Object> data, UUID currentUserId) {
        return true;
    }

    @Override
    public Map<String, Object> create(EditFieldUpdate update) {
        ToolUser user = Query.from(ToolUser.class).where("_id = ?", update.getUserId()).first();

        if (user == null) {
            return null;
        }

        Map<String, Object> data = new CompactMap<>();

        data.put("userId", user.getId().toString());
        data.put("userName", user.getName());
        data.put("userAvatarHtml", user.createAvatarHtml());
        data.put("contentId", update.getContentId().toString());
        data.put("closed", update.isClosed());
        data.put("fieldNamesByObjectId", update.getFieldNamesByObjectId());

        return data;
    }
}
