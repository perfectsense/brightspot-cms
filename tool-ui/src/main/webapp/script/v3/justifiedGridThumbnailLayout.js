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

                // When more images are loaded (lazyload, infinitescroll), remove the extra previewContainer that's added and tell flexImages to redo layout
                $grid.data('onLoadCallback', function() {
                    $grid.find('.previewContainer:not(:first)').remove();
                    $grid.flexImages(options);
                });

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
                        //console.log("clicked preview");
                        togglePreviewFor($figure);
                    });
                });

                // Setup the next/previous buttons on the preview container
                var $previewContainer = $grid.find('.previewContainer');
                $previewContainer.find('.left-scroll').click({
                    action: 'previous',
                    previewContainer: $previewContainer,
                    grid: $grid
                }, displayNextPreviousItem);
                $previewContainer.find('.right-scroll').click({
                    action: 'next',
                    previewContainer: $previewContainer,
                    grid: $grid
                }, displayNextPreviousItem);

                // setup a resize handler
                $(window).resize(function(eventObject) {
                    // Set the size of the preview container
                    // console.log("Handling rezize");
                    //$previewContainer.css('width', $grid.innerWidth);
                    console.log("Setting previewContainer height to " + ($grid.innerWidth() / 2), $previewContainer);
                    $previewContainer.height($grid.innerWidth() / 2);
                    return true;
                })
            });
        }
    });

    // Toggle the actual preview area
    // If it's already displayed and showing the current image's preview, then hide it
    // If it's already displayed and showing a different image's preview, switch it to the current image
    // If it's not displayed, display it and show the current image's preview
    var togglePreviewFor = function($gridItemContainer) {
        var uuidToDisplay = $gridItemContainer.attr('data-uuid');
        if (uuidToDisplay === undefined || uuidToDisplay === null) { // TODO: is this how we check for undefined?
            console.log("Cannot load previewContainer, no uuid");
            return;
        }

        var $previewContainer = $gridItemContainer.parents('.searchResult-images').find('.previewContainer');
        var $displayedItem = $previewContainer.data('displayedItem');
        //console.log('displayed item', $displayedItem);
        var uuidCurrentlyDisplayed = $displayedItem === undefined || $displayedItem === null || $displayedItem.length === 0
            ? null
            : $displayedItem.attr('data-uuid');

        //console.log("[uuid=" + uuidToDisplay + "] [currentUuid=" + uuidCurrentlyDisplayed + "] [displayed=" + $previewContainer.data('displayed') + ']');
        if (uuidToDisplay === uuidCurrentlyDisplayed && $previewContainer.data('displayed') === true) {
            // If it's already displaying the preview and is visible, we hide it
            hidePreviewContainer($previewContainer);
            return;
        }

        if (uuidToDisplay !== uuidCurrentlyDisplayed) {
            loadPreview($previewContainer, $gridItemContainer);
            // Position the preview container under the container
            $previewContainer.insertAfter(findLastContainerInRow($gridItemContainer));
            // Scroll the frame to make sure the preview is visible
            // TODO: ^ Scroll the frame to make sure the preview is visible
            setupNextPreviousButtons($previewContainer, $gridItemContainer);
        }

        if ($previewContainer.data('displayed') !== true) {
            displayPreviewContainer($previewContainer);
        }

    }

    // Load the preview content into the preview container
    var loadPreview = function($previewContainer, $gridItemContainer) {
        var uuidToDisplay = $gridItemContainer.attr('data-uuid');

        // TODO: should the url generation be handled better?
        $previewContainer.find('.content').load("/cms/gridPreview?uuid=" + uuidToDisplay, function( response, status, xhr ) {
            if ( status == "error" ) {
                var msg = "Sorry but there was an error: ";
                $( "#error" ).html( msg + xhr.status + " " + xhr.statusText );
            } else {
                $previewContainer.data('displayedItem', $gridItemContainer);
            }
        });
    }

    // find the last container on the same row as the grid item we're previewing
    var findLastContainerInRow = function($gridItemContainer) {
        var $currentGridItemContainer = $gridItemContainer;
        while ($currentGridItemContainer.next('figure').length !== 0) {
            var $nextSibling = $currentGridItemContainer.next('figure');
            if ($nextSibling.position().top !== $currentGridItemContainer.position().top) {
                return $currentGridItemContainer;
            }
            $currentGridItemContainer = $nextSibling;
        }
        return $currentGridItemContainer;
    }

    // Displays (makes visible) the preview container, if it's not already
    var displayPreviewContainer = function($previewContainer) {
        //console.log("displaying preview container");
        if ($previewContainer.data('displayed') === true) {
            return;
        }
        $previewContainer.css('display', 'flex');
        $previewContainer.data('displayed', true);
    }

    var hidePreviewContainer = function($previewContainer) {
        //console.log("hiding preview container");
        $previewContainer.css('display', 'none');
        $previewContainer.data('displayed', false);
    }

    /*
     * TODO: 1. Have each of these ( setupNextPreviousButtons , displayNextPreviousItem ) use the same method to get the next/prev
     * TODO: 2. Use next/prev and iterate (until we find a figure or the end) instead of nextAll/prevAll (which will scale poorly as more assets are added)
     */
    var setupNextPreviousButtons = function($previewContainer, $gridItemContainer) {
        // TODO: make the hover not do anything, or look disabled
        var $previous = $gridItemContainer.prevAll('figure');
        if ($previous.length > 0) {
            // There is a previous grid item, the previous button should send us to it
            $previewContainer.find('.left-scroll').css('cursor', 'pointer');
        } else {
            // There is no previous grid item, the previous button should do nothing
            $previewContainer.find('.left-scroll').css('cursor', 'auto');
        }

        var $next = $gridItemContainer.nextAll('figure');
        if ($next.length > 0) {
            // There is a previous grid item, the previous button should send us to it
            $previewContainer.find('.right-scroll').css('cursor', 'pointer');
        } else {
            // There is no previous grid item, the previous button should do nothing
            $previewContainer.find('.right-scroll').css('cursor', 'auto');
        }

    }

    var displayNextPreviousItem = function(eventObject) {
        var $previewContainer = $(eventObject.data.previewContainer);
        var action = eventObject.data.action;
        var currentlySelectedItem =  $previewContainer.data('displayedItem');

        if (currentlySelectedItem === undefined || currentlySelectedItem === null || currentlySelectedItem.length === 0) {
            // nothing is currently displayed, we can't do anything
            return false;
        }

        var $target = action === 'previous' ? $(currentlySelectedItem).prevAll('figure')
            : action === 'next' ? $(currentlySelectedItem).nextAll('figure')
                : null;

        if ($target.length > 0) {
            // There is a previous/next grid item, the button should send us to it
            togglePreviewFor($($target[0]));
        }
        return false;
    }
});

