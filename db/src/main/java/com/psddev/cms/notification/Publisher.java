package com.psddev.cms.notification;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.psddev.dari.db.ObjectType;
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
     * Sub-classes may override this method to filter the types of Receivers
     * eligible to receive this notification.
     *
     * @return The type of receivers to notify. Default is ALL types.
     */
    protected Class<? extends Receiver> getReceiverType() {
        return Receiver.class;
    }

    /**
     * Sub-classes may override this method to alter the query used to fetch
     * potential receivers.
     *
     * @return The query used to fetch all potential receivers.
     */
    protected Query<? extends Receiver> getReceiversQuery() {

        Query<? extends Receiver> receiverQuery = Query.from(getReceiverType());

        Class<S> subscriptionTypeClass = getSubscriptionType();

        if (subscriptionTypeClass != null) {
            ObjectType subscriptionType = ObjectType.getInstance(subscriptionTypeClass);

            if (subscriptionType != null) {
                receiverQuery.where(ReceiverData.SUBSCRIPTION_TYPES_FIELD + " = ?", subscriptionType);
            }
        }

        return receiverQuery;
    }

    /**
     * Gets the potential receivers to be notified when the subscription is
     * published. The default implementation queries for all Receiver instances
     * in the database. Sub-classes may override this method to provide a
     * narrower set of receivers that can receive the notification.
     *
     * @return The receivers that should be notified.
     */
    protected Iterable<? extends Receiver> getReceivers() {
        return getReceiversQuery().iterable(0);
    }

    /**
     * Notifies this receiver about a particular subscription.
     *
     * @param subscriptionClass The type of subscription to notify on.
     * @param context Contextual data about the subscription.
     */
    protected final void notifyReceiver(Receiver receiver, Class<S> subscriptionClass, C context) {

        ReceiverData receiverData = receiver.as(ReceiverData.class);

        List<DeliveryOption> receiverDeliveryOptions = receiverData.getDeliveryOptions();

        // Get the subscriptions matching the given the subscription class type.
        List<Subscription<?>> subscriptions = receiverData.getSubscriptions().stream()
                .filter(s -> subscriptionClass.isAssignableFrom(s.getClass()))
                .collect(Collectors.toList());

        for (Subscription<?> subscription : subscriptions) {

            @SuppressWarnings("unchecked")
            Notification<C> notification = new Notification<>((S) subscription, receiver, context);

            Set<ObjectType> deliveryOptionTypes = subscription.getDeliveryOptionTypes();

            // Get the delivery options from the subscription that match those configured by the receiver.
            List<DeliveryOption> deliveryOptions = receiverDeliveryOptions.stream()
                    .filter(rdo -> deliveryOptionTypes.contains(rdo.getState().getType()))
                    .collect(Collectors.toList());

            for (DeliveryOption deliveryOption : deliveryOptions) {
                deliveryOption.deliver(notification);
            }
        }
    }

    /**
     * Publishes the subscription with the given {@code context} by notifying
     * all registered receivers as determined by {@link #getReceivers()}.
     */
    public void publishNotification() {
        for (Receiver receiver : getReceivers()) {
            notifyReceiver(receiver, getSubscriptionType(), context);
        }
    }
}
