<%@ page import="
         com.psddev.cms.tool.ToolPageContext,
         com.psddev.cms.db.ToolRole,
         com.psddev.dari.db.Query,
         java.util.List
         " %>
<% 
ToolPageContext wp = new ToolPageContext(pageContext);
List<ToolRole> roles = Query
                        .from(ToolRole.class)
                        .sortAscending("name")
                        .select();
%>
<% if (request.getMethod().equalsIgnoreCase("POST")) {
    List<ToolRole> defaultRoles = Query.from(ToolRole.class).where("defaultRole = true").select();
    if(defaultRoles != null && !defaultRoles.isEmpty()) {
        for(ToolRole r : defaultRoles) {
            r.setDefaultRole(false);
            r.save();
        }
    }
    ToolRole role = Query.from(ToolRole.class).where("id = ?", request.getParameter("role")).first();
    if(role != null) {
        role.setDefaultRole(true);
        role.save();
    }
%>
saved!
<% } else { %>
<div name="defaultRole">    
    Default Role For New Users:                            
    <form id="defaultRole" action="<%= wp.url("/admin/defaultRole.jsp")%>" method="post" style="padding: 5px;">
        <select name="role" style="width: 120px;margin-right: 5px;">
            <% for(ToolRole role : roles) { %>
            <option value="<%= role.getId() %>" <%= role.getDefaultRole() ? "selected" : "" %>><%= role.getName() %></option>
            <% } %>
        </select>
        <span id="defaultRoleChanged" class="success message" style="display: none;"></span>
    </form>    
    <script type="text/javascript">       
        $('form#defaultRole').change(function () {
            if(confirm("Are you sure you want to change the default role for new user accounts?")) {
            $.post(
            $('form#defaultRole').attr('action'),
                $('form#defaultRole').serialize(),
                function(data) {
                    $('#defaultRoleChanged').hide();
                    $('#defaultRoleChanged').html(data);
                    $('#defaultRoleChanged').fadeIn(1000).delay(1000).fadeOut();
                }
            );
            }
        });              
    </script>
</div>
<% } %>