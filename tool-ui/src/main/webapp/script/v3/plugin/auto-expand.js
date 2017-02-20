(function(globals, factory) {
    if (typeof define === 'function' && define.amd) {
        define([ 'jquery', 'bsp-utils' ], factory);

    } else {
        factory(globals.jQuery, globals.bsp_utils, globals);
    }

})(this, function($, bsp_utils, globals) {
    var SHADOW_DATA_KEY = 'shadow';

    var $d = $(document);
    var $shadowContainer;

    return bsp_utils.plugin(globals, 'bsp', 'autoExpand', {
        '_install': function() {
            var plugin = this;

            plugin._on(window, 'resize', bsp_utils.throttle(200, function() {
                plugin.expand($.makeArray($d.find('.' + plugin._itemClassName + ':visible')));
            }));
        },

        '_init': function(roots, selector) {
            var plugin = this;

            plugin._on(roots, 'change input', selector, function() {
                plugin.expand(this);
            });
        },

        '_each': function(input) {
            if (this._data(input, SHADOW_DATA_KEY)) {
                return;
            }

            var $input = $(input);

            if (!$input.is(':visible')) {
                return;
            }

            var display = $input.css('display');
            var $element = $('<div/>');
            var inputCss;
            var inputCssKey;
            var inputCssValue;

            if (window.getComputedStyle) {
                inputCss = window.getComputedStyle(input, null);
                var inputCssIndex = 0;
                var inputCssLength = inputCss.length;

                for (; inputCssIndex < inputCssLength; ++ inputCssIndex) {
                    inputCssKey = inputCss.item(inputCssIndex);
                    inputCssValue = inputCss.getPropertyValue(inputCssKey);
                    $element[0].style.setProperty(inputCssKey, inputCssValue);
                }

            } else {
                inputCss = input.currentStyle;

                for (inputCssKey in inputCss) {
                    $element[0].style[inputCssKey] = inputCss[inputCssKey];
                }
            }

            $element.css({
                'height': 'auto',
                'white-space': 'pre-wrap',
                'width': 'auto'
            });

            this._data(input, SHADOW_DATA_KEY, {
                'display': display,
                '$input': $input,
                '$element': $element
            });
        },

        '_all': function(inputs, inExpand) {
            var plugin = this;
            var $inputs = $(inputs);

            if (!$shadowContainer) {
                $shadowContainer = $('<div/>', {
                    'class': plugin._classNamePrefix + 'shadows',
                    'css': {
                        'left': -10000,
                        'position': 'absolute',
                        'top': -10000,
                        'visibility': 'hidden'
                    }
                });

                $(document.body).append($shadowContainer);
            }

            $inputs.each(function() {
                var shadow = plugin._data(this, SHADOW_DATA_KEY);

                if (shadow && !shadow.elementAppended) {
                    $shadowContainer.append(shadow.$element);
                    $shadowContainer.append(' ');
                    shadow.elementAppended = true;
                }
            });

            if (!inExpand) {
                plugin.expand(inputs);
            }
        },

        'expand': function(inputs) {
            var plugin = this;
            var $inputs = $(inputs).filter(':visible');
            var shadows = [ ];

            $inputs.each(function() {
                plugin._each(this);
            });

            plugin._all(inputs, true);

            // Group reads and writes together across multiple inputs to
            // minimize forced synchronous layouts.
            $inputs.each(function() {
                var shadow = plugin._data(this, SHADOW_DATA_KEY);

                if (shadow) {
                    shadows.push(shadow);
                }
            });

            // Read the input dimension.
            $.each(shadows, function(i, shadow) {
                var bounds = shadow.$input[0].getBoundingClientRect();

                shadow.width = bounds.right - bounds.left;
                shadow.height = bounds.bottom - bounds.top;
            });

            // Write the input text into the shadow.
            $.each(shadows, function(i, shadow) {
                var $input = shadow.$input;
                var value = $input.val();
                var extra = shadow.display === 'block' ? ' foo foo foo' : ' foo';

                shadow.$element.text(value ?
                value + extra :
                        ($input.prop('placeholder') || extra));
            });

            // Write the shadow width if the input's a block element.
            $.each(shadows, function(i, shadow) {
                if (shadow.display === 'block') {
                    shadow.$element.css('width', shadow.width);
                }
            });

            // Read the shadow size.
            $.each(shadows, function(i, shadow) {
                var shadowBounds = shadow.$element[0].getBoundingClientRect();

                if (shadow.display === 'block') {
                    shadow.height = shadowBounds.bottom - shadowBounds.top;

                } else {
                    shadow.width = shadowBounds.right - shadowBounds.left;
                }
            });

            // Write the input size using the shadow size.
            $.each(shadows, function(i, shadow) {
                var $input = shadow.$input;

                $input.css('overflow', 'hidden');

                if (shadow.display === 'block') {
                    $input.css('height', shadow.height);

                } else {
                    $input.css('width', shadow.width);
                }
            });

            return $inputs;
        }
    });
});
