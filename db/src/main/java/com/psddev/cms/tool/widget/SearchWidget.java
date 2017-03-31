package com.psddev.cms.tool.widget;

import com.psddev.cms.db.ToolUi;
import com.psddev.cms.tool.CmsTool;
import com.psddev.cms.tool.Dashboard;
import com.psddev.cms.tool.DashboardWidget;
import com.psddev.cms.tool.Search;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.util.JspUtils;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SearchWidget extends DashboardWidget {

    @ToolUi.Placeholder("Search")
    private String heading;

    @ToolUi.Note("Leave blank for all types.")
    private Set<ObjectType> types;

    public String getHeading() {
        return heading;
    }

    public Set<ObjectType> getTypes() {
        if (types == null) {
            types = new LinkedHashSet<>();
        }
        return types;

    }

    @Override
    public void writeHtml(ToolPageContext page, Dashboard dashboard) throws IOException, ServletException {
        page.writeStart("div", "class", "widget SearchWidget");
            page.writeStart("h1");
                page.writeHtml(ObjectUtils.firstNonBlank(getHeading(), page.localize(SearchWidget.class, "placeholder.search")));
            page.writeEnd();

            JspUtils.include(
                    page.getRequest(),
                    page.getResponse(),
                    page,
                    StringUtils.addQueryParameters(page.toolPath(CmsTool.class, "/WEB-INF/search.jsp"),
                            Search.TYPES_PARAMETER, getTypes().stream().map(ObjectType::getId).collect(Collectors.toList())),
                    "name", "fullScreen",
                    "newJsp", "/content/edit.jsp",
                    "newTarget", "_top",
                    "resultJsp", StringUtils.addQueryParameters("/misc/searchResult.jsp",
                            Search.IGNORE_SITE_PARAMETER, page.getRequest().getParameter(Search.IGNORE_SITE_PARAMETER)));
        page.writeEnd();
    }
}
