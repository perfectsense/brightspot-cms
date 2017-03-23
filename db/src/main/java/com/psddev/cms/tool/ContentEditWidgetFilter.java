package com.psddev.cms.tool;

import com.psddev.cms.db.PageFilter;
import com.psddev.cms.tool.page.content.Edit;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.AbstractFilter;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.TypeDefinition;
import com.psddev.dari.util.UrlBuilder;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class ContentEditWidgetFilter extends AbstractFilter implements AbstractFilter.Auto {

    private static final String PATH = "/_content-edit-widget";
    private static final String WIDGET_PARAMETER = "widget";
    private static final String CONTENT_PARAMETER = "content";
    private static final String SECTION_PARAMETER = "section";

    public static void writeFrame(ToolPageContext page, Object content, ContentEditSection section, ContentEditWidget widget) throws IOException {
        page.writeStart("div", "class", "frame");
        page.writeStart("a", "href", new UrlBuilder(page.getRequest())
                .absolutePath(PATH)
                .parameter(WIDGET_PARAMETER, widget.getClass().getName())
                .parameter(CONTENT_PARAMETER, State.getInstance(content).getId())
                .parameter(SECTION_PARAMETER, section.name())
                .toString());
        page.writeEnd();
        page.writeEnd();
    }

    @Override
    public void updateDependencies(Class<? extends AbstractFilter> filterClass, List<Class<? extends Filter>> dependencies) {
        if (PageFilter.class.isAssignableFrom(filterClass)) {
            dependencies.add(getClass());
        }
    }

    @Override
    protected void doRequest(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws Exception {
        if (!request.getServletPath().equals(PATH)) {
            chain.doFilter(request, response);
            return;
        }

        ToolPageContext page = new ToolPageContext(getServletContext(), request, response);
        Class<?> widgetClass = ObjectUtils.getClassByName(page.param(String.class, WIDGET_PARAMETER));

        Edit.writeWidgetOrError(
                page,
                Query.fromAll().where("_id = ?", page.param(UUID.class, CONTENT_PARAMETER)).first(),
                ContentEditSection.valueOf(page.param(String.class, SECTION_PARAMETER)),
                (ContentEditWidget) TypeDefinition.getInstance(widgetClass).newInstance());
    }
}
