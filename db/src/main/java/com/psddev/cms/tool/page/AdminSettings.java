package com.psddev.cms.tool.page;

import com.psddev.cms.db.ReferentialTextMarker;
import com.psddev.cms.db.StandardImageSize;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Application;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.db.State;
import com.psddev.dari.db.WebResourceOverride;
import com.psddev.dari.util.RoutingFilter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RoutingFilter.Path(application = "cms", value = "admin/settings.jsp")
public class AdminSettings extends PageServlet {

    @Override
    protected String getPermissionId() {
        return "area/admin/adminSettings";
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        // --- Logic ---

        if (page.requirePermission("area/admin/adminSettings")) {
            return;
        }

        Object selected = Query.findById(Object.class, page.uuidParam("id"));
        if (selected == null) {
            if (page.uuidParam("typeId") != null) {
                selected = page.findOrReserve(StandardImageSize.class);
            } else {
                selected = page.getCmsTool();
            }
        }

        if (selected != null && page.tryStandardUpdate(selected)) {
            return;
        }

        // --- Presentation ---

        page.writeHeader();

            page.writeStart("div", "class", "withLeftNav");
                writeLeftNavHtml(page, selected);
                writeMainHtml(page, selected);
            page.writeEnd();

        page.writeFooter();
    }

    private void writeLeftNavHtml(ToolPageContext page, Object selected) throws IOException, ServletException {
        page.writeStart("div", "class", "leftNav");
            page.writeStart("div", "class", "widget");

                page.writeStart("h1", "class", "icon icon-cogs");
                    page.writeHtml(page.localize("com.psddev.cms.tool.page.admin.Settings", "title"));
                page.writeEnd();

                writeApplicationsListHtml(page, selected);
                writeImageSizesListHtml(page, selected);
                writeReferentialTextMarkersListHtml(page, selected);
                writeWebResourceOverridesListHtml(page, selected);

            page.writeEnd();
        page.writeEnd();
    }

    private void writeApplicationsListHtml(ToolPageContext page, Object selected) throws IOException, ServletException {
        page.writeStart("h2");
            page.writeHtml(page.localize("com.psddev.cms.tool.page.admin.Settings", "subtitle.applications"));
        page.writeEnd();

        page.writeStart("ul", "class", "links");
            writeListItemsHtml(page,
                    Query.from(Application.class).where("name != missing").sortAscending("name").selectAll()
                            .stream()
                            .filter(app -> !app.getState().getType().as(ToolUi.class).isHidden())
                            .collect(Collectors.toList()),
                    selected);
        page.writeEnd();
    }

    private void writeImageSizesListHtml(ToolPageContext page, Object selected) throws IOException, ServletException {

        page.writeStart("h2");
            page.writeHtml(page.localize("com.psddev.cms.tool.page.admin.Settings", "subtitle.imageSizes"));
        page.writeEnd();

        writeListHtml(page,
                StandardImageSize.class,
                Query.from(StandardImageSize.class).sortAscending("displayName"),
                selected);
    }

    private void writeReferentialTextMarkersListHtml(ToolPageContext page, Object selected) throws IOException, ServletException {

        page.writeStart("h2");
            page.writeHtml(page.localize("com.psddev.cms.tool.page.admin.Settings", "subtitle.refTextMarkers"));
        page.writeEnd();

        writeListHtml(page,
                ReferentialTextMarker.class,
                Query.from(ReferentialTextMarker.class).sortAscending("displayName"),
                selected);
    }

    private void writeWebResourceOverridesListHtml(ToolPageContext page, Object selected) throws IOException, ServletException {
        page.writeStart("h2");
            page.writeHtml(page.localize("com.psddev.cms.tool.page.admin.Settings", "subtitle.webResourceOverrides"));
        page.writeEnd();

        page.writeStart("ul", "class", "links");
            writeListHtml(page,
                    WebResourceOverride.class,
                    Query.from(WebResourceOverride.class).sortAscending("path"),
                    selected);
        page.writeEnd();
    }

    private void writeMainHtml(ToolPageContext page, Object selected) throws IOException, ServletException {
        page.writeStart("div", "class", "main");
            page.writeStart("div", "class", "widget");
                if (selected != null) {
                    page.writeStandardForm(selected);
                }
            page.writeEnd();
        page.writeEnd();
    }

    private void writeListHtml(ToolPageContext page, Class<? extends Recordable> recordableClass, Query query, Object selected) throws IOException, ServletException {

        State selectedState = State.getInstance(selected);

        page.writeStart("ul", "class", "links");

            page.writeStart("li", "class", "new" + (recordableClass.isInstance(selected) && selectedState.isNew() ? " selected" : ""));
                page.writeStart("a", "href", page.typeUrl(null, recordableClass));
                    page.writeHtml(page.localize(recordableClass, "action.newType"));
                page.writeEnd();
            page.writeEnd();

            writeListItemsHtml(page, query.selectAll(), selected);
        page.writeEnd();
    }

    private void writeListItemsHtml(ToolPageContext page, List<?> items, Object selected) throws IOException, ServletException {
        for (Object object : items) {
            page.writeStart("li", "class", object.equals(selected) ? "selected" : "");
                page.writeStart("a", "href", page.objectUrl(null, object));
                    page.writeObjectLabel(object);
                page.writeEnd();
            page.writeEnd();
        }
    }
}
