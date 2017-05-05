define([ 'jquery', 'bsp-utils', 'v3/rtc' ], function($, bsp_utils, rtc) {
  rtc.receive('com.psddev.cms.tool.page.content.PublishBroadcast', bsp_utils.throttle(30000, function() {
    setTimeout(function() {
      $('.dashboard-widget').each(function() {
        var $widget = $(this);
        var widgetUrl = $widget.attr('data-dashboard-widget-url');

        if (widgetUrl && !$widget.is(':hover')) {
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
