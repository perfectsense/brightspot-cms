/* global alert document require window NodeFilter setInterval */

require([ 'bsp-utils', 'js.cookie', 'jquery', 'iframeResizer' ], function (bsp_utils, Cookies, $) {
    var $document = $(window.document),
            $body = $($document[0].body),
            $parent = $(window.parent),
            $parentDocument = $($parent[0].document),
            $parentBody = $($parentDocument[0].body),
            mainObjectData,
            objectFields = {},
            objectFieldList;

    /**
    * Controler for the inline conentEditable editor, used for "text" fields.
    * The user can directly click on the text and edit it only the page,
    * then the text will be updated by making a service call to the backend.
    *
    * We start by finding a BrightspotCmsObjectBegin comment that specifies a brightspot object.
    * The first element we find after that comment will be considered the wrapping element for the object.
    * 
    * Then we find a BrightspotCmsFieldAccess comment that specifies the field to be edited.
    * We only use this if the comment contains a "text" parameter. For example:
    * <!--BrightspotCmsFieldAccess {"id":"123,"name":"firstname","text":"Patrick"}-->
    *
    * Since these text fields do not have a standardized HTML structure, we just do our best to find a
    * text node that matches the text we are looking for, within the wrapping element for the object.
    * Note the text node might contain extra text that is not actually part of the field, so we need
    * to search for the text as a substring within the text node, and split the text node if necessary.
    *
    * @example
    * var editor = Object.create(contendEditableEditor);
    * editor.init({el:myObjectElement, id:myObjectId, field:myFieldName, text:myText});
    */
    var contentEditor = {
        
        // Key to use to save the text value (in case user decides to cancel the change)
        dataKey: 'contentEditor-value',
        
        // Classname to add to the body when the content editor is showing.
        // This can be used to hide other controls on the page.
        showingClass: 'contentEditor-showing',
        
        controlsClass: 'contentEditor-controls',
        
        eventNamespace: '.contentEditor',
        
        defaults: {},
        
        /**
        * Initialize the contentEditor.
        * @param  {Object} options Set of key/value paris to set up the editor.
        * @param  {Element} options.el
        * The element wrapper for the object that contains the text we will be editing.
        * @param  {String} options.id
        * The id of the object that we will be editing.
        * @param {String} options.field
        * The field name that we will be editing.
        * @return {String} options.text
        * The text value that will be edited.
        */
        init: function(options) {
            var self;
            self = this;
            self.options = $.extend({}, self.defaults, options || {});
            self.initTextNode();
        },
        
        /**
        * Find the text node within the object that matches the text we are looking for.
        * If found, splits the text node if necessary, then wraps it in an element,
        * and returns that element as a jQuery object.
        * If not found returns an empty jQuery object.
        * @param
        * @return {jQueryElemnt}
        */
        initTextNode: function() {
            
            var $wrapper;
            var end;
            var i;
            var node;
            var nodes;
            var self;
            var start;
            var text;
            var value;
            
            self = this;
            value = self.options.text;
            nodes = self.textNodesUnder(self.options.el);
            for (i=0; i < nodes.length; i++) {
                node = nodes[i];
                text = node.nodeValue || '';
                start = text.indexOf(value);
                if (start === -1) {
                    // Did not find the value in this text node go to the next node
                    continue;
                }
                
                // If the value is in the middle of a text node, we might
                // need to split the text node. For example, if looking for FOO and
                // we find "xxxFOOyyy" then split the text node into "xxx", "FOO", and "yyy" text nodes.
                
                // Trim the end of the text node if necessary
                end = start + value.length;
                if (end < text.length) {
                    node.splitText(end);
                }
                
                // Trim the start of the text node if necessary
                if (start > 0) {
                    node = node.splitText(start);
                }
                
                // Create an element to wrap the text node
                // because contentEditable only works for elements
                $wrapper = $('<span>', {
                    'style': 'cursor: cell;'
                }).on('click', function(event){
                    
                    // Prevent the click event from propagating up,
                    // so if the text is in a link for example we don't want
                    // to leave the page
                    event.preventDefault();
                    event.stopPropagation();

                    // Just in case the user is already editing this value and clicks on it again...
                    if (self.isEditable()) {
                        return;
                    }
                    
                    // Check if another items is already being edited
                    if (self.controlsAreShowing()) {
                        alert('Another value is being edited: save or cancel to continue.');
                        return;
                    }
                    
                    self.setEditable();
                    self.focus();
                    
                }).hover(
                    function(event){
                        if (!self.isEditable()) {
                            self.setOutline(true);
                        }
                    },
                    function(event) {
                        if (!self.isEditable()) {
                            self.setOutline(false);
                        }
                    }
                );
                
                $(node).wrap($wrapper);
                
                $wrapper = $(node).parent();
                
                // In case the user turns off the other inline editor controls, stop allowing edits
                $( $wrapper[0].ownerDocument ).on('brightspot-editor-close', function(){
                    self.disable();
                })

                self.$el = $wrapper;

                // Save the default value in the text node in case user starts edting but then decides to cancel
                self.setDefaultValue();
            }
        },
        
        
        /**
         * Make the element editable, or turn off editable.
         * @param  {Boolean} flag
         * Optional flag. Set to true to make the element editable, or false to turn off editable.
         * Defaults to true if not specified.
         */
        setEditable: function(flag) {
            var self;
            self = this;
            
            // Default the flag to true
            flag = (flag === false) ? false : true;
            
            if (flag) {
                self.showControls();
                self.$el.attr('contenteditable', true);
            } else {
                self.hideControls();
                self.$el.removeAttr('contenteditable');
            }
        },


        isEditable: function() {
            var self;
            self = this;
            return self.$el[0].hasAttribute('contenteditable');
        },
        
        
        /**
         * Focus on the element.
         * @return {[type]}
         */
        focus: function() {
            var self;
            self = this;
            self.$el.focus();
        },


        /**
         * Save the current value of the element so it can be restored if user decides to cancel.
         * @return {[type]}
         */
        setDefaultValue: function() {
            var self;
            var value;
            self = this;
            value = self.getValue();
            self.$el.data(self.dataKey, value);
        },

        
        /**
         * Retrieves the value that was saved before the user made chagnes.
         * @return {String}
         * Text value that was saved, or undefined if a value was not previously saved.
         */
        getDefaultValue: function() {
            var self;
            self = this;
            return self.$el.data(self.dataKey);
        },

        
        /**
         * Restore the value that was saved before user started modifying the text.
         */
        resetDefaultValue: function() {
            var self;
            var value;
            self = this;
            value = self.getDefaultValue();
            if (value !== undefined) {
                self.setValue(value);
            }
        },


        /**
         * Get the current value of the text.
         * @return {String}
         */
        getValue: function() {
            var self;
            self = this;
            return self.$el.text();
        },

        
        /**
         * Set the text to a new value.
         * @param {String} value [description]
         */
        setValue: function(value) {
            var self;
            self = this;
            self.$el.text(value);
        },

        
        showControls: function() {
            var self;
            self = this;
            
            // Add a class to the body to indicate the content editor is showing,
            // so other controls can be hidden
            $('body').addClass(self.showingClass);
            
            // Add buttons for save and cancel.
            // Note these appear on the iframe overlay
            self.$controls = $('<div>', {
                'class': self.controlsClass,
                css: {
                    position: 'absolute',
                    'z-index': '999',
                    visibility: 'visible'
                }
            }).appendTo('body');
            self.$controlsSave = $('<button>', {
                'type': 'button',
                text: 'Save',
                on: {
                    click: function(event) {
                        self.hideControls();
                        self.publish();
                    }
                }
            }).appendTo(self.$controls);
            self.$controlsCancel = $('<button>', {
                'type': 'button',
                text: 'Cancel',
                on: {
                    click: function(event) {
                        self.hideControls();
                        self.cancel();
                    }
                }
            }).appendTo(self.$controls);
            
            self.updateControlsPosition();
            self.setOutline(true);
            
            // Listen for scroll changes in the parent window so we can keep the save/cancel button aligned
            $(window.parent).on('scroll' + self.eventNamespace, bsp_utils.throttle(10, function(){
                self.updateControlsPosition();
            }));
        },

        
        hideControls: function() {
            var self;
            self = this;
            
            self.$controls.remove();
            
            // Remove class from the body to indicate the content editor is no longer showing,
            // so other controls can be shown again
            $('body').removeClass(self.showingClass);
            
            self.setOutline(false);
            
            // Cancel the scroll listener
            $(window.parent).off('scroll' + self.eventNamespace);
        },


        controlsAreShowing: function() {
            var self;
            self = this;
            return $('body').hasClass(self.showingClass);
        },

        
        updateControlsPosition: function() {
            // Get position of the element we are editing
            // Move controls above it
            var pos;
            var self;
            self = this;
            pos = self.$el.offset();
            self.$controls.css({
                left: pos.left,
                top: pos.top - 40
            })
        },

        setOutline: function(flag) {
            var self;
            self = this;
            flag = (flag === false) ? false : true;
            if (flag) {
                self.$el.css({outline:'4px solid #54D1F1'});
            } else {
                self.$el.css({outline:'inherit'});                
            }
        },

        
        /**
         * Publish the current value.
         * @return {Promise}
         * Returns a jquery promise so you can determine if the publish was successful.
         */
        publish: function() {
            var self;
            var valueNew;
            var valueOld;

            self = this;
            
            // Turn off contentEditable
            self.setEditable(false);
            
            // Check if the value has actually changed
            valueNew = self.getValue();
            valueOld = self.getDefaultValue();
            if (valueNew !== valueOld) {
                updateService.update(self.options.id, self.options.field, valueNew).done(function(){
                    
                    // Since we successfully updated the value, make this value the new default
                    // in case user edits the value again
                    self.setDefaultValue();
                    
                }).fail(function(){
                    self.setEditable(true);
                    alert('Unable to update the value.');
                });
            }
        },
        
        
        cancel: function() {
            var self;
            self = this;
            self.setEditable(false);
            self.resetDefaultValue();
        },

        
        /**
         * Turn off the content editor for the element.
         */
        disable: function() {
            var self;
            self = this;
            self.$el.removeAttr('style').off('click blur');
        },


        /**
        * Get an array of text nodes that live within a certain element.
        * @param  {Element} el
        * @return {Array} Array of text nodes.
        */
        textNodesUnder: function(el){
            var textNodes;
            var n;
            var walk;
            
            textNodes = [];
            walk = document.createTreeWalker(el, NodeFilter.SHOW_TEXT, null, false);
            while(n = walk.nextNode()) {
                textNodes.push(n);
            }
            return textNodes;
        }
    };
    
    
    // Define an API for updating a single field
    var updateService = {
        
        // The URL of the update service
        // TODO: this is a temporary value until the actual service can be created
        url: (window.CONTEXT_PATH || '') + '/content/editInline',

        /**
         * Function to call the update service
         * @param  {String} id
         * The object id that will be updated.
         * @param  {String} field
         * The name of the field to be updated.
         * @param  {String} value
         * The new text value for the field.
         * 
         * @return {Promise}
         * A promise that can be used to determine if the service call was successful.
         */
        update: function(id, field, value) {
            var data;
            var url;
            
            url = (window.CONTEXT_PATH || '');
            url = url.replace(/\/$/, ''); // remove slash at end of line if necessary
            url += '/content/editInline';

            data = {
                id:id,
                f:field,
                value:value,
                '_csrf':Cookies.get('bsp.inlineCsrf')
            };

            return $.ajax(url, {
                type: 'POST',
                data: data,
                // For cross-domain request include cookies
                xhrFields: {
                    withCredentials: true
                }
            });
        }
    };
    
    
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
            objectFieldList[ fieldData.name ] = fieldData;
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

        href = window.CONTEXT_PATH + '/content/editInline?id=' + objectData.id;
        if (objectFields[id]) {
            
            $.each(objectFields[id], function(fieldName, fieldData){
                
                var textEditor;
                
                href += '&f=' + encodeURIComponent(fieldName);
                
                // Check if this field has a "text" parameter and if so create a contentEditor
                // so the user can do inplace editing
                if (fieldData && fieldData.text) {
                    textEditor = Object.create(contentEditor);
                    textEditor.init({
                        el: $begin[0],
                        id: id,
                        field: fieldName,
                        text: fieldData.text
                    })
                }
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
            self.$iframe.iFrameResize({
                // Prevent cross-domain errors
                checkOrigin:false,
                // Need to use 'lowestElement' method because we sometimes have content
                // that goes outside the body element
                heightCalculationMethod:'lowestElement'
            });
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
            var w;
            w = window.parent;
            if (w) {
                w.location.reload();
            }
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
