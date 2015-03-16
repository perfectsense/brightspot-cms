define([ 'jquery', 'bsp-utils' ], function($, bsp_utils) {
  var SELECTOR = '.withLeftNav > .leftNav, .withLeftNav > .main, .contentForm-main, .contentForm-aside';
  var VISIBLE_DATA_KEY = 'cs-visible';
  var TOP_ORIGINAL_DATA_KEY = 'cs-topOriginal';
  var HEIGHT_DATA_KEY = 'cs-height';

  // Set up all the elements so that they can be moved.
  bsp_utils.onDomInsert(document, SELECTOR, {
    'insert': function(element) {
      $(element).css({
        'margin-bottom': 0,
        'position': 'relative',
        'top': 0
      });
    }
  });

  // Update the current state of the elements every so often. This is
  // throttled heavily so that the actual scroll events can run fast
  // using the cached information.
  var bodyHeight = 0;
  var toolHeaderHeight = 0;
  var $window = $(window);

  $window.scroll(bsp_utils.throttle(1000, function() {
    bodyHeight = $(document.body).height();
    toolHeaderHeight = $('.toolHeader').outerHeight(true);

    $(SELECTOR).each(function() {
      var element = this;
      var $element = $(element);

      $.data(element, VISIBLE_DATA_KEY, $element.is(':visible'));
      $.data(element, TOP_ORIGINAL_DATA_KEY, $element.offset().top - (parseInt($element.css('top'), 10) || 0));
      $.data(element, HEIGHT_DATA_KEY, $element.outerHeight());
    });
  }));

  // Move the elements on scroll.
  var lastWindowScrollTop = $window.scrollTop();

  $window.scroll(function() {
    var windowScrollTop = $window.scrollTop();
    var windowHeight = $window.height();

    $(SELECTOR).each(function() {
      var element = this;
      var $element = $(element);

      // Skip the move if not visible.
      if (!$.data(element, VISIBLE_DATA_KEY) ||
          $element.closest('.popup-objectId-edit-loaded').length > 0) {
        return;
      }

      var elementTopOriginal = $.data(element, TOP_ORIGINAL_DATA_KEY);
      var elementHeight = $.data(element, HEIGHT_DATA_KEY);
      var elementTop = $element.offset().top;

      function moveToTop(offset) {
        var top = Math.max(windowScrollTop - (offset || 0), 0);

        $element.css({
          'margin-bottom': top,
          'top': top
        });
      }

      // The element height is less than the window height,
      // so there's no need to account for being fixed at the bottom.
      if (elementTopOriginal + elementHeight < windowHeight) {
        moveToTop();

      // The user is scrolling down.
      } else if (lastWindowScrollTop < windowScrollTop) {
        var windowBottom = windowScrollTop + windowHeight;

        // The bottom of the element is about to be hidden, so move it up.
        if (windowBottom > elementTop + elementHeight) {
          var top = Math.max(Math.min(windowBottom - elementHeight - elementTopOriginal, bodyHeight - elementHeight - elementTopOriginal), 0);

          $element.css({
            'margin-bottom': top,
            'top': top
          });
        }

      // The user is scrolling up, and the top of the element is about to
      // be hidden, so move it down.
      } else if (lastWindowScrollTop > windowScrollTop &&
          elementTop > windowScrollTop + toolHeaderHeight) {

        moveToTop(elementTopOriginal - toolHeaderHeight);
      }
    });

    lastWindowScrollTop = windowScrollTop;
  });
});
