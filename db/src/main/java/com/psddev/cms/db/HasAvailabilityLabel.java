package com.psddev.cms.db;

import com.psddev.dari.db.State;
import com.psddev.dari.db.Recordable;

/**
 * Created by rhseeger on 11/3/15.
 */
public interface HasAvailabilityLabel extends Recordable {
    /**
     * Returns the label that identifies the current visibility in effect.
     * Defaults to returning the same value from the object's {@code State}, but
     * can be overridden by implementors to use more complicated logic, such
     * as returning the visibilityLabel of a specific field.
     *
     * @return May be {@code null}.
     */
    public default String getAvailabilityLabel() {
        return State.getInstance(this).getVisibilityLabel();
    }
}
