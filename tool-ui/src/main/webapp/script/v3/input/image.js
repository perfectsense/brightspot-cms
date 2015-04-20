/* jshint browser:true, jquery:true, unused:true, undef:true */
/* global Pixastic, define */

define([
    'jquery',
    'bsp-utils',
    'pixastic/pixastic.core',
    'pixastic/actions/blurfast',
    'pixastic/actions/brightness',
    'pixastic/actions/crop',
    'pixastic/actions/desaturate',
    'pixastic/actions/fliph',
    'pixastic/actions/flipv',
    'pixastic/actions/invert',
    'pixastic/actions/rotate',
    'pixastic/actions/sepia',
    'pixastic/actions/sharpen'
], function($, bsp_utils) {
    
    var imageEditorUtility;

    // Singleton object for the image editor.
    //
    // Do not directly use this object - instead create your own instance using Object.create():
    //   myImageEditor = Object.create(imageEditorUtility);
    //   myImageEditor.init(element, settings);

    imageEditorUtility = {

        /**
         * The data for each image size comes over in multiple different hidden inputs.
         * This is an array of the hidden input names.
         */
        inputNames: 'x y width height texts textSizes textXs textYs textWidths'.split(' '),

        
        /**
         * The text overlay information comes over in a single string.
         * Split on this delimiter to separate the data.
         */
        textDelimiter: 'aaaf7c5a9e604daaa126f11e23e321d8',

        
        /**
         * Each size will have an entry in this object.
         * Key = name of the size
         * Value = an object containing information about the size such as a description,
         * a jquery object containing the inputs, etc.
         *
         * @example
         * {
         *   'thumb_109x73': {description:'Thumbnail 109 x 73', inputs:{}},
         *   'promo_177x118' {description:'Small Promo 177 x 118', inputs:{}}
         * }
         */
        sizeInfos: {},


        /**
         * We want to group together sizes that have approximately the same aspect ratio.
         * Key = the aspect ratio, or the name of an individual size
         * Value = an object that contains the element for the group, and a sizeInfos object
         * with all the sizes in the group.
         *   
         * @example
         * {
         *   '1.5': {
         *     $element: e,
         *     $sizeBox: e,
         *     sizeInfos: {}
         *   }
         * }
         */
        sizeGroups: {},

        
        /**
         * Names for all the tabs.
         */
        tabNames: {
            image: 'Focus',
            edit: 'Edit',
            sizes: 'Sizes & Texts',
            hotspots: 'Hotspots'
        },

        
        /**
         * Call to initialize the image editor.
         *
         * @param Element element
         * The element that contains the input information.
         * This element must contain the various HTML elements that will be used by the image editor.
         *
         * @param Object settings
         * And object of key value pairs to override the image editor default settings.
         */
        init: function(element, settings) {

            var self = this;

            // Save the main element for future use
            self.$element = $(element);

            // Add any settings to the object
            if (settings) {
                $.extend(self, settings);
            }

            // Get other stuff from the DOM
            self.initDOM().done(function() {
            
                self.tabsInit();

                self.focusInit();
                self.editInit();
                self.sizesInit();
                self.textInit();
                self.hotspotInit();

                // Process all current adjustments on the image
                self.adjustmentProcess();

                self.tabsReady();
                
                // Periodically check if edit inputs have changed
                // TODO: need to turn this on only when the edit tab is selected?
                self.adjustmentProcessTimerStart();
            });
        },

        
        /**
         * Find stuff on the page and save it for future reference.
         */
        initDOM: function() {

            var dom, $el, scale, self;
            
            self = this;
            
            $el = self.$element;
            
            // Set up an object to hold all dom information
            dom = {};
            self.dom = dom;

            dom.$form = $el.closest('form');

            // Find the image. Note this will be removed from the DOM and
            // replaced by an adjusted image.
            dom.$image = $el.find('.imageEditor-image img');
            dom.imageSrc = dom.$image.attr('src');

            // Create a clone of the image so we always have a copy of the original image.
            dom.$imageClone = dom.$image.clone();
            dom.imageClone = dom.$imageClone[0];

            // Get the data-scale attribute to use for hotspot
            scale = dom.$image.attr('data-scale');
            if (scale === undefined || scale === '') {
                scale = 1.0;
            }
            self.scale = scale;

            dom.$aside = $el.find('> .imageEditor-aside');
            dom.$tools = $el.find('.imageEditor-tools ul');
            dom.$edit = $el.find('.imageEditor-edit');

            dom.$sizes = $el.find('.imageEditor-sizes');
            dom.$sizesTable = dom.$sizes.find('table');
            
            // The data-name attribute is used to create new hidden inputs when needed
            self.dataName = $el.closest('.inputContainer').attr('data-name');

            // Return a promise that can be used to run more code after the image size is determined
            return self.getImageSize(dom.$image).done(function(width, height) {
                self.dom.imageCloneWidth = width;
                self.dom.imageCloneHeight = height;
            });
        },

        
        //--------------------------------------------------
        // TABS
        //--------------------------------------------------

        /**
         */
        tabsInit: function() {
            
            var self;

            self = this;
            
            self.dom.tabs = {};
        },

        
        /**
         */
        tabsCreate: function(tabKey, className) {
            
            var self;

            self = this;

            self.dom.tabs = self.dom.tabs || {};

            // Make sure this tab is defined in the tabNames object
            if (self.tabNames[tabKey]) {

                // Create a div for the tab but don't add it to the DOM yet
                self.dom.tabs[tabKey] = $('<div/>', {
                    'class': className || '',
                    'data-tab': self.tabNames[tabKey],
                    'style': 'position:relative'
                });
            }

        },

        
        /**
         * Call this after all tabs have been created and we're ready to go.
         */
        tabsReady: function() {
            
            var self;

            self = this;
            
            // Now add all the tabs to the main element
            $.each(self.dom.tabs, function(){
                var $tab = $(this);
                $tab.appendTo(self.$element);
            });
            
            // Finally, add "tabber" class to the container to trigger the tab plugin to run
            self.$element.addClass('tabbed');

            // Immediately run the tabbed plugin because it doesn't seem to run when in a popup
            self.$element.tabbed();
        },

        
        //--------------------------------------------------
        // IMAGE ADJUSTMENTS
        //--------------------------------------------------
        
        /**
         * Sets up the "Edit Image" controls for modifying image adjustments.
         */
        editInit: function() {
            
            var $brightness, $contrast, $filters, $rotateFlip, self;

            self = this;
            
            self.tabsCreate('edit');

            // Create an image in the edit tab to show the image changes
            self.dom.$editImage = self.dom.$imageClone.clone();

            // Save a copy of the original image so we will always have
            // it after adjustments are made and self.dom.$image is overwritten.
            self.dom.$originalImage = self.dom.$editImage;

            $('<div/>', {
                'class': 'imageEditor-image',
                'html': self.dom.$editImage
            }).appendTo(self.dom.tabs.edit);

            // Move the imageEditor-aside element into this tab
            self.dom.$aside.appendTo(self.dom.tabs.edit);
            
            // Create a place to put some hidden inputs
            self.dom.$editInputs = $('<div/>').appendTo(self.dom.$edit);

            // Reformat the controls for editing the image

            self.dom.$edit.find('table').hide();

            $brightness = $('<div class="imageEditor-adjustment"><div class="imageEditor-adjustment-heading">Brightness <span class="imageEditor-value">0</span></div></div>').appendTo(self.dom.$edit);
            self.dom.$editBrightnessValue = $brightness.find('.imageEditor-value');
            self.dom.$editBrightness = self.dom.$edit.find('table :input[name$=".brightness"]')
                .on('change input', function(){
                    self.dom.$editBrightnessValue.text($(this).val());
                })
                .trigger('change')
                .appendTo($brightness);

            $contrast = $('<div class="imageEditor-adjustment"><div class="imageEditor-adjustment-heading">Contrast <span class="imageEditor-value">0</span></div></div>').appendTo(self.dom.$edit);
            self.dom.$editContrastValue = $contrast.find('.imageEditor-value');
            self.dom.$editContrast = self.dom.$edit.find('table :input[name$=".contrast"]')
                .on('change input', function(){
                    self.dom.$editContrastValue.text($(this).val());
                })
                .trigger('change')
                .appendTo($contrast);

            // Add rotate and flip controls
            
            $rotateFlip = $('<div class="imageEditor-adjustment"><div class="imageEditor-adjustment-heading">Rotate/Flip</div><ul class="imageEditor-adjustment-icons"></ul></div>')
                .appendTo(self.dom.$edit)
                .find('ul');

            $('<li/>').append(
                $('<a/>', {
                    title: 'Rotate Left',
                    text: '',
                    class: 'imageEditor-rotate-left'
                }).on('click', function(){
                    self.editRotateLeft();
                    return false;
                })
            ).appendTo($rotateFlip);
            
            $('<li/>').append(
                $('<a/>', {
                    title: 'Rotate Right',
                    text: '',
                    class: 'imageEditor-rotate-right'
                }).on('click', function(){
                    self.editRotateRight();
                    return false;
                })
            ).appendTo($rotateFlip);

            $('<li/>').append(
                $('<a/>', {
                    title: 'Flip Horizontally',
                    text: '',
                    class: 'imageEditor-flip-h'
                }).on('click', function(){
                    self.editFlipH();
                    return false;
                })
            ).appendTo($rotateFlip);
            
            $('<li/>').append(
                $('<a/>', {
                    title: 'Flip Vertically',
                    text: '',
                    class: 'imageEditor-flip-v'
                }).on('click', function(){
                    self.editFlipV();
                    return false;
                })
            ).appendTo($rotateFlip);

            // Modify the rotation input so it allows values for 180 and 270 rotation
            // (although later we will only go to 90 and 180)
            self.dom.$edit.find('table :input[name$=".rotate"]').attr({
                min: '-270',
                max: '270',
            });

            // Add filter controls for invert, sepia, grayscale
            
            $filters = $('<div class="imageEditor-adjustment"><div class="imageEditor-adjustment-heading">Filters</div><ul></ul></div>')
                .appendTo(self.dom.$edit)
                .find('ul');
            
            self.dom.$editInvert = self.dom.$edit.find('table :input[name$=".invert"]');
            $('<li/>', {
                html: $('<label>Invert</label>').prepend(self.dom.$editInvert)
            }).appendTo($filters);
            
            self.dom.$editSepia = self.dom.$edit.find('table :input[name$=".sepia"]');
            $('<li/>', {
                html: $('<label>Sepia</label>').prepend(self.dom.$editSepia)
            }).appendTo($filters);

            self.dom.$editGrayscale = self.dom.$edit.find('table :input[name$=".grayscale"]');
            $('<li/>', {
                html: $('<label>Grayscale</label>').prepend(self.dom.$editGrayscale)
            }).appendTo($filters);
            
            // Create the reset button
            self.dom.$editResetButton = $('<button/>', {
                'type': 'button',
                'text': 'Reset',
                'click': function() {
                    self.editReset();
                    return false;
                }
            }).appendTo(self.dom.$edit);

            // Trigger events whenever the adjustments are changed
            // so other code can listen for changes
            self.dom.$edit.on('change', ':input', function(){

                var inputName;
                
                // Get the name of the input without all the extra crud
                inputName = self.editParseName($(this).attr('name'));
                
                // Trigger an event and pass the input element
                // and the name of the input (like 'rotate' or 'flipH')
                self.$element.trigger('imageAdjustment', [this, inputName]);
            });
            
        },

        
        /**
         * Creates a place to store some additional hidden inputs for the edit controls.
         * These additional inputs will be destroyed and rewritten each time we change the image adjustments.
         *
         * Note some of the adjustments are checkboxes in which case the checkbox is directly modified.
         * Other adjustments are ranges (<input type="range">) in which case an additional hidden input
         * is created to hold the actual value.
         */
        editInitInputs: function() {
            
            var self;

            self = this;

            self.dom.$editInputs = $('<div/>').appendTo(self.dom.$editButton);
        },

        
        /**
         * Open the Adjustments pop up.
         */
        editOpenAdjustments: function() {
            
            var $edit, self;

            self = this;
            $edit = self.dom.$edit;
            $edit.popup('source', self.dom.$editButton).popup('open');

            // If there are any blurred areas defined, then show them on the image
            // ??? How do these get removed if the adjustments are closed?
            self.adjustmentBlurShow();
        },


        /**
         * Reset all adjustments on the image.
         */
        editReset: function() {

            var self;

            self = this;

            // Clear all the checkboxes and inputs for the adjustments
            self.dom.$edit.find(':input:not([type=button])').each(function() {
                
                var $input, name, value;
                
                $input = $(this);

                value = 0;
                
                if ($input.is(':checkbox')) {
                    value = 1;
                    $input.removeAttr('checked');
                } else {
                    value = parseFloat( $input.val() );
                    $input.val(0);
                }

                if (value !== 0) {

                    name = self.editParseName($input.attr('name'));

                    $input.trigger('change');
                    
                    // Trigger an event and pass the input element
                    // and the name of the input (like 'rotate' or 'flipH')
                    // This is used so other code can learn when the image is rotated.
                    self.$element.trigger('imageAdjustment', [$input.get(0), name]);
                }
            });

            // Remove all the blur adjustments for the image
            // Note this only appears when using the java image editor
            // TODO: move to separate blur function
            self.$element.find(".imageEditor-blurOverlay").remove();

            // Reprocess the image to remove the adjustments
            self.adjustmentProcess();
        },


        /**
         * Given in an input name like '00000149-821b-d7b5-aded-fbff734f0000/file.brightness',
         * this function returns the last part after the "." character, like 'brightness'.
         *
         * @param String name
         * The name of an input like '00000149-821b-d7b5-aded-fbff734f0000/file.brightness'
         *
         * @returns String
         * The part of the name after the last "." character, like 'brightness'
         */
        editParseName: function(name) {
            name = name || '';
            return name.replace(/.*\./, '');
        },


        editRotateLeft: function() {
            var self;
            self = this;
            self.adjustmentRotateSet(self.adjustmentRotateGet() - 90);
        },

        
        editRotateRight: function() {
            var self;
            self = this;
            self.adjustmentRotateSet(self.adjustmentRotateGet() + 90);
        },


        /**
         * Flip the current image horizontally.
         * @param Boolean flag
         */
        editFlipH: function() {
            
            var rotation, self;

            self = this;
            
            // Check the current rotation of the image.
            // If it is not rotated, or rotated 180, we will flip horizontally.
            // But if it is rotated on its side we will actually flip vertically.
            rotation = Math.abs(self.adjustmentRotateGet());
            if (rotation === 0 || rotation === 180) {
                self.adjustmentFlipHToggle();
            } else {
                self.adjustmentFlipVToggle();
            }
        },

        
        /**
         * Set or unset the "Flip Vertical" adjustment for the image.
         * @param Boolean flag
         */
        editFlipV: function() {
            var rotation, self;

            self = this;
            
            // Check the current rotation of the image.
            // If it is not rotated, or rotated 180, we will flip vertically.
            // But if it is rotated on its side we will actually flip horizontally.
            rotation = Math.abs(self.adjustmentRotateGet());
            if (rotation === 0 || rotation === 180) {
                self.adjustmentFlipVToggle();
            } else {
                self.adjustmentFlipHToggle();
            }
        },

        
        /**
         * Start repeatedly processing the image adjustments.
         */
        adjustmentProcessTimerStart: function() {
            var self;

            self = this;

            // Kill any existing timer just in case
            self.adjustmentProcessTimerStop();

            // Make sure we are still in the DOM.
            // If not we can stop looping.
            // This is used in case we are running in a popup
            // and the popup gets removed so we don't end up with multiple
            // timers running when they are no longer needed.
            if (self.$element.closest('body').length) {

                // Run the image adjustments, then when they are done delay a bit
                // and run them again. Continue until the timer is killed.
                self.adjustmentProcess().always(function(){

                    self.adjustmentProcessTimer = setTimeout(function(){
                        self.adjustmentProcessTimerStart();
                    }, 100);
                });

            } else {
                // We are no longer in the DOM,
                // so we can stop looping.
            }
        },

        
        /**
         * Stop repeatedly processing the image adjustments.
         */
        adjustmentProcessTimerStop: function() {
            
            var self;
            
            self = this;
            
            clearTimeout(self.adjustmentProcessTimer);
        },

        
        /**
         * Process the image to perform adjustments based on the adjustment inputs.
         * Only actually processes the image if the adjustment settings have changed
         * since the last time this function was called.
         *
         * @return Promise
         * Returns a promise that can be used to run additional code
         * after all the processing is done.
         */
        adjustmentProcess: function() {
            
            var operations, operationsJson, promise, self;

            self = this;

            // Get the list of operations that should be performed
            operations = self.adjustmentGetOperations();

            // Now turn the operations object into a JSON string so we
            // can compare it to previous operations that have already
            // been applied, to avoid running adjustments unnecessarily
            operationsJson = JSON.stringify(operations);
            if (operationsJson !== self.operationsPrevious) {

                // The operations are different from the previous operations,
                // so perform the processing

                // Save the operations so they can be compared later
                self.operationsPrevious = operationsJson;

                // Create hidden inputs to save all the adjustment values
                self.adjustmentSetInputs();

                // Now perform the operations and return a promise that can
                // be used to run more code after the operations have completed
                promise = self.adjustmentProcessExecuteAll(operations);

                // When all the adjustments are done replace the original image with the adjusted image
                promise.done(function(){

                    // Remove the old image from the dom unless this is the first time making an adjustment.
                    // If this is the first time, then the original image will be hidden so we don't remove it.
                    if (self.dom.$editImage !== self.dom.$originalImage) {
                        self.dom.$editImage.remove();
                    }

                    // Add the adjusted image to the dom
                    self.dom.$originalImage.hide().before( self.dom.processedImage );

                    // Save the adjusted image for later comparison
                    self.dom.$editImage = $( self.dom.processedImage );

                    // Trigger an event so other code can tell when the image changed
                    self.$element.trigger('imageUpdated', [self.dom.$editImage]);
                    
                });

            }
            
            // No operations have to be performed, so just return
            // an already resolved promise
            return $.Deferred().resolve().promise();
        },


        /**
         * After the operations object has been set up, this function executes all the image adjustments.
         * It does each operation sequentially.
         *
         * @returns Promise
         * Returns a promise that can be used to run additional code after the processing has completed.
         */
        adjustmentProcessExecuteAll: function(operations) {

            var promise, self;

            self = this;
            
            // Create an already resolved promise to start things off.
            // We will modify this promise and add a task for each adjustment.
            promise = $.Deferred().resolve().promise();

            // Create the processed image - start from a copy of the original image
            self.dom.processedImage = self.dom.$imageClone.clone().get(0);
            
            // Loop through each of the operations and perform them sequentially
            $.each(operations, function(name, value) {

                var data;

                // If the operation has a "data" parameter that means there is an array
                // of operations to perform. To keep things simple let's convert even
                // the single operations into an array then loop through them.
                data = value.data || [value];

                // Loop through the array of operations
                $.each(data, function(index, value) {

                    var previousPromise;

                    previousPromise = promise;
                    
                    // Chain a new promise to run after the last one
                    promise = previousPromise.then(function(){
                    
                        // Run a single operation.
                        // This will return a new promise that will be used to chain
                        // multiple operations one after another.
                        return self.adjustmentProcessExecuteSingle(name, value);
                    });
                });
            });
            
            // Return a promise that can be used to run additional code
            // after the processing has completed.
            return promise;
        },

        
        /**
         * Run a single adjustment operation
         *
         * @param String operationName
         * The name of the operation (used by Pixastic)
         *
         * @param Object operationValue
         * The operation values to be sent to Pixastic.
         *
         * @returns Promise
         * Returns a promise that can be used to run additional code after the processing has completed.
         * Also puts the processed image into self.dom.processedImage.
         */
        adjustmentProcessExecuteSingle: function(operationName, operationValue) {
            
            var deferred, self;

            self = this;

            deferred = $.Deferred();

            // Run the Pixastic process to create a new image, then mark the deferred as resolved when it completes
            Pixastic.process(self.dom.processedImage, operationName, operationValue, function(newImage) {

                // Save the processed image so additional operations can be performed on it
                self.dom.processedImage = newImage;
                    
                // Resolve the deferred object to indicate it is done
                deferred.resolve();
            });
            
            // Return a promise so additional code can run after the processing is done
            // to let us chain one operation after another
            return deferred.promise();
        },
        
        
        /**
         * Create hidden inputs for each of the adjustment values.
         */
        adjustmentSetInputs: function() {

            var self;

            self = this;
            
            // Remove any of the previous hidden inputs for the adjustments
            self.dom.$editInputs.empty();

            self.dom.$edit.find(':input:not([type=button])').each(function() {
                
                var $input = $(this);

                // Determine if this is a selected checkbox, or an input that is not a checkbox
                if (!$input.is(':checkbox') || $input.is(':checked')) {

                    // Create a hidden input to store the value
                    self.dom.$editInputs.append($('<input/>', {
                        'type': 'hidden',
                        'name': $input.attr('name'),
                        'value': $input.val()
                    }));
                }
            });

        },

        
        /**
         * Get the Pixastic operations needed for the image adjustment.
         *
         * @returns Object
         * An object with multiple operations that should be performed by Pixastic.
         * This object is also saved in this.operations.
         */
        adjustmentGetOperations: function() {
            
            var self;
            
            self = this;
            
            // Create a list of operations
            self.operations = {};

            // Loop through all the inputs in the edit section
            // and add operations for each one
            self.dom.$edit.find(":input:not([type=hidden])").each(function(){
                
                var $input, name, value, processFunctionName;
                
                $input = $(this);
                
                // Get the name of the input and remove the extra junk
                // to just get the part at the end like "brightness"
                name = self.editParseName($input.attr('name'));
                //name = /([^.]+)$/.exec(name);
                //name = name ? name[1] : '';

                if (!name) {
                    return;
                }

                // Get the value of the input
                value = $input.is(':checkbox') ? $input.is(':checked') : parseFloat($input.val());

                // Skip this if the checkbox is not checked, or if the value was invalid
                if (value === false || isNaN(value) || value === 0) {
                    return;
                }
                
                // Get the function that will be used to process this adjustment
                // such as self.adjustmentProcess_brightness
                processFunctionName = 'adjustmentGetOperation_' + name;
                if (!self[processFunctionName]) {
                    return;
                }

                // Call the process function and pass in the value.
                // Each function will add to the self.operations object.
                self[processFunctionName](value);
            });

            // All the operations are now in self.operations,
            // but we'll go ahead and pass them as a return value
            return self.operations;
        },


        /**
         * Add operations for changing image brightness.
         * @param Number value
         */
        adjustmentGetOperation_brightness: function(value) {
            var operations, self;
            self = this;
            operations = self.operations;
            operations.brightness = operations.brightness || {};
            operations.brightness.brightness = value * 150;
            operations.brightness.legacy = true;
        },

        
        /**
         * Add operations for changing image contrast.
         * @param Number value
         */
        adjustmentGetOperation_contrast: function(value) {
            var operations, self;
            self = this;
            operations = self.operations;
            operations.brightness = operations.brightness || { };
            operations.brightness.contrast = value < 0 ? value : value * 3;
        },


        /**
         * Add operations for flipping the image horizontally.
         */
        adjustmentGetOperation_flipH: function() {
            var operations, self;
            self = this;
            operations = self.operations;
            operations.fliph = operations.fliph || { };
        },
        

        /**
         * Add operations for flipping the image vertically.
         */
        adjustmentGetOperation_flipV: function() {
            var operations, self;
            self = this;
            operations = self.operations;
            operations.flipv = operations.flipv || { };
        },

        
        /**
         * Add operations for changing image to grayscale.
         */
        adjustmentGetOperation_grayscale: function() {
            var operations, self;
            self = this;
            operations = self.operations;
            operations.desaturate = operations.desaturate || { };
        },

        
        /**
         * Add operations for changing image to sepia.
         */
        adjustmentGetOperation_sepia: function() {
            var operations, self;
            self = this;
            operations = self.operations;
            operations.sepia = operations.sepia || { };
        },

        
        /**
         * Add operations for inverting the image.
         */
        adjustmentGetOperation_invert: function() {
            var operations, self;
            self = this;
            operations = self.operations;
            operations.invert = operations.desaturate || { };
        },

        
        /**
         * Add operations for rotating the image.
         * @param Number value
         * Rotation amount: 90, 0, -90
         */
        adjustmentGetOperation_rotate: function(value) {
            var operations, self;
            self = this;
            operations = self.operations;
            operations.rotate = operations.rotate|| { };
            operations.rotate.angle = -value;
        },
        

        /**
         * Add operations for sharpening the image.
         * @param Number value
         */
        adjustmentGetOperation_sharpen: function(value) {
            var operations, self;
            self = this;
            operations = self.operations;
            operations.sharpen = operations.sharpen || { };
            operations.sharpen.amount = value;
        },

        
        /**
         * Add operations for bluring a region of the image.
         * @param String value
         * The region to blur, a string concatenated with the 'x' character:
         * [left]x[top]x[width]x[height
         */
        adjustmentGetOperation_blur: function(value) {
            var operations, rect, self, values;
            self = this;
            operations = self.operations;
            operations.blurfast = operations.blurfast || {
                count: 0,
                data: []
            };
            values = value.split("x");
            rect = {
                left: values[0],
                top: values[1],
                width: values[2],
                height: values[3]
            };
            operations.blurfast.data[operations.blurfast.count] = {"amount" : 1.0, "rect" :rect};
            operations.blurfast.count++;
        },


        /**
         * Returns the current value of the rotate adjustment.
         * @returns Number
         * 90, -90, 0
         */
        adjustmentRotateGet: function() {

            var self, value;

            self = this;

            value = parseInt( self.dom.$edit.find(":input[name$='.rotate']").val() || 0, 10 );
            
            return value;
        },


        /**
         * Sets a new value for rotate adjustment and triggers a change event.
         * @param Number rotation
         * Number between -360 and 360
         */
        adjustmentRotateSet: function(rotation) {

            var self;

            self = this;

            // Don't go to 360, reset to zero
            if (Math.abs(rotation) >= 360) {
                rotation = 0;
            }

            // Instead of allowing -180, convert to 180 to keep things simple
            if (rotation === -180) {
                rotation = 180;
            }
            
            // Instead of allowing rotation of 270 or -270,
            // use -90 or 90
            if (rotation === 270) {
                rotation = -90;
            } else if (rotation === -270) {
                rotation = 90;
            }

            self.dom.$edit.find(":input[name$='.rotate']").val(rotation).trigger('change');
        },


        /**
         * Determines if the "flipH" adjustment is checked.
         * @returns Boolean
         */
        adjustmentFlipHGet: function() {
            
            var self, value;

            self = this;

            value = self.dom.$edit.find(":input[name$='.flipH']").is(':checked');
            
            return value;
        },


        /**
         * Set or unset the FlipH value.
         * @param Boolean flag
         */
        adjustmentFlipHSet: function(flag) {
            var self;
            self = this;
            self.dom.$edit.find(":input[name$='.flipH']").prop('checked', flag);
        },

        
        /**
         * Toggle the the FlipH value.
         */
        adjustmentFlipHToggle: function() {
            var self;
            self = this;
            self.adjustmentFlipHSet(!self.adjustmentFlipHGet());
        },

        
        /**
         * Determines if the "flipV" adjustment is checked.
         * @returns Boolean
         */
        adjustmentFlipVGet: function() {
            
            var self, value;

            self = this;

            value = self.dom.$edit.find(":input[name$='.flipV']").is(':checked');
            
            return value;
        },

        
        /**
         * Set or unset the FlipV value.
         * @param Boolean flag
         */
        adjustmentFlipVSet: function(flag) {
            var self;
            self = this;
            self.dom.$edit.find(":input[name$='.flipV']").prop('checked', flag);
        },

        
        /**
         * Toggle the the FlipH value.
         */
        adjustmentFlipVToggle: function() {
            var self;
            self = this;
            self.adjustmentFlipVSet(!self.adjustmentFlipVGet());
        },

        
        
        //--------------------------------------------------
        // BLUR ADJUSTMENT
        //--------------------------------------------------

        /**
         * TODO
         */
        adjustmentBlurShow: function() {

            var self;

            self = this;
            
            // Check if there are hidden inputs with ".blur", which will contain
            // dimensions for an overlay box to blur part of the image
            self.dom.$edit.find("input[name=\'" + self.dataName + ".blur\']").each(function(){
                var attributes;
                attributes = $(this).val().split("x");
                // TODO: change addSizeBox
                //addSizeBox(this, attributes[0], attributes[1], attributes[2], attributes[3]);
            });
        },

        
        //--------------------------------------------------
        // SIZES
        //--------------------------------------------------

        /**
         * Initialize the size selector interface, which lets the user select a group of sizes
         * then adjust the cropping on the image.
         */
        sizesInit: function() {
            
            var self;

            self = this;

            self.tabsCreate('sizes');

            // Move everything into the tab
            self.$element.children().appendTo( self.dom.tabs.sizes );

            // Create a sidebar to hold the size selector
            self.dom.$sizesAside = $('<div/>', {
                'class': 'imageEditor-aside'
            }).appendTo(self.dom.tabs.sizes);

            // Move the sizes into the sidebar
            self.dom.$sizes.appendTo(self.dom.$sizesAside);
            
            // Set up the "cover" divs that mask off the cropped areas of the image
            self.coverInit();
            
            // Extract all the info about sizes from the DOM
            // After this self.sizeInfos and self.sizeGroups should be available
            self.sizesGetSizeInfo();

            // Create a list to hold all the size groups
            self.dom.$sizeSelectors = $('<ul/>', { 'class': 'imageEditor-sizeSelectors' });

            // Loop through all the sizeGroups info and set up the inferface for selecting thumbnails
            $.each(self.sizeGroups, function(groupName, groupInfo) {

                var groupLabel, $groupElement, addBreak;

                // Create a new list item for this size group
                $groupElement = $('<li/>', {
                    'class': 'imageEditor-sizeButton'
                }).appendTo(self.dom.$sizeSelectors);

                // Save the group name on the group element, so we can retrieve it later on
                $groupElement.attr('data-group-name', groupName);
                
                // Save the group LI for later use
                groupInfo.$element = $groupElement;
                
                // We'll get the label for the group by combining all the individual size descriptions
                groupLabel = $('<span/>', {
                    'class': 'imageEditor-sizeLabel'
                }).appendTo($groupElement);
                
                // Save the group label so we can use it later when creating the "Add Text" link
                groupInfo.$groupLabel = groupLabel;

                // Loop through all the individual sizes within the group
                $.each(groupInfo.sizeInfos, function(sizeName, sizeInfo) {

                    // Determine if we need to have a line break before this label.
                    // addBreak will be undefined on the first loop so we will not add a break,
                    // but on subsequent loops it will be true.
                    if (addBreak) {
                        groupLabel.append('<br/>');
                    } else {
                        addBreak = true;
                    }

                    $('<span/>', {
                        title: sizeInfo.description + ' (' + sizeInfo.width + ' x ' + sizeInfo.height + ')',
                        html: sizeInfo.description
                    }).appendTo(groupLabel);

                });

                // Create the preview image for the group
                $('<div/>', {
                    'class': 'imageEditor-sizePreview',
                    'html': $('<img/>', {
                        'alt': groupLabel.text(),
                        'src': self.dom.imageSrc,
                        'load': function() {
                            // After the preview image loads, update the cropping based on the size info
                            self.sizesUpdatePreview(groupName);
                        }
                    })
                }).appendTo($groupElement);

                // Add click event to the size group
                $groupElement.on('click', function() {

                    // Toggle the selected state for the size group
                    if (self.sizesIsSelected(groupName)) {
                        self.sizesUnselect();
                    } else {
                        self.sizesSelect(groupName);
                    }

                    return false;
                });

                // Create a size box for the group (initially hidden)
                self.sizeBoxInit(groupName);

            });

            // Now add the size selector to the dom,
            // and hide the table that contained all the size inputs
            self.dom.$sizesTable.before(self.dom.$sizeSelectors).hide();

            // Automatically select the first size group if there is only one
            self.sizesSelectSingle();

            // Make sure scrolling within the "aside" area doesn't start scrolling the whole page
            self.scrollFix( self.dom.$sizesAside );

            // If the image is rotated, clear all the crops because the aspect ratio will be incorrect
            self.$element.on('imageAdjustment', function(event, image, adjustment){
                if (adjustment === 'rotate') {
                    self.sizesResetAll();
                }
            });

            // If the image is updated then update the sizes image
            self.$element.on('imageUpdated', $.throttle(2000, function(event, $image) {

                var $newImage;

                $newImage = $( self.cloneCanvas($image.get(0)) );
                
                self.dom.$image.before($newImage);
                self.dom.$image.remove();
                self.dom.$image = $newImage;

                // Set a flag so next time user switches to tab it will update the thumbnails
                self.sizesNeedsUpdate = true;
                
            }));

            // When user switches to the sizes tab, check if the image has been updated
            // so we can update all the thumbnail images.
            self.dom.tabs.sizes.on('tabbedShow', function(){

                // For performance reasons only do this if we previously marked that the image has changed
                if (self.sizesNeedsUpdate) {

                    self.sizesNeedsUpdate = false;

                    // For performance reasons, add a slight pause before updating,
                    // so the tabs have time to finish displaying
                    setTimeout(function() {

                        var groupName;
                    
                        // Re-select the selected group to update the display
                        groupName = self.sizesGetSelected();
                        if (groupName) {
                            self.sizesSelect(groupName);
                        }
                        
                        // Update (and re-crop) all the thumbnail images
                        self.sizesUpdatePreview();
                        
                    }, 100);
                }

            });

        },

        
        /**
         * Search the DOM to get all the size information.
         * Combine sizes with similar aspect ratios into groups.
         * When this is done you should have two sets of information:
         * self.sizeInfos and self.sizeGroups
         */
        sizesGetSizeInfo: function() {
            
            var groupsApproximate, self;

            self = this;

            // We want to group together sizes that have approximately the same aspect ratio.
            // So we will create a list of groups based on an approximate aspect ratio.
            //
            // Key = an aspect ratio
            // Value = a group name from self.sizeGroups
            //
            // When we find an aspect ratio we will create multiple keys:
            // aspect, +/- 0.1, +/- 0.2
            //
            // So there will be multiple aspect ratios listed for a single group name.
            // This is the most efficient way of finding aspect ratios that are close in size.
            //
            // Example:
            // {
            //   '1.18': 'thumb_109x73',
            //   '1.19': 'thumb_109x73',
            //   '1.2': 'thumb_109x73',
            //   '1.21': 'thumb_109x73',
            //   '1.22': 'thumb_109x73',
            // }
            groupsApproximate = { };
            
            // Loop through all the TH elements. These are the names of the sizes.
            // From there we can get to the other information about the size.
            self.dom.$sizesTable.find('th').each(function(){
                
                var group, independent, inputs, sizeAspectRatio, sizeAspectRatioApproximate,
                    sizeDescription, sizeHeight, sizeInfo, sizeName, sizeWidth, sizes, $source, $th, $tr;

                $th = $(this);
                $tr = $th.closest('tr');
                sizeName = $tr.attr('data-size-name');
                sizeDescription = $th.text();
                independent = $tr.attr('data-size-independent') === 'true';
                sizeWidth = parseFloat($tr.attr('data-size-width'));
                sizeHeight = parseFloat($tr.attr('data-size-height'));
                sizeAspectRatio = sizeWidth / sizeHeight;

                // If the width or height is zero, then this is a special case
                // and there is no fixed aspect ratio.
                // Instead the other dimension specifies the size, and the
                // width or height should be adjusted automatically.
                if (sizeWidth === 0 || sizeHeight === 0) {
                    sizeAspectRatio = 0;
                    independent = true;
                }
                
                // If we are inside a popup, only make this size selectable
                // if it a "standard image size" for the page it is on.
                $source = $th.popup('source');
                if ($source) {
                    sizes = $source.closest('.inputContainer').attr('data-standard-image-sizes');
                    if (sizes) {
                        if ($.inArray(sizeName, sizes.split(' ')) < 0) {
                            // skip this and continue with the next item in the each loop
                            return;
                        }
                    }
                }

                // Get all the hidden inputs for the size
                inputs = { };
                $.each(self.inputNames, function(index, name) {
                    inputs[name] = $tr.find(':input[name$=".' + name + '"]');
                });

                // Save the size information so we can use it later
                sizeInfo = self.sizeInfos[sizeName] = {
                    name: sizeName,
                    description: sizeDescription,
                    inputs: inputs,
                    independent: independent,
                    width: sizeWidth,
                    height: sizeHeight,
                    aspectRatio: sizeAspectRatio
                };

                // Group the sizes according to aspect ratio
                // (unless this size is marked as an independent size which should be presented on its own)
                
                if (independent) {
                    
                    // Create a new group just for this individual item
                    group = self.sizesCreateGroup(sizeName);

                } else {

                    // Calculate an approximate aspect ratio for this size, down to two decimal places
                    sizeAspectRatioApproximate = Math.floor(sizeAspectRatio * 100) / 100;

                    // If there is not an existing group for this aspect ratio create a new group
                    group = groupsApproximate['' + sizeAspectRatioApproximate ];
                    if (!group) {

                        // Create a new group for this aspect ratio
                        group = self.sizesCreateGroup( sizeAspectRatioApproximate );

                        // Also group together other aspect ratios that are nearly the same
                        // (making sure not to overwrite if already existing, not likely but just in case)
                        groupsApproximate[ sizeAspectRatioApproximate ] = group;
                        groupsApproximate[sizeAspectRatioApproximate - 0.02] = groupsApproximate[sizeAspectRatioApproximate - 0.02] || group;
                        groupsApproximate[sizeAspectRatioApproximate - 0.01] = groupsApproximate[sizeAspectRatioApproximate - 0.01] || group;
                        groupsApproximate[sizeAspectRatioApproximate + 0.01] = groupsApproximate[sizeAspectRatioApproximate + 0.01] || group;
                        groupsApproximate[sizeAspectRatioApproximate + 0.02] = groupsApproximate[sizeAspectRatioApproximate + 0.02] || group;
                    }
                }
                
                // Now add the current size to the group
                group.sizeInfos[sizeName] = sizeInfo;

            }); // END find th elements

            // Finished: self.sizeInfos and self.sizeGroups objects are now available with all sizing info
        },


        /**
         * Select a size to let the user edit it.
         *
         * @param String groupName
         */
        sizesSelect: function(groupName) {
            
            var self;

            self = this;

            // Unselect any other size groups
            self.sizesUnselect();

            // Mark the size selector so it appears selected
            self.sizeGroups[groupName].$element.addClass('imageEditor-sizeSelected');

            // Show the size box for this group
            self.sizeBoxShow(groupName);

            // Show the text overlays for this group
            self.textSelectGroup(groupName);
        },

        
        /**
         * Unselect the currently selected size.
         */
        sizesUnselect: function() {

            var self;

            self = this;
            
            self.dom.$sizeSelectors.find('li').removeClass('imageEditor-sizeSelected');

            self.sizeBoxHide();

            // Remove the "Add Text" button
            self.textUnselect();
        },

        
        /**
         * Determine if a group of sizes is currently selected.
         *
         * @param String groupName
         *
         * @returns Boolean
         */
        sizesIsSelected: function(groupName) {
            
            var self;

            self = this;

            return self.sizeGroups[groupName].$element.hasClass('imageEditor-sizeSelected');
        },

        
        /**
         * If there is only a single size group, select it.
         */
        sizesSelectSingle: function() {
            var self;
            self = this;
            if (self.sizeGroups.length === 1) {
                $.each(self.sizeGroups, function(groupName) {
                    self.sizesSelect(groupName);
                    return false;
                });
            }
        },

        
        /**
         * Determine which size group is currently selected.
         *
         * @returns String
         * Returns the group name of the currently selected size, or a blank string if none is selected.
         */
        sizesGetSelected: function() {
            
            var self;
            self = this;
            return self.dom.$sizeSelectors.find('.imageEditor-sizeSelected').attr('data-group-name') || '';
        },

        
        /**
         * Creates a new group of sizes.
         *
         * @param String groupName
         * The name to use for the group. This can be the size name or an aspect ratio.
         */
        sizesCreateGroup: function(groupName) {
            
            var self;

            self = this;

            return(self.sizeGroups[groupName] = {
                $element: $(),
                sizeInfos: {}
            });
        },


        /**
         * Update the preview image for a single size group.
         *
         * @param String [groupName]
         * The name of the group you are resizing.
         * Leave this undefined to update all the previews.
         */
        sizesUpdatePreview: function(groupName) {
            
            var groupInfos, operations, self;

            self = this;

            if (groupName) {
                groupInfos = {};
                groupInfos[groupName] = self.sizeGroups[groupName];
            } else {
                groupInfos = self.sizeGroups;
            }

            $.each(groupInfos, function(groupName, groupInfo) {

                var bounds, height, $imageWrapper, rotation, sizeInfoFirst, width;

                // Get the group info from the group name
                // groupInfo = self.sizeGroups[groupName];

                // Get the sizeInfo from the first size in the group
                sizeInfoFirst = self.sizesGetGroupFirstSizeInfo(groupName);
            
                // Find the element wrapping the preview image
                $imageWrapper = groupInfo.$element.find('.imageEditor-sizePreview');

                // Get the crop bounds for this group, based on the original image size
                // But adjusted if we will be rotating the image
                rotation = self.adjustmentRotateGet();
                if (rotation === 90 || rotation === -90) {
                    width = self.dom.imageCloneHeight;
                    height = self.dom.imageCloneWidth;
                } else {
                    width = self.dom.imageCloneWidth;
                    height = self.dom.imageCloneHeight;
                }

                bounds = self.sizesGetSizeBounds(width, height, sizeInfoFirst);

                // Crop the image based on the current crop dimension,
                // then replace the thumbnail image with the newly cropped image

                // Get the current image adjustments (rotation, etc.) so we can also use that when cropping
                operations = self.adjustmentGetOperations();

                // Add a crop operation
                operations.crop = bounds;

                // Perform all the operations and the crop
                self.adjustmentProcessExecuteAll(operations).done(function(){
                    $imageWrapper.empty().append( self.dom.processedImage );
                });

            });


        },


        /**
         * For an image and an individual size, get the current crop dimension information.
         * This converts the crop information which is in percentages (0 - 1)
         * into actual pixel values.
         *
         * @param Number imageWidth
         * @param Number imageHeigth
         *
         * @param Object sizeInfo
         * Information about this size, including all the size inputs.
         *
         * @returns Object bounds
         * An object of size bounds, consisting of the following parameters:
         * @returns Number bounds.left
         * @returns Number bounds.top
         * @returns Number bounds.width
         * @returns Number bounds.height
         */
        sizesGetSizeBounds: function(imageWidth, imageHeight, sizeInfo) {
            
            var aspectRatio, height, left, self, top, width;

            self = this;

            left = parseFloat(sizeInfo.inputs.x.val()) || 0.0;
            top = parseFloat(sizeInfo.inputs.y.val()) || 0.0;
            width = parseFloat(sizeInfo.inputs.width.val()) || 0.0;
            height = parseFloat(sizeInfo.inputs.height.val()) || 0.0;
            aspectRatio = sizeInfo.aspectRatio;

            // Check if cropping values have been previously set
            if (width === 0 || height === 0) {

                // If no cropping values, and there is an aspect ratio for this size,
                // make the crop area as big as possible while staying within the aspect ratio
                if (aspectRatio) {
                    
                    width = imageHeight * aspectRatio;
                    height = imageWidth / aspectRatio;

                    if (width > imageWidth) {
                        width = height * aspectRatio;
                    } else {
                        height = width / aspectRatio;
                    }

                    left = (imageWidth - width) / 2;
                    top = 0;
                    
                } else {

                    // There is no aspect ratio so just select the whole image
                    left = 0;
                    top = 0;
                    width = imageWidth;
                    height = imageHeight;
                }

            } else {

                // There was a cropping value previously set,
                // so just convert from percentages to pixels
                left *= imageWidth;
                top *= imageHeight;
                width *= imageWidth;
                height *= imageHeight;
            }

            // Return as an object of pixel values
            return {
                left: left,
                top: top,
                width: width,
                height: height
            };
        },

        
        /**
         * For a size group, update all the inputs.
         *
         * @param String groupName
         * @param Object bounds
         * @param Number bounds.x
         * @param Number bounds.y
         * @param Number bounds.width
         * @param Number bounds.height
         */
        sizesSetGroupBounds: function(groupName, bounds) {

            var self, groupInfo;

            self = this;

            groupInfo = self.sizeGroups[groupName];

            $.each(groupInfo.sizeInfos, function() {
                
                var sizeInfo = this;

                $.each(bounds, function(inputName, value) {
                    sizeInfo.inputs[inputName].val(value);
                });
            });
        },


        /**
         * Clear the cropping 
         */
        sizesResetAll: function() {
            
            var self;

            self = this;

            $.each(self.sizeGroups, function(groupName) {
                self.sizesResetGroup(groupName);
            });
        },


        /**
         * Clear the cropping values for a single group.
         */
        sizesResetGroup: function(groupName) {
            
            var groupInfo, self;

            self = this;
            
            groupInfo = self.sizeGroups[groupName];

            $.each(groupInfo.sizeInfos, function() {
                
                var sizeInfo = this;

                sizeInfo.inputs.x.val('0.0');
                sizeInfo.inputs.y.val('0.0');
                sizeInfo.inputs.width.val('0.0');
                sizeInfo.inputs.height.val('0.0');
            });
        },
        
        
        /**
         * Get the first sizeInfo object for a size group.
         * @returns Object
         * The sizeInfo object for the first size in the group.
         */
        sizesGetGroupFirstSizeInfo: function(groupName) {
            var groupInfo, self, firstSize;
            self = this;
            groupInfo = self.sizeGroups[groupName];
            $.each(groupInfo.sizeInfos, function(sizeName, sizeInfo){
                firstSize = sizeInfo;
                return false; // return after first one
            });
            return firstSize;
        },

        
        /**
         * Get the aspect ratio for a group.
         * @returns Number
         * The aspect ratio of the first size in the group.
         */
        sizesGetGroupAspectRatio: function(groupName) {
            var self;
            self = this;
            return self.sizesGetGroupFirstSizeInfo(groupName).aspectRatio;
        },

        
        //--------------------------------------------------
        // COVER
        // Used to indicate the crop area for a particular size.
        // These divs darken the outside of the crop area.
        // There is only one set of cover divs, which are readjusted each time
        // a size group is selected.
        //--------------------------------------------------

        /**
         * Create the cover divs.
         */
        coverInit: function() {

            var $cover, self;

            self = this;
            
            $cover = $('<div/>', {
                'class': 'imageEditor-cover',
                'css': {
                    'display': 'none',
                    'left': '0',
                    'position': 'absolute',
                    'top': '0'
                }
            });

            self.dom.$coverTop = $cover;
            self.dom.$coverLeft = $cover.clone(true);
            self.dom.$coverRight = $cover.clone(true);
            self.dom.$coverBottom = $cover.clone(true);
            self.dom.$covers = $().add(self.dom.$coverTop).add(self.dom.$coverLeft).add(self.dom.$coverRight)
                .add(self.dom.$coverBottom).appendTo(self.dom.tabs.sizes);
        },

        
        /**
         * Update the size and position of the cover.
         *
         * @param Object bounds
         * @param Number bounds.top
         * @param Number bounds.left
         * @param Number bounds.width
         * @param Number bounds.height
         */
        coverUpdate: function(bounds) {
            
            var self, imageWidth, imageHeight, boundsRight, boundsBottom;

            self = this;
            
            imageWidth = self.dom.$image.width();
            imageHeight = self.dom.$image.height();
            boundsRight = bounds.left + bounds.width;
            boundsBottom = bounds.top + bounds.height;

            self.dom.$coverTop.css({
                'height': bounds.top,
                'width': imageWidth
            });
            self.dom.$coverLeft.css({
                'height': bounds.height,
                'top': bounds.top,
                'width': bounds.left
            });
            self.dom.$coverRight.css({
                'height': bounds.height,
                'left': boundsRight,
                'top': bounds.top,
                'width': imageWidth - boundsRight
            });
            self.dom.$coverBottom.css({
                'height': imageHeight - boundsBottom,
                'top': boundsBottom,
                'width': imageWidth
            });

            self.coverShow();
        },

        
        /**
         * Hide the cover.
         */
        coverHide: function() {
            var self;
            self = this;
            self.dom.$covers.hide();
        },

        
        /**
         * Show the cover.
         */
        coverShow: function() {
            var self;
            self = this;
            self.dom.$covers.show();
        },

        
        //--------------------------------------------------
        // SIZEBOX
        // Showing and setting the cropped area of an image.
        // The sizebox cgan be dragged by clicking the box,
        // or resized by clicking the left/top handle
        // or the bottom/right handle.
        // Each size group creates a separate size box.
        // The size box will also contain other items such as
        // text overlays and hotspots.
        //--------------------------------------------------

        /**
         * Create the size box that shows the cropped area of an image,
         * and lets the user edit the cropped area.
         *
         * @param String groupName
         * The name of the group from self.sizeGroups for this size box.
         */
        sizeBoxInit: function(groupName) {
            
            var groupInfo, self, $sizeBox, $sizeBoxTopLeft, $sizeBoxBottomRight;

            self = this;

            groupInfo = self.sizeGroups[groupName];
            
            // Create the sizebox
            $sizeBox = $('<div/>', {
                'class': 'imageEditor-sizeBox',
                'css': { 'position': 'absolute' }
            }).hide().appendTo(self.dom.tabs.sizes);

            // Save the size box along with the group info so we can access it later
            groupInfo.$sizeBox = $sizeBox;
            
            // Create the top/left sizebox handle
            $sizeBoxTopLeft = $('<div/>', {
                'class': 'imageEditor-resizer imageEditor-resizer-topLeft',
            }).appendTo($sizeBox);

            // Create the top/right sizebox handle
            $sizeBoxBottomRight = $('<div/>', {
                'class': 'imageEditor-resizer imageEditor-resizer-bottomRight',
            }).appendTo($sizeBox);

            // Set up  event handlers
            
            // Event handler to support dragging the left/top handle
            $sizeBoxTopLeft.on('mousedown', self.sizeBoxMousedownDragHandler(groupName, function(event, original, delta) {
                
                // When user drags the top left handle,
                // adjust the top and left position of the size box,
                // and adjust the width and height
                return {
                    'left': original.left + delta.constrainedX,
                    'top': original.top + delta.constrainedY,
                    'width': original.width - delta.constrainedX,
                    'height': original.height - delta.constrainedY
                };
            }));

            // Event handler to support dragging the bottom/right handle
            $sizeBoxBottomRight.on('mousedown', self.sizeBoxMousedownDragHandler(groupName, function(event, original, delta) {
                
                // When user drags the bottom right handle, adjust the width and height of the size box
                return {
                    'width': original.width + delta.constrainedX,
                    'height': original.height + delta.constrainedY
                };
            }));

            // Event handler to support moving the size box
            $sizeBox.on('mousedown', self.sizeBoxMousedownDragHandler(groupName, function(event, original, delta) {
                
                // Set the "moving" parameter to prevent the size box from being moved
                // outside the bounds of the image, and adjust the left and top position
                return {
                    'moving': true,
                    'left': original.left + delta.x,
                    'top': original.top + delta.y
                };
                
            }));
        },

        
        /**
         * Show the size box and resize it to show the crop settings
         * for a particular group size.
         *
         * @param String groupName
         */
        sizeBoxShow: function(groupName) {
            
            var bounds, self, sizeInfo;
            
            self = this;

            // Get the first sizeInfo object for this group
            sizeInfo = self.sizesGetGroupFirstSizeInfo(groupName);

            // Get the boundaries for the size box, based on the current image size on the page
            bounds = self.sizesGetSizeBounds(self.dom.$image.width(), self.dom.$image.height(), sizeInfo);
            
            self.coverUpdate(bounds);
            self.coverShow();
            
            self.sizeBoxUpdate(groupName, bounds);

            self.sizeGroups[groupName].$sizeBox.show();
        },


        /**
         * Set the bounds for the size box.
         *
         * @param String groupName
         *
         * @param Object bounds
         * Object that contains CSS settings for the size box:
         * @param Object top
         * @param Object left
         * @param Object width
         * @param Object height
         */
        sizeBoxUpdate: function(groupName, bounds) {
            var self;
            self = this;
            self.sizeGroups[groupName].$sizeBox.css(bounds);
        },

        
        /**
         * Hide all the size boxes.
         */
        sizeBoxHide: function() {
            var self;
            self = this;
            self.coverHide();
            $.each(self.sizeGroups, function(groupName, groupInfo) {
                groupInfo.$sizeBox.hide();
            });
        },

        
        /**
         * Create a mousedown handler function that lets the user drag the size box
         * or the size box handles.
         *
         * @param String groupName
         * Name of the size group that we are modifying.
         *
         * @param Function filterBoundsFunction(event, original, delta)
         * A function that will modify the bounds of the size box,
         * and adjust it according to what is being dragged. For example,
         * if the left/top handle is being dragged.
         * Ths function must return a modified bounds object.
         * Also the function can set moving:true in the bounds object if
         * the entire size box is being moved (instead of resizing the size box).
         *
         */
        sizeBoxMousedownDragHandler: function(groupName, filterBoundsFunction) {

            var mousedownHandler, self, $sizeBox;

            self = this;

            $sizeBox = self.sizeGroups[groupName].$sizeBox;
            
            mousedownHandler = function(mousedownEvent) {

                var aspectRatio, element, imageWidth, imageHeight, original, sizeBoxPosition;

                // The element that was dragged
                element = this;

                // Get the aspect ratio for this group
                aspectRatio = self.sizesGetGroupAspectRatio(groupName);

                sizeBoxPosition = $sizeBox.position();
                
                original = {
                    'left': sizeBoxPosition.left,
                    'top': sizeBoxPosition.top,
                    'width': $sizeBox.width(),
                    'height': $sizeBox.height(),
                    'pageX': mousedownEvent.pageX,
                    'pageY': mousedownEvent.pageY
                };

                imageWidth = self.dom.$image.width();
                imageHeight = self.dom.$image.height();

                // On mousedown, let the user start dragging the element
                // The .drag() function takes the following parameters:
                // (element, event, startCallback, moveCallback, endCallback)
                $.drag(element, mousedownEvent, function() {
                    
                    // This is the start callback for .drag()
                    
                }, function(dragEvent) {
                    
                    // This is the move callback for .drag()

                    var bounds, deltaX, deltaY, overflow;
                    
                    deltaX = dragEvent.pageX - original.pageX;
                    deltaY = dragEvent.pageY - original.pageY;

                    // Use the filterBoundsFunction to adjust the value of the bounds
                    // based on what is being dragged.
                    bounds = filterBoundsFunction(dragEvent, original, {
                        'x': deltaX,
                        'y': deltaY,
                        'constrainedX': aspectRatio ? Math.max(deltaX, deltaY * aspectRatio) : deltaX,
                        'constrainedY': aspectRatio ? Math.max(deltaY, deltaX / aspectRatio) : deltaY
                    });

                    // Fill out the missing bounds
                    bounds = $.extend({}, original, bounds);

                    // The sizebox can be resized or moved.
                    // The filterBoundsFunction should have told us if it is being moved.
                    if (bounds.moving) {

                        // The sizebox is being moved,
                        // but we can't let it move outside the range of the image.

                        if (bounds.left < 0) {
                            bounds.left = 0;
                        }

                        if (bounds.top < 0) {
                            bounds.top = 0;
                        }

                        overflow = bounds.left + bounds.width - imageWidth;
                        if (overflow > 0) {
                            bounds.left -= overflow;
                        }

                        overflow = bounds.top + bounds.height - imageHeight;
                        if (overflow > 0) {
                            bounds.top -= overflow;
                        }

                    } else {
                        
                        // We're not moving the sizebox so we must be resizing.
                        // We still need to make sure we don't resize past the boundaries of the image.

                        if (bounds.width < 10 || bounds.height < 10) {
                            if (aspectRatio > 1.0) {
                                bounds.width = aspectRatio * 10;
                                bounds.height = 10;
                            } else {
                                bounds.width = 10;
                                bounds.height = aspectRatio ? (10 / aspectRatio) : 10;
                            }
                        }

                        // Check if the box extends past the left
                        if (bounds.left < 0) {
                            bounds.width += bounds.left;
                            if (aspectRatio) {
                                bounds.height = bounds.width / aspectRatio;
                                bounds.top -= bounds.left / aspectRatio;
                            }
                            bounds.left = 0;
                        }

                        // Check if the box extends above the top
                        if (bounds.top < 0) {
                            bounds.height += bounds.top;
                            if (aspectRatio) {
                                bounds.width = bounds.height * aspectRatio;
                                bounds.left -= bounds.top * aspectRatio;
                            }
                            bounds.top = 0;
                        }

                        // Check if the box extends past the right
                        overflow = bounds.left + bounds.width - imageWidth;
                        if (overflow > 0) {
                            bounds.width -= overflow;
                            if (aspectRatio) {
                                bounds.height = bounds.width / aspectRatio;
                            }
                        }

                        // Check if the box extends past the bottom
                        overflow = bounds.top + bounds.height - imageHeight;
                        if (overflow > 0) {
                            bounds.height -= overflow;
                            if (aspectRatio) {
                                bounds.width = bounds.height * aspectRatio;
                            }
                        }
                    }

                    // Now that the bounds have been sanitized,
                    // update the sizebox display
                    self.coverUpdate(bounds);
                    self.sizeBoxUpdate(groupName, bounds);

                    // Trigger an event to tell others the size box has changed size
                    if (!bounds.moving) {
                        $sizeBox.trigger('sizeBoxResize');
                    }

                }, function() {

                    var sizeBoxHeight, sizeBoxPosition, sizeBoxWidth;
                    
                    // .drag() end callback

                    // Now that we're done dragging, update the size box
                    
                    sizeBoxPosition = $sizeBox.position();
                    sizeBoxWidth = $sizeBox.width();
                    sizeBoxHeight = $sizeBox.height();

                    // Set the hidden inputs to the current bounds.
                    self.sizesSetGroupBounds(groupName, {
                        x: sizeBoxPosition.left / imageWidth,
                        y: sizeBoxPosition.top / imageHeight,
                        width: sizeBoxWidth / imageWidth,
                        height: sizeBoxHeight / imageHeight // sizeBoxWidth / aspectRatio / imageHeight
                    });
                    
                    // Update the preview image thumbnail so it will match the new crop values
                    self.sizesUpdatePreview(groupName);

                    // Trigger an event to tell others the size box has changed size
                    $sizeBox.trigger('sizeBoxResize');
                });

                return false;
            };

            return mousedownHandler;
        },

        
        //--------------------------------------------------
        // FOCUS FOR CROPPING
        //--------------------------------------------------

        /**
         * Initialize the "click to set focus" functionality.
         */
        focusInit: function() {

            var self, focusMessage;

            self = this;

            self.tabsCreate('image', 'imageEditor-focus');

            // Create an image in the hotspot tab to show the hotspots
            // Note this image will need to be kept in sync with image changes
            // to flip and rotate the original image
            self.dom.$focusImage = self.dom.$imageClone.clone();
            $('<div/>', {
                'class': 'imageEditor-image',
                'html': self.dom.$focusImage
            }).appendTo(self.dom.tabs.image);

            // Create a sidebar to hold a message
            self.dom.$focusAside = $('<div/>', {
                'class': 'imageEditor-aside',
            }).appendTo(self.dom.tabs.image);

            focusMessage = '<p>Click inside the image to set a focus point for all image sizes.</p>';
            self.dom.$focusMessage = $('<div/>', {
                'class': 'imageEditor-focus-message',
                'html': focusMessage
            }).appendTo(self.dom.$focusAside);

            // If the image is updated then update the focus image
            self.$element.on('imageUpdated', function(event, $image) {

                var $newImage;

                $newImage = $( self.cloneCanvas($image.get(0)) );
                
                self.dom.$focusImage.before($newImage);
                self.dom.$focusImage.remove();
                self.dom.$focusImage = $newImage;
            });

            // Add click event to the main image
            
            // The image might be removed and replaced, so we can't put a click event on the image itself.
            // Add the click event on the element wrapping the image because that is not removed.

            self.dom.$focusImage.parent().on('click', 'img,canvas', function(event) {

                var focus, $image, originalAspect, originalHeight, originalWidth;

                $image = $(this);

                // Figure out the original aspect ratio of the image
                originalWidth = $image.width();
                originalHeight = $image.height();
                originalAspect = {
                    width: originalWidth,
                    height: originalHeight
                };

                // Get the position that was clicked
                focus = self.getClickPositionInElement($image, event);

                // if (!window.confirm('Set all sizes to focus on this point?')) {
                //     return;
                // }

                // Go through all sizes to get the aspect ratio of each
                $.each(self.sizeGroups, function(groupName) {

                    var aspect, crop, sizeInfo;

                    // Get the aspect ratio values for this size group
                    sizeInfo = self.sizesGetGroupFirstSizeInfo(groupName);
                    aspect = {
                        'width': sizeInfo.width,
                        'height': sizeInfo.height
                    };

                    // Calculate the crop for this aspect ratio
                    crop = self.focusGetCrop({
                        x: focus.xPercent,
                        y: focus.yPercent
                    }, originalAspect, aspect);

                    // Set the cropping for this size group
                    self.sizesSetGroupBounds(groupName, crop);
                    self.sizesUpdatePreview(groupName);
                });

                // When switching to sizes tab, update the thumbnails
                self.sizesNeedsUpdate = true;

                if (!self.dom.$focusPoint) {
                    self.dom.$focusPoint = $('<div/>', {'class':'imageEditor-focus-point'}).appendTo(self.dom.$focusImage.parent());
                }
                self.dom.$focusPoint.css({left:(focus.xPercent * 100) + '%', top:(focus.yPercent * 100) + '%'});

                self.$element.trigger('change');
            });

/***
            // Add floating tooltip to display focus position
            self.dom.$focusTooltip = $('<div/>', {
                'class':'imageEditor-focus-tooltip',
                'text': 'Click to focus all sizes on this point.'
            }).hide().appendTo(document.body);

            self.dom.$focusImage.parent().on('mouseover', function(){
                self.dom.$focusTooltip.show();
            }).mousemove(function(e) {
                var position;
                //position = self.getClickPositionInElement(this, e);
                self.dom.$focusTooltip.css({left:e.pageX + 10, top: e.pageY + 10});
            }).mouseout(function(){
                self.dom.$focusTooltip.hide();
            });
**/            
        },


        /**
         * Return cropping data based on a centering focus point in an image, and a target image size.
         *
         * @param Object focusPoint
         *
         * @param Number focusPoint.x
         * Value between 0 and 1 to indicate where on the original image the focus point was set.
         *
         * @param Number focusPoint.y
         * Value between 0 and 1 to indicate where on the original image the focus point was set.
         *
         * @param Object originalSize
         *
         * @param Number originalSize.width
         * The width of the original image.
         *
         * @param Number originalSize.height
         * The height of the original image.
         *
         * @param Object targetSize
         *
         * @param Number targetSize.width
         * The width of the target image.
         *
         * @param Number targetSize.height
         * The height of the target image.
         *
         * @returns Object crop
         *
         * @returns Number crop.x
         * Value between 0 and 1 to indicate the top left point for the crop.
         *
         * @returns Number crop.y
         * Value between 0 and 1 to indicate the top left point for the crop.
         *
         * @returns Number crop.width
         * Value between 0 and 1 to indicate the width of the crop (in relation to the original image).
         *
         * @returns Number crop.height
         * Value between 0 and 1 to indicate the height of the crop (in relation to the original image).
         */
        focusGetCrop: function(focusPoint, originalSize, targetSize) {

            var originalAspect;
            var targetAspect;
            var crop;
            var adjustedValue;
            var adjustedDifference;
            var adjustedPercentage;
            var focusDifference;
            
            // Set up crop return value
            crop = {
                x: 0,
                y: 0,
                width: 1,
                height: 1
            };

            // Get the aspect ratio of both the original and target images
            // so we can determine which part of the image needs to be cropped.
            originalAspect = originalSize.width / originalSize.height;
            if (targetSize.width === 0 || targetSize.height === 0) {
                targetAspect = 0;
            } else {
                targetAspect = targetSize.width / targetSize.height;
            }

            if (targetAspect === 0) {
                
                // There is no aspect ratio restraint,
                // so leave the crop return value covering the whole image.
                
            } else if (originalAspect > targetAspect) {
                
                // We need to crop the WIDTH because the target aspect ratio has less width

                // Determine what the width should be to maintain the aspect ratio
                adjustedValue = originalSize.height * targetAspect;

                // Determine how much we would need to crop to maintain the aspect radio
                adjustedDifference = originalSize.width - adjustedValue;

                // Now convert that into a percentage of the original image size,
                // a number between 0 and 1
                adjustedPercentage = adjustedDifference / originalSize.width;

                crop.width = 1 - adjustedPercentage;
                crop.x = adjustedPercentage / 2;

                // Adjust the crop position so it is centered on the focus point
                focusDifference = crop.x - (0.5 - focusPoint.x);

                if (focusDifference < 0) {
                    focusDifference = 0;
                } else if (focusDifference + crop.width > 1) {
                    focusDifference = 1 - crop.width;
                }

                crop.x = focusDifference;
                
            } else if (originalAspect < targetAspect) {
                
                // We need to crop the HEIGHT because the target aspect ratio has less height
                
                // Determine what the width should be to maintain the same aspect ratio
                adjustedValue = originalSize.width / targetAspect;

                // Determine how much we would need to shave off each side to maintain the aspect radio
                adjustedDifference = originalSize.height - adjustedValue;

                // Now convert that into a percentage of the original image size,
                // a number between 0 and 1
                adjustedPercentage = adjustedDifference / originalSize.height;

                crop.height = 1 - adjustedPercentage;
                crop.y = adjustedPercentage / 2;

                // Adjust the crop position so it is centered on the focus point
                focusDifference = crop.y - (0.5 - focusPoint.y);

                if (focusDifference < 0) {
                    focusDifference = 0;
                } else if (focusDifference + crop.height > 1) {
                    focusDifference = 1 - crop.height;
                }

                crop.y = focusDifference;
            }

            return crop;
        },

        //--------------------------------------------------
        // SIZES - TEXT OVERLAYS
        //--------------------------------------------------

        /**
         * Initialize all the text overlays.
         */
        textInit: function() {

            var self;

            self = this;

            // Use a number as a unique key in the textInfo object.
            // The items are not ordered because we need the flexibility to easily add and remove texts.
            self.textInfoIndex = 1;

            // Create text overlays within the size box for each group
            $.each(self.sizeGroups, function(groupName, groupInfo) {

                // Get the text info from the hidden inputs
                groupInfo.textInfos = self.textGetTextInfo(groupName);

                // Create the text overlays
                $.each(groupInfo.textInfos, function(textKey) {
                    self.textOverlayAdd(groupName, textKey);
                });

                // Set up resize listeners on the sizebox
                // to resize the text overlays if the sizebox changes
                groupInfo.$sizeBox.on('sizeBoxResize', function(){
                    self.textOverlaySetFont(groupName);
                });
            });

            // Set up a form submit handler to copy all the rich text editor content back into the hidden variables
            self.dom.$form.submit(function() {
                $.each(self.sizeGroups, function(groupName) {
                    self.textSetTextInfo(groupName);
                });
            });

        },


        /**
         */
        textSelectGroup: function(groupName) {

            var self;

            self = this;

            // Create the "Add Text" button within the group
            self.textButtonCreate(groupName);

            // Resize the texts within the group
            self.textOverlaySetFont(groupName);
        },


        /**
         * Removes the "Add Text" link from within all groups.
         */
        textUnselect: function() {
            
            var self;

            self = this;

            // Remove the "Add Text" link
            self.textButtonRemove();
            
        },

        
        /**
         * Create the "Add Text" button and add it to the group selector.
         * Set up an event to add a new text when the button is clicked.
         *
         * @param String groupName
         */
        textButtonCreate: function(groupName) {
            
            var self;

            self = this;

            $('<a/>', {
                'class': 'imageEditor-addTextOverlay',
                'text': 'Add Text',
                'click': function() {
                    self.textAdd(groupName);
                    return false;
                }
            }).insertAfter( self.sizeGroups[groupName].$groupLabel );
        },

        
        /**
         * Remove the 'Add Text' button from all size groups.
         */
        textButtonRemove: function() {
            var self;
            self = this;
            self.$element.find('.imageEditor-addTextOverlay').remove();
        },

        
        /**
         * Get the text information for a group from the hidden inputs,
         * then transform it from multiple delimited strings into more usable javascript format.
         *
         * @param String groupName
         * Name of the sizes group that contains the text.
         * 
         * @returns Object textInfo
         * @returns Object textInfo[n]
         * @returns String textInfo[n].text
         * @returns Number textInfo[n].x
         * @returns Number textInfo[n].y
         * @returns Number textInfo[n].width
         * @returns Number textInfo[n].size
         */
        textGetTextInfo: function(groupName) {

            var groupInfo, self, sizeInfo, textInfo, texts, textSizes, textWidths, textXs, textYs;

            self = this;

            groupInfo = self.sizeGroups[groupName];
            sizeInfo = self.sizesGetGroupFirstSizeInfo(groupName);
            
            // If there are no texts return an empty array
            if (!sizeInfo.inputs.texts.val()) {
                return {};
            }
            
            // Convert the delimited strings to arrays of values
            texts = sizeInfo.inputs.texts.val().split(self.textDelimiter);
            textXs = sizeInfo.inputs.textXs.val().split(self.textDelimiter);
            textYs = sizeInfo.inputs.textYs.val().split(self.textDelimiter);
            textWidths = sizeInfo.inputs.textWidths.val().split(self.textDelimiter);
            textSizes = sizeInfo.inputs.textSizes.val().split(self.textDelimiter);

            textInfo = {};
            
            // Now loop through the individual arrays and add them to the textInfo object.
            $.each(texts, function(index) {
                
                // Skip the first item because the delimited string starts with the delimeter
                // and we end up with an empty item as the first array item.
                if (index === 0) {
                    return;
                }

                textInfo[ self.textInfoIndex++ ] = {
                    text: texts[index],
                    x: textXs[index],
                    y: textYs[index],
                    width: textWidths[index],
                    size: textSizes[index],
                };
            });

            return textInfo;
        },


        /**
         * Save text information into the hidden variables for all the sizes within a group.
         *
         * @param String groupName
         * Name of the sizes group to update.
         */
        textSetTextInfo: function(groupName) {
            
            var self;

            self = this;

            // Loop through all the sizes in the group because we need to update
            // hidden inputs in each one.
            $.each(self.sizeGroups[groupName].sizeInfos, function(sizeName, sizeInfo) {

                var texts, textXs, textYs, textWidths, textSizes;

                texts = '';
                textXs = '';
                textYs = '';
                textWidths = '';
                textSizes = '';
                
                // Loop  through all the text blocks within this group
                $.each(self.sizeGroups[groupName].textInfos, function(textInfoKey, textInfo) {

                    var fontSize, $rteInput;

                    // Update the text from the rich text editor
                    $rteInput = textInfo.$textOverlay.find('.imageEditor-textOverlayInput');
                    if ($rteInput.length) {
                        textInfo.text = $rteInput.val();
                    }
                    
                    texts += self.textDelimiter + textInfo.text;
                    textXs += self.textDelimiter + parseFloat(textInfo.x).toFixed(3);
                    textYs += self.textDelimiter + parseFloat(textInfo.y).toFixed(3);
                    textWidths += self.textDelimiter + parseFloat(textInfo.width).toFixed(3);

                    // Get the font size for the text overlay CSS, but default to something reasonable if it wasn't found
                    fontSize = textInfo.originalFontSize || 16;

                    // Save the scaling factor for the font size based on the height of this size.
                    // If height is zero then use the width instead.
                    textInfo.size = fontSize / (sizeInfo.height ? sizeInfo.height : sizeInfo.width);

                    textSizes += self.textDelimiter + textInfo.size.toFixed(3);
                });

                // Save the concatenated delimted strings to the hidden variables
                sizeInfo.inputs.texts.val(texts || '');
                sizeInfo.inputs.textXs.val(textXs || '');
                sizeInfo.inputs.textYs.val(textYs || '');
                sizeInfo.inputs.textWidths.val(textWidths || '');
                sizeInfo.inputs.textSizes.val(textSizes || '');
            });
        },

        
        /**
         * Create a new text for a size group.
         *
         * @param String groupName
         */
        textAdd: function(groupName) {

            var groupInfo, self, textInfoKey;

            self = this;

            groupInfo = self.sizeGroups[groupName];
            
            // Create a new text object
            textInfoKey = self.textInfoIndex++;
            
            groupInfo.textInfos[textInfoKey] = {
                text:'',
                x:0.25,
                y:0.25,
                width:0.5,
                size:0
            };

            // Create a new text overlay and focus on it.
            // Note this will also update the hidden input values.
            self.textOverlayAdd(groupName, textInfoKey, true);
        },


        /**
         * Create a new text overlay on top of the image.
         *
         * @param String groupName
         * The size group to which the text belongs.
         *
         * @param String textInfoKey
         * The key that indexes into the selectedGroupTextInfo object to give the textInfo.
         * 
         * @param Boolean focus
         * Set to true if we should focus on the text input after creating the text.
         */

        textOverlayAdd: function(groupName, textInfoKey, focus) {

            var groupInfo, self, $sizeBox, textInfo, $textOverlay, $textOverlayInput, $textOverlayToolbar;

            self = this;

            groupInfo = self.sizeGroups[groupName];

            textInfo = groupInfo.textInfos[textInfoKey];
            if (!textInfo) {
                return;
            }

            $sizeBox = groupInfo.$sizeBox;

            $textOverlay = $('<div/>', {
                'class': 'imageEditor-textOverlay',
                'css': {
                    'left': (textInfo.x * 100) + '%',
                    'position': 'absolute',
                    'top': (textInfo.y * 100) + '%',
                    'width': (textInfo.width * 100) + '%',
                    'z-index': 1
                }
            }).appendTo($sizeBox);

            // Add some data to the overlay element so it can be used later in event handlers
            // to update the text info.
            $textOverlay.data('textInfoKey', textInfoKey);
            $textOverlay.data('groupName', groupName);

            // Also save the overlay element within the data for the textInfo,
            // so we can retrieve the new value later
            textInfo.$textOverlay = $textOverlay;
            
            // Add the label to drag the text overlay
            $('<div/>', {
                'class': 'imageEditor-textOverlayLabel',
                'text': 'Text #' + textInfoKey
            }).on('mousedown', self.textMousedownDragHandler(function(event, original, delta) {
                return {
                    'moving': true,
                    'left': original.left + delta.x,
                    'top': original.top + delta.y
                };
            })).appendTo($textOverlay);

            // Add resize handle to change the left border
            $('<div/>', {
                'class': 'imageEditor-resizer imageEditor-resizer-left',
                'mousedown': self.textMousedownDragHandler(function(event, original, delta) {
                    return {
                        'left': original.left + delta.x,
                        'width': original.width - delta.x
                    };
                })
            }).appendTo($textOverlay);

            // Add resize handle to change the right border.
            $('<div/>', {
                'class': 'imageEditor-resizer imageEditor-resizer-right',
                'mousedown': self.textMousedownDragHandler(function(event, original, delta) {
                    return {
                        'left': original.left,
                        'width': original.width + delta.x
                    };
                })
            }).appendTo($textOverlay);

            // Add the remove button
            $('<div/>', {
                'class': 'imageEditor-textOverlayRemove',
                'text': 'Remove',
                'click': function() {
                    $textOverlay.remove();
                    self.textOverlayRemove(groupName, textInfoKey);
                    return false;
                }
            }).appendTo($textOverlay);
            
            // Add the text input and rich text editor
            // Activate rich text editor on the input
            $textOverlayInput = $('<input/>', {
                'class': 'imageEditor-textOverlayInput',
                'type': 'text',
                'value': textInfo.text || ''
            }).appendTo($textOverlay);

            $textOverlayInput.rte({
                'initImmediately': true,
                'useLineBreaks': true
            });
            
            // Move the rich text toolbar controls to on top of the image.
            // Note we need to wrap the toolbar inside another div,
            // because the rte messes around with the styles on the toolbar,
            // and we will need to hide and show the toolbar later.
            
            $textOverlayToolbar = $('<div/>', {'class':'imageEditor-text-toolbar'})
                .hide()
                .append( $textOverlay.find('.rte-toolbar-container') )
                .appendTo(self.dom.tabs.sizes);
            
            $textOverlayInput.on('rtefocus', function(){
                self.$element.find('.imageEditor-text-toolbar').hide();
                $textOverlayToolbar.show();
                return false;
            });
            
            $textOverlayInput.on('rteblur', function(){
                $textOverlayToolbar.hide();
                return false;
            });

            // Save the toolbar so we can delete it later
            textInfo.$toolbar = $textOverlayToolbar;
        
            // Try to set the font size after the rich text editor loads
            // TODO: need a better way to do this, such as a ready event that the RTE fires
            var wait = 5000;
            
            var repeatResizeTextOverlayFont = function() {
                
                var contentDocument, iframe, loaded;

                iframe = $textOverlay.find('.rte-container iframe')[0] || {};
                contentDocument = iframe.contentDocument || {};
                loaded = $(contentDocument.body).is('.rte-loaded');
                
                if (loaded) {
                    self.textOverlaySetFont(groupName, textInfoKey);
                } else {
                    // The RTE isn't loaded.
                    // Try again after a delay, but give up after a certain number of tries.
                    wait -= 100;
                    if (wait > 0) {
                        setTimeout(repeatResizeTextOverlayFont, 100);
                    }
                }
            };

            repeatResizeTextOverlayFont();
            
            // Focus on the text input
            if (focus) {
                $textOverlayInput.focus();
                $textOverlayToolbar.show();
            }

        },

        
        /**
         * Set the font on all text overlays that are visible,
         * or for a single text overlay.
         *
         * @param String groupName
         *
         * @param String [textInfoKey]
         * Optionally provide the textInfoKey for a single text info,
         * and it will be the only one that is resized.
         */
        textOverlaySetFont: function(groupName, singleTextInfoKey) {

            var groupInfo, groupSize, scaleBy, self, $sizeBox;

            self = this;

            groupInfo = self.sizeGroups[groupName];
            $sizeBox = groupInfo.$sizeBox;

            // Find a single height in the group so we can use it
            // to determine the font size when displaying the text overlay.
            // Note since each size will scale the font 
            $.each(groupInfo.sizeInfos, function(sizeName, sizeInfo) {
                
                // Special case if there is a variable height, use the width to scale instead
                if (sizeInfo.height === 0) {
                    scaleBy = 'width';
                    groupSize = sizeInfo.width;
                    return false; // stop looping
                }
                
                if (groupSize === undefined || sizeInfo.height > groupSize) {
                    scaleBy = 'height';
                    groupSize = sizeInfo.height;
                }
            });

            // Don't do anything if the size box is not visible
            if (!$sizeBox.is(':visible')) {
                return;
            }

            // Loop through each text overlay within the sizebox
            $sizeBox.find('.imageEditor-textOverlay').each(function() {

                var originalFontSize, $rteBody, sizeHeight, sizeWidth, $textOverlay, textInfoKey, textInfo;

                $textOverlay = $(this);

                textInfoKey = $textOverlay.data('textInfoKey');

                // If a single text was requested, only resize that one
                if (singleTextInfoKey && textInfoKey !== singleTextInfoKey) {
                    return;
                }
                
                textInfo = groupInfo.textInfos[ textInfoKey ];

                // Get the cropped dimensions of the selected group
                sizeHeight = self.sizesGetGroupFirstSizeInfo(groupName).height;
                sizeWidth = self.sizesGetGroupFirstSizeInfo(groupName).width;
                
                // Check to see if we previously saved a font size
                originalFontSize = $textOverlay.data('imageEditor-originalFontSize');
                
                // Get the body of the rich text editor
                $rteBody = $( $textOverlay.find('.rte-container iframe')[0].contentDocument.body );

                if (!originalFontSize && $rteBody.is('.rte-loaded')) {
                    originalFontSize = parseFloat($rteBody.css('font-size'));
                    $.data(this, 'imageEditor-originalFontSize', originalFontSize);
                }

                if (originalFontSize > 0) {

                    // Scale the text as it would appear on the cropped image
                    $rteBody.css({
                        'font-size': ((scaleBy === 'height') ? $sizeBox.height() : $sizeBox.width()) * (originalFontSize / groupSize),
                        'line-height': 1.2
                    });

                    // Save the font size so we can use it later when updating the hidden inputs.
                    // Each individual size needs to have a different textSize applied.
                    textInfo.originalFontSize = originalFontSize;
                }

            });

            // Update the hiden variables with the new textSize values
            // based on the font size and the sizes of the cropped image
            self.textSetTextInfo(groupName);
        },

        
        textOverlayRemove: function(groupName, textInfoKey) {
            
            var self, textInfos;
            
            self = this;

            textInfos = self.sizeGroups[groupName].textInfos;

            textInfos[textInfoKey].$toolbar.remove();
            
            delete textInfos[textInfoKey];
            
            // Update the hidden variables
            self.textSetTextInfo(groupName);
        },
        
        
        textMousedownDragHandler: function(filterBoundsFunction) {
            
            var mousedownHandler, self;

            self = this;
            
            mousedownHandler = function(mousedownEvent) {

                var element, groupInfo, groupName, textOverlayPosition, original, $textOverlay, textInfo, textInfoKey, $sizeBox, sizeBoxWidth, sizeBoxHeight;

                // The element that was dragged and the text overlay that contains it
                element = this;
                $textOverlay = $(element).closest('.imageEditor-textOverlay');

                // Get the data attribute for the textInfoKey so we can update the text info
                groupName = $textOverlay.data('groupName') || '';
                textInfoKey = $textOverlay.data('textInfoKey') || '';

                groupInfo = self.sizeGroups[groupName];
                $sizeBox = groupInfo.$sizeBox;
                
                textInfo = groupInfo.textInfos[textInfoKey];
                
                //resizeTextOverlayFont();

                // When user presses mouse down, get the current position of the
                // text overlay, plus the size of the sizeBox container
                // so we can constrain how much we allow it to be dragged.
                
                textOverlayPosition = $textOverlay.position();
                sizeBoxWidth = $sizeBox.width();
                sizeBoxHeight = $sizeBox.height();
                
                original = {
                    'left': textOverlayPosition.left,
                    'top': textOverlayPosition.top,
                    'width': $textOverlay.width(),
                    'height': $textOverlay.height(),
                    'pageX': mousedownEvent.pageX,
                    'pageY': mousedownEvent.pageY
                };

                mousedownEvent.dragImmediately = true;

                $.drag(this, mousedownEvent, function() {

                    // This is the start callback for .drag()

                }, function(dragEvent) {

                    // This is the move callback for .drag()

                    var deltaX, deltaY, bounds;
                    
                    deltaX = dragEvent.pageX - original.pageX;
                    deltaY = dragEvent.pageY - original.pageY;

                    // Use the filterBoundsFunction to adjust the value of the bounds
                    // based on what is being dragged.
                    bounds = filterBoundsFunction(dragEvent, original, {
                        'x': deltaX,
                        'y': deltaY
                    });

                    // Fill out the missing bounds
                    bounds = $.extend({}, original, bounds);

                    // Prevent from moving outside the size box

                    if (bounds.left < 0) {
                        bounds.left = 0;
                    }

                    if (bounds.top < 0) {
                        bounds.top = 0;
                    }

                    if (bounds.top + original.height > sizeBoxHeight) {
                        bounds.top = sizeBoxHeight - original.height;
                    }
                    
                    if (bounds.moving) {
                        if (bounds.left + bounds.width > sizeBoxWidth) {
                            bounds.left = sizeBoxWidth - bounds.width;
                        }
                    } else {
                        
                        if (bounds.left + bounds.width > sizeBoxWidth) {
                            bounds.width = sizeBoxWidth - bounds.left;
                        }
                    }

                    // Save the bounds to the textInfo object
                    textInfo.x = bounds.left / sizeBoxWidth;
                    textInfo.y = bounds.top / sizeBoxHeight;
                    textInfo.width = bounds.width / sizeBoxWidth;

                    // Convert the bounds to percentage values and update the position of the text overlay
                    bounds.left = (textInfo.x * 100) + '%';
                    bounds.top = (textInfo.y * 100) + '%';
                    bounds.width = (textInfo.width * 100) + '%';
                    bounds.height = 'auto';
                    
                    $textOverlay.css(bounds);

                }, function() {
                    
                    // .drag() end callback
                    
                    // Now that we're done dragging, update the textInfo for this text,
                    // then update the hidden variables
                    self.textSetTextInfo(groupName, groupInfo.textInfos);
                    
                });

                return false;
            };

            return mousedownHandler;
        },

        
        //--------------------------------------------------
        // HOTSPOTS
        // Hotspots use a repeatableForm object on the page.
        // We listen for create events to determine if a new hotspot is created on the page,
        // then we wipe out all hotspots and add new ones.
        // The user can also edit the individual form values (x/y/width/height) and the hotspots
        // will automatically adjust to the new values.
        // If the image is rotated or flipped, then the hotspots should also be updated.
        //--------------------------------------------------

        hotspotInit: function() {

            var self;

            self = this;

            // Get the hotspot form element
            self.dom.$hotspots = self.$element.closest('.inputContainer').find('.hotSpots');
            if (self.dom.$hotspots.length === 0) {
                return;
            }

            // Create a tab for hotspots
            self.tabsCreate('hotspots');
            
            // Create an image in the hotspot tab to show the hotspots
            // Note this image will need to be kept in sync with image changes
            // to flip and rotate the original image
            self.dom.$hotspotImage = self.dom.$imageClone.clone();
            self.dom.$hotspotImageWrapper = $('<div/>', {
                'class': 'imageEditor-image',
                'style': 'position:relative',
                'html': self.dom.$hotspotImage
            }).appendTo(self.dom.tabs.hotspots);
            
            // Move hotspot form into the hotspot tab so it only shows when that tab is active
            self.dom.$hotspots.appendTo( self.dom.tabs.hotspots );
            
            // Set up all the hotspot overlays based on the form inputs
            self.hotspotOverlayResetAll();

            // Monitor the page so if the hotspot form inputs are modified we redisplay the hotspots
            self.$element.closest('.inputContainer').on('create change', function() {

                // If a new hotspot was added it probably has blank values,
                // so give it some reasonable default values instead
                self.hotspotInputSetDefaults();

                // Recreate the hotspot overlays
                self.hotspotOverlayResetAll();

            });

            // If the image is updated then update the hotspot image
            self.$element.on('imageUpdated', function(event, $image) {

                var $newImage;

                $newImage = $( self.cloneCanvas($image.get(0)) );
                
                self.dom.$hotspotImage.before($newImage);
                self.dom.$hotspotImage.remove();
                self.dom.$hotspotImage = $newImage;
                
                self.hotspotOverlayResetAll();
            });

            // When user switches to the hotspot tab, check if the image has been updated
            // so we can update all the hotspots overlays
            self.dom.tabs.hotspots.on('tabbedShow', function(){
                self.hotspotOverlayResetAll();
            });

        },


        /**
         * Set up all the hotspot overlays based on the form inputs.
         * Remove any existing hotspot overlays before beginning.
         */
        hotspotOverlayResetAll: function() {

            var self;

            self = this;

            // Remove any existing overlays so we can recreate them with new values
            self.hotspotOverlayRemoveAll();

            // Loop through all the hotspot inputs
            self.dom.$hotspots.find('.objectInputs').each(function(){
                
                var data, $objectInput;
                
                $objectInput = $(this);

                // Skip this hotspot if it is marked to be removed
                if ($objectInput.closest('li').hasClass('toBeRemoved')) {
                    return;
                }

                // Get all the data for the hotspot from the inputs
                data = self.hotspotInputGet($objectInput);

                // Create the hotspot overlay
                self.hotspotOverlayAdd($objectInput.parent(), data);

            });
        },

        
        /**
         * Returns the information for a single hotspot, adjusting based on image rotation and so forth.
         * Note the numbers returned are based on the original image and not based on the image scale that
         * was served to the page, nor on the CSS-styled image size on the page.
         *
         * @param Element 
         * @returns Object
         * Object with x, y, width, height values for the hotspot overlay.
         */
        hotspotInputGet: function(input) {
            
            var heightAdjusted, widthAdjusted, data, heightOriginal, $input, rotation, self, widthOriginal;

            self = this;

            $input = $(input);
            
            // Get the image height and width and adjust for scale
            widthOriginal = self.dom.imageCloneWidth / self.scale;
            heightOriginal = self.dom.imageCloneHeight / self.scale;

            // In case we need to rotate and flip at the same time,
            // keep track of the width and height after rotation
            widthAdjusted = widthOriginal;
            heightAdjusted = heightOriginal;
            
            // Get the form input values
            data = {
                x: Math.abs(parseInt($input.find(':input[name$="x"]').val()) || 1),
                y: Math.abs(parseInt($input.find(':input[name$="y"]').val()) || 1),
                width: Math.abs(parseInt($input.find(':input[name$="width"]').val()) || 0),
                height: Math.abs(parseInt($input.find(':input[name$="height"]').val()) || 0)
            };

            // Adjust for rotation
            rotation = self.adjustmentRotateGet();

            switch (rotation) {

            case -90:
            case 270:

                // Adjust the hotspot data for 90 rotation (rotating to the left)

                widthAdjusted = heightOriginal;
                heightAdjusted = widthOriginal;

                data = {
                    width: data.height,
                    height: data.width,
                    x: data.y,
                    y: widthOriginal - data.x - data.width
                };

                break;

            case 90:
            case -270:
                
                // Adjust the hotspot data for 90 rotation (rotating to the right)
                
                widthAdjusted = heightOriginal;
                heightAdjusted = widthOriginal;
                
                data = {
                    width: data.height,
                    height: data.width,
                    x: heightOriginal - data.y - data.height,
                    y: data.x
                };

                break;

            case 180:
            case -180:

                // Adjust the hotspot data for 180 rotation
                
                data = {
                    width: data.width,
                    height: data.height,
                    x: widthOriginal - data.x - data.width,
                    y: heightOriginal - data.y - data.height
                };

                break;
            }

            // Check if we need to flip the hotspots to match the flipH or flipV of the image
            if (self.adjustmentFlipHGet()) {

                if (rotation === 90 || rotation === -90) {
                    data.y = heightAdjusted - data.y - data.height;
                } else {
                    data.x = widthAdjusted - data.x - data.width;
                }
            }

            if (self.adjustmentFlipVGet()) {

                if (rotation === 90 || rotation === -90) {
                    data.x = widthAdjusted - data.x - data.width;
                } else {
                    data.y = heightAdjusted - data.y - data.height;
                }
            }

            // Calculate percentages
            data.xPercent = (data.x / widthAdjusted) * 100;
            data.yPercent = (data.y / heightAdjusted) * 100;
            if (data.width === 0) {
                data.widthPercent = 0;
            } else {
                data.widthPercent = data.width / widthAdjusted * 100;
            }

            if (data.height === 0) {
                data.heightPercent = 0;
            } else {
                data.heightPercent = data.height / heightAdjusted * 100;
            }

            return data;
        },

        
        /**
         * Saves the hotspot overlay information, adjusting based on image rotation and so forth.
         * This is meant to be used after the user adjusts the hotspot position.
         *
         * Note: this function will onlhy work if self.dom.$hotspotImage is visible,
         * because if it is hidden the browser will not return the correct width;
         *
         * @param Element input
         * The DOM element that contains the hotspot inputs.
         *
         * @param Object data The position and size of the hotspot box.
         * @param Object data.x The x position in pixels for the top left corner.
         * @param Object data.y The y position in pixels for the top left corner.
         * @param Object data.width The width in pixels.
         * @param Object data.height The height in pixels.
         */
        hotspotInputSet: function(input, data) {

            var heightAdjusted, heightOnPage, heightOriginal, $input, scaleOnPage, self, rotation, widthAdjusted, widthOnPage, widthOriginal;

            self = this;

            $input = $(input);

            // Get the dimensions for the image that is on the page.
            // The hotspot data is relative to this image
            widthOnPage = self.dom.$hotspotImage.width();
            heightOnPage = self.dom.$hotspotImage.height();
            
            // Get the dimensions of the original image on the backend.
            // Our final hotspot coordinates must be relative to this image.
            widthOriginal = self.dom.imageCloneWidth / self.scale;
            heightOriginal = self.dom.imageCloneHeight / self.scale;

            // In case we need to rotate and flip at the same time,
            // keep track of the width and height after rotation
            widthAdjusted = widthOriginal;
            heightAdjusted = heightOriginal;
            
            // Calculate how much our page image differs from the original image
            rotation = self.adjustmentRotateGet();
            if (rotation === 90 || rotation === -90) {
                scaleOnPage = widthOnPage / heightOriginal;
            } else {
                scaleOnPage = widthOnPage / widthOriginal;
            }

            // Convert the pixels from page scale to original scale
            $.each(data, function(name, value) {
                data[name] = value / scaleOnPage;
            });

            // If the image is rotated we need to change the coordinates so they
            // represent the unrotated state
            
            if (rotation === 90) {
                // Rotated to the right
                widthAdjusted = heightOriginal;
                heightAdjusted = widthOriginal;
                data = {
                    width: data.height,
                    height: data.width,
                    x: data.y,
                    y: heightOriginal - data.x - data.width
                };
            } else if (rotation === -90) {
                // Rotated to the left
                widthAdjusted = heightOriginal;
                heightAdjusted = widthOriginal;
                data = {
                    width: data.height,
                    height: data.width,
                    x: widthOriginal - data.y - data.height,
                    y: data.x
                };
            }

            // Check if we need to flip the hotspots to match the flipH or flipV of the image
            if (self.adjustmentFlipHGet()) {
                data.x = widthAdjusted - data.x - data.width;
            }

            if (self.adjustmentFlipVGet()) {
                data.y = heightAdjusted - data.y - data.height;
            }

            // Write the values back to the inputs
            $input.find(':input[name$="x"]').val( parseInt(data.x) );
            $input.find(':input[name$="y"]').val( parseInt(data.y) );
            $input.find(':input[name$="width"]').val( parseInt(data.width) );
            $input.find(':input[name$="height"]').val( parseInt(data.height) );

        },


        /**
         * If hotspot inputs are blank give them reasonable values.
         */
        hotspotInputSetDefaults: function() {
            
            var defaultX, defaultY, defaultWidth, defaultHeight, height, self, width;

            self = this;

            // Get the width of the original image
            width = self.dom.imageCloneWidth / self.scale;
            height = self.dom.imageCloneHeight / self.scale;
                
            // If a blank hotspot is added, set up a default size and position in the middle of the image
            defaultX = parseInt(width / 4);
            defaultY = parseInt(height / 4);
            defaultWidth = parseInt(width / 2);
            defaultHeight = parseInt(height  / 2);

            self.dom.$hotspots.find('.objectInputs').each(function() {

                var $hotspot;

                $hotspot = $(this);

                $hotspot.find(':input[name$="x"]').val(function(index, value) {
                    return value === '' ? defaultX : value;
                });
                $hotspot.find(':input[name$="y"]').val(function(index, value) {
                    return value === '' ? defaultY : value;
                });
                $hotspot.find(':input[name$="width"]').val(function(index, value) {
                    return value === '' ? defaultWidth : value;
                });
                $hotspot.find(':input[name$="height"]').val(function(index, value) {
                    return value === '' ? defaultHeight : value;
                });
            });
        },


        /**
         * Add a single hotspot overlay to the image.
         */
        hotspotOverlayAdd: function(input, data) {

            var $hotspotOverlay, $hotspotOverlayBox, $input, self;

            self = this;
            
            $input = $(input);

            $hotspotOverlay = $('<div/>', {
                'class': 'imageEditor-hotSpotOverlay',
                'css': {
                    'height': data.heightPercent + '%',
                    'left': data.xPercent  + '%',
                    'position': 'absolute',
                    'top': data.yPercent + '%',
                    'width': data.widthPercent + '%',
                    'z-index': 1
                },
                //'data-type-id' : $input.find('input[name$="file.hotspots.typeId"]').val(),
                'mousedown' : function() {
                    // If you click on the overlay box, add class to make it appear selected.
                    // Also select the input for the hotspot data.
                    self.$element.find('.imageEditor-hotSpotOverlay').removeClass("selected");
                    self.$element.find('.state-focus').removeClass("state-focus");
                    $input.addClass("state-focus");
                    $hotspotOverlay.addClass("selected");
                }
            }).appendTo(self.dom.$hotspotImageWrapper);
            
            // Save the input value to the overlay so it can be used later
            // to mark the input to be removed when removing the overlay
            $hotspotOverlay.data('hotspotInput', $input);

            $hotspotOverlayBox = $('<div/>', {
                'class': 'imageEditor-hotSpotOverlayBox',
                'css': {
                    'height': '100%',
                    'position': 'absolute',
                    'width': '100%',
                    'z-index': 1
                }
            }).appendTo($hotspotOverlay);

            // Label to move the hotspot
            $('<div/>', {
                'class': 'imageEditor-textOverlayLabel',
                'text': 'HotSpot',
                'mousedown' : self.hotspotMousedownDragHandler(function(event, original, delta) {
                    $input.addClass("state-focus");
                    $hotspotOverlay.addClass("selected");
                    return {
                        'moving': true,
                        'left': original.left + delta.x,
                        'top': original.top + delta.y
                    };
                })
            }).appendTo($hotspotOverlay);

            // Control to remove the hotspot
            $('<div/>', {
                'class': 'imageEditor-textOverlayRemove',
                'text': 'Remove',
                'click': function() {
                    self.hotspotRemove($hotspotOverlay);
                    return false;
                }
            }).appendTo($hotspotOverlay);

            // Check if we are a single point or a region
            if (data.width === 0) {
                
                // This hotspot is a single point so make the box 10x10
                $hotspotOverlay.css("width", "10px");
                $hotspotOverlay.css("height", "10px");
                
            } else {

                // This hotspot is a region, so add more controls to resize the region
                
                $('<div/>', {
                    'class': 'imageEditor-resizer imageEditor-resizer-bottomRight',
                    'mousedown': self.hotspotMousedownDragHandler(function(event, original, delta) {
                        $input.addClass("state-focus");
                        $hotspotOverlay.addClass("selected");
                        return {
                            'width': original.width + delta.x,
                            'height': original.height + delta.y
                        };
                    })
                }).appendTo($hotspotOverlayBox);
                
            }

            // Adding a focus class to the input when we mouse over
            
            // TODO: since we are removing and adding hotspots whenever a change is made,
            // there is a possibility that these events might get multiple handlers.
            // Need to test to see how many times they fire in that case.
            
            $input.mouseover(function() {
                $input.addClass("state-focus");
                $hotspotOverlay.addClass("selected");
            }).mouseleave(function() {
                $input.removeClass("state-focus");
                $hotspotOverlay.removeClass("selected");
            });
            self.$element.mouseleave(function() {
                $input.removeClass("state-focus");
                $hotspotOverlay.removeClass("selected");
            });

        },


        /**
         * Remove a single hotspot overlay.
         *
         * @param Element overlayElement
         */
        hotspotRemove: function(overlayElement) {

            var height, $input, left, $overlay, self, top, width;

            self = this;

            $overlay = $(overlayElement);

            // Check if there are any overlays of the exact size and position underneath,
            // and if so remove them as well
            left = $overlay.css("left");
            top = $overlay.css("top");
            width = $overlay.css("width");
            height = $overlay.css("height");

            self.$element.find('.imageEditor-hotSpotOverlay').each(function() {
                var $input, $overlay;

                $overlay = $(this);
                
                if ($overlay.css("left") === left &&
                    $overlay.css("top") === top &&
                    $overlay.css("width") === width &&
                    $overlay.css("height") === height) {
                    
                    // We previously saved the hotspotInput data on the hotspot element.
                    // We'll get it now so we can mark the input to be removed.
                    $input = $overlay.data('hotspotInput');
                    self.hotspotInputMarkToBeRemoved($input);

                    $overlay.remove();
                }
            });

            // We previously saved the hotspotInput data on the hotspot element.
            // We'll get it now so we can mark the input to be removed.
            $input = $overlay.data('hotspotInput');
            self.hotspotInputMarkToBeRemoved($input);
            
            $overlay.remove();
        },

        
        /**
         * Mark a hotspot input to be removed.
         *
         * @param Element inputContainer
         * The element that contains the inputs for a single hotspot.
         */
        hotspotInputMarkToBeRemoved: function(inputElement) {
            var $input;
            $input = $(inputElement);
            $input.addClass("toBeRemoved");
            $input.find("input").attr("disabled", "disabled");
        },

        
        /**
         * Remove all hotspot overlays (but do not modify the form inputs).
         */
        hotspotOverlayRemoveAll: function() {
            var self;
            self = this;
            self.$element.find('.imageEditor-hotSpotOverlay').remove();
        },


        /**
         * Create a mousedown handler function that lets the user drag the hotspot box
         * or the hotspot box handles.
         *
         * @param Function filterBoundsFunction
         * A function that will modify the bounds of the overlay,
         * and adjust it according to what is being dragged. For example,
         * if the left/top handle is being dragged.
         * Ths function must return a modified bounds object.
         * Also the function can set moving:true in the bounds object if
         * the entire size box is being moved (instead of resizing the size box).
         *
         * @returns Function
         * Returns a function that can be used as a mousedown handler on a drag handle.
         * The handler will update the hotspot overlay appropriately.
         */
        hotspotMousedownDragHandler: function(filterBoundsFunction) {

            var mousedownHandler, self;

            self = this;

            mousedownHandler = function(mousedownEvent) {

                var imageHeight, imageWidth, $input, $mousedownElement, original, $overlay, $overlayBox, overlayPosition;
                
                $mousedownElement = $(this);
                $overlay = $mousedownElement.closest('.imageEditor-hotSpotOverlay');
                $overlayBox = $overlay.find('.imageEditor-hotSpotOverlayBox');
                $input = $overlay.data('hotspotInput');
                
                overlayPosition = $overlay.position();
                
                original = {
                    'left': overlayPosition.left,
                    'top': overlayPosition.top,
                    'width': $overlayBox.width(),
                    'height': $overlayBox.height(),
                    'pageX': mousedownEvent.pageX,
                    'pageY': mousedownEvent.pageY
                };

                imageWidth = self.dom.$hotspotImage.width();
                imageHeight = self.dom.$hotspotImage.height();

                // .drag(element, event, startCallback, moveCallback, endCallback)
                $.drag(this, event, function() {
                    
                    // drag start callback
                    
                }, function(dragEvent) {
                    
                    // drag move callback

                    var bounds, deltaX, deltaY, overflow;
                    
                    deltaX = dragEvent.pageX - original.pageX;
                    deltaY = dragEvent.pageY - original.pageY;
                    bounds = filterBoundsFunction(dragEvent, original, {
                        'x': deltaX,
                        'y': deltaY
                    });

                    // Fill out the missing bounds
                    bounds = $.extend({}, original, bounds);

                    // When moving, don't let it go outside the image.
                    if (bounds.moving) {
                        
                        // We're not resizing the box, we are moving it
                        
                        if (bounds.left < 0) {
                            bounds.left = 0;
                        }

                        if (bounds.top < 0) {
                            bounds.top = 0;
                        }

                        if (bounds.width === 0) {
                            bounds.width = 10;
                        }

                        overflow = bounds.left + bounds.width - imageWidth;
                        if (overflow > 0) {
                            bounds.left -= overflow;
                        }

                        overflow = bounds.top + bounds.height - imageHeight;
                        if (overflow > 0) {
                            bounds.top -= overflow;
                        }

                    } else {

                        // We're not moving the box, we are resizing it.

                        // Don't let the hotspot get too small
                        if (bounds.width < 10) {
                            bounds.width = 10;
                        }
                        
                        if (bounds.height < 10) {
                            bounds.height = 10;
                        }

                        if (bounds.left < 0) {
                            bounds.width += bounds.left;
                            bounds.left = 0;
                        }

                        if (bounds.top < 0) {
                            bounds.height += bounds.top;
                            bounds.top = 0;
                        }

                        overflow = bounds.left + bounds.width - imageWidth;
                        if (overflow > 0) {
                            bounds.width -= overflow;
                        }

                        overflow = bounds.top + bounds.height - imageHeight;
                        if (overflow > 0) {
                            bounds.height -= overflow;
                        }
                    }

                    $overlay.css(bounds);
                    //$overlayBox.css('width', bounds.width);
                    //$overlayBox.css('height', bounds.height);

                }, function() {
                    
                    // Drag end callback

                    // Set the hidden inputs to the current bounds of the overlay

                    var position;

                    position = $overlay.position();

                    self.hotspotInputSet($input, {
                        x: position.left,
                        y: position.top,
                        width: $overlayBox.width(),
                        height: $overlayBox.height()
                    });
                    
                });

                return false;
            };

            return mousedownHandler;
        },


        //--------------------------------------------------
        // MISC SUPPORT FUNCTIONS
        //--------------------------------------------------
        
        /**
         * Fix scrolling behavior of the "aside" scrollable area.
         * If user is scrolling the area and reaches the top or bottom,
         * do not let the browser start scrolling the entire page.
         */
        scrollFix: function(element) {

            var self;
            
            self = this;

            $(element).bind('mouswheel', function(event, delta, deltaX, deltaY) {
                if (self.elementIsScrolledToMax(this, deltaY)) {
                    event.preventDefault();
                }
            });
        },

        
        /**
         * Check if an element (with an internal scrollbar) is already scrolled to the top or the bottom.
         *
         * @param Element|jQuery element
         * The element that is being scrolled.
         *
         * @param Number deltaY
         * The number of pixels being scrolled.
         * This is typically retrieved from the mousewheel event.
         *
         * @returns Boolean
         * Return true if the element is already scrolled to the top.
         */
        elementIsScrolledToMax: function(element, deltaY) {

            var self, $el, attr, maxScrollTop, scrollTop;
            
            self = this;
            $el = $(element);
            
            // Name of the data attribute we will save on the element to remember the scroll position
            attr = 'imageEditor-maxScrollTop';

            // Get a previously saved scroll position
            maxScrollTop = $el.data(attr);

            // Get the current scroll position
            scrollTop = $el.scrollTop();
            
            // See if we previously saved the scroll position
            if (typeof maxScrollTop === 'undefined') {
                
                // We haven't saved the scroll position previously,
                // so we'll assume the current scroll position is the top
                maxScrollTop = $el.prop('scrollHeight') - $el.innerHeight();
                $el.data(attr, maxScrollTop);
            }

            // Check if we are already scrolled to the top or bottom
            if ((deltaY > 0 && scrollTop === 0) ||
                (deltaY < 0 && scrollTop >= maxScrollTop)) {
                return true;
            }

            return false;
        },


        /**
         * Returns the width of the canvas.
         */
        getCanvasWidth: function() {
            
            var self, value;

            self = this;
            value = self.dom.$image.parent().find('canvas').width();
            return value;
        },

        
        /**
         * Returns the width of the canvas.
         */
        getCanvasHeight: function() {
            
            var self, value;

            self = this;
            
            value = self.dom.$image.parent().find('canvas').height();
            return value;
        },


        /**
         * Assuming a click event occurred on an element, this function
         * returns the position within the element that was clicked.
         *
         * @param Element element
         * The element that was clicked.
         *
         * @param Event clickEvent
         * The click event from your on click handler.
         *
         * @returns Object position
         * @returns Number position.x The x position in pixels.
         * @returns Number position.y The y position in pixels.
         * @returns Number position.xPercent The x position in percent (between 0 and 1).
         * @returns Number position.yPercent The y position in percent (between 0 and 1).
         * @returns Number position.width The width of the element that was clicked.
         * @returns Number position.height The height of the element that was clicked.
         */
        getClickPositionInElement: function(element, clickEvent) {
            
            var $element, height, offset, width, x, y;
            
            $element = $(element);
            width = $element.width();
            height = $element.height();
            offset = $element.offset();
            x = Math.ceil(clickEvent.pageX - offset.left) || 1;
            y = Math.ceil(clickEvent.pageY - offset.top) || 1;
            
            // Just in case something weird happens check for boundaries
            if (x <= 0) { x = 1; }
            if (y <= 0) { y = 1; }

            return {
                'x': x,
                'y': y,

                'xPercent': x / width,
                'yPercent': y / height,

                'width': width,
                'height': height
            };
        },


        /**
         * Clone a canvas image.
         * @returns Canvas
         */
        cloneCanvas: function(oldCanvas) {

            var context, newCanvas;

            if ($(oldCanvas).is('canvas')) {

                //create a new canvas
                newCanvas = document.createElement('canvas');
                context = newCanvas.getContext('2d');

                //set dimensions
                newCanvas.width = oldCanvas.width;
                newCanvas.height = oldCanvas.height;
 
                //apply the old canvas to the new one
                context.drawImage(oldCanvas, 0, 0);

                //return the new canvas
                return newCanvas;
            } else {
                return $(oldCanvas).clone();
            }
        },

        
        /**
         * Get the natural width and height of an image.
         *
         * @returns Promise
         * A promise that can be used to continue running after the image size is retrieved.
         *
         * @example
         * self.getImageSize(myimage).done(function(width, height) {
         *   alert('Image width is ' + width);
         * });
         */
        getImageSize: function(imageElement) {

            var deferred, height, $img, width;

            // Create a deferred object that we can return.
            // We will resolve it after we figure out the width and height.
            deferred = $.Deferred();

            // Try to get the naturalWidth and naturalHeight properties
            // (if the image is already loaded and those properties are supported)
            $img = $(imageElement);
            width = $img.prop('naturalWidth');
            height = $img.prop('naturalHeight');

            if (width || height) {
                
                // Resolve the deferred object to say we are ready
                deferred.resolve(width, height);
                
            } else {

                // We couldn't get the naturalWidth or naturalHeight
                // Maybe image has not finished loading,
                // or maybe those properties are not supported.
                
                // Create a new image with a load event
                $('<img/>').on('load', function() {
                    
                    // After the image finishes loading try to get the width and height again
                    var height, $img, width;
                    $img = $(this);
                    width = $img.prop('naturalWidth') || $img.prop('width') || 0;
                    height = $img.prop('naturalHeight') || $img.prop('height') || 0;
                    deferred.resolve(width, height);
                    
                }).on('error', function() {

                    // If there was some kind of problem loading the image,
                    // resolve the deferred so other code can continue
                    deferred.resolve(0,0);
                    
                }).attr('src', $img.attr('src'));

                // Just in case something goes wrong like the image taking too long,
                // resolve the deferred, so the other code can continue
                setTimeout(function(){
                    deferred.resolve(0,0);
                }, 10000);
            }

            // Return a promise that can be used to continue running other code
            return deferred.promise();
        }

    }; // END imageEditorUtilty object

    
    // Whenever an element with class "imageEditor" is added to the page,
    // we create a new instance of the imageEditorUtility object and initialize it.
    
    bsp_utils.onDomInsert(document, '.imageEditor', {
        
        insert: function(element) {

            var imageEditor;
            
            // Create a copy of the repeatableUtility object
            // This uses prototypal inheritance so we're not actually copying the entire object
            // It allows each repeatable instance to have its own object that saves state
            imageEditor = Object.create(imageEditorUtility);
            
            // Initialize the image editor
            imageEditor.init(element);

            // Save the instance of the object on the element in case anyone wants to use it
            $(element).data('imageEditor', imageEditor);

        }
    });

    return imageEditorUtility;

}); // END define


