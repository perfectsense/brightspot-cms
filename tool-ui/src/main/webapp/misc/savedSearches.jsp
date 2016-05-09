<%@ page session="false" import="
    com.psddev.dari.util.StringUtils,
    com.psddev.cms.tool.Search,
    com.psddev.cms.tool.ToolPageContext,
    com.psddev.cms.db.ToolUser,

    java.util.ArrayList,
    java.util.List,
    java.util.Map,
    java.util.Collections
"%>

<%
    ToolPageContext wp = new ToolPageContext(pageContext);

    ToolUser user = wp.getUser();
    Map<String, String> savedSearches = user.getSavedSearches();

    String searchNameToRemove = wp.param(String.class, "remove");
    if (!StringUtils.isBlank(searchNameToRemove)) {
        savedSearches.remove(searchNameToRemove);
        user.save();
    }

    String context = wp.param(String.class, Search.CONTEXT_PARAMETER);

    if (savedSearches.isEmpty()) {
        wp.writeStart("div", "class", "message");
            wp.writeHtml("No saved searches yet.");
        wp.writeEnd();
    }
    else {
        List<String> savedSearchNames = new ArrayList<String>(savedSearches.keySet());

        Collections.sort(savedSearchNames, String.CASE_INSENSITIVE_ORDER);

        wp.writeStart("ul", "class", "links");
            for (String savedSearchName : savedSearchNames) {
                String savedSearch = savedSearches.get(savedSearchName);

                    wp.writeStart("li");
                        wp.writeStart("a",
                              "href", StringUtils.addQueryParameters(wp.cmsUrl("/misc/search.jsp") + "?" + savedSearch, Search.CONTEXT_PARAMETER, context),
                              "target", "miscSearch");
                            wp.writeHtml(savedSearchName);
                        wp.writeEnd();
                        wp.writeStart("a",
                              "class", "savedSearches-remove",
                              "href", wp.cmsUrl("/misc/savedSearches.jsp", "remove", savedSearchName, Search.CONTEXT_PARAMETER, context),
                              "target", "savedSearches");
                    wp.writeEnd();
            }
        wp.writeEnd();
    }
    wp.writeStart("form", "action", wp.cmsUrl("/misc/savedSearches.jsp", Search.CONTEXT_PARAMETER, context), "name", "refreshSavedSearches");
    wp.writeEnd();

%>