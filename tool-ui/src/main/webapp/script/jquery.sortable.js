(function($, win, undef) {

var $win = $(win);
var $doc = $(win.document);

function findRelatedContainers($selected) {
    var itemType = $selected.attr('data-sortable-item-type');

    return itemType ? $('[data-sortable-valid-item-types~="' + itemType + '"]') : $('.plugin-sortable._sortable:not([data-sortable-valid-item-types])');
}

$.plugin2('sortable', {
    '_defaultOptions': {
        'itemSelector': '> *',
        'scrollingDown': false,
        'scrollingUp': false,
        'scrollInterval': null
    },
    'scrollPage': function() {
      var scrollByYaxis = 50;

      if (this.option().scrollingDown) {
        window.scrollBy(0 , scrollByYaxis );
      }
      if (this.option().scrollingUp) {
        window.scrollBy(0 , -scrollByYaxis);
      }
    },
    '_create': function(container) {
        var $container = $(container);
        var options = this.option();
        var self = this;

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

                options.scrollInterval = window.setInterval(function(){self.scrollPage()}, 100);

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

                // Trigger an event to indicate 'sortable' is started
                $container.trigger('sortable.start', [this]);

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
                var scrollOffset = 100;

                //scroll page as user drags sortable item down and up the page
                options.scrollingUp = false;
                options.scrollingDown = false;
                if (event.clientY <=  scrollOffset){
                  options.scrollingUp = true;
                }
                if (event.clientY >= $(window).height() - scrollOffset){
                  options.scrollingDown = true;
                }

                $selected.css({
                    'left': event.pageX - $win.scrollLeft() + data.adjustX,
                    'top': event.pageY - $win.scrollTop() + data.adjustY
                });

                $selected.hide();
                data.$dragCover.hide();

                // Get the top-most element under the current mouse position
                var dropPoint = $.elementFromPoint(event.clientX, event.clientY)[0];

                if (dropPoint) {

                    // Find all the drop zones for this type.
                    // Sort the drop zones by depth in the DOM so we can guarantee we check
                    // a child before we check the parent.
                    // This will ensure nested drop zones are handled reasonably.
                    findRelatedContainers($selected).sortDepthFirst().each(function() {

                        var $container = $(this);

                        // Get all the children elements of the container (except the selected item if any)
                        var $items = $container.find(options.itemSelector).not($selected);

                        // There are children elements.
                        $items.each(function () {

                            // Check if the drop point is inside the element
                            if ($.contains(this, dropPoint)) {
                                $dropContainer = $container;
                                $drop = $(this);
                                return false;
                            }
                        });

                        // If the drop point was not on one of the child elements,
                        // lets double check to see if it was on the container element itself.
                        // This will handle cases like an empty list, or when there is padding
                        // on the container.
                        if ($container.is(dropPoint) || $.contains($container[0], dropPoint)) {
                            $dropContainer = $container;
                        }

                        // We can stop searching if we found a drop container
                        if ($dropContainer) {
                            return false;
                        }
                    });
                }

                if ($dropContainer) {

                    // Determine where to put the drop-target placeholder

                    // Is mouse over another draggable element?
                    if ($drop && $drop.length > 0 && $drop[0] !== data.$placeholder[0]) {

                        var $items = $dropContainer.find(options.itemSelector);
                        var placeholderIndex = Array.prototype.indexOf.call($items, data.$placeholder[0]);
                        var dropIndex = Array.prototype.indexOf.call($items, $drop[0]);
                        var position;

                        if (placeholderIndex === -1) {
                            position = 'after';
                        } else if (placeholderIndex > dropIndex) {
                            position =  'before';
                        } else {
                            position = 'after';
                        }

                        // TODO: this does not correctly handle when placeholder is already before or after the current element
                        // and causes the UI to bounce around.

                        // Move the placeholder into position (before or after the element)
                        $drop[position](data.$placeholder);

                    } else {
                        // Mouse is not over a draggable element.
                        // Note it might be in a margin between elements so we need to ensure
                        // we don't make the placeholder jump around.
                        if (!$.contains($dropContainer[0], data.$placeholder[0])) {
                            $dropContainer.append(data.$placeholder);
                        }
                    }
                }

                $selected.show();
                data.$dragCover.show();

            }, function(event, data) {
                var $selected = $(this);
                //clear interval for scrolling page
                window.clearInterval(options.scrollInterval);

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
                $selected.find('input, select, textarea').change();
                $container.trigger('sortable.end', [this]);
            });
        });
    }
});

}(jQuery, window));
