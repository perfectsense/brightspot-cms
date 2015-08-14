define([
    'jquery',
    'bsp-utils' ],

function($, bsp_utils) {
    var $win = $(window);
    var $doc = $(document);

    // Automatically focus on certain elements.
    bsp_utils.onDomInsert(document, '[autofocus], .autoFocus', {
        'insert': function(input) {
            var $input = $(input);

            if ($input.closest('.dashboard-widget').length > 0) {
                return;
            }

            if ($input.closest('.popup').length > 0) {
                $input.focus();
            }

            var focus = document.activeElement;

            if (!focus || focus === document || focus === document.body) {
                $input.focus();
            }
        }
    });

    $doc.ready(function() {
        $(document.activeElement).focus();
    });

    // Make sure that the label for the focused input is visible.
    $doc.on('focus', ':input', function() {
        var $input = $(this);
        var $firstInput = $input.closest('form').find('.inputContainer:visible').eq(0);
        var $parents = $input.parentsUntil('form');

        $parents.addClass('state-focus');

        // Don't expand the label for rich text editor
        if ($input.closest('.CodeMirror').length) {
            return;
        }
        
        function onScroll() {
            var focusLabelHeight;
            var index;
            var $parent;
            var headerHeight = $('.toolHeader').outerHeight();
            var labelText = '';
            var $focusLabel = $('.focusLabel');
            var $parentLabel;
            var parentLabelText;

            if ($focusLabel.length === 0) {
                $focusLabel = $('<div/>', { 'class': 'focusLabel' });

                $(document.body).append($focusLabel);
            }

            focusLabelHeight = $focusLabel.outerHeight();

            $parents.each(function() {
                $(this).find('> .inputLabel label, > .repeatableLabel').css('visibility', '');
            });

            for (index = $parents.length - 1; index >= 0; -- index) {
                $parent = $($parents[index]);

                if ($parent.offset().top > $win.scrollTop() + (focusLabelHeight * 2 / 3) + headerHeight) {
                    if (labelText) {
                        $focusLabel.css({
                            'left': $firstInput.offset().left,
                            'top': headerHeight,
                            'width': $firstInput.outerWidth()
                        });
                        $focusLabel.text(labelText);
                        $focusLabel.show();
                        return;

                    } else {
                        break;
                    }
                }

                $parentLabel = $parent.find('> .inputLabel label, > .repeatableLabel');
                parentLabelText = $parentLabel.text();

                if (parentLabelText) {
                    $parentLabel.css('visibility', 'hidden');

                    if (labelText) {
                        labelText += ' \u2192 ';
                    }

                    labelText += parentLabelText;
                }
            }

            $focusLabel.hide();
        }

        onScroll();
        $win.bind('scroll.focusLabel', bsp_utils.throttle(50, onScroll));
    });

    $doc.on('blur', ':input', function() {
        $(this).parents('.state-focus').each(function() {
            var $parent = $(this);

            $parent.removeClass('state-focus');
            $parent.find('> .inputLabel label, > .repeatableLabel').css('visibility', '');
        });

        $('.focusLabel').hide();
        $win.unbind('.focusLabel');
    });
});
