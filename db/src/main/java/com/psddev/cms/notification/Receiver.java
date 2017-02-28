package com.psddev.cms.notification;

import java.util.function.Supplier;

import com.psddev.dari.db.Recordable;

/**
 * An entity capable of receiving notifications.
 */
public interface Receiver extends Recordable {

    /**
     * Sends a notification to each recipient.
     *
     * @param subscriptionClass The type of subscription to notify on.
     * @param context Contextual data for the notification.
     * @param receivers The notification receivers.
     * @param <S> The type of subscription.
     * @param <C> The type of contextual data for the subscription.
     */
    static <S extends Subscription<C>, C> void notify(Class<S> subscriptionClass, C context, Iterable<? extends Receiver> receivers) {
        for (Receiver receiver : receivers) {
            receiver.as(ReceiverData.class).notify(subscriptionClass, context);
        }
    }

    /**
     * Sends a notification to each recipient asynchronously.
     *
     * @param subscriptionClass The type of subscription to notify on.
     * @param context Contextual data about the subscription.
     * @param receiversSupplier A supplier of the notification receivers.
     * @param <S> The type of subscription.
     * @param <C> The type of contextual data for the subscription.
     */
    static <S extends Subscription<C>, C> void notifyAsynchronously(Class<S> subscriptionClass, C context, Supplier<Iterable<? extends Receiver>> receiversSupplier) {
        Thread sendNotification = new Thread() {
            @Override
            public void run() {
                Receiver.notify(subscriptionClass, context, receiversSupplier.get());
            }
        };
        sendNotification.start();
    }
}
