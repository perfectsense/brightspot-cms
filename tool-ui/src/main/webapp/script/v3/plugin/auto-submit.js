(function(globals, factory) {
    if (typeof define === 'function' && define.amd) {
        define([ 'jquery', 'bsp-utils' ], factory);

    } else {
        factory(globals.jQuery, globals.bsp_utils, globals);
    }

})(this, function($, bsp_utils, globals) {
    return bsp_utils.plugin(globals, 'bsp', 'autoSubmit', {
        '_defaultOptions': {
            'disableAutocomplete': true,
            'inputSubmitDelay': 100,
            'submitThrottle': 500
        },

        '_each': function(input) {
            var plugin = this;
            var $input = $(input);
            var $form = $input.closest('form');

            var submit;
            var lastFormData;

            submit = bsp_utils.throttle(plugin.option(input, 'submitThrottle'), function() {
                var formData = $form.serialize();

                if (lastFormData !== formData) {
                    lastFormData = formData;
                    $form.submit();
                }
            });

            var submitCheck;
            var inputSubmitDelay = plugin.option(input, 'inputSubmitDelay');
            var submitTimer;

            submitCheck = inputSubmitDelay <= 0 ? submit : function() {
                clearTimeout(submitTimer);

                submitTimer = setTimeout(function() {
                    submitTimer = null;
                    submit();
                }, inputSubmitDelay);
            };

            if (plugin.option(input, 'disableAutoComplete')) {
                (input === $form[0] ? $form.find(':input') : $input).prop('autocomplete', 'off');
            }

            plugin._on(input, 'change', function(event) {
                if (event.target.name) {
                    submit();
                }
            });
            
            plugin._on(input, 'input', function(event){
                if (event.target.name) {
                    submitCheck();
                }
            });
        }
    });
});
