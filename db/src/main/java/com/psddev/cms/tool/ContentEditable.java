package com.psddev.cms.tool;

import com.psddev.dari.db.State;

/**
 * Interface for displaying {@link com.psddev.cms.db.Content} edit form as read-only.
 */
public interface ContentEditable {

    /**
     * Returns {@code true} if {@link com.psddev.cms.db.Content}
     * edit form should be editable.
     */
    public boolean shouldContentBeEditable();

    /**
     * Returns {@code true} if the given Object
     * should be editable.
     * @param object Can't be {@code null}
     */
    public static boolean shouldContentBeEditable(Object object) {
        if (object instanceof State) {
            object = ((State) object).getOriginalObject();
        }

        return !(object instanceof ContentEditable)
                || ((ContentEditable) object).shouldContentBeEditable();
    }
}
