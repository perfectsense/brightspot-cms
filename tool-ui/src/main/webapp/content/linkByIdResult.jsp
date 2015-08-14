<%@ page session="false" import="

com.psddev.cms.db.Content,
com.psddev.cms.db.Directory,
com.psddev.cms.tool.Search,
com.psddev.cms.tool.SearchResultRenderer,
com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.db.State,

java.io.IOException
" %><%

// --- Logic ---

ToolPageContext wp = new ToolPageContext(pageContext);
if (wp.requireUser()) {
    return;
}

Search search = new Search(wp);
String pageId = wp.createId();
String removeId = wp.createId();

// --- Presentation ---

%><div id="<%= pageId %>">
    <% new SearchResultRenderer(wp, search) {

        @Override
        public void renderBeforeItem(Object item) throws IOException {
            writer.start("span",
                    "class", "link",
                    "data-id", State.getInstance(item).getId(),
                    "data-permalink", item instanceof Content ?
                            ((Content) item).getPermalink() :
                            State.getInstance(item).as(Directory.ObjectModification.class).getPermalink());
        }

        @Override
        public void renderAfterItem(Object item) throws IOException {
            writer.end();
        }
    }.render(); %>
</div>

<script type="text/javascript">
    if (typeof jQuery !== 'undefined') (function(win, $) {
        var $win = $(win),
                $page = $('#<%= pageId %>');

        $page.delegate('[data-permalink]', 'click', function() {
            var $sourceParent = $page.popup('source').parent(),
                    $idInput = $sourceParent.find('.rte-dialogLinkId, .rte2-dialogLinkId'),
                    $hrefInput = $sourceParent.find('.rte-dialogLinkHref, .rte2-dialogLinkHref'),
                    $link = $(this);

            $idInput.val($link.attr('data-id'));
            $hrefInput.val($link.attr('data-permalink'));
            $hrefInput.change();
            $page.popup('close');
            return false;
        });
    })(window, jQuery);
</script>
