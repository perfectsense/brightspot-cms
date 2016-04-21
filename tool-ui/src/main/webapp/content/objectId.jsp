<%@ page session="false" import="

com.psddev.cms.tool.ToolPageContext,

java.util.UUID
" %><%

ToolPageContext wp = new ToolPageContext(pageContext);

if (wp.requireUser()) {
    return;
}

wp.include(
        "/WEB-INF/search.jsp",
        "newJsp", "/content/edit.jsp",
        "newTarget", "objectId-create-" + UUID.randomUUID().toString(),
        "resultJsp", "/content/objectIdResult.jsp");
%>
