package com.psddev.cms.db;

import com.psddev.cms.notification.NotificationReceiver;
import com.psddev.dari.db.Recordable;

public interface ToolEntity extends Global, NotificationReceiver, Recordable {

    /**
     * Returns all tool users that are represented by this entity.
     *
     * @return Never {@code null}.
     */
    public Iterable<? extends ToolUser> getUsers();
}
