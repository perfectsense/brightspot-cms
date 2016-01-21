package com.psddev.cms.tool.file;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.psddev.dari.util.AbstractStorageItem;
import com.psddev.dari.util.StorageItem;
import com.psddev.dari.util.StorageItemBeforeSave;
import com.psddev.dari.util.StorageItemUploadPart;

public class MetadataBeforeSave implements StorageItemBeforeSave {

    @Override
    public void beforeSave(StorageItem storageItem, StorageItemUploadPart part) {

        if (part != null) {

            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("originalFilename", part.getName());

            Map<String, List<String>> httpHeaders = new LinkedHashMap<>();
            httpHeaders.put("Cache-Control", Collections.singletonList("public, max-age=31536000"));
            httpHeaders.put("Content-Length", Collections.singletonList(String.valueOf(part.getSize())));
            httpHeaders.put("Content-Type", Collections.singletonList(part.getContentType()));
            metadata.put(AbstractStorageItem.HTTP_HEADERS, httpHeaders);

            storageItem.getMetadata().putAll(metadata);
        }
    }
}
