define([ 'jquery', 'bsp-utils' ], function($, bsp_utils) {
  var $window = $(window);

  // Publishing widget behaviors.
  bsp_utils.onDomInsert(document, '.widget-publishing[data-publishable]', {
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
        $publishButton.text($publishButton.attr('data-schedule-label'));

      } else {
        onChange = function() {
          if ($dateInput.val()) {
            $publishButton.addClass('schedule');
            $publishButton.text($dateInput.attr('data-schedule-label'));

          } else {
            $publishButton.removeClass('schedule');
            $publishButton.text(oldPublishText);
          }
        };

        onChange();

        $dateInput.change(onChange);
        $newSchedule.change(onChange);
      }

      // Bind command/control-S to saving a draft.
      var $draftButton = $widget.find('button[name="action-draft"]');

      if ($draftButton.length > 0 && $draftButton.closest('.widget-publishing').find('.publishing-workflow').length === 0) {
        $(document).on('keydown', function(event) {
          if (event.which === 83 && (event.ctrlKey || event.metaKey)) {
            $draftButton.click();
            event.preventDefault();
            event.stopPropagation();
          }
        });

        if (!window.DISABLE_AJAX_SAVES) {
          var saving = false;
          var toolMessageTimeout;

          $draftButton.click(function (event) {
            if (saving) {
              event.preventDefault();
              event.stopPropagation();
              return;
            }

            saving = true;

            var frameName = 'saveDraft';
            var $form = $draftButton.closest('form');
            var $frame = $('<iframe/>', {
              'name': frameName,
              'css': {
                'display': 'none'
              }
            });

            var $toolMessage = $('.toolMessage');

            if ($toolMessage.length === 0) {
              $toolMessage = $('<div/>', {
                'class': 'toolMessage'
              });

              $(document.body).append($toolMessage);
            }

            $toolMessage.html($('<div/>', {
              'class': 'message message-info',
              'text': 'Saving...'
            }));

            if (toolMessageTimeout) {
              clearTimeout(toolMessageTimeout);
            }

            $toolMessage.fadeIn('fast');

            $(document.body).append($frame);
            $form.attr('target', frameName);
            $frame.load(function () {
              var $message = $('.contentForm-main > .widget-content > .message:not(.message-info)', this.contentDocument);

              if ($message.length > 0) {
                $toolMessage.html($message.clone());

              } else {
                $toolMessage.html($('<div/>', {
                  'class': 'message message-error',
                  'text': 'Save failed with an unknown error!'
                }));
              }

              toolMessageTimeout = setTimeout(function () {
                toolMessageTimeout = null;

                $toolMessage.fadeOut('fast');
              }, 5000);

              saving = false;

              $form.find('input, textarea').each(function() {
                var $input = $(this);

                $input.prop('defaultValue', $input.val());
              });

              $form.find('option').each(function() {
                var $option = $(this);

                $option.prop('defaultSelected', $option.prop('selected'));
              });

              $form.find('.state-changed').removeClass('state-changed');
              $form.find('.toBeRemoved').remove();

              var OLD_VALUES_SELECTOR = '.contentForm > input[type="hidden"][name$="/oldValues"]';
              var $oldValuesInput = $(OLD_VALUES_SELECTOR);

              $oldValuesInput.val($(OLD_VALUES_SELECTOR, this.contentDocument).val());
              $oldValuesInput.change();

              $form.removeAttr('target');
              $frame.remove();
              $.removeData($form[0], 'bsp-publish-submitting');
            });
          });
        }
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
      $tabWorkflow.find('a').click();
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
});
