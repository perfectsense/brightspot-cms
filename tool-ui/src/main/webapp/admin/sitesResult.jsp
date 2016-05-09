<%@ page session="false" import="

com.psddev.cms.db.Site,
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

Object selected = wp.findOrReserve(Site.class);

Query<Site> query = Query.from(Site.class).sortAscending("name");
String queryString = wp.param("query");
if (!ObjectUtils.isBlank(queryString)) {
    query.where("name ^=[c] ?", queryString);
}

List<Site> sites = query.selectAll();
for (Iterator<Site> i = sites.iterator(); i.hasNext(); ) {
    if (!wp.hasPermission(i.next().getPermissionId())) {
        i.remove();
    }
}

// --- Presentation ---

%><% wp.include("/WEB-INF/header.jsp"); %>

<% if (!sites.isEmpty()) { %>
    <ul class="links">
        <% for (Site site : sites) { %>
            <li<%= site.equals(selected) ? " class=\"selected\"" : "" %>>
                <a href="<%= wp.objectUrl("/admin/sites.jsp", site) %>" target="_top"><%= wp.objectLabel(site) %></a>
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
