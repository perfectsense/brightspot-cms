package com.psddev.cms.tool.search;

import com.psddev.cms.tool.Search;
import com.psddev.cms.tool.SearchResultAction;
import com.psddev.cms.tool.SearchResultSelection;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.cms.tool.page.BulkWorkflow;

import javax.servlet.ServletException;
import java.io.IOException;

public class BulkWorkflowSearchResultAction implements SearchResultAction {

    @Override
    public String getGroup() {
        return "Bulk";
    }

    @Override
    public void writeHtml(ToolPageContext page, Search search, SearchResultSelection selection) throws IOException {

        try {
            new BulkWorkflow().execute(page, search, selection, BulkWorkflow.WidgetState.BUTTON);
        } catch (ServletException e) {
            throw new IOException(e);
        }
    }
}
