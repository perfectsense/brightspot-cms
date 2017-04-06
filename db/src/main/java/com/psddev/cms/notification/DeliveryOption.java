package com.psddev.cms.notification;

import com.psddev.dari.db.Record;
import com.psddev.dari.db.Recordable;

/**
 * Provides notifications via a unique delivery method, i.e. Email, SMS, etc.
 */
@Recordable.Embedded
public abstract class DeliveryOption extends Record {

    static final String INTERNAL_NAME = "com.psddev.cms.notification.DeliveryOption";

    /**
     * Delivers a notification to one or more recipients.
     *
     * @param notification the notification to send.
     */
    public abstract void deliver(Notification<?> notification);
}
