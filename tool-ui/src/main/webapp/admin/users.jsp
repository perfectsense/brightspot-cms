<%@ page session="false" import="

com.psddev.cms.db.ToolRole,
com.psddev.cms.db.ToolUser,
com.psddev.cms.tool.Search,
com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.db.ObjectType,
com.psddev.dari.db.Query,
com.psddev.dari.db.State,

java.util.UUID
" %><%

// --- Logic ---

ToolPageContext wp = new ToolPageContext(pageContext);
if (wp.requirePermission("area/admin/adminUsers")) {
    return;
}

String queryString = wp.param("query");
Object selected = wp.findOrReserve(ToolUser.class, ToolRole.class);
State selectedState = State.getInstance(selected);

if (selected instanceof ToolUser && selectedState.isNew()) {
    ((ToolUser) selected).setRole(wp.getCmsTool().getDefaultRole());
}

if (wp.tryStandardUpdate(selected)) {
    return;
}

// --- Presentation ---

%><% wp.include("/WEB-INF/header.jsp"); %>

<div class="withLeftNav">
    <div class="leftNav">

        <div class="widget">
            <h1 class="icon icon-object-toolUser">
                <%= wp.h(wp.localize("com.psddev.cms.tool.page.admin.Users", "title.users"))%>
            </h1>

            <div class="widget-controls">
                <ul class="piped">
                    <li><a class="icon icon-action-search icon-only" href="<%= wp.cmsUrl(
                            "/searchAdvancedFull",
                            Search.TYPES_PARAMETER, ObjectType.getInstance(ToolUser.class).getId(),
                            Search.LIMIT_PARAMETER, 50)
                            %>">Search</a></li>
                </ul>
            </div>

            <ul class="links">
                <li class="new<%= selected.getClass() == ToolUser.class && selectedState.isNew() ? " selected" : "" %>">
                    <a href="<%= wp.typeUrl(null, ToolUser.class) %>">
                        <%= wp.h(wp.localize(ToolUser.class, "action.newType"))%>
                    </a>
                </li>
            </ul>

            <form action="<%= wp.url("/admin/usersResult.jsp") %>" data-bsp-autosubmit="" method="get" target="usersResult">
                <input name="id" type="hidden" value="<%= selectedState.getId() %>">
                <input name="offset" type="hidden" value="<%= wp.longParam("offset", 0L) %>">
                <div class="searchInput">
                    <label for="<%= wp.createId() %>">
                        <%= wp.h(wp.localize("com.psddev.cms.tool.page.admin.Users", "label.search"))%>
                    </label>
                    <input id="<%= wp.getId() %>" class="autoFocus" name="query" type="text" value="<%= wp.h(queryString) %>">
                    <input type="submit" value="Go">
                </div>
            </form>

            <div class="frame" name="usersResult">
            </div>
        </div>

        <div class="widget">
            <h1 class="icon icon-object-toolRole">
                <%= wp.h(wp.localize("com.psddev.cms.tool.page.admin.Users", "title.roles"))%>
            </h1>
            <ul class="links">
                <li class="new<%= selected.getClass() == ToolRole.class && selectedState.isNew() ? " selected" : "" %>">
                    <a href="<%= wp.typeUrl(null, ToolRole.class) %>">
                        <%= wp.h(wp.localize(ToolRole.class, "action.newType"))%>
                    </a>
                </li>
                <% for (ToolRole role : Query
                        .from(ToolRole.class)
                        .sortAscending("name")
                        .select()) { %>
                    <li<%= role.equals(selected) ? " class=\"selected\"" : "" %>>
                        <a href="<%= wp.objectUrl(null, role) %>"><%= wp.objectLabel(role) %></a>
                    </li>
                <% } %>
            </ul>
        </div>

    </div>
    <div class="main">

        <div class="widget">
            <% wp.writeStandardForm(selected, false); %>
        </div>

    </div>
</div>

<% wp.include("/WEB-INF/footer.jsp"); %>
