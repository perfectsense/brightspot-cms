package com.psddev.cms.tool;

import java.io.IOException;
import java.lang.reflect.Modifier;

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

    void writePreview(ToolPageContext page, State state, StorageItem fieldValue) throws IOException, ServletException;

    static FileContentType getFileFieldWriter(StorageItem storageItem) {

        if (storageItem == null) {
            return null;
        }

        FileContentType fileContentType = null;

        for (Class<? extends FileContentType> fileContentTypeClass : ClassFinder.Static.findClasses(FileContentType.class)) {

            if (fileContentTypeClass.isInterface() || Modifier.isAbstract(fileContentTypeClass.getModifiers())) {
                continue;
            }

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

        FileContentType fileContentType = FileContentType.getFileFieldWriter(fieldValue);
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
}
