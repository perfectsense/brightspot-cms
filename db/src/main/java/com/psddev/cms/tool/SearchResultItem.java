package com.psddev.cms.tool;

import java.io.IOException;

import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;

public class SearchResultItem {

    public void writeCheckboxHtml(ToolPageContext page, Search search, Object item) throws IOException {
        String url = page.toolUrl(CmsTool.class, "/searchResultActions",
                "search", ObjectUtils.toJson(search.getState().getSimpleValues()),
                "id", State.getInstance(item).getId());

        page.writeElement("input",
                "type", "checkbox",
                "name", "id",
                "value", State.getInstance(item).getId(),
                "data-frame-target", "searchResultActions",
                "data-frame-check", StringUtils.addQueryParameters(url, "action", "item-add"),
                "data-frame-uncheck", StringUtils.addQueryParameters(url, "action", "item-remove"));
    }

    public void writeBeforeHtml(ToolPageContext page, Search search, Object item) throws IOException {
        page.writeStart("a",
                "href", page.objectUrl("/content/edit.jsp", item,
                        "search", ObjectUtils.toJson(search.getState().getSimpleValues())),
                "data-objectId", State.getInstance(item).getId(),
                "target", "_top");
    }

    public void writeAfterHtml(ToolPageContext page, Search search, Object item) throws IOException {
        page.writeEnd();
    }
}
