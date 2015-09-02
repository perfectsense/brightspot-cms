package com.psddev.cms.tool.page.content;

import com.psddev.cms.rtc.RtcAction;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.TypeReference;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

class EditFieldUpdateAction implements RtcAction {

    @Override
    public void execute(Map<String, Object> data, UUID userId, UUID sessionId) {
        UUID contentId = ObjectUtils.to(UUID.class, data.get("contentId"));
        String unlockObjectId = ObjectUtils.to(String.class, data.get("unlockObjectId"));

        if (!ObjectUtils.isBlank(unlockObjectId)) {
            String unlockFieldName = ObjectUtils.to(String.class, data.get("unlockFieldName"));

            if (!ObjectUtils.isBlank(unlockFieldName)) {
                for (EditFieldUpdate update : Query
                        .from(EditFieldUpdate.class)
                        .where("contentId = ?", contentId)
                        .selectAll()) {

                    Set<String> fieldNames = update.getFieldNamesByObjectId().get(unlockObjectId);

                    if (fieldNames != null && !fieldNames.isEmpty()) {
                        fieldNames.remove(unlockFieldName);
                        update.save();
                    }
                }
            }

        } else {
            EditFieldUpdate.save(
                    userId,
                    sessionId,
                    contentId,
                    ObjectUtils.to(new TypeReference<Map<String, Set<String>>>() {
                    }, data.get("fieldNamesByObjectId")));
        }
    }
}
