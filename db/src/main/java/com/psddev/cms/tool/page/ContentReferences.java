package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.UUID;

import javax.servlet.ServletException;

import com.psddev.cms.db.Content;
import com.psddev.cms.db.Directory;
import com.psddev.cms.db.Draft;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.Search;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.PaginatedResult;
import com.psddev.dari.util.RoutingFilter;

@RoutingFilter.Path(application = "cms", value = "/content/references")
@SuppressWarnings("serial")
public class ContentReferences extends PageServlet {

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(final ToolPageContext page) throws IOException, ServletException {
        UUID id = page.param(UUID.class, "id");
        Query<Object> query = Query
                .fromGroup(Content.SEARCHABLE_GROUP)
                .and("* matches ?", id)
                .and("_type != ?", Draft.class)
                .and("_id != ?", id)
                .sortDescending("cms.content.updateDate");
        PaginatedResult<Object> result = query.select(0L, 10);

        if (result.getItems().isEmpty()) {
            return;
        }

        page.writeStart("div", "class", "widget");
            page.writeStart("h1", "class", "icon icon-book");
                page.writeHtml("References");
            page.writeEnd();

            if (result.hasNext()) {
                page.writeStart("p");
                    page.writeStart("a",
                            "class", "icon icon-action-search",
                            "target", "_top",
                            "href", page.cmsUrl("/searchAdvancedFull",
                                    Search.ADVANCED_QUERY_PARAMETER, query.getPredicate().toString()));
                        page.writeHtml("View All ");
                        page.writeHtml(result.getCount());
                        page.writeHtml(" References");
                    page.writeEnd();
                page.writeEnd();

                page.writeStart("h2");
                    page.writeHtml("10 Most Recent");
                page.writeEnd();
            }

            page.writeStart("ul", "class", "links pageThumbnails");
                for (Object item : result.getItems()) {
                    page.writeStart("li",
                            "data-preview-url", State.getInstance(item).as(Directory.ObjectModification.class).getPermalink());
                        page.writeStart("a",
                                "href", page.objectUrl("/content/edit.jsp", item),
                                "target", "_top");
                            page.writeTypeObjectLabel(item);
                        page.writeEnd();
                    page.writeEnd();
                }
            page.writeEnd();
        page.writeEnd();
    }
}
