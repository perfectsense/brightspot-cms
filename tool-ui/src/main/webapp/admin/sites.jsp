<%@ page session="false" import="

com.psddev.cms.db.Site,
com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.db.Query,
com.psddev.dari.db.State,

java.util.UUID
" %><%

// --- Logic ---

ToolPageContext wp = new ToolPageContext(pageContext);
if (wp.requirePermission("area/admin/adminSites")) {
    return;
}

String queryString = wp.param("query");
Object selected = wp.findOrReserve(Site.class);
State selectedState = State.getInstance(selected);
if (wp.tryStandardUpdate(selected)) {
    return;
}

// --- Presentation ---

%><% wp.include("/WEB-INF/header.jsp"); %>

<div class="withLeftNav">
    <div class="leftNav">
        <div class="widget">

            <h1 class="icon icon-object-site">Sites</h1>
            <ul class="links">
                <li class="new<%= selectedState.isNew() ? " selected" : "" %>">
                    <a href="<%= wp.url(null) %>"><%= wp.h(wp.localize(Site.class, "action.new")) %></a>
                </li>
            </ul>

            <form action="<%= wp.url("/admin/sitesResult.jsp") %>" data-bsp-autosubmit="" method="get" target="sitesResult">
                <input name="id" type="hidden" value="<%= selectedState.getId() %>">
                <div class="searchInput">
                    <label for="<%= wp.createId() %>">Search</label>
                    <input id="<%= wp.getId() %>" class="autoFocus" name="query" type="text" value="<%= wp.h(queryString) %>">
                    <input type="submit" value="Go">
                </div>
            </form>

            <div class="frame" name="sitesResult">
            </div>

        </div>
    </div>
    <div class="main">
        <div class="widget">

            <% wp.writeStandardForm(selected); %>

        </div>
    </div>
</div>

<% wp.include("/WEB-INF/footer.jsp"); %>
