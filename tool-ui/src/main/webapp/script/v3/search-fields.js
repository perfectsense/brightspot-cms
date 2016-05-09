define(['jquery', 'string'], function ($, S) {

  $(document).on('change', '.searchResultFields input[name="fieldNames"]', function() {
    var $input = $(this);
    var $li = $input.closest('li');
    var hiddenFieldsContainerClass = 'searchResultFields-hide';
    var displayFieldsContainerClass = 'searchResultFields-display';

    // locked fields cannot currently be removed
    //if (!$input.val()) {
    //  return false;
    //}

    if ($li.closest('div').hasClass(hiddenFieldsContainerClass)) {
      $('.' + displayFieldsContainerClass + ' > ul').append($li);
    } else {
      $('.' + hiddenFieldsContainerClass + ' > ul').append($li);
    }
  });

  $(document).on('input', '.searchResultFields .searchResultFields-filter', function () {
    var $input = $(this);
    var $ul = $input.siblings('ul').first();

    var re = new RegExp(S($input.val().replace(/\s/, '').split('').join('(?:.*\\W)?')).latinise().s, 'i');

    $ul.find('label').each(function () {
      var $item = $(this);

      if (re.test(S($item.attr('data-display-name')).latinise().s)) {
        $item.show();

      } else {
        $item.hide();
      }
    });
  });
});