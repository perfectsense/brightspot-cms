package com.psddev.cms.notification;

public abstract class NotificationPublisher<S extends Subscription<C>, C> {

    /**
     * @return The type of subscription that gets published.
     */
    protected abstract Class<S> getSubscriptionType();

    /**
     * Gets the receivers to be notified when the subscription is published
     * based on the given {@code context}.
     *
     * @param context Contextual data for the subscription notification.
     * @return The receivers that should be notified.
     */
    protected abstract Iterable<? extends Receiver> getReceivers(C context);

    /**
     * Publishes the subscription with the given {@code context} by notifying
     * all registered receivers as determined by {@link #getReceivers(Object)}.
     * This method blocks until all of the notifications are sent.
     *
     * @param context Contextual data for the notification.
     */
    public final void publishNotification(C context) {

        for (Receiver receiver : getReceivers(context)) {
            receiver.as(ReceiverData.class).notify(getSubscriptionType(), context);
        }
    }

    /**
     * Publishes the subscription with the given {@code context} by notifying
     * all registered receivers as determined by {@link #getReceivers(Object)}.
     * The notifications are sent asynchronously and this method will return
     * immediately.
     *
     * @param context Contextual data for the notification.
     */
    public final void publishNotificationEventually(C context) {

        // TODO: Use AsyncQueue for high volume scenarios rather than spawning a new Thread each time.
        Thread sendNotification = new Thread() {
            @Override
            public void run() {
                publishNotification(context);
            }
        };
        sendNotification.start();
    }
}
