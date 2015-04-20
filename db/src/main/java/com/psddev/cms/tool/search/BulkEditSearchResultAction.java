package com.psddev.cms.tool.search;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.psddev.cms.tool.CmsTool;
import com.psddev.cms.tool.Search;
import com.psddev.cms.tool.SearchResultAction;
import com.psddev.cms.tool.SearchResultSelection;
import com.psddev.cms.tool.SearchResultSelectionItem;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.UrlBuilder;

public class BulkEditSearchResultAction implements SearchResultAction {

    @Override
    public void writeHtml(
            ToolPageContext page,
            Search search,
            SearchResultSelection selection)
            throws IOException {

        UUID typeId = null;
        UUID selectionId = null;

        if (selection != null) {
            Set<UUID> itemIds = new HashSet<>();

            for (SearchResultSelectionItem item : Query.
                    from(SearchResultSelectionItem.class).
                    where("selectionId = ?", selection.getId()).
                    selectAll()) {

                itemIds.add(item.getItemId());
            }

            Set<UUID> itemTypeIds = new HashSet<>();

            for (Object item : Query.
                    fromAll().
                    where("_id = ?", itemIds).
                    selectAll()) {

                itemTypeIds.add(State.getInstance(item).getTypeId());
            }

            if (itemTypeIds.size() != 1) {
                return;

            } else {
                typeId = itemTypeIds.iterator().next();
                selectionId = selection.getId();
            }

        } else if (search != null) {
            ObjectType type = search.getSelectedType();

            if (type == null || type.isAbstract()) {
                return;

            } else {
                typeId = type.getId();
            }
        }

        page.writeStart("div", "class", "searchResult-action-simple");
            page.writeStart("a",
                    "class", "button",
                    "target", "_top",
                    "href", new UrlBuilder(page.getRequest()).
                            absolutePath(page.toolPath(CmsTool.class, "/contentEditBulk")).
                            currentParameters().
                            parameter("typeId", typeId).
                            parameter("selectionId", selectionId));
                page.writeHtml("Bulk Edit ");
                page.writeHtml(selection != null ? "Selected" : "All");
            page.writeEnd();
        page.writeEnd();
    }
}
