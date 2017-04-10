/* global define, ENABLE_PADDED_CROPS */
//# sourceURL=image.js

define([ 'jquery', 'bsp-utils', 'v3/input/ImageEditor' ], function($, bsp_utils, ImageEditor) {
    bsp_utils.onDomInsert(document, '.imageEditor', {
        insert: function(element) {
            if (!ENABLE_PADDED_CROPS) {
                return;
            }

            var imageEditor = Object.create(ImageEditor);

            /**
             * Update the preview image for a single size group.
             *
             * @param String [groupName]
             * The name of the group you are resizing.
             * Leave this undefined to update all the previews.
             */
            imageEditor.sizesUpdatePreview = function(groupName) {

                var groupInfos, operations, self, area;

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

                    // Adjust image preview with padding
                    area = self.sizesGetImageArea(width, height, sizeInfoFirst);
                    var inputX, inputY, inputWidth, inputHeight;
                    inputX = Number(sizeInfoFirst.inputs.x.val());
                    inputY = Number(sizeInfoFirst.inputs.y.val());
                    inputWidth = Number(sizeInfoFirst.inputs.width.val());
                    inputHeight = Number(sizeInfoFirst.inputs.height.val());

                    var padTop = inputY < 0 ? Math.abs(inputY) / bounds.height * height : 0;
                    var padBottom = inputY + inputHeight > 1 ? (inputY + inputHeight - 1) / bounds.height * height : 0;

                    var padLeft = inputX < 0 ? Math.abs(inputX) / bounds.width * width : 0;
                    var padRight = inputX + inputWidth > 1 ? (inputX + inputWidth - 1) / bounds.width * width : 0;

                    bounds.top -= area.topPadPx;
                    bounds.left -= area.leftPadPx;
                    bounds.height = bounds.height * (1 - padBottom - padTop);
                    bounds.width = bounds.width * (1 - padLeft - padRight);

                    $imageWrapper.css({
                        'padding' : (padTop * 100) + '% ' + (padRight * 100) + '% ' + (padBottom * 100) + '% ' + (padLeft * 100) + '%'
                    });

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


            };


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
            imageEditor.sizesGetSizeBounds = function(imageWidth, imageHeight, sizeInfo) {

                var sizeAspectRatio, height, left, self, top, width, area;

                self = this;

                var useFocusCrop = self.sizeInfoIsEmpty(sizeInfo);

                if (!useFocusCrop) {
                    left = parseFloat(sizeInfo.inputs.x.val())|| 0.0;
                    top = parseFloat(sizeInfo.inputs.y.val()) || 0.0;
                    width = parseFloat(sizeInfo.inputs.width.val()) || 0.0;
                    height = parseFloat(sizeInfo.inputs.height.val()) || 0.0;
                } else {
                    left = parseFloat(sizeInfo.focusCrop.x) || 0.0;
                    top = parseFloat(sizeInfo.focusCrop.y) || 0.0;
                    width = parseFloat(sizeInfo.focusCrop.width) || 0.0;
                    height = parseFloat(sizeInfo.focusCrop.height) || 0.0;
                }

                sizeAspectRatio = sizeInfo.aspectRatio;

                area = self.sizesGetImageArea(imageWidth, imageHeight, sizeInfo);

                // Check if cropping values have been previously set
                if (width === 0 || height === 0) {

                    width = sizeInfo.width;
                    height = sizeInfo.height;

                    // If no cropping values, and there is an aspect ratio for this size,
                    // make the crop area as big as possible while staying within the aspect ratio
                    if (sizeAspectRatio) {

                        width = imageHeight * sizeAspectRatio;
                        height = imageWidth / sizeAspectRatio;

                        if (width > imageWidth) {
                            width = height * sizeAspectRatio;
                        } else {
                            height = width / sizeAspectRatio;
                        }

                        var widthDiff = area.totalWidth - imageWidth;
                        var heightDiff = area.totalHeight - imageHeight;

                        left = widthDiff !== 0 ? widthDiff / 2 :  (imageWidth - width) / 2;
                        top = heightDiff !== 0 ? heightDiff / 2 : (imageHeight - height) / 2;

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
                    left = (left + area.left) * imageWidth;
                    top = (top + area.top) * imageHeight;
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
            };


            /**
             * Calculates area dimensions to displaying image for the given size.
             * Area will be "padded" around the image on the dimension with a greater
             * difference in aspect ratios between the image and the crop. The numbers
             * returned in percentages (top, left) are relative to the image, not the
             * total area.
             *
             */
            imageEditor.sizesGetImageArea = function(imageWidth, imageHeight, sizeInfo) {
                var imageAspectRatio, sizeAspectRatio, topPad, leftPad, paddedImageHeight, paddedImageWidth;

                imageAspectRatio = imageWidth / imageHeight;
                sizeAspectRatio = sizeInfo.aspectRatio;

                topPad = Math.max((imageAspectRatio / sizeAspectRatio - 1), 0) / 2;
                leftPad =  Math.max((sizeAspectRatio / imageAspectRatio - 1), 0) / 2;

                if (leftPad > topPad) {
                    paddedImageHeight = imageHeight;
                    paddedImageWidth = paddedImageHeight * sizeAspectRatio;
                } else {
                    paddedImageWidth = imageWidth;
                    paddedImageHeight = paddedImageWidth / sizeAspectRatio;
                }

                return {
                    top: topPad,
                    left: leftPad,
                    topPadPx: topPad * imageHeight,
                    leftPadPx: leftPad * imageWidth,
                    totalWidth: paddedImageWidth,
                    totalHeight: paddedImageHeight,
                    scale: 1 / (1 + (leftPad * 2))
                }
            };


            /**
             * Update the size and position of the cover.
             *
             * @param Object bounds
             * @param Number bounds.top
             * @param Number bounds.left
             * @param Number bounds.width
             * @param Number bounds.height
             */
            imageEditor.coverUpdate = function(bounds, sizeInfo) {

                var self, imageWidth, imageHeight, boundsRight, boundsBottom;

                self = this;

                imageWidth = self.dom.$image.width();
                imageHeight = self.dom.$image.height();

                var area = self.sizesGetImageArea(imageWidth, imageHeight, sizeInfo);

                boundsRight = bounds.left + (bounds.width * area.scale);
                boundsBottom = bounds.top + (bounds.height * area.scale);

                var scaledAreaWidth = area.totalWidth * area.scale;
                var scaledAreaHeight = area.totalHeight * area.scale;

                self.dom.$coverTop.css({
                    'height': bounds.top,
                    'width': scaledAreaWidth
                });
                self.dom.$coverLeft.css({
                    'height': bounds.height * area.scale,
                    'top': bounds.top,
                    'width': bounds.left
                });
                self.dom.$coverRight.css({
                    'height': bounds.height * area.scale,
                    'left': boundsRight,
                    'top': bounds.top,
                    'width': scaledAreaWidth - boundsRight
                });
                self.dom.$coverBottom.css({
                    'height': scaledAreaHeight - boundsBottom,
                    'top': boundsBottom,
                    'width': scaledAreaWidth
                });

                self.coverShow();
            };


            /**
             * Show the size box and resize it to show the crop settings
             * for a particular group size.
             *
             * @param String groupName
             */
            imageEditor.sizeBoxShow = function(groupName) {

                var bounds, self, sizeInfo, area;

                self = this;

                // Get the first sizeInfo object for this group
                sizeInfo = self.sizesGetGroupFirstSizeInfo(groupName);

                // Get the boundaries for the size box, based on the current image size on the page
                var imageWidth = self.dom.$image.width();
                var imageHeight = self.dom.$image.height();
                var imageContainer = self.dom.$imageContainer;
                bounds = self.sizesGetSizeBounds(imageWidth, imageHeight, sizeInfo);
                area = self.sizesGetImageArea(imageWidth, imageHeight, sizeInfo);

                if (area.topPadPx < area.leftPadPx) {
                    var originalImageWidth = self.dom.$image.width();
                    var transformCss = {
                        'transform': 'scale(' + area.scale + ')',
                        'transform-origin': 'top left'
                    };
                    imageContainer.css(transformCss);
                    imageContainer.css({
                        'padding-left' :  area.leftPadPx+ 'px',
                        'width' : (imageContainer.width() + (area.leftPadPx * 2)) + 'px',
                    });
                    self.dom.$image.width(originalImageWidth);
                } else {

                    imageContainer.css({
                        'padding-top' : area.topPadPx + 'px',
                        'height' : (imageContainer.height() + (area.topPadPx * 2)) + 'px'
                    });
                }

                bounds.left *= area.scale;
                bounds.top *= area.scale;

                self.coverUpdate(bounds, sizeInfo);
                self.coverShow();

                self.sizeBoxUpdate(groupName, bounds);

                self.sizeGroups[groupName].$sizeBox.show();
            };


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
            imageEditor.sizeBoxUpdate = function(groupName, bounds) {
                var self, $imageContainer, padData, sizeInfo, sizeBox;

                self = this;
                $imageContainer = self.dom.$imageContainer;
                sizeInfo = self.sizesGetGroupFirstSizeInfo(groupName);
                padData = self.sizesGetImageArea(self.dom.$image.width(), self.dom.$image.height(), sizeInfo);

                var transformCss = {
                    'transform': 'scale(' + padData.scale + ')',
                    'transform-origin': 'top left'
                };

                sizeBox = self.sizeGroups[groupName].$sizeBox;
                sizeBox.css(bounds);
                sizeBox.css(transformCss);
                sizeBox.data('exactHeight', bounds.height);
                sizeBox.data('exactWidth', bounds.width);
            };


            /**
             * Hide all the size boxes.
             */
            imageEditor.sizeBoxHide = function() {
                var self;
                self = this;
                self.coverHide();
                $.each(self.sizeGroups, function(groupName, groupInfo) {
                    groupInfo.$sizeBox.hide();
                });

                // Resets styles injected for padded crop
                self.dom.$imageContainer.attr('style', '');
            };


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
            imageEditor.sizeBoxMousedownDragHandler = function(groupName, filterBoundsFunction) {

                var mousedownHandler, self, $sizeBox;

                self = this;

                $sizeBox = self.sizeGroups[groupName].$sizeBox;

                mousedownHandler = function(mousedownEvent) {

                    var aspectRatio, sizeInfo, element, imageWidth, imageHeight, areaWidth, areaHeight, original, sizeBoxPosition;

                    // The element that was dragged
                    element = this;

                    // Get the aspect ratio for this group
                    sizeInfo = self.sizesGetGroupFirstSizeInfo(groupName);
                    aspectRatio = sizeInfo.aspectRatio;

                    sizeBoxPosition = $sizeBox.position();

                    original = {
                        'left': sizeBoxPosition.left,
                        'top': sizeBoxPosition.top,
                        'width': $sizeBox.width(),
                        'height': $sizeBox.width() / aspectRatio,
                        'pageX': mousedownEvent.pageX,
                        'pageY': mousedownEvent.pageY
                    };

                    imageWidth = self.dom.$image.width();
                    imageHeight = self.dom.$image.height();

                    // Adjust height and width if padded crop is used
                    var area = self.sizesGetImageArea(imageWidth, imageHeight, sizeInfo);
                    areaWidth = area.totalWidth;
                    areaHeight = area.totalHeight;

                    // Drag and resize boundaries are limited to scaled width/height
                    var scaledWidth = areaWidth * area.scale;
                    var scaledHeight = areaHeight * area.scale;

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

                            overflow = bounds.left + (bounds.width * area.scale) - scaledWidth;
                            if (overflow > 0) {
                                bounds.left -= overflow;
                            }

                            overflow = bounds.top + (bounds.height * area.scale) - scaledHeight;
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
                            overflow = bounds.left + (bounds.width * area.scale) - scaledWidth;
                            if (overflow > 0) {
                                bounds.width -= overflow / area.scale;
                                if (aspectRatio) {
                                    bounds.height = bounds.width / aspectRatio;
                                }
                            }

                            // Check if the box extends past the bottom
                            overflow = bounds.top + (bounds.height * area.scale) - scaledHeight;
                            if (overflow > 0) {
                                bounds.height -= overflow / area.scale;
                                if (aspectRatio) {
                                    bounds.width = bounds.height * aspectRatio;
                                }
                            }
                        }

                        // Now that the bounds have been sanitized,
                        // update the sizebox display
                        self.coverUpdate(bounds, sizeInfo);
                        self.sizeBoxUpdate(groupName, bounds);

                        // Trigger an event to tell others the size box has changed size
                        if (!bounds.moving) {
                            $sizeBox.trigger('sizeBoxResize');
                        }

                    }, function() {

                        var sizeBoxHeight, sizeBoxPosition, sizeBoxWidth, x, y;

                        // .drag() end callback

                        // Now that we're done dragging, update the size box

                        sizeBoxPosition = $sizeBox.position();
                        sizeBoxWidth = $sizeBox.data('exactWidth');
                        sizeBoxHeight = $sizeBox.data('exactHeight');

                        x = sizeBoxPosition.left / area.scale / imageWidth - area.left;
                        y = sizeBoxPosition.top / area.scale / imageHeight - area.top;

                        // Set the hidden inputs to the current bounds.
                        self.sizesSetGroupBounds(groupName, {
                            x: x,
                            y: y,
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
            };


            /**
             * Initialize the "click to set focus" functionality.
             */
            imageEditor.focusInit = function() {

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


                if (self.dom.$focusInputX.val() !== '' && self.dom.$focusInputY.val() !== '') {
                    self.insertFocusPoint(self.dom.$focusImage, self.dom.$focusInputX.val() * 100, self.dom.$focusInputY.val() * 100);
                }

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

                        // Update focus crop info to be used
                        // in #sizeBoxShow and #sizesUpdatePreview
                        sizeInfo.focusCrop = self.focusGetCrop({
                            x: focus.xPercent,
                            y: focus.yPercent
                        }, originalAspect, aspect);

                        // Set the cropping for this size group
                        self.sizesUpdatePreview(groupName);
                    });

                    // When switching to sizes tab, update the thumbnails
                    self.sizesNeedsUpdate = true;

                    self.dom.$focusInputX.val(focus.xPercent);
                    self.dom.$focusInputY.val(focus.yPercent);

                    self.insertFocusPoint(self.dom.$focusImage, focus.xPercent * 100, focus.yPercent * 100);
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
            };


            imageEditor.init(element);
            $(element).data('imageEditor', imageEditor);
        }
    });
});
