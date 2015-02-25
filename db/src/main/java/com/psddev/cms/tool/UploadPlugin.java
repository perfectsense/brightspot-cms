package com.psddev.cms.tool;

import java.io.IOException;

public interface UploadPlugin {

    public boolean isSupported(String key);

    public String getClassIdentifier();

    public void writeHtml(ToolPageContext page, String storageKey) throws IOException;

}
