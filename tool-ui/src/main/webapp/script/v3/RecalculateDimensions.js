define([ 'jquery' ], function ($) {
    var EVENT_TYPE = 'RecalculateDimensions';

    return {
        bind: function (elements, callback) {
            if (elements && callback) {
                $(elements).each(function () {
                    var element = this;

                    $(window).bind(EVENT_TYPE, function (event) {
                        var target = event.target;

                        if (target && (target === element || $.contains(target, element))) {
                            callback();
                        }
                    });
                });
            }
        },

        trigger: function (elements) {
            if (elements) {
                var $elements = $(elements);
                $elements.trigger(EVENT_TYPE);
                $elements.resize();
            }
        }
    };
});