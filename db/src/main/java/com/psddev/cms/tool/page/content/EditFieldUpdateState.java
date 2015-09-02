package com.psddev.cms.tool.page.content;

import com.psddev.cms.rtc.RtcState;
import com.psddev.dari.db.Query;

import java.util.Map;

class EditFieldUpdateState implements RtcState {

    @Override
    public Iterable<? extends Object> create(Map<String, Object> data) {
        return Query.from(EditFieldUpdate.class)
                .where("contentId = ?", data.get("contentId"))
                .selectAll();
    }
}
