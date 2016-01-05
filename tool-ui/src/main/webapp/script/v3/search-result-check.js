define([ 'jquery' ], function($) {
  $(document).on('change', '.searchResult-checkAll', function() {
    var $checkbox = $(this);
    var checked = $checkbox.prop('checked');

    $checkbox.closest('table').find('> tbody :checkbox').each(function() {
      var $c = $(this);

      $c.prop('checked', checked);
      $c.change();
    });
  });

  $(document).on('click', '.searchResult-images img', function() {
    $(this).closest('figure').find(':checkbox').click();
  });
});
