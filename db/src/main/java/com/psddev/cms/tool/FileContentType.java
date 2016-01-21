package com.psddev.cms.tool;

import java.io.IOException;

import javax.servlet.ServletException;

import com.psddev.dari.db.State;
import com.psddev.dari.util.ClassFinder;
import com.psddev.dari.util.StorageItem;
import com.psddev.dari.util.TypeDefinition;

public interface FileContentType {

    double DEFAULT_PRIORITY_LEVEL = 0;

    /**
     * Returns {@code double} as a priority rating for
     * this FileContentType. The highest priority will be used
     * by {@code StorageItemField}. Return a value less
     * than zero if FileContentType should not be supported.
     *
     * @param storageItem Can't be {@code null}.
     */
    double getPriority(StorageItem storageItem);

    /**
     * Enables additional processing of a {@link StorageItem}
     * during the POST request by {@link com.psddev.cms.tool.page.content.field.StorageItemField}.
     *
     * @param page Can't be {@code null}
     * @param storageItem StorageItem value for the field
     */
    default void process(ToolPageContext page, StorageItem storageItem) {

    }

    /**
     * Enables custom preview rendering for a {@link StorageItem} field in the CMS.
     * Executed only by a GET request in {@link com.psddev.cms.tool.page.content.field.StorageItemField}.
     *
     * @param page Can't be {@code null}.
     * @param state State of the object with the StorageItem field
     * @param fieldValue StorageItem value for the field
     *
     * @throws IOException
     * @throws ServletException
     */
    void writePreview(ToolPageContext page, State state, StorageItem fieldValue) throws IOException, ServletException;

    static FileContentType getFileContentType(StorageItem storageItem) {

        if (storageItem == null) {
            return null;
        }

        FileContentType fileContentType = null;

        for (Class<? extends FileContentType> fileContentTypeClass : ClassFinder.findConcreteClasses(FileContentType.class)) {
            FileContentType candidateFileContentType = TypeDefinition.getInstance(fileContentTypeClass).newInstance();

            if (candidateFileContentType.getPriority(storageItem) >= 0) {

                if (fileContentType == null || fileContentType.getPriority(storageItem) < candidateFileContentType.getPriority(storageItem)) {
                    fileContentType = candidateFileContentType;
                }
            }
        }

        return fileContentType;
    }

    static void writeFilePreview(ToolPageContext page, State state, StorageItem fieldValue) throws IOException, ServletException {

        if (fieldValue == null) {
            return;
        }

        FileContentType fileContentType = FileContentType.getFileContentType(fieldValue);
        if (fileContentType != null) {
            fileContentType.writePreview(page, state, fieldValue);
        } else {
            page.writeStart("a",
                    "href", page.h(fieldValue.getPublicUrl()),
                    "target", "_blank");
                page.write(page.h(fieldValue.getContentType()));
                page.write(": ");
                page.write(page.h(fieldValue.getPath()));
            page.writeEnd();
        }
    }

    /**
     * @deprecated Use {@link #getFileContentType(StorageItem)} instead.
     */
    @Deprecated
    static FileContentType getFileFieldWriter(StorageItem storageItem) {
        return getFileContentType(storageItem);
    }

}
