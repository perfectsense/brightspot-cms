/** Inline popup. */
(function($, win, undef) {

  var $win = $(win),
      doc = win.document;

  $.plugin2('popup', {
    '_defaultOptions': {
      'padding': {
        'left': 35,
        'right': 35,
        'top': 20
      }
    },

    '_create': function(element) {
      var $inner = $(element);
      var options = this.option();
      var $container = $('<div/>', { 'class': 'popup' });
      var $content = $('<div/>', { 'class': 'content' });
      var $closeButton = $('<div/>', { 'class': 'closeButton' });

      var name = $inner.attr('name');
      if (name) {
        $container.attr('name', name);
      }

      // Bind open and close events.
      $container.bind('open.popup', function() {
        var $original = $(this);
        var scrollLeft = $original.data('popup-scrollLeft');
        var scrollTop = $original.data('popup-scrollTop');
        if (typeof scrollLeft !== 'number' && typeof scrollTop !== 'number') {
          $original.data('popup-scrollLeft', $win.scrollLeft());
          $original.data('popup-scrollTop', $win.scrollTop());
        }
        $original.addClass('popup-show');
      });

      $container.bind('restoreOriginalPosition.popup', function() {
        var $original = $(this);
        var scrollLeft = $original.data('popup-scrollLeft');
        var scrollTop = $original.data('popup-scrollTop');
        $original.removeData('popup-scrollLeft');
        $original.removeData('popup-scrollTop');
        if (typeof scrollLeft === 'number' && typeof scrollTop === 'number') {
          $win.scrollLeft(scrollLeft);
          $win.scrollTop(scrollTop);
        }
      });

      $container.bind('close.popup', function() {
        if ($container.is(':visible') &&
            $container.find('.contentForm .inputContainer.state-changed').length > 0 &&
            confirm('Are you sure you want to close this popup and discard the unsaved changes?')) {
          return;
        }

        var $original = $(this);
        $original.removeClass('popup-show');
        $('.popup').each(function() {
          var $popup = $(this);
          var $source = $popup.popup('source');
          if ($source && $.contains($original[0], $source[0])) {
            $popup.popup('close');
          }
        });
      });

      $closeButton.bind('click.popup', function() {
        $(this).popup('close');
      });

      var $body = $(doc.body);
      $content.append($inner);
      $content.append($closeButton);
      $container.append($content);
      $body.append($container);
    },

    // Opens the popup.
    'open': function() {
      this.$caller.popup('container').trigger('open');
      return this.$caller;
    },

    'restoreOriginalPosition': function() {
      this.$caller.popup('container').trigger('restoreOriginalPosition');
      return this.$caller;
    },

    // Closes the popup.
    'close': function() {
      this.container().trigger('close');
      return this.$caller;
    },

    // Returns the enclosing element that contains the popup.
    'container': function() {
      return this.$caller.closest('.popup');
    },

    // Returns the popup content element.
    'content': function() {
      return this.$caller.popup('container').find('> .content');
    },

    // Returns the source element that triggered the popup to open.
    'source': function($newSource, event) {
      var options = this.option();

      if (event &&
          ($newSource.height() < 30 ||
          typeof event.pageX !== 'number' ||
          typeof event.pageY !== 'number')) {
        event = undef;
      }

      if (typeof $newSource === 'undefined') {
        var container = this.$caller.popup('container')[0];
        return container ? $.data(container, 'popup-$source') : null;

      } else {
        var $container = this.$caller.popup('container');
        $container.attr('data-popup-source-class', $newSource.prop('class'));
        $container.each(function() {
          $.data(this, 'popup-$source', $newSource);
        });

        // Change position.
        var sourceOffset = $newSource.offset();
        var popupWidth = $container.outerWidth();

        // Make sure left is within bounds.
        var markerDelta = 0;
        var left = event ? event.pageX - popupWidth / 2 : sourceOffset.left + ($newSource.outerWidth() - popupWidth) / 2;
        if (left < options.padding.left) {
          markerDelta = left - options.padding.left;
          left = options.padding.left;
        } else {
          var leftDelta = left + popupWidth - $(doc).width() + options.padding.right;
          if (leftDelta > 0) {
            markerDelta = leftDelta;
            left -= leftDelta;
          }
        }

        // Create a arrow-like marker.
        var $content = $container.popup('content');
        var $marker = $content.find('> .marker');
        if ($marker.length === 0) {
          $marker = $('<div/>', { 'class': 'marker' });
          $content.append($marker);
        }
        var markerLeft = (popupWidth  - $marker.outerWidth()) / 2 + markerDelta;
        $marker.css('left', markerLeft < 5 ? 5 : markerLeft);

        // Make sure top is within bounds.
        var top = event ? event.pageY : sourceOffset.top + $newSource.outerHeight();
        if (top < 30) {
          top = 30;
        }

        // Adjust left/top if position is fixed.
        var isFixedPosition = $newSource.isFixedPosition();
        if (isFixedPosition) {
          left -= $(window).scrollLeft();
          top -= $(window).scrollTop();
        }

        var $newSourceParent = $newSource.closest('.popup');
        if ($newSourceParent.length === 0) {
          $newSourceParent = $newSource.parent();
        }

        if (isFixedPosition) {
          $container.addClass('popup-fixed');
        }

        $container.css({
          'left': left,
          'margin': 0,
          'position': isFixedPosition ? 'fixed' : 'absolute',
          'top': top,
          'z-index': $newSource.parent().zIndex() + 1
        });

        return this.$caller;
      }
    }
  });

  // Clicking outside the popup should close it.
  $win.click(function(event) {
    var target = event.target;
    var $target = $(target);

    // Special containers that should never close the popup.
    if ($target.closest('#context-menu-layer, .context-menu-root, .search-reset, .queryField_frames').length > 0) {
      return;
    }

    // Close the popup if clicked outside of its own content (e.g. shadow).
    var $targetPopup = $target.popup('container');

    if ($targetPopup.length > 0) {
      var $targetPopupContent = $targetPopup.find('> .content');

      if ($targetPopupContent.length === 0 || !$.contains($targetPopupContent[0], target)) {
        $targetPopup.popup('close');
      }

    } else {

      // Close all popups except the one that was just clicked on.
      $('.popup').each(function() {
        var $popup = $(this);
        var $popupSource = $popup.popup('source');

        if ($popupSource && $popupSource.length > 0) {
          var popupSource = $popupSource[0];

          if (popupSource === target || $.contains(popupSource, target)) {
            return;
          }
        }

        $popup.popup('close');
      });
    }
  });

  // Hitting ESC closes all popups.
  $win.keydown(function(event) {
    if (event.which === 27) {
      var $popups = $('.popup');

      if ($popups.length > 0) {
        $popups.popup('close');
        return false;

      } else {
        return true;
      }
    }
  });

}(jQuery, window));