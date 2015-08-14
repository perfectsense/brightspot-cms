define([
    'jquery',
    'bsp-utils' ],

function($, bsp_utils) {

    var carouselUtility;

    // Singleton object for the carousel.
    //
    // Do not directly use this object - instead create your own instance using Object.create():
    //   mycarousel = Object.create(carouselUtility);
    //   mycarousel.init(element, settings);
    //
    // To control the carousel, save the object instance. Or you could save it in the data
    // of the element so other code can get to it and control the carousel:
    //   $(element).data('carousel', mycarousel);
    //
    // Then other code can always get the carousel and control it:
    //   var mycarousel = $(element).data('carousel');
    //   if (mycarousel) {
    //     mycarousel.nextPage();
    //   }
    //
    // The object has functions for you to control the carousel:
    // 
    // init() Call this after you create the object to initialize the carousel
    // addTile() Add a tile to the carousel
    // update() After adding multiple tiles call the update function to ensure the carousel updates the display
    // 
    // The carousel also triggers events - listen for these events to perform actions such as loading more tiles
    // when the user reaches the end of the carousel.
    //
    // carousel.end = The user has reached the end of the carousel tiles
    // carousel.tile = The user clicked one of the tiles
    // 
    // @example
    // 
    // var mydiv = $('#mycarousel');
    // var mycarousel = Object.create(carouselUtility);
    //
    // mycarousel.init(mydiv);
    // mycarousel.addTile('<div>My tile 1</div>');
    // mycarousel.addTile('<div>My tile 2</div>');
    // mycarousel.addTile('<div>My tile 3</div>');
    // mycarousel.update();
    //
    // // Do something when user clicks a tile
    // mydiv.on('carousel.tile', function(event, data) {
    //   // data.carousel = the carousel object
    //   // data.index = which tile was clicked
    //   // data.tile = the tile content
    //   alert('User clicked tile number ' + data.index);
    // });
    //
    // // Ajax more content when reach end of carousel
    // mydiv.on('carousel.end', function(event, data) {
    //   // data.carousel = the carousel object
    //   $.get('/moredata.jsp', function(ajaxData){
    //     data.carousel.addTile(ajaxData);
    //   });
    // });

    carouselUtility = {

        /**
         * Default settings for the carousel.
         */
        defaults: {

            // @param {Boolean} numbered
            // true = overlay numbers on top of the tiles
            // false = do not overlay numbers on top of the tiles
            numbered: false

        },


        /**
         * Various class names
         */
        classTileActive: 'carousel-tile-active',
        classNavHide: 'carousel-nav-hide',
        classNumbered: 'carousel-numbered',

        /**
         * Names for custom events
         *
         * When these events are triggered, an object is sent so the event listener
         * will have access to the following:
         *
         * carousel = the carousel object
         * n = the number of the tile that was clicked
         * tile = the dom element for the tile
         */

        // Event when a tile is clicked
        eventTile: 'carousel.tile',

        // Event when reach the end of a carousel
        // For example, you can listen for this event then dynamically add more tiles
        eventEnd: 'carousel.end',
        eventBegin: 'carousel.begin',

        // Events for when user clicks next or previous buttons
        // Note these events will be called even if the carousel does not move
        // due to already being at the beginning or end of the tiles.
        eventNext: 'carousel.next',
        eventPrevious: 'carousel.previous',
        
        /**
         * HTML template for the carousel
         */
        template:
          '<div class="carousel-wrapper">' +
            '<div class="carousel-viewport">' +
              '<ol class="carousel-tiles"></ol>' +
            '</div>' +
            '<div class="carousel-nav carousel-nav-previous"><a href="#">Previous</a></div>' +
            '<div class="carousel-nav carousel-nav-next"><a href="#">Next</a></div>' +
          '</div>',


        /**
         * Initialize the object. Call this once.
         *
         * @param {Element} element
         * The element where the carousel should be created.
         *
         * @param {Object} [settings]
         * A set of key/value pairs to override the default settings.
         *
         */
        init: function(element, settings) {

            var self = this;

            // Save the main element so we can use it later
            self.element = $(element);

            // Use the default settings, overridden by whatever settings were passed in.
            self.settings = $.extend(true, {}, self.defaults, settings);

            self._initHTML();
        },


        /**
         * @private
         * Create the HTML for the carousel.
         */
        _initHTML: function() {
            var self = this;

            // A place to cache all our dom elements so we don't have to query the dom repeatedly
            self.dom = {};

            // Create the HTML for the carousel
            self.dom.carousel = $(self.template).appendTo(self.element);

            // Add the numbered class if user requested a numberd carousel
            if (self.settings.numbered) {
                self.dom.carousel.addClass(self.classNumbered);
            }

            // Save pointers to the elements we will use later
            self.dom.previous = self.dom.carousel.find('.carousel-nav-previous');
            self.dom.next = self.dom.carousel.find('.carousel-nav-next');
            self.dom.tiles = self.dom.carousel.find('.carousel-tiles');
            self.dom.viewport = self.dom.carousel.find('.carousel-viewport');

            self._initEvents();
        },


        /**
         * @private
         * Create the event handlers for the carousel and bind to the HTML.
         */
        _initEvents: function() {

            var self = this;

            // Previous button
            self.dom.previous.on('click', function(e) {
                
                self.element.trigger(self.eventPrevious, {
                    carousel: self
                });
                
                self.previousPage();
                return false;
            });

            // Next button
            self.dom.next.on('click', function(e) {
                
                self.element.trigger(self.eventNext, {
                    carousel: self
                });

                self.nextPage();
                return false;
            });

            // Clicking on a tile
            self.dom.tiles.on('click', '> .carousel-tile', function(e) {

                // Determine which tile was clicked
                // The index() function returns the index of the <li> element relative to its siblings.
                // Since it is zero-based we add one to make it the tile number.
                var n = $(this).index() + 1;
                
                // Make it the active tile
                self.setActive(n);

                // If the tile is not currently visible move it to the center
                if (!self.tileIsVisible(n)) {
                    self.goToTile(n, true);
                }
            
                // Do the action for the tile
                self.doTile(n);
            });
        },


        /**
         * Add a new tile to the end of the carousel.
         * @param {String|jQueryObject} content
         * HTML content (or a jQuery object) to add to the carousel.
         * Note this will be enclosed in an <LI> element.
         */
        addTile: function(content) {

            var self = this;

            var tile = $('<li/>', {'class': 'carousel-tile'}).append(content).appendTo(self.dom.tiles);
        },

        /**
         * Prepends new tile to beginning of carousel
         * @param {String|jQueryObject} content
         * HTML content (or a jQuery object) to prepend to the carousel.
         * Note this will be enclosed in an <LI> element.
         */
        prependTile: function(content) {
            
            var self = this;
            var tile;

            // First get some information about where we are currently scrolled
            var layout = self._getLayout();

            // Adjust the viewport position to account for a new tile at the beginning
            var offset = layout.tilesOffset + layout.tileWidth;
            
            // Turn off transitions for the offset temporarily.
            // When carousel.update() is called again transitions will turn on again.
            // This is necessary because adding a tile to the beginning of the carousel
            // would push all the other tiles to the right, but we will shift everything
            // back to the left so the carousel remains at the same spot.
            // While doing this we don't want any animation occurring.
            self.dom.tiles.addClass('no-transition');
            
            self._setOffset(offset);

            // Add new tile to the beginning
            tile = $('<li/>', {'class': 'carousel-tile'}).append(content).prependTo(self.dom.tiles);
            
        },


        /**
         * Remove a tile from the carousel by specifying its index.
         *
         * @param {Number} n
         * Number of the tile to remove.
         */
        removeTile: function(n) {

            var self = this;

            var tile = self.dom.tiles.find('> .carousel-tile').eq(n - 1).remove();
        },


        /**
         * Move a tile within the carousel.
         *
         * @param {Element|jQuery object} element
         * The tile element. This can be an element within the tile content, or the LI element of the tile itself.
         *
         * @param {Number} newPosition
         * The new index for the tile (starting with 1).
         */
        repositionTile: function(currentPosition, newPosition) {
            
            var self = this;
            var $tiles = self.dom.tiles.find('> .carousel-tile');
            var $tile = $tiles.eq(currentPosition - 1);

            // Make sure newPosition is valid
            if (newPosition > $tiles.length ) {
                return;
            }
            
            // Make sure tile at current position exists
            if (!$tile.length) {
                return;
            }

            // Remove the tile temporarily
            $tile = $tile.detach();

            // Add the tile in the new position
            if (newPosition === 1) {
                // Special case add to the front of the carousel
                self.dom.tiles.prepend($tile);
            } else {
                self.dom.tiles.find('> .carousel-tile').eq(newPosition - 2).after($tile);
            }

            self.update();
        },

        
        /**
         * Return the content of a tile by specifying the tile index.
         *
         * @param {Number} n
         * Number of the tile to retrieve (starting at 1 for the first tile).
         *
         * @returns {jQuery Object}
         *
         * @example
         * mycarousel.getTileContent(1).addClass('toBeRemoved');
         */
        getTileContent: function(n) {
            
            var self = this;

            return self.dom.tiles.find('> .carousel-tile').eq(n - 1).contents();
        },


        /**
         * Given the tile content, returns the index of the tile.
         *
         * @param {Element|jQuery object} element
         * The tile content (an element within the carousel tile).
         *
         * @returns {Number} index
         * The tile number within the carousel (starting with 1).
         */
        getTileIndex: function(element) {
            var self = this;
            var $element = $(element);

            return $element.closest('.carousel-tile').index() + 1;
        },

        
        /**
         * Set or clear the "error" class for a tile.
         *
         * @param {Number} n
         * Number of the tile to retrieve (starting at 1 for the first tile).
         *
         * @param {Boolean} [state]
         * True to set the error state, false to clear error state,
         * or undefined to toggle the error state.
         */
        toggleTileError: function(n, state) {
            var self = this;
            var $tile = self.dom.tiles.find('> .carousel-tile').eq(n - 1);
            $tile.toggleClass('carousel-tile-error', state);
        },

        
        /**
         * @private
         * Move the carousel forward or back.
         *
         * @param {Boolean|Number} direction
         * To move one "page" forward, set to true.
         * To move one "page" backward, set to false.
         * To move forward tiles set to a positive number.
         * To move backward tiles set to a negative number.
         */
        _move: function(amount) {

            var self = this;

            // First get some information about where we are currently scrolled
            var layout = self._getLayout();

            // Sometimes the tilesOffset value can get out of whack
            // (for example if user clicks nav links in quick succession)
            // So re-baseline on a tile border if necessary
            layout.tilesOffset -= (layout.tilesOffset % layout.tileWidth);

            var newOffset = 0;

            // Number of tiles to shift
            var moveTiles = 0;

            if ($.isNumeric(amount)) {
                moveTiles = amount;
            } else {

                // Determine if we are moving back or forward

                if (amount === false) {
                    // Move back one page
                    moveTiles = -layout.tilesPerViewport;
                } else {
                    // Move forward one page
                    moveTiles = layout.tilesPerViewport;
                }
            }

            // Calculate the new margin-left value
            newOffset = layout.tilesOffset + (moveTiles * layout.tileWidth);

            // Don't move too far
            if (newOffset < 0) {
                newOffset = 0;
            }

            // Don't move too far
            if (newOffset > layout.tilesWidth) {
                newOffset = layout.tilesOffset;
            }

            self._moveTo(newOffset);
        },


        /**
         * Move the carousel to a specific pixel position
         *
         * @param {Number} offset
         * The pixel position to move the carousel (should be a positive number).
         */
        _moveTo: function(offset) {
            
            var self = this;

            // Set the left offset to move the tiles to the left within the viewport
            self._setOffset(offset);

            // Update the nav links etc
            setTimeout(function(){
                self.update();
            }, 1100);
        },

        
        /**
         * Set the left offset for the content within the viewport
         * (without doing anything else).
         */
        _setOffset: function(offset) {
            
            var self = this;
            
            // Set the left offset to move the tiles to the left within the viewport
            self.dom.tiles.css('margin-left', '-' + offset + 'px');
        },

        
        /**
         * Go to the next page of tiles.
         */
        nextPage: function() {
            var self = this;
            self._move(true);
        },


        /**
         * Go to the previous page of tiles.
         */
        previousPage: function() {
            var self = this;
            self._move(false);
        },


        /**
         * Advance to the next tile (move only one tile).
         */
        nextTile: function() {
            var self = this;
            self._move(+1);
        },


        /**
         * Go to the previous tile (move only one tile).
         */
        previousTile: function() {
            var self = this;
            self._move(-1);
        },

        
        /**
         * Go to a specific tile and optionally center it in the carousel viewport.
         *
         * @param {Number} index
         * Tile number (starting with 1).
         *
         * @param {Boolean} [center]
         * Set to true if you want the tile centered in the carousel viewport.
         */
        goToTile: function(n, center) {
            
            var self = this;
            
            // First get some information about our tiles
            var layout = self._getLayout();

            // Now figure out the offset to get to this tile
            var offset = (n-1) * layout.tileWidth;

            // Adjust to center the tile in the viewport?
            if (center) {
                offset = offset - (layout.viewportWidth / 2) + (layout.tileWidth / 2);
                if (offset < 0) {
                    offset = 0;
                }
            }
            
            // Move to the offset
            self._moveTo(offset);
        },

        
        /**
         * Go to the currently active tile.
         */
        goToActiveTile: function() {
            var self = this;
            var n = self.getActive();
            self.goToTile(n, true);
        },


        /**
         * Determine if tile is completely visible within the viewport,
         * or if it is hidden (even partially).
         *
         * @param {Number} n
         * Tile number to check.
         *
         * @returns {Boolean}
         * Returns true if the tile is completely visible within the carousel viewport.
         * Returns false if the tile is hidden or partially cut off.
         */
        tileIsVisible: function(n) {
            
            var self = this;
            
            // First get some information about our tiles
            var layout = self._getLayout();
            
            // Now figure out the offset to get to the tile we want
            var tileOffset = (n-1) * layout.tileWidth;

            // Determine the distance from the current offset to the tile we want
            var offsetDiff = tileOffset - layout.tilesOffset;

            var isVisible = Boolean((offsetDiff > 0) && (offsetDiff + layout.tileWidth < layout.viewportWidth));
            
            return isVisible;
        },

        
        /**
         * Set a tile to the active state.
         *
         * @param Number n
         * The tile number (1-n) to set active.
         */
        setActive: function(n) {

            var self = this;

            self.clearActive();
            self.dom.tiles.find('> .carousel-tile').eq(n - 1).addClass(self.classTileActive);
        },


        /**
         * Return the tile number that is currenly active.
         *
         * @returns Number
         * Index of the active tile (1-n) or 0 if there is no active tile.
         */
        getActive: function() {

            var self = this;
            var index;

            // return tile number of the active tile
            index = self.dom.tiles.find('> .carousel-tile').filter(function(){
                return $(this).hasClass(self.classTileActive);
            }).index() + 1;

            return index;
        },


        /**
         * Clear the active state from all tiles.
         */
        clearActive: function() {

            var self = this;

            self.dom.tiles.find('> .carousel-tile').removeClass(self.classTileActive);
        },


        /**
         * Update various parts of the carousel ui after a change has been made.
         * This should be called after a move() or after a tile is added or removed.
         * Note the carousel does not call this after a tile is added or removed for performance reasons.
         * So after you add or remove one or more tiles you should call it directly.
         */
        update: function(layout) {

            var self = this;

            var layout = self._getLayout();

            // Enable transitions, in case they were disabled by the prependTile function.
            self.dom.tiles.removeClass('no-transition');

            // Determine if we should show the next/previous buttons
            // Note the actual showing/hiding is done via CSS rules
            self.dom.previous.toggleClass(self.classNavHide, layout.atMin);
            self.dom.next.toggleClass(self.classNavHide, layout.atMax);

            // Trigger an event if we reach the end of the carousel
            // This can be used to fetch more data and add more tiles to the carousel
            if (layout.atMax) {

                self.element.trigger(self.eventEnd, {
                    carousel: self
                });

            } else if (layout.atMin) {

                self.element.trigger(self.eventBegin, {
                    carousel: self
                });
            }
        },


        /**
         * @private
         * Get various layout numbers to calculate positions.
         */
        _getLayout: function() {

            var self = this;
            var layout;

            layout = {

                viewportWidth: self.dom.viewport.width(),

                // Get the total width of all tiles
                tilesWidth: self.dom.tiles.width(),

                // Get width of first tile including margin
                // (note we assume all tiles will have same width)
                tileWidth: self.dom.tiles.find('> .carousel-tile:first-child').outerWidth(true),

                // Get the current left offset of the tiles within the viewport
                tilesOffset: Math.abs(parseInt(self.dom.tiles.css('margin-left'), 10)) || 0

            };

            // Calculate how many tiles fit within the viewport
            layout.tilesPerViewport = Math.floor( layout.viewportWidth / layout.tileWidth );

            // Determine if we are already at the maximum / minimum range of movement
            layout.atMax = Boolean(layout.tilesWidth - layout.tilesOffset <= layout.viewportWidth);
            layout.atMin = Boolean(layout.tilesOffset <= 0);

            return layout;
        },


        /**
         * Perform the action for the user clicking a tile.
         * Note this function does not mark the tile "active".
         *
         * @param {Number} n
         * Index of the tile that was clicked (starting at 1).
         */
        doTile: function(n) {

            var self = this;

            // Get the content for tile n,
            // so we can pass it when we trigger a custom event.
            // That way someone using the carousel can attach data to the tiles
            // and receive it back when the event occurs, so they will
            // know which tile was clicked
            var content = self.dom.tiles.find('> .carousel-tile').eq(n - 1).contents();

            // Data to pass as part of the custom event
            var data = {
                carousel: self,
                index: n,
                tile: content
            };

            self.element.trigger(self.eventTile, data);
        }
    };

    return carouselUtility;
});
