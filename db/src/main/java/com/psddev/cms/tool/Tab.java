package com.psddev.cms.tool;

import java.io.IOException;

/**
 * Tab display within {@link ToolPageContext#writeSomeFormFields}.
 */
public interface Tab {

    /**
     * Returns the display name.
     *
     * @return Nonnull.
     */
    String getDisplayName();

    /**
     * Returns {@code true} if the tab should be displayed for the given
     * {@code content}.
     *
     * @param content Nonnull.
     */
    boolean shouldDisplay(Object content);

    /**
     * Writes the tab display to the given {@code page} for the given
     * {@code content}.
     *
     * @param page Nonnull.
     * @param content Nonnull.
     */
    void writeHtml(ToolPageContext page, Object content) throws IOException;

    /**
     * Called when updating the given {@code content}.
     *
     * @param page Nonnull.
     * @param content Nonnull.
     */
    default void onUpdate(ToolPageContext page, Object content) {
    }
}
