define([ 'jquery', 'bsp-utils', 'diff' ], function($, bsp_utils, JsDiff) {
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
      });

      $container.bind('contentDiff-sideBySide', function() {
        $container.removeClass('contentDiff-edit').addClass('contentDiff-sideBySide');
        $tabs.find('li').removeClass('state-selected');
        $tabSideBySide.addClass('state-selected');
        $container.resize();
      });

      var equalizeHeights = $.throttle(100, function() {
        $container.find('.contentDiffLeft .inputContainer').each(function() {
          var $leftInput = $(this);
          var $rightInput = $container.find('.contentDiffRight .inputContainer[data-name="' + $leftInput.attr('data-name') + '"]');
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

        if (getValues($leftInput) === getValues($rightInput)) {
          $leftInput.addClass('contentDiffSame');
          $rightInput.addClass('contentDiffSame');
        }
      });

      $left.find('> .objectInputs > .inputContainer > .inputSmall > textarea').each(function() {
        var $leftText = $(this);
        var $rightText = $right.find('> .objectInputs > .inputContainer[data-field="' + $leftText.closest('.inputContainer').attr('data-field') + '"] textarea');
        var left = $leftText.val().replace(new RegExp('[\\r\\n]', 'g'), ' ').replace(new RegExp('<br[^>]*\/?>', 'ig'), '\n');
        var right = $rightText.val().replace(new RegExp('[\\r\\n]', 'g'), ' ').replace(new RegExp('<br[^>]*\/?>', 'ig'), '\n');
        var diffs = JsDiff.diffWords(left, right);
        var $leftCopy = $('<div/>', { 'class': 'contentDiffCopy' });
        var $rightCopy = $('<div/>', { 'class': 'contentDiffCopy' });

        $.each(diffs, function(i, diff) {
          if (!diff.added) {
            $leftCopy.append(diff.removed ?
                $('<span/>', { 'class': 'contentDiffRemoved', 'text': diff.value }) :
                $('<span/>', { 'text': diff.value }));
          }
        });

        $.each(diffs, function(i, diff) {
          if (!diff.removed) {
            $rightCopy.append(diff.added ?
                $('<span/>', { 'class': 'contentDiffAdded', 'text': diff.value }) :
                $('<span/>', { 'text': diff.value }));
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
});
