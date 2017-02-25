package com.psddev.cms.notification;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.psddev.cms.db.ToolUi;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.util.ClassFinder;
import com.psddev.dari.util.TypeDefinition;

/**
 * Creates a notification to be sent.
 *
 * @param <T> The type of contextual data used to create the notification.
 */
@Recordable.Embedded
public abstract class Notification<T> extends Record {

    @ToolUi.NoteHtml("<span data-dynamic-html='${content.getSendersNoteHtml()}'></span>")
    @Where("groups = " + NotificationSender.INTERNAL_NAME
            + " && internalName != " + NotificationSender.INTERNAL_NAME)
    @ToolUi.DropDown
    @DisplayName("Delivery Preference")
    private Set<ObjectType> senderTypes;

    /**
     * Gets the list of notification senderTypes configured to deliver
     * this notification.
     *
     * @return Never {@code null}.
     */
    public Set<ObjectType> getSenderTypes() {
        if (senderTypes == null) {
            senderTypes = new LinkedHashSet<>();
        }
        return senderTypes;
    }

    /**
     * Sets the list of notification senderTypes configured to deliver
     * this notification.
     *
     * @param senderTypes the list of notification senderTypes to set.
     */
    public void setSenderTypes(Set<ObjectType> senderTypes) {
        if (senderTypes != null) {
            // filter out un supported types
            this.senderTypes = senderTypes.stream()
                    .filter(type -> type.getGroups().contains(NotificationSender.INTERNAL_NAME))
                    .filter(type -> !type.getInternalName().equals(NotificationSender.INTERNAL_NAME))
                    .collect(Collectors.toCollection(LinkedHashSet::new));

        } else {
            this.senderTypes = null;
        }
    }

    /**
     * Creates the default message format to be sent.
     *
     * @param receiver The receiver of the notification.
     * @param context The contextual data used to create the notification.
     * @return The notification to be sent.
     */
    protected final Message getMessage(NotificationReceiver receiver, T context) {

        Object defaultFormat = getDefaultMessageFormat(receiver, context);

        Message message = new Message();

        if (defaultFormat != null) {
            message.addFormat(defaultFormat);
        }

        for (Class<? extends MessageFormatter> formatterClass : ClassFinder.findConcreteClasses(MessageFormatter.class)) {

            TypeDefinition<? extends MessageFormatter> formatterTypeDef = TypeDefinition.getInstance(formatterClass);

            Class<?> notificationClass = formatterTypeDef.getInferredGenericTypeArgumentClass(MessageFormatter.class, 0);
            Class<?> contextClass = formatterTypeDef.getInferredGenericTypeArgumentClass(MessageFormatter.class, 1);

            if (getClass().isAssignableFrom(notificationClass)
                    && context.getClass().isAssignableFrom(contextClass)) {

                MessageFormatter<Notification<T>, T> formatter = formatterTypeDef.newInstance();

                Object format = formatter.format(this, receiver, context);

                if (format != null) {
                    message.addFormat(format);
                }
            }
        }

        return message;
    }

    /**
     * Creates the default message format to be sent.
     *
     * @param receiver The receiver of the notification.
     * @param context The contextual data used to create the notification.
     * @return The notification to be sent.
     */
    protected abstract Object getDefaultMessageFormat(NotificationReceiver receiver, T context);

    /**
     * Gets a user friendly label that describes the types of senders
     * configured to deliver this notification.
     *
     * @return a user friendly label.
     */
    protected String getSenderTypesLabel() {
        List<String> senderLabels = getSenderTypes().stream()
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
    protected String getSendersLabel() {
        return "Sends " + getSenderTypesLabel() + " notification when triggered.";
    }

    @Override
    public String getLabel() {

        String deliveryLabel = getSenderTypes().stream()
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
    public String getSendersNoteHtml() {
        return getSendersLabel();
    }
}
