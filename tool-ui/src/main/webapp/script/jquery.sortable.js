(function($, win, undef) {

var $win = $(win);
var $doc = $(win.document);

function findRelatedContainers($selected) {
    var itemType = $selected.attr('data-sortable-item-type');

    return itemType ? $('[data-sortable-valid-item-types~="' + itemType + '"') : $();
}

$.plugin2('sortable', {
    '_defaultOptions': {
        'itemSelector': '> *'
    },

    '_create': function(container) {
        var $container = $(container);
        var options = this.option();

        $container.addClass('_sortable');
        $container.find(options.itemSelector).addClass('_sortable-item');

        $container.delegate(options.itemSelector, 'mousedown.sortable', function(event) {
            var $target = $(event.target);

            if ($target.closest('._sortable')[0] !== container) {
                return;
            }

            if ($target.is(':input') ||
                    $target.closest('.CodeMirror').length > 0) {
                return;
            }

            // Hack to ignore mousedown firing on a scrollbar in Firefox.
            if (event.pageX > ($target.offset().left + $target.outerWidth() - 20)) {
                return;
            }

            $.drag(this, event, function(event, data) {
                var $selected = $(this);
                var selectedOffset = $selected.offset();

                data.originalStyle = $selected.attr('style');

                $selected.height($selected.height());
                $selected.width($selected.width());

                data.$placeholder = $selected.clone().empty();
                $selected.before(data.$placeholder);

                data.adjustX = selectedOffset.left - event.pageX;
                data.adjustY = selectedOffset.top - event.pageY;

                $selected.addClass('sortable-dragging');
                $selected.css({
                    'display': 'block',
                    'left': event.pageX - $win.scrollLeft() + data.adjustX,
                    'position': 'fixed',
                    'top': event.pageY - $win.scrollTop() + data.adjustY,
                    'z-index': 1000000
                });

                data.$parents = $selected.parentsUntil('.toolContent');

                data.$parents.addClass('sortable-parent');

                var $relatedContainers = findRelatedContainers($selected);

                if ($relatedContainers.length > 1) {
                    $relatedContainers.addClass('sortable-droppable');
                }

            }, function(event, data) {
                var $selected = $(this);
                var $dropContainer;
                var $drop;

                $selected.css({
                    'left': event.pageX - $win.scrollLeft() + data.adjustX,
                    'top': event.pageY - $win.scrollTop() + data.adjustY
                });

                $selected.hide();
                data.$dragCover.hide();

                var dropPoint = $.elementFromPoint(event.clientX, event.clientY)[0];

                if (dropPoint) {
                    $container.find(options.itemSelector).each(function() {
                        if ($.contains(this, dropPoint)) {
                            $dropContainer = $container;
                            $drop = $(this);
                            return false;
                        }
                    });

                    if (!$drop) {
                        findRelatedContainers($selected).each(function() {
                            var $container = $(this);
                            var $items = $container.find(options.itemSelector).not($selected);

                            if ($items.length === 0) {
                                if (this === dropPoint || $.contains(this, dropPoint)) {
                                    $dropContainer = $container;
                                }

                            } else {
                                $items.each(function () {
                                    if ($.contains(this, dropPoint)) {
                                        $dropContainer = $container;
                                        $drop = $(this);
                                        return false;
                                    }
                                });
                            }

                            if ($dropContainer) {
                                return false;
                            }
                        });
                    }
                }

                if ($dropContainer) {
                    if ($drop && $drop.length > 0 && $drop[0] !== data.$placeholder[0]) {
                        var $items = $container.find(options.itemSelector);
                        var placeholderIndex = Array.prototype.indexOf.call($items, data.$placeholder[0]);
                        var dropIndex = Array.prototype.indexOf.call($items, $drop[0]);

                        $drop[placeholderIndex > dropIndex ? 'before' : 'after'](data.$placeholder);

                    } else {
                        $dropContainer.append(data.$placeholder);
                    }
                }

                $selected.show();
                data.$dragCover.show();

            }, function(event, data) {
                var $selected = $(this);

                $selected.removeClass('sortable-dragging');

                if (data.originalStyle) {
                    $selected.attr('style', data.originalStyle);

                } else {
                    $selected.removeAttr('style');
                }

                $selected.find('.richtext').each(function() {
                    var $rte = $(this);
                    var $rteIframe = $rte.closest('.inputContainer').find('.rte-container iframe');

                    if ($rteIframe.length === 0) {
                        return;
                    }

                    $rte.val($($rteIframe[0].contentDocument.body).html());
                });

                if (data.$placeholder.next()[0] !== $selected[0]) {
                    data.$placeholder.after($selected);
                }
                data.$placeholder.remove();

                var oldInputName = $container.attr('data-sortable-input-name');
                var newInputName = $selected.closest('._sortable').attr('data-sortable-input-name');

                if (oldInputName !== newInputName) {
                    $selected.find('[name^="' + oldInputName + '"]').each(function() {
                        var $input = $(this);

                        $input.attr('name', $input.attr('name').replace(oldInputName, newInputName));
                    });
                }

                $selected.find('.richtext').each(function() {
                    var $rte = $(this);
                    var $rteIframe = $rte.closest('.inputContainer').find('.rte-container iframe');

                    if ($rteIframe.length === 0) {
                        return;
                    }

                    var rteValue = $rte.val();
                    var $inputContainer = $rte.closest('.inputContainer');
                    $rte = $rte.clone();
                    var $rteContainer = $inputContainer.find('.rte-container');

                    $rte.val(rteValue);
                    $rteContainer.after($rte);
                    $rteContainer.remove();
                    $rte.removeClass('plugin-expandable plugin-rte rte-textarea rte-source');
                    $rte.show();
                    $inputContainer.trigger('create');
                });

                data.$parents.removeClass('sortable-parent');
                findRelatedContainers($selected).removeClass('sortable-droppable');

                // Trigger an event so other code can act after sortable is done
                $container.trigger('sortable.end', [this]);
            });
        });
    }
});

}(jQuery, window));
