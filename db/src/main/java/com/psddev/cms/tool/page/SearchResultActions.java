package com.psddev.cms.tool.page;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;

import com.psddev.cms.db.ToolUser;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.Search;
import com.psddev.cms.tool.SearchResultAction;
import com.psddev.cms.tool.SearchResultSelection;
import com.psddev.cms.tool.SearchResultSelectionItem;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.ClassFinder;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.TypeDefinition;

@RoutingFilter.Path(application = "cms", value = "/searchResultActions")
public class SearchResultActions extends PageServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        Search search = new Search();
        @SuppressWarnings("unchecked")
        Map<String, Object> searchValues = (Map<String, Object>) ObjectUtils.fromJson(page.param(String.class, "search"));

        search.getState().setValues(searchValues);

        String action = page.param(String.class, "action");
        ToolUser user = page.getUser();
        SearchResultSelection selection = user.getCurrentSearchResultSelection();

        if (selection == null) {
            throw new IllegalStateException();
        }

        if ("item-add".equals(action)) {
            UUID itemId = page.param(UUID.class, "id");

            if (!Query.
                    from(SearchResultSelectionItem.class).
                    where("selectionId = ?", selection.getId()).
                    and("itemId = ?", itemId).
                    hasMoreThan(0)) {

                SearchResultSelectionItem item = new SearchResultSelectionItem();

                item.setSelectionId(selection.getId());
                item.setItemId(itemId);
                item.save();
            }

        } else if ("item-remove".equals(action)) {
            Query.
                    from(SearchResultSelectionItem.class).
                    where("selectionId = ?", selection.getId()).
                    and("itemId = ?", page.param(UUID.class, "id")).
                    deleteAll();

        } else if ("clear".equals(action)) {
            Query.
                    from(SearchResultSelectionItem.class).
                    where("selectionId = ?", selection.getId()).
                    deleteAll();

            page.writeStart("div", "id", page.createId());
            page.writeEnd();

            page.writeStart("script", "type", "text/javascript");
                page.writeRaw("$('#" + page.getId() + "').closest('.search-result').find('.searchResultList :checkbox').prop('checked', false);");
            page.writeEnd();
        }

        long count = Query.
                from(SearchResultSelectionItem.class).
                where("selectionId = ?", selection.getId()).
                count();

        if (count > 0) {
            page.writeStart("h2");
                page.writeHtml("Selection");
            page.writeEnd();

            page.writeStart("a",
                    "class", "action action-cancel",
                    "href", page.url("", "action", "clear"));

                page.writeHtml("Clear");
            page.writeEnd();

            page.writeStart("p");
                page.writeHtml(count);
                page.writeHtml(" items selected.");
            page.writeEnd();

        } else {
            page.writeStart("h2");
                page.writeHtml("All");
            page.writeEnd();
        }

        for (Class<? extends SearchResultAction> c : ClassFinder.Static.findClasses(SearchResultAction.class)) {
            if (!c.isInterface() && !Modifier.isAbstract(c.getModifiers())) {
                TypeDefinition.
                        getInstance(c).
                        newInstance().
                        writeHtml(page, search, count > 0 ? selection : null);
            }
        }
    }
}
