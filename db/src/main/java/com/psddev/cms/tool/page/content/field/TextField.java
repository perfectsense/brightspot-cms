package com.psddev.cms.tool.page.content.field;

import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class TextField {

    /**
     * Validates and associates the given {@code value} with the given
     * {@code field} in the given {@code state}.
     *
     * @param state Nonnull.
     * @param field Nonnull.
     * @param value Nullable.
     */
    public static void put(State state, ObjectField field, Object value) {
        if (value != null) {
            String type = field.getInternalItemType();

            if (ObjectField.UUID_TYPE.equals(type)) {
                recurse(value, item -> {
                    if (ObjectUtils.to(UUID.class, item) == null) {
                        state.addError(field, String.format(
                                "[%s] is not a UUID!", item));
                    }
                });
            }
        }

        state.put(field.getInternalName(), value);
    }

    private static void recurse(Object value, Consumer<Object> consumer) {
        if (value != null) {
            if (value instanceof Iterable) {
                for (Object item : (Iterable<?>) value) {
                    recurse(item, consumer);
                }

            } else if (value instanceof Map) {
                for (Object item : ((Map<?, ?>) value).values()) {
                    recurse(item, consumer);
                }

            } else {
                consumer.accept(value);
            }
        }
    }
}
