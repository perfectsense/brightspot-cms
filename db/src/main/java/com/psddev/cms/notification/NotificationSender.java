package com.psddev.cms.notification;

import com.psddev.dari.db.Record;
import com.psddev.dari.db.Recordable;

/**
 * Provides notifications via a unique delivery method, i.e. Email, SMS, etc.
 */
@Recordable.Embedded
public abstract class NotificationSender extends Record {

    static final String INTERNAL_NAME = "com.psddev.cms.notification.NotificationProvider";

    /**
     * Sends a notification with the given message.
     *
     * @param message the notification message to send.
     */
    public abstract void sendNotification(Message message);
}