/***======================================================================


/// BLUR control

            var $blurOverlayIndex = 0;
            var addSizeBox = function(input, left, top, width, height) {
                //add blur box
                $blurOverlayIndex++;

                var $blurInput;

                if (input === null) {
                    $blurInput = $('<input>', {
                        'type' : 'hidden',
                        'name' : $dataName + ".blur",
                        'value' : left + "x" + top + "x" + width + "x" + height
                    });
                    $edit.append($blurInput);
                } else {
                    $blurInput = $(input);
                }

                var $blurOverlay = $('<div/>', {
                    'class': 'imageEditor-blurOverlay',
                    'css': {
                        'height': height + 'px',
                        'left': left  + 'px',
                        'position': 'absolute',
                        'top': top + 'px',
                        'width': width + 'px',
                        'z-index': 1
                    }
                });

                var $blurOverlayBox = $('<div/>', {
                    'class': 'imageEditor-blurOverlayBox',
                    'css': {
                        'height': height + 'px',
                        'position': 'absolute',
                        'width': width + 'px',
                        'z-index': 1,
                        'outline': '1px dashed #fff'
                    }
                });

                var $blurOverlayLabel = $('<div/>', {
                    'class': 'imageEditor-textOverlayLabel',
                    'text': 'Blur #' + $blurOverlayIndex
                });

                var sizeAspectRatio = width / height;
                var updateSizeBox = function(callback) {

                    // create a function that will be used as an event handler
                    // for a mousedown event
                    return function(event) {

                        // mousedown even has occurred

                        var sizeBoxPosition = $blurOverlayBox.parent().position();

                        var original = {
                            'left': sizeBoxPosition.left,
                            'top': sizeBoxPosition.top,
                            'width': $blurOverlayBox.width(),
                            'height': $blurOverlayBox.height(),
                            'pageX': event.pageX,
                            'pageY': event.pageY
                        };

                        var imageWidth = $image.width();
                        var imageHeight = $image.height();

                        //.drag( element, event, startCallback, moveCallback, endCallback )
                        $.drag(this, event, function(event) {
                            // drag start callback
                        }, function(event) {
                            // drag move callback
                            var deltaX = event.pageX - original.pageX;
                            var deltaY = event.pageY - original.pageY;
                            var bounds = callback(event, original, {
                                'x': deltaX,
                                'y': deltaY,
                                'constrainedX': Math.max(deltaX, deltaY * sizeAspectRatio),
                                'constrainedY': Math.max(deltaY, deltaX / sizeAspectRatio)
                            });

                            // Fill out the missing bounds.
                            for (key in original) {
                                bounds[key] = bounds[key] || original[key];
                            }

                            var overflow;

                            // When moving, don't let it go outside the image.
                            if (bounds.moving) {
                                if (bounds.left < 0) {
                                    bounds.left = 0;
                                }

                                if (bounds.top < 0) {
                                    bounds.top = 0;
                                }

                                overflow = bounds.left + bounds.width - imageWidth;
                                if (overflow > 0) {
                                    bounds.left -= overflow;
                                }

                                overflow = bounds.top + bounds.height - imageHeight;
                                if (overflow > 0) {
                                    bounds.top -= overflow;
                                }

                            // Resizing...
                            } else {
                                if (bounds.width < 10 || bounds.height < 10) {
                                    if (sizeAspectRatio > 1.0) {
                                        bounds.width = sizeAspectRatio * 10;
                                        bounds.height = 10;
                                    } else {
                                        bounds.width = 10;
                                        bounds.height = 10 / sizeAspectRatio;
                                    }
                                }

                                if (bounds.left < 0) {
                                    bounds.width += bounds.left;
                                    bounds.height = bounds.width / sizeAspectRatio;
                                    bounds.top -= bounds.left / sizeAspectRatio;
                                    bounds.left = 0;
                                }

                                if (bounds.top < 0) {
                                    bounds.height += bounds.top;
                                    bounds.width = bounds.height * sizeAspectRatio;
                                    bounds.left -= bounds.top * sizeAspectRatio;
                                    bounds.top = 0;
                                }

                                overflow = bounds.left + bounds.width - imageWidth;
                                if (overflow > 0) {
                                    bounds.width -= overflow;
                                    bounds.height = bounds.width / sizeAspectRatio;
                                }

                                overflow = bounds.top + bounds.height - imageHeight;
                                if (overflow > 0) {
                                    bounds.height -= overflow;
                                    bounds.width = bounds.height * sizeAspectRatio;
                                }
                            }

                            $blurOverlay.css(bounds);
                            $blurOverlayBox.css('width', bounds.width);
                            $blurOverlayBox.css('height', bounds.height);

                        // Set the hidden inputs to the current bounds.
                        }, function() {
                            // drag end callback
                            var blurOverlayBoxPosition = $blurOverlayBox.parent().position();
                            $blurInput.attr("value", blurOverlayBoxPosition.left + "x" + blurOverlayBoxPosition.top + "x" + $blurOverlayBox.width() + "x" + $blurOverlayBox.height());

                        });

                        return false;
                    };
                };

                $blurOverlayBox.append($('<div/>', {
                    'class': 'imageEditor-resizer imageEditor-resizer-left',
                    'mousedown': updateSizeBox(function(event, original, delta) {
                        return {
                            'left': original.left + delta.x,
                            'width': original.width - delta.x
                        };
                    }) 
                }));
                $blurOverlayBox.append($('<div/>', {
                    'class': 'imageEditor-resizer imageEditor-resizer-right',
                    'mousedown': updateSizeBox(function(event, original, delta) {
                        return {
                            'left': original.left,
                            'width': original.width + delta.x
                        };
                    })
                }));
                $blurOverlayBox.append($('<div/>', {
                    'class': 'imageEditor-resizer imageEditor-resizer-bottomRight',
                    'mousedown': updateSizeBox(function(event, original, delta) {
                        return {
                            'width': original.width + delta.constrainedX,
                            'height': original.height + delta.constrainedY
                        };
                    })
                }));

                $blurOverlay.append($blurOverlayBox);

                $blurOverlayLabel.mousedown(updateSizeBox(function(event, original, delta) {
                    return {
                        'moving': true,
                        'left': original.left + delta.x,
                        'top': original.top + delta.y
                    };
                }));

                $blurOverlay.append($blurOverlayLabel);

                var $blurOverlayRemove = $('<div/>', {
                    'class': 'imageEditor-textOverlayRemove',
                    'text': 'Remove',
                    'click': function() {
                        var left = $blurOverlay.css("left");
                        var top = $blurOverlay.css("top");
                        var width = $blurOverlay.css("width");
                        var height = $blurOverlay.css("height");

                        $('.imageEditor-blurOverlay').each(function() {
                            var $overlay = $(this);
                            if ($overlay.css("left") === left &&
                                    $overlay.css("top") === top &&
                                    $overlay.css("width") === width &&
                                    $overlay.css("height") === height) {
                                $overlay.remove();
                            }
                        });

                        var input = $blurInput;
                        var value = $(input).attr("value");
                        var name = $(input).attr("name");
                        $("input[name='" + name + "'][value='" + value + "']").remove();

                        $blurInput.remove();
                        $blurOverlay.remove();
                        return false;
                    }
                });

                $blurOverlay.append($blurOverlayRemove);

                $editor.append($blurOverlay);
            };

            $edit.find('.imageEditor-addBlurOverlay').bind('click', function() {
                var $imageEditorCanvas = $editor.find('.imageEditor-image canvas');
                var left = Math.floor($imageEditorCanvas.attr('width') / 2 - 50);
                var top = Math.floor($imageEditorCanvas.attr('height') / 2 - 50);
                var width = 100;
                var height = 100;
                addSizeBox(null, left, top, width, height);
            });



======================================================================***/
