package com.psddev.cms.tool.page.content;

import com.google.common.collect.ImmutableMap;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.rtc.RtcBroadcast;
import com.psddev.dari.db.Query;

import java.util.Map;
import java.util.UUID;

class EditFieldUpdateBroadcast implements RtcBroadcast<EditFieldUpdate> {

    @Override
    public boolean shouldBroadcast(Map<String, Object> data, UUID currentUserId) {
        return !currentUserId.toString().equals(data.get("userId"));
    }

    @Override
    public Map<String, Object> create(EditFieldUpdate update) {
        ToolUser user = Query.from(ToolUser.class).where("_id = ?", update.getUserId()).first();

        if (user == null) {
            return null;
        }

        return ImmutableMap.of(
                "userId", user.getId().toString(),
                "userName", user.getName(),
                "contentId", update.getContentId().toString(),
                "fieldNamesByObjectId", update.getFieldNamesByObjectId()
        );
    }
}
