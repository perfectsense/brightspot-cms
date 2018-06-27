define([ 'bsp-utils', 'js.cookie', 'jquery' ], function(bsp_utils, Cookies, $) {
  var csrf = Cookies.get('bsp.csrf');

  if (!csrf) {
    return;
  }

  bsp_utils.onDomInsert(document, 'form', {
    'insert': function(form) {
      $(form).append($('<input/>', {
        type: 'hidden',
        name: '_csrf',
        value: csrf
      })).trigger("csrfReady");
    }
  });

  $(document).ajaxSend(function(event, jqxhr, options) {
    if (options.type === 'POST') {
      jqxhr.setRequestHeader('Brightspot-CSRF', csrf)
    }
  });
});