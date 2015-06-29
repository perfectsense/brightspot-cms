package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.util.AmazonStorageItem;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.Settings;
import com.psddev.dari.util.StorageItem;
import com.psddev.dari.util.StringUtils;

class AmazonUploader implements Uploader {

    @Override
    public double getPriority(Optional<ObjectField> field) {

        String storageItemClassName = Settings.getOrDefault(String.class, StorageItem.SETTING_PREFIX + "/" + StorageItemField.getStorageSetting(field) + "/class", "");

        if (StringUtils.isBlank(storageItemClassName)) {
            return -1;
        }

        Class<?> storageItemClass = ObjectUtils.getClassByName(storageItemClassName);
        if (storageItemClass == null || !AmazonStorageItem.class.isAssignableFrom(storageItemClass)) {
            return -1;
        }

        return DEFAULT_PRIORITY;
    }

    @Override
    public String getClassIdentifier() {
        return "evaporate";
    }

    @Override
    public void writeHtml(ToolPageContext page, Optional<ObjectField> field) throws IOException {
        String storageSetting = StorageItemField.getStorageSetting(field);

        if (StringUtils.isBlank(storageSetting)) {
            return;
        }

        page.writeTag("meta",
                "name", "evaporateSettings",
                "content", ObjectUtils.toJson(ImmutableMap.of(
                                "signerUrl", page.cmsUrl("/amazonAuth"),
                                "aws_key", Settings.get(String.class, StorageItem.SETTING_PREFIX + "/" + storageSetting + "/" + AmazonStorageItem.ACCESS_SETTING),
                                "bucket", Settings.get(String.class, StorageItem.SETTING_PREFIX + "/" + storageSetting + "/" + AmazonStorageItem.BUCKET_SETTING)
                )),
                "data-path-start", StorageItemField.createStoragePathPrefix(),
                "data-field-name", field.isPresent() ? field.get().getInternalName() : null,
                "data-storage", storageSetting,
                "data-type-id", field.isPresent() ? field.get().getParentType().getId() : null);
    }
}
