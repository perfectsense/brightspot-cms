// Toggle display of other areas.
(function($, window, undefined) {

$.plugin2('toggleable', {
    '_init': function(selector) {
        var plugin = this;

        plugin.$caller.delegate(selector, 'toggle.toggleable change', function() {
            var $select = $(this),
                    rootSelector = $select.attr('data-root'),
                    $root,
                    $option = $select.find(':selected');

            if (rootSelector) {
                $root = $select.closest(rootSelector);
            }

            if (!$root || $root.length === 0) {
                $root = $(window.document.body);
            }

            plugin._toggle($root, $option.attr('data-hide'), true);
            plugin._toggle($root, $option.attr('data-show'), false);
        });
    },

    '_toggle': function($root, selector, disable) {
        var $matching,
                $inputs;

        if (selector) {
            $matching = $root.find(selector);

            if (!disable) {
                $matching.find('[data-form-fields-url]').each(function() {
                    var $div = $(this);
                    var url = $div.attr('data-form-fields-url');
                    var data = $div.attr('data-form-fields-data');

                    $div.removeAttr('data-form-fields-url');
                    $div.removeAttr('data-form-fields-data');

                    $.ajax({
                        'type': 'POST',
                        'cache': false,
                        'url': url,
                        'data': { 'data': data },
                        'complete': function(response) {
                            $div.append(response.responseText);
                            $div.trigger('create');
                            $div.trigger('load');
                            $div.resize();
                        }
                    });
                })
            }

            $inputs = $matching.find(':input');

            if ($matching.is(':input')) {
                $inputs = $inputs.add($matching);
            }

            $matching.toggle(!disable);
            $inputs.prop('disabled', disable);

            if (!disable) {
                $matching.rte('enable');
            }
        }
    },

    '_create': function(element) {
        $(element).trigger('toggle');
    }
});

}(jQuery, window));
