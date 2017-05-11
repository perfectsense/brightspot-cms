/**
 * Created by rhseeger on 5/9/17.
 * Using:
 * - grid view: https://github.com/Pixabay/jQuery-flexImages (with changes)
 * - expanded preview: https://tympanus.net/codrops/2013/03/19/thumbnail-grid-with-expanding-preview/ (as guidance)
 */

define([ 'jquery', 'bsp-utils' ], function ($, bsp_utils) {

    // Should this be converted to a plugin like popup.js?
    var thumbnailGridDefaults = {
        gridSelector: '.searchResult-images',
        containerSelector: 'figure',
        maxRowHeight: 200
    };

    bsp_utils.onDomInsert(document, thumbnailGridDefaults.gridSelector, {
        insert: function() {
            $('.searchResult-images').each(function() {
                var $grid = $(this);
                var currentHeight = $grid.attr('data-size-height');
                var options = {
                    container: 'figure',
                    object: 'img',
                    rowHeight: (currentHeight === undefined ? 200 : currentHeight)
                    //maxRows: null,
                    //truncate: false
                };
                $grid.flexImages(options);
                $grid.data('onLoadCallback', function() { $grid.flexImages(options); });

                // Setup the preview toggle area to display on mouseover
                $grid.find("figure").each(function() {
                    var $figure = $(this);
                    var $previewToggleArea = $figure.find(".previewToggleArea");
                    var $caption = $figure.find('figcaption')
                    $figure.hover(
                        function(event) { // hover start
                            $previewToggleArea.css("display", "block");
                            // Set the bottom to the top of the label and the height to make it reach of to the 1/2 (from the bottom) point
                            var top = $figure.height() * 1/2,
                                height = $caption.position().top - top;

                            $previewToggleArea.css("top", top);
                            $previewToggleArea.css("height", height);
                            $previewToggleArea.css("font-size", height + "px");
                            $previewToggleArea.css("line-height", height + "px");
                            // If the caption changes size, we need to also.. this doesn't actually seem to do anything
                            // and it may actually cause many, many callbacks (since we'd register it on every hover)
                            // $caption.resize(function() {
                            //     console.log("caption was resized");
                            //     $previewToggleArea.css("top", top);
                            //     var height = $caption.position().top - top;
                            //     $previewToggleArea.css("height", height);
                            // });
                        },
                        function(event) { // hover end
                            $previewToggleArea.css("display", "none");
                            //console.log("focus in hiding", $figure, $previewToggleArea);
                        });
                    $previewToggleArea.click(function() {
                        console.log("clicked preview");
                        togglePreviewFor($figure);
                    });
                });

            });
        }
    });

    // Toggle the actual preview area
    // If it's already displayed and showing the current image's preview, then hide it
    // If it's already displayed and showing a different image's preview, switch it to the current image
    // If it's not displayed, display it and show the current image's preview
    var togglePreviewFor = function($gridItemContainer) {
        var uuid = $gridItemContainer.attr('data-uuid');
        if (uuid === undefined || uuid === null) { // TODO: is this how we check for undefined?
            console.log("Cannot load previewContainer, no uuid");
            return;
        }

        var $previewContainer = $gridItemContainer.parents('.searchResult-images').find('.previewContainer');
        var currentDisplayedUuid = $previewContainer.data('displayedUuid');

        console.log("[uuid=" + uuid + "] [currentUuid=" + currentDisplayedUuid + "] [displayed=" + $previewContainer.data('displayed') + ']');
        if (uuid === currentDisplayedUuid && $previewContainer.data('displayed') === true) {
            // If it's already displaying the preview and is visible, we hide it
            hidePreviewContainer($previewContainer);
            return;
        }

        if (uuid !== currentDisplayedUuid) {
            loadPreview($previewContainer, uuid);
            positionPreviewContainer($previewContainer, $gridItemContainer);
        }

        if ($previewContainer.data('displayed') !== true) {
            displayPreviewContainer($previewContainer);
        }

    }

    // Load the preview content into the preview container
    var loadPreview = function($previewContainer, uuid) {
        // do the load .. yadda yadda
        // for now we pretend
        // TODO: should the url generation be handled better?
        $previewContainer.find('.content').load("/cms/gridPreview?uuid=" + uuid, function( response, status, xhr ) {
            if ( status == "error" ) {
                var msg = "Sorry but there was an error: ";
                $( "#error" ).html( msg + xhr.status + " " + xhr.statusText );
            } else {
                $previewContainer.data('displayedUuid', uuid);
            }
        });
    }

    // Position the preview container under the container
    // We also resize it if necessary
    var positionPreviewContainer = function($previewContainer, $gridItemContainer) {
        // $grid = $previewContainer.parents('.searchResult-images');
        // $previewContainer.width($grid.innerWidth());
        // TODO
    }

    // Displays (makes visible) the preview container, if it's not already
    var displayPreviewContainer = function($previewContainer) {
        console.log("displaying preview container");
        if ($previewContainer.data('displayed') === true) {
            return;
        }
        $previewContainer.css('display', 'flex');
        $previewContainer.data('displayed', true);
    }

    var hidePreviewContainer = function($previewContainer) {
        console.log("hiding preview container");
        $previewContainer.css('display', 'none');
        $previewContainer.data('displayed', false);
    }

});

// We need to figure out how to call .flexImages on the grid whenever an ajax event loads more images (lazy load)