package com.psddev.cms.notification;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A notification that can be sent to a notifiable recipient.
 */
public final class Notification {

    private Set<Object> formats = new LinkedHashSet<>();

    /**
     * Adds a message format to this notification.
     *
     * @param format the message format to add.
     */
    public void addFormat(Object format) {
        formats.add(format);
    }

    /**
     * Gets the first message format in this notification.
     * @return the first format or {@code null} if there are none.
     */
    public Object getFirstFormat() {
        return !formats.isEmpty() ? formats.iterator().next() : null;
    }

    /**
     * Gets the first message format of the given type.
     *
     * @param formatType the type of message format to get.
     * @param <T> the message format type.
     * @return the first format or {@code null} if there are none.
     */
    public <T> T getFormatForType(Class<T> formatType) {
        for (Object format : formats) {
            if (formatType.isAssignableFrom(format.getClass())) {
                return (T) format;
            }
        }

        return null;
    }

    /**
     * Gets all of the message formats for this notification.
     *
     * @return Never {@code null}.
     */
    public Set<Object> getAllFormats() {
        return new LinkedHashSet<>(formats);
    }

    /**
     * Creates a simple String based notification.
     *
     * @param message the notification message
     * @return a new Notification with the given message.
     */
    public static Notification fromString(String message) {
        Notification notification = new Notification();
        notification.addFormat(message);
        return notification;
    }
}
