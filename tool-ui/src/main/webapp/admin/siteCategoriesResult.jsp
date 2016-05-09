<%@ page session="false" import="

com.psddev.cms.db.Site,
com.psddev.cms.db.SiteCategory,
com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.db.Query,
com.psddev.dari.util.ObjectUtils,

java.util.Iterator,
java.util.List
" %><%

// --- Logic ---

ToolPageContext wp = new ToolPageContext(pageContext);
if (wp.requirePermission("area/admin/adminSites")) {
    return;
}

Object selected = wp.findOrReserve(SiteCategory.class);

Query<SiteCategory> query = Query.from(SiteCategory.class).sortAscending("name");
String queryString = wp.param("query");
if (!ObjectUtils.isBlank(queryString)) {
    query.where("name ^=[c] ?", queryString);
}

List<SiteCategory> siteCategories = query.selectAll();
for (Iterator<SiteCategory> i = siteCategories.iterator(); i.hasNext(); ) {
    if (!wp.hasPermission(i.next().getPermissionId())) {
        i.remove();
    }
}

// --- Presentation ---

%><% wp.include("/WEB-INF/header.jsp"); %>

<% if (!siteCategories.isEmpty()) { %>
<ul class="links">
    <% for (SiteCategory siteCategory : siteCategories) { %>
    <li<%= siteCategory.equals(selected) ? " class=\"selected\"" : "" %>>
        <a href="<%= wp.objectUrl("/admin/sites.jsp", siteCategory) %>" target="_top"><%= wp.objectLabel(siteCategory) %>
        </a>
    </li>
    <% } %>
</ul>

<% } else { %>
<div class="message message-warning">
    <p>
        <%= wp.h(wp.localize("com.psddev.cms.tool.page.admin.SitesResult", "message.noMatches"))%>
    </p>
</div>
<% } %>

<% wp.include("/WEB-INF/footer.jsp"); %>
