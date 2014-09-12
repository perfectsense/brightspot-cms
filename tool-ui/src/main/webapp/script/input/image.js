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
    'pixastic/actions/sharpen' ],
    
function($, bsp_utils) {
    var INPUT_NAMES = 'x y width height texts textSizes textXs textYs textWidths'.split(' ');
    var DELIMITER = 'aaaf7c5a9e604daaa126f11e23e321d8';

    bsp_utils.onDomInsert(document, '.imageEditor', {
        'insert': function(element) {
            var $editor = $(element);
            var $form = $editor.closest('form');
            var $image = $editor.find('.imageEditor-image img');
            var imageSrc = $image.attr('src');
            var $imageReSizeScale = $image.attr('data-scale') !== undefined && $image.attr('data-scale') !== "" ? $image.attr('data-scale') : 1.0;
            var $originalImage = $image;
            var $imageClone = $image.clone();
            var imageClone = $imageClone[0];

            $editor.find('> .imageEditor-aside').bind('mousewheel', function(event, delta, deltaX, deltaY) {
                var $aside = $(this),
                        maxScrollTop = $.data(this, 'imageEditor-maxScrollTop');

                if (typeof maxScrollTop === 'undefined') {
                    maxScrollTop = $aside.prop('scrollHeight') - $aside.innerHeight();
                    $.data(this, 'imageEditor-maxScrollTop', maxScrollTop);
                }

                if ((deltaY > 0 && $aside.scrollTop() === 0) ||
                        (deltaY < 0 && $aside.scrollTop() >= maxScrollTop)) {
                    event.preventDefault();
                }
            });

            var $tools = $editor.find('.imageEditor-tools ul');
            var $edit = $editor.find('.imageEditor-edit');
            var $dataName = $editor.parents('.inputContainer').attr('data-name');
            var $editButton = $('<li/>', {
                'html': $('<a/>', {
                    'class': 'action-image-edit',
                    'text': 'Edit Image',
                    'click': function() {
                        $edit.popup('source', $(this));
                        $edit.popup('open');
                        $(".imageEditor-edit input[name=\'" + $dataName + ".blur\']").each(function(){
                            var attributes = $(this).val().split("x");
                            addSizeBox(this, attributes[0], attributes[1], attributes[2], attributes[3]);
                        });
                        return false;
                    }
                })
            });

            $tools.prepend($editButton);

            var $editInputs = $('<div/>');

            $editButton.append($editInputs);

            var previousOperations;
            var processImageTimer;

            var processImage = function() {
                var operations = { };

                var applyOperation = function($input) {
                    var value = $input.is(':checkbox') ? $input.is(':checked') : parseFloat($input.val());

                    if (value === false || isNaN(value)) {
                        return;
                    }

                    var name = /.([^.]+)$/.exec($input.attr('name'));
                    name = name ? name[1] : null;

                    if (name === 'brightness') {
                        operations.brightness = operations.brightness || { };
                        operations.brightness.brightness = value * 150;
                        operations.brightness.legacy = true;

                    } else if (name === 'contrast') {
                        operations.brightness = operations.brightness || { };
                        operations.brightness.contrast = value < 0 ? value : value * 3;

                    } else if (name === 'flipH') {
                        operations.fliph = operations.fliph || { };

                    } else if (name === 'flipV') {
                        operations.flipv = operations.flipv || { };

                    } else if (name === 'grayscale') {
                        operations.desaturate = operations.desaturate || { };

                    } else if (name === 'invert') {
                        operations.invert = operations.invert || { };

                    } else if (name === 'rotate') {
                        operations.rotate = operations.rotate|| { };
                        operations.rotate.angle = -value;

                    } else if (name === 'sepia') {
                        operations.sepia = operations.sepia || { };

                    } else if (name === 'sharpen') {
                        operations.sharpen = operations.sharpen || { };
                        operations.sharpen.amount = value;

                    } else if (name === "blur") {
                        operations.blurfast = operations.blurfast || {"count" : 0, "data" : []};
                        var values = $input.val().split("x");
                        var rect = {"left" : values[0], "top" : values[1], "width" : values[2], "height" : values[3]};
                        operations.blurfast.data[operations.blurfast.count] = {"amount" : 1.0, "rect" :rect};
                        operations.blurfast.count++;
                    }
                }

                $edit.find(":input[name$='.rotate']").each(function() {
                    applyOperation($(this));
                });

                $edit.find(":input:not([name$='.rotate'])").each(function() {
                    applyOperation($(this));
                });

                var serialized = JSON.stringify(operations);

                if (serialized !== previousOperations) {
                    previousOperations = serialized;

                    $editInputs.empty();
                    $edit.find(':input').each(function() {
                        var $input = $(this);

                        if (!$input.is(':checkbox') || $input.is(':checked')) {
                            $editInputs.append($('<input/>', {
                                'type': 'hidden',
                                'name': $input.attr('name'),
                                'value': $input.val()
                            }));
                        }
                    });

                    var operationKeys = [ ];
                    var operationKeyIndex = 0;

                    for (var key in operations) {
                        if (operations.hasOwnProperty(key)) {
                            operationKeys[operationKeys.length] = key;
                        }
                    }

                    var operateItem = function(processedImage, index, key, operations, operationIndex) {
                        if (operationIndex < operations.length) {
                            Pixastic.process(processedImage, key, operations[operationIndex], function(newImage) {
                                ++ operationIndex;
                                operateItem(newImage, index, key, operations, operationIndex);
                            });
                        } else {
                            ++ index;
                            operate(processedImage, index);
                        }
                    }

                    var operate = function(processedImage, index) {
                        if (index < operationKeys.length) {
                            var key = operationKeys[index];

                            if (operations[key].count !== undefined) {
                                operateItem(processedImage, index, key, operations[key].data, 0);
                            } else {
                                Pixastic.process(processedImage, key, operations[key], function(newImage) {
                                    ++ index;
                                    operate(newImage, index);
                                });
                            }

                        } else {
                            if ($image !== $originalImage) {
                                $image.remove();
                            }

                            $originalImage.before(processedImage);
                            $originalImage.hide();
                            $image = $(processedImage);

                            processImageTimer = setTimeout(processImage, 100);
                        }
                    };

                    operate(imageClone, 0);

                } else {
                    processImageTimer = setTimeout(processImage, 100);
                }
            };

            processImage();

            $edit.popup('container').bind('open', function() {
                processImage();
            });

            $edit.popup('container').bind('close', function() {
                if (processImageTimer) {
                    clearTimeout(processImageTimer);
                }
            });

            var $resetButton = $('<button/>', {
                'text': 'Reset',
                'click': function() {
                    $edit.find(':input').each(function() {
                        var $input = $(this);

                        if ($input.is(':checkbox')) {
                            $input.removeAttr('checked');

                        } else {
                            $input.val(0);
                        }
                    });
                    $editor.find(".imageEditor-blurOverlay").remove();

                    processImage();

                    return false;
                }
            });

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
                    return function(event) {
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

                        $.drag(this, event, function(event) {

                        }, function(event) {
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

            var addHotSpot = function(input, left, top, width, height) {
                var $input = $(input);

                var $hotSpotOverlay = $('<div/>', {
                    'class': 'imageEditor-hotSpotOverlay',
                    'css': {
                        'height': height + 'px',
                        'left': left  + 'px',
                        'position': 'absolute',
                        'top': top + 'px',
                        'width': width + 'px',
                        'z-index': 1
                    },
                    'data-type-id' : $input.find('input[name$="file.hotspots.typeId"]').val()
                });

                var $hotSpotOverlayBox = $('<div/>', {
                    'class': 'imageEditor-hotSpotOverlayBox',
                    'css': {
                        'height': height + 'px',
                        'position': 'absolute',
                        'width': width + 'px',
                        'z-index': 1
                    }
                });

                var $hotSpotOverlayLabel = $('<div/>', {
                    'class': 'imageEditor-textOverlayLabel',
                    'text': 'HotSpot'
                });

                var sizeAspectRatio = width / height;
                var updateSizeBox = function(callback) {
                    return function(event) {
                        var sizeBoxPosition = $hotSpotOverlayBox.parent().position();
                        var original = {
                            'left': sizeBoxPosition.left,
                            'top': sizeBoxPosition.top,
                            'width': $hotSpotOverlayBox.width(),
                            'height': $hotSpotOverlayBox.height(),
                            'pageX': event.pageX,
                            'pageY': event.pageY
                        };

                        var imageWidth = $image.width();
                        var imageHeight = $image.height();

                        $.drag(this, event, function(event) {

                        }, function(event) {
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

                            $hotSpotOverlay.css(bounds);
                            $hotSpotOverlayBox.css('width', bounds.width);
                            $hotSpotOverlayBox.css('height', bounds.height);

                        // Set the hidden inputs to the current bounds.
                        }, function() {
                            var hotSpotOverlayBoxPosition = $hotSpotOverlayBox.parent().position();
                            var scale = $imageReSizeScale;
                            var rotation = $edit.find(":input[name$='.rotate']").first().val();

                            if ($imageReSizeScale < 1) {
                                if (rotation === '0') {
                                    var cavnasWidth = $image.parent().find("canvas").width();
                                    scale = (cavnasWidth / 1000) * scale;
                                } else if (rotation === '90' || rotation === '-90') {
                                    var cavnasHeight = $image.parent().find("canvas").height();
                                    scale = (cavnasHeight / 1000) * scale;
                                }
                            }
                            scale = 1 / scale;

                            var x = parseInt(hotSpotOverlayBoxPosition.left * scale);
                            var y = parseInt(hotSpotOverlayBoxPosition.top * scale);
                            var width;
                            var height;
                            if (rotation === "0") {
                                width = parseInt($hotSpotOverlayBox.width() * scale);
                                height = parseInt($hotSpotOverlayBox.height() * scale);
                            } else if (rotation === '90' || rotation === '-90') {
                                height = parseInt($hotSpotOverlayBox.width() * scale);
                                width = parseInt($hotSpotOverlayBox.height() * scale);

                                if (rotation === '90') {
                                    var originalWidth = $image.width() / scale;
                                    y = originalWidth - x + height;
                                    x = parseInt(hotSpotOverlayBoxPosition.top * scale);
                                } else if (rotation === '-90') {
                                    var originalWidth = $image.height() * scale;
                                    x = originalWidth - parseInt(hotSpotOverlayBoxPosition.top * scale) - width;
                                    y = parseInt(hotSpotOverlayBoxPosition.left * scale);
                                }
                            }

                            if ($edit.find(":input[name$='.flipH']").first().prop('checked')) {
                                var originalWidth = $image.width() * scale;
                                x = originalWidth - x - width;
                            }

                            if ($edit.find(":input[name$='.flipV']").first().prop('checked')) {
                                var originalHeight = $image.height() * scale;
                                y = originalHeight - y - height;
                            }

                            $input.find(':input[name$="x"]').val(x);
                            $input.find(':input[name$="y"]').val(y);
                            $input.find(':input[name$="width"]').val(width);
                            $input.find(':input[name$="height"]').val(height);
                        });

                        return false;
                    };
                };

                $hotSpotOverlayBox.append($('<div/>', {
                    'class': 'imageEditor-resizer imageEditor-resizer-left',
                    'mousedown': updateSizeBox(function(event, original, delta) {
                        $input.addClass("state-focus");
                        $hotSpotOverlay.addClass("selected");
                        return {
                            'left': original.left + delta.x,
                            'width': original.width - delta.x
                        };
                    })
                }));
                $hotSpotOverlayBox.append($('<div/>', {
                    'class': 'imageEditor-resizer imageEditor-resizer-right',
                    'mousedown': updateSizeBox(function(event, original, delta) {
                        $input.addClass("state-focus");
                        $hotSpotOverlay.addClass("selected");
                        return {
                            'left': original.left,
                            'width': original.width + delta.x
                        };
                    })
                }));
                $hotSpotOverlayBox.append($('<div/>', {
                    'class': 'imageEditor-resizer imageEditor-resizer-bottomRight',
                    'mousedown': updateSizeBox(function(event, original, delta) {
                        $input.addClass("state-focus");
                        $hotSpotOverlay.addClass("selected");
                        return {
                            'width': original.width + delta.constrainedX,
                            'height': original.height + delta.constrainedY
                        };
                    })
                }));

                $hotSpotOverlay.append($hotSpotOverlayBox);

                $hotSpotOverlayLabel.mousedown(updateSizeBox(function(event, original, delta) {
                    $input.addClass("state-focus");
                    $hotSpotOverlay.addClass("selected");
                    return {
                        'moving': true,
                        'left': original.left + delta.x,
                        'top': original.top + delta.y
                    };
                }));

                $hotSpotOverlay.mousedown(function() {
                    $('.imageEditor-hotSpotOverlay').removeClass("selected");
                    $('.state-focus').removeClass("state-focus");
                    $input.addClass("state-focus");
                    $hotSpotOverlay.addClass("selected");
                });
                $editor.mouseleave(function() {
                    $input.removeClass("state-focus");
                    $hotSpotOverlay.removeClass("selected");
                });

                $input.mouseover(function() {
                    $input.addClass("state-focus");
                    $hotSpotOverlay.addClass("selected");
                });

                $input.mouseleave(function() {
                    $input.removeClass("state-focus");
                    $hotSpotOverlay.removeClass("selected");
                });

                $hotSpotOverlay.append($hotSpotOverlayLabel);

                var $hotSpotOverlayRemove = $('<div/>', {
                    'class': 'imageEditor-textOverlayRemove',
                    'text': 'Remove',
                    'click': function() {
                        var left = $hotSpotOverlay.css("left");
                        var top = $hotSpotOverlay.css("top");
                        var width = $hotSpotOverlay.css("width");
                        var height = $hotSpotOverlay.css("height");

                        $('.imageEditor-hotSpotOverlay').each(function() {
                            var $overlay = $(this);
                            if ($overlay.css("left") === left &&
                                    $overlay.css("top") === top &&
                                    $overlay.css("width") === width &&
                                    $overlay.css("height") === height) {
                                $overlay.remove();
                            }
                        });

                        $input.addClass("toBeRemoved");
                        $input.find("input").attr("disabled", "disabled");
                        $hotSpotOverlay.remove();
                        return false;
                    }
                });

                $hotSpotOverlay.append($hotSpotOverlayRemove);
                $editor.append($hotSpotOverlay);
            };

            var rotateHotSpot = function (angle, x, y, width, height) {
                var $rotatedHotSpot = {};
                angle = parseInt(angle);

                var scale = $imageReSizeScale;
                if ($imageReSizeScale < 1) {
                    var cavnasHeight = $image.parent().find("canvas").height();
                    scale = (cavnasHeight / 1000) * scale;
                }
                if (angle === 90 ) {
                    var originalHeight = $image.width() / scale;

                    $rotatedHotSpot.x = originalHeight - y - width;
                    $rotatedHotSpot.y = x;
                    $rotatedHotSpot.width = height;
                    $rotatedHotSpot.height = width;
                } else if (angle === -90 ) {
                    var originalWidth = $image.height() / scale;

                    $rotatedHotSpot.x = y;
                    $rotatedHotSpot.y = originalWidth - x - height;
                    $rotatedHotSpot.width = height;
                    $rotatedHotSpot.height = width;
                }
                return $rotatedHotSpot;
            };

            var $initalizeHotSpots = function() {
                $image.parents(".inputContainer").find('.imageEditor-hotSpotOverlay').remove();
                var $hotSpots = $image.parents(".inputContainer").find('.hotSpots .objectInputs');
                if ($hotSpots !== undefined && $hotSpots.length > 0) {
                    var scale = $imageReSizeScale;
                    var rotation = $edit.find(":input[name$='.rotate']").first().val();
                    if ($imageReSizeScale < 1) {
                        if (rotation === '0') {
                            var cavnasWidth = $image.parent().find("canvas").width();
                            scale = (cavnasWidth / 1000) * scale;
                        } else if (rotation === '90' || rotation === '-90') {
                            var cavnasHeight = $image.parent().find("canvas").height();
                            scale = (cavnasHeight / 1000) * scale;
                        }
                    }

                    $hotSpots.each(function() {
                        var $this = $(this);
                        if (!$this.parents('li').first().hasClass('toBeRemoved')) {
                            var x = parseInt($this.find(':input[name$="x"]').val());
                            var y = parseInt($this.find(':input[name$="y"]').val());
                            var width = parseInt($this.find(':input[name$="width"]').val());
                            var height = parseInt($this.find(':input[name$="height"]').val());

                            if (rotation !== '0') {
                                var $rotatedHotSpot = rotateHotSpot(rotation, x, y, width, height);
                                x = $rotatedHotSpot.x;
                                y = $rotatedHotSpot.y;
                                width = $rotatedHotSpot.width;
                                height = $rotatedHotSpot.height;
                            }

                            if ($edit.find(":input[name$='.flipH']").first().prop('checked')) {
                                var originalWidth = $image.width() / scale;
                                x = originalWidth - x - width;
                            }

                            if ($edit.find(":input[name$='.flipV']").first().prop('checked')) {
                                var originalHeight = $image.height() / scale;
                                y = originalHeight - y - height;
                            }

                            x = x * scale;
                            y = y * scale;
                            width = width * scale;
                            height = height * scale;

                            addHotSpot($this.parent(), x, y, width, height);
                        }
                    });

                    $image.parents(".inputContainer").find(".hotSpots").unbind('change');
                    $image.parents(".inputContainer").find(".hotSpots").bind('change', function() {
                        $initalizeHotSpots();
                    });
                }
            };

            $initalizeHotSpots();

            $editor.parents('.inputContainer').bind('create', function() {
               var defaultX = parseInt(($image.width() / $imageReSizeScale) / 2);
               var defaultY = parseInt(($image.height() / $imageReSizeScale) / 2);

               $image.parents(".inputContainer").find('.hotSpots .objectInputs').each(function() {
                   var $this = $(this);

                   $this.find(':input[name$="x"]').val($this.find(':input[name$="x"]').val() === "" ? defaultX : $this.find(':input[name$="x"]').val());
                   $this.find(':input[name$="y"]').val($this.find(':input[name$="y"]').val() === "" ? defaultY : $this.find(':input[name$="y"]').val());
                   $this.find(':input[name$="width"]').val($this.find(':input[name$="width"]').val() === "" ? 100 : $this.find(':input[name$="width"]').val());
                   $this.find(':input[name$="height"]').val($this.find(':input[name$="height"]').val() === "" ? 100 : $this.find(':input[name$="height"]').val());
               });
               $initalizeHotSpots();
            });

            $edit.find(":input[name$='.rotate']").change(function() {
                setTimeout($initalizeHotSpots(), 250);
            });
            $edit.find(":input[name$='.flipH']").change(function() {
                setTimeout($initalizeHotSpots(), 250);
            });

            $edit.find(":input[name$='.flipV']").change(function() {
                setTimeout($initalizeHotSpots(), 250);
            });

            $edit.append($resetButton);

            $edit.popup();
            $edit.popup('container').addClass('imageEditor-editPopup');
            $edit.popup('close');

            var $coverTop = $('<div/>', {
                'class': 'imageEditor-cover',
                'css': {
                    'display': 'none',
                    'left': '0',
                    'position': 'absolute',
                    'top': '0'
                }
            });

            var $coverLeft = $coverTop.clone(true);
            var $coverRight = $coverTop.clone(true);
            var $coverBottom = $coverTop.clone(true);

            $editor.append($coverTop);
            $editor.append($coverLeft);
            $editor.append($coverRight);
            $editor.append($coverBottom);

            var updateCover = function(bounds) {
                var imageWidth = $image.width();
                var imageHeight = $image.height();
                var boundsRight = bounds.left + bounds.width;
                var boundsBottom = bounds.top + bounds.height;

                $coverTop.css({
                    'height': bounds.top,
                    'width': imageWidth
                });
                $coverLeft.css({
                    'height': bounds.height,
                    'top': bounds.top,
                    'width': bounds.left
                });
                $coverRight.css({
                    'height': bounds.height,
                    'left': boundsRight,
                    'top': bounds.top,
                    'width': imageWidth - boundsRight
                });
                $coverBottom.css({
                    'height': imageHeight - boundsBottom,
                    'top': boundsBottom,
                    'width': imageWidth
                });

                $coverTop.show();
                $coverLeft.show();
                $coverRight.show();
                $coverBottom.show();
            };

            var $sizes = $editor.find('.imageEditor-sizes table');
            var $sizeSelectors = $('<ul/>', { 'class': 'imageEditor-sizeSelectors' });
            var $mainInputs = { };

            $sizes.find('th').each(function() {
                var $th = $(this);
                var $tr = $th.closest('tr');

                var sizeName = $tr.attr('data-size-name');
                var $source = $th.popup('source');
                if ($source) {
                    var sizes = $source.closest('.inputContainer').attr('data-standard-image-sizes');
                    if (sizes) {
                        if ($.inArray(sizeName, sizes.split(' ')) < 0) {
                            return;
                        }
                    }
                }

                var independent = $tr.attr('data-size-independent') === 'true';
                var sizeWidth = parseFloat($tr.attr('data-size-width'));
                var sizeHeight = parseFloat($tr.attr('data-size-height'));
                var sizeAspectRatio = sizeWidth / sizeHeight;

                var $input = { };

                $.each(INPUT_NAMES, function(index, name) {
                    $input[name] = $tr.find(':input[name$=".' + name + '"]');
                });

                var ratio = Math.floor(sizeAspectRatio * 100) / 100;
                var $mainInput = $mainInputs['' + ratio];

                if (!independent) {
                    if ($mainInput) {
                        $mainInput['$sizeLabel'].html(
                                $mainInput['$sizeLabel'].html() +
                                '<br><span title="' + $th.text() + '">' + $th.text() + '</span>');

                        $.each(INPUT_NAMES, function(index, name) {
                            $mainInput[name] = $mainInput[name].add($input[name]);
                        });
                        return;

                    } else {
                        $mainInputs['' + (ratio - 0.02)] = $input;
                        $mainInputs['' + (ratio - 0.01)] = $input;
                        $mainInputs['' + ratio] = $input;
                        $mainInputs['' + (ratio + 0.01)] = $input;
                        $mainInputs['' + (ratio + 0.02)] = $input;
                    }
                }

                var $sizeBox = $('<div/>', {
                    'class': 'imageEditor-sizeBox',
                    'css': { 'position': 'absolute' }
                });

                $sizeBox.hide();
                $editor.append($sizeBox);

                var getSizeBounds = function($image) {
                    var imageWidth = $image.width();
                    var imageHeight = $image.height();

                    var left = parseFloat($input.x.val()) || 0.0;
                    var top = parseFloat($input.y.val()) || 0.0;
                    var width = parseFloat($input.width.val()) || 0.0;
                    var height = parseFloat($input.height.val()) || 0.0;

                    if (width === 0.0 || height === 0.0) {
                        width = imageHeight * sizeAspectRatio;
                        height = imageWidth / sizeAspectRatio;

                        if (width > imageWidth) {
                            width = height * sizeAspectRatio;
                        } else {
                            height = width / sizeAspectRatio;
                        }

                        left = (imageWidth - width) / 2;
                        top = 0;

                    } else {
                        left *= imageWidth;
                        top *= imageHeight;
                        width *= imageWidth;
                        height *= imageHeight;
                    }

                    return {
                        'left': left,
                        'top': top,
                        'width': width,
                        'height': height
                    };
                };

                var $sizeButton = $('<li/>', {
                    'class': 'imageEditor-sizeButton',
                    'click': function() {
                        var $item = $(this).closest('li');

                        if ($item.is('.imageEditor-sizeSelected')) {
                            $item.removeClass('imageEditor-sizeSelected');
                            $coverTop.hide();
                            $coverLeft.hide();
                            $coverRight.hide();
                            $coverBottom.hide();
                            $sizeBox.hide();
                            return false;

                        } else {
                            $item.closest('ul').find('li').removeClass('imageEditor-sizeSelected');
                            $item.addClass('imageEditor-sizeSelected');
                        }

                        $editor.find('.imageEditor-sizeBox').hide();

                        var bounds = getSizeBounds($image);

                        updateCover(bounds);
                        $sizeBox.css(bounds);
                        $sizeBox.show();

                        $sizeBox.find('.imageEditor-textOverlay').each(function(index) {
                            var $textOverlay = $(this);

                            $textOverlay.trigger('resizeTextOverlayFont');

                            var textWidth = parseFloat($input.textWidths.val().split(DELIMITER)[index + 1]) || 0.0;

                            if (textWidth !== 0.0) {
                                var textX = parseFloat($input.textXs.val().split(DELIMITER)[index + 1]) || 0.0;
                                var textY = parseFloat($input.textYs.val().split(DELIMITER)[index + 1]) || 0.0;

                                $textOverlay.css({
                                    'left': (textX * 100) + '%',
                                    'top': (textY * 100) + '%',
                                    'width': (textWidth * 100) + '%'
                                });
                            }
                        });

                        return false;
                    }
                });

                $sizeSelectors.append($sizeButton);

                var updatePreview = function() {
                    var $body = $(document.body);
                    $body.append($imageClone);
                    var bounds = getSizeBounds($imageClone);
                    $imageClone.remove();
                    Pixastic.process(imageClone, 'crop', bounds, function(newImage) {
                        var $preview = $sizeButton.find('.imageEditor-sizePreview');
                        $preview.empty();
                        $preview.append(newImage);
                    });
                };

                var updateSizeBox = function(callback) {
                    return function(event) {
                        var sizeBoxPosition = $sizeBox.position();
                        var original = {
                            'left': sizeBoxPosition.left,
                            'top': sizeBoxPosition.top,
                            'width': $sizeBox.width(),
                            'height': $sizeBox.height(),
                            'pageX': event.pageX,
                            'pageY': event.pageY
                        };

                        var imageWidth = $image.width();
                        var imageHeight = $image.height();

                        $.drag(this, event, function(event) {

                        }, function(event) {
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

                            updateCover(bounds);
                            $sizeBox.css(bounds);
                            $sizeBox.find('.imageEditor-textOverlay').trigger('resizeTextOverlayFont');

                        // Set the hidden inputs to the current bounds.
                        }, function() {
                            var sizeBoxPosition = $sizeBox.position();
                            var sizeBoxWidth = $sizeBox.width();
                            var sizeBoxHeight = $sizeBox.height();

                            $input.x.val(sizeBoxPosition.left / imageWidth);
                            $input.y.val(sizeBoxPosition.top / imageHeight);
                            $input.width.val(sizeBoxWidth / imageWidth);
                            $input.height.val(sizeBoxWidth / sizeAspectRatio / imageHeight);

                            updatePreview();
                            $sizeBox.find('.imageEditor-textOverlay').trigger('resizeTextOverlayFont');
                        });

                        return false;
                    };
                };

                $sizeBox.mousedown(updateSizeBox(function(event, original, delta) {
                    return {
                        'moving': true,
                        'left': original.left + delta.x,
                        'top': original.top + delta.y
                    };
                }));

                $sizeBox.append($('<div/>', {
                    'class': 'imageEditor-resizer imageEditor-resizer-topLeft',
                    'mousedown': updateSizeBox(function(event, original, delta) {
                        return {
                            'left': original.left + delta.constrainedX,
                            'top': original.top + delta.constrainedY,
                            'width': original.width - delta.constrainedX,
                            'height': original.height - delta.constrainedY
                        };
                    })
                }));
                $sizeBox.append($('<div/>', {
                    'class': 'imageEditor-resizer imageEditor-resizer-bottomRight',
                    'mousedown': updateSizeBox(function(event, original, delta) {
                        return {
                            'width': original.width + delta.constrainedX,
                            'height': original.height + delta.constrainedY
                        };
                    })
                }));

                var $sizeLabel = $('<span/>', {
                    'class': 'imageEditor-sizeLabel',
                    'html': '<span title="' + $th.text() + '">' + $th.text() + '</span>'
                });

                $input['$sizeLabel'] = $sizeLabel;
                $sizeButton.append($sizeLabel);

                var $sizePreview = $('<div/>', {
                    'class': 'imageEditor-sizePreview',
                    'html': $('<img/>', {
                        'alt': $th.text(),
                        'src': imageSrc,
                        'load': function() {
                            updatePreview();
                        }
                    })
                });

                $sizeButton.append($sizePreview);

                var textOverlayIndex = 0;

                $sizeLabel.after($('<a/>', {
                    'class': 'imageEditor-addTextOverlay',
                    'text': 'Add Text',
                    'click': function(event, init) {
                        ++ textOverlayIndex;

                        var $textOverlay = $('<div/>', {
                            'class': 'imageEditor-textOverlay',
                            'css': {
                                'left': (($input.textXs.val().split(DELIMITER)[textOverlayIndex] || 0.25) * 100) + '%',
                                'position': 'absolute',
                                'top': (($input.textYs.val().split(DELIMITER)[textOverlayIndex] || 0.25) * 100) + '%',
                                'width': (($input.textWidths.val().split(DELIMITER)[textOverlayIndex] || 0.50) * 100) + '%',
                                'z-index': 1
                            }
                        });

                        $sizeBox.append($textOverlay);

                        var $textOverlayLabel = $('<div/>', {
                            'class': 'imageEditor-textOverlayLabel',
                            'text': 'Text #' + textOverlayIndex
                        });

                        var resizeTextOverlayFont = function() {
                            if (!$sizeBox.is(':visible')) {
                                return;
                            }

                            var textSizes = '';

                            $sizeBox.find('.imageEditor-textOverlay').each(function() {
                                var $to = $(this),
                                        $body = $($to.find('.rte-container iframe')[0].contentDocument.body),
                                        originalFontSize = $.data(this, 'imageEditor-originalFontSize'),
                                        textSize,
                                        newFontSize;

                                if (!originalFontSize && $body.is('.rte-loaded')) {
                                    originalFontSize = parseFloat($body.css('font-size'));
                                    $.data(this, 'imageEditor-originalFontSize', originalFontSize);
                                }

                                textSizes += DELIMITER;

                                if (originalFontSize > 0) {
                                    textSize = 1 / sizeHeight * originalFontSize;
                                    textSizes += textSize;
                                    $body.css('font-size', $sizeBox.height() * textSize);
                                }
                            });

                            $input.textSizes.val(textSizes);
                        };

                        $textOverlay.bind('resizeTextOverlayFont', resizeTextOverlayFont);

                        var updateTextOverlay = function(callback) {
                            return function(event) {
                                resizeTextOverlayFont();

                                var textOverlayPosition = $textOverlay.position();
                                var original = {
                                    'left': textOverlayPosition.left,
                                    'top': textOverlayPosition.top,
                                    'width': $textOverlay.width(),
                                    'height': $textOverlay.height(),
                                    'pageX': event.pageX,
                                    'pageY': event.pageY
                                };

                                var sizeBoxWidth = $sizeBox.width();
                                var sizeBoxHeight = $sizeBox.height();

                                event.dragImmediately = true;

                                $.drag(this, event, function(event) {

                                }, function(event) {
                                    var deltaX = event.pageX - original.pageX;
                                    var deltaY = event.pageY - original.pageY;
                                    var bounds = callback(event, original, {
                                        'x': deltaX,
                                        'y': deltaY
                                    });

                                    // Fill out the missing bounds.
                                    for (key in original) {
                                        bounds[key] = bounds[key] || original[key];
                                    }

                                    bounds.left = ((bounds.left / sizeBoxWidth) * 100) + '%';
                                    bounds.top = ((bounds.top / sizeBoxHeight) * 100) + '%';
                                    bounds.width = ((bounds.width / sizeBoxWidth) * 100) + '%';
                                    bounds.height = 'auto';

                                    $textOverlay.css(bounds);

                                // Set the hidden inputs to the current bounds.
                                }, function() {
                                    var texts = '',
                                            xs = '',
                                            ys = '',
                                            widths = '';

                                    $sizeBox.find('.imageEditor-textOverlay').each(function() {
                                        var $to = $(this),
                                                textOverlayPosition = $to.position(),
                                                textOverlayWidth = $to.width(),
                                                textOverlayHeight = $to.height();

                                        texts += DELIMITER + ($to.find('.imageEditor-textOverlayInput').val());
                                        xs += DELIMITER + (textOverlayPosition.left / sizeBoxWidth);
                                        ys += DELIMITER + (textOverlayPosition.top / sizeBoxHeight);
                                        widths += DELIMITER + (textOverlayWidth / sizeBoxWidth);
                                    });

                                    $input.texts.val(texts);
                                    $input.textXs.val(xs);
                                    $input.textYs.val(ys);
                                    $input.textWidths.val(widths);
                                });

                                return false;
                            };
                        };

                        $textOverlayLabel.mousedown(updateTextOverlay(function(event, original, delta) {
                            return {
                                'moving': true,
                                'left': original.left + delta.x,
                                'top': original.top + delta.y
                            };
                        }));

                        $textOverlay.append($textOverlayLabel);

                        var $textOverlayRemove = $('<div/>', {
                            'class': 'imageEditor-textOverlayRemove',
                            'text': 'Remove',
                            'click': function() {
                                $textOverlay.remove();
                                return false;
                            }
                        });

                        $textOverlay.append($textOverlayRemove);

                        $textOverlay.append($('<div/>', {
                            'class': 'imageEditor-resizer imageEditor-resizer-left',
                            'mousedown': updateTextOverlay(function(event, original, delta) {
                                return {
                                    'left': original.left + delta.x,
                                    'width': original.width - delta.x
                                };
                            })
                        }));
                        $textOverlay.append($('<div/>', {
                            'class': 'imageEditor-resizer imageEditor-resizer-right',
                            'mousedown': updateTextOverlay(function(event, original, delta) {
                                return {
                                    'left': original.left,
                                    'width': original.width + delta.x
                                };
                            })
                        }));

                        var $textOverlayInput = $('<input/>', {
                            'class': 'imageEditor-textOverlayInput',
                            'type': 'text',
                            'value': $input.texts.val().split(DELIMITER)[textOverlayIndex] || ''
                        });

                        $textOverlay.append($textOverlayInput);
                        $textOverlayInput.rte({
                            'initImmediately': true,
                            'useLineBreaks': true
                        });

                        $editor.before($textOverlay.find('.rte-toolbar-container'));

                        if (!init) {
                            $textOverlayInput.focus();
                        }

                        var wait = 5000;
                        var repeatResizeTextOverlayFont = function() {
                            var $body = $($textOverlay.find('.rte-container iframe')[0].contentDocument.body);
                            if ($body.is('.rte-loaded')) {
                                resizeTextOverlayFont();
                            } else {
                                wait -= 100;
                                if (wait > 0) {
                                    setTimeout(repeatResizeTextOverlayFont, 100);
                                }
                            }
                        };

                        repeatResizeTextOverlayFont();

                        $form.submit(function() {
                            var texts = '';

                            $sizeBox.find('.imageEditor-textOverlay').each(function() {
                                texts += DELIMITER + $(this).find('.imageEditor-textOverlayInput').val();
                            });

                            $input.texts.val(texts);
                        });

                        if (!init) {
                            var sizeBoxWidth = $sizeBox.width();
                            var sizeBoxHeight = $sizeBox.height();
                            var texts = '',
                                    xs = '',
                                    ys = '',
                                    widths = '';

                            $sizeBox.find('.imageEditor-textOverlay').each(function() {
                                var $to = $(this),
                                        textOverlayPosition = $to.position(),
                                        textOverlayWidth = $to.width(),
                                        textOverlayHeight = $to.height();

                                texts += DELIMITER + ($to.find('.imageEditor-textOverlayInput').val());
                                xs += DELIMITER + (textOverlayPosition.left / sizeBoxWidth);
                                ys += DELIMITER + (textOverlayPosition.top / sizeBoxHeight);
                                widths += DELIMITER + (textOverlayWidth / sizeBoxWidth);
                            });

                            $input.texts.val(texts);
                            $input.textXs.val(xs);
                            $input.textYs.val(ys);
                            $input.textWidths.val(widths);
                        }

                        return false;
                    }
                }));

                $.each($input.texts.val().split(DELIMITER), function(index, text) {
                    if (index > 0) {
                        $sizeButton.find('.imageEditor-addTextOverlay').trigger('click', true);
                    }
                });
            });

            $sizes.before($sizeSelectors);
            $sizes.hide();

            var $buttons = $sizeSelectors.find('.imageEditor-sizeButton');
            if ($buttons.length == 1) {
                $buttons.click();
            }
        }
    });
});
