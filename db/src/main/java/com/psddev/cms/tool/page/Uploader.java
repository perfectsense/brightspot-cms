package com.psddev.cms.tool.page;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Optional;

import com.psddev.cms.tool.CmsTool;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Application;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.util.ClassFinder;
import com.psddev.dari.util.TypeDefinition;

/**
 * An interface to extend the StorageItemField to allow
 * for adding front end uploaders
 */
interface Uploader {

    public static final double DEFAULT_PRIORITY = 0;

    /**
     * Returns {@code double} as a priority rating for
     * this Uploader. The highest priority will be used
     * by {@code StorageItemField}. Return a value less
     * than zero if Uploader should not be supported.
     *
     * @param field
     */
    public double getPriority(Optional<ObjectField> field);

    /**
     * Returns class used by javascript to which DOM
     * elements the uploader requires
     */
    public String getClassIdentifier();

    /**
     * @param page   Can't be {@code null}.
     * @param field
     * @throws IOException if unable to write to the given {@code page}.
     */
    public void writeHtml(
            ToolPageContext page,
            Optional<ObjectField> field)
            throws IOException;

    static Uploader getUploader(Optional<ObjectField> field) {

        Uploader uploader = null;

        if (Application.Static.getInstance(CmsTool.class).isEnableFrontEndUploader()) {
            for (Class<? extends Uploader> uploaderClass : ClassFinder.Static.findClasses(Uploader.class)) {

                if (uploaderClass.isInterface() || Modifier.isAbstract(uploaderClass.getModifiers())) {
                    continue;
                }

                Uploader candidateUploader = TypeDefinition.getInstance(uploaderClass).newInstance();

                if (candidateUploader.getPriority(field) >= 0) {

                    if (uploader == null || uploader.getPriority(field) < candidateUploader.getPriority(field)) {
                        uploader = candidateUploader;
                    }
                }
            }
        }

        return uploader;
    }
}
