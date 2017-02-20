<%@ page session="false" import="

com.psddev.cms.db.ToolRole,
com.psddev.cms.db.ToolUser,
com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.db.Query,
com.psddev.dari.util.ObjectUtils,
com.psddev.dari.util.PaginatedResult,

java.util.stream.Collectors
" %><%

// --- Logic ---

ToolPageContext wp = new ToolPageContext(pageContext);
if (wp.requirePermission("area/admin/adminUsers")) {
    return;
}

ToolUser selectedUser = null;
Object selected = wp.findOrReserve(ToolUser.class, ToolRole.class);
if (selected instanceof ToolUser) {
    selectedUser = (ToolUser) selected;
}

Query<ToolUser> query = Query.from(ToolUser.class).where("name isnt missing").and("cms.content.trashed = true || cms.content.trashed is missing").sortAscending("cms.content.trashed").sortAscending("name");
String queryString = wp.param("query");
if (!ObjectUtils.isBlank(queryString)) {
    query.where("name contains ? or email contains ? or username contains ?", queryString, queryString, queryString);
}

long offset = wp.longParam("offset", 0L);
PaginatedResult<ToolUser> users = query.select(offset, wp.intParam("limit", 10));

// --- Presentation ---

%><% wp.include("/WEB-INF/header.jsp"); %>

<% if (users.getOffset() > 0 || !users.getItems().isEmpty()) { %>
    <ul class="pagination">
        <% if (users.hasPrevious()) { %>
            <li class="previous"><a href="<%= wp.h(wp.url("", "offset", users.getPreviousOffset())) %>"><%= users.getLimit() %></a></li>
        <% } %>
        <li class="label">
            <strong><%= wp.h(users.getFirstItemIndex()) %></strong>
            to <strong><%= wp.h(users.getLastItemIndex()) %></strong>
        </li>
        <% if (users.hasNext()) { %>
            <li class="next"><a href="<%= wp.h(wp.url("", "offset", users.getNextOffset())) %>"><%= users.getLimit() %></a></li>
        <% } %>
    </ul>

    <ul class="links">
        <% for (ToolUser user : users.getItems().stream().distinct().collect(Collectors.toList())) { %>
            <li<%= user.equals(selectedUser) ? " class=\"selected\"" : "" %>>
                <a href="<%= wp.objectUrl("/admin/users.jsp", user,
                        "query", queryString,
                        "offset", offset) %>" target="_top"><%= wp.createObjectLabelHtml(user) %></a>
            </li>
        <% } %>
    </ul>

<% } else { %>
    <div class="message message-warning">
        <%= wp.h(wp.localize("com.psddev.cms.tool.page.admin.UsersResult", "message.noMatches"))%>
    </div>
<% } %>

<% wp.include("/WEB-INF/footer.jsp"); %>
