define([ 'jquery' ], function($) {
  $(document).on('change', '.searchResult-checkAll', function() {
    var $checkbox = $(this);
    var checked = $checkbox.prop('checked');

    $checkbox.closest('table').find('> tbody :checkbox').each(function() {
      $(this).prop('checked', checked);
    });

    var itemIds = $checkbox.closest('table').find('> tbody :checkbox').
        map(function() {
          return 'id=' + $(this).attr('value');
        }).
        get().
        join('&');

    // Update url
    var attr = $checkbox.prop('checked') ? 'data-frame-check' : 'data-frame-uncheck';
    $checkbox.attr(attr, $checkbox.attr(attr) + '&' + itemIds);
  });

  $(document).on('click', '.searchResult-images img', function() {
    $(this).closest('figure').find(':checkbox').click();
  });
});
