define([
  'jquery',
  'jquery.extra',
  'v3/plugin/popup' ],

function($) {
  var refresh,
      SHADOW_DATA = 'objectId-shadow',
      TARGET_DATA = 'objectId-target',
      targetIndex = 0;

  refresh = function($inputs) {
    $inputs.each(function() {
      var $input = $(this),
          shadow = $.data(this, SHADOW_DATA),
          $select,
          $edit,
          $clear,
          preview,
          visibility,
          label,
          dynamicPlaceholderText,
          dynamicFieldName,
          placeholder,
          value,
          selectHref,
          aqIndex,
          aqParam;

      if (!shadow) {
        return;
      }

      $select = shadow.$select;
      $edit = shadow.$edit;
      $clear = shadow.$clear;
      preview = $input.attr('data-preview');
      label = $input.attr('data-label');
      visibility = $input.attr('data-visibility');
      value = $input.val();

      if (preview) {
        var $caption = $('<figcaption>', {
          'text': label
        });

        $select.html($('<figure/>', {
          'html': [
            $('<img/>', {
              'src': preview
            }),
            $caption
          ]
        }));

        if (visibility) {
          $caption.prepend(' ');
          $caption.prepend($('<span/>', {
            'class': 'visibilityLabel',
            'text': visibility
          }));
        }

      } else {
        if (label) {
          $select.text(label);

          if (visibility) {
            $select.prepend(' ');
            $select.prepend($('<span/>', {
              'class': 'visibilityLabel',
              'text': visibility
            }));
          }

        } else {
          dynamicPlaceholderText = $input.attr('data-dynamic-placeholder');
          dynamicFieldName = $input.attr('data-dynamic-field-name');
          placeholder = $input.attr('placeholder');

          if (dynamicPlaceholderText) {
            $select.html($('<span/>', {
              'type': 'text',
              'class': 'objectId-placeholder',
              'data-dynamic-text': dynamicPlaceholderText,
              'data-dynamic-field-name': dynamicFieldName
            }));
          } else if (placeholder) {
            $select.html($('<span/>', {
              'class': 'objectId-placeholder',
              'text': placeholder
            }));

          } else {
            $select.text('\u00a0');
          }
        }
      }

      // update additional query parameter in select href
      selectHref = $select.attr('href');
      aqIndex = selectHref.indexOf('aq');
      aqParam = 'aq=' + encodeURIComponent($input.attr('data-additional-query') || '');

      if (aqIndex === -1) {
          selectHref += '&' + aqParam;
      } else {
          selectHref = selectHref.substr(0, aqIndex) + aqParam + selectHref.substr(selectHref.indexOf('&', aqIndex));
      }
      $select.attr('href', selectHref);

      $edit.toggle(
          $input.attr('data-editable') !== 'false' &&
          !!value);

      $clear.toggle(
          $clear.is('.restore') ||
          ($input.attr('data-clearable') !== 'false' &&
          $input.closest('.repeatableObjectId').length === 0 &&
          !!value && !$clear.hasClass('state-disabled')));

      $edit.attr('href', CONTEXT_PATH + 'content/edit.jsp' +
          '?id=' + (value || '') +
          '&' + (((/[&?](variationId=[^&]+)/).exec(window.location.search) || [ ])[1] || ''));
    });
  };

  $.plugin2('objectId', {
    '_init': function(selector) {
      this.$caller.on('change.objectId', selector, function() {
        refresh($(this));
      });

      this.$caller.on('refresh.objectId', selector, function() {
        refresh($(this));
      });
    },

    '_create': function(input) {
      var $input,
          $form,
          target,
          typeIds,
          formAction,
          searcherPath,
          $select,
          $edit,
          $clear,
          shadow;

      if ($.data(input, SHADOW_DATA)) {
        return;
      }

      $input = $(input);
      $form = input.form ? $(input.form) : $input.closest('form');

      // Make sure that there's only one frame target per form.
      target = $.data($form[0], TARGET_DATA);

      if (!target) {
        ++ targetIndex;
        target = 'objectId-' + targetIndex;
        $.data($form[0], TARGET_DATA, target);
      }

      var genericArgumentIndex = $input.attr('data-generic-argument-index');
      var genericArguments = $input.closest('[data-generic-arguments]').attr('data-generic-arguments');

      if (genericArgumentIndex && genericArguments) {
        typeIds = genericArguments.split(',')[genericArgumentIndex];
      }

      if (!typeIds) {
        typeIds = $input.attr('data-typeIds');
      }

      formAction = $form.attr('action');
      searcherPath = $input.attr('data-searcher-path') || (CONTEXT_PATH + 'content/objectId.jsp');

      $select = $('<a/>', {
        'class': 'objectId-select',
        'target': target,
        'click': function() { return !$(this).is('.state-disabled'); },
        'href': searcherPath +
            (searcherPath.indexOf('?') > -1 ? '&' : '?') + 'pt=' + encodeURIComponent((/id=([^&]+)/.exec(formAction) || [ ])[1] || '') +
            '&py=' + encodeURIComponent((/typeId=([^&]+)/.exec(formAction) || [ ])[1] || '') +
            '&p=' + encodeURIComponent($input.attr('data-pathed')) +
            '&' + (typeIds ? $.map(typeIds.split(','), function(typeId) { return 'rt=' + typeId; }).join('&') : '') +
            '&aq=' + encodeURIComponent($input.attr('data-additional-query') || '') +
            '&sg=' + encodeURIComponent($input.attr('data-suggestions') || '')
      });

      $edit = $('<a/>', {
        'class': 'objectId-edit',
        'target': target,
        'text': 'Edit'
      });

      $clear = $('<a/>', {
        'class': 'objectId-clear',
        'text': 'Clear',
        'click': function() {
          var $clear = $(this),
              shadow = $.data($clear[0], SHADOW_DATA),
              $select = shadow.$select,
              $edit = shadow.$edit;

          if ($input.val()) {
            if ($input.attr('data-restorable') === 'false') {
              $input.removeAttr('data-label');
              $input.removeAttr('data-preview');

            } else {
              $select.addClass('toBeRemoved');
              $edit.addClass('toBeRemoved');
              $clear.addClass('restore');
              $clear.text('Restore');
            }

            $input.val('');

          } else {
            $select.removeClass('toBeRemoved');
            $edit.removeClass('toBeRemoved');
            $clear.removeClass('restore');
            $clear.text('Clear');
            $input.val($input[0].defaultValue);
          }

          $input.change();
          return false;
        }
      });

      shadow = {
        '$input': $input,
        '$select': $select,
        '$edit': $edit,
        '$clear': $clear
      };

      $.data(input, SHADOW_DATA, shadow);
      $.data($select[0], SHADOW_DATA, shadow);
      $.data($edit[0], SHADOW_DATA, shadow);
      $.data($clear[0], SHADOW_DATA, shadow);
    },

    '_createAll': function(target, selector) {
      var $inputs = $(target).find(selector);

      $inputs.each(function() {
        var $input = $(this),
            shadow = $.data(this, SHADOW_DATA);

        $input.hide();
        $input.after(shadow.$clear);
        $input.after(shadow.$edit);
        $input.after(shadow.$select);
      });

      $inputs.bind('input-disable', function(event, disable) {
        var $input = $(this),
            shadow = $.data(this, SHADOW_DATA);

        if (shadow) {
          shadow.$clear.toggleClass('state-disabled', disable);
          shadow.$select.toggleClass('state-disabled', disable);
        }
      });

      refresh($inputs);
    }
  });

  $(document).onCreate('[data-o-id]', function() {
    var $element = $(this),
        $source = $element.popup('source'),
        $input;

    if ($source &&
        ($source.is('.objectId-select') ||
        $source.is('.objectId-edit'))) {
      $input = $source.parent().find(':input.objectId');

      $input.attr('data-label', $element.attr('data-o-label'));
      $input.attr('data-preview', $element.attr('data-o-preview'));
      $input.val($element.attr('data-o-id'));
      $input.change();
    }
  });
});
