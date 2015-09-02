package com.psddev.cms.tool.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;

import com.psddev.cms.tool.Search;
import com.psddev.cms.tool.Tool;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.StorageItem;

public class MixedSearchResultView extends ListSearchResultView {

    @Override
    public String getIconName() {
        return "asterisk";
    }

    @Override
    public String getDisplayName() {
        return "Mixed";
    }

    @Override
    public boolean isSupported(Search search) {
        return search.getSelectedType() == null;
    }

    @Override
    public boolean isPreferred(Search search) {
        return true;
    }

    @Override
    protected void writeItemsHtml(Collection<?> items) throws IOException {
        List<Object> tableItems = new ArrayList<>(items);
        List<Object> imageItems = new ArrayList<>();

        ITEM: for (ListIterator<Object> i = tableItems.listIterator(); i.hasNext();) {
            Object item = i.next();

            for (Tool tool : Query.from(Tool.class).selectAll()) {
                if (!tool.isDisplaySearchResultItem(search, item)) {
                    continue ITEM;
                }
            }

            State itemState = State.getInstance(item);
            StorageItem preview = itemState.getPreview();

            if (preview != null) {
                String contentType = preview.getContentType();

                if (contentType != null && contentType.startsWith("image/")) {
                    i.remove();
                    imageItems.add(item);
                }
            }
        }

        page.writeStart("div", "class", "searchResult-mixed");
            if (!imageItems.isEmpty()) {
                writeImagesHtml(imageItems);
            }

            if (!tableItems.isEmpty()) {
                writeTableHtml(tableItems);
            }
        page.writeEnd();
    }
}
