/* global require window NodeFilter setInterval */

require([ 'bsp-utils', 'jquery' ], function (bsp_utils, $) {
    var $document = $(window.document),
            $body = $($document[0].body),
            $parent = $(window.parent),
            $parentDocument = $($parent[0].document),
            $parentBody = $($parentDocument[0].body),
            mainObjectData,
            objectFields = {},
            objectFieldList;

    // Find all objects in the parent document.
    var MAIN_OBJECT_PREFIX = 'BrightspotCmsMainObject ';
    var OBJECT_BEGIN_PREFIX = 'BrightspotCmsObjectBegin ';
    var OBJECT_END_PREFIX = 'BrightspotCmsObjectEnd';
    var FIELD_PREFIX = 'BrightspotCmsFieldAccess ';
    var parentCommentWalker = $parentDocument[0].createTreeWalker($parentBody[0], NodeFilter.SHOW_COMMENT, null, null);
    
    while (parentCommentWalker.nextNode()) {
        
        var comment = parentCommentWalker.currentNode;
        var commentValue = comment.nodeValue;
        var objectData = '';
        var $objectElement;
        var fieldData;

        if (commentValue.indexOf(MAIN_OBJECT_PREFIX) === 0) {
            mainObjectData = $.parseJSON(commentValue.substring(MAIN_OBJECT_PREFIX.length));

        } else if (commentValue.indexOf(OBJECT_BEGIN_PREFIX) === 0) {
            
            // Object begin comment looks like this:
            // <!--BrightspotCmsObjectBegin {"typeLabel":"Story","id":"00000157-affa-dfd2-a15f-efffd89a0000","label":"Testing Headline"}-->
            
            // Get the JSON data part of the comment but leave it as a string
            objectData = commentValue.substring(OBJECT_BEGIN_PREFIX.length);
            
            // Get the next element after the comment
            $objectElement = $(comment.nextElementSibling);
            
            // Save the object data in an attribue on the object element
            $objectElement.attr('data-brightspot-cms-object', objectData);
                        
        } else if (commentValue.indexOf(OBJECT_END_PREFIX) === 0) {
            // Object end comment looks like this:
            // <!--BrightspotCmsObjectEnd-->
            
        } else if (commentValue.indexOf(FIELD_PREFIX) === 0) {
            // Field comment looks like this:
            // <!--BrightspotCmsFieldAccess {"id":"00000157-affa-dfd2-a15f-efffd89a0000","name":"paths"}-->
            
            // Get the JSON data from the comment
            fieldData = $.parseJSON(commentValue.substring(FIELD_PREFIX.length));

            // Add this field to the list of fields for an object id. For example:
            // objectFields['123'] = {"firstname":true,"lastname":true};
            objectFieldList = objectFields[ fieldData.id ] || {};
            objectFieldList[ fieldData.name ] = true;
            objectFields[ fieldData.id ] = objectFieldList;
        }
    }

    var ids = [ mainObjectData.id ];

    // Create controls for all the objects in the parent document.
    $parentBody.find('[data-brightspot-cms-object]').each(function() {
        var $begin = $(this),
                objectData = $.parseJSON($begin.attr('data-brightspot-cms-object')),
                id = objectData.id,
                $outline,
                $edit,
                $controls,
                href;

        if ($.inArray(id, ids) > -1) {
            return;
        }

        ids.push(id);
        
        $outline = $('<div/>', {
            'class': 'InlineEditorOutline'
        });

        href = window.CONTEXT_PATH + '/content/edit.jsp?id=' + objectData.id;
        if (objectFields[id]) {
            $.each(objectFields[id], function(fieldName){
                href += '&f=' + encodeURIComponent(fieldName);
            });
        }
        
        $edit = $('<a/>', {
            'class': 'icon icon-action-edit',
            'href': href,
            'target': '_blank',
            'text': objectData.typeLabel,

            'mouseenter': function() {
                var box = $.data($begin[0], 'InlineEditor-box');

                $controls.addClass('InlineEditorControls-hover');

                // Fade all the controls that overlap with this one.
                $parentBody.find('.BrightspotCmsObject').each(function() {
                    var previousBox = $.data(this, 'InlineEditor-box');

                    if (previousBox &&
                            previousBox !== box &&
                            previousBox.controlsCss.left <= box.controlsCss.left + box.outlineCss.width &&
                            box.controlsCss.left <= previousBox.controlsCss.left + previousBox.controlsDimension.width &&
                            previousBox.controlsCss.top + previousBox.outlineCss.top <= box.controlsCss.top + box.outlineCss.top + box.outlineCss.height &&
                            box.controlsCss.top + box.outlineCss.top <= previousBox.controlsCss.top + previousBox.outlineCss.top + previousBox.controlsDimension.height) {
                        previousBox.$controls.addClass('InlineEditorControls-under');
                    }
                });
            },

            'mouseleave': function() {
                $controls.removeClass('InlineEditorControls-hover');
                $body.find('.InlineEditorControls').removeClass('InlineEditorControls-under');
            }
        });

        $controls = $('<ul/>', {
            'class': 'InlineEditorControls',
            'html': $('<li/>', {
                'html': [ $outline, $edit ]
            })
        });

        $.data(this, 'InlineEditor-$controls', $controls);
        $body.append($controls);
        $begin.addClass('BrightspotCmsObject');
    });

    var positionControls = bsp_utils.throttle(1, function () {
        var previousBoxes = [ ];

        $parentBody.find('.BrightspotCmsObject').each(function() {
            var $begin = $(this);
            var beginOffset = $begin.offset();
            var minX = beginOffset.left;
            var maxX = minX + $begin.outerWidth();
            var minY = beginOffset.top;
            var maxY = minY + $begin.outerHeight();

            if (minY >= 0 && minY < 37) {
                minY = 37;
            }

            var $controls = $.data(this, 'InlineEditor-$controls');

            if (!$controls) {
                return;
            }

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

            $.data($begin[0], 'InlineEditor-box', box);

            // Move the controls down until they don't overlay with
            // any other controls.
            if (previousBoxes.length > 0) {
                var retry;
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
            $controls.find('.InlineEditorOutline').css(box.outlineCss);
            $controls.show();
        });
    });

    positionControls();
    setInterval(positionControls, 1000 / 60);
    $parent.scroll(positionControls);
});
