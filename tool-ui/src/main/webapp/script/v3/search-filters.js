define([ 'jquery' ], function ($) {

  // Disables search filter inputs if missing is checked.
  $(document).on('change', '.searchFilter > :checkbox[name$=".m"]', function () {
    var $checkbox = $(this);

    $checkbox.closest('.searchFilter').toggleClass('searchFilter-missing', $checkbox.prop('checked'));
  });

  // Allows multiple object IDs to be selected.
  $(document).on('change', '.searchFilterItem > .objectId', function () {
    var $input = $(this);
    var $item = $input.closest('.searchFilterItem');
    var $filter = $item.closest('.searchFilter');

    if ($input.val()) {
      if ($filter.find('> .searchFilterItem').eq(-1).find('> .objectId').val()) {
        var $clone = $item.clone();
        var $cloneInput = $clone.find('.objectId');

        $clone.find('.objectId-select, .objectId-edit, .objectId-clear').remove();
        $cloneInput.removeClass('plugin-objectId');
        $cloneInput.removeAttr('data-label');
        $cloneInput.val('');
        $item.after($clone);
        $clone.trigger('create');
      }

    } else {
      $item.remove();
    }

    $filter.toggleClass(
        'searchFilter-multiple',
        $filter.find('> .searchFilterItem').length > 1);
  });
});