<%@ page session="false" import="

com.psddev.cms.db.Site,
com.psddev.cms.db.ToolUser,
com.psddev.cms.tool.JspWidget,
com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.db.Database,
com.psddev.dari.db.Query,
com.psddev.dari.db.State,

java.util.Arrays,
java.util.List,
java.util.Set,
java.util.UUID
" %><%

// --- Logic ---

ToolPageContext wp = new ToolPageContext(pageContext);
List<Site> allSites = Site.findAll();
if (allSites.isEmpty()) {
    return;
}

Object object = JspWidget.getOriginal(wp);
State state = State.getInstance(object);
Site.ObjectModification siteData = state.as(Site.ObjectModification.class);

String namePrefix = state.getId() + "/sites/";
String ownerName = namePrefix + "owner";
String accessName = namePrefix + "access";
String consumerIdName = namePrefix + "consumerId";

Site owner = siteData.getOwner();
Set<Site> consumers = siteData.getConsumers();

if (JspWidget.isUpdating(wp)) {

    if (owner == null || wp.hasPermission(owner.getPermissionId())) {
        owner = Database.Static.findById(wp.getDatabase(), Site.class, wp.uuidParam(ownerName));
        siteData.setOwner(owner);
    }

    String access = wp.param(accessName);
    consumers.clear();

    if ("no".equals(access)) {
        siteData.setGlobal(false);
        siteData.setBlacklist(null);
        siteData.setConsumers(null);

    } else if ("all".equals(access)) {
        siteData.setGlobal(true);
        siteData.setBlacklist(null);
        siteData.setConsumers(null);

    } else if ("some".equals(access)) {
        siteData.setGlobal(false);
        List<UUID> consumerIds = Arrays.asList(wp.uuidParams(consumerIdName));
        for (Site site : allSites) {
            if (consumerIds.contains(site.getId())) {
                consumers.add(site);
            }
        }
    }

    return;
}


// --- Presentation ---

String sitesContainerId = wp.createId();
String access = siteData.isGlobal() ? "all" :
        consumers.isEmpty() ? "no" :
        "some";

%>
<label for="<%= wp.createId() %>">
    <% wp.writeHtml(wp.localize("com.psddev.cms.tool.widget.Sites", "label.owner")); %>
</label><br>

<% if (owner != null && !wp.hasPermission(owner.getPermissionId())) {%>
    <div><%= wp.getObjectLabel(owner) %></div>

<% } else {%>
<select class="toggleable" data-root=".widget" name="<%= ownerName %>" style="width: 100%;">
    <option<%= owner == null ? " selected" : "" %> value="" data-show=".siteItem">None</option>
    <% for (Site site : allSites) { %>
        <% if (wp.hasPermission(site.getPermissionId())) { %>
            <option<%= site.equals(owner) ? " selected" : "" %> value="<%= site.getId() %>" data-show=".siteItem:not(.siteItem-<%= site.getId() %>)" data-hide=".siteItem-<%= site.getId() %>"><%= wp.objectLabel(site) %></option>
        <% } %>
    <% } %>
</select>
<% } %>

<label for="<%= wp.createId() %>">
    <% wp.writeHtml(wp.localize("com.psddev.cms.tool.widget.Sites", "label.access")); %>
</label><br>
<select class="toggleable" id="<%= wp.getId() %>" name="<%= accessName %>" style="width: 100%;">
    <option<%= "no".equals(access) ? " selected" : "" %> data-hide="#<%= sitesContainerId %>" value="no">
        <% wp.writeHtml(wp.localize("com.psddev.cms.tool.widget.Sites", "option.none")); %>
    </option>
    <option<%= "all".equals(access) ? " selected" : "" %> data-hide="#<%= sitesContainerId %>" value="all">
        <% wp.writeHtml(wp.localize("com.psddev.cms.tool.widget.Sites", "option.all")); %>
    </option>
    <option<%= "some".equals(access) ? " selected" : "" %> data-show="#<%= sitesContainerId %>" value="some">
        <% wp.writeHtml(wp.localize("com.psddev.cms.tool.widget.Sites", "option.some")); %>
    </option>
</select>
<ul id="<%= sitesContainerId %>">
    <% for (Site site : allSites) { %>
        <% if (wp.hasPermission(site.getPermissionId())) { %>
            <li class="siteItem siteItem-<%= site.getId() %>">
                <input<%= consumers.contains(site) ? " checked" : "" %> id="<%= wp.createId() %>" name="<%= consumerIdName %>" type="checkbox" value="<%= site.getId() %>">
                <label for="<%= wp.getId() %>"><%= wp.objectLabel(site) %></label>
            </li>

        <% } else if (consumers.contains(site)) { %>
            <input name="<%= consumerIdName %>" type="hidden" value="<%= site.getId() %>">
        <% } %>
    <% } %>
</ul>
