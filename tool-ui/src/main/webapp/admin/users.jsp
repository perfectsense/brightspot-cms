<%@ page import="

com.psddev.cms.db.ToolRole,
com.psddev.cms.db.ToolUser,
com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.db.Query,
com.psddev.dari.db.State,

java.util.UUID,
java.util.List
" %><%

// --- Logic ---

ToolPageContext wp = new ToolPageContext(pageContext);
if (wp.requirePermission("area/admin/adminUsers")) {
    return;
}

String queryString = wp.param("query");
Object selected = wp.findOrReserve(ToolUser.class, ToolRole.class);
State selectedState = State.getInstance(selected);

if(selected instanceof ToolUser && selectedState.isNew()) {
    ToolRole defaultRole = Query.from(ToolRole.class).where("defaultRole = ?", true).first();
    ((ToolUser)selected).setRole(defaultRole);
}

if (wp.include("/WEB-INF/updateObject.jsp", "object", selected)) {
    return;
}

// --- Presentation ---

%><% wp.include("/WEB-INF/header.jsp"); %>

<div class="withLeftNav">
    <div class="leftNav">

        <div class="widget">
            <h1>Users</h1>

            <ul class="links">
                <li class="new<%= selected.getClass() == ToolUser.class && selectedState.isNew() ? " selected" : "" %>">
                    <a href="<%= wp.typeUrl(null, ToolUser.class) %>">New User</a>
                </li>
            </ul>

            <form action="<%= wp.url("/admin/usersResult.jsp") %>" class="autoSubmit" method="get" target="usersResult">
                <input name="id" type="hidden" value="<%= selectedState.getId() %>">
                <input name="offset" type="hidden" value="<%= wp.longParam("offset", 0L) %>">
                <div class="searchInput">
                    <label for="<%= wp.createId() %>">Search</label>
                    <input id="<%= wp.getId() %>" class="autoFocus" name="query" type="text" value="<%= wp.h(queryString) %>">
                    <input type="submit" value="Go">
                </div>
            </form>

            <div class="frame" name="usersResult">
            </div>
        </div>

        <div class="widget">
            <h1>Roles</h1>
            <ul class="links">
                <li class="new<%= selected.getClass() == ToolRole.class && selectedState.isNew() ? " selected" : "" %>">
                    <a href="<%= wp.typeUrl(null, ToolRole.class) %>">New Role</a>
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
            <% wp.include("/admin/defaultRole.jsp"); %>
        </div>

    </div>
    <div class="main">

        <div class="widget">
            <% wp.include("/WEB-INF/editObject.jsp", "object", selected); %>
        </div>

    </div>
</div>

<% wp.include("/WEB-INF/footer.jsp"); %>
