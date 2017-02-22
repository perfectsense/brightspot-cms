define([
    'jquery',
    'bsp-utils',
    'spectrum' ],

function($, bsp_utils) {
    function setColorAttribute($replacer, color) {
        var $preview = $replacer.find('.sp-preview');
        var $color = $preview.find('> .ColorPreviewColor');

        if ($color.length === 0) {
            $color = $('<div/>', {
                'class': 'ColorPreviewColor'
            });

            $preview.append($color);
        }

        if (color) {
            $preview.attr('data-color', color);
            $color.show();
            $color.css('background-color', color);

        } else {
            $preview.removeAttr('data-color');
            $color.hide();
        }
    }

    bsp_utils.onDomInsert(document, '.sp-replacer', {
        insert: function (replacer) {
            var $replacer = $(replacer);
            var $input = $replacer.prev(':text.color');

            setColorAttribute($replacer, $input.val());
        }
    });

    bsp_utils.onDomInsert(document, ':text.color', {
        'insert': function(input) {
            var $input = $(input);

            $input.spectrum({
                'allowEmpty': true,
                'cancelText': 'Cancel',
                'chooseText': 'OK',
                'preferredFormat': 'hex6',
                'showAlpha': true,
                'showInitial': false,
                'showInput': true,

                'show': function () {
                    function reflow() {
                        var $input = $(this);
                        var $replacer = $input.next('.sp-replacer');
                        var $container = $input.spectrum('container');
                        var $marker = $container.find('> .ColorDropDownMarker');

                        if ($marker.length === 0) {
                            $marker = $('<div/>', {
                                'class': 'ColorDropDownMarker'
                            });

                            $container.append($marker);
                        }

                        var replacerTop = $replacer.offset().top;
                        var containerTop = $container.offset().top;

                        if (containerTop < replacerTop) {
                            $replacer.addClass('ColorDropDownAbove');
                            $container.addClass('ColorDropDownAbove');
                            $container.css('top', containerTop + 1);

                        } else {
                            $replacer.removeClass('ColorDropDownAbove');
                            $container.removeClass('ColorDropDownAbove');
                            $container.css('top', containerTop - 1);
                        }

                        $marker.css('width', $replacer.outerWidth());
                    }

                    reflow.call(this);
                    $(this).on('reflow.spectrum', reflow);
                },

                'hide': function () {
                    $(this).off('reflow.spectrum');
                },

                'change': function (color) {
                    setColorAttribute($(this).next('.sp-replacer'), color);
                }
            });

            $input.next('.sp-replacer').find('.sp-preview').attr('data-placeholder', $input.attr('placeholder'));
        }
    });
});
