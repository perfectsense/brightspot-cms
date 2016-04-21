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

        if (!idle) {
          $form.find('[data-dynamic-predicate]:not([data-dynamic-predicate = ""])').each(function () {
            $(this).removeClass('state-loaded');
          });
        }

        $.ajax({
          'type': 'post',
          'url': CONTEXT_PATH + 'contentState?idle=' + (!!idle) + (questionAt > -1 ? '&' + action.substring(questionAt + 1) : ''),
          'cache': false,
          'dataType': 'json',

          // If we are looking at a content update, then the current state (for viewing the diff) resides in the form
          // as well. We need to remove that from the form post or it messes up the dynamic values that return.
          'data': $form.find('[name]').not($form.find('.contentDiffCurrent [name]')).serialize() + $dynamicTexts.map(function() {
            var $element = $(this);

            return '&_dti=' + ($element.closest('[data-object-id]').attr('data-object-id') || '') +
                '&_dtt=' + (($element.attr('data-dynamic-text') ||
                $element.attr('data-dynamic-html') ||
                $element.attr('data-dynamic-placeholder') ||
                '')) +
                  '&_dtf=' + ($element.attr('data-dynamic-field-name') || '') +
                  '&_dtq=' + ($element.attr('data-dynamic-predicate') || '');
          }).get().join(''),

          'success': function(data) {
            $form.trigger('cms-updateContentState', [ data ]);

            $dynamicTexts.each(function(index) {
              var $element = $(this),
                  text = data._dynamicTexts[index],
                  dynamicPredicate = data._dynamicPredicates[index];

              if ($element.is('[data-dynamic-predicate]')) {

                $element.attr('data-additional-query', dynamicPredicate);
                $element.trigger('refresh.objectId');
                $element.addClass('state-loaded');
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
                }
              }

              $element.attr('data-previous-text', text);
            });

            $form.resize();
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
