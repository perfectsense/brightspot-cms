package com.psddev.cms.tool.page.content;

import com.google.common.collect.ImmutableMap;
import com.psddev.cms.db.Content;
import com.psddev.cms.db.Draft;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.rtc.RtcBroadcast;
import com.psddev.dari.db.State;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class PublishBroadcast implements RtcBroadcast<Object> {

    @Override
    public boolean shouldBroadcast(Map<String, Object> data, UUID currentUserId) {
        return true;
    }

    @Override
    public Map<String, Object> create(Object object) {
        State state = State.getInstance(object);
        Content.ObjectModification contentData = state.as(Content.ObjectModification.class);
        ToolUser user = contentData.getUpdateUser();
        Date date = contentData.getUpdateDate();

        if (user == null || date == null) {
            return null;
        }

        return ImmutableMap.of(
                "userId", user.getId().toString(),
                "userName", user.getName(),
                "date", date.getTime(),
                "contentId", state.getId().toString(),
                "values", object instanceof Draft
                        ? State.getInstance(((Draft) object).recreate()).getSimpleValues()
                        : state.getSimpleValues()
        );
    }
}
