package com.psddev.cms.notification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.psddev.dari.util.ClassFinder;
import com.psddev.dari.util.TypeDefinition;

/**
 * A notification that can be sent to a recipient.
 */
public class Notification<C> {

    private Subscription<C> subscription;
    private Receiver receiver;
    private C context;

    private Map<Class<?>, List<Object>> formats;

    public Notification(Subscription<C> subscription, Receiver receiver, C context) {

        this.subscription = subscription;
        this.receiver = receiver;
        this.context = context;
    }

    /**
     * Gets the last message format added compatible with the given type.
     *
     * @param formatType the type of message format to get.
     * @param <F> the message format type.
     * @return the first format or {@code null} if there are none.
     */
    public <F> F format(Class<F> formatType) {

        // TODO: Can make this lazier to improve performance by not executing certain formatters unless they are absolutely necessary.
        for (Object format : getFormats().values().stream().flatMap(Collection::stream).collect(Collectors.toList())) {

            // TODO: This needs to be smarter to make sure it returns the
            // object whose type is "closest" to the requested type, not just
            // the first one encountered.
            if (formatType.isAssignableFrom(format.getClass())) {
                @SuppressWarnings("unchecked")
                F typedFormat = (F) format;
                return typedFormat;
            }
        }

        return null;
    }

    private Map<Class<?>, List<Object>> getFormats() {

        if (formats == null) {
            formats = new HashMap<>();

            for (Class<? extends MessageFormatter> formatterClass : ClassFinder.findConcreteClasses(MessageFormatter.class)) {

                TypeDefinition<? extends MessageFormatter> formatterTypeDef = TypeDefinition.getInstance(formatterClass);

                Class<?> subscriptionClass = formatterTypeDef.getInferredGenericTypeArgumentClass(MessageFormatter.class, 0);
                Class<?> contextClass = formatterTypeDef.getInferredGenericTypeArgumentClass(MessageFormatter.class, 1);
                Class<?> formatClass = formatterTypeDef.getInferredGenericTypeArgumentClass(MessageFormatter.class, 2);

                if (getClass().isAssignableFrom(subscriptionClass)
                        && context.getClass().isAssignableFrom(contextClass)) {

                    MessageFormatter<Subscription<C>, C, ?> formatter = formatterTypeDef.newInstance();

                    // TODO: Catch formatter errors?
                    Object format = formatter.format(subscription, receiver, context);

                    if (format != null) {
                        List<Object> formatsForType = formats.get(formatClass);

                        if (formatsForType == null) {
                            formatsForType = new ArrayList<>();
                            formats.put(formatClass, formatsForType);
                        }

                        formatsForType.add(format);
                    }
                }
            }

            // Add the default string format at the end so that it can be
            // overridden, since items earlier in the list will get checked first.
            String defaultStringFormat = subscription.toStringFormat(receiver, context);

            if (defaultStringFormat != null) {
                List<Object> stringFormats = formats.get(String.class);

                if (stringFormats == null) {
                    stringFormats = new ArrayList<>();
                    formats.put(String.class, stringFormats);
                }

                stringFormats.add(defaultStringFormat);
            }
        }

        return formats;
    }
}
