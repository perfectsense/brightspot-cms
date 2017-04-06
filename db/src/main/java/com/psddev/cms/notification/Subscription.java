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
 * A subscription to receive notifications on.
 *
 * @param <C> The type of contextual data for the subscription.
 */
@Recordable.Embedded
public abstract class Subscription<C> extends Record {

    @ToolUi.NoteHtml("<span data-dynamic-html='${content.getDeliveryOptionTypesNoteHtml()}'></span>")
    @Where("groups = " + DeliveryOption.INTERNAL_NAME
            + " && internalName != " + DeliveryOption.INTERNAL_NAME)
    @ToolUi.DropDown
    @DisplayName("Delivery Options")
    private Set<ObjectType> deliveryOptionTypes;

    /**
     * Gets the list of notification deliveryOptionTypes configured to deliver
     * this notification.
     *
     * @return Never {@code null}.
     */
    public Set<ObjectType> getDeliveryOptionTypes() {
        if (deliveryOptionTypes == null) {
            deliveryOptionTypes = new LinkedHashSet<>();
        }
        return deliveryOptionTypes;
    }

    /**
     * Sets the list of notification deliveryOptionTypes configured to deliver
     * this notification.
     *
     * @param deliveryOptionTypes the list of notification deliveryOptionTypes to set.
     */
    public void setDeliveryOptionTypes(Set<ObjectType> deliveryOptionTypes) {
        if (deliveryOptionTypes != null) {
            // filter out un supported types
            this.deliveryOptionTypes = deliveryOptionTypes.stream()
                    .filter(type -> type.getGroups().contains(DeliveryOption.INTERNAL_NAME))
                    .filter(type -> !type.getInternalName().equals(DeliveryOption.INTERNAL_NAME))
                    .collect(Collectors.toCollection(LinkedHashSet::new));

        } else {
            this.deliveryOptionTypes = null;
        }
    }

    /**
     * Creates the default notification message format to be sent.
     *
     * @param receiver The receiver of the notification.
     * @param context The contextual data used to create the notification.
     * @return The notification to be sent.
     */
    protected abstract String toStringFormat(Receiver receiver, C context);

    /**
     * Gets a user friendly label that describes the types of senders
     * configured to deliver this notification.
     *
     * @return a user friendly label.
     */
    protected final String getDeliveryOptionTypesLabel() {
        List<String> senderLabels = getDeliveryOptionTypes().stream()
                .map(ObjectType::getLabel)
                .collect(Collectors.toList());

        if (senderLabels.isEmpty()) {
            return "";

        } else {
            String sendersLabel;

            if (senderLabels.size() == 1) {
                sendersLabel = senderLabels.get(0);

            } else {
                sendersLabel = senderLabels.subList(0, senderLabels.size() - 1).stream().collect(Collectors.joining(", "))
                        + " and "
                        + senderLabels.get(senderLabels.size() - 1);
            }

            return sendersLabel;
        }
    }

    /**
     * Gets the label that describes when and how this notification is delivered.
     * Sub-classes may override this method to provide a custom message.
     *
     * @return The trigger label.
     */
    protected String getDeliveryOptionsLabel() {
        return "Sends " + getDeliveryOptionTypesLabel() + " notification when triggered.";
    }

    @Override
    public String getLabel() {

        String deliveryLabel = getDeliveryOptionTypes().stream()
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
    public String getDeliveryOptionTypesNoteHtml() {
        return getDeliveryOptionsLabel();
    }
}
