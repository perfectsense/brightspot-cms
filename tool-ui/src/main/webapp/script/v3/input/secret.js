define([ 'jquery', 'bsp-utils' ], function ($, bsp_utils) {
    bsp_utils.onDomInsert(document, 'input[type="text"].secret', {
        insert: function (input) {
            var $input = $(input);
            var showText = $input.attr('data-show-text') || 'Show Secret';
            var hideText = $input.attr('data-hide-text') || 'Hide Secret';
            var hidden = true;

            var $secret = $('<div/>', {
                'class': 'SecretInputPlaceholder'
            });

            function updatePlaceholder() {
                $secret.text($input.val() ? '\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022' : '');
            }

            updatePlaceholder();
            $input.on('change input', updatePlaceholder);

            $input.before($secret);
            $secret.before($('<div/>', {
                'class': 'SecretInputToggle',
                html: $('<a/>', {
                    href: '#',
                    text: showText,
                    click: function () {
                        hidden = !hidden;

                        $(this).text(hidden ? showText : hideText);
                        $input.toggle(!hidden);
                        $secret.toggle(hidden);

                        return false;
                    }
                })
            }));
        }
    });
});