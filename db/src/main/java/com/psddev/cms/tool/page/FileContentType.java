package com.psddev.cms.tool.page;

import java.io.IOException;
import java.lang.reflect.Modifier;

import javax.servlet.ServletException;

import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ClassFinder;
import com.psddev.dari.util.StorageItem;
import com.psddev.dari.util.TypeDefinition;

public interface FileContentType {

    public boolean isSupported(StorageItem storageItem);
    public boolean isPreferred(StorageItem storageItem);
    public void writePreview(ToolPageContext page) throws IOException, ServletException;
    public void setMetadata(ToolPageContext page, State state, StorageItem fieldValue) throws IOException, ServletException;

    public static class Static {

        public static FileContentType getFileFieldWriter(StorageItem storageItem) {

            if (storageItem == null) {
                return null;
            }

            FileContentType fileContentType = null;

            for (Class<? extends FileContentType> contentTypeClass : ClassFinder.Static.findClasses(FileContentType.class)) {
                if (!contentTypeClass.isInterface() && !Modifier.isAbstract(contentTypeClass.getModifiers())) {
                    FileContentType tester = TypeDefinition.getInstance(contentTypeClass).newInstance();
                    if (tester.isSupported(storageItem)) {
                        fileContentType = tester;

                        if (tester.isPreferred(storageItem)) {
                            break;
                        }
                    }
                }
            }

            return fileContentType;
        }
    }
}
