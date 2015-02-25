package com.psddev.cms.tool;

import java.io.IOException;

import com.google.common.collect.ImmutableMap;
import com.psddev.dari.util.AmazonStorageItem;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.Settings;
import com.psddev.dari.util.StorageItem;

public class EvaporateUploadPlugin implements UploadPlugin {

    @Override
    public boolean isSupported(String key) {
        return Settings.getOrDefault(String.class, StorageItem.SETTING_PREFIX + "/" + key + "/class", "").equals(AmazonStorageItem.class.getName());

    }

    @Override
    public String getClassIdentifier() {
        return "s3Upload";
    }

    @Override
    public void writeHtml(ToolPageContext page, String storageKey) throws IOException {

        page.writeStart("script", "type", "text/javascript");
            page.writeRaw("var _e_ = new Evaporate(");
            page.write(ObjectUtils.toJson(ImmutableMap.of(
                    "signerUrl", "/cms/s3auth",
                    "aws_key", Settings.get(String.class, StorageItem.SETTING_PREFIX + "/" + storageKey + "/access"),
                    "bucket", Settings.get(String.class, StorageItem.SETTING_PREFIX + "/" + storageKey + "/bucket"))));
            page.writeRaw(");");
        page.writeEnd();

    }
}
