package com.psddev.cms.notification;

import com.psddev.dari.db.Query;

/**
 * Publishes a notification to registered subscribers.
 *
 * @param <S> The subscription type.
 * @param <C> The contextual data for the subscription.
 */
public abstract class Publisher<S extends Subscription<C>, C> {

    protected C context;

    /**
     * Creates a new Publisher instance for the given {@code context}.
     *
     * @param context Contextual data for the subscription notification.
     */
    public Publisher(C context) {
        this.context = context;
    }

    /**
     * @return The type of subscription that gets published.
     */
    protected abstract Class<S> getSubscriptionType();

    /**
     * Gets the potential receivers to be notified when the subscription is
     * published. The default implementation queries for all Receiver instances
     * in the database. Sub-classes may override this method to provide a
     * narrower set of receivers that can receive the notification.
     *
     * @return The receivers that should be notified.
     */
    protected Iterable<? extends Receiver> getReceivers() {
        return Query.from(Receiver.class).iterable(0);
    }

    /**
     * Publishes the subscription with the given {@code context} by notifying
     * all registered receivers as determined by {@link #getReceivers()}.
     * This method blocks until all of the notifications are sent.
     */
    public final void publishNotification() {

        for (Receiver receiver : getReceivers()) {
            receiver.as(ReceiverData.class).notify(getSubscriptionType(), context);
        }
    }

    /**
     * Publishes the subscription with the given {@code context} by notifying
     * all registered receivers as determined by {@link #getReceivers()}.
     * The notifications are sent asynchronously and this method will return
     * immediately.
     */
    public final void publishNotificationEventually() {

        // TODO: Use AsyncQueue for high volume scenarios rather than spawning a new Thread each time.
        Thread sendNotification = new Thread() {
            @Override
            public void run() {
                publishNotification();
            }
        };
        sendNotification.start();
    }
}
