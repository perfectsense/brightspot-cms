package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.ServletException;

import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.PaginatedResult;
import com.psddev.dari.util.RoutingFilter;

@RoutingFilter.Path(application = "cms", value = "/adminTrash")
@SuppressWarnings("serial")
public class AdminTrash extends PageServlet {

    @Override
    protected String getPermissionId() {
        return "area/admin/trash";
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        Object trash = Query
                .from(Object.class)
                .where("_id = ?", page.param(UUID.class, "id"))
                .first();

        if (page.tryStandardUpdate(trash)) {
            return;
        }

        PaginatedResult<Object> trashes = Query
                .from(Object.class)
                .where("cms.content.trashed = true")
                .sortDescending("cms.content.updateDate")
                .select(page.param(long.class, "offset"), page.paramOrDefault(int.class, "limit", 10));

        page.writeHeader();
            page.writeStart("div", "class", "withLeftNav");
                page.writeStart("div", "class", "leftNav");
                    page.writeStart("div", "class", "widget");
                        page.writeStart("h1", "class", "icon icon-action-trash");
                            page.writeHtml("Trash");
                        page.writeEnd();

                        if (trashes.getOffset() + trashes.getItems().size() <= 0) {
                            page.writeStart("div", "class", "message message-info");
                                page.writeHtml("Nothing in trash!");
                            page.writeEnd();

                        } else {
                            if (trashes.hasPrevious() || trashes.hasNext()) {
                                page.writeStart("ul", "class", "pagination");
                                    if (trashes.hasPrevious()) {
                                        page.writeStart("li", "class", "paginationPrevious");
                                            page.writeStart("a", "href", page.url("", "offset", trashes.getPreviousOffset()));
                                                page.writeHtml("Newer");
                                            page.writeEnd();
                                        page.writeEnd();
                                    }

                                    if (trashes.hasNext()) {
                                        page.writeStart("li", "class", "paginationNext");
                                            page.writeStart("a", "href", page.url("", "offset", trashes.getNextOffset()));
                                                page.writeHtml("Older");
                                            page.writeEnd();
                                        page.writeEnd();
                                    }
                                page.writeEnd();
                            }

                            page.writeStart("ul", "class", "links");
                                for (Object item : trashes.getItems()) {
                                    State itemState = State.getInstance(item);

                                    page.writeStart("li", "class", item.equals(trash) ? "selected" : null);
                                        page.writeStart("a", "href", page.url(null, "id", itemState.getId()));
                                            page.writeHtml(page.getObjectLabelOrDefault(itemState, "Untitled"));
                                        page.writeEnd();
                                    page.writeEnd();
                                }
                            page.writeEnd();
                        }
                    page.writeEnd();
                page.writeEnd();

                if (trash != null) {
                    page.writeStart("div", "class", "main");
                        page.writeStart("div", "class", "widget");
                            page.writeStandardForm(trash);
                        page.writeEnd();
                    page.writeEnd();
                }
            page.writeEnd();
        page.writeFooter();
    }
}
