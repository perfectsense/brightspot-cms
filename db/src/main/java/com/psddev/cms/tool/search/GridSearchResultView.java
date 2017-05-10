package com.psddev.cms.tool.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.psddev.cms.db.Localization;
import com.psddev.cms.db.Site;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.tool.CmsTool;
import com.psddev.cms.tool.Search;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Predicate;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.PaginatedResult;
import com.psddev.dari.util.StringUtils;

public class GridSearchResultView extends ListSearchResultView {

    @Override
    public String getIconName() {
        return "th-large";
    }

    @Override
    public String getDisplayName() {
        return Localization.currentUserText(this, "displayName");
    }

    @Override
    public boolean isSupported(Search search) {
        ObjectType selectedType = search.getSelectedType();

        if (selectedType != null) {
            return selectedType.getPreviewField() != null;

        } else {
            Set<ObjectType> types = search.getTypes();

            if (types.isEmpty()) {
                return false;

            } else {
                for (ObjectType type : types) {
                    if (type.getPreviewField() == null) {
                        return false;
                    }
                }

                return true;
            }
        }
    }

    @Override
    protected void doWriteHtml() throws IOException {
        ObjectType selectedType = search.getSelectedType();

        sortField = updateSort();
        showSiteLabel = Query.from(CmsTool.class).first().isDisplaySiteInSearchResult()
                && Query.from(Site.class).hasMoreThan(0);

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
        // TODO: It would make sense to move this up to the right of the view selection
        //       However, that happens in Search.java, which is also the model for search.
        //       I didn't want to touch that for the proof of concept
        writeSizesHtml();

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
        String size = page.pageParam(String.class, SIZE_PARAMETER, SIZES[0]);
        int sizeIndex = Arrays.asList(SIZES).indexOf(size);
        int maxHeight = SIZES_MAX_HEIGHT[Math.max(0, sizeIndex)];
        //System.out.println("RHS [size=" + size + "] [sizeIndex=" + sizeIndex + "] [maxHeight=" + maxHeight + "]");
        writeImagesHtml(items, maxHeight);
    }

}
