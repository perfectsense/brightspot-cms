<%@ page session="false" import="

com.psddev.cms.tool.CmsTool,
com.psddev.cms.tool.PageWriter,
com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.util.ObjectUtils
"
 language="java"
 contentType="text/css; charset=UTF-8"
 pageEncoding="UTF-8"
%><%
    
ToolPageContext wp = new ToolPageContext(pageContext);

if (wp.requireUser()) {
    return;
}

CmsTool cms = wp.getCmsTool();

for (CmsTool.CssClassGroup group : cms.getTextCssClassGroups()) {
    
    String groupName = group.getInternalName();

    for (CmsTool.CssClass c : group.getCssClasses()) {
        String css = c.getCss();
        String cmsOnlyCss = c.getCmsOnlyCss();

        wp.write(".cms-");
        wp.write(groupName);
        wp.write("-");
        wp.write(c.getInternalName());
        wp.write(" { ");

        if (!ObjectUtils.isBlank(css)) {
            wp.write(css);
        }

        if (!ObjectUtils.isBlank(cmsOnlyCss)) {
            wp.write(cmsOnlyCss);
        }

        wp.write(" }\n");
    }
}

%>
