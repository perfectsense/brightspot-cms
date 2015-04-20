<%@ page session="false" import="com.psddev.cms.tool.page.StorageItemField, com.psddev.cms.tool.ToolPageContext" %>

<%
    StorageItemField.reallyDoService(new ToolPageContext(pageContext));
%>
