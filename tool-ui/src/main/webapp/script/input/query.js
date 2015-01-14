define([
    'jquery',
    'bsp-utils' ],

function($, bsp_utils) {
    var $frames;
    var $fields = $();

    bsp_utils.onDomInsert(document, '.queryField', {
        'insert': function(field) {
            var $field = $(field);
            var inputValue = $field.find('input').val();
            var search;
            var $body = $(window.document.body);
            var $frame;

            $fields = $fields.add($field);

            if (!$frames) {
                $frames = $('<div/>', {
                    'class': 'queryField_frames'
                });

                $(window.document.body).append($frames);
            }

            var search = inputValue ? JSON.parse(inputValue)['cms.ui.search'] : null;

            if (search) {
                var types = $field.closest('.inputLarge').attr('data-generic-arguments');

                if (types) {
                    search.types = types.split('\\s*,\\s*');
                }
            }

            $frame = $('<div/>', {
                'class': 'frame',
                'html': $('<form/>', {
                    'method': 'post',
                    'action': CONTEXT_PATH + '/queryField',
                    'html': $('<input/>', {
                        'type': 'hidden',
                        'name': 'search',
                        'value': search ? JSON.stringify(search) : ''
                    })
                })
            });

            $.data($field[0], 'query-$frame', $frame);
            $.data($frame[0], 'query-$field', $field);
        }
    });

    setInterval(function() {
        function inNonInitializedRepeatable($field) {
            var $repeatable = $field.closest('.repeatableForm, .repeatableInputs, .repeatableLayout, .repeatableObjectId, .repeatableText');

            return $repeatable.length > 0 && !$repeatable.is('.plugin-repeatable');
        }

        $fields.filter(':visible').each(function() {
            var $field = $(this);

            if (inNonInitializedRepeatable($field)) {
                return;
            }

            var $frame = $.data($field[0], 'query-$frame');
            var fieldOffset = $field.offset();

            if ($frame.closest('.queryField_frames').length === 0) {
                $frames.append($frame);
                $frames.trigger('create');
            }

            $frame.css({
                'left': fieldOffset.left,
                'position': 'absolute',
                'top': fieldOffset.top,
                'z-index': 1000000
            });

            $frame.outerWidth($field.outerWidth());
            $field.outerHeight($frame.outerHeight());
            $frame.show();
        });

        $fields.filter(':not(:visible)').each(function() {
            var $field = $(this);

            if (inNonInitializedRepeatable($field)) {
                return;
            }

            var $frame = $.data($field[0], 'query-$frame');

            if ($frame && $frame.closest('.queryField_frames').length > 0) {
                $frame.hide();
            }
        });

        $('.queryField_frames > .frame').each(function() {
            var $field = $.data(this, 'query-$field');

            if (inNonInitializedRepeatable($field)) {
                return;
            }

            if (!$field || !$field.is(':visible')) {
                $(this).hide();
            }
        });
    }, 100);
});
