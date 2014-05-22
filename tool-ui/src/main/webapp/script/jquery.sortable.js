(function($, win, undef) {

var $win = $(win),
        doc = win.document;

$.plugin2('sortable', {
    '_defaultOptions': {
        'itemSelector': '> *'
    },

    '_create': function(container) {
        var $container = $(container),
                options = this.option();

        $container.addClass('_sortable');
        $container.find(options.itemSelector).addClass('_sortable-item');

        $container.delegate(options.itemSelector, 'mousedown.sortable', function(event) {
            var $target = $(event.target);

            if ($target.closest('._sortable')[0] != container) {
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
                var $selected = $(this),
                        selectedOffset = $selected.offset();

                data.originalStyle = $selected.attr('style');

                $selected.css({
                    'height': $selected.height(),
                    'width': $selected.width()
                });

                data.$placeholder = $selected.clone().empty();
                $selected.before(data.$placeholder);

                data.adjustX = selectedOffset.left - event.pageX;
                data.adjustY = selectedOffset.top - event.pageY;

                $selected.css({
                    'display': 'block',
                    'left': event.pageX - $win.scrollLeft() + data.adjustX,
                    'position': 'fixed',
                    'top': event.pageY - $win.scrollTop() + data.adjustY,
                    'z-index': 1000000
                });

            }, function(event, data) {
                var $selected = $(this),
                        $drop,
                        $items,
                        placeholderIndex,
                        dropIndex;

                $selected.css({
                    'left': event.pageX - $win.scrollLeft() + data.adjustX,
                    'top': event.pageY - $win.scrollTop() + data.adjustY
                });

                $selected.hide();
                data.$dragCover.hide();

                $drop = $.elementFromPoint(event.clientX, event.clientY).closest('._sortable-item');

                if ($drop.closest('._sortable')[0] != container) {
                    $drop = $();
                }

                if ($drop.length > 0 && $drop[0] !== data.$placeholder[0]) {
                    $items = $container.find(options.itemSelector);
                    placeholderIndex = Array.prototype.indexOf.call($items, data.$placeholder[0]);
                    dropIndex = Array.prototype.indexOf.call($items, $drop[0]);
                    $drop[placeholderIndex > dropIndex ? 'before' : 'after'](data.$placeholder);
                }

                $selected.show();
                data.$dragCover.show();

            }, function(event, data) {
                var $selected = $(this);

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
            });
        });
    }
});

}(jQuery, window));
