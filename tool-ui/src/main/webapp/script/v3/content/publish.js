define([ 'jquery', 'bsp-utils' ], function($, bsp_utils) {
  var $window = $(window);

  // Publishing widget behaviors.
  bsp_utils.onDomInsert(document, '.widget-publishing', {
    'insert': function(widget) {
      var $widget = $(widget);
      var $dateInput = $widget.find('.dateInput');
      var $newSchedule = $widget.find('select[name="newSchedule"]');
      var $publishButton = $widget.find('[name="action-publish"]');
      var oldPublishText = $publishButton.text();
      var oldDate = $dateInput.val();
      var onChange;

      // Change the publish button label if scheduling.
      if ($dateInput.length === 0) {
        $publishButton.addClass('schedule');
        $publishButton.text('Schedule');

      } else {
        onChange = function() {
          if ($dateInput.val()) {
            $publishButton.addClass('schedule');
            $publishButton.text(oldDate && !$newSchedule.val() ? 'Reschedule' : 'Schedule');

          } else {
            $publishButton.removeClass('schedule');
            $publishButton.text(oldPublishText);
          }
        };

        onChange();

        $dateInput.change(onChange);
        $newSchedule.change(onChange);
      }
    }
  });

  // Create tabs if the publishing widget contains both the workflow
  // and the publish areas.
  bsp_utils.onDomInsert(document, '.widget-publishing', {
    'insert': function(widget) {
      var $widget = $(widget);
      var $workflow = $widget.find('.widget-publishingWorkflow');
      var $publish = $widget.find('.widget-publishingPublish');
      var $tabs;
      var $tabWorkflow;
      var $tabPublish;

      if ($workflow.length === 0 || $publish.length === 0) {
        return;
      }

      $tabs = $('<ul/>', {
        'class': 'tabs'
      });

      $tabWorkflow = $('<li/>', {
        'html': $('<a/>', {
          'text': 'Workflow',
          'click': function() {
            $workflow.show();
            $tabWorkflow.addClass('state-selected');
            $publish.hide();
            $tabPublish.removeClass('state-selected');
            $window.resize();
            return false;
          }
        })
      });

      $tabPublish = $('<li/>', {
        'html': $('<a/>', {
          'text': 'Publish',
          'click': function() {
            $workflow.hide();
            $tabWorkflow.removeClass('state-selected');
            $publish.show();
            $tabPublish.addClass('state-selected');
            $window.resize();
            return false;
          }
        })
      });

      $tabs.append($tabWorkflow);
      $tabs.append($tabPublish);
      $workflow.before($tabs);

      if ($('.widget-publishingWorkflowState').length > 0) {
        $tabWorkflow.find('a').click();

      } else {
        $tabPublish.find('a').click();
      }
    }
  });

  (function() {
    var SUBMITTING_DATA = 'bsp-publish-submitting';

    $(document).on('submit', '.contentForm', function(event) {
      $.data($(event.target)[0], SUBMITTING_DATA, true);
    });

    $(document).on('click', '.widget-publishing button, .widget-publishing :submit', function(event) {
      return !$.data($(event.target).closest('.contentForm')[0], SUBMITTING_DATA);
    });
  })();

  // Keep the publishing widget in view at all times.
  (function() {
    var OFFSET_DATA_KEY = 'cp-offset';
    var HEIGHT_DATA_KEY = 'cp-height';
    var WIDTH_DATA_KEY = 'cp-width';

    // Update the various element sizing information to be used later.
    var toolHeaderHeight;

    function updateSizes() {
      toolHeaderHeight = $('.toolHeader').outerHeight(true);

      $('.contentForm-aside').each(function() {
        var aside = this;
        var $aside = $(aside);
        var asideTop = $aside.css('top');

        $aside.css('top', '');
        $.data(aside, OFFSET_DATA_KEY, $aside.offset());
        $aside.css('top', asideTop);

        var asideWidth = $aside.width();
        var $widget = $aside.find('.widget-publishing');
        var widget = $widget[0];

        $.data(widget, HEIGHT_DATA_KEY, $widget.outerHeight(true));
        $.data(widget, WIDTH_DATA_KEY, $widget.css('box-sizing') === 'border-box' ?
            asideWidth :
            asideWidth - ($widget.outerWidth() - $widget.width()));
      });
    }

    // Keep the publishing widget fixed at the top below the tool header,
    // and move down all the elements below.
    function moveElements() {
      var windowScrollTop = $window.scrollTop();

      $('.contentForm-aside').each(function() {
        var aside = this;
        var $aside = $(aside);
        var asideOffset = $.data(aside, OFFSET_DATA_KEY);
        var $widgets = $aside.find('> .contentWidgets');
        var $publishing = $aside.find('> .widget-publishing');
        var publishing = $publishing[0];

        if (asideOffset.top - windowScrollTop <= toolHeaderHeight) {
          $widgets.css({
            'padding-top': $.data(publishing, HEIGHT_DATA_KEY)
          });

          $publishing.css({
            'left': asideOffset.left,
            'position': 'fixed',
            'top': toolHeaderHeight,
            'width': $.data(publishing, WIDTH_DATA_KEY),
            'z-index': 1
          });

          // Hide the right rail widgets when under the publishing widget.
          var asideTop = $aside.closest('.popup').length > 0 ? parseInt($aside.css('top'), 10) || 0 : 0;
          var clipPathTop = (windowScrollTop - asideOffset.top - asideTop + toolHeaderHeight + 20) + 'px';
          var clipPath = 'inset(' + clipPathTop + ' 0 0 0)';

          $widgets.css({
            '-webkit-clip-path': clipPath,
            'clip-path': clipPath
          });

        } else {
          $widgets.css({
            '-webkit-clip-path': '',
            'clip-path': '',
            'padding-top': ''
          });

          $publishing.css({
            'left': '',
            'position': '',
            'top': '',
            'width': '',
            'z-index': ''
          });
        }
      });
    }

    // Execute on resizes and scrolls.
    updateSizes();
    moveElements();

    $window.resize(bsp_utils.throttle(50, function() {
      updateSizes();
      moveElements();
    }));

    $window.scroll(moveElements);
  })();
});
