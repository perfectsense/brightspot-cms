package com.psddev.cms.tool.page.content;

import com.psddev.cms.db.Content;
import com.psddev.cms.db.Draft;
import com.psddev.cms.db.ToolUser;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.Modification;
import com.psddev.dari.db.State;

import java.util.Date;

public class PublishModification extends Modification<Object> {

    private static final String BROADCAST_EXTRA = PublishModification.class.getName() + ".broadcast";

    public static void setBroadcast(Object object, boolean broadcast) {
        State.getInstance(object).getExtras().put(BROADCAST_EXTRA, broadcast);
    }

    @Override
    protected void afterSave() {
        Object object = getState().getOriginalObjectOrNull();

        if (object == null) {
            return;
        }

        if (!Boolean.TRUE.equals(State.getInstance(object).getExtras().get(BROADCAST_EXTRA))) {
            return;
        }

        Publish publish = new Publish();
        Content.ObjectModification contentData = State.getInstance(object).as(Content.ObjectModification.class);
        ToolUser updateUser = contentData.getUpdateUser();

        if (updateUser == null) {
            return;
        }

        Date updateDate = contentData.getUpdateDate();

        if (updateDate == null) {
            return;
        }

        publish.setUserId(updateUser.getId());
        publish.setUserName(updateUser.getName());
        publish.setDate(updateDate.getTime());

        if (object instanceof Draft) {
            object = ((Draft) object).recreate();
        }

        publish.setValues(State.getInstance(object).getSimpleValues());
        publish.saveImmediately();

        Database db = Database.Static.getDefault();

        try {
            db.beginIsolatedWrites();
            publish.delete();
            db.commitWrites();

        } finally {
            db.endWrites();
        }
    }
}
