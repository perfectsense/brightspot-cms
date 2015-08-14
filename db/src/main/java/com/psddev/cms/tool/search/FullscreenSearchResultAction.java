package com.psddev.cms.tool.search;

import java.io.IOException;

import com.psddev.cms.tool.Search;
import com.psddev.cms.tool.SearchResultAction;
import com.psddev.cms.tool.SearchResultSelection;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.util.UrlBuilder;

public class FullscreenSearchResultAction implements SearchResultAction {

    @Override
    public void writeHtml(
            ToolPageContext page,
            Search search,
            SearchResultSelection selection)
            throws IOException {

        page.writeStart("div", "class", "searchResult-action-simple");
            page.writeStart("a",
                    "class", "button",
                    "target", "_top",
                    "href", new UrlBuilder(page.getRequest())
                            .absolutePath(page.cmsUrl("/searchAdvancedFull"))
                            .currentParameters()
                            .parameter(Search.NAME_PARAMETER, null));
                page.writeHtml("Fullscreen");
            page.writeEnd();
        page.writeEnd();
    }
}
