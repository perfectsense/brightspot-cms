define([
    'jquery',
    'bsp-utils' ],

function($, bsp_utils) {
    var $win = $(window);

    bsp_utils.onDomInsert(document, '.message', {
        'insert': function(message) {
            var $message = $(message);

            if ($message.text() === '' &&
                    $message.find('[data-dynamic-html], [data-dynamic-text]').length > 0) {
                $message.hide();
            }
        }
    });

    bsp_utils.onDomInsert(document, '.contentForm, .enhancementForm, .standardForm', {
        'insert': function(form) {
            var $form = $(form),
                    updateContentState,
                    updateContentStateThrottled,
                    changed,
                    idleTimeout;

            updateContentState = function(idle, wait) {
                if ($form.find(
                        '.repeatableForm:not(.plugin-repeatable),' +
                        '.repeatableInputs:not(.plugin-repeatable),' +
                        '.repeatableLayout:not(.plugin-repeatable),' +
                        '.repeatableObjectId:not(.plugin-repeatable),' +
                        '.repeatableText:not(.plugin-repeatable)').
                        length > 0) {

                    setTimeout(function() {
                        updateContentState(idle, wait);
                    }, 100);
                    return;
                }

                var action,
                        questionAt,
                        complete,
                        end,
                        $dynamicTexts;

                action = $form.attr('action');
                questionAt = action.indexOf('?');
                end = +new Date() + 1000;
                $dynamicTexts = $form.find(
                        '[data-dynamic-text][data-dynamic-text != ""],' +
                        '[data-dynamic-html][data-dynamic-html != ""],' +
                        '[data-dynamic-placeholder][data-dynamic-placeholder != ""]');

                $dynamicTexts = $dynamicTexts.filter(function() {
                    return $(this).closest('.collapsed').length === 0;
                });

                $.ajax({
                    'type': 'post',
                    'url': CONTEXT_PATH + 'contentState?idle=' + (!!idle) + (questionAt > -1 ? '&' + action.substring(questionAt + 1) : ''),
                    'cache': false,
                    'dataType': 'json',

                    'data': $form.serialize() + $dynamicTexts.map(function() {
                        var $element = $(this);

                        return '&_dti=' + ($element.closest('[data-object-id]').attr('data-object-id') || '') +
                                '&_dtt=' + (($element.attr('data-dynamic-text') ||
                                $element.attr('data-dynamic-html') ||
                                $element.attr('data-dynamic-placeholder') ||
                                '')) +
                                '&_dtf=' + $element.attr('data-dynamic-field-name') || '';
                    }).get().join(''),

                    'success': function(data) {
                        $form.trigger('cms-updateContentState', [ data ]);

                        $dynamicTexts.each(function(index) {
                            var $element = $(this),
                                    text = data._dynamicTexts[index];

                            if (text === null) {
                                return;
                            }

                            $element.closest('.message').toggle(text !== '');

                            if ($element.is('[data-dynamic-text]')) {
                                $element.text(text);

                            } else if ($element.is('[data-dynamic-html]')) {
                                $element.html(text);

                            } else if ($element.is('[data-dynamic-placeholder]')) {
                                $element.prop('placeholder', text);
                            }
                        });

                        $form.resize();
                    },

                    'complete': function() {
                        complete = true;
                    }
                });

                if (wait) {
                    while (!complete) {
                        if (+new Date() > end) {
                            break;
                        }
                    }
                }
            };

            updateContentStateThrottled = $.throttle(200, updateContentState);

            updateContentStateThrottled();

            $form.bind('change input', function() {
                updateContentStateThrottled();

                clearTimeout(idleTimeout);

                changed = true;
                idleTimeout = setTimeout(function() {
                    updateContentStateThrottled(true);
                }, 5000);
            });

            $(window).bind('beforeunload', function() {
                if (changed) {
                    updateContentState(true, true);
                }
            });
        }
    });
});
