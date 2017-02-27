package com.psddev.cms.notification;

import java.util.function.Supplier;

import com.psddev.dari.db.Recordable;

/**
 * An entity capable of receiving notifications.
 */
public interface NotificationReceiver extends Recordable {

    /**
     * Sends a notification to each recipient.
     *
     * @param notificationClass The type of notification to send.
     * @param context Contextual data for the notification.
     * @param receivers The notification receivers.
     * @param <T> The type of contextual data for the notification.
     */
    static <T> void notify(Class<? extends Notification<T>> notificationClass, T context, Iterable<? extends NotificationReceiver> receivers) {
        for (NotificationReceiver receiver : receivers) {
            receiver.as(NotificationReceiverData.class).notify(notificationClass, context);
        }
    }

    /**
     * Sends a notification to each recipient asynchronously.
     *
     * @param notificationClass The type of notification to send.
     * @param context Contextual data for the notification.
     * @param receiversSupplier A supplier of the notification receivers.
     * @param <T> The type of contextual data for the notification.
     */
    static <T> void notifyAsynchronously(Class<? extends Notification<T>> notificationClass, T context, Supplier<Iterable<? extends NotificationReceiver>> receiversSupplier) {
        Thread sendNotification = new Thread() {
            @Override
            public void run() {
                NotificationReceiver.notify(notificationClass, context, receiversSupplier.get());
            }
        };
        sendNotification.start();
    }
}
