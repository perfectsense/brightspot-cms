/* global require window NodeFilter setInterval document */

require([ 'bsp-utils', 'jquery', 'iframeResizer' ], function (bsp_utils, $) {
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

        href = window.CONTEXT_PATH + '/content/inlineEdit?id=' + objectData.id;
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
    
    
    /**
     * Controler for the inline iframe-based editor.
     * @example
     * var editor = Object.create(iframeEditor);
     * editor.init({url:'/edit?id=123'});
     * editor.open();
     */
    var iframeEditor = {

        /**
         * Default values for the options.
         */
        defaults: {
            positionElement: $('body'),
            url: ''
        },

        /**
         * Initialize the iframe editor.
         * @param  {Object} options
         * @param {Element} options.positionElement
         * The element that provides the position for the iframe.
         * @param {String} options.url
         * The url of the edit page to open in the iframe.
         */
        init: function(options) {
            var self;
            self = this;
            self.options = $.extend({}, self.defaults, options);
        },

        /**
         * Open the iframe and set everything up for editing
         * @return {[type]}
         */
        open: function() {
            var self;
            self = this;
            
            // Create a container for the iframe editor
            self.$container = $('<div/>', {'class':'iframeEdit-container'});
            
            // Create a close button
            self.$closeButton = $('<button>', {
                'type': 'button',
                'class': 'iframeEdit-close',
                html: '<span>Close</span>'
            }).on('click', function(event){
                event.preventDefault();
                self.close();
            }).appendTo(self.$container);
            
            // Create a loading message
            self.$loadingMsg = $('<div>', {
                'class': 'iframeEdit-loading',
                html: '<span>Loading...</span>'
            }).appendTo(self.$container);
            
            // Create the iframe and add a class when it has loaded
            self.$iframe = $('<iframe/>', {
                'class': 'iframeEdit-iframe',
                'src': self.options.url,
                'load': function(event) {
                    
                    /*
                    // Alternative way to determine if the iframe has been published:
                    // just assume that the content has been updated if the iframe loads a second page.
                    if (self.$container.hasClass('loaded')) {
                        // This is the second load of the iframe so we assume the content has been updated
                        self.close();
                        self.reload();
                        return;
                    }
                    */
                   
                    self.$container.addClass('loaded');
                }
            }).appendTo(self.$container);
            
            self.$container.appendTo('body');

            // Move the iframe just below the link that launched it
            self.position();
            
            // Listen for a postMessage event that tells us the data was updated in the CMS
            self.listenerOpen();

            // Set up the iframe to resize automatically.
            self.$iframe.iFrameResize();
        },


        /**
         * Listen for the "brightspot-update" message that will be sent from the embedded iframe.
         * Because the page we are editing might be on a different domain from the CMS, the browser's
         * same-origin restrictions might prevent the CMS page from directly notifying the page.
         * So instead the CMS page can notify via postMessage and we can listen for that message.
         * @return {[type]}
         */
        listenerOpen: function() {
            var self;
            self = this;
            
            // Prevent multiple listeners from running
            self.listenerClose();
            
            $(window).on('message', function(event) {
                var data;

                // Note: normally using postMessage you would want to check the origin of the message
                // for security; however in this case, the message doesn't really make any changes
                // other than reloading the page so we probably don't need to check.
                // if (event.origin !== "http://example.com:8080") {
                //     return;
                // }
                 
                // Get the data that was sent in the message
                data = event.originalEvent.data;
                
                // Pass the data to our update handler
                self.listenerHandleMessage(data);
            });
        },
        
        
        /**
         * Remove the update event listener.
         */
        listenerClose: function() {
            $(window).off('message');
        },


        /**
         * Check a message received from the iframe and act on it.
         * @param  {String} data
         * Data from the postMessage event.
         */
        listenerHandleMessage: function(data) {
            var self;
            self = this;
            // If we receive 'brightspot-updated' message, close the iframe and reload the page
            // so we can display the changed content.
            if (/^brightspot-updated$/.test(data)) {
                self.close();
                self.reload();
            }
        },
        
        
        /**
         * Example of how to post the update message.
         * This would normally be done on the CMS edit page after the form has posted and updated
         * the content, but is provided here as an example.
         */
        listenerSendMessage: function() {
            var w;
            
            // Since we're in an iframe, get the parent window (or do nothing)
            w = window.parent;
            if (w) {
            
                // Send a message to the parent window.
                // We're using '*' as the targetOrigin because security is not an issue for this message.
                w.postMessage('brightspot-updated', '*');
            }
        },


        /**
         * Position the iframe near the link that triggered it.
         * This depends on the options.positionElement being set.
         */
        position: function() {
            var self;
            var $el;
            var pos;
            self = this;
            
            // Get the position element
            if (!self.options.positionElement) { return; }
            $el = $(self.options.positionElement);
            if (!$el.length) {
                return;
            }
            
            pos = $el.offset();
            self.$container.css('top', pos.top)
        },


        /**
         * Close the iframe editor.
         */
        close: function() {
            var self;
            self = this;
            self.$container.remove();
            self.listenerClose();
        },
        
        
        /**
         * Reload the page. We'll do this after the user publishes a change
         * in the iframe editor.
         */
        reload: function() {
            // Reload the page without using the browser cache
            document.location.reload(true);
        }
    };
    
    // Intercept edit links so when they are clicked we use iframe editor instead of going to the url
    $('body').on('click', 'a.icon-action-edit', function(event) {
        var editor;
        var link;
        
        // Do not follow the link since we will open an iframe instead
        event.preventDefault();
        
        // Get the link that was clicked
        link = this;
        
        // Create the iframeEditor
        editor = Object.create(iframeEditor);
        editor.init({
            positionElement: $(link).closest('.InlineEditorControls'),
            url: link.href
        });
        editor.open();
    });
});
