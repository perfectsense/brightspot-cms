define([ 'jquery', 'v3/rtc' ], function($, rtc) {
  function reload() {
    $('.dashboard-widget').each(function() {
      var $widget = $(this);
      var widgetUrl = $widget.attr('data-dashboard-widget-url');

      if (widgetUrl) {
        $.ajax({
          'cache': false,
          'type': 'get',
          'url': widgetUrl,
          'complete': function(response) {
            $widget.html(response.responseText);
            $widget.trigger('create');
            $widget.trigger('load');
            $widget.trigger('frame-load');
          }
        });
      }
    })
  }

  var reloadTimeout;

  rtc.receive('com.psddev.cms.tool.page.content.PublishBroadcast', function() {
    if (reloadTimeout) {
      clearTimeout(reload);
    }

    reloadTimeout = setTimeout(function() {
      reloadTimeout = null;

      reload();
    }, 5000)
  });
});