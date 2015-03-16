package com.psddev.cms.tool.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.psddev.cms.db.Site;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.tool.CmsTool;
import com.psddev.cms.tool.Search;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Predicate;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.PaginatedResult;

public class GridSearchResultView extends ListSearchResultView {

    @Override
    public String getIconName() {
        return "th-large";
    }

    @Override
    public String getDisplayName() {
        return "Grid";
    }

    @Override
    public boolean isSupported(Search search) {
        ObjectType selectedType = search.getSelectedType();

        return selectedType != null && selectedType.getPreviewField() != null;
    }

    @Override
    protected void doWriteHtml() throws IOException {
        ObjectType selectedType = search.getSelectedType();

        sortField = updateSort();
        showSiteLabel = Query.from(CmsTool.class).first().isDisplaySiteInSearchResult() &&
                page.getSite() == null &&
                Query.from(Site.class).hasMoreThan(0);

        if (selectedType != null) {
            showTypeLabel = selectedType.as(ToolUi.class).findDisplayTypes().size() != 1;

            if (ObjectType.getInstance(ObjectType.class).equals(selectedType)) {
                List<ObjectType> types = new ArrayList<ObjectType>();
                Predicate predicate = search.toQuery(page.getSite()).getPredicate();

                for (ObjectType t : Database.Static.getDefault().getEnvironment().getTypes()) {
                    if (t.is(predicate)) {
                        types.add(t);
                    }
                }

                result = new PaginatedResult<ObjectType>(search.getOffset(), search.getLimit(), types);
            }

        } else {
            showTypeLabel = search.findValidTypes().size() != 1;
        }

        if (result == null) {
            result = search.toQuery(page.getSite()).select(search.getOffset(), search.getLimit());
        }

        writeSortsHtml();

        page.writeStart("div", "class", "searchResult-list infiniteScroll");
        if (result.hasPages()) {
            writeItemsHtml(result.getItems());
            writePaginationHtml(result);

        } else {
            writeEmptyHtml();
        }
        page.writeEnd();
    }

    @Override
    public boolean isPreferred(Search search) {
        return isSupported(search);
    }

    @Override
    protected void writeItemsHtml(Collection<?> items) throws IOException {
        writeImagesHtml(items);
    }
}
