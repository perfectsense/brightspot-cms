define([ 'jquery', 'bsp-utils', 'v3/rtc' ], function($, bsp_utils, rtc) {
  rtc.receive('com.psddev.cms.tool.page.content.PublishBroadcast', bsp_utils.throttle(30000, function() {
    setTimeout(function() {
      $('.dashboard-widget').each(function() {
        var $widget = $(this);
        var widgetUrl = $widget.attr('data-dashboard-widget-url');

        // Prevent widget refresh under the following scenarios:
        // 1. User is hovering over the widget
        // 2. User has a dropdown open for the current widget
        // 3. User has activated the page thumbnail preview icon (only relevant for search result widgets)
        // 4. User has triggered a click or a change event on the widget form
        if (widgetUrl
            && !$widget.is(':hover')
            && $widget.data('refresh-disabled') !== true
            && $widget.find('.dropDown-list-open').size() === 0
            && $('body').children('.pageThumbnails_toggle').size() === 0) {

          $.ajax({
            'cache': false,
            'type': 'get',
            'url': widgetUrl,
            'complete': function(response) {
              $widget.html(response.responseText);
              $widget.trigger('create');
              $widget.trigger('load');
              $widget.trigger('frame-load');
              $widget.resize();
            }
          });
        }
      });
    }, 2000);
  }));

  // Disables automatic refresh on click or change event
  $(document).on('change click', '[data-dashboard-widget-url] form, [data-dashboard-widget-url] a', function() {
    var $widget = $(this).closest('[data-dashboard-widget-url]');

    // prevent disabling refresh on first form load
    if (!$widget.hasClass('loading') && !$widget.hasClass('loaded')) {
      return false;
    }

    $(this).closest('[data-dashboard-widget-url]').data('refresh-disabled', true);
  });
});
