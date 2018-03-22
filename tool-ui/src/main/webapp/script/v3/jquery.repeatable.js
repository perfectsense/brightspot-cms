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


 Acceptable variation for previewable items
 -------------------------------------------
 * The first visible `editable-view` <div> under view switcher will decide the default editing view for previewable items;
   By default, vertical view switcher is hidden.
 * By default, new items are inserted to the end of list;
   set global variable 'repeatableInsertFront' to be true to let new *previewable* items inserted to the beginning of the list.

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
    
    require(['v3/input/carousel', 'v3/color-utils'], function(carouselUtility, colors) {

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
                $container.data('repeatable', repeatable);
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

                self.initializingCount = 0;
                
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
                
                // Initialize collection item weights if needed
                self.modeWeightedInit();

                // For each item initialize it
                self.dom.$list.find('> li').each(function(){
                    self.initItem(this);
                });

                // After we're done initilizing all the items,
                // update the carousel if ncessary
                self.modePreviewUpdateCarousel();

                // Create the "Add Item" button(s)
                self.initAddButton();

                if (self.mode == 'preview') {
                    // Create the modal for waiting items to be initialized
                    self.initWaitingModal();
                }

                if (self.dom.$defaultEditableView == self.dom.$viewVertical) {
                    // Create index selector
                    self.initIndexer();
                }

                self.isInitialized = true;
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

                // Save form for later use
                self.dom.$form = self.dom.$list.closest('form');

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
                
                if (self.$element.hasClass('repeatableForm-weighted')) {
                    self.mode = 'weighted';
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
                var $addButtonSelect;
                var tooManyButtons;
                
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

                // Create an "Add Item" link for each template we found,
                // or if there are too many links create a dropdown
                
                tooManyButtons = Boolean(self.dom.$templates.length > 10);
                
                if (tooManyButtons) {

                    // Create a dropdown to list the available add buttons
                    var $addButtonSelectContainer = $('<span/>', {
                        'class': 'addButtonSelectContainer'
                    }).appendTo($addButtonContainer);

                    $addButtonSelect = $('<select/>', {
                        'class': 'addButtonSelect',
                        'data-searchable': true,
                        on: {
                            change: function () {

                                // Get the selected option
                                var $selected = $addButtonSelect.find(':selected');

                                // Get a link to the add button for that option, and click it.
                                // The "addButton" data is saved on the OPTION element.
                                var $button = $selected.data('addButton');
                                if ($button) {
                                    // Note: normally I wouldn't simulate a click on the button;
                                    // however, there is other pre-existing code that requires that
                                    // add button to be there, so we will continue creating the button
                                    // and hide it if we have too many buttons to show.
                                    $button.click();
                                }

                                $addButtonSelect.val('');
                                $addButtonSelectContainer.find('.dropDown-label').trigger('dropDown-update');
                                $addButtonSelect.blur();
                            }
                        }
                    }).appendTo($addButtonSelectContainer);

                    $addButtonSelect.append($('<option/>', {
                        text: 'Add'
                    }));
                }
                
                self.dom.$templates.each(function() {
                    
                    var $template = $(this);
                    var itemType = $template.attr('data-type') || 'Item';
                    var $addButton;
                    var addButtonText;

                    // Determine which text to use for the add button
                    addButtonText = itemType;
                    if (self.options.addButtonText) {
                        addButtonText = self.options.addButtonText + ' ' + addButtonText;
                    }
                    
                    // Add an element for the "Add Button" control
                    $addButton = $('<span/>', {
                        
                        'class': 'addButton',
                        text: addButtonText,
                        'data-sortable-item-type': $template.attr('data-sortable-item-type')

                        // Save the template on the add button control so when user
                        // clicks it we will know which template to add
                        // 'data-addButtonTemplate': $template
                        
                    }).on('click', function(event, customCallback) {
                        ++ self.initializingCount;
                        if (self.mode == 'preview') {
                            // Pop up a loading modal and blocks page scroll when items are initializing
                            if (!self.dom.$modal.closest('.popup').hasClass('popup-show')) {
                                self.dom.$modal.popup('open');
                                $('body').addClass('loading');
                            }
                        }
                        // The click event for the add button supports an
                        // optional callback function that will be called after
                        // the new item is added.
                        // Add the new item based on this template
                        self.addItem($template, customCallback);

                        return false;
                        
                    }).toggle(!tooManyButtons) // Hide button if there are too many buttons
                        .appendTo($addButtonContainer);

                    if (tooManyButtons) {
                        $('<option/>', {
                            value: itemType,
                            text: itemType,
                            data: {
                                // Save the add button in a data attribute,
                                // so later when user selects this item we know which
                                // add button should be clicked
                                'addButton': $addButton
                            }
                        }).appendTo($addButtonSelect);
                    }
                });
            },

            /**
             * Initialize the index selector that will be shared across item on vertical view.
             * This indexer will show on hovering item and get current active item index;
             */
            initIndexer: function () {
                var self = this;
                var $index = $('<div/>', {'class': 'item-index-select'});
                var $select = $('<select/>').appendTo($index);
                for (var i = 1; i < self.dom.$list.find('li').length + 1; i++) {
                    var $option = $('<option></option>').val(i).html(i);
                    $select.append($option);
                }
                self.dom.$indexer = $index.hide();
                self.dom.$indexer.$activeItem = null;
                self.dom.$indexer.find('select').change(function () {
                    if (self.dom.$indexer.$activeItem) {
                        var oldIndex = self.dom.$indexer.$activeItem.index();
                        var newIndex = self.dom.$indexer.find('select').val() - 1;
                        if (oldIndex != newIndex) {
                            self.repositionItem(oldIndex, newIndex, self.dom.$indexer.$activeItem);
                            self.verticalViewFocus(null, newIndex);
                            self.carousel.repositionTile(oldIndex + 1, newIndex + 1); // this api use index start from 1
                        }
                    }
                })
            },

            /**
             * Initialize the modal that presents when there're more than 5 items being added through #addItem()
             */
            initWaitingModal: function () {
                var self = this;
                var $modal = $('<div>');
                $modal.append($('<div class="spinner"></div>'));
                self.dom.$modal = $modal;
                $('body').append(self.dom.$modal);
                self.dom.$modal.popup();
                self.dom.$modal.closest('.popup').addClass('loading-modal');
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
                
                // Create item weight for the item if necessary,
                // and set up other stuff for mode=weighted
                self.modeWeightedInitItem($item);
                
                // Create a the label for the item if necessary.
                // The label acts as a toggle to show and hide the item,
                // plus it can load the content of the item from a URL
                self.initItemLabel($item);

                // Collapse the item unless it has an error message within it
                // Do not collapse "preview" or "object" mode
                if ($item.find('.message-error').length === 0
                    && $item.find('> .layouts').length === 0
                    && !$item.hasClass('expanded')
                    && !self.modeIsPreview()
                    && !self.modeIsObject()) {
                    self.itemCollapse($item);
                }

                // An input with ".toggle" in the name will be checked
                // to tell the backend that it should be saved, but the checkbox will not actually
                // be shown to the user, so we hid it here
                $item.find(':input[name$=".toggle"]').hide();
                
                // Create progress UI if necessary
                self.initCollectionItemProgress($item);
                
                // Create toggle UI if necessary
                self.initCollectionItemToggle($item);

                // Add the remove control to the item
                self.initItemRemove($item);

                if (self.dom.$defaultEditableView == self.dom.$viewVertical) {
                    // Make service call to create full edit form
                    self.initVerticalItem($item);
                }
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
                var label = $item.attr('data-label-html') || $item.attr('data-label');
                var $label;
                var $existingLabel;
                var labelHtml;

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
                labelHtml = type;
                if (label) {
                    labelHtml += ': ' + label;
                }

                $label = $('<div/>', {
                    'class': 'repeatableLabel',
                    'html': labelHtml,
                    
                    // Set up some parameters so the label text will dynamically update based on the input field
                    'data-object-id': $item.find('> input[type="hidden"][name$=".id"]').val(),
                    'data-dynamic-html': '${content.state.getType().label}: ${toolPageContext.createObjectLabelHtml(content)}'
                    
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
             * Conditionally initializes the progress information for an individual item.
             *
             * @param {Element|jquery object} item
             * the item (LI element).
             */
            initCollectionItemProgress: function (item) {

                var $item = $(item);
                var progressFieldValue = $item.attr('data-progress-field-value');

                if (!progressFieldValue) {
                    return;
                }

                $('<div>', {
                    'class' : 'repeatableLabel-progress'
                }).append($('<div>', {
                        'class': 'repeatableLabel-progressBar'
                    }).append($('<div>', {
                        'class': 'repeatableLabel-progressFill',
                        'style': 'width: ' + progressFieldValue + '%'
                    })
                )).prepend($('<div>', {
                    'class': 'repeatableLabel-progressLabel',
                    'text': progressFieldValue + '% of Target'
                })).appendTo($item);

            },

            /**
             * Conditionally initializes the toggle button for an individual item.
             *
             * @param {Element|jquery object} item
             * the item (LI element).
             */
            initCollectionItemToggle: function(item) {

                var self = this;
                var $item = $(item);
                var toggleField = $item.attr('data-toggle-field');
                var toggleFieldValue = $item.attr('data-toggle-field-value');

                if (!toggleField) {
                    return;
                }

                // Add the remove button to the item
                var id = $item.find('> input[type="hidden"][name$=".id"]').val();

                var input = $('<input/>', {
                    'id': id + '-toggle',
                    'class': 'repeatableLabel-toggle',
                    'type': 'checkbox',
                    'name':  id + '/' + toggleField,
                    'value': true
                }).on('change', function(event) {
                    // Add/remove weight and recalculate
                    var checked = $(this).is(':checked');
                    $item.attr('data-toggle-field-value', checked);

                    if ($item.closest('.repeatableForm').hasClass('repeatableForm-weighted')) {
                        if (checked === true) {
                            self.addCollectionItemWeight($item);
                        } else {
                            self.removeCollectionItemWeight($item);
                        }
                    }
                });

                if (toggleFieldValue === 'true') {
                    input.attr('checked', 'checked');
                }

                input.after($('<label>', {
                    'for': id + '-toggle',
                    'class': 'repeatableLabel-toggleLabel'
                }));

                input.appendTo($item);

            },

            /**
             * Initialize item for vertical view; do nothing if vertical view is disabled
             **/
            initVerticalItem: function(item) {
                var self = this;
                var $item = $(item);

                // if vertical view switch is hidden, do nothing
                if (!self.dom.$viewSwitcher || self.dom.$viewSwitcher.find('.view-switcher-vertical').is(':hidden')) {
                    return
                }

                // load item edit form but hide for now
                var $itemEditContainer = $item.find('.itemEdit-vertical-container');
                if ($itemEditContainer.length == 0) {
                    $itemEditContainer = $('<div/>', {'class': 'itemEdit-vertical-container'}).on('change', function () {
                        self.modePreviewImageChangeSync($item);
                    }).appendTo($item);
                }
                var $itemEdit = $itemEditContainer.find('> .objectInputs');
                if ($itemEdit.length == 0) {
                    $item.data("currentView", "vertical");
                    self.itemLoadOrMove($item, $itemEditContainer);
                }
                $itemEditContainer.hide();

                // init item preview placeholder and indexer
                var $preview = $('<div/>', {
                    'class': 'item-preview'
                }).mouseenter(function(){
                    if ($itemEditContainer.is(":hidden")) {
                        return;
                    }
                    $(this).prepend(self.dom.$indexer);
                    self.dom.$indexer.$activeItem = $item;
                    self.dom.$indexer.find('select').val($item.index() + 1).change();
                    self.dom.$indexer.show();

                    // trigger 'create' event to apply bsp modification on selector
                    self.dom.$indexer.find('select').show().css('visibility', 'hidden');
                    self.dom.$indexer.find('>.dropDown-input').remove();
                    self.dom.$indexer.trigger('create');

                }).appendTo($item);

                var dataPreviewField = $item.attr('data-preview-field');
                if (dataPreviewField) {
                    var fieldName = dataPreviewField.split('/')[0];
                    var $previewEle = $item.find('> .itemEdit-vertical-container').find('> .objectInputs').find('> .inputContainer[data-field ="' + fieldName + '"]')
                        .find('> .inputSmall').find('> input[data-dynamic-field-name ="' + fieldName + '"]');
                    $previewEle.on('change', function () {
                        var imgUrl = $previewEle.attr('data-preview');
                        $preview.find('> .previewable-image').attr('src', imgUrl);
                    })
                }
            },

            //==================================================
            // MODE WEIGHTED
            //==================================================

           /**
            * Initialize weighted mode
            */
            modeWeightedInit: function() {
                
                var self = this;
                    
                if (!self.modeIsWeighted()) { 
                    return;
                }

                // Set up a listener for when the user drags and drops an item to change the order,
                // so we can adjust the order of weights
                self.dom.$list.on('sortable.end', function (event, element) {

                    var $item = $(element);
    
                    // Find the index for the new position of the element
                    var newIndex = $item.index();
                    var oldIndex = $item.data('repeatableIndex');
                    self.repositionCollectionItemWeight(oldIndex, newIndex);

                });
                 
                var $itemWeightsContainer = self.dom.$list.parent().find('.repeatableForm-itemWeights');
                self.collectionItemWeightsCalculated = $itemWeightsContainer.data('calculated');

                // If values are calculated, handle updates from cms-updateContentState event
                if (self.collectionItemWeightsCalculated) {
                    // TODO: Make this functionality generic, using meta tags or hidden fields
                    self.dom.$form.on('cms-updateContentState', (function(self) {
                        return function(event, data) {

                            var allDiffs = $.extend({}, data._differences, data._hiddenDifferences);
                            if (!allDiffs) {
                                return;
                            }

                            $.each(allDiffs, function (id, fields) {
                                if (fields.length === 0) {
                                    return;
                                }
                                
                                var $inputs = self.dom.$list.find('.objectInputs[data-object-id="' + id + '"]');

                                if (!$inputs) {
                                    return;
                                }

                                $item = $inputs.closest('li');

                                if (!$item) {
                                    return;
                                }

                                var weightFieldName = $item.attr('data-weight-field');
                                if (!weightFieldName){
                                    return;
                                }

                                var newWeightFieldValue = fields[weightFieldName];
                                if (newWeightFieldValue 
                                        && $item.data('weight-field-value') !== newWeightFieldValue) {

                                    $item.data('weight-field-value', newWeightFieldValue);
                                    self.updateCollectionItemWeightData($(self.dom.$list
                                        .siblings('.repeatableForm-itemWeights')
                                        .find('.repeatableForm-itemWeight')
                                        .get($item.index())), Math.round(newWeightFieldValue * 100));
                                    self.fixCollectionItemWeights();
                                }

                                var weightMarkersFieldName = $item.attr('data-weight-markers-field');
                                if (!weightMarkersFieldName) {
                                    return;
                                }

                                var newWeightMarkersFieldValue = fields[weightMarkersFieldName];
                                if (newWeightMarkersFieldValue 
                                        && $item.data('weight-markers-field-value') !== newWeightMarkersFieldValue) {
                                    
                                    $item.data('weight-markers-field-value', newWeightMarkersFieldValue);
                                    self.setCollectionItemWeightMarkers($item); 
                                }
                            });
                        };
                        }(self)
                    ));
                }
              
                self.initCollectionItemWeightResetButton();
          
            },

           /**
            * Conditionally initializes the weighting display for an individual item.
            *
            * @param {Element|jquery object} item
            * the item (LI element).
            */
            modeWeightedInitItem: function(item) {

                var self = this;
                var $item = $(item);

                var weightFieldName = $item.data('weight-field');

                // Only display field weights if all valid types support collection weights
                if (!self.modeIsWeighted() || !weightFieldName) {
                    return false;
                }
                
                $item.on('click', '.removeButton', function() {
                    var $this = $(this);
                    if ($this.closest('li').hasClass('toBeRemoved')) {
                        self.removeCollectionItemWeight($item);
                    } else {
                        self.addCollectionItemWeight($item);
                    }
                });

                // Save original index so that we can use this in sortable.end handler
                $item.data('repeatableIndex', $item.index());

                self.addCollectionItemWeight($item);
            },

            /**
             * Adds a weight to the collection for a given item.
             */
            addCollectionItemWeight: function(item) {
                var self = this;
                var $item = $(item);
                var color = self.getCollectionItemWeightColor($item);
                var inputName = $item.find('> input[type="hidden"][name$=".id"]').val() + '/' + $item.data("weight-field");
                var $repeatableWeightLabel = $item.find('> .repeatableLabel-weightLabel');
                
                var weightFieldValue;
                if ($repeatableWeightLabel.size() === 0) {
                    weightFieldValue = $item.attr('data-weight-field-value');

                    $('<div>', {
                        'class': 'repeatableLabel-weightLabel'
                    }).append($('<span>', {
                        'class': 'repeatableLabel-color',
                        'style': 'background-color: ' + color
                    })).append(
                      $('<input>', {
                          'type': 'hidden',
                          'name': inputName,
                          'value': weightFieldValue
                      })
                    ).prependTo($item);
                } else {
                    weightFieldValue = $item.find('> .repeatableLabel-weightLabel').find('input').val();
                }
                
                // Only add weight and handle if toggle is true or not available
                if ($item.attr('data-toggle-field-value') === 'false') {
                    return;
                }

                var $itemWeight = $('<div>', {
                    'class': 'repeatableForm-itemWeight',
                    'style': 'background-color: ' + color + ';',
                    'data-target': inputName,
                    'data-weight': weightFieldValue
                });

                // Filter items that have been toggled off
                var itemIndex = $item.parent()
                                     .children(':not([data-toggle-field-value="false"], .toBeRemoved)')
                                     .index($item);

                var $itemWeightContainer = $item.closest('.repeatableForm').find('.repeatableForm-itemWeights');
                if (itemIndex === 0) {
                    $itemWeightContainer.prepend($itemWeight);
                } else {
                    $itemWeightContainer.children().eq(itemIndex - 1).after($itemWeight);
                }

                self.setCollectionItemWeightMarkers($item);

                // Avoid creating handles if draggable is false
                if (!self.collectionItemWeightsCalculated) {
                    $itemWeight.prepend(self.createCollectionItemWeightHandle($item));
                }

                var itemWeightDoubleValue = $itemWeight.attr('data-weight');
                
                // If item has no data-weight attr, it is a new item,
                // and should scale other weights against `baseWeight`
                
                if (self.isInitialized === true || !itemWeightDoubleValue) {
                    
                    var newWeightDouble;
                    
                    if (!itemWeightDoubleValue) {
                        // new items are added with a weight proportional to the number of items
                        newWeightDouble = Math.round((1 / ($itemWeightContainer.children().size())) * 100) / 100;
                    } else {
                        // if item exists, use existing weight
                        newWeightDouble = parseFloat(itemWeightDoubleValue);
                    }
                    
                    var modifier = 1 - newWeightDouble;

                    $($itemWeightContainer.children().not($itemWeight)).each(function () {
                        self.updateCollectionItemWeightData($(this), Math.floor(modifier * $(this).attr('data-weight') * 100));
                    });

                    self.updateCollectionItemWeightData($itemWeight, (newWeightDouble * 100).toFixed());
                    self.fixCollectionItemWeights();
                } else {
                    self.updateCollectionItemWeightData($itemWeight, (itemWeightDoubleValue * 100).toFixed());
                }
            },

           /**
            * Sets markers to repeatableForm-itemWeight.
            */
            setCollectionItemWeightMarkers: function(item) {
                var self = this;
                var $item = $(item);
                var $itemWeight = $(self.dom.$list
                                    .siblings('.repeatableForm-itemWeights')
                                    .find('.repeatableForm-itemWeight')
                                    .get($item.index()));

                $itemWeight.find('.repeatableForm-itemWeight-marker').remove();

                var markerValues = $item.data('weight-markers-field-value');
                if (markerValues) {
                    Array.prototype.forEach.call(markerValues, function(markerValue, i) {
                        $itemWeight.append($('<div/>', {
                            'class': 'repeatableForm-itemWeight-marker',
                            'style': 'left:' + markerValue * 100 + "%;"
                        }));
                    });
                }
            },

            /**
             * Removes a weight to the collection for a given item.
             */
            removeCollectionItemWeight: function(item) {

                var self = this;
                var $item = $(item);

                var $repeatableForm = self.dom.$list.parent();
                var $itemWeightsContainer = $repeatableForm.find('.repeatableForm-itemWeights');

                $itemWeightsContainer.find('.repeatableForm-itemWeight[data-target="' + $item.find('.repeatableLabel-weightLabel input').attr('name') + '"]').remove();

                var $itemWeights = $repeatableForm.find('.repeatableForm-itemWeight');

                var totalRemainingPercent = 0;
                $itemWeights.each(function(i) {
                    var $itemWeight = $(this);
                    totalRemainingPercent += parseFloat($itemWeight.attr('data-weight'));
                });
                
                $itemWeights.each(function(i) {
                   var $itemWeight = $(this);
                   var newDouble = parseFloat($itemWeight.attr('data-weight')) / totalRemainingPercent;
                   self.updateCollectionItemWeightData($itemWeight, Math.round(newDouble * 100));
                });

                self.fixCollectionItemWeights();
            },
            
            repositionCollectionItemWeight: function(oldIndex, newIndex) {
                var self = this;
                
                var $weightsContainer = self.dom.$list.parent().find('.repeatableForm-itemWeights');
                var $weights = $weightsContainer.children();
                var $weight = $weights.eq(oldIndex);
                
                if (newIndex === 0) {
                    $weightsContainer.prepend($weight);
                } else {
                    $weights.eq(newIndex).after($weight);
                }
                
                self.dom.$list.children().each(function() {
                    var $item = $(this);
                    $item.data('repeatableIndex', $item.index());
                })
                
            },

            initCollectionItemWeightResetButton: function () {
                var self = this;

                if (self.collectionItemWeightsCalculated) {
                    return;
                }

                self.dom.$list.parent().prepend(
                  $('<a>', {
                      'class': 'repeatableForm-weightResetButton',
                      'text': 'Reset Weights'
                  }).on('click', function () {
                      self.resetCollectionItemWeights();
                  })
                );
            },

          /**
           * Resets the collection item weights with an even distribution.
           */
            resetCollectionItemWeights: function () {

                var self = this;
                var $itemWeights = self.dom.$list.parent().find('.repeatableForm-itemWeight');
                var itemWeightsCount = $itemWeights.size();

                if (itemWeightsCount <= 0) {
                    return false;
                }

                var weight = Math.floor(100 / itemWeightsCount);

                $itemWeights.each(function () {
                    self.updateCollectionItemWeightData(this, weight);
                });

                self.fixCollectionItemWeights();
            },

            /**
             * Prevents rounding errors and sum of weights from exceeding 100%.
             */
            fixCollectionItemWeights: function() {

                var self = this;
                var $itemWeights = self.dom.$list.parent().find('.repeatableForm-itemWeights  > .repeatableForm-itemWeight');
                var totalPercent = 0;

                $itemWeights.each(function(i) {
                    var $itemWeight = $(this);
                    var $itemWeightValue = parseFloat($itemWeight.attr('data-weight'));
                    var $itemWeightPercent = $itemWeightValue * 100;
                    totalPercent += $itemWeightPercent;

                    if (i + 1 === $itemWeights.size() && totalPercent !== 100) {
                        self.updateCollectionItemWeightData($itemWeight, $itemWeightPercent + 100 - totalPercent);
                    }
                });
            },

            updateCollectionItemWeightData: function(itemWeight, percent) {

                var self = this;
                var $itemWeight = $(itemWeight);
                var $repeatableForm = $itemWeight.closest('.repeatableForm');
                var double = percent / 100;

                $itemWeight.css({ 'flex' :  double + ' 1 0%'});
                $itemWeight.attr('data-weight', double);

                var liIndex = $repeatableForm.find('input[name="' + $itemWeight.attr('data-target') + '"]').closest('li').index();

                var $repeatableWeightDisplay = $(self.dom.$list.children('li').get(liIndex)).find('.repeatableLabel-weightLabel');
                $repeatableWeightDisplay.attr('data-weight-label', percent + '%');
                $repeatableWeightDisplay.find('input').val(double);
            },

            /**
             * Gets a color for an item to represent the item's weight.
             */
            getCollectionItemWeightColor: function(item) {
                var $item = $(item);
                var weightColor = $item.data('weight-color');

                if (weightColor) {
                    return weightColor;
                }

                var $repeatableForm = $item.closest('.repeatableForm');
                var hue = $repeatableForm.data('lastHue');

                if (!hue) {
                    hue = colors.getHue('#38b5dc');
                } else {
                    hue = colors.changeHue(hue);
                }

                weightColor = colors.generateFromHue(hue);

                $repeatableForm.data('lastHue', hue);
                $item.data('weight-color', weightColor);

                return weightColor;
            },

            /**
             * Creates an element that handles dragging to
             * change the collection item weight field.
             */
            createCollectionItemWeightHandle: function(item) {
                
                var self = this;
                
                return $(
                    '<div>', {
                        'class': 'repeatableForm-itemWeightHandle',
                        'mousedown': function(event) {
                            $.drag(this, event, function(event, data) {
                                data.originX = event.clientX;
                                data.rightElement = $(this).parent();
                                data.leftElement = data.rightElement.prev();

                                data.originalRightWeight = parseFloat(data.rightElement.attr('data-weight'));
                                data.originalLeftWeight = parseFloat(data.leftElement.attr('data-weight'));

                                data.rightIndex = data.rightElement.index();
                                data.leftIndex = data.leftElement.index();
                                
                                var $listElements = self.dom.$list.children(':not([data-toggle-field-value="false"], .toBeRemoved)');

                                data.rightLabel = $($listElements.get(data.rightIndex)).find('.repeatableLabel-weightLabel');
                                data.leftLabel = $($listElements.get(data.leftIndex)).find('.repeatableLabel-weightLabel');

                                data.rightInput = data.rightLabel.find('input');
                                data.leftInput = data.leftLabel.find('input');

                                data.totalWidth = data.rightElement.outerWidth() + data.leftElement.outerWidth();
                                data.totalWeightDouble = data.originalRightWeight + data.originalLeftWeight;

                            }, function(event, data) {

                                var delta = data.originX - event.clientX;
                                var deltaPercent = Math.round((delta / data.totalWidth) * 100);
                                var deltaDouble =  deltaPercent / 100;

                                var newLeftWeightDouble = Math.round((data.originalLeftWeight - deltaDouble) * 100) / 100;
                                var newRightWeightDouble = Math.round((data.totalWeightDouble - newLeftWeightDouble) * 100) / 100;

                                if (newLeftWeightDouble < 0) {
                                    newLeftWeightDouble = 0;
                                    newRightWeightDouble = data.totalWeightDouble;
                                }

                                if (newRightWeightDouble < 0) {
                                    newRightWeightDouble = 0;
                                    newLeftWeightDouble = data.totalWeightDouble;
                                }
                                
                                var newLeftWeightPct = Math.max(Math.round(newLeftWeightDouble * 100), 0);
                                var newRightWeightPct = Math.max(Math.round(newRightWeightDouble * 100), 0);

                                data.leftElement.css({
                                    'flex': newLeftWeightDouble + ' 1 0%'
                                }).attr('data-weight', newLeftWeightDouble);
                                
                                data.rightElement.css({
                                    'flex': newRightWeightDouble + ' 1 0% '
                                }).attr('data-weight', newRightWeightDouble);
                                
                                // Update input and numerical display
                                data.rightLabel.attr('data-weight-label', newRightWeightPct + '%');
                                data.leftLabel.attr('data-weight-label', newLeftWeightPct + '%');
                                
                                data.rightInput.val(newRightWeightDouble);
                                data.leftInput.val(newLeftWeightDouble);

                            }, function(event) {
                                // do nothing.
                            });
                        }
                    }
                );
            },


            /**
             * Add a new item to the list, based from a template.
             *
             * checks on global variable `repeatableInsertFront` to decide if to insert new item to the beginning of the list
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

                        // Close loading modal and release page scroll when all calls returned
                        if (self.mode == 'preview' && --self.initializingCount == 0 && self.dom.$modal.closest('.popup').hasClass('popup-show')) {
                            self.dom.$modal.popup('close');
                            $('body').removeClass('loading');
                        }

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

                    var currentIndex = $addedItem.index();

                    if (win.repeatableInsertFront && self.modeIsPreview()) {
                        // move added item to the beginning
                        self.repositionItem(currentIndex, 0, $addedItem);
                        self.carousel.repositionTile(currentIndex + 1, 1); // this api use index start from 1
                        self.repositionCarouselTarget(currentIndex, 0)
                    }

                    if (self.modeIsPreview()) {
                        if (self.itemIsVerticalView($addedItem)) {
                            // Switch to vertical view and focuse to added item
                            self.modePreviewEditVertical($addedItem);
                            // Make sure newly added image is used for preview
                            self.modePreviewImageChangeSync($addedItem);
                        } else {
                            $addedItem.data("currentView", "gallery");
                            self.modePreviewEdit($addedItem, true);
                        }
                    }

                    // Trigger a resize event for the window
                    // Since we have added new content
                    $win.resize();

                    // Insert another option to indexer
                    if (self.dom.$indexer) {
                        self.dom.$indexer.find('select').append($('<option></option>').val(currentIndex + 1).html(currentIndex + 1));
                    }

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
           * Shortcut function to tell if the mode is 'weighted'
           * @returns {Boolean}
           */
          modeIsWeighted: function() {
                var self = this;
                return self.mode === 'weighted';
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

                if (collapseFlag && $item.hasClass('expanded')) {
                    $item.removeClass('expanded');
                }
                
                // Don't do anything if mode=preview
                if (self.modeIsPreview()) {
                    return;
                }

                // Load the item if necessary,
                // or if it's already loaded do some stuff immediately
                if (!self.itemIsCollapsed($item)) {
                    self.itemLoadOrMove($item).always(function(){
                        self.itemLazyLoad($item);
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
             * Determine if an item is an vertical view item
             *
             * @param {Element|jQuery object} item
             * The list item to test.
             */
            itemIsVerticalView: function (item) {
                return item && $(item).hasClass('item-vertical-view');
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

                if ($item.data('loaded')) {
                    return promise;
                }
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
                        var $content = $(content);
                        $content.find('textarea.richtext').removeClass('richtext').addClass('richtextLazy');
                        // When ajax completes add the content to the page
                        $content.appendTo($location);
                        $item.data('loaded', true);

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
             * Checks if an item edit form has already been loaded in vertical view or gallery view
             * if not, load it using itemLoad function
             * otherwise, move it to location
             * @param {Element|jQuery object} item has data of current view;
             **/
            itemLoadOrMove: function(item, location) {
                var self = this;
                var $item = $(item);
                var $location = location ? $(location) : $item;
                var currentView = $item.data("currentView");
                var itemIndex = $item.index();
                var $editForm;
                if (currentView == "vertical") {
                    var $sourceViewItemList = self.dom.$viewCarousel.find('> .carousel-target').find('> .carousel-target-items')
                        .find('> .itemEdit');
                    if ($sourceViewItemList.length > itemIndex) {
                        $editForm = $sourceViewItemList.eq(itemIndex).find('> .objectInputs');
                    }
                } else {
                    // currently there're only vertical view and gallery view that will use these function
                    $editForm = $item.find('> .itemEdit-vertical-container').find('> .objectInputs');
                }

                // if an edit form is already loaded in source view, move it
                if ($editForm.length > 0) {
                    $location.append($editForm);
                    return self.itemLoad($item, $location);
                }

                // otherwise load item
                return self.itemLoad($item, $location);
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
            * Immediately removes an item from
            * the DOM. Invoked by objectIdResult.jsp.
            */
            removeItemImmediately: function(item) {
                var self = this;
                var $item = $(item);

                if (self.modeIsWeighted) {
                    self.removeCollectionItemWeight($item);
                }
            
                $item.remove();
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
                var $viewVertical;
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
                // Create buttons for available view, up to each project to decide which ones to hide if necessary
                // First 'editable-view' will be used for editing a slide
                $viewSwitcher = $('<span class="view-switcher">' +
                    '<a href="#" class="view-switcher-active view-switcher-grid">Grid</a>' +
                    '<span class="view-switcher-vertical editable-view">|</span> <a href="#" class="view-switcher-vertical editable-view">Vertical</a>' +
                    ' <span class="view-switcher-gallery editable-view">|</span> <a href="#" class="view-switcher-gallery editable-view">Carousel</a>' +
                    '</span>').appendTo($topButtonContainer);

                // win.showVerticalView is set in edit.jsp basing on CmsTool.galleryDisplay
                if (!win.showVerticalView) {
                    $viewSwitcher.find('.view-switcher-vertical').hide();
                } else {
                    $viewSwitcher.find('.view-switcher-gallery').hide();
                }

                self.dom.$viewSwitcher = $viewSwitcher; // Save for later

                // The grid view will use the existing UL or OL
                $viewGrid = self.dom.$list;
                self.dom.$viewGrid = $viewGrid; // save for later

                // The vertical view will also use the existing UL or OL as skeleton
                $viewVertical = self.dom.$list;
                self.dom.$viewVertical = $viewVertical; // save for later

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

                // Set up events so user can switch between views
                $viewSwitcher.on('click', '.view-switcher-grid', function(event) {
                    self.modePreviewShowGrid();
                    return false;
                });
                $viewSwitcher.on('click', '.view-switcher-vertical', function(event) {
                    self.modePreviewShowVertical(null);
                    return false;
                });
                $viewSwitcher.on('click', '.view-switcher-gallery', function(event) {
                    self.modePreviewShowCarousel(null);
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

                self.dom.$list.on('sortable.start', function () {
                    if (self.dom.$indexer != null) {
                        self.dom.$indexer.hide();
                        self.dom.$indexer.$activeItem = null;
                    }
                })

                // initializing $defaultEditableView to be the first visible .editable-view
                var $defaultEditableView = self.dom.$viewCarousel;
                if (self.dom.$viewSwitcher.find('.editable-view:visible').first().hasClass('view-switcher-vertical')) {
                    $defaultEditableView = self.dom.$viewVertical;
                }
                self.dom.$defaultEditableView = $defaultEditableView;
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
                imageUrl = $item.attr('data-preview') || '';

                $('<img/>', {
                    'class': 'previewable-image',
                    src: imageUrl,
                    alt: ''
                }).on('click', function(){
                    if (self.itemIsVerticalView($item)) {
                        // for vertical, use preview image to always open image selector
                        self.modePreviewImageEditOrSelect($item, true);
                    } else {
                        self.modePreviewEditHandler($item);
                    }
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
                    'data-dynamic-html': '${toolPageContext.createObjectLabelHtml(content)}'
                }).on('click', function(){
                    self.modePreviewEditHandler($item);
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

                    if (self.itemIsVerticalView($item)) {
                        self.modePreviewImageEditOrSelect($item);
                    } else {
                        self.modePreviewEditHandler($item);
                    }
                    return false;
                }).appendTo($controls);
                
                // Add the item to the carousel
                self.modePreviewInitItemCarousel($item);
                
                // Create the edit container for this item below the carousel,
                // and if this item has a form already on the page move it there
                $editContainer = self.modePreviewCreateEditContainer($item);
                $item.find('> .objectInputs').appendTo($editContainer);

                // If there are validation messages in the form,
                // mark  the grid and gallery tiles to show an error state,
                // then select the item and show the edit form so user can correct the error.
                if ($editContainer.find('.message-error').length) {
                    self.modePreviewMarkError($item);
                    self.modePreviewEditHandler($item);
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
                    'data-dynamic-html': '${toolPageContext.createObjectLabelHtml(content)}'

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
             * Reposition an item by index
             */
            repositionItem: function (oldIndex, newIndex, item) {
                var self = this;
                var $item = $(item);
                var $items = self.dom.$list.find('li');

                if (!$item || newIndex == oldIndex || newIndex > $items.length) {
                    return;
                }

                // Remove active item temporarily
                $item = $item.detach();

                // Add active item in the new position
                if (newIndex === 0) {
                    // Special case add to the front of the list
                    self.dom.$list.prepend($item);
                } else {
                    if (self.itemIsVerticalView($item)) {
                        self.dom.$list.find('li.item-vertical-view').eq(newIndex - 1).after($item);
                    } else {
                        self.dom.$list.find('li').eq(newIndex - 1).after($item);
                    }
                }
            },

            /**
             * Reposition a carousel target by index
             */
            repositionCarouselTarget: function (oldIndex, newIndex) {
                var self = this;
                var $items = self.dom.$carouselTargetItems.find('.itemEdit');
                var $item = $items.eq(oldIndex);

                if (!$item) {
                    return;
                }

                if (newIndex > $items.length) {
                    return;
                }

                // Remove active item temporarily
                $item = $item.detach();

                // Add active item in the new position
                if (newIndex === 0) {
                    // Special case add to the front of the list
                    self.dom.$carouselTargetItems.prepend($item);
                } else {
                    self.dom.$carouselTargetItems.find('.itemEdit').eq(newIndex - 1).after($item);
                }
            },

            /**
             * Moves preview elements when switching between vertical and grid view
             **/
            toggleWrapPreviewSection: function(item) {
                var self = this;
                var $item = $(item);
                var $previewSection = $item.find('.item-preview');
                if (self.itemIsVerticalView($item)) {
                    $item.find('.previewable-image').appendTo($previewSection);
                    $item.find('.previewable-label').appendTo($previewSection);
                    $item.find('.previewable-controls').appendTo($previewSection);
                    $item.find('.removeButton').appendTo($previewSection);
                } else {
                    if ($previewSection) {
                        $previewSection.find('.previewable-image').appendTo($item);
                        $previewSection.find('.previewable-label').appendTo($item);
                        $previewSection.find('.previewable-controls').appendTo($item);
                        $previewSection.find('.removeButton').appendTo($item);
                    }
                }
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

                $item.addClass('state-new');
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

                // Switch to default editing view
                self.modePreviewEditHandler($item);
            },

            /**
             * Decide which is the default view to go to when editing an item
             *
             * @param {Element|jQuery object} item
             * The item to edit.
             */
            modePreviewEditHandler: function(item) {
                var self = this;
                var $item = $(item);

                if (self.dom.$defaultEditableView == self.dom.$viewCarousel || self.dom.$viewCarousel.is(":visible")) {
                    self.modePreviewEdit($item, true);
                    return;
                }
                if (self.dom.$defaultEditableView == self.dom.$viewVertical || self.dom.$viewVertical.is(":visible")) {
                    // if already under vertical view, do nothing
                    if (self.itemIsVerticalView($item)) {
                        return;
                    }
                    self.modePreviewEditVertical($item);
                }
            },

            /**
             * Decide if to open image selector or image editing ui
             * based on whether there's an image selected, unless 'select' param = true;
             *
             * @param {Element|jQuery object} item
             * The item to edit.
             * @param {Boolean} select - whether forces to open select
             */
            modePreviewImageEditOrSelect: function(item, select) {
                var $item = $(item);

                // do nothing if item is in toBeRemoved status
                if ($item.hasClass('toBeRemoved')) {
                    return;
                }
                var dataPreviewField = $item.attr('data-preview-field');
                if (dataPreviewField) {
                    var fieldName = dataPreviewField.split('/')[0];
                    var $previewEle = $item.find('> .itemEdit-vertical-container').find('> .objectInputs').find('> .inputContainer[data-field ="' + fieldName + '"]')
                        .find('> .inputSmall');
                    var $previewEdit = $previewEle.find('> .objectId-edit');
                    if ($previewEdit.css('display') == 'none' || select == true) {
                        $previewEle.find('> .objectId-select').click();
                    } else {
                        $previewEdit.click();
                    }
                }
            },

            /**
             * Edit an item for mode=preview under Vertical view
             *
             * @param {Element|jQuery object} item
             * The item to edit.
             */
            modePreviewEditVertical: function(item) {
                var self = this;
                var $item = $(item);

                if (!self.modeIsPreview()) {
                    return;
                }
                self.modePreviewShowVertical($item);
                self.verticalViewFocus($item);
            },

            /**
             * Scroll screen to focus on item under editing
             *
             * @param {Element|jQuery object} item
             * The item to focus.
             * @param {int} index
             * The index of the item under editing, used after indexer value changed
             */
            verticalViewFocus: function (item, index) {
                var self = this;
                var $item = (item == null && index >= 0) ? self.dom.$viewVertical.find('> li').eq(index) : $(item);
                $item.focus();

                // Scroll window to active item
                var scrollPosition = $item.offset().top - $item.height() / 2;
                // Check if the header is overlaying at the top
                // so we can scroll a little more to account for it
                var headerHeight = $('.toolHeader').height();
                if (headerHeight) {
                    scrollPosition = scrollPosition - headerHeight - 6;
                }
                win.scrollTo(0, scrollPosition);
            },

            /**
             * Edit an item for mode=preview under Gallery view
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
                $item.data('currentView', 'gallery');
                self.itemLoadOrMove($item, $editContainer).always(function(){
                    var $location = $editContainer.find('.richtextLazy').closest('.inputContainer');
                    self.itemLazyLoad($item, $location);

                    var editPosition;
                    var headerHeight;
                    var scrollPosition;
                    
                    // Set the active tile in the carousel
                    self.carousel.setActive( $item.index() + 1 );

                    // Switch to the carousel view
                    self.modePreviewShowCarousel($item);

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
                    $editContainer.trigger('resize');
                });
            },


            /**
             * Start editing the next or previous tile.
             *
             * @param Number direction
             * If a negative number edit the previous tile.
             * Otherwise edit the next tile.
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
             * Sync image changes and update thumbnail/preview for both gallery and vertical view
             */
            modePreviewImageChangeSync: function (item, target) {
                var self = this;
                var $item = $(item);
                var itemId = $item.find('> input[type="hidden"][name$=".id"]').val();
                var imageUrl, $target, targetName, thumbnailName;

                // Mark the tiles as changed so user can see which items have been modified.
                // Inputs are not marked as changed until the change event bubbles up to the body of the page,
                // so we put this in a timeout to hopefully force it to run after the event bubbles up.
                // An alternative would be to bind another event on the document body, but then we
                // would have no way to unbind that event.
                setTimeout(function(){
                    self.modePreviewMarkAsChanged($item);
                }, 1);

                // Get preview field from item
                // The data-preview-field contains the name of the field and the type,
                // so we need to remove the /type part
                thumbnailName = $item.attr('data-preview-field') || '';
                thumbnailName = thumbnailName.replace(/(.*)\/.*/, '$1'); // remove last / and beyond

                if (target) {
                    // If a change is made to the preview image, update the thumbnail image in the carousel and grid view
                    $target = $(target);
                } else {
                    $target = $item.find('[data-field = "' + thumbnailName + '"]').find('[data-preview]');
                }

                imageUrl = $target.attr('data-preview');
                targetName = $target.attr('name');

                // Make sure this changed item is actually used for the thumbnail
                thumbnailName = itemId + '/' + thumbnailName;

                // Make sure the preview that was changed is actually used as the thumbnail for this repeatable object.
                // This will account for cases where the repeatable object contains multiple images, or nested objects.
                if (imageUrl && targetName === thumbnailName) {
                    self.modePreviewSetThumbnail($item, imageUrl);
                }

                // Update vertical view
                $item.find('.item-preview').find('.previewable-image').attr('src', imageUrl);
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
                        'class': 'itemEdit',
                        'data-sortable-item-type': $item.attr('data-sortable-item-type')
                    }).on('change', function(event) {
                        var $target = $(event.target).closest('[data-preview]');
                        self.modePreviewImageChangeSync($item, $target);
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

                // Mark the "Grid View" link as active and the other link as inactive
                self.dom.$viewSwitcher.find('a').removeClass('view-switcher-active').filter('.view-switcher-grid').addClass('view-switcher-active');
                self.dom.$viewGrid.show();
                // toggle styling class
                self.dom.$viewVertical.find('> li').removeClass('item-vertical-view');
                self.dom.$viewGrid.find('.itemEdit-vertical-container').hide();
                if (self.dom.$indexer) {
                    self.dom.$indexer.hide();
                }
                self.dom.$viewGrid.find('> li').each(function () {
                    var $item = $(this);
                    self.toggleWrapPreviewSection($item);
                })
                self.dom.$viewCarousel.hide();
            },

            /**
             * When in preview mode, show the vertical view.
             */
            modePreviewShowVertical: function(activeItem) {
                var self = this;

                if (!self.modeIsPreview()) {
                    return;
                }

                // Mark the "Vertical View" link as active and the other link as inactive
                self.dom.$viewSwitcher.find('a').removeClass('view-switcher-active').filter('.view-switcher-vertical').addClass('view-switcher-active');
                self.dom.$viewVertical.show();
                self.dom.$viewCarousel.hide();
                self.dom.$indexer.show();
                // toggle styling class
                self.dom.$viewVertical.find('> li').addClass('item-vertical-view');

                // Load itemEdit forms
                var $activeItem = activeItem ? $(activeItem) : null;
                self.dom.$viewVertical.find('> li').each(function () {
                    var $item = $(this);
                    self.toggleWrapPreviewSection($item)

                    // load item edit forms if necessary
                    var $itemEditContainer = $item.find('.itemEdit-vertical-container');
                    if ($itemEditContainer.length == 0) {
                        $itemEditContainer = $('<div/>', {'class': 'itemEdit-vertical-container'}).on('change', function (event) {
                            self.modePreviewImageChangeSync($item);
                        }).appendTo($item);
                    }
                    var $itemEdit = $itemEditContainer.find('> .objectInputs');
                    if ($itemEdit.length == 0) {
                        $item.data("currentView", "vertical");
                        self.itemLoadOrMove($item, $itemEditContainer);
                    }

                    var dataPreviewField = $item.attr('data-preview-field');
                    if (dataPreviewField) {
                        var fieldName = dataPreviewField.split('/')[0];
                        var $previewEle = $item.find('> .itemEdit-vertical-container').find('> .objectInputs').find('> .inputContainer[data-field ="' + fieldName + '"]');
                        $previewEle.hide();
                    }

                    // hide richtext content temporarily before it's properly rendered
                    $itemEdit.find('.richtextLazy').css('color', 'transparent');
                    $itemEditContainer.show();

                    if (!$activeItem && ($item.hasClass('state-changed') || $item.hasClass('state-new'))) {
                        $activeItem = $item;
                    }
                });

                // focus on first changed or newly-added item, or the first item
                if (!$activeItem) {
                    $activeItem = self.dom.$viewVertical.find('> li').first();
                }

                if ($activeItem) {
                    var $rteLazy = $activeItem.find('.richtextLazy');
                    if ($rteLazy.length > 0) {
                        self.itemLazyLoad($activeItem, $rteLazy.closest('.inputContainer'));
                        $win.resize();
                    }
                    self.verticalViewFocus($activeItem);
                }

                self.lazyLoad();
            },

            /**
             * Lazy loading richtext fields on the form
             * only loads when user scrolls to that item and stops for 100 ms
             * Load the scrolled-to items three at a time
             * */
            lazyLoad: function () {
                var self = this;
                if (self.dom.$viewVertical.is(':visible') && self.dom.$viewVertical.find('> li').find('.richtextLazy').length > 0) {
                    $win.scroll(function () {
                        clearTimeout($.data(this, "scrollCheck"));
                        $.data(this, "scrollCheck", setTimeout(function () {
                            var len = self.dom.$viewVertical.find('> li').length;
                            var firstInView = self.dom.$viewVertical.find('> li').filter(self.isToLazyLoad).first().index();
                            self.dom.$viewVertical.find('> li').slice(firstInView, Math.min(len, firstInView + 3))
                                .each(function () {
                                    var $item = $(this);
                                    if (self.itemIsVerticalView($item)) {
                                        var $location = $item.find('.itemEdit-vertical-container').find('.richtextLazy').closest('.inputContainer');
                                        self.itemLazyLoad($item, $location);
                                    }
                                })
                        }, 200));
                    })
                }
            },

            /**
             * Checks if item is in view and has richtext input fields that needs to be rendered
             * Used by lazyLoad
             * */
            isToLazyLoad: function () {
                var docViewTop = $win.scrollTop();
                var docViewBottom = docViewTop + $win.height();

                var elemTop = $(this).offset().top;
                var elemBottom = elemTop + $(this).height();
                var $rteLazy = $(this).find('.richtextLazy');
                return ((elemBottom <= docViewBottom) && (elemTop >= docViewTop)) && $rteLazy.length > 0;
            },

            /**
             * Loads richtext tool bar and some clean up
             */
            itemLazyLoad: function (item, location) {
                var $item = $(item);
                var $location = location ? $(location) : $item;
                var $input = $item.find('> input[data-form-fields-url]');
                //remove attr so it won't be loaded again
                $input.removeAttr('data-form-fields-url');
                //remove value so changes can be detected
                $input.val('');

                $location.find('.richtextLazy').toggleClass('richtextLazy, richtext');
                $location.trigger('create');
            },
            /**
             * When in preview mode, show the carousel view.
             */
            modePreviewShowCarousel: function(activeItem) {

                var self = this;
                if (!self.modeIsPreview() || self.dom.$viewCarousel.is(':visible')) {
                    return;
                }

                // Mark the "Gallery View" link as active and the other links as inactive
                self.dom.$viewSwitcher.find('a').removeClass('view-switcher-active').filter('.view-switcher-gallery').addClass('view-switcher-active');
                if (self.dom.$indexer) {
                    self.dom.$indexer.hide();
                }
                self.dom.$viewGrid.hide();
                self.dom.$viewVertical.hide();
                self.dom.$viewCarousel.show();

                self.dom.$viewVertical.find('> li').removeClass('item-vertical-view');
                // Load or move all input forms for each item and go to first active item, or first changed/added item;
                var $activeItem = activeItem ? $(activeItem) : null;

                if (!$activeItem) {
                    $activeItem = self.dom.$list.find('> li.state-changed, > li.state-new')[0];
                }
                if ($activeItem) {
                    self.modePreviewEdit($activeItem, true);
                }

                // In some cases carousel update doesn't work if carousel is hidden,
                // so we'll call update whenever we show the carousel to ensure
                // it is displaying everything correctly
                self.carousel.update();

                // If no tile is active in the carousel, make the first one active
                // so there will be an edit form underneath
                if (self.carousel.getActive() === 0) {
                    // Make the first tile active and show the edit form
                    self.modePreviewEditNext();
                }
                
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

// Set filename for debugging tools to allow breakpoints even when using a cachebuster
//# sourceURL=jquery.repeatable.js