/*
 * Example code for a (very) simple custom renderer for the BEX Image class

    public class ImageGridPreviewRenderer extends GridPreviewRenderer<Image> {
        public ImageGridPreviewRenderer(Image asset) {
            super(asset);
        }

        @Override
        public void renderPreviewMetaData(ToolPageContext page) throws IOException, ServletException {
            page.writeStart("div", "class", "field", "data-field", "title");
                page.writeHtml(asset.getLabel());
            page.writeEnd();
            page.writeStart("div", "class", "field", "data-field", "orientation");
                page.writeStart("span", "class", "label");
                    page.writeHtml("Orientation");
                page.writeEnd();
                page.writeStart("span", "class", "value");
                    page.writeHtml(asset.getWidth() > asset.getHeight() ? "Horizontal"
                        : asset.getWidth() < asset.getHeight() ? "Vertical"
                        : "None");
                page.writeEnd();
            page.writeEnd();
            page.writeStart("div", "class", "field", "data-field", "dimensions");
                page.writeStart("span", "class", "label");
                    page.writeHtml("Dimensions");
                page.writeEnd();
                page.writeStart("span", "class", "value");
                    page.writeHtml(asset.getWidth() + "x" + asset.getHeight());
                page.writeEnd();
            page.writeEnd();
        }

    }

 */