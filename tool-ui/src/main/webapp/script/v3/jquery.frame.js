/** Inline FRAME/IFRAME replacement. */
(function($, win, undef) {

var $win = $(win),
    doc = win.document,
    formTargetIndex = 0;

$.plugin2('frame', {
  '_defaultOptions': {
    'frameClassName': 'dari-frame',
    'loadingClassName': 'dari-frame-loading',
    'loadedClassName': 'dari-frame-loaded',
    'bodyClassName': 'dari-frame-body',
    'setBody': function(body) {
      $(this).html(body);
    }
  },

  '_init': function(selector, options) {
    var plugin = this,
        $caller = plugin.$caller,
        frameClassName = options.frameClassName,
        loadingClassName = options.loadingClassName,
        loadedClassName = options.loadedClassName,
        bodyClassName = options.bodyClassName,
        findTargetFrame,
        beginLoad,
        endLoad,
        loadPage;

    // Finds the target frame, creating one if necessary.
    findTargetFrame = function(element, callback) {
      var $element = $(element),
          href = $element.attr('href') || '',
          target = $element.attr('data-frame-target') || $element.attr('target'),
          $frame;

      // Standard HTML elements that can handle the target.
      if (target && $('frame[name="' + target + '"], iframe[name="' + target + '"]').length > 0) {
        return true;
      }

      // Skip processing on special target names.
      if (target !== '_top' && target !== '_blank') {
        if (target === '_parent') {
          $frame = $element.frame('container').parent().frame('container');

        } else if (target) {
          $frame = $('.' + frameClassName + '[name="' + target + '"]');

          if ($frame.length === 0) {
            $frame = $('<div/>', { 'class': frameClassName, 'name': target });
            $(doc.body).append($frame);
            $frame.popup();
          }

        } else {

          // There is no target.
          $frame = $element.frame('container');

          // Special case - if the HREF is not going to a new page, don't try to load the frame again
          if (href.indexOf('#') === 0) {
            return true;
          }
          
        }

        if ($frame.length > 0 && $frame[0] !== doc) {
          return callback($element, $frame);
        }
      }

      // Natural browser event.
      return true;
    };

    // Begins loading $frame using $source.
    beginLoad = function($frame, $source, event) {
      var version = ($frame.data('frame-loadVersion') || 0) + 1,
          $popup = $frame.popup('container');

      $frame.add($popup).removeClass(loadedClassName).addClass(loadingClassName);
      $frame.data('frame-loadVersion', version);
      $frame.data('frame-$source', $source);

      if ($popup[0] && $source[0] && !$.contains($popup[0], $source[0])) {
        $frame.popup('source', $source, event);
        $frame.empty();
      }

      if ($frame.parent().frame('container').length == 0) {
        $frame.popup('open');
      }

      return version;
    };

    // Ends loading $frame by setting it using data.
    endLoad = function($frame, version, data) {
      var $popup,
          $wrapper,
          $bodyContainer,
          body;

      if (version >= $frame.data('frame-loadVersion')) {
        $popup = $frame.popup('container');

        body = data;
        body = body.replace(/^[^]*?<body[^>]*>/ig, '');
        body = body.replace(/<\/body>[^]*?$/ig, '');

        $frame.add($popup).removeClass(loadingClassName).addClass(loadedClassName);
        options.setBody.call($frame[0], body);

        $frame.trigger('create');
        $frame.trigger('load');
        $frame.trigger('frame-load');

        $win.resize();
      }
    };

    // Loads the page at url into the $frame.
    loadPage = function($frame, $source, method, url, data, event) {
      var plugin = this,
          version = beginLoad($frame, $source, event),
          extraFormData = $frame.attr('data-extra-form-data');

      if (extraFormData) {
        url += (url.indexOf('?') < 0 ? '?' : '&') + extraFormData;
      }

      $.ajax({
        'cache': false,
        'type': method,
        'url': url,
        'data': data,
        'complete': function(response) {
          endLoad($frame, version, response.responseText);
        }
      });
    };

    // Intercept anchor clicks to see if it's targeted.
    $caller.delegate('a', 'click.frame', function(event) {
      return findTargetFrame(this, function($anchor, $frame) {
        loadPage($frame, $anchor, 'get', $anchor.attr('href'), null, event);
        return false;
      });
    });

    $caller.on('change', ':checkbox[data-frame-target]', function(event) {
      return findTargetFrame(this, function($checkbox, $frame) {
        var href = $checkbox.prop('checked') ?
            $checkbox.attr('data-frame-check') :
            $checkbox.attr('data-frame-uncheck');

        loadPage($frame, $checkbox, 'get', href, null, event);
      });
    });

    // Intercept form submits to see if it's targeted.
    $caller.delegate('form', 'click.frame', function(event) {
      if (!$(event.target).is('button, input[type="submit"]')) {
        return;
      }

      $.data(this, 'frame-clicked', event.target);
    });

    $caller.delegate('form', 'submit.frame', function() {
      return findTargetFrame(this, function($form, $frame) {
        var action = $form.attr('action'),
            extraFormData = $frame.attr('data-extra-form-data'),
            clicked = $.data($form[0], 'frame-clicked'),
            clickedName,
            clickedValue;

        if (clicked) {
          clickedName = $(clicked).prop('name');
          clickedValue = $(clicked).prop('value');

          if (clickedName && clickedValue) {
            action += (action.indexOf('?') > -1 ? '&' : '?') + encodeURIComponent(clickedName) + '=' + encodeURIComponent(clickedValue);
          }
        }

        if ($form.attr('enctype') !== 'multipart/form-data') {
          loadPage($frame, $form, $form.attr('method'), action, $form.serialize());
          return false;
        }

        $form.attr('action', action + (action.indexOf('?') < 0 ? '?' : '&') + extraFormData);

        if ($form.find('input[type="hidden"][name="_frame"]').length === 0) {
          $form.prepend($('<input/>', {
            'name': '_frame',
            'type': 'hidden',
            'value': 'true'
          }));
        }

        // Add a target for $submitFrame later in case one doesn't exist.
        var target = $form.attr('target');
        var hasTarget = true;
        if (!target) {
          formTargetIndex += 1;
          target = 'frameTarget' + formTargetIndex + (+new Date());
          $form.attr('target', target);
          hasTarget = false;
        }

        var $submitFrame = $('iframe[name=' + target + ']');
        if ($submitFrame.length === 0) {
          $submitFrame = $('<iframe/>', { 'name': target });
          $submitFrame.hide();
          $(doc.body).append($submitFrame);
        }

        var version = beginLoad($frame, $form);
        $submitFrame.unbind('.frame');
        $submitFrame.bind('load.frame', function() {
          $form.attr('action', action);
          endLoad($frame, version, $submitFrame.contents().find('textarea#frameBody').val() || $submitFrame.contents().find('body').html());
          if (!hasTarget) {
            $form.removeAttr('target');
            setTimeout(function() { $submitFrame.remove(); }, 0);
          }
        });

        return true;
      });
    });

    // Any existing frame should be loaded.
    $caller.onCreate('.' + frameClassName, function() {
      var $frame = $(this),
          $anchor;

      if ($frame.is(':not(.' + loadingClassName + '):not(.' + loadedClassName + ')')) {
        $anchor = $frame.find('a:only-child:not([target])');

        if ($anchor.length > 0) {
          $anchor.click();

        } else {
          $frame.find('form:only-child:not([target])').submit();
        }
      }
    });

    return $caller;
  },

  // Returns the enclosing element that contains the frame.
  'container': function() {
    return this.$caller.closest('.' + this.option('frameClassName'));
  },

  // Returns the source element that triggered the frame to be populated.
  'source': function() {
    return this.container().data('frame-$source');
  }
});

}(jQuery, window));
