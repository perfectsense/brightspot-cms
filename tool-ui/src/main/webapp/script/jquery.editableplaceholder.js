(function($, window, undefined) {

$.plugin2('editablePlaceholder', {
    '_init': function(selector) {
        var plugin = this;

        function getPlaceholder($input) {
            return $input.attr('data-editable-placeholder') || $input.prop('placeholder');
        }

        /**
         * Get or set the value for an input or a rich text editor.
         *
         * @param {Element|jQuery} $input
         * Element or jQuery object for the input or text area.
         *
         * @param {String} [value]
         * Optional value to set.
         * If provided we will set the value.
         * If not provided we will get the value.
         */
        function val($input, value) {
            
            var rte;
            
            // Convert element to jquery object if necessary
            $input = $($input);

            // Determine if this is a textarea rich text editor.
            // The rte controller is stored in the 'rte2' data.
            rte = $input.data('rte2');

            if (rte) {
                if (value === undefined) {
                    return rte.toHTML();
                } else {
                    if (value==='') {
                        rte.rte.empty();
                    } else {
                        rte.fromHTML(value);
                    }

                    // In some cases when we put HTML into the rich text editor,
                    // it might get modified when we pull it back out.
                    // So to ensure that we know when the value was actually modified by the user,
                    // we'll pull out the HTML right after we put it in, and save
                    // it for future comparison.
                    $input.data('editable-placeholder-rte-value', rte.toHTML());

                    rte.placeholderRefresh();
                }
            } else {
                if (value === undefined) {
                    return $input.val();
                } else {
                    $input.val(value);
                }
            }
        }

        plugin.$caller.delegate(selector, 'focus.editablePlaceholder mouseenter.editablePlaceholder rteFocus', function() {
            var $input = $(this);
            
            if (!val($input)) {
                val($input, getPlaceholder($input));
                $.data(this, 'editable-placeholder-empty-before-focus', true);
            }
        });

        plugin.$caller.delegate(selector, 'input.editablePlaceholder rteChange', function() {
            $.removeData(this, 'editable-placeholder-empty-before-focus');
        });

        plugin.$caller.delegate(selector, 'blur.editablePlaceholder rteBlur', function() {
            
            var $input = $(this), placeholder, rte, value, valueRte;

            value = val($input);
            
            // In case this is a rich text editor, get the value that was originally set on the focus event
            valueRte = $input.data('editable-placeholder-rte-value');
            
            placeholder = getPlaceholder($input);

            if ( $.data(this, 'editable-placeholder-empty-before-focus') ||
                 value === placeholder ||
                 value === valueRte) {
                
                val($input, '');
            }
        });

        plugin.$caller.delegate(selector, 'mouseleave.editablePlaceholder', function() {
            var $input = $(this);

            if (!$input.is(':focus') &&
                val($input) === getPlaceholder($input)) {
                
                val($input, '');
            }
        });
    }
});

})(jQuery, window);
