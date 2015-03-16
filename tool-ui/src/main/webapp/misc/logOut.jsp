<%@ page session="false" import="

com.psddev.cms.tool.AuthenticationFilter,
com.psddev.cms.tool.ToolPageContext
" %><%

// --- logic ---
ToolPageContext wp = new ToolPageContext(pageContext);
AuthenticationFilter.Static.logOut(request, response);
wp.redirect("/");
%>
