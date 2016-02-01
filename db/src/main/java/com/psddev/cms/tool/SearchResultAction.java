package com.psddev.cms.tool;

import java.io.IOException;

public interface SearchResultAction {

    default int getPosition() {
        return 0;
    }

    public void writeHtml(
            ToolPageContext page,
            Search search,
            SearchResultSelection selection)
            throws IOException;
}
