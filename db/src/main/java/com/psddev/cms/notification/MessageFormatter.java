package com.psddev.cms.notification;

/**
 * Custom message formatters discoverable via call to ClassFinder API from
 * within {@link Notification#format(Class)}.
 *
 * @param <S> The type of subscription.
 * @param <C> The type of contextual data for the subscription.
 * @param <F> The type of message format.
 */
public interface MessageFormatter<S extends Subscription<C>, C, F> {

    /**
     * Formats a notification.
     *
     * @param subscription The subscription.
     * @param receiver The receiver of the notification.
     * @param context The contextual data for the subscription.
     * @return A formatted message.
     */
    F format(S subscription, Receiver receiver, C context);
}
