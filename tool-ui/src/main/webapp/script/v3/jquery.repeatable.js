/***
Repeatable Inputs
-----------------

For the purposes of this code, there are three types of repeatables:

* object = an input that can be repeated (but not collapsed/uncollapsed)

* form = multiple inputs can be repeated (and collapsed/uncollapsed)

* preview = a slideshow, which will have grid view and gallery view

* single = A single input is used instead of an add more button.
  This is used only when the addButtonText option is set to an empty string
  and there is a single template and a single input
  If user presses enter on the input, then a new input is created so the user can add more keywords.

The types have the following things in common:

* They can have a template (or multiple templates) to add new items.
  An "add item" button is included for each template.

* A remove button is added next to each item item (or in the case of single input,
  the remove button might be styled to overlay the input).


How removing / restoring works
------------------------------

When user clicks the "remove" button, the item is not actually removed from the page,
instead a "toBeRemoved" class is added so the item can be styled to indicate it will be removed,
the inputs are all set to be disabled so they will not be sent to the backend, and the remove
button is changed to a restore button.

When user clicks the "restore" button, the "toBeRemoved" class is removed, the inputs
are no longer disabled, and the restore button is changed to a remove button.

HTML for repeatables
--------------------

The HTML within the repeatable element must conform to these standards:

* It must contain a list (UL or OL) where each item within the list is
  a repeatable input

* Within the list there should be a template. The template can be one of the list items
  with classname li.template or a script element with type="text/template".

==================================================
***/


