<%@ page session="false" import="

java.util.List,
com.psddev.cms.db.ReferentialTextMarker,
com.psddev.cms.db.Site,
com.psddev.cms.db.StandardImageSize,
com.psddev.cms.tool.ToolPageContext,
com.psddev.dari.db.Application,

com.psddev.dari.db.Query,
com.psddev.dari.db.State,
com.psddev.dari.db.WebResourceOverride" %><%

// --- Logic ---

ToolPageContext wp = new ToolPageContext(pageContext);
if (wp.requirePermission("area/admin/adminSettings")) {
    return;
}

Object selected = Query.findById(Object.class, wp.uuidParam("id"));
if (selected == null) {
    if (wp.uuidParam("typeId") != null) {
        selected = wp.findOrReserve(StandardImageSize.class);
    } else {
        selected = wp.getCmsTool();
    }
}

if (selected != null && wp.tryStandardUpdate(selected)) {
    return;
}

State selectedState = State.getInstance(selected);
List<StandardImageSize> standardImageSizes = Query.from(StandardImageSize.class).where(Site.CONSUMERS_FIELD + " is missing || " + Site.IS_GLOBAL_FIELD + " = true").sortAscending("displayName").select();

// --- Presentation ---

%><% wp.include("/WEB-INF/header.jsp"); %>

<div class="withLeftNav">
    <div class="leftNav">

        <div class="widget">
            <h1 class="icon icon-cogs">
                <%= wp.h(wp.localize("com.psddev.cms.tool.page.admin.Settings", "title"))%>
            </h1>

            <h2>
                <%= wp.h(wp.localize("com.psddev.cms.tool.page.admin.Settings", "subtitle.applications"))%>
            </h2>
            <ul class="links">
                <% for (Object app : Query
                        .from(Application.class)
                        .sortAscending("name")
                        .select()) { %>
                    <li<%= app.equals(selected) ? " class=\"selected\"" : "" %>>
                        <a href="<%= wp.objectUrl(null, app) %>"><%= wp.objectLabel(app) %></a>
                    </li>
                <% } %>
            </ul>

            <h2>
                <%= wp.h(wp.localize("com.psddev.cms.tool.page.admin.Settings", "subtitle.imageSizes"))%>
            </h2>
            <ul class="links">
                <li class="new<%= selected instanceof StandardImageSize && selectedState.isNew() ? " selected" : "" %>">
                    <a href="<%= wp.typeUrl(null, StandardImageSize.class) %>">
                        <%= wp.h(wp.localize(StandardImageSize.class, "action.newType"))%>
                    </a>
                </li>
                <% for (StandardImageSize size : standardImageSizes) { %>
                    <li<%= size.equals(selected) ? " class=\"selected\"" : "" %>>
                        <a href="<%= wp.objectUrl(null, size) %>"><%= wp.objectLabel(size) %></a>
                    </li>
                <% } %>
            </ul>

            <h2>
                <%= wp.h(wp.localize("com.psddev.cms.tool.page.admin.Settings", "subtitle.refTextMarkers"))%>
            </h2>
            <ul class="links">
                <li class="new<%= selected instanceof ReferentialTextMarker && selectedState.isNew() ? " selected" : "" %>">
                    <a href="<%= wp.typeUrl(null, ReferentialTextMarker.class) %>">
                        <%= wp.h(wp.localize(ReferentialTextMarker.class, "action.newType"))%>
                    </a>
                </li>
                <% for (ReferentialTextMarker marker : Query.
                        from(ReferentialTextMarker.class).
                        sortAscending("displayName").
                        select()) { %>
                    <li<%= marker.equals(selected) ? " class=\"selected\"" : "" %>>
                        <a href="<%= wp.objectUrl(null, marker) %>"><%= wp.objectLabel(marker) %></a>
                    </li>
                <% } %>
            </ul>

            <h2>
                <%= wp.h(wp.localize("com.psddev.cms.tool.page.admin.Settings", "subtitle.webResourceOverrides"))%>
            </h2>
            <ul class="links">
                <li class="new<%= selected instanceof WebResourceOverride && selectedState.isNew() ? " selected" : "" %>">
                    <a href="<%= wp.typeUrl(null, WebResourceOverride.class) %>">
                        <%= wp.h(wp.localize(WebResourceOverride.class, "action.newType"))%>
                    </a>
                </li>
                <% for (WebResourceOverride override : Query.
                        from(WebResourceOverride.class).
                        sortAscending("path").
                        selectAll()) { %>
                    <li<%= override.equals(selected) ? " class=\"selected\"" : "" %>>
                        <a href="<%= wp.objectUrl(null, override) %>"><%= wp.objectLabel(override) %></a>
                    </li>
                <% } %>
            </ul>
        </div>

    </div>
    <div class="main">

        <% if (selected != null) { %>
            <div class="widget">
                <% wp.writeStandardForm(selected); %>
            </div>
        <% } %>

    </div>
</div>

<% wp.include("/WEB-INF/footer.jsp"); %>
