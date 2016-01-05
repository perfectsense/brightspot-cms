<%@ page session="false" import="

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
            State itemState = State.getInstance(item);

            writer.start("span",
                    "class", "link",
                    "data-type-id", itemState.getTypeId(),
                    "data-objectId", itemState.getId());
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

        $page.delegate('[data-objectId]', 'click', function() {
            var $source = $page.popup('source'),
                    $input = $source.parent().find(':input.objectId'),
                    $link = $(this),
                    $repeatableObjectId = $source.closest('.inputContainer').find('.repeatableObjectId'),
                    $repeatableForm = $source.closest('.repeatableForm'),
                    $sourceContainer,
                    fieldName,
                    $added;

            $input.attr('data-label', $link.text());
            $input.attr('data-preview', $link.find('img').attr('src'));
            $input.val($link.attr('data-objectId'));
            $input.change();

            if ($repeatableObjectId.length > 0) {
                $sourceContainer = $source.closest('li');

                $sourceContainer.attr('data-sortable-item-type', $link.attr('data-type-id'));

                if ($sourceContainer.nextAll('li').length === 0) {
                    $repeatableObjectId.find('.addButton').click();

                    $added = $sourceContainer.nextAll('li').eq(0);

                    if ($added.length > 0) {
                        $page.popup('source', $added.find('a.objectId-select'));
                        $win.scrollTop($win.scrollTop() + $sourceContainer.outerHeight(true));
                        return false;
                    }
                }

            } else if ($repeatableForm.length > 0) {
                $sourceContainer = $source.closest('li');

                if ($sourceContainer.length > 0 && $sourceContainer.nextAll('li').length === 0) {
                    fieldName = $source.closest('.inputContainer').attr('data-field-name');

                    $repeatableForm.find('.addButton').eq(-1).trigger('click', [
                        function () {
                            var added = this;

                            require([ 'bsp-utils' ], function (bsp_utils) {
                                bsp_utils.onDomInsert(added, '.inputContainer[data-field-name="' + fieldName + '"] > .inputSmall > a.objectId-select', {
                                    'insert': function (select) {
                                        $page.popup('source', $(select));
                                        $win.scrollTop($win.scrollTop() + $sourceContainer.outerHeight(true));
                                    }
                                })
                            });
                        }
                    ]);

                    return false;

                } else {
                    $sourceContainer = $source.closest('.itemEdit');

                    if ($sourceContainer.length > 0 && $sourceContainer.nextAll('.itemEdit').length === 0) {
                        fieldName = $source.closest('.inputContainer').attr('data-field-name');

                        $repeatableForm.find('.addButton').eq(-1).trigger('click', [
                            function () {
                                var added = this;
                                var id = $(added).find('> :hidden[name$=".id"]').val();

                                require([ 'bsp-utils' ], function (bsp_utils) {
                                    bsp_utils.onDomInsert($sourceContainer.parent()[0], '.objectInputs[data-object-id="' + id + '"] > .inputContainer[data-field-name="' + fieldName + '"] > .inputSmall > a.objectId-select', {
                                        'insert': function (select) {
                                            $page.popup('source', $(select));
                                        }
                                    })
                                });
                            }
                        ]);

                        return false;
                    }
                }
            }

            $page.popup('close');
            return false;
        });
    })(window, jQuery);
</script>
