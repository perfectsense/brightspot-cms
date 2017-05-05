define([ 'jquery', 'bsp-utils', 'v3/rtc' ], function($, bsp_utils, rtc) {
  rtc.receive('com.psddev.cms.tool.page.content.PublishBroadcast', bsp_utils.throttle(30000, function() {
    setTimeout(function() {
      $('.dashboard-widget').each(function() {
        var $widget = $(this);
        var widgetUrl = $widget.attr('data-dashboard-widget-url');


        // prevent widget refresh under the followign scenarios:
        // 1. User is hovering over the widget
        // 2. User has a dropdown open for the current widget
        // 3. User has activated the page thumbnail preview icon (only relevant for search result widgets)
        if (widgetUrl
            && !$widget.is(':hover')
            && $widget.find('.dropDown-list-open').size() === 0
            && $('body').find('.pageThumbnails_toggle').size() === 0) {

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
});
