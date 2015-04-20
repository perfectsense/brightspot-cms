<%@ page session="false" import="com.psddev.cms.tool.ToolPageContext, com.psddev.cms.tool.page.StorageItemField" %>

<%
    StorageItemField.reallyDoService(new ToolPageContext(pageContext));
%>