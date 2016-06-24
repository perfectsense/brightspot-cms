package com.psddev.cms.tool.page.user;

import com.psddev.cms.db.WorkInProgress;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.StringUtils;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RoutingFilter.Path(application = "cms", value = "/user/wips")
public class ToolUserWorksInProgress extends PageServlet {

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        if (page.param(String.class, "action-delete") != null) {
            Query.from(WorkInProgress.class)
                    .where("_id = ?", page.param(UUID.class, "wip"))
                    .deleteAll();

            String returnUrl = page.param(String.class, "returnUrl");

            if (!ObjectUtils.isBlank(returnUrl)) {
                page.getResponse().sendRedirect(StringUtils.ensureStart(returnUrl, "/"));
                return;
            }
        }

        Query<WorkInProgress> query = Query.from(WorkInProgress.class)
                .where("owner = ?", page.getUser())
                .and("updateDate != missing")
                .sortDescending("updateDate");

        boolean deleteAll = page.isFormPost() && page.param(String.class, "action-delete-all") != null;
        List<WorkInProgress> wips = null;

        if (deleteAll) {
            query.deleteAll();

        } else {
            wips = query.selectAll();
        }

        page.writeHeader();

        page.writeStart("div", "class", "widget ToolUserWorksInProgress");
        {
            page.writeStart("h1");
                page.writeHtml("Works In Progress");
            page.writeEnd();

            if (deleteAll) {
                page.writeStart("div", "class", "message message-info");
                page.writeHtml("All works in progress deleted!");
                page.writeEnd();

            } else if (ObjectUtils.isBlank(wips)) {
                page.writeStart("div", "class", "message message-info");
                page.writeHtml("No works in progress!");
                page.writeEnd();

            } else {
                page.writeStart("div", "class", "ToolUserWorksInProgress-body");
                page.writeStart("ul", "class", "links");
                {
                    for (WorkInProgress wip : wips) {
                        ObjectType contentType = wip.getContentType();
                        UUID contentId = wip.getContentId();

                        page.writeStart("li");
                        {
                            page.writeStart("a",
                                    "target", "_top",
                                    "href", page.cmsUrl("/content/edit.jsp",
                                            "typeId", contentType.getId(),
                                            "id", contentId));
                            {
                                page.writeObjectLabel(contentType);
                                page.writeHtml(": ");
                                page.writeHtml(wip.getContentLabel());
                            }
                            page.writeEnd();

                            page.writeStart("form",
                                    "method", "post",
                                    "action", page.url(""));
                            {
                                page.writeElement("input",
                                        "type", "hidden",
                                        "name", "wip",
                                        "value", wip.getId());

                                page.writeStart("button",
                                        "class", "link",
                                        "name", "action-delete",
                                        "value", "true");
                                page.writeHtml("Delete");
                                page.writeEnd();
                            }
                            page.writeEnd();
                        }
                        page.writeEnd();
                    }
                }
                page.writeEnd();

                page.writeStart("form",
                        "class", "ToolUserWorksInProgress-actions",
                        "method", "post",
                        "action", page.url(""));
                {

                    page.writeStart("button",
                            "class", "link icon icon-action-remove",
                            "name", "action-delete-all",
                            "data-confirm-message", "Are you sure you want to delete all your works in progress?",
                            "value", "true");
                    page.writeHtml("Delete All Works In Progress");
                    page.writeEnd();
                }
                page.writeEnd();
                page.writeEnd();
            }
        }
        page.writeEnd();

        page.writeFooter();
    }
}
