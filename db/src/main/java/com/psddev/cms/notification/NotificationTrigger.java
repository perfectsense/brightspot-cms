package com.psddev.cms.notification;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.psddev.cms.db.ToolUi;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.Recordable;

/**
 * Creates a notification to be sent.
 *
 * @param <T> The type of contextual data used to create the notification.
 */
@Recordable.Embedded
public abstract class NotificationTrigger<T> extends Record {

    @ToolUi.NoteHtml("<span data-dynamic-html='${content.getProvidersNoteHtml()}'></span>")
    @Where("groups = " + NotificationProvider.INTERNAL_NAME
            + " && internalName != " + NotificationProvider.INTERNAL_NAME)
    @ToolUi.DropDown
    @DisplayName("Delivery Preference")
    private Set<ObjectType> providers;

    /**
     * Gets the list of notification providers configured to deliver
     * notifications for this trigger.
     *
     * @return Never {@code null}.
     */
    public Set<ObjectType> getProviders() {
        if (providers == null) {
            providers = new LinkedHashSet<>();
        }
        return providers;
    }

    /**
     * Sets the list of notification providers configured to deliver
     * notifications for this trigger.
     *
     * @param providers the list of notification providers to set.
     */
    public void setProviders(Set<ObjectType> providers) {
        if (providers != null) {
            // filter out un supported types
            this.providers = providers.stream()
                    .filter(type -> type.getGroups().contains(NotificationProvider.INTERNAL_NAME))
                    .filter(type -> !type.getInternalName().equals(NotificationProvider.INTERNAL_NAME))
                    .collect(Collectors.toCollection(LinkedHashSet::new));

        } else {
            this.providers = null;
        }
    }

    /**
     * Creates the notification to be sent.
     *
     * @param notifiable The receiver of the notification.
     * @param context The contextual data used to create the notification.
     * @return The notification to be sent.
     */
    public abstract Notification getNotification(Notifiable notifiable, T context);

    /**
     * Gets a user friendly label that describes the providers configured to
     * deliver notifications for this trigger.
     *
     * @return a user friendly label.
     */
    protected String getProvidersLabel() {
        List<String> providerLabels = getProviders().stream()
                .map(ObjectType::getLabel)
                .collect(Collectors.toList());

        if (providerLabels.isEmpty()) {
            return "";

        } else {
            String providersLabel;

            if (providerLabels.size() == 1) {
                providersLabel = providerLabels.get(0);

            } else {
                providersLabel = providerLabels.subList(0, providerLabels.size() - 1).stream().collect(Collectors.joining(", "))
                        + " and "
                        + providerLabels.get(providerLabels.size() - 1);
            }

            return providersLabel;
        }
    }

    /**
     * Gets the label that describes when and how this trigger is fired.
     * Sub-classes may override this method to provide a custom message.
     *
     * @return The trigger label.
     */
    protected String getTriggerLabel() {
        return "Sends " + getProvidersLabel() + " notification when triggered.";
    }

    @Override
    public String getLabel() {

        String deliveryLabel = getProviders().stream()
                .map(ObjectType::getLabel)
                .collect(Collectors.joining(", "));

        if (!deliveryLabel.isEmpty()) {
            return "[" + deliveryLabel + "]";

        } else {
            return "Disabled";
        }
    }

    /**
     * Not for public use.
     */
    public String getProvidersNoteHtml() {
        return getTriggerLabel();
    }
}
