package com.psddev.cms.tool.search;

import com.psddev.cms.tool.Search;
import com.psddev.cms.tool.SearchResultAction;
import com.psddev.cms.tool.SearchResultSelection;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.cms.tool.page.ExportContent;

import javax.servlet.ServletException;
import java.io.IOException;

public class ExportContentSearchResultAction implements SearchResultAction {

    @Override
    public String getGroup() {
        return "Export";
    }

    @Override
    public void writeHtml(ToolPageContext page, Search search, SearchResultSelection selection) throws IOException {
        try {
            new ExportContent().execute(page, search, selection);
        } catch (ServletException e) {
            throw new IOException(e);
        }
    }
}
