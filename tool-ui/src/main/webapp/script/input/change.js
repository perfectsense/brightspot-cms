define([
    'jquery' ],

function($) {
    var DEFAULT_SELECTED_DATA = 'bsp-change-defaultSelected';

    // Mark changed inputs.
    $(document).on('change', '.inputContainer', function() {
        var $container = $(this);
        var changed = false;

        $container.find('input, textarea').each(function() {
            if (this.defaultValue !== this.value) {
                changed = true;
                return false;
            }
        });

        if (!changed) {
            $container.find('option').each(function() {
                if (this.defaultSelected === this.selected) {
                    return true;
                }

                var $select = $(this).closest('select');
                var select = $select[0];

                if (!select) {
                    changed = true;
                    return false;
                }

                var defaultSelected = $.data(select, DEFAULT_SELECTED_DATA);

                if (!defaultSelected) {
                    defaultSelected = $select.find('> option').eq(0)[0];
                    $.data(select, DEFAULT_SELECTED_DATA, defaultSelected);
                }

                if (defaultSelected !== this) {
                    changed = true;
                    return false;
                }
            });
        }

        $container.toggleClass('state-changed', changed);
    });
});
