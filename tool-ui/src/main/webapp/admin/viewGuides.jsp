<%@ page session="false"
    import="
    com.psddev.dari.util.StorageItem,
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

    "%>
<%@ taglib prefix="cms" uri="http://psddev.com/cms"%>
<%

// --- Logic ---

ToolPageContext wp = new ToolPageContext(pageContext);
if (wp.requirePermission("area/admin/viewGuides")) {
    return;
}

String queryString = wp.param("query");
Object selected = wp.findOrReserve(Guide.class);
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
String incompleteIndicator = "*";

// --- Presentation ---
wp.include("/WEB-INF/header.jsp"); %>

<div class="withLeftNav">
    <div class="leftNav">
        <div class="widget">

            <h1 class="icon icon-object-guide">Production Guides</h1>
            <h2>Overall Guides</h2>

            <ul class="links">

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

        </div>
    </div>
    <div class="main">

        <div class="widget">
            <h1 class="icon icon-object-guide">View Guides</h1>
            <% 
                if (wp.uuidParam("id") == null){
            %>

            <h2 class="card-title">Production Guides</h2>
            <div class="card-wrapper">
                <% for (Guide guide : guides) {
                     String label = wp.objectLabel(guide);
                     request.setAttribute("productionGuide", guide);
                     String descript = guide.getDescription();
                %>
                <div class="card">
                    <div class="card-img">
                        <%
                        if (guide.getIcon() == null){
                            wp.writeElement("img", "src", wp.cmsUrl("/style/brightspot-icon.png"), "class", "card-icon");
                        }
                        %>
                        <cms:img class="card-icon" src="${productionGuide.icon}" />
                    </div>
                    <div class="card-content">
                        <a href="<%= wp.objectUrl(null, guide) %>"><h3 class="card-content-title"><%=label%></h3></a>
                        <% if (descript != null){
                                wp.write(descript);
                            }
                        %>
                    </div>
                </div>
                <% } %>
            </div>
            <%
                } else {
                    String label = wp.objectLabel(selected);
                    Guide selectedGuide = (Guide) selected;
                    request.setAttribute("productionGuide", selectedGuide);
            %>
            <a class="link-one" href="<%= wp.url("/admin/viewGuides.jsp") %>"><button class="link imageEditor-rotate-left">Back to View</button></a>
            <a href="<%= wp.objectUrl("/admin/guides.jsp", selected) %>"><button class="link icon icon-object-draft">Edit</button></a>
            <h2 class="card-title"><%=label%></h2>
            <div class="guideOverview">
                <cms:render value="${productionGuide.overview}" />
            </div>

            <%
                }
            %>
        </div>

    </div>
</div>

<% wp.include("/WEB-INF/footer.jsp"); %>
