define([ 'jquery', 'bsp-utils' ], function($, bsp_utils) {

  // Make sure that there's at least one defaultSelected option.
  bsp_utils.onDomInsert(document, 'select:not([multiple])', {
    'insert': function(select) {
      var $options = $(select).find('option');

      if ($options.length > 0) {
        var $select = $(select);
        var hasSelected = false;

        $select.find('option').each(function() {
          if (this.defaultSelected) {
            hasSelected = true;
            return false;
          }
        });

        if (!hasSelected) {
          var selectValue = $select.val();

          $options.eq(0).prop('defaultSelected', true);
          $select.val(selectValue);
        }
      }
    }
  });

  // Mark changed inputs.
  $(document).on('change', '.inputContainer', function() {
    var $container = $(this);
    var changed = false;
      
    $container.find('input[name], textarea[name]').each(function() {
      if (this.defaultValue !== this.value) {
        changed = true;
        return false;
      }
    });

    if (!changed) {
      $container.find('option').each(function() {
        if (this.defaultSelected !== this.selected) {
          changed = true;
          return false;
        }
      });
    }

    $container.toggleClass('state-changed', changed);
  });
});
