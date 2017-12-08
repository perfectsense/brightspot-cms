package com.psddev.cms.tool.search;

import com.psddev.cms.db.ToolUser;
import com.psddev.cms.tool.Search;
import com.psddev.cms.tool.SearchResultAction;
import com.psddev.cms.tool.SearchResultSelection;
import com.psddev.cms.tool.ToolPageContext;

import java.io.IOException;

public class SaveSelectionSearchResultAction implements SearchResultAction {

    @Override
    public void writeHtml(
            ToolPageContext page,
            Search search,
            SearchResultSelection selection)
            throws IOException {

        ToolUser user = page.getUser();

        if (selection == null) {
            selection = user.getCurrentSearchResultSelection();
        }

        if (selection == null) {
            return;
        }

        if (user.isSavedSearchResultSelection(selection)) {
            writeAction(page, selection, page.localize(SaveSelectionSearchResultAction.class, "action.editSelection"));

        } else if (selection.size() > 0) {
            writeAction(page, selection, page.localize(SaveSelectionSearchResultAction.class, "action.saveSelection"));
        }
    }

    private void writeAction(ToolPageContext page, SearchResultSelection selection, String key) throws IOException {
        page.writeStart("div", "class", "searchResult-action-simple");
            page.writeStart("a",
                    "class", "button",
                    "target", "toolUserSaveSearch",
                    "href", page.cmsUrl("/toolUserSaveSelection",
                            "selectionId", selection.getId().toString()));
                page.writeHtml(key);
            page.writeEnd();
        page.writeEnd();
    }
}
