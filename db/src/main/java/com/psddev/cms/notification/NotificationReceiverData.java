package com.psddev.cms.notification;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.psddev.cms.db.ToolUi;
import com.psddev.dari.db.Modification;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Recordable;

/**
 * The delivery preferences and registered notifications for a notification recipient.
 */
@Recordable.FieldInternalNamePrefix(NotificationReceiverData.FIELD_PREFIX)
public class NotificationReceiverData extends Modification<NotificationReceiver> {

    private static final String TAB_NAME = "Notifications";

    static final String FIELD_PREFIX = "cms.notification.";

    @ToolUi.Heading("Delivery Preferences")
    @ToolUi.Tab(TAB_NAME)
    @Embedded
    @ToolUi.Unlabeled
    @Indexed
    private List<NotificationSender> deliveryPreferences;

    @ToolUi.Heading("Notifications")
    @Embedded
    @ToolUi.Tab(TAB_NAME)
    @ToolUi.Unlabeled
    @ToolUi.Expanded
    private List<Notification> notifications;

    /**
     * Gets the delivery preferences for this receiver.
     *
     * @return Never {@code null}.
     */
    public List<NotificationSender> getDeliveryPreferences() {
        if (deliveryPreferences == null) {
            deliveryPreferences = new ArrayList<>();
        }
        return deliveryPreferences;
    }

    /**
     * Sets the delivery preferences for this receiver.
     *
     * @param deliveryPreferences The list of delivery preferences to set.
     */
    public void setDeliveryPreferences(List<NotificationSender> deliveryPreferences) {
        this.deliveryPreferences = deliveryPreferences;
    }

    /**
     * Gets the registered notifications for this receiver.
     *
     * @return Never {@code null}.
     */
    public List<Notification> getNotifications() {
        if (notifications == null) {
            notifications = new ArrayList<>();
        }
        return notifications;
    }

    /**
     * Sets the notification notifications this receiver would like to receive.
     *
     * @param notifications The list of notifications to set.
     */
    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications;
    }

    /**
     * Triggers a notification to be sent this entity.
     *
     * @param notificationClass The type of notification to send.
     * @param context Contextual data for the notification.
     * @param <T> The type of contextual data for the notification.
     */
    public final <T> void notify(Class<? extends Notification<T>> notificationClass, T context) {

        List<NotificationSender> userSenders = getDeliveryPreferences();

        List<Notification> notifications = getNotifications();

        for (Notification<?> notification : notifications) {

            if (notificationClass.isAssignableFrom(notification.getClass())) {

                Set<ObjectType> senderTypes = notification.getSenderTypes();

                List<NotificationSender> senders = userSenders.stream()
                        .filter(np -> senderTypes.contains(np.getState().getType()))
                        .collect(Collectors.toList());

                @SuppressWarnings("unchecked")
                Notification<T> typedNotification = (Notification<T>) notification;

                Message message = typedNotification.getMessage(getOriginalObject(), context);

                if (message != null) {

                    for (NotificationSender sender : senders) {
                        sender.sendNotification(message);
                    }
                }
            }
        }
    }
}
