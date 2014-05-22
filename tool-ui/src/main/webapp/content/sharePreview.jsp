<%@ page session="false" import="

com.psddev.cms.db.PageFilter,
com.psddev.cms.db.Preview,
com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.db.ObjectType,
com.psddev.dari.util.ObjectUtils,
com.psddev.dari.util.StringUtils,

java.util.Date,
java.util.Map,
java.util.UUID
" %><%

// --- Logic ---

ToolPageContext wp = new ToolPageContext(pageContext);
if (wp.requireUser()) {
    return;
}

String objectString = wp.param(PageFilter.PREVIEW_OBJECT_PARAMETER);
if (ObjectUtils.isBlank(objectString)) {
    return;
}

Map<String, Object> objectMap = (Map<String, Object>) ObjectUtils.fromJson(objectString.trim());
Preview preview = new Preview();
preview.setCreateDate(new Date());
preview.setObjectType(ObjectType.getInstance(ObjectUtils.to(UUID.class, objectMap.remove("_typeId"))));
preview.setObjectId(ObjectUtils.to(UUID.class, objectMap.remove("_id")));
preview.setObjectValues(objectMap);
preview.setSite(wp.getSite());
wp.publish(preview);

String host = request.getHeader("X-Forwarded-Host");
if (ObjectUtils.isBlank(host)) {
    host = request.getHeader("Host");
}
response.sendRedirect(StringUtils.addQueryParameters(
        wp.getCmsTool().getPreviewUrl(),
        PageFilter.PREVIEW_ID_PARAMETER, preview.getId(),
        "_date", request.getParameter("previewDate")));
%>
