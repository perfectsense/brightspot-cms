(function($, win, undef) {

var $win = $(win),
        doc = win.document;

// Standard plugin structure.
$.plugin2 = function(name, methods) {
    var CLASS_NAME= 'plugin-' + name,
            OPTIONS_DATA_KEY = name + '-options';

    methods._mergeOptions = function(options) {
        return $.extend(true, { }, this._defaultOptions, options);
    };

    methods.closestInit = function() {
        var $init = this.$caller.closest('.' + CLASS_NAME);
        return $init.length > 0 ? $init : $(doc);
    };

    methods.option = function(key, value) {
        var $init = this.closestInit(),
                first;

        if (typeof key === 'undefined') {
            first = $init[0];
            return first ? $.data(first, OPTIONS_DATA_KEY) : null;

        } else if (typeof value === 'undefined') {
            first = $init[0];
            return first ? $.data(first, OPTIONS_DATA_KEY)[key] : null;

        } else {
            $init.each(function() {
                $.data(this, OPTIONS_DATA_KEY)[key] = value;
            });
            return this.$caller;
        }
    };

    methods.live = function(selector, options) {
        var plugin = this,
                $caller = plugin.$caller;

        options = plugin._mergeOptions(options);

        if (plugin._init) {
            plugin._init(selector, options);
        }

        $caller.onCreate(selector, function() {
            var $element,
                    elementPlugin;

            if (!$.data(this, OPTIONS_DATA_KEY)) {
                $element = $(this);
                elementPlugin = $.extend({ }, plugin, { '$caller': $element });

                $.data(this, OPTIONS_DATA_KEY, options);
                $element.addClass(CLASS_NAME);

                if (elementPlugin._create) {
                    elementPlugin._create(this, options);
                }
            }
        });

        if (plugin._createAll) {
            $caller.bind('create', function(event) {
                plugin._createAll(event.target, selector, options);
            });
        }

        return $caller;
    };

    methods.init = function(options) {
        return this.live(null, options);
    };

    $.fn[name] = function(method) {
        var plugin = $.extend({ }, methods, { '$caller': this });

        if (!method || typeof method === 'object') {
            return plugin.init(method);
        }

        method = '' + method;

        if (method.substr(0, 1) !== '_' && plugin[method]) {
            return plugin[method].apply(plugin, Array.prototype.slice.call(arguments, 1));

        } else {
            return $.error('[' + method + '] method doesn\'t exist on [' + name + '] plugin!');
        }
    };
};

// Runs the function and returns it instead of the result.
$.run = function(runFunction) {
    runFunction();
    return runFunction;
};

// $.trottle(delay, [ no_trailing, ] callback)
// $.debounce( delay, [ at_begin, ] callback );
/*!
 * jQuery throttle / debounce - v1.1 - 3/7/2010
 * http://benalman.com/projects/jquery-throttle-debounce-plugin/
 * 
 * Copyright (c) 2010 "Cowboy" Ben Alman
 * Dual licensed under the MIT and GPL licenses.
 * http://benalman.com/about/license/
 */
(function(b,c){var $=b.jQuery||b.Cowboy||(b.Cowboy={}),a;$.throttle=a=function(e,f,j,i){var h,d=0;if(typeof f!=="boolean"){i=j;j=f;f=c}function g(){var o=this,m=+new Date()-d,n=arguments;function l(){d=+new Date();j.apply(o,n)}function k(){h=c}if(i&&!h){l()}h&&clearTimeout(h);if(i===c&&m>e){l()}else{if(f!==true){h=setTimeout(i?k:l,i===c?e-m:e)}}}if($.guid){g.guid=j.guid=j.guid||$.guid++}return g};$.debounce=function(d,e,f){return f===c?a(d,e,false):a(d,f,e!==false)}})(this);
    
// Handles mouse dragging movements.
(function() {
    var $dragCover,
            endDrag,
            $dragElement,
            startDrag,
            dragStartTimeout,
            startPageX,
            startPageY,
            lastPageX,
            lastPageY,
            dragMoveCallback,
            dragEndCallback;

    $dragCover = $('<div/>', {
        'css': {
            'height': '100%',
            'left': 0,
            'position': 'fixed',
            'top': 0,
            'user-select': 'none',
            'width': '100%',
            'z-index': 1000000
        }
    });

    endDrag = function(event) {
        if (dragStartTimeout) {
            clearTimeout(dragStartTimeout);
            dragStartTimeout = null;
        }

        dragMoveCallback = null;

        $(doc.body).css('user-select', '');

        if ($dragElement) {
            $dragElement.unbind('.drag');
            $dragElement = null;
        }

        if (dragEndCallback) {
            dragEndCallback(event);
            dragEndCallback = null;
        }
    };

    $.drag = function(element, event, startCallback, moveCallback, endCallback) {
        var data;

        // Skip unless left click.
        if (event.which !== 1 ||
                event.altKey ||
                event.ctrlKey ||
                event.metaKey ||
                event.shiftKey) {
            return;
        }

        data = {
            '$dragCover': $dragCover
        };

        // Reset in case we're in a bad state.
        endDrag(event);

        // Suppress native browser drag behaviors.
        $(doc.body).css('user-select', 'none');

        $dragElement = $(element);
        $dragElement.bind('dragstart.drag', function() {
            return false;
        });

        startPageX = lastPageX = event.pageX;
        startPageY = lastPageY = event.pageY;

        startDrag = function() {
            var drag = function() {
                dragMoveCallback = function(event) {
                    return moveCallback.call(element, event, data);
                };

                dragEndCallback = function(event) {
                    return endCallback.call(element, event, data);
                };

                $(doc.body).append($dragCover);
                startCallback.call(element, event, data);
            };

            if (event.dragImmediately) {
                drag();

            } else {
                dragStartTimeout = setTimeout(function() {
                    var deltaX = lastPageX - startPageX,
                            deltaY = lastPageY - startPageY;

                    if (Math.sqrt(deltaX * deltaX + deltaY * deltaY) < (event.dragDistance || 5)) {
                        startDrag();
                        return;
                    }

                    drag();
                }, (event.dragDelay || 100));
            }
        };

        startDrag();
    };

    $win.mousemove($.throttle(50, function(event) {
        if (dragStartTimeout) {
            lastPageX = event.pageX;
            lastPageY = event.pageY;
        }

        if (dragMoveCallback) {
            dragMoveCallback(event);
        }
    }));

    $win.mouseup(function(event) {
        endDrag(event);
        $dragCover.remove();
    });
}());

// In some cases we need to handle nested elements and
// need to access the children before the parents
// (for example, nested drag-and-drop targets)    
$.fn.sortDepthFirst = function() {
    var ar = this.map(function() {
            return {length: $(this).parents().length, elt: this}
        }).get(),
        result = [],
        i = ar.length;


    ar.sort(function(a, b) {
        return a.length - b.length;
    });

    while (i--) {
        result.push(ar[i].elt);
    }
    return $(result);
};

// Returns true if the element should be in fixed CSS position.
$.fn.isFixedPosition = function() {
    var $parent = this,
            $newParent;

    while (true) {
        if (!$parent.hasClass('is_stuck') && $parent.css('position') === 'fixed') {
            return true;
        }

        $newParent = $parent.offsetParent();

        if ($parent.length === 0 || $parent[0] === $newParent[0]) {
            break;

        } else {
            $parent = $newParent;
        }
    }

    return false;
};

// Returns an accurate CSS z-index, taking all the parents into account.
$.fn.zIndex = function() {
    var zIndex;
    for (var $parent = this; $parent.length > 0; $parent = $parent.parent()) {
        try {
            zIndex = parseInt($parent.css('z-index'), 10);
        } catch (error) {
            break;
        }
        if (!isNaN(zIndex) && zIndex !== 0) {
            break;
        }
    }
    return zIndex;
};

// Patch $.fn.delegate to call $.fn.bind if called without a selector.
(function() {
    var oldDelegate = $.fn.delegate;

    $.fn.delegate = function() {
        if (arguments[0]) {
            return oldDelegate.apply(this, arguments);
        } else {
            return $.fn.bind.apply(this, Array.prototype.slice.call(arguments, 1));
        }
    };
}());

// Polyfill for HTML5 input event.
(function() {
    var CHECK_INTERVAL_DATA = 'input-checkInterval';

    if (!('oninput' in doc.createElement('input'))) {
        $.event.special.input = {
            'add': function(handleObject) {
                var $root = $(this),
                        selector = handleObject.selector,
                        handler,
                        clearCheckInterval;

                // Don't trigger the handler too often.
                handler = $.throttle(50, function() {
                    $(this).trigger('input');
                });

                // The keyup event is pretty close to the input event...
                $root.delegate(selector, 'keyup', function() {
                    setTimeout(handler, 0);
                });

                // Call the handler periodically for corners cases.
                clearCheckInterval = function() {
                    var interval = $.data(this, CHECK_INTERVAL_DATA);

                    if (interval) {
                        clearInterval(interval);
                        $.removeData(this, CHECK_INTERVAL_DATA);
                    }
                };

                $root.delegate(selector, 'focus', function() {
                    clearCheckInterval.call(this);
                    $.data(this, CHECK_INTERVAL_DATA, setInterval(handler, 50));
                });

                $root.delegate(selector, 'blur', clearCheckInterval);
            }
        };
    }
}());

// Wrapper around document.elementFromPoint to make it easier to use.
$.elementFromPoint = function(x, y) {
    var element = doc.elementFromPoint(x, y);

    if (element) {
        if (element.nodeType === 3) {
            element = element.parentNode;
        }
        if (element) {
            return $(element);
        }
    }

    return $();
};

$.fn.onCreate = function(selector, handler) {
    if (selector) {
        this.bind('create', function(event) {
            $(event.target).find(selector).each(handler);
        });

    } else {
        this.each(handler);
    }
};

$.easing.easeOutBack = function (x, t, b, c, d, s) {
    if (s === undefined) {
        s = 1.70158;
    }
    return c * ((t = t / d - 1) * t * ((s + 1) * t + s) + 1) + b;
};

$.addQueryParameters = function() {
    var uri = arguments[0],
            questionAt = uri.indexOf('?'),
            queryString,
            argIndex,
            argLength = arguments.length,
            paramName,
            paramValue;

    if (questionAt > -1) {
        queryString = '&' + uri.substring(questionAt + 1);
        uri = uri.substring(0, questionAt);

    } else {
        queryString = '';
    }

    for (argIndex = 1; argIndex < argLength; argIndex += 2) {
        paramName = arguments[argIndex];
        paramValue = arguments[argIndex + 1];
        queryString = queryString.replace(new RegExp('&' + paramName + '=[^&]*', 'g'), '');

        if (paramValue !== undefined && paramValue !== null) {
            $.each($.isArray(paramValue) ? paramValue : [ paramValue ], function(i, v) {
                queryString += '&';
                queryString += encodeURIComponent(paramName);
                queryString += '=';
                queryString += encodeURIComponent(v);
            });
        }
    }

    return queryString.length > 1 ?
            uri + '?' + queryString.substring(1) :
            uri;
};

// window.requestAnimationFrame polyfill.
window.requestAnimationFrame =
        window.requestAnimationFrame ||
        window.mozRequestAnimationFrame ||
        window.msRequestAnimationFrame ||
        window.webkitRequestAnimationFrame ||
        function(callback) {
            window.setTimeout(callback, 1000 / 60);
        };

}(jQuery, window));
