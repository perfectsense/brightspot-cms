define([ 'jquery', 'bsp-utils', 'diff', 'v3/RecalculateDimensions' ], function($, bsp_utils, JsDiff, RecalculateDimensions) {
  bsp_utils.onDomInsert(document, '.contentDiff', {
    'insert': function(container) {
      var $container = $(container);
      var $tabs;
      var $tabEdit;
      var $tabSideBySide;
      var $left = $container.find('> .contentDiffLeft');
      var $right = $container.find('> .contentDiffRight');

      $tabs = $('<ul/>', {
        'class': 'tabs'
      });

      $tabEdit = $('<li/>', {
        'html': $('<a/>', {
          'text': 'Edit',
          'click': function() {
            $container.trigger('contentDiff-edit');
            return false;
          }
        })
      });

      $tabSideBySide = $('<li/>', {
        'html': $('<a/>', {
          'text': 'Side By Side',
          'click': function() {
            $container.trigger('contentDiff-sideBySide');
            return false;
          }
        })
      });

      $container.bind('contentDiff-edit', function() {
        $container.removeClass('contentDiff-sideBySide').addClass('contentDiff-edit');
        $tabs.find('li').removeClass('state-selected');
        $tabEdit.addClass('state-selected');

        var $both = $left.add($right);

        $both.find('.inputContainer').css('height', '');
        RecalculateDimensions.trigger($container);
      });

      $container.bind('contentDiff-sideBySide', function() {
        $container.removeClass('contentDiff-edit').addClass('contentDiff-sideBySide');
        $tabs.find('li').removeClass('state-selected');
        $tabSideBySide.addClass('state-selected');
        RecalculateDimensions.trigger($container);
      });

      var equalizeHeights = $.throttle(100, function() {
        var $contentDiffRightContainers = $container.find('.contentDiffRight .inputContainer');
        $container.find('.contentDiffLeft .inputContainer').not('.contentDiffSame').each(function() {
          var $leftInput = $(this);
          var $rightInput = $contentDiffRightContainers.filter('[data-name="' + $leftInput.attr('data-name') + '"]');
          var $bothInputs = $leftInput.add($rightInput);

          $bothInputs.css('height', '');
          $bothInputs.css('height', Math.max($leftInput.height(), $rightInput.height()));
        });
      });

      $container.bind('resize', equalizeHeights);
      setInterval(equalizeHeights, 500);

      function getValues($input) {
        return $input.
            find(':input, select, textarea').
            serialize().
            replace(new RegExp('(^|&)[^%]+%2F', 'g'), '$1%2F');
      }

      $left.find('> .objectInputs > .inputContainer').each(function() {
        var $leftInput = $(this);
        var $rightInput = $right.find('> .objectInputs > .inputContainer[data-field="' + $leftInput.attr('data-field') + '"]');

        if ($rightInput.length === 0 || getValues($leftInput) === getValues($rightInput)) {
          $leftInput.addClass('contentDiffSame');
          $rightInput.addClass('contentDiffSame');
        }
      });

      $left.find('> .objectInputs > .inputContainer > .inputSmall > textarea').each(function() {
        var $leftText = $(this);
        var $rightText = $right.find('> .objectInputs > .inputContainer[data-field="' + $leftText.closest('.inputContainer').attr('data-field') + '"] textarea');
        
        if ($rightText.length === 0) {
          return;
        }
        
        var left = $leftText.val().replace(new RegExp('[\\r\\n]', 'g'), ' ').replace(new RegExp('<br[^>]*\/?>', 'ig'), '\n');
        var right = $rightText.val().replace(new RegExp('[\\r\\n]', 'g'), ' ').replace(new RegExp('<br[^>]*\/?>', 'ig'), '\n');
        var rich = $leftText.is('.richtext');

        if (rich) {
          function preprocess(html, enhancements) {
            return html.
                replace(new RegExp('<[^>]*class="[^"]*enhancement[^"]*"[^>]*>.*?</[^>]*>', 'ig'), function (match) {
                  var id = match.replace(/\s/g, '');
                  enhancements[id] = match;

                  return '\n\u2603' + id + '\u2603\n';
                }).
                replace(new RegExp('<([^>\\s]+)[^>]*>', 'ig'), function (match, tag) {
                  return '<' + tag + '>';
                });
          }

          var leftEnhancements = { };
          var rightEnhancements = { };

          left = preprocess(left, leftEnhancements);
          right = preprocess(right, rightEnhancements);
        }

        var diffs = JsDiff.diffWordsWithSpace(left, right);
        var $leftCopy = $('<div/>', { 'class': 'contentDiffCopy' });
        var $rightCopy = $('<div/>', { 'class': 'contentDiffCopy' });

        function postprocess(enhancements, value) {
          return  value.replace(new RegExp('\u2603(\\S+)\u2603', 'ig'), function (match, id) {
            var $wrapper = $('<div/>', { html: enhancements[id] });
            var ref = $wrapper.find('> .enhancement').attr('data-reference');
            var label;
            var preview;

            if (ref) {
              ref = JSON.parse(ref);
              label = ref.label;
              preview = ref.preview;
            }

            var $placeholder = $('<div/>', {
              'class': 'contentDiffEnhancement'
            });

            if (preview) {
              $placeholder.append($('<div/>', {
                'class': 'contentDiffEnhancement-preview',
                html: $('<img/>', {
                  src: preview
                })
              }));
            }

            if (label) {
              $placeholder.append($('<div/>', {
                'class': 'contentDiffEnhancement-label',
                text: label
              }));
            }

            return $('<div/>').html($placeholder).html();
          });
        }

        $.each(diffs, function(i, diff) {
          var value = diff.value;

          if (rich) {
            value = postprocess(leftEnhancements, value);
          }

          if (!diff.added) {
            $leftCopy.append(diff.removed ?
                $('<span/>', { 'class': 'contentDiffRemoved', 'html': value }) :
                $('<span/>', { 'html': value }));
          }
        });

        $.each(diffs, function(i, diff) {
          var value = diff.value;

          if (rich) {
            value = postprocess(rightEnhancements, value);
          }

          if (!diff.removed) {
            $rightCopy.append(diff.added ?
                $('<span/>', { 'class': 'contentDiffAdded', 'html': value }) :
                $('<span/>', { 'html': value }));
          }
        });

        $leftText.addClass('contentDiffText');
        $leftText.before($leftCopy);

        $rightText.addClass('contentDiffText');
        $rightText.before($rightCopy);
      });

      $tabs.append($tabEdit);
      $tabs.append($tabSideBySide);
      $container.prepend($tabs);
      $container.trigger($right.is('.contentDiffCurrent') ?
          'contentDiff-sideBySide' :
          'contentDiff-edit');

      $container.closest('form').submit(function() {
        if ($left.is('.contentDiffCurrent')) {
          $left.find(':input').prop('disabled', true);

        } else {
          $right.find(':input').prop('disabled', true);
        }
      });
    }
  });

  bsp_utils.onDomInsert(document, '.contentDiff > .contentDiffLeft > .objectInputs > .tabs-wrapper > .tabs > li, .contentDiff > .contentDiffRight > .objectInputs > .tabs-wrapper > .tabs > li', {
    'insert': function (item) {
      var $item = $(item);

      if ($item.closest('.objectInputs').find('> .inputContainer[data-tab="' + $item.text() + '"]:not(.contentDiffSame)').length === 0) {
        $item.attr('data-diff-same', '');
      }
    }
  });
});
