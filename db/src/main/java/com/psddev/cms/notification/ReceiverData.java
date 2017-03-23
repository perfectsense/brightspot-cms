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
 * The delivery preferences and registered subscriptions for a notification recipient.
 */
@Recordable.FieldInternalNamePrefix(ReceiverData.FIELD_PREFIX)
public class ReceiverData extends Modification<Receiver> {

    private static final String TAB_NAME = "Notifications";

    static final String FIELD_PREFIX = "cms.notification.";

    public static final String DELIVERY_OPTIONS_FIELD = FIELD_PREFIX + "deliveryOptions";

    public static final String SUBSCRIPTIONS_FIELD = FIELD_PREFIX + "subscriptions";

    public static final String SUBSCRIPTION_TYPES_FIELD = FIELD_PREFIX + "getSubscriptionTypes";

    @ToolUi.Tab(TAB_NAME)
    @ToolUi.Heading("Delivery Preferences")
    @ToolUi.Unlabeled
    @Embedded
    @Indexed
    private List<DeliveryOption> deliveryOptions;

    @ToolUi.Tab(TAB_NAME)
    @ToolUi.Heading("Notifications")
    @ToolUi.Unlabeled
    @ToolUi.Expanded
    @Embedded
    @Indexed
    private List<Subscription<?>> subscriptions;

    /**
     * Gets the delivery options for this receiver.
     *
     * @return Never {@code null}.
     */
    public List<DeliveryOption> getDeliveryOptions() {
        if (deliveryOptions == null) {
            deliveryOptions = new ArrayList<>();
        }
        return deliveryOptions;
    }

    /**
     * Sets the delivery options for this receiver.
     *
     * @param deliveryOptions The list of delivery options to set.
     */
    public void setDeliveryOptions(List<DeliveryOption> deliveryOptions) {
        this.deliveryOptions = deliveryOptions;
    }

    /**
     * Gets the subscriptions this receiver should be notified about.
     *
     * @return Never {@code null}.
     */
    public List<Subscription<?>> getSubscriptions() {
        if (subscriptions == null) {
            subscriptions = new ArrayList<>();
        }
        return subscriptions;
    }

    /**
     * Sets the subscriptions this receiver should be notified about.
     *
     * @param subscriptions The list of subscriptions to set.
     */
    public void setSubscriptions(List<Subscription<?>> subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Indexed
    @ToolUi.Hidden
    public Set<ObjectType> getSubscriptionTypes() {
        return getSubscriptions().stream()
                .map(s -> s.getState().getType())
                .collect(Collectors.toSet());
    }
}
