<%@ page session="false"
    import="
    com.psddev.cms.db.Guide,
    com.psddev.cms.db.Guide.GuideSettings,
    com.psddev.cms.db.GuidePage,
    com.psddev.cms.db.GuidePage.Static,
    com.psddev.cms.db.GuideType,
    com.psddev.cms.db.Page,
    com.psddev.cms.db.Section,
    com.psddev.cms.db.Template,
    com.psddev.cms.tool.ToolPageContext,
    com.psddev.dari.db.Query,
    com.psddev.dari.db.State,
    java.util.ArrayList,
    java.util.List
    , com.google.common.collect.ImmutableMap"%>
<%

// --- Logic ---

ToolPageContext wp = new ToolPageContext(pageContext);
if (wp.requirePermission("area/admin/adminGuides")) {
    return;
}

Object selected = wp.findOrReserve(Guide.class, Page.class, Template.class, GuideType.class, GuidePage.class);
Class<?> selectedClass = selected.getClass();
State selectedState = State.getInstance(selected);

if (wp.tryStandardUpdate(selected)) {
    return;
}

// Ensure that there is a GuidePage object created for any templates
GuideSettings settings = (wp.getCmsTool()).as(GuideSettings.class);

// Ensure that there is a GuideType object created for any objects referenced by templates.
if (settings.isAutoGenerateContentTypeGuides() == true) {
    GuideType.Static.createDefaultTypeGuides();
}

List<Guide> guides = Query.from(Guide.class).sortAscending("title").select();
List<GuideType> typeGuides = Query.from(GuideType.class).sortAscending("documentedType/name").select();
List<GuidePage> pageGuides = Query.from(GuidePage.class).sortAscending("name").select();
List<Page> templates = Query.from(Page.class).sortAscending("name").select();
String incompleteIndicator = "*";

// --- Presentation ---
wp.include("/WEB-INF/header.jsp"); %>

<div class="withLeftNav">
    <div class="leftNav">
        <div class="widget">

            <h1 class="icon icon-object-guide">
                <%= wp.h(wp.localize("com.psddev.cms.tool.page.admin.Guides", "title"))%>
            </h1>

            <h2>
                <%= wp.h(wp.localize("com.psddev.cms.tool.page.admin.Guides", "subtitle.overall"))%>
            </h2>
            <ul class="links">
                <li
                    class="new<%= selectedClass == Guide.class && selectedState.isNew() ? " selected" : "" %>">
                    <a href="<%= wp.typeUrl(null, Guide.class) %>">
                        <%= wp.h(wp.localize(Guide.class, "action.newType"))%>
                    </a>
                </li>
                <% for (Guide guide : guides) {
                     String label = wp.objectLabel(guide);
                     if (guide.isIncomplete()) {
                         label += incompleteIndicator;
                     }
                %>
                <li <%= guide.equals(selected) ? " class=\"selected\"" : "" %>>
                    <a href="<%= wp.objectUrl(null, guide) %>"><%=label%></a>
                </li>
                <% } %>
            </ul>

            <h2>
                <%= wp.h(wp.localize("com.psddev.cms.tool.page.admin.Guides", "subtitle.templateAndPage"))%>
            </h2>
            <ul class="links">
                <li
                    class="new<%= selectedClass == GuidePage.class && selectedState.isNew() ? " selected" : "" %>">
                    <a href="<%= wp.typeUrl(null, GuidePage.class) %>">
                        <%= wp.h(wp.localize(Guide.class, "action.newType"))%>
                    </a>
                </li>
                <% for (GuidePage guide : pageGuides) {
                    String templateLabel = wp.objectLabel(guide);
                    if (guide.isIncomplete()) {
                        templateLabel += incompleteIndicator;
                    }
                %>
                <li <%= guide.equals(selected) ? " class=\"selected\"" : "" %>>
                    <a href="<%= wp.objectUrl(null, guide) %>"><%= templateLabel %></a>
                </li>
                <% } %>
            </ul>

            <h2>
                <%= wp.h(wp.localize("com.psddev.cms.tool.page.admin.Guides", "subtitle.contentType"))%>
            </h2>
            <ul class="links">
                <li
                    class="new<%= selectedClass == GuideType.class && selectedState.isNew() ? " selected" : "" %>">
                    <a href="<%= wp.typeUrl(null, GuideType.class) %>">
                    <%= wp.h(wp.localize(Guide.class, "action.newType"))%>
                    </a>
                </li>
                <% for (GuideType guide : typeGuides) { %>
                <li <%= guide.equals(selected) ? " class=\"selected\"" : "" %>>
                    <a href="<%= wp.objectUrl(null, guide) %>"><%= wp.objectLabel(guide) %></a>
                </li>
                <% } %>
            </ul>

            <div class="guideFootnote">
                <%= wp.h(wp.localize(
                        "com.psddev.cms.tool.page.admin.Guides",
                        ImmutableMap.of("indicator", (Object) incompleteIndicator),
                        "message.footnote"))%>
            </div>

        </div>
    </div>
    <div class="main">

        <div class="widget">
            <% if (selected instanceof Guide && guides.contains(selected)){ %>
                    <a class="view-guide-link" href="<%= wp.objectUrl("/admin/viewGuides.jsp", selected) %>">View Guide</a>
            <%    }
                wp.writeStandardForm(selected); %>
        </div>

    </div>
</div>

<% wp.include("/WEB-INF/footer.jsp"); %>
