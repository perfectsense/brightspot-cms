<%@ page session="false" import="

com.psddev.cms.db.ToolUser,
com.psddev.cms.tool.ToolPageContext,

java.util.ArrayList,
java.util.Collections,
java.util.List,
java.util.Map
" %><%

ToolPageContext wp = new ToolPageContext(pageContext);

if (wp.requireUser()) {
    return;
}

ToolUser user = wp.getUser();

wp.include(
        "/WEB-INF/search.jsp",
        "name", "toolHeader",
        "newJsp", "/content/edit.jsp",
        "newTarget", "_top",
        "resultJsp", "/misc/searchResult.jsp",
        "savedSearchesJsp", "/misc/savedSearches.jsp");

%>
<script type="text/javascript">
    if (typeof jQuery !== 'undefined') (function(win, $, undef) {
        var $headerInput = $('.toolSearch :text');
        var $miscSearchFrame = $('.frame[name="miscSearch"]');
        if (!$headerInput.val() && $miscSearchFrame.length) {
            $headerInput.val($('.frame[name="miscSearch"] .searchFilters .searchInput :text').val()).trigger('input');
            $headerInput[0].select();
        }
    })(window, jQuery);
</script>
