define([ 'jquery', 'bsp-utils' ], function($, bsp_utils) {
  var $window = $(window);

  function isFixed($popup) {
    return $popup.css('position') === 'fixed';
  }

  function resizeScrollable() {
    var $scrollable = $(this);
    var $popup = $scrollable.closest('.popup');

    if (!isFixed($popup)) {
      $scrollable.removeClass('fixedScrollableArea');
      return;
    }

    var oldScrollTop = $scrollable.scrollTop();

    $scrollable.addClass('fixedScrollableArea');
    $scrollable.css('max-height', '');

    var scrollableTop = $scrollable.offset().top;
    var bottomPadding =
        $popup.outerHeight(true) -
        $scrollable.outerHeight(true) -
        scrollableTop +
        $popup.offset().top;

    $scrollable.css('max-height',
        $window.height() -
        scrollableTop +
        $window.scrollTop() -
        bottomPadding -
        20);

    $scrollable.scrollTop(oldScrollTop);
  }

  return bsp_utils.plugin(window, 'bsp', 'fixedScrollable', {
    '_init': function(roots, selector) {
      var $roots = $(roots);

      $(document).ready(function() {
        var updateMaxScrollTop = $.throttle(100, function($scrollable) {
          $.data($scrollable[0], 'fixedScrollable-maxScrollTop', $scrollable.prop('scrollHeight') - $scrollable.innerHeight());
        });

        $roots.on('mousewheel', selector, function(event, delta, deltaX, deltaY) {
          var $scrollable = $(this);

          if (!isFixed($scrollable.closest('.popup'))) {
            return;
          }

          updateMaxScrollTop($scrollable);

          if ((deltaY > 0 && $scrollable.scrollTop() === 0) ||
              (deltaY < 0 && $scrollable.scrollTop() >= $.data($scrollable[0], 'fixedScrollable-maxScrollTop'))) {
            event.preventDefault();
          }
        });
      });

      $window.resize($.throttle(100, function() {
        $roots.find(selector).each(resizeScrollable);
      }));
    },

    '_each': function(scrollable) {
      resizeScrollable.call($(scrollable));
    }
  });
});
