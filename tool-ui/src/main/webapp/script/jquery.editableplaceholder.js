(function($, window, undefined) {

$.plugin2('editablePlaceholder', {
    '_init': function(selector) {
        var plugin = this;

        function getPlaceholder($input) {
            return $input.attr('data-editable-placeholder') || $input.prop('placeholder');
        }

        plugin.$caller.delegate(selector, 'focus.editablePlaceholder mouseenter.editablePlaceholder', function() {
            var $input = $(this);

            if (!$input.val()) {
                $.data(this, 'editable-placeholder-empty-before-focus', true);
                $input.val(getPlaceholder($input));
            }
        });

        plugin.$caller.delegate(selector, 'input.editablePlaceholder', function() {
            $.removeData(this, 'editable-placeholder-empty-before-focus');
        });

        plugin.$caller.delegate(selector, 'blur.editablePlaceholder', function() {
            var $input = $(this);

            if ($.data(this, 'editable-placeholder-empty-before-focus') ||
                    $input.val() === getPlaceholder($input)) {
                $input.val('');
            }
        });

        plugin.$caller.delegate(selector, 'mouseleave.editablePlaceholder', function() {
            var $input = $(this);

            if (!$input.is(':focus') &&
                    $input.val() === getPlaceholder($input)) {
                $input.val('');
            }
        });
    }
});

})(jQuery, window);
