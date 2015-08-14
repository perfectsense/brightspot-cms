package com.psddev.cms.db;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;

public class Schedule extends Record {

    public static final String FIRST_TRIGGER_EXTRA = "cms.schedule.firstTrigger";

    private static final Logger LOGGER = LoggerFactory.getLogger(Schedule.class);

    @Indexed(unique = true)
    private String name;

    private String description;

    @Indexed
    private Date triggerDate;

    @ToolUi.Hidden
    private Site triggerSite;

    @ToolUi.Hidden
    private ToolUser triggerUser;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getTriggerDate() {
        return triggerDate;
    }

    public void setTriggerDate(Date triggerDate) {
        this.triggerDate = triggerDate;
    }

    public Site getTriggerSite() {
        return triggerSite;
    }

    public void setTriggerSite(Site triggerSite) {
        this.triggerSite = triggerSite;
    }

    public ToolUser getTriggerUser() {
        return triggerUser;
    }

    public void setTriggerUser(ToolUser triggerUser) {
        this.triggerUser = triggerUser;
    }

    protected void beforeSave() {
        if (ObjectUtils.isBlank(getName()) && getTriggerDate() == null) {
            throw new IllegalArgumentException("Must provide either the name or the trigger date!");
        }
    }

    /**
     * @return {@code true} if this schedule was triggered.
     */
    public boolean trigger() {
        Date triggerDate = getTriggerDate();

        if (triggerDate == null
                || !triggerDate.before(new Date())) {
            return false;
        }

        LOGGER.debug("Triggering [{}] schedule", getLabel());

        try {
            beginWrites();

            for (Object draftObject : Query
                    .fromAll()
                    .where("com.psddev.cms.db.Draft/schedule = ?", this)
                    .master()
                    .noCache()
                    .resolveInvisible()
                    .selectAll()) {
                if (!(draftObject instanceof Draft)) {
                    continue;
                }

                Draft draft = (Draft) draftObject;
                Object object = draft.getObject();

                LOGGER.debug("Processing [{}] draft in [{}] schedule", draft.getLabel(), getLabel());

                if (object != null) {
                    ToolUser triggerUser = getTriggerUser();

                    if (triggerUser == null) {
                        triggerUser = draft.as(Content.ObjectModification.class).getUpdateUser();

                        if (triggerUser == null) {
                            triggerUser = draft.getOwner();
                        }
                    }

                    State state = State.getInstance(object);
                    Content.ObjectModification contentData = state.as(Content.ObjectModification.class);

                    if (!state.isVisible()) {
                        state.getExtras().put(FIRST_TRIGGER_EXTRA, Boolean.TRUE);
                    }

                    contentData.setDraft(false);
                    contentData.setPublishDate(triggerDate);
                    contentData.setPublishUser(triggerUser);
                    state.as(BulkUploadDraft.class).setRunAfterSave(true);
                    Content.Static.publish(object, getTriggerSite(), triggerUser);
                }

                draft.delete();
            }

            delete();
            commitWrites();
            return true;

        } finally {
            endWrites();
        }
    }

    @Override
    public String getLabel() {
        String name = getName();
        StringBuilder label = new StringBuilder();

        if (ObjectUtils.isBlank(name)) {
            Date triggerDate = getTriggerDate();

            label.append(triggerDate != null
                    ? triggerDate.toString()
                    : getId().toString());

        } else {
            label.append(name);
        }

        long draftCount = Query
                .from(Draft.class)
                .where("schedule = ?", this)
                .count();

        if (draftCount > 1) {
            label.append(" (");
            label.append(draftCount);
            label.append(" items)");
        }

        return label.toString();
    }
}
