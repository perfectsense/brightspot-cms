define([ 'string', 'bsp-utils' ], function (S, bsp_utils) {
    
/** Better drop-down list than standard SELECT. */
(function($, win, undef) {

  var $win = $(win),
      doc = win.document,
      $doc = $(doc),
      $openOriginal,
      $openList;

  var dropdownIdIndex = 0;

  $.plugin2('dropDown', {
    '_defaultOptions': {
      'classPrefix': 'dropDown-'
    },

    'className': function(name) {
      return this.option('classPrefix') + name;
    },

    _initVisible: function ($original) {
      var plugin = this,
          opened = false,
          isFixedPosition = $original.isFixedPosition(),
          isMultiple = $original.is('[multiple]'),
          isSearchable = $original.is('[data-searchable="true"]'),
          placeholder = $original.attr('placeholder'),
          dynamicPlaceholderText = $original.attr('data-dynamic-placeholder'),
          dynamicPlaceholderHtml = dynamicPlaceholderText && '<span data-dynamic-text=' + dynamicPlaceholderText + '>',
          $input,
          $label,
          $search,
          containerCss,
          $markerContainer,
          $marker,
          $listContainer,
          $list,
          addItem;

      var dropdownId = dropdownIdIndex;
      ++ dropdownIdIndex;

      if (!isMultiple &&
          $original.find('option:selected').length === 0) {
        $original.find('option:first').prop('selected', true);
      }

      $original.bind('input-disable', function(event, disable) {
        $input.toggleClass('state-disabled', disable);
      });
      
      var minWidth = $original.outerWidth();

      $input = $('<div/>', {
        'class': plugin.className('input'),
        'data-dropdown-id': dropdownId,
        'css': {
          'margin-bottom': $original.css('margin-bottom'),
          'margin-left': $original.css('margin-left'),
          'margin-right': $original.css('margin-right'),
          'margin-top': $original.css('margin-top'),
          'min-width': minWidth,
          'max-width': '100%',
          'position': 'relative',
          'width': isMultiple ? minWidth * 2 : ''
        }
      });

      $input.click(function() {
        if ($label.is(':visible')) {
          $label.click();

        } else {
          if ($input.hasClass(plugin.className('list-open'))) {
            $list.trigger('dropDown-close');

          } else {
            $search.click();
          }
        }

        return false;
      });

      $label = $('<a/>', {
        'class': plugin.className('label'),
        'href': '#',
        'click': function() {
          if (!$input.is('.state-disabled')) {
            if ($openList && $openList[0] === $list[0]) {
              $list.trigger('dropDown-close');

            } else {
              $list.trigger('dropDown-open');
            }
          }

          return false;
        }
      });

      function resize() {
        if (!opened) {
          return;
        }

        if (!$input.is(':visible')) {
          $openOriginal = null;
          $openList = null;
          $markerContainer.hide();
          $listContainer.hide();
          return;
        }

        var inputOffset = $input.offset();
        var inputLeft = inputOffset.left;
        var inputTop = inputOffset.top;
        var inputWidth = $input.outerWidth();
        var inputHeight = $input.outerHeight();
        var winScrollTop = $win.scrollTop();
        var winHeight = $win.height();

        var attachmentFunctions = {
          left: function () {
            if (inputWidth > $listContainer.outerWidth()) {
              $listContainer.css('min-width', inputWidth + 20);
            }

            $input.add($listContainer).add($markerContainer).attr('data-attachment', 'left');

            $listContainer.css({
              'bottom': (0 - winScrollTop),
              'left': inputLeft - $listContainer.outerWidth(),
              'top': winScrollTop
            });

            $markerContainer.css({
              'height': $input.outerHeight(),
              'left': inputLeft,
              'top': inputTop
            });
          },

          right: function () {
            if (inputWidth > $listContainer.outerWidth()) {
              $listContainer.css('min-width', inputWidth + 20);
            }

            $input.add($listContainer).add($markerContainer).attr('data-attachment', 'right');
            $listContainer.add($markerContainer).css({
              'left': inputLeft + inputWidth,
            });

            $listContainer.css({
              'bottom': (0 - winScrollTop),
              'top': winScrollTop
            });

            $markerContainer.css({
              'height': $input.outerHeight(),
              'top': inputTop
            });
          },

          side: function () {
            if (inputLeft < $win.width() - inputLeft - inputWidth) {
              attachmentFunctions.right();

            } else {
              attachmentFunctions.left();
            }
          },

          top: function () {
            if (inputWidth > $listContainer.outerWidth()) {
              $listContainer.css('min-width', inputWidth + 20);
            }

            var markerBottom = winHeight - inputTop;

            if (isFixedPosition) {
              markerBottom += winScrollTop;
            }

            var listContainerHeight = inputTop - winScrollTop;

            $listContainer.css('height', listContainerHeight);

            $list.css({
              'bottom': '',
              'top': $list.outerHeight() > listContainerHeight ? '' : 'auto'
            });

            $input.add($listContainer).add($markerContainer).attr('data-attachment', 'top');
            $listContainer.add($markerContainer).css({
              'bottom': markerBottom,
              'left': inputLeft,
              'top': ''
            });

            $markerContainer.css('width', $input.outerWidth());
          },

          bottom: function () {
            if (inputWidth > $listContainer.outerWidth()) {
              $listContainer.css('min-width', inputWidth + 20);
            }

            var markerTop = inputTop + inputHeight;

            if (isFixedPosition) {
              markerTop -= winScrollTop;
            }

            var listContainerHeight = winScrollTop + winHeight - inputTop - inputHeight;

            $listContainer.css('height', listContainerHeight);

            $list.css({
              'bottom': $list.outerHeight() > listContainerHeight ? '' : 'auto',
              'top': ''
            });

            $input.add($listContainer).add($markerContainer).attr('data-attachment', 'bottom');
            $listContainer.add($markerContainer).css({
              'bottom': '',
              'left': inputLeft,
              'top': markerTop
            });

            $markerContainer.css('width', $input.outerWidth());
          },

          end: function () {
            var heightAbove = inputTop - winScrollTop;

            if (heightAbove < winHeight - (heightAbove + inputHeight)) {
              attachmentFunctions.bottom();

            } else {
              attachmentFunctions.top();
            }
          }
        };

        var attach = attachmentFunctions[$original.attr('data-attachment')];

        if (!attach) {
          attach = attachmentFunctions.end;
        }

        attach();
      }

      $label.bind('dropDown-update', function() {
        var newLabel = $.map($original.find('option').filter(':selected'), function(option) {
          return $(option).attr("data-drop-down-html") || $(option).text();
        }).join(', ');

        $label.html(newLabel || dynamicPlaceholderHtml || placeholder || '&nbsp;');
        $label.toggleClass('state-placeholder', !newLabel);

        resize();
      });

      containerCss = {
        'display': 'none',
        'position': isFixedPosition ? 'fixed' : 'absolute',
        'z-index': $original.zIndex() + 1000000
      };

      $markerContainer = $('<div/>', {
        'css': containerCss
      });

      $marker = $('<div/>', {
        'class': plugin.className('marker')
      });

      $listContainer = $('<div/>', {
        'class': plugin.className('container'),
        'data-dropdown-id': dropdownId,
        'data-original-class': $original.attr('class'),
        'css': containerCss
      });

      $list = $('<div/>', {
        'class': plugin.className('list')
      });

      $list.bind('dropDown-open', function() {
        opened = true;

        resize();

        $input.addClass(plugin.className('list-open'));

        $list.find('.' + plugin.className('listItem')).removeClass('state-hover');
        $list.find('.' + plugin.className('listItem') + (isMultiple ? ':first' : ':has(:checked)')).addClass('state-hover');

        if ($openList) {
          $openList.trigger('dropDown-close');
        }

        $openOriginal = $original;
        $openList = $list;
        $markerContainer.show();
        $listContainer.show();
        $list.scrollTop(0);

        var $selected = $list.find('.' + plugin.className('listItem-selected'));

        if ($selected.length > 0) {
          var attachment = $listContainer.attr('data-attachment');
          var scrollTop = $selected.position().top - $list.find('.' + plugin.className('listItem')).eq(0).position().top;

          if (attachment === 'left' || attachment === 'right') {
            scrollTop -= $input.offset().top - $win.scrollTop() - 20;
          }

          $list.scrollTop(scrollTop);
        }
      });

      $list.bind('dropDown-close', function() {
        opened = false;

        $input.removeClass(plugin.className('list-open'));

        $openOriginal = null;
        $openList = null;
        $markerContainer.hide();
        $listContainer.hide();

        if (isMultiple) {
          $original.change();
        }
      });

      $list.bind('dropDown-hover', function(event, $item) {
        $list.find('.' + plugin.className('listItem')).removeClass('state-hover');

        if ($item) {
          $item.addClass('state-hover');
        }
      });

      $list.bind('mousewheel', function(event, delta, deltaX, deltaY) {
        var $list = $(this),
            maxScrollTop = $.data(this, 'dropDown-maxScrollTop');

        if (typeof maxScrollTop === 'undefined') {
          maxScrollTop = $list.prop('scrollHeight') - $list.innerHeight();
          $.data(this, 'dropDown-maxScrollTop', maxScrollTop);
        }

        if ((deltaY > 0 && $list.scrollTop() === 0) ||
            (deltaY < 0 && $list.scrollTop() >= maxScrollTop)) {
          event.preventDefault();
        }
      });

      // Detect clicks within the window to toggle the list properly.
      $doc.mousedown(function(event) {
        if ($listContainer.is(':visible') &&
            !$.contains($listContainer[0], event.target) &&
            $input[0] !== event.target &&
            !$.contains($input[0], event.target)) {
          $list.trigger('dropDown-close');
        }
      });

      // Recalculate position and size whenever viewport is affected.
      $(window).bind('resize', bsp_utils.throttle(15, resize));

      // Create the list based on the options in the original input.
      addItem = function($option) {
        var $item,
            $check;

        $item = $('<div/>', {
          'class': plugin.className('listItem'),
          'html': $option.attr("data-drop-down-html") || $option.text() || '&nbsp;'
        });

        $check = $('<input/>', {
          'type': isMultiple ? 'checkbox' : 'radio'
        });

        if ($option.is(':selected')) {
          $check.prop('checked', true);
          $item.addClass(plugin.className('listItem-selected'));
        }

        $item.mouseenter(function() {
          $list.trigger('dropDown-hover', [ $item ]);
        });

        $item.mouseleave(function() {
          $list.trigger('dropDown-hover');
        });

        $item.click(isMultiple ? function() {
          if ($option.is(':selected')) {
            $option.prop('selected', false);
            $item.find(':checkbox').prop('checked', false);
            $item.removeClass(plugin.className('listItem-selected'));

          } else {
            $option.prop('selected', true);
            $item.find(':checkbox').prop('checked', true);
            $item.addClass(plugin.className('listItem-selected'));
          }

          $label.trigger('dropDown-update');

          return false;

        } : function() {
          if (!$option.is(':selected')) {
            $original.find('option').prop('selected', false);
            $list.find(':radio').prop('checked', false);
            $list.find('.' + plugin.className('listItem')).removeClass(plugin.className('listItem-selected'));

            $option.prop('selected', true);
            $check.prop('checked', true);
            $item.addClass(plugin.className('listItem-selected'));

            $label.trigger('dropDown-update');
            $original.change();
          }

          $list.trigger('dropDown-close');

          return false;
        });

        $item.prepend(' ');
        $item.prepend($check);
        $list.append($item);
      };

      $original.find('> optgroup, > option').each(function() {
        var $child = $(this);

        if ($child.is('option')) {
          addItem($child);

        } else {
          $list.append($('<div/>', {
            'class': plugin.className('listGroupLabel'),
            'text': $child.attr('label')
          }));

          $child.find('> option').each(function() {
            addItem($(this));
          });
        }
      });

      // Replace input with the custom control.
      $label.trigger('dropDown-update');
      $input.append($label);
      $original.before($input);
      $original.hide();

      var $containers = $(doc.body).find('> .' + plugin.className('containers'));

      if ($containers.length === 0) {
        $(doc.body).append($containers = $('<div/>', {
          'class': plugin.className('containers')
        }));
      }

      $listContainer.append($list);
      $containers.append($listContainer);
      $listContainer.css('min-width', $listContainer.outerWidth());

      $markerContainer.append($marker);
      $containers.append($markerContainer);

      if (isSearchable) {
        $search = $('<input/>', {
          'class': plugin.className('search'),
          'type': 'text'
        });

        $search.bind('input', function() {
          var re = new RegExp(S($search.val().replace(/\s/, '').split('').join('(?:.*\\W)?')).latinise().s, 'i'),
              $first;

          $list.find('.' + plugin.className('listItem')).each(function() {
            var $item = $(this);

            if (re.test(S($item.text()).latinise().s)) {
              $item.show();

              if (!$first) {
                $first = $item;
              }

            } else {
              $item.hide();
            }
          });

          $list.trigger('dropDown-hover', [ $first ]);
        });

        $search.click(function() {
          return false;
        });

        $list.bind('dropDown-open', function() {
          $label.hide();
          $list.find('.' + plugin.className('listItem')).show();

          $search.val($label.text());
          $search.show();
          $search.focus();
          $search.select();
        });

        $list.bind('dropDown-close', function() {
          $label.show();
          $search.hide();
        });

        $input.append($search);
        $input.css('min-width', $input.outerWidth());
        $search.hide();
      }
    },

    '_create': function(original) {
      var plugin = this;
      var $original = $(original);
      var init = function () {
        plugin._initVisible($original);
      };

      $win.resize(function () {
        if ($original.is(':visible')) {
          $win.unbind('resize', init);
          init();
        }
      })
    }
  });

  $doc.keydown(function(event) {
    var which,
        isUp,
        LIST_ITEM_CLASS,
        $hover,
        hoverTop,
        hoverHeight,
        delta;

    if ($openList) {
      which = event.which;
      isUp = which === 38;

      if (!(event.altKey || event.ctrlKey || event.metaKey || event.shiftKey)) {
        LIST_ITEM_CLASS = $openOriginal.dropDown('className', 'listItem');

        if (isUp || which === 40) {
          $hover = $openList.find('.state-hover:visible.' + LIST_ITEM_CLASS).eq(0);

          if ($hover.length === 0) {
            $hover = $openList.find(':visible.' + LIST_ITEM_CLASS).eq(isUp ? -1 : 0);

          } else {
            $hover = $hover[isUp ? 'prevAll' : 'nextAll'](':visible.' + LIST_ITEM_CLASS).eq(0);
          }

          if ($hover.length > 0) {
            $openList.trigger('dropDown-hover', [ $hover ]);

            hoverTop = $hover.position().top;
            hoverHeight = $hover.outerHeight();

            if (isUp) {
              if (hoverTop < 0) {
                $openList.scrollTop($openList.scrollTop() + hoverTop);
              }

            } else {
              delta = hoverTop + hoverHeight - $openList.height();

              if (delta > 0) {
                $openList.scrollTop($openList.scrollTop() + delta);
              }
            }
          }

          return false;

        } else if (which === 13) {
          $openList.find('.state-hover.' + LIST_ITEM_CLASS).click();

          return false;

        } else if (which === 27) {
          $openList.trigger('dropDown-close');

          return false;
        }
      }
    }

    return true;
  });
  
}(jQuery, window));

    // Return an empty object just for the benefit of the AMD module.
    // Not expected to be used since we just set up a jquery plugin.
    return {};
    
}); // define()
