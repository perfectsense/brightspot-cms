(function($, window, undefined) {
    var $document = $(window.document),
            $body = $($document[0].body),
            $parent = $(window.parent),
            $parentDocument = $($parent[0].document),
            $parentBody = $($parentDocument[0].body),
            $editor = $parentBody.find('.cms-inlineEditor'),
            mainObjectData = $.parseJSON($parentBody.find('.cms-mainObject').attr('data-object')),
            ids = [ mainObjectData.id ];

    // Find all objects in the parent document.
    var OBJECT_BEGIN_PREFIX = 'brightspot.object-begin ';
    var parentCommentWalker = $parentDocument[0].createTreeWalker($parentBody[0], NodeFilter.SHOW_COMMENT, null, null);

    while (parentCommentWalker.nextNode()) {
        var comment = parentCommentWalker.currentNode;
        var commentValue = comment.nodeValue;

        if (commentValue.indexOf(OBJECT_BEGIN_PREFIX) !== 0) {
            continue;
        }

        $(comment.nextElementSibling).attr(
                'data-cms-object',
                commentValue.substring(OBJECT_BEGIN_PREFIX.length));
    }

    // Create controls for all the objects in the parent document.
    $parentBody.find('[data-cms-object]').each(function() {
        var $begin = $(this),
                objectData = $.parseJSON($begin.attr('data-cms-object')),
                id = objectData.id,
                $outline,
                $edit,
                $controls;

        if ($.inArray(id, ids) > -1) {
            return;
        }

        ids.push(id);

        $outline = $('<div/>', {
            'class': 'inlineEditorOutline'
        });

        $edit = $('<a/>', {
            'class': 'icon icon-action-edit',
            'href': CONTEXT_PATH + '/content/edit.jsp?id=' + objectData.id,
            'target': '_blank',
            'text': objectData.typeLabel,

            'mouseenter': function() {
                var box = $.data($begin[0], 'inlineEditor-box');

                $controls.addClass('inlineEditorControls-hover');

                // Fade all the controls that overlap with this one.
                $parentBody.find('.cms-object-hasControls').each(function() {
                    var previousBox = $.data(this, 'inlineEditor-box');

                    if (previousBox &&
                            previousBox !== box &&
                            previousBox.controlsCss.left <= box.controlsCss.left + box.outlineCss.width &&
                            box.controlsCss.left <= previousBox.controlsCss.left + previousBox.controlsDimension.width &&
                            previousBox.controlsCss.top + previousBox.outlineCss.top <= box.controlsCss.top + box.outlineCss.top + box.outlineCss.height &&
                            box.controlsCss.top + box.outlineCss.top <= previousBox.controlsCss.top + previousBox.outlineCss.top + previousBox.controlsDimension.height) {
                        previousBox.$controls.addClass('inlineEditorControls-under');
                    }
                });
            },

            'mouseleave': function() {
                $controls.removeClass('inlineEditorControls-hover');
                $body.find('.inlineEditorControls').removeClass('inlineEditorControls-under');
            }
        });

        $controls = $('<ul/>', {
            'class': 'inlineEditorControls',
            'html': $('<li/>', {
                'html': [ $outline, $edit ]
            })
        });

        $.data(this, 'inlineEditor-$controls', $controls);
        $body.append($controls);
        $begin.addClass('cms-object-hasControls');
    });

    function positionControls() {
        var previousBoxes = [ ];

        $parentBody.find('.cms-object-hasControls').each(function() {
            var $begin = $(this);
            var beginOffset = $begin.offset();
            var minX = beginOffset.left;
            var maxX = minX + $begin.outerWidth();
            var minY = beginOffset.top;
            var maxY = $begin.outerHeight();

            if (minY < 37) {
                minY = 37;
            }

            var $controls = $.data(this, 'inlineEditor-$controls');
            var box = {
                '$controls': $controls,
                'controlsCss': {
                    'left': minX,
                    'top': minY
                },
                'controlsDimension': {
                    'height': $controls.outerHeight() + 5,
                    'width': $controls.outerWidth()
                },
                'outlineCss': {
                    'height': maxY - minY,
                    'top': 0,
                    'width': maxX - minX
                }
            };

            $.data($begin[0], 'inlineEditor-box', box);

            // Move the controls down until they don't overlay with
            // any other controls.
            if (previousBoxes.length > 0) {
                do {
                    retry = false;

                    $.each(previousBoxes, function(i, previousBox) {
                        if (previousBox.controlsCss.left <= box.controlsCss.left + box.controlsDimension.width &&
                                box.controlsCss.left <= previousBox.controlsCss.left + previousBox.controlsDimension.width &&
                                previousBox.controlsCss.top <= box.controlsCss.top + box.controlsDimension.height &&
                                box.controlsCss.top <= previousBox.controlsCss.top + previousBox.controlsDimension.height) {
                            retry = true;

                            box.controlsCss.top += 1;
                            box.outlineCss.top -= 1;
                            return false;
                        }
                    });
                } while (retry);
            }

            previousBoxes.push(box);
            $controls.css(box.controlsCss);
            $controls.find('.inlineEditorOutline').css(box.outlineCss);
            $controls.show();
        });
    }

    positionControls();
    setInterval(positionControls, 2000);

    // Enable "click-through" editor IFRAME.
    $parentDocument.on('mousemove', function(event) {
        if ($($document[0].elementFromPoint(event.pageX, event.pageY)).closest('.inlineEditorControls').length > 0) {
            $editor.css('pointer-events', 'auto');
        }
    });

    $document.on('mousemove', function(event) {
        if ($body.find('.popup:visible').length === 0 &&
                $($document[0].elementFromPoint(event.pageX, event.pageY)).closest('.inlineEditorControls').length === 0) {
            $editor.css('pointer-events', 'none');
        }
    });

    $document.on('click', 'a[target]', function() {
        $editor.css('pointer-events', 'auto');
    });

    // Collapse the editor on right click because Chrome activates it on the
    // editor even with pointer-events: none.
    $document.on('contextmenu', function(event) {
        var $logo = $body.find('.inlineEditorLogo');

        $editor.css({
            'max-height': $logo.outerHeight(true),
            'max-width': $logo.outerWidth(true)
        });

        $logo.find('a').one('click', function() {
            $editor.css({
                'max-height': '',
                'max-width': ''
            });

            return false;
        });
    });

    // Make sure that the editor IFRAME is at least as high as the parent
    // document.
    function equalizeHeight() {
        $editor.height(Math.max($document.height(), $parentBody.height()));
        $editor.css({
            'border': 'none',
            'left': 0,
            'margin': 0,
            'position': 'absolute',
            'top': 0,
            'width': '100%',
            'z-index': 1000000
        });
    }

    equalizeHeight();
    setInterval(equalizeHeight, 100);

    // Make sure that the main object controls are fixed at the top.
    $parent.scroll(function() {
        $('.inlineEditorControls-main').css('top', $parent.scrollTop());
    });
})(jQuery, window);
