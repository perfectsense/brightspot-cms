package com.psddev.cms.tool;

import java.io.IOException;

public abstract class UpdatingContentEditWidget extends ContentEditWidget {

    /**
     * @param page Nonnull.
     * @param content Nonnull.
     * @param section {@code null} on update.
     */
    public abstract void displayOrUpdate(ToolPageContext page, Object content, ContentEditSection section) throws IOException;

    @Override
    public final void display(ToolPageContext page, Object content, ContentEditSection section) throws IOException {
        displayOrUpdate(page, content, section);
    }
}
