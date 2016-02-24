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
        var $win = $(win);
        var $page = $('#<%= pageId %>');
        var $addedInputs = $();

        $page.delegate('[data-objectId]', 'click', function() {
            var $source = $page.popup('source'),
                    $input = $source.parent().find(':input.objectId'),
                    $link = $(this),
                    $repeatableObjectId = $source.closest('.inputContainer').find('.repeatableObjectId'),
                    $repeatableForm = $source.closest('.repeatableForm'),
                    $sourceContainer,
                    fieldName,
                    $added;

            $input.attr('data-label', $link.clone().find('span.visibilityLabel').remove().end().text().trim());
            $input.attr('data-preview', $link.find('img').attr('src'));
            $input.attr('data-visibility', $link.find('span.visibilityLabel').text());
            $input.val($link.attr('data-objectId'));
            $input.change();

            if ($repeatableObjectId.length > 0) {
                $sourceContainer = $source.closest('li');

                $sourceContainer.attr('data-sortable-item-type', $link.attr('data-type-id'));

                if ($sourceContainer.nextAll('li').length === 0) {
                    $repeatableObjectId.find('.addButton').click();

                    $added = $sourceContainer.nextAll('li').eq(0);

                    if ($added.length > 0) {
                        $addedInputs = $addedInputs.add($added.find(':input.objectId'));

                        $page.popup('source', $added.find('a.objectId-select'));
                        $win.scrollTop($win.scrollTop() + $sourceContainer.outerHeight(true));
                        return false;
                    }
                }

            } else if ($repeatableForm.length > 0) {
                $sourceContainer = $source.closest('li');

                if ($sourceContainer.length > 0 && $sourceContainer.nextAll('li').length === 0) {
                    fieldName = $source.closest('.inputContainer').attr('data-field-name');

                    $repeatableForm.find('.addButton[data-sortable-item-type="' + $sourceContainer.attr('data-sortable-item-type') + '"]').eq(-1).trigger('click', [
                        function () {
                            var added = this;

                            require([ 'bsp-utils' ], function (bsp_utils) {
                                bsp_utils.onDomInsert(added, '.inputContainer[data-field-name="' + fieldName + '"] > .inputSmall > a.objectId-select', {
                                    'insert': function (select) {
                                        var $select = $(select);

                                        $addedInputs = $addedInputs.add($select.closest('.inputSmall').find('> :input.objectId'));
                                        $page.popup('source', $select);
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

                        $repeatableForm.find('.addButton[data-sortable-item-type="' + $sourceContainer.attr('data-sortable-item-type') + '"]').eq(-1).trigger('click', [
                            function () {
                                var added = this;
                                var id = $(added).find('> :hidden[name$=".id"]').val();

                                require([ 'bsp-utils' ], function (bsp_utils) {
                                    bsp_utils.onDomInsert($sourceContainer.parent()[0], '.objectInputs[data-object-id="' + id + '"] > .inputContainer[data-field-name="' + fieldName + '"] > .inputSmall > a.objectId-select', {
                                        'insert': function (select) {
                                            var $select = $(select);

                                            $addedInputs = $addedInputs.add($select.closest('.inputSmall').find('> :input.objectId'));
                                            $page.popup('source', $select);
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

        var $popup = $page.closest('.popup');

        $popup.on('close', function (event) {
            if ($popup[0] === event.target) {
                $addedInputs.each(function () {
                    var $input = $(this);

                    if (!$input.val()) {
                        $input.closest('li').remove();

                        var $itemEdit = $input.closest('.itemEdit');

                        if ($itemEdit.length > 0) {
                            var index = $itemEdit.parent().index($itemEdit);

                            $itemEdit.closest('.viewCarousel').find('.carousel-tiles > li').eq(index).remove();
                            $itemEdit.closest('.repeatableForm').find('> ol, > ul').find('> li').eq(index).remove();
                            $itemEdit.remove();
                        }
                    }
                });
            }
        });
    })(window, jQuery);
</script>
