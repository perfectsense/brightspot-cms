define([ 'jquery', 'bsp-utils' ], function($, bsp_utils) {
  bsp_utils.onDomInsert(document, '.message', {
    'insert': function(message) {
      var $message = $(message);

      if ($message.text() === '' &&
          $message.find('[data-dynamic-html], [data-dynamic-text]').length > 0) {
        $message.hide();
      }
    }
  });

  bsp_utils.onDomInsert(document, '.contentForm, .enhancementForm, .standardForm', {
    'insert': function(form) {
      var $form = $(form);
      var wipEnabled = $form.is('.contentForm');
      var running;
      var rerun;
      var idleTimeout;
      var idle = true;

      function update() {
        if ($form.find(
                '.repeatableForm:not(.plugin-repeatable),' +
                '.repeatableInputs:not(.plugin-repeatable),' +
                '.repeatableLayout:not(.plugin-repeatable),' +
                '.repeatableObjectId:not(.plugin-repeatable),' +
                '.repeatableText:not(.plugin-repeatable)').
                length > 0) {

          setTimeout(update, 100);
          return;
        }

        if (running) {
          rerun = true;
          return;

        } else {
          running = true;
        }

        var action = $form.attr('action');
        var questionAt = action.indexOf('?');
        var end = +new Date() + 1000;
        var $dynamicTexts = $form.find(
            '[data-dynamic-text]:not([data-dynamic-text=""]),' +
            '[data-dynamic-html]:not([data-dynamic-html=""]),' +
            '[data-dynamic-placeholder]:not([data-dynamic-placeholder=""]),' +
            '[data-dynamic-predicate]:not([data-dynamic-predicate=""])');

        $dynamicTexts = $dynamicTexts.filter(function() {
          return $(this).closest('.collapsed').length === 0
              && $(this).closest('.contentDiffCurrent').length === 0;
        });

        var fieldNames = { };

        $form.find('.objectInputs').each(function () {
          var $inputs = $(this);

          fieldNames[$inputs.attr('data-object-id')] = $.makeArray($inputs.find('> .inputContainer').map(function () {
            return $(this).attr('data-field-name');
          }));
        });

        var $publishingHeading = $form.find('.widget-publishing > h1');

        $.ajax({
          'type': 'post',
          'url': CONTEXT_PATH + 'contentState?wip=' + wipEnabled + '&idle=' + (!!idle) + (questionAt > -1 ? '&' + action.substring(questionAt + 1) : ''),
          'cache': false,
          'dataType': 'json',

          // If we are looking at a content update, then the current state (for viewing the diff) resides in the form
          // as well. We need to remove that from the form post or it messes up the dynamic values that return.
          'data': $form.find('[name]').not($form.find('.contentDiffCurrent [name]')).serialize() + '&_fns=' + encodeURIComponent(JSON.stringify(fieldNames)) + $dynamicTexts.map(function() {
            var $element = $(this);

            return '&_fns=' + encodeURIComponent(JSON.stringify(fieldNames)) +
                '&_dti=' + encodeURIComponent($element.closest('[data-object-id]').attr('data-object-id') || '') +
                '&_dtt=' + encodeURIComponent(($element.attr('data-dynamic-text') ||
                $element.attr('data-dynamic-html') ||
                $element.attr('data-dynamic-placeholder') ||
                '')) +
                  '&_dtf=' + encodeURIComponent($element.attr('data-dynamic-field-name') || '') +
                  '&_dtq=' + encodeURIComponent($element.attr('data-dynamic-predicate') || '');
          }).get().join(''),

          'success': function(data) {
            if (!data) {
              return;
            }

            if (wipEnabled) {
              var wipMessage = data._wip;

              if (wipMessage) {
                var $wipSaveStatus = $publishingHeading.find('> .WorkInProgressSaveStatus');

                if ($wipSaveStatus.length === 0) {
                  $wipSaveStatus = $('<span/>', {
                    'class': 'WorkInProgressSaveStatus'
                  });

                  $publishingHeading.append($wipSaveStatus);
                }

                $wipSaveStatus.removeAttr('data-status');
                $wipSaveStatus.text(wipMessage);

                setTimeout(function () {
                  $wipSaveStatus.attr('data-status', 'saved');
                }, 0)
              }
            }

            $form.trigger('cms-updateContentState', [ data ]);

            $dynamicTexts.each(function(index) {
              var $element = $(this),
                  text = data._dynamicTexts[index],
                  dynamicPredicate = data._dynamicPredicates[index];

              if ($element.attr('data-placeholder-clear-on-change') === 'true') {
                var oldClearKey = $element.attr('data-old-clear-key');
                var newClearKey = text || 'null';

                if (!oldClearKey) {
                  $element.attr('data-old-clear-key', newClearKey)

                } else if (oldClearKey !== newClearKey) {
                  $element.attr('data-old-clear-key', newClearKey);

                  if ($element.is('input, select, textarea')) {
                    var rte = $element.data('rte2');

                    if (rte) {
                      rte.fromHTML('');
                    }

                    $element.val('');
                    $element.change();
                  }
                }
              }

              if ($element.is('[data-dynamic-predicate]')) {

                $element.attr('data-additional-query', dynamicPredicate);
                $element.trigger('refresh.objectId');
              }

              //If text is missing set to empty to compare & replace below
              if (text === null) {
                text = '';
              }

              $element.closest('.message').toggle(text !== '');

              var $previousText = $element.attr('data-previous-text');

              //Replace only when text has changed
              if ($previousText === null || $previousText !== text) {
                if ($element.is('[data-dynamic-text]')) {
                    $element.text(text);

                } else if ($element.is('[data-dynamic-html]')) {
                    $element.html(text);

                } else if ($element.is('[data-dynamic-placeholder]')) {
                    
                    $element.prop('placeholder', text);
                    
                    // Trigger a placeholderUpdate event so other code can listen for placeholder changes
                    // (used by the rich text editor).
                    // Note: originally called this event 'placeholder' but that caused a jquery error for some reason,
                    // so using placeholderUpdate instead.
                    $element.trigger('placeholderUpdate');
                }
              }

              $element.attr('data-previous-text', text);
            });

            // Highlight fields that have changed.
            var diffs = data._differences;

            if (diffs) {
              $form.find('.inputContainer').removeClass('state-changed');
              $form.find('.repeatableForm > ol > li, .repeatableForm > ul > li').removeClass('state-changed');

              $.each(diffs, function (id, fields) {
                if (fields.length === 0) {
                  return;
                }
                
                var $inputs = $form.find('.objectInputs[data-object-id="' + id + '"]');

                $.each(fields, function (name) {
                  $inputs.find('> .inputContainer[data-field-name="' + name + '"]').addClass('state-changed');
                });
                
                var $li = $inputs.closest('li');

                if ($li.parent().parent().is('.repeatableForm')) {
                  $li.addClass('state-changed');
                }
              });

              $form.trigger('content-state-differences');
            }

            $form.resize();
          },

          'error': function (xhr) {
            if (xhr.readyState === 0) {
              rerun = true;
            }
          },

          'complete': function() {
            if (rerun) {
              setTimeout(function() {
                running = false;
                rerun = false;
                idle = false;
                update();
              }, 1000);

            } else {
              running = false;
              idle = false;
            }
          }
        });
      }

      $form.bind('create change input', function() {
        update();

        clearTimeout(idleTimeout);

        idleTimeout = setTimeout(function() {
          idle = true;
          update();
        }, 5000);
      });

      update();
    }
  });
});
