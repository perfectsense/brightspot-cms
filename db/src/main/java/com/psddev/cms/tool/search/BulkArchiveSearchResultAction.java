package com.psddev.cms.tool.search;

import com.psddev.cms.tool.page.BulkArchive;
import com.psddev.cms.tool.Search;
import com.psddev.cms.tool.SearchResultAction;
import com.psddev.cms.tool.SearchResultSelection;
import com.psddev.cms.tool.ToolPageContext;

import javax.servlet.ServletException;
import java.io.IOException;

public class BulkArchiveSearchResultAction implements SearchResultAction {

    @Override
    public void writeHtml(ToolPageContext page, Search search, SearchResultSelection selection) throws IOException {

        try {
            new BulkArchive().execute(page, search, selection, BulkArchive.WidgetState.BUTTON, BulkArchive.Action.ARCHIVE);
        } catch (ServletException e) {
            throw new IOException(e);
        }

        try {
            new BulkArchive().execute(page, search, selection, BulkArchive.WidgetState.BUTTON, BulkArchive.Action.RESTORE);
        } catch (ServletException e) {
            throw new IOException(e);
        }
    }
}
