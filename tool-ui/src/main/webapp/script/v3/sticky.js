define([ 'jquery', 'bsp-utils', 'sticky-kit' ], function($, bsp_utils) {
  function toolHeaderBottom(includeMargin) {
    var $toolHeader = $('.toolHeader');
    return $toolHeader.offset().top - $(window).scrollTop() + $toolHeader.outerHeight(includeMargin);
  }

  function inFullscreen($element) {
    return $element.closest('.popup').parent().is('body.rte-fullscreen');
  }

  bsp_utils.onDomInsert(document, '.withLeftNav > .leftNav, .withLeftNav > .main, .contentForm-main', {
    insert: function (element) {
      var $element = $(element);

      if (inFullscreen($element)) {
        return;
      }

      // If we are in an iframe do not make it sticky
      // This is used for editInline
      if (window.parent !== window.top) {
          return;
      }
      
      $element.stick_in_parent({
        offset_top: function () {
          return toolHeaderBottom(true);
        }
      });
    }
  });

  bsp_utils.onDomInsert(document, '.contentForm-aside', {
    insert: function (aside) {
      var $aside = $(aside);

      if (inFullscreen($aside)) {
        return;
      }

      var $publishing = $aside.find('> .widget-publishing');
      var $widgets = $aside.find('> .contentWidgets');
      var $window = $(window);
      var attached;

      function stick() {
        if ($publishing.outerHeight(true) > $window.height() * 0.5) {
          if (attached) {
            attached = false;

            $publishing.add($widgets).trigger('sticky_kit:detach');
            $widgets.css('clip', '');
          }

        } else if (!attached) {
          attached = true;

          $publishing.stick_in_parent({
            parent: '.contentForm',
            offset_top: function () {
              return toolHeaderBottom(true);
            }
          });

          $widgets.stick_in_parent({
            parent: '.contentForm',
            offset_top: function () {
              return toolHeaderBottom(true) + $publishing.outerHeight(true);
            },
            offset_change: function (offset) {
              $widgets.css({
                clip: 'rect(' + (150 - offset) + 'px auto auto auto)'
              });
            }
          });
        }
      }

      stick();
      $window.resize(bsp_utils.throttle(500, stick));
    }
  });

  bsp_utils.onDomInsert(document, '.rte2-toolbar', {
    insert: function (element) {
      var $element = $(element);

      if (inFullscreen($element)) {
        return;
      }

      $element.stick_in_parent({
        recalc_every: 100,
        offset_top: function () {
          return toolHeaderBottom(false);
        }
      });
    }
  });

  bsp_utils.onDomInsert(document, '.rte2-wrapper.rte-fullscreen', {
    insert: function (wrapper) {
      $(wrapper).scroll(bsp_utils.throttle(500, function () {
        $(document.body).trigger("sticky_kit:recalc");
      }));
    }
  });

  $(document).on('tabbed-select', function () {
    $(document.body).trigger("sticky_kit:recalc");
  });
});
