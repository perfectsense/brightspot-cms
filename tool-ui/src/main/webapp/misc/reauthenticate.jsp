<%@ page session="false" import="

com.psddev.cms.tool.AuthenticationFilter,
com.psddev.cms.tool.ToolPageContext
" %><%

// --- logic ---
ToolPageContext wp = new ToolPageContext(pageContext);
AuthenticationFilter.Static.reauthenticate(request, response);
wp.redirect("/");
%>