(function($, win, undef) {

    var $win = $(win);

    // Counter to use to make sure the browser doesn't cache ajax requests
    var cacheNonce = 0;

    // Defaults to use for repeatables
    var repeatableDefaults = {
        addButtonText: 'Add',
        removeButtonText: 'Remove',
        restoreButtonText: 'Restore',
        sortableOptions: {
            delay: 300
        }
    };
    
    require(['v3/input/carousel'], function(carouselUtility) {

        // Create the jquery plugin .repeatable() (using plugin2)
        $.plugin2('repeatable', {

            /**
             * Default options used by plugin2
             */
            _defaultOptions: repeatableDefaults,

            
            /**
             * Function called by plugin2 when a new element is added to the page.
             *
             * ??? - it appears that a second parameter "options" is supported here,
             * but we don't seem to be using that
             *
             * @param {Element} container
             * The repeatable element that was added to the page.
             */
            _create: function(container) {
                var plugin = this;
                var $container = $(container);
                var options = plugin.option();
                var repeatable;
                
                // Create a copy of the repeatableUtility object
                // This uses prototypal inheritance so we're not actually copying the entire object
                // It allows each repeatable instance to have its own object that saves state
                repeatable = Object.create(repeatableUtility);

                // Initialize the repeatable utility to get things started
                repeatable.init($container, options);
            },


            /**
             * Function to add a new item after bulk file upload.
             * This is called by by some code delivered by the back-end.
             *
             * @param {Function} callback
             * Function to call after the item has been added.
             * Refer to repeatableUtility.addItem
             */
            'add': function(callback) {

                var plugin = this;

                // In this case, the plugin is called on the add button
                var $caller = plugin.$caller;

                // Not a great way to do this, but leaving this for now.
                // The callback function modifies the template to insert actual values,
                // then triggers a change event
                $caller.closest('.addButton').trigger('click', [ callback ]);
                
                return $caller;
            }
        });

        
        /**
         * Object to initialize and control repeatable inputs.
         *
         * This object should not be used directly, instead create a new object for each
         * repeatable using Object.create and prototypal inheritance.
         * After creating the new object, call the .init() function.
         *
         * @example
         * myRepeatable = Object.create(repeatableUtility);
         * myRepeatable.init(element, options);
         */
        var repeatableUtility = {

            /**
             * Default options
             */
            defaults: repeatableDefaults,


            /**
             */
            templateEdit:
            '<div class="carousel-target">' +
                '<div class="carousel-target-nav carousel-target-prev"><a href="#">Previous</a></div>' +
                '<div class="carousel-target-items"></div>' +
                '<div class="carousel-target-nav carousel-target-next"><a href="#">Next</a></div>' +
            '</div>',

            
            /**
             * Initialize the object and set up the user interface.
             *
             * @param {Element|jQuery object|jQuery selector} element
             * The element that contains the repeatable HTML.
             *
             * @param {Object} [options]
             */
            init: function(element, options) {
                
                var self = this;

                // Save the element for later use
                self.$element = $(element).first();

                // Set up a place to save other dom elements for later use
                self.dom = {};
                
                // Override the default options
                self.options = $.extend(true, {}, self.defaults, options);
                
                // Get the various things we will need from the DOM
                self.initDOM();

                // Set up the event-input-disable functionality
                self.initInputDisable();
                
                // If the list needs to be sortable make it so
                self.initSortable();

                // Determine which type of repeatable we are dealing with
                self.initMode();

                // Intialize a carousel if we need it (for mode=preview)
                self.modePreviewInit();

                // For each item initialize it
                self.dom.$list.find('> li').each(function(){
                    self.initItem(this);
                });

                // After we're done initilizing all the items,
                // update the carousel if ncessary
                self.modePreviewUpdateCarousel();

                // Create the "Add Item" button(s)
                self.initAddButton();
            },

            
            /**
             * Get various things we will need from the DOM and save for later.
             */
            initDOM: function() {

                var self = this;
                var $list;
                var $templates = $();

                // Find the list of inputs (must be in a UL or OL)
                $list = self.$element.find('> ul, > ol').first();
                self.dom.$list = $list; // save for later use
                
                // Templates can be in an LI or a SCRIPT element
                $list.find('> li.template, > script[type="text/template"]').each(function() {

                    var $template = $(this);

                    if ($template.is('li.template')) {
                        $templates = $templates.add($template);
                    } else {
                        $templates = $templates.add($($template.text()));
                    }

                    // Remove the template from the page
                    $template.remove();
                });

                // Save templates for later use
                self.dom.$templates = $templates;
                
            },

            
            /**
             * Set up the event-input-disable functionality.
             * ??? need more info on how this works
             */
            initInputDisable: function() {

                var self = this;
                
                // Mark the input as something that can be locked or disabled
                self.$element.addClass('event-input-disable');

                // If the "input-disable" event is triggered, then add or remove a class
                self.$element.bind('input-disable', function(event, disable) {
                    $(event.target).closest('.inputContainer').toggleClass('state-disabled', disable);
                });
            },


            /**
             * Make the list of items sortable.
             * Only applies if the list is an ordered list (OL element).
             */
            initSortable: function() {
                
                var self = this;
                
                // Check if the list is an OL and if so make it sortable
                if (self.dom.$list.is('ol')) {
                    
                    self.dom.$list.sortable(self.options.sortableOptions);
                    
                    // Note: there is a 'sortable.end' event triggered after an item is dragged and dropped,
                    // which will be used later to adjust the carousel
                   
                }
            },

            
            /**
             * Determine which type of repeatable we are dealing with:
             * Sets the "mode" property to one of the following:
             * form, single, preview
             *
             * Note that initDOM() must be called before this is called.
             *
             * Also see the following functions:
             * modeIsPreview(), modeIsSingle(), ...
             */
            initMode: function() {
                
                var self = this;
                var $templateInputs;
                
                self.mode = 'form';

                // Set to object mode if we are in a repeatableObjectId
                if (self.$element.hasClass('repeatableObjectId')) {
                    self.mode = 'object';
                }
                
                // Set to single input mode if there is no add button text,
                // and if there is a single template with a single input
                $templateInputs = self.dom.$templates.find(':input').not(':input[name$=".toggle"]');
                if (!self.options.addButtonText &&
                    self.dom.$templates.length === 1 &&
                    $templateInputs.length === 1) {
                    
                    self.mode = 'single';
                    return;
                }

                // Set to preview mode
                if (self.$element.hasClass('repeatableForm-previewable')) {
                    self.mode = 'preview';
                    return;
                }
            },


            /**
             * Initialize the "Add Item" controls.
             *
             * There is a single container for the add item controls,
             * and within it can be mutiple add buttons (one for each template).
             *
             * The text of the add button is defined by a data attribute on the template
             * and the addButtonText options.
             */
            initAddButton: function() {

                var self = this;
                var $addButtonContainer;
                
                // Create the Add Item container
                //   -- Initialize single input mode if necessary
                //   -- Create an add button for each template
                //   -- Set up click event on the add button

                // Create a container where all the "Add Item" buttons will be displayed
                // First check to see if there is already a div to place them,
                // if not create a new div at the bottom.
                $addButtonContainer = self.$element.find('.addButtonContainer');
                if (!$addButtonContainer.length) {
                    $addButtonContainer = $('<div/>', { 'class': 'addButtonContainer' }).appendTo(self.$element);
                }

                // Save the add button container for later use
                // (in single mode we will add more to it)
                self.dom.$addButtonContainer = $addButtonContainer;
                
                // If we are in single input mode then we'll do some extra stuff
                self.modeSingleInitAddButton();

                // Create an "Add Item" link for each template we found
                var idIndex = 0;
                self.dom.$templates.each(function() {
                    
                    var $template = $(this);
                    var itemType = $template.attr('data-type') || 'Item';
                    var addButtonText;

                    // Determine which text to use for the add button
                    addButtonText = itemType;
                    if (self.options.addButtonText) {
                        addButtonText = self.options.addButtonText + ' ' + addButtonText;
                    }
                    
                    // Add an element for the "Add Button" control
                    $('<span/>', {
                        
                        'class': 'addButton',
                        text: addButtonText,
                        
                        // Save the template on the add button control so when user
                        // clicks it we will know which template to add
                        // 'data-addButtonTemplate': $template
                        
                    }).on('click', function(event, customCallback) {

                        // The click event for the add button supports an
                        // optional callback function that will be called after
                        // the new item is added.

                        // Add the new item based on this template
                        self.addItem($template, customCallback);

                        return false;
                        
                    }).appendTo($addButtonContainer);
                });
            },

            
            /**
             * Initialize an item. This should be called for each list item on the page,
             * plus it should be called for any new item added to the page.
             *
             * @param {Element|jQuery object|jQuery selector} element
             * The LI element that contains the item HTML.
             */
            initItem: function(element) {
                
                var self = this;
                var $item = $(element);

                // Create an image for the item if necessary,
                // and set up other stuff for mode=preview
                self.modePreviewInitItem($item);
                
                // Create a the label for the item if necessary.
                // The label acts as a toggle to show and hide the item,
                // plus it can load the content of the item from a URL
                self.initItemLabel($item);

                // Collapse the item unless it has an error message within it
                // Do not collapse "preview" or "object" mode
                if ($item.find('.message-error').length === 0
                    && $item.find('> .layouts').length === 0
                    && !self.modeIsPreview()
                    && !self.modeIsObject()) {
                    self.itemCollapse($item);
                }

                // An input with ".toggle" in the name will be checked
                // to tell the backend that it should be saved, but the checkbox will not actually
                // be shown to the user, so we hid it here
                $item.find(':input[name$=".toggle"]').hide();

                // Add the remove control to the item
                self.initItemRemove($item);
            },


            /**
             * Initialize the label for an item.
             *
             * @param {Element|jQuery object} element
             * The item (LI element).
             */
            initItemLabel: function(element) {
                
                var self = this;
                var $item = $(element);
                var type = $item.attr('data-type');
                var label = $item.attr('data-label');
                var $label;
                var $existingLabel;
                var labelText;

                // Do not add a label if  there is no data-type attribute
                if (!type) {
                    return;
                }

                // Do not add a label if this is a mode=preview
                if (self.modeIsPreview()) {
                    return;
                }
                
                // The text for the label will be the data type such as "Slideshow Slide"
                // And if a data-label attribute was provided append it after a colon such as "Slideshow Slide: My Slide"
                labelText = type;
                if (label) {
                    labelText += ': ' + label;
                }

                $label = $('<div/>', {
                    'class': 'repeatableLabel',
                    text: labelText,
                    
                    // Set up some parameters so the label text will dynamically update based on the input field
                    'data-object-id': $item.find('> input[type="hidden"][name$=".id"]').val(),
                    'data-dynamic-text': '${content.state.getType().label}: ${content.label}'
                    
                }).on('click', function() {
                    self.itemToggle($item);
                });

                // If there was already a label on the page remove the repeatableLabel class
                // so it doesn't conflict with the new label we are creating,
                // then add this existing label to the new label we are creating.
                $existingLabel = $item.find(" > .repeatableLabel");
                if ($existingLabel.length) {
                    
                    $existingLabel.removeClass('repeatableLabel').appendTo($label);

                    // Just in case the label has any inputs, make sure clicks on the input do not bubble up
                    // to the label and make it toggle the item
                    $label.find(':input').click(function(e) {
                        e.stopPropagation();
                    });
                }

                // Add our label to item at the top
                $item.prepend($label);
            },


            /**
             * Initialize the remove button for an individiual item.
             *
             * @param {Element|jQuery object} item
             * The item (LI element).
             */
            initItemRemove: function(item) {
                
                var self = this;
                var $item = $(item);
                
                // Add the remove button to the item
                $('<span/>', {
                    'class': 'removeButton',
                    'text': self.options.removeButtonText
                }).on('click', function(){
                    self.removeItemToggle( $item );
                }).appendTo($item);

            },

            
            /**
             * Add a new item to the list, based from a template.
             *
             * @param {Element|jQuery object} template
             * The template for the new item.
             *
             * @param {Function} customCallback
             * A function to call after the new item has been added.
             */
            addItem: function(template, customCallback) {
                
                var self = this;
                var $template = $(template);
                var $addedItem;
                var promise;
                var templateUrl;
                var $templateInputs;
                
                // For single input mode, don't add another item if the input is empty
                if (self.modeIsSingle() && !self.dom.$singleInput.val()) {
                    return false;
                }

                // ???
                self.$element.find(".objectId-placeholder").hide();

                // Create a copy of the template to use as the new item
                $addedItem = $template.clone();
                $addedItem.removeClass('template');

                // If the template contains inputs that end with ".toggle" then
                // start by making them checked to tell the backend it should save this item
                $addedItem.find(':input[name$=".toggle"]').attr('checked', 'checked');

                // Determine if we need to AJAX the template onto the page
                // (if the template has no input but has a link)
                templateUrl = $addedItem.find('a').attr('href');
                $templateInputs = $addedItem.find(':input');
                if (templateUrl && $templateInputs.length === 0) {

                    // Not sure why, but sending some kind of cache breaker in the data?
                    ++ cacheNonce;

                    promise = $.ajax({
                        'cache': false,
                        'url': templateUrl,
                        'data': { '_nonce': cacheNonce }
                    }).always(function(response) {
                        
                        // The response will either be the data (on success), or a jqXHR object (on error)
                        var content = typeof(response) === 'string' ? response : response.responseText;

                        // Add the loaded content into the new item
                        $addedItem.html(content);
                    });
                    
                } else {
                    // We're not loading any data, so we'll create an already
                    // resolved promise to the next step will complete immediately
                    promise = $.Deferred().resolve().promise();
                }
                
                // Run more code after the item has been loaded
                promise.always(function() {

                    // Add the item to the page
                    self.dom.$list.append($addedItem);
                    
                    // Initialize stuff on the new item
                    // If there was a custom callback provided, call it now.
                    // This is used for example, in Image Upload.
                    // After the image is uploaded a new repeatable item is added,
                    // then the callback inserts values into the template.
                    if (customCallback) {
                        
                        // Call the customcallback funtion, setting "this" to be the element for the new item
                        customCallback.call( $addedItem[0] );
                    }

                    // Note this will collapse the item as well so we will uncollapse it below
                    self.initItem($addedItem);
                    
                    // Make sure it's not collapsed since it's a new item
                    self.itemUncollapse($addedItem);

                    // For single input mode, when user enters a value into the input,
                    // we copy that value into the added item, then clear the value
                    // from the original input so the user can enter another value
                    if (self.modeIsSingle()) {
                        $addedItem.find(':input:not([name$=".toggle"])').val( self.dom.$singleInput.val() );
                        self.dom.$singleInput.val('');
                    }

                    // Adjust the element ids so they don't conflict
                    $addedItem.find('*[id]').attr('id', function(index, attr) {

                        var newAttr;
                        
                        self.idIndex += 1;
                        
                        newAttr = attr + 'r' + self.idIndex;

                        // Also adjust the LABEL elements that are linked to other inputs
                        $addedItem.find('*[for=' + attr + ']').attr('for', newAttr);

                        // Also adjust the auto-show elements so they show the correct elements
                        $addedItem.find('*[data-show=#' + attr + ']').attr('data-show', '#'+newAttr);
                        
                        return newAttr;
                    });

                    // Trigger a custom "create" event for the item
                    // So other code can act on this if necessary
                    $addedItem.trigger('create');

                    // If we are in mode preview, adjust the added item.
                    // Add a preview image, and move the form to the carousel.
                    self.modePreviewAddItem( $addedItem );
                    
                    $addedItem.trigger('change');
                        
                    // Trigger a resize event for the window
                    // Since we have added new content
                    $win.resize();
                    

                }); // END promise.always()

                // Return the promise just in case someone wants to do something after the item is added
                // (but doesn't want to use the callback function)
                return promise;
            },


            /**
             * Shortcut function to tell if the mode is 'form'
             * @returns {Boolean}
             */
            modeIsObject: function() {
                var self = this;
                return self.mode === 'object';
            },

            
            /**
             * Shortcut function to tell if the mode is 'form'
             * @returns {Boolean}
             */
            modeIsForm: function() {
                var self = this;
                return self.mode === 'form';
            },

            
            /**
             * Shortcut function to tell if the mode is 'single'
             * @returns {Boolean}
             */
            modeIsSingle: function() {
                var self = this;
                return self.mode === 'single';
            },

            
            /**
             * Shortcut function to tell if the mode is 'preview'
             * @returns {Boolean}
             */
            modeIsPreview: function() {
                var self = this;
                return self.mode === 'preview';
            },


            /**
             * Toggle the display of an item (and load the item if necessary).
             * If the item contains a hidden input with data-form-fields-url attribute,
             * then that means it has not been loaded yet.
             * In that case dynamically load the item the first time the item is toggled.
             *
             * @param {Element|jQuery object} item
             * The list item that should be toggled.
             *
             * @param {Boolean} [collapseFlag]
             * Set to true to force the item to collapse.
             * Set to false to force the item to uncollapse.
             * If undefined then the item will toggle between collapsed and uncollapsed.
             */
            itemToggle: function(item, collapseFlag) {
                
                var self = this;
                var $item = $(item);
                var deferred;

                // Collapse or uncollapse the item
                $item.toggleClass('collapsed', collapseFlag);
                
                // Don't do anything if mode=preview
                if (self.modeIsPreview()) {
                    return;
                }
                
                // Load the item if necessary,
                // or if it's already loaded do some stuff immediately
                if (!self.itemIsCollapsed($item)) {
                    
                    self.itemLoad($item).always(function(){
                    
                        // Trigger the resize event since we changed the item size
                        $item.resize();

                        // Trigger a change event on the first input within the item
                        // so other code can do something if necessary
                        $item.find(':input:first').change();
                    });
                }
            },


            /**
             * Collapse an item.
             *
             * @param {Element|jQuery object} item
             * The list item that should be collapsed.
             */
            itemCollapse: function(item) {
                var self = this;
                self.itemToggle(item, true);
            },

            
            /**
             * Uncollapse an item (and load the item if necessary).
             *
             * @param {Element|jQuery object} item
             * The list item that should be uncollapsed.
             */
            itemUncollapse: function(item) {
                var self = this;
                self.itemToggle(item, false);
            },

            
            /**
             * Determine if an item is currently collapsed.
             *
             * @param {Element|jQuery object} item
             * The list item to test.
             */
            itemIsCollapsed: function(item) {
                var self = this;
                var $item = $(item);
                return $item.hasClass('collapsed');
            },

            
            /**
             * Checks an item to determine if it needs to load some dynamic content,
             * and loads it if necessary.
             *
             * @param {Element|jQuery object} item
             * The item to load.
             *
             * @param {Element|jQuery object} [location]
             * Optional location where to append the loaded content.
             * If not specified will append the loaded content to the item.
             *
             * @returns {Promise}
             * Returns a promise that tells you when the item is done loading.
             * You can use this promise even if the item is already on the page
             * and doesn't need to load anything.
             *
             * @example
             * myRepeatable.itemLoad(element).always(function(){ alert('Item is done loading'); });
             */
            itemLoad: function(item, location) {
                
                var self = this;
                var $item = $(item);
                var $location = location ? $(location) : $item;
                var $input;
                var url;
                var data;

                // In case we do not need to load anything, we'll create a deferred
                // promise that is already resolved
                var promise = $.Deferred().resolve().promise();
                
                // Look for a hidden input that has a data-form-fields-url attribute
                // which indicates we need to load the form fields fields for this item.
                // Note after we load the content we will remove data-form-fields-url
                // so it will only be found and loaded once.
                $input = $item.find('> input[data-form-fields-url]');

                if ($input.length > 0) {
                    
                    // Get the URL to fetch
                    url = $input.attr('data-form-fields-url');

                    // Get the data to pass to the URL
                    data = $input.val();
                    
                    // Remove the attribute and data so we don't fetch again
                    $input.removeAttr('data-form-fields-url');
                    $input.val('');

                    // Fetch the content
                    // Override the promise we created by that returned by the ajax call,
                    // so we can pass that back and you can take action when the ajax completes
                    promise = $.ajax({
                        'type': 'POST',
                        'cache': false,
                        'url': url,
                        'data': { 'data': data },
                    }).always(function(response) {

                        // The response will either be the data (on success), or a jqXHR object (on error)
                        var content = typeof(response) === 'string' ? response : response.responseText;

                        // When ajax completes add the content to the page
                        $(content).appendTo($location);

                        // If the item has already been removed, mark the new content to be removed as well
                        if (self.itemIsRemoved($item)) {
                            // Call the removeItem function again and this time
                            // it will also mark the newly loaded content to be disabled
                            self.removeItem($item);
                        }
                        
                        // Trigger some events so other code can know we have added content
                        // and can add more controls to the form we just loaded
                        $location.trigger('create');
                        $location.trigger('load');

                    });
                }

                // Return a promise so other events can be triggered after the item is loaded
                return promise;
            },

            
            /**
             * Toggle the remove state for an item.
             * When an item is marked for removal we perform the following steps:
             * - Add toBeRemoved class on the item.
             * - Disable all inputs.
             * - Change remove link text.
             *
             * @param {Element|jQuery object} item
             * The item (LI element).
             *
             * @param {Boolean} [removeFlag]
             * Set to true to force the item to be removed.
             * Set to false to force the item to be restored.
             * If undefined then the item will toggle between remove/restore.
             */
            removeItemToggle: function(item, removeFlag) {

                var self = this;
                var $item = $(item);
                var $removeButton = $item.find('.removeButton');
                var $inputs;
                var itemNumber = $item.index() + 1;
                var carousel = self.carousel;
                var $itemCarousel;
                var $content = $();

                // Define the content where we need to disable inputs and mark toBeRemoved
                $content = $content.add($item);
                
                // If we previously set up a carousel, get the tile in the carousel
                // so we can mark it as removed
                if (carousel) {
                    
                    // Add the carousel tile so it will get the "toBeRemoved" class
                    $content = $content.add( carousel.getTileContent(itemNumber) );

                    // Add the edit form for this item so we can mark it toBeRemoved
                    // and disable those inputs
                    $content = $content.add( $item.data('editContainer') );
                }

                // Find all the input elements within the content so we can disable them
                $inputs = $content.find(':input');
                
                // If the removeFlag was not specified, determine if it should be true
                // or false, based on the current class of the item
                if (removeFlag === undefined) {
                    removeFlag = !$item.is('.toBeRemoved');
                }
                
                if (removeFlag) {
                    
                    // Add the "toBeRemoved" class to both the item and the item in the carousel
                    $content.addClass('toBeRemoved');
                    
                    // Disable all the inputs within the item so they will not be sent to the backend
                    // when the form is submitted
                    $inputs.attr('disabled', 'disabled');

                    // Change the text in the remove button
                    if (self.options.restoreButtonText) {
                        $removeButton.text(self.options.restoreButtonText);
                    }

                } else {

                    // Remove the "toBeRemoved" class to both the item and the carousel
                    $content.removeClass('toBeRemoved');

                    // Disable all the inputs within the item so they will not be sent to the backend
                    // when the form is submitted
                    $inputs.removeAttr('disabled');
                    
                    // Change the text in the remove button
                    if (self.options.removeButtonText) {
                        $removeButton.text(self.options.removeButtonText);
                    }
                }

                // Trigger a change event to notify other code
                $item.change();
            },

            
            /**
             * Mark an item to be removed.
             * 
             * @param {Element|jQuery object} item
             * The item (LI element).
             */
            removeItem: function(item) {
                var self = this;
                self.removeItemToggle(item, true);
            },

            
            /**
             * Unmark an item to be removed (that is, restore the item).
             * 
             * @param {Element|jQuery object} item
             * The item (LI element).
             */
            restoreItem: function(item) {
                var self = this;
                self.removeItemToggle(item, false);
            },


            /**
             * Determine if an item is currently removed.
             * 
             * @param {Element|jQuery object} item
             * The item (LI element).
             */
            itemIsRemoved: function(item) {
                var self = this;
                return $(item).hasClass('toBeRemoved');
            },

            
            //==================================================
            // MODE SINGLE
            //==================================================

            
            /**
             * Additional initializing for the "Add Item" controls when mode is "single".
             */
            modeSingleInitAddButton: function() {
                
                var self = this;
                var $input;
                var $toggle;
                var $singleInput;
                var $addButtonContainer = self.dom.$addButtonContainer;
                
                // Only do this for single input mode
                if (!self.modeIsSingle()) {
                    return;
                }

                // Get the single input from the template, but not the ".toggle" input.
                // We will also clone the $toggle input later
                $toggle = self.dom.$templates.find(':input[name$=".toggle"]');
                $input = self.dom.$templates.find(':input').not($toggle);

                // Make a copy of the input
                $singleInput = $input.clone();

                // Remove ID if it has it since duplicates not allowed
                $singleInput.removeAttr('id');
                
                // When user presses Enter on the input then simulate a click on Add Item button
                // so another item can be added
                $singleInput.keydown(function(event) {
                    // Check for Enter key
                    if (event.which == 13) {
                        // TODO: instead of triggering a click on the add button
                        // we should just call the method that the click button calls
                        $addButtonContainer.find('.addButton').trigger('click');
                        return false;
                    }
                });

                // Put a toggle input into the add button container
                // So it will tell the back-end that this input should be added
                // ???: this is not checked initially (but is is checked when the new input is added?)
                $('<input/>', {
                    'name': $toggle.attr('name'),
                    'type': 'hidden',
                    'value': $toggle.attr('value')
                }).appendTo($addButtonContainer);

                // Put the copy of the single input into the add button container
                // so user can type into it and create new items
                $singleInput.appendTo($addButtonContainer);

                // Save for later use (so we can check if it contains a value)
                self.dom.$singleInput = $singleInput;
                
                // TODO: determine how these inputs are duplicated when the add button container is clicked...
            },

            
            //==================================================
            // MODE PREVIEW
            //==================================================

            /**
             * Initialize the carousel viewer for mode=preview
             *
             * TODO: needs lots of cleanup
             */
            modePreviewInit: function() {
                
                var self = this;
                var $container = self.$element;
                var carousel;
                var $viewGrid;
                var $viewCarousel;
                var $carouselTarget;
                var $carouselContainer;
                var $viewSwitcher;
                var $topButtonContainer;
                
                // We only need a carousel for "preview" mode
                if (!self.modeIsPreview()) {
                    return;
                }

                // Create controls at the top
                $topButtonContainer = $('<div/>', { 'class': 'repeatablePreviewControls' }).prependTo($container);

                // Move the "action-upload" link into the top button container
                $container.find('> .action-upload').appendTo($topButtonContainer);
                
                // Add a placeholder for the "Add Item" button(s) to later be added to the top.
                // Refer to initAddButton() to see how this is used.
                $('<span/>', { 'class': 'addButtonContainer' }).appendTo($topButtonContainer);

                // Create buttons to switch between grid view and gallery view
                $viewSwitcher = $('<span class="view-switcher">' +
                                  '<a href="#" class="view-switcher-active view-switcher-grid">Grid</a> | ' +
                                  '<a href="#" class="view-switcher-gallery">Gallery</a>' +
                                  '</span>').appendTo($topButtonContainer);
                
                self.dom.$viewSwitcher = $viewSwitcher; // Save for later

                // The grid view will use the existing UL or OL
                $viewGrid = self.dom.$list;
                self.dom.$viewGrid = $viewGrid; // save for later

                // For the carousel view, create a new placeholder but hide it initially
                $viewCarousel = $('<div/>', { 'class': 'viewCarousel' }).insertAfter(self.dom.$list).hide();
                self.dom.$viewCarousel = $viewCarousel; // save for later

                $carouselContainer = $('<div/>', {'class': 'carousel-container'}).appendTo($viewCarousel);

                // Also create a placeholder where we can edit each item and hide it initially
                $carouselTarget = $(self.templateEdit).appendTo($viewCarousel).hide();
                
                // Save for later so we have a place to insert the edit forms
                self.dom.$carouselTarget = $carouselTarget;
                self.dom.$carouselTargetItems = $carouselTarget.find('.carousel-target-items');

                // Set up the next/previous buttons on the edit form
                self.dom.$carouselTargetPrev = $carouselTarget.find('.carousel-target-prev').on('click', function(event){
                    event.preventDefault();
                    self.modePreviewEditNext(-1);
                });
                
                self.dom.$carouselTargetNext = $carouselTarget.find('.carousel-target-next').on('click', function(event){
                    event.preventDefault();
                    self.modePreviewEditNext();
                });
                
                // Now create the carousel object, initialize it, and save it for later use
                carousel = Object.create(carouselUtility);
                carousel.init($carouselContainer, {numbered:true});
                self.carousel = carousel;
                
                // Add a listener so we can do something when carousel items are clicked
                $carouselContainer.on('carousel.tile', function(e, carouselData) {

                    var $item;
                    
                    // @param {Object} carouselData
                    // @param {Object} carouselData.carousel
                    // @param {jQuery object} carouselData.index = the number of the tile that was clicked
                    // @param {Element} carouselData.tile = the tile that was clicked

                    // We previously saved the item element on the carousel tile
                    $item = $(carouselData.tile).data('item');
                    if ($item) {
                        // Send "false" as the second argument because
                        // we do not want to move the carousel position to
                        // the active tile.
                        self.modePreviewEdit($item, false);
                    }
                    
                });

                // Set up events so user can switch between the carousel view and the grid view
                $viewSwitcher.on('click', '.view-switcher-grid', function(event) {
                    self.modePreviewShowGrid();
                    return false;
                });
                $viewSwitcher.on('click', '.view-switcher-gallery', function(event) {
                    self.modePreviewShowCarousel();
                    return false;
                });

                // Set up a listener for when the user drags and drops an item to change the order,
                // so we can adjust the order of tiles in the carousel
                self.dom.$list.on('sortable.end', function(event, element) {

                    var $item = $(element);
                    
                    // Find the index for the new position of the element
                    var newIndex = $item.index() + 1;

                    // Get the carousel tile that corresponds to the item that was moved
                    var carouselTile = $item.data('carouselTile');

                    var oldIndex;
                    
                    if (carouselTile) {

                        // Take the carousel tile content and find the current index in the carousel
                        oldIndex = self.carousel.getTileIndex(carouselTile) || 0;
                        
                        self.carousel.repositionTile(oldIndex, newIndex);
                    }

                });
            },


            /**
             * Initialize an item for mode=preview 
             */
            modePreviewInitItem: function(element) {
                var self = this;
                var $item = $(element);
                var itemId = $item.find('> input[type="hidden"][name$=".id"]').val();
                var imageUrl;
                var $label;
                var $controls;
                var labelType = $item.attr('data-type') || 'Title';
                var labelText = $item.attr('data-label') || '[Empty Title]';
                var $editContainer;
                
                // Only do this for mode=preview
                if (!self.modeIsPreview()) {
                    return;
                }

                // Check for the preview image or use a placeholder image
                // TODO: update the placeholder image
                imageUrl = $item.attr('data-preview') || '';

                $('<img/>', {
                    'class': 'previewable-image',
                    src: imageUrl,
                    alt: ''
                }).on('click', function(){
                    self.modePreviewEdit($item);
                    return false;
                }).appendTo($item);
                
                // Add the title of the slide here
                //$label = $('<div class="previewable-label"><span class="previewable-label-prefix">' + labelType + ': </span></div>').appendTo($item);
                $label = $('<div/>', {
                    'class': 'previewable-label',
                    html: $('<span/>', {
                        'class': 'previewable-label-prefix',
                        text: labelType + ': '
                    })
                }).appendTo($item);

                $('<a/>', {
                    href: '#',
                    text: labelText,
                    // Set up some parameters so the label text will dynamically update based on the input field
                    'data-object-id': itemId,
                    'data-dynamic-text': '${content.label}'
                }).on('click', function(){
                    self.modePreviewEdit($item);
                    return false;
                }).appendTo($label);
               
                // Add controls at bottom of preview image:
                // - Set as cover
                // - Edit image
                // - Remove slide
                
                $controls = $('<div/>', {
                    'class': 'previewable-controls',
                }).appendTo($item);

                // Add control to set cover
                // TODO: will need some back-end changes to support this
                
                // Add control to edit image
                $('<span/>', {
                    'class': 'previewable-control-edit',
                    text: ''
                }).on('click', function(event) {
                    self.modePreviewEdit($item);
                    return false;
                }).appendTo($controls);
                
                // Add the item to the carousel
                self.modePreviewInitItemCarousel($item);
                
                // Create the edit container for this item below the carousel,
                // and if this item has a form already on the page move it there
                $editContainer = self.modePreviewCreateEditContainer($item);
                $item.find('.objectInputs').appendTo($editContainer);

                // If there are validation messages in the form,
                // mark  the gride and gallery tiles to show an error state,
                // then select the item and show the edit form so user can correct the error.
                if ($editContainer.find('.message-error').length) {
                    self.modePreviewMarkError($item);
                    self.modePreviewEdit($item);
                }
            },


            /**
             * Additional setup when initializing an item when a carousel is present.
             * This function adds a tile to the carousel for the item.
             */
            modePreviewInitItemCarousel: function(item) {
                
                var self = this;
                var $item = $(item);
                var carousel = self.carousel;
                var $carouselTile;
                var preview = $item.attr('data-preview');
                var label = $item.attr('data-label');
                
                if (!carousel) {
                    return;
                }
                
                $carouselTile = $('<figure/>');

                // Add the thumbnail image.
                // If there is no thumbnail image use a placeholder image.
                $('<img/>', {
                    'class': 'carousel-tile-content-image',
                    src: preview || '',
                    alt: ''
                }).appendTo($carouselTile);
                
                // Add the text label
                $('<figcaption/>', {
                    text: label || '[Empty Tile]',

                    // Set up some parameters so the label text will dynamically update based on the input field
                    'data-object-id': $item.find('> input[type="hidden"][name$=".id"]').val(),
                    'data-dynamic-text': '${content.label}'

                }).appendTo($carouselTile);

                // Add a remove/restore button to the carousel tile
                // This will be hidden by CSS unless the tile is active or "toBeRemoved"
                $('<span/>', {
                    'class': 'removeButton',
                    'text': self.options.removeButtonText
                }).on('click', function(){
                    self.removeItemToggle( $item );
                }).appendTo($carouselTile);

                // On the item, save a reference to the carousel tile,
                // so later if user changes sort order of the items,
                // we can determine which carousel tile needs to be moved
                $item.data('carouselTile', $carouselTile);
                
                // On the carousel tile, save a reference back to the item,
                // so later when carousel events occur we can find the item
                $carouselTile.data('item', $item);
                
                // Add the tile to the carousel
                self.carousel.addTile($carouselTile);
                
                // Note we don't call carouse.update() after each tile we add,
                // instead we wait until all tiles are added the call it once
                // at the end for best performance
            },



            /**
             * After adding a new item, this function cleans it up for mode=preview.
             * Create the thumbnail image for the empty item.
             * The added item will have a complete form, so move the form to the gallery view.
             */
            modePreviewAddItem: function(item) {
                
                var self = this;
                var $item = $(item);
                var $editContainer;
                
                if (!self.modeIsPreview()) {
                    return;
                }
                
                // Create a container in the gallery view for editing the item
                $editContainer = self.modePreviewCreateEditContainer($item);
                
                // Remove the item's edit form and move it to the edit container
                $item.find('> .objectInputs').appendTo($editContainer);

                // Trigger create event to process new content of edit container
                $editContainer.trigger('create');
                
                // Trigger a change to update any thumbnails
                $editContainer.find(':input').trigger('change');

                // Make sure the carousel is updated after we added a tile
                self.carousel.update();

                // Switch to carousel view and select the tile to edit
                self.modePreviewEdit($item);
            },

            
            /**
             * Edit an item for mode=preview
             *
             * @param {Element|jQuery object} item
             * The item to edit.
             *
             * @param {Boolean} [goToActiveTile]
             * Set to false if you do not want to move the carousel to the active tile.
             * Defaults to true.
             */
            modePreviewEdit: function(item, goToActiveTile) {
                var self = this;
                var $item = $(item);
                var $editContainer;

                if (!self.modeIsPreview()) {
                    return;
                }
                
                self.dom.$carouselTarget.show();
                
                // If necessary create the container for editing this item
                $editContainer = self.modePreviewCreateEditContainer($item);

                // Load the item into the edit container
                self.itemLoad($item, $editContainer).always(function(){

                    var editPosition;
                    var headerHeight;
                    var scrollPosition;
                    
                    // Switch to the carousel view
                    self.modePreviewShowCarousel();

                    // Set the active tile in the carousel
                    self.carousel.setActive( $item.index() + 1 );

                    if (goToActiveTile !== false) {
                        
                        self.carousel.goToActiveTile();
                        
                        // Also scroll the page so we can see the carousel
                        editPosition = self.$element.closest('.inputContainer').offset();
                        scrollPosition = editPosition.top;

                        // Check if the header is overlaying at the top
                        // so we can scroll a little more to account for it
                        headerHeight = $('.toolHeader').height();
                        if (headerHeight) {
                            scrollPosition = scrollPosition - headerHeight - 6;
                        }

                        // Only scroll up, never down
                        if (scrollPosition < $(window).scrollTop()) {
                            window.scrollTo(0, scrollPosition);
                        }
                    }
                    
                    // Hide all the other slide edit forms
                    self.dom.$carouselTarget.find('.itemEdit').hide();

                    // Update the next/previous buttons within the edit form
                    self.modePreviewUpdateEditContainer();

                    // Show the current edit form
                    $editContainer.show();
                });
            },


            /**
             * Start editing the next or previous tile.
             *
             * @param Number direction
             * If a negative number edit the previous tile.
             * Othersize edit the next tile.
             */
            modePreviewEditNext: function(direction) {

                var self = this;
                var tileIndex;

                if (direction && direction < 0) {
                    direction = -1;
                } else {
                    direction = 1;
                }
                
                // Determine which is the next item to edit
                tileIndex = self.carousel.getActive() + direction;

                // Bail if that's not a valid item
                if (tileIndex < 1 || tileIndex > self.dom.$list.find('> li').length) {
                    return;
                }

                // Go to the tile in the carousel
                self.carousel.setActive(tileIndex);
                self.carousel.goToActiveTile();

                // Activate the tile in the carousel, which will in turn
                // start editing the active item
                self.carousel.doTile(tileIndex);

                // After moving the carousel update the next/previous buttons on the edit form
                self.modePreviewUpdateEditContainer();
            },

            
            /**
             * For an item in mode=preview, create a placeholder for the edit container,
             * and optionally load some content into it.
             *
             * @param {Element|jQuery object} item
             * The item to edit.
             *
             * @param {String|Element|jQuery object} [content]
             * Optional content to add into the 
             */
            modePreviewCreateEditContainer: function(item) {
                
                var self = this;
                var $item = $(item);
                var itemId = $item.find('> input[type="hidden"][name$=".id"]').val();
                var $editContainer;

                // If necessary create the container for editing this item
                $editContainer = $item.data('editContainer');
                if (!$editContainer) {
                    $editContainer = $('<div/>', {
                        'class': 'itemEdit'
                    }).on('change', function(event) {

                        var imageUrl, $target, targetName, thumbnailName;

                        // Mark the tiles as changed so user can see which items have been modified.
                        // Inputs are not marked as changed until the change event bubbles up to the body of the page,
                        // so we put this in a timeout to hopefully force it to run after the event bubbles up.
                        // An alternative would be to bind another event on the document body, but then we
                        // would have no way to unbind that event.
                        setTimeout(function(){
                            self.modePreviewMarkAsChanged($item);
                        }, 1);

                        // If a change is made to the preview image update the thumbnail image in the carousel and grid view
                        $target = $(event.target).closest('[data-preview]');
                        imageUrl = $target.attr('data-preview');
                        targetName = $target.attr('name');

                        // Make sure this changed item is actually used for the thumbnail
                        // The data-preview-field contains the name of the field and the type,
                        // so we need to remove the /type part
                        thumbnailName = $item.attr('data-preview-field') || '';
                        thumbnailName = thumbnailName.replace(/(.*)\/.*/, '$1'); // remove last / and beyond
                        thumbnailName = itemId + '/' + thumbnailName;
                        
                        // Make sure the preview that was changed is actually used as the thumbnail for this repeatable object.
                        // This will account for cases where the repeatable object contains multiple images, or nested objects.
                        if (imageUrl && targetName === thumbnailName) {
                            self.modePreviewSetThumbnail($item, imageUrl);
                        }
                    }).appendTo(self.dom.$carouselTargetItems);
                    
                    $item.data('editContainer', $editContainer);
                }

                return $editContainer;
            },


            /**
             * Update the previous/next buttons in the edit container,
             * to show or hide the buttons based on which item is currently being edited.
             */
            modePreviewUpdateEditContainer: function() {
                
                var self = this;
                var tileIndex = self.carousel.getActive();
                var total = self.dom.$list.find('> li').length;
                
                self.dom.$carouselTargetPrev.toggleClass('carousel-target-nav-hide', !Boolean(tileIndex > 1));
                self.dom.$carouselTargetNext.toggleClass('carousel-target-nav-hide', !Boolean(tileIndex < total));
            },

            
            /**
             * When in preview mode, show the grid view.
             */
            modePreviewShowGrid: function() {

                var self = this;
                
                if (!self.modeIsPreview()) {
                    return;
                }

                // Mark the "Grid View" link as active and the "Gallery View" link as inactive
                self.dom.$viewSwitcher.find('a').removeClass('view-switcher-active').filter('.view-switcher-grid').addClass('view-switcher-active');
                self.dom.$viewGrid.show();
                self.dom.$viewCarousel.hide();
            },

            
            /**
             * When in preview mode, show the carousel view.
             */
            modePreviewShowCarousel: function() {

                var self = this;

                if (!self.modeIsPreview()) {
                    return;
                }

                self.dom.$viewSwitcher.find('a').removeClass('view-switcher-active').filter('.view-switcher-gallery').addClass('view-switcher-active');
                self.dom.$viewGrid.hide();
                self.dom.$viewCarousel.show();

                // In some cases carousel update doesn't work if carousel is hidden,
                // so we'll call update whenever we show the carousel to ensure
                // it is displaying everything correctly
                self.carousel.update();

                // After showing the carousel update the next/previous buttons on the edit form
                // This might be needed in case the tiles were reordered or something like that
                self.modePreviewUpdateEditContainer();

                // After showing the carousel center the active tile in the carousel
                self.carousel.goToActiveTile();
            },

            
            /**
             * After adding one or more items to the carousel,
             * call this to update the carousel display.
             */
            modePreviewUpdateCarousel: function() {
                var self = this;
                if (self.carousel) {
                    self.carousel.update();
                }
            },


            /**
             * Update the thumbnail preview image.
             * This updates both the grid view and the carousel view thumbnail.
             */
            modePreviewSetThumbnail: function(item, imageUrl) {
                var $item = $(item);
                var $thumbnails = $item.find('.previewable-image');
                var $carouselTile = $(item).data('carouselTile');

                if ($carouselTile) {
                    $thumbnails = $thumbnails.add($carouselTile.find('.carousel-tile-content-image'));
                }

                $thumbnails.attr('src', imageUrl);
            },


            /**
             * Mark the tiles as changed so the user can see they have been updated.
             * @param Boolean changed
             * Set to true if the item was changed, or false if changes were removed.
             */
            modePreviewMarkAsChanged: function(item) {
                var $item = $(item); // the thumbnail in grid view
                var $carouselTile = $item.data('carouselTile'); // the tile in gallery view
                var $editContainer = $item.data('editContainer'); // the edit form
                var isChanged = Boolean($editContainer.find('.state-changed').length > 0);
                
                $item.add($carouselTile).toggleClass('state-changed', isChanged);
            },

            /**
             * Update the thumbnail so it shows an error state.
             * This updates both the grid view and the carousel view thumbnail.
             * Used in cases where there is a validation error within the item.
             */
            modePreviewMarkError: function(item) {

                var self = this;
                var $item = $(item);
                var $thumbnails = $item.find('.previewable-image');
                var $carouselTile = $(item).data('carouselTile');
                var carousel = self.carousel;

                if (carousel && $carouselTile) {
                    carousel.toggleTileError( carousel.getTileIndex($carouselTile), true);
                }
                
                $thumbnails.closest('li').addClass('state-error');
            }
            
        }; // END repeatableUtility
        
    }); // END require

}(jQuery, window));
