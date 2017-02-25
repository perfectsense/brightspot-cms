package com.psddev.cms.notification;

/**
 * Custom message formatters discoverable via call to ClassFinder API from
 * within {@link Notification#getMessage(NotificationReceiver, Object)}.
 *
 * @param <N> The type of notification.
 * @param <T> The type of the contextual data used to create the notification.
 */
public interface MessageFormatter<N extends Notification<T>, T> {

    /**
     * Formats a notification.
     *
     * @param notification The notification to format.
     * @param receiver The receiver of the notification.
     * @param context The contextual data for the notification.
     * @return A formatted message.
     */
    Object format(N notification, NotificationReceiver receiver, T context);
}
