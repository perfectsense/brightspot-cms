package com.psddev.cms.tool.page.content;

import com.psddev.cms.rtc.RtcState;
import com.psddev.dari.db.Query;

import java.util.Map;
import java.util.stream.Collectors;

class OpenContentState implements RtcState {

    @Override
    public Iterable<?> create(Map<String, Object> data) {
        return Query.from(OpenContent.class)
                .where("contentId = ?", data.get("contentId"))
                .selectAll()
                .stream()
                .filter(o -> !o.isClosed())
                .collect(Collectors.toList());
    }
}
