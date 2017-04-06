package com.psddev.cms.db;

/**
 * @deprecated Use {@link com.psddev.cms.notification.DeliveryOption} instead.
 */
@Deprecated
public enum NotificationMethod {

    EMAIL("Email"),
    SMS("Text Message");

    private String displayName;

    private NotificationMethod(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
