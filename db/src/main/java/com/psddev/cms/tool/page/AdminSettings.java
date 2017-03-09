package com.psddev.cms.tool.page;

import com.psddev.cms.db.ReferentialTextMarker;
import com.psddev.cms.db.StandardImageSize;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Application;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;
import com.psddev.dari.db.WebResourceOverride;
import com.psddev.dari.util.RoutingFilter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;

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

    private void writeApplicationsListHtml(ToolPageContext page, Object selected) throws IOException{
        page.writeStart("h2");
            page.writeHtml(page.localize("com.psddev.cms.tool.page.admin.Settings", "subtitle.applications"));
        page.writeEnd();

        page.writeStart("ul", "class", "links");
            for (Object app : Query.from(Application.class).sortAscending("name").select()) {
                page.writeStart("li", "class", app.equals(selected) ? "selected" : "");
                    page.writeStart("a", "href", page.objectUrl(null, app));
                        page.writeObjectLabel(app);
                    page.writeEnd();
                page.writeEnd();
            }
        page.writeEnd();
    }

    private void writeImageSizesListHtml(ToolPageContext page, Object selected) throws IOException, ServletException {

        State selectedState = State.getInstance(selected);
        List<StandardImageSize> standardImageSizes = Query.from(StandardImageSize.class).sortAscending("displayName").select();

        page.writeStart("h2");
            page.writeHtml(page.localize("com.psddev.cms.tool.page.admin.Settings", "subtitle.imageSizes"));
        page.writeEnd();

        page.writeStart("ul", "class", "links");

            page.writeStart("li", "class", "new" + (selected instanceof StandardImageSize && selectedState.isNew() ? " selected" : ""));
                page.writeStart("a", "href", page.typeUrl(null, StandardImageSize.class));
                    page.writeHtml(page.localize(StandardImageSize.class, "action.newType"));
                page.writeEnd();
            page.writeEnd();

            for (StandardImageSize size : standardImageSizes) {
                page.writeStart("li", "class", size.equals(selected) ? "selected" : "");
                    page.writeStart("a", "href", page.objectUrl(null, size));
                        page.writeObjectLabel(size);
                    page.writeEnd();
                page.writeEnd();
            }
        page.writeEnd();
    }

    private void writeReferentialTextMarkersListHtml(ToolPageContext page, Object selected) throws IOException, ServletException {

        State selectedState = State.getInstance(selected);

        page.writeStart("h2");
            page.writeHtml(page.localize("com.psddev.cms.tool.page.admin.Settings", "subtitle.refTextMarkers"));
        page.writeEnd();

        page.writeStart("ul", "class", "links");

            page.writeStart("li", "class", "new" + (selected instanceof ReferentialTextMarker && selectedState.isNew() ? " selected" : ""));
                page.writeStart("a", "href", page.typeUrl(null, ReferentialTextMarker.class));
                    page.writeHtml(page.localize(ReferentialTextMarker.class, "action.newType"));
                page.writeEnd();
            page.writeEnd();

            for (ReferentialTextMarker marker : Query.from(ReferentialTextMarker.class).sortAscending("displayName").select()) {
                page.writeStart("li", "class", marker.equals(selected) ? "selected" : "");
                    page.writeStart("a", "href", page.objectUrl(null, marker));
                        page.writeObjectLabel(marker);
                    page.writeEnd();
                page.writeEnd();
            }
        page.writeEnd();
    }

    private void writeWebResourceOverridesListHtml(ToolPageContext page, Object selected) throws IOException, ServletException {

        State selectedState = State.getInstance(selected);

        page.writeStart("h2");
            page.writeHtml(page.localize("com.psddev.cms.tool.page.admin.Settings", "subtitle.webResourceOverrides"));
        page.writeEnd();

        page.writeStart("ul", "class", "links");

            page.writeStart("li", "class", "new" + (selected instanceof WebResourceOverride && selectedState.isNew() ? " selected" : ""));
                page.writeStart("a", "href", page.typeUrl(null, WebResourceOverride.class));
                    page.writeHtml(page.localize(WebResourceOverride.class, "action.newType"));
                page.writeEnd();
            page.writeEnd();

            for (WebResourceOverride override : Query.from(WebResourceOverride.class).sortAscending("path").selectAll()) {
                page.writeStart("li", "class", override.equals(selected) ? "selected" : "");
                    page.writeStart("a", "href", page.objectUrl(null, override));
                        page.writeObjectLabel(override);
                    page.writeEnd();
                page.writeEnd();
            }
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
}
