define([ 'jquery', 'bsp-utils', 'v3/input/carousel' ], function($, bsp_utils, carouselUtility) {

  var settings  = {
    containerSelector         : '.widget-searchCarousel',
    nextAttr                  : 'next-page',
    prevAttr                  : 'prev-page',
    itemsSelector             : 'a'
  };

  bsp_utils.onDomInsert(document, settings.containerSelector, {

    insert : function (container) {

      var carousel;

      var $container = $(container);
      carousel = Object.create(carouselUtility);
      carousel.init($container, {numbered:false});

      addTiles($container.children(settings.itemsSelector), false);
      carousel.update();

      // In case the active tile is not within the window we should center it
      // (note this might trigger even more tiles to load)
      carousel.goToActiveTile();
        
      // Add more tiles via ajax when carousel ends
      $container.on('carousel.end carousel.begin', function(e, data) {

        var isEndEvent = e.namespace === 'end';
        var dataAttr = isEndEvent ? settings.nextAttr : settings.prevAttr;
        var url = $container.data(dataAttr);
        if (!url) { return; }
          
        $.ajax({
          'method' : 'get',
          'url'    : $container.data(dataAttr),
          'cache'  : false
        }).done(function(html) {

            var $searchCarousel = $(html);
            var url = $searchCarousel.data(dataAttr);

            // Update the data attribute with the URL to fetch the next set of results
            // Note we use jQuery.data() to set the data,
            // but we also change the data- attribute on the elment so it can be used by CSS.
            $container.data(dataAttr, url).attr('data-' + dataAttr, url);
            addTiles($searchCarousel.children(settings.itemsSelector), !isEndEvent);
            data.carousel.update();
        });

      });

      /**
       * Appends or prepends tiles to carousel
       *
       * @param {Array|jQueryObject} tiles
       * jQuery object array
       *
       * @param {Boolean} [isPrepend]
       * Optionally specifies to prepend
       */
      function addTiles(tiles, isPrepend) {

        var $tiles = $(tiles);

        if (isPrepend) {
          $tiles = $tiles.get().reverse();
        }

        // Adds tiles to carousel
        $.each($tiles, function(i, elem) {
          var $elem = $(elem);
          var index;
            
          if (isPrepend) {
            carousel.prependTile($elem);
          } else {
            carousel.addTile($elem);
          }

          // Sets active carousel item
          if ($elem.is('.widget-searchCarousel-item-selected')) {
              index = ($elem.closest('.carousel-tile').index() + 1) || 0;
              carousel.setActive(index);
          }
        });
      }
    }
  });

});
