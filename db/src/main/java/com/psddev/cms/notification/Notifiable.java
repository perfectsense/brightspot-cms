package com.psddev.cms.notification;

import java.util.function.Supplier;

import com.psddev.dari.db.Recordable;

/**
 * An entity capable of being notified.
 */
public interface Notifiable extends Recordable {

    /**
     * Gets the notification settings for this entity.
     *
     * @return Never {@code null}.
     */
    default NotifiableData asNotifiableData() {
        return as(NotifiableData.class);
    }

    /**
     * Triggers a notification to be sent each recipient.
     *
     * @param triggerClass The type of trigger to fire.
     * @param context Contextual data for the trigger.
     * @param notifiables The entities that should be notified.
     * @param <T> The type of contextual data for the trigger.
     */
    static <T> void fireTrigger(Class<? extends NotificationTrigger<T>> triggerClass, T context, Iterable<? extends Notifiable> notifiables) {
        for (Notifiable notifiable : notifiables) {
            notifiable.asNotifiableData().fireTrigger(triggerClass, context);
        }
    }

    /**
     * Triggers a notification to be sent each recipient asynchronously.
     *
     * @param triggerClass The type of trigger to fire.
     * @param context Contextual data for the trigger.
     * @param notifiablesSupplier A supplier of the entities that should be notified.
     * @param <T> The type of contextual data for the trigger.
     */
    static <T> void fireTriggerAsynchronously(Class<? extends NotificationTrigger<T>> triggerClass, T context, Supplier<Iterable<? extends Notifiable>> notifiablesSupplier) {
        Thread fireTriggers = new Thread() {
            @Override
            public void run() {
                fireTrigger(triggerClass, context, notifiablesSupplier.get());
            }
        };
        fireTriggers.start();
    }
}
