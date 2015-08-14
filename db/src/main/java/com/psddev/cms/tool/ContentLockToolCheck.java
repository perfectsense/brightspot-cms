package com.psddev.cms.tool;

import java.util.Map;
import java.util.UUID;

import com.psddev.cms.db.ToolUser;
import com.psddev.dari.util.ObjectUtils;

/**
 * @deprecated No replacement.
 */
@Deprecated
public class ContentLockToolCheck extends ToolCheck {

    @Override
    public String getName() {
        return "contentLock";
    }

    @Override
    protected ToolCheckResponse doCheck(ToolUser user, String url, Map<String, Object> parameters) {
        UUID contentId = ObjectUtils.to(UUID.class, parameters.get("contentId"));
        UUID oldOwnerId = ObjectUtils.to(UUID.class, parameters.get("ownerId"));
        UUID newOwnerId = user.lockContent(contentId).getId();

        if (newOwnerId.equals(oldOwnerId)) {
            return null;

        } else {
            return new ToolCheckResponse("newOwner", "id", newOwnerId);
        }
    }
}
