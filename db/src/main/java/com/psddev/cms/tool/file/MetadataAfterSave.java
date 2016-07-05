package com.psddev.cms.tool.file;

import com.psddev.dari.util.StorageItem;
import com.psddev.dari.util.StorageItemAfterSave;

/**
 * {@link StorageItemAfterSave} implementation that updates storage item
 * metadata.
 */
public class MetadataAfterSave implements StorageItemAfterSave {

    @Override
    public void afterSave(StorageItem item) {
        if (item != null) {
            StorageItemMetadataCache.update(item);
        }
    }
}
