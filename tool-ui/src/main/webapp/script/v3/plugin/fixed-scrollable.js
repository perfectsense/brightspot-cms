define([ 'jquery', 'bsp-utils' ], function($, bsp_utils) {
  var $window = $(window);
  var $scrollables = $();

  function isFixed($popup) {
    return $popup.css('position') === 'fixed';
  }

  function resizeScrollables() {
    var fixedScrollables = [ ];

    // Filter out scrollables that aren't in a fixed popup.
    $scrollables.each(function() {
      var $scrollable = $(this);
      var $popup = $scrollable.closest('.popup, .widget[name="searchAdvancedFull"]');

      if (!isFixed($popup)) {
        $scrollable.removeClass('fixedScrollableArea');

      } else {
        fixedScrollables.push({
          '$scrollable': $scrollable,
          '$popup': $popup
        });
      }
    });

    // Prepare:
    // 1. Remember the current scroll position for restoring it later.
    // 2. Make the area scrollable.
    // 3. Set the height 0 for accurate bottom padding calculation.
    $.each(fixedScrollables, function(i, fs) {
      var $scrollable = fs.$scrollable;
      fs.oldScrollTop = $scrollable.scrollTop();

      $scrollable.addClass('fixedScrollableArea');
      $scrollable.css('max-height', 0);
    });

    $.each(fixedScrollables, function(i, fs) {
      var $scrollable = fs.$scrollable;
      var $popup = fs.$popup;

      $scrollable.css('max-height', '');

      fs.maxHeight =
          $window.scrollTop() +
          $window.height() -
          $popup.offset().top -
          $popup.outerHeight(true) +
          $scrollable.outerHeight(true) -
          20;

      $scrollable.css('max-height', 0);
    });

    $.each(fixedScrollables, function(i, fs) {
      var $scrollable = fs.$scrollable;

      $scrollable.css('max-height', fs.maxHeight);
      $scrollable.scrollTop(fs.oldScrollTop);
    });
  }

  var resizeRegistered;
  var updateMaxScrollTop = $.throttle(100, function($scrollable) {
    $.data($scrollable[0], 'fixedScrollable-maxScrollTop', $scrollable.prop('scrollHeight') - $scrollable.innerHeight());
  });

  return bsp_utils.plugin(window, 'bsp', 'fixedScrollable', {
    '_init': function(roots, selector) {

      if (!resizeRegistered) {
        resizeRegistered = true;

        $window.resize($.throttle(100, function () {
          resizeScrollables();
        }));
      }

      $(document).ready(function() {
        $(roots).on('mousewheel', selector, function(event, delta, deltaX, deltaY) {
          var $scrollable = $(this);

          if (!isFixed($scrollable.closest('.popup'))) {
            return;
          }

          updateMaxScrollTop($scrollable);

          if (deltaY !== 0) {
            var scrollTop = $scrollable.scrollTop();

            if ((deltaY > 0 && scrollTop === 0) ||
                (deltaY < 0 && scrollTop >= $.data($scrollable[0], 'fixedScrollable-maxScrollTop'))) {
              event.preventDefault();
            }
          }
        });
      });
    },

    '_all': function(scrollables) {
      $scrollables = $scrollables.add(scrollables);

      resizeScrollables();
    }
  });
});
