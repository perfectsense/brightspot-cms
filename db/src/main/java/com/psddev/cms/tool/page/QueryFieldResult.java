package com.psddev.cms.tool.page;

import java.io.IOException;

import javax.servlet.ServletException;

import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.Search;
import com.psddev.cms.tool.SearchResultRenderer;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;

@RoutingFilter.Path(application = "cms", value = "queryFieldResult")
public class QueryFieldResult extends PageServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        Search search = new Search(page);
        Renderer renderer = new Renderer(page, search);

        renderer.render();

        String pageId = page.createId();
        Query<?> query;

        try {
            query = search.toQuery(page.getSite());

        } catch (IllegalArgumentException | Query.NoFieldException error) {
            return;
        }

        State queryState = query.getState();

        queryState.put("cms.ui.search", search.getState().getSimpleValues());

        page.writeStart("div", "id", pageId);
        page.writeEnd();

        page.writeStart("script", "type", "text/javascript");
            page.writeRaw("var $page = $('#").writeRaw(pageId).writeRaw("'),");
                    page.writeRaw("$frame = $page.closest('.queryField_frames > .frame'),");
                    page.writeRaw("$field = $.data($frame[0], 'query-$field'),");
                    page.writeRaw("$input = $field.find('input');");

            page.writeRaw("$input.val('");
            page.writeRaw(page.js(ObjectUtils.toJson(queryState.getSimpleValues())));
            page.writeRaw("');");

            page.writeRaw("$input.change();");
        page.writeEnd();
    }

    private static class Renderer extends SearchResultRenderer {

        public Renderer(ToolPageContext page, Search search) throws IOException {
            super(page, search);
        }

        @Override
        public void renderBeforeItem(Object item) throws IOException {
        }

        @Override
        public void renderAfterItem(Object item) throws IOException {
        }
    }
}
