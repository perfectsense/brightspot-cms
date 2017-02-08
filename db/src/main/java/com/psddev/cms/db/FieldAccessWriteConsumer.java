package com.psddev.cms.db;

import java.io.IOException;
import java.io.Writer;

/**
 * {@link java.util.function.Consumer}-like interface for use with
 * {@link FieldAccessFilter#write(boolean, FieldAccessWriteConsumer)}.
 */
@FunctionalInterface
public interface FieldAccessWriteConsumer {

    /**
     * Performs this operation on the given {@code writer}.
     *
     * @param writer Nonnull.
     */
    void accept(Writer writer) throws IOException;
}
