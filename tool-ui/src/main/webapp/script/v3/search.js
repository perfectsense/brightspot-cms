define([ 'jquery', 'bsp-utils' ], function ($, bsp_utils) {
    var SAVABLE_DATA = 'search-savable';

    $(document).on('click', '.searchHistory .links > li > a', function (event) {
        var $source = $(event.target).popup('source');

        if ($source) {
            $source.removeData(SAVABLE_DATA);
        }
    });

    $(document).on('submit', '.searchForm form', function (event) {
        var $form = $(event.target);

        if ($form.length > 0) {
            var $source = $form.popup('source');

            if ($source) {
                var savable = $source.data(SAVABLE_DATA);

                if (savable) {
                    $form.find('input[name="cx"], input[name="si"]').prop('disabled', false);

                } else {
                    $source.data(SAVABLE_DATA, true);
                }
            }
        }
    });

    bsp_utils.onDomInsert(document, '.searchRecent', {
        insert: function (recent) {
            var $recent = $(recent);
            var $frame = $recent.closest('.frame');

            if ($frame.attr('name') !== 'miscSearch') {
                var $select = $('<select/>', {
                    change: function () {
                        $(this).find('> :selected').data('$link').click();
                        return false;
                    }
                });

                $select.append($('<option/>', {
                    text: $recent.find('> h2').text()
                }));

                $recent.find('> ul > li > a').each(function () {
                    var $link = $(this);
                    var $option = $('<option/>', {
                        text: $link.text()
                    });

                    $option.data('$link', $link);
                    $select.append($option);
                });

                $frame.find('.searchControls').prepend($('<form/>', {
                    'class': 'searchRecentSelect',
                    html: $select
                }));
            }
        }
    });
});