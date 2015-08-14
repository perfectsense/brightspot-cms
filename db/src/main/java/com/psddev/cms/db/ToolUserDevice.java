package com.psddev.cms.db;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;
import com.psddev.dari.util.ObjectUtils;

/**
 * Device used by a user to access the tool.
 */
public class ToolUserDevice extends Record {

    @Indexed
    private ToolUser user;

    @Indexed
    private UUID lookingGlassId;

    private String userAgent;

    public ToolUser getUser() {
        return user;
    }

    public void setUser(ToolUser user) {
        this.user = user;
    }

    public UUID getLookingGlassId() {
        return lookingGlassId;
    }

    public void setLookingGlassId(UUID lookingGlassId) {
        this.lookingGlassId = lookingGlassId;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    /**
     * Returns a descriptive label of the user agent suitable for display.
     *
     * @return Never {@code null}.
     */
    public String getUserAgentDisplay() {
        String ua = getUserAgent();

        if (ObjectUtils.isBlank(ua)) {
            return "Unknown Device";
        }

        ua = ua.toLowerCase(Locale.ENGLISH);

        if (ua.contains("iphone")) {
            return "iPhone";

        } else if (ua.contains("chrome/")) {
            return "Google Chrome";

        } else if (ua.contains("safari/")) {
            return "Apple Safari";

        } else if (ua.contains("firefox/")) {
            return "Mozilla Firefox";

        } else {
            return ua;
        }
    }

    /**
     * Returns or creates an unique looking glass ID.
     *
     * @return Never {@code null}.
     */
    public UUID getOrCreateLookingGlassId() {
        UUID id = getLookingGlassId();

        if (id == null) {
            id = UUID.randomUUID();

            setLookingGlassId(id);
            save();
        }

        return id;
    }

    /**
     * Finds the last action from this device.
     *
     * @return May be {@code null}.
     */
    public ToolUserAction findLastAction() {
        return Query
                .from(ToolUserAction.class)
                .where("device = ?", this)
                .sortDescending("time")
                .first();
    }

    /**
     * Saves the given {@code action} associated with this device.
     *
     * @param action Can't be {@code null}.
     */
    public void saveAction(ToolUserAction action) {
        action.setDevice(this);
        action.setTime(System.currentTimeMillis());

        Map<String, Object> actionMap = action.getState().getSimpleValues();

        actionMap.remove("_id");
        actionMap.remove("time");

        List<ToolUserAction> actions = Query
                .from(ToolUserAction.class)
                .where("device = ?", this)
                .sortDescending("time")
                .selectAll();

        for (Iterator<ToolUserAction> i = actions.iterator(); i.hasNext();) {
            ToolUserAction a = i.next();
            Map<String, Object> currentMap = a.getState().getSimpleValues();

            currentMap.remove("_id");
            currentMap.remove("time");

            if (currentMap.equals(actionMap)) {
                i.remove();
                a.delete();
            }
        }

        action.save();

        while (actions.size() > 5) {
            actions.remove(actions.size() - 1).delete();
        }
    }
}
