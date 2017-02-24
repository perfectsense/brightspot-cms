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
 * The delivery preferences and registered triggers for a notification recipient.
 */
@Recordable.FieldInternalNamePrefix(NotifiableData.FIELD_PREFIX)
public class NotifiableData extends Modification<Notifiable> {

    private static final String TAB_NAME = "Notifications";

    static final String FIELD_PREFIX = "cms.notification.";

    @ToolUi.Heading("Delivery Preferences")
    @ToolUi.Tab(TAB_NAME)
    @Embedded
    @ToolUi.Unlabeled
    @Indexed
    private List<NotificationProvider> deliveryPreferences;

    @ToolUi.Heading("Notifications")
    @Embedded
    @ToolUi.Tab(TAB_NAME)
    @ToolUi.Unlabeled
    @ToolUi.Expanded
    private List<NotificationTrigger> triggers;

    /**
     * Gets the delivery preferences for this notifiable entity.
     *
     * @return Never {@code null}.
     */
    public List<NotificationProvider> getDeliveryPreferences() {
        if (deliveryPreferences == null) {
            deliveryPreferences = new ArrayList<>();
        }
        return deliveryPreferences;
    }

    /**
     * Sets the delivery preferences for this notifiable entity.
     *
     * @param deliveryPreferences The list of delivery preferences to set.
     */
    public void setDeliveryPreferences(List<NotificationProvider> deliveryPreferences) {
        this.deliveryPreferences = deliveryPreferences;
    }

    /**
     * Gets the registered triggers for this notifiable entity.
     *
     * @return Never {@code null}.
     */
    public List<NotificationTrigger> getTriggers() {
        if (triggers == null) {
            triggers = new ArrayList<>();
        }
        return triggers;
    }

    /**
     * Sets the notification triggers this notifiable entity would like to receive.
     *
     * @param triggers The list of triggers to set.
     */
    public void setTriggers(List<NotificationTrigger> triggers) {
        this.triggers = triggers;
    }

    /**
     * Triggers a notification to be sent this entity.
     *
     * @param triggerClass The type of trigger to fire.
     * @param context Contextual data for the trigger.
     * @param <T> The type of contextual data for the trigger.
     */
    public final <T> void fireTrigger(Class<? extends NotificationTrigger<T>> triggerClass, T context) {

        List<NotificationProvider> userNotificationProviders = getDeliveryPreferences();

        List<NotificationTrigger> triggers = getTriggers();

        for (NotificationTrigger<?> trigger : triggers) {

            if (triggerClass.isAssignableFrom(trigger.getClass())) {

                Set<ObjectType> triggerProviderTypes = trigger.getProviders();

                List<NotificationProvider> triggerProviders = userNotificationProviders.stream()
                        .filter(np -> triggerProviderTypes.contains(np.getState().getType()))
                        .collect(Collectors.toList());

                @SuppressWarnings("unchecked")
                NotificationTrigger<T> typedTrigger = (NotificationTrigger<T>) trigger;

                Notification notification = typedTrigger.getNotification(getOriginalObject(), context);

                if (notification != null) {

                    for (NotificationProvider notificationProvider : triggerProviders) {
                        notificationProvider.sendNotification(notification);
                    }
                }
            }
        }
    }
}
