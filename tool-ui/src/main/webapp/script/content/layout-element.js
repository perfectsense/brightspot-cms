define([ 'jquery', 'bsp-utils' ], function($, bsp_utils) {
  bsp_utils.onDomInsert(document, '.objectInputs', {
    'insert': function(inputs) {
      var $inputs = $(inputs);
      var placeholders = $.parseJSON($inputs.attr('data-layout-placeholders')) || [ ];
      var $fields = $inputs.find('> .inputContainer[data-layout-field]');

      if (placeholders.length === 0 && $fields.length === 0) {
        return;
      }

      var maxRight = 0;
      var maxBottom = 0;
      var tabName = null;

      function setMaxes(element) {
        var right = element.left + element.width;
        var bottom = element.top + element.height;

        if (maxRight < right) {
          maxRight = right;
        }

        if (maxBottom < bottom) {
          maxBottom = bottom;
        }
      }

      $.each(placeholders, function(i, placeholder) {
        setMaxes(placeholder);
      });

      $fields.each(function() {
        if (tabName === null && $(this).attr('data-tab')) {
            tabName = $(this).attr('data-tab');
        }
        setMaxes($.parseJSON($(this).attr('data-layout-field')));
      });

      var $firstField = $fields.eq(0);

      var $wrapper = $('<div/>', {
        'class': 'inputLayout-wrapper',
        'data-tab': tabName
      });

      var $constrain = $('<div/>', {
        'class': 'inputLayout-constrain',
        'css': {
          'padding-bottom': (maxBottom / maxRight * 100) + '%'
        }
      });

      var $container = $('<div/>', {
        'class': 'inputLayout-container'
      });

      function addElement(element, $field) {

        var hasErrors = $field && $field.data('layout-errors');

        var $label = $('<div/>', {
          'class': 'inputLayout-label' + ($field ? '' : ' inputLayout-label-placeholder') + (hasErrors ? ' inputLayout-error' : ''),
          'click': function() {
            if (!$field) {
              return;
            }

            var $label = $(this);

            $label.closest('.inputLayout-container').find('.inputLayout-label').removeClass('inputLayout-label-selected');
            $fields.toggleClass('inputLayout-hidden', true);
            $label.addClass('inputLayout-label-selected');
            $field.toggleClass('inputLayout-hidden', false);
            $field.trigger('resize');
          }
        });

        if ($field) {
          $label.text($field.find('> .inputLabel > label').text());

        } else {
          $label.text(element.name);
        }

        var dynamicText = element.dynamicText;

        if (dynamicText) {
          $label.attr('data-dynamic-text', dynamicText);
        }

        $container.append($('<div/>', {
          'class': 'inputLayout-element ' + ($field ? 'inputLayout-element-field' : 'inputLayout-element-placeholder'),
          'html': $label,
          'css': {
            'height': (element.height / maxBottom * 100) + '%',
            'left': (element.left / maxRight * 100) + '%',
            'top': (element.top / maxBottom * 100) + '%',
            'width': (element.width / maxRight * 100) + '%'
          }
        }))
      }

      $.each(placeholders, function(i, placeholder) {
        addElement(placeholder);
      });

      $fields.each(function() {
        var $field = $(this);

        addElement($.parseJSON($field.attr('data-layout-field')), $field);
      });

      $wrapper.append($constrain);
      $constrain.append($container);
      $firstField.before($wrapper);
      $fields.toggleClass('inputLayout-hidden', true);
      $container.find('.inputLayout-element-field').eq(0).find('.inputLayout-label').addClass('inputLayout-label-selected');
      $firstField.toggleClass('inputLayout-hidden', false);
      $firstField.trigger('input');
    }
  });
});

// Set filename for debugging tools to allow breakpoints even when using a cache-buster
//# sourceURL=layout-element.js
