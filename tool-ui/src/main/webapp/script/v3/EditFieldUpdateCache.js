define(['jquery'], function($) {
    var cache = { };
    var puts = 0;
    var gets = 0;
    var hits = 0;

    function log() {
        if (!window.DEBUG_EDIT_FIELD_UPDATE_CACHE || typeof console === 'undefined') {
            return;
        }

        var newArguments = Array.prototype.slice.call(arguments, 0);

        newArguments[0] = '[EditFieldUpdateCache] ' + newArguments[0];
        newArguments.push('puts');
        newArguments.push(puts);
        newArguments.push('gets');
        newArguments.push(gets);
        newArguments.push('hits');
        newArguments.push(((gets === 0 || hits === 0) ? 0.0 : (gets === 0 ? 1.0 : (hits === 0 ? 0.0 : hits / gets)) * 100) + '%');
        newArguments.push('entries');
        newArguments.push(Object.keys(cache).length);

        console.log.apply(console, newArguments);
    }

    return {
        init: function(key) {
            if (!cache[key]) {
                cache[key] = [ ];

                log('%cinit', 'color: blue', key);
            }
        },

        put: function(data) {
            if (!data) {
                return;
            }

            var contentId = data.contentId;

            if (!contentId) {
                return;
            }

            // only cache data that's existed or been
            // pre-seeded to ensure that data in the
            // cache was intentionally placed there
            // starting with a restore
            if (cache[contentId]) {

                // caches the specified viewer data in the specified cache object,
                // keyed by contentId then userId.
                puts += 1;

                log('%cput', 'color: blue', contentId);

                var userId = data.userId;
                var contentData = cache[contentId];
                var userDataIndex = undefined;
                var i;

                if (contentData === undefined) {
                    contentData = [ ];
                    cache[contentId] = contentData;
                }

                for (i = 0; i < contentData.length; i += 1) {
                    if (contentData[i].userId === userId) {
                        userDataIndex = i;
                    }
                }

                if (userDataIndex !== undefined && userDataIndex >= 0) {
                    contentData.splice(userDataIndex, 1, data);

                } else {
                    contentData.push(data);
                }

            } else {
                log('%cput (skipped)', 'color: blue', contentId);
            }
        },

        get: function(contentId) {
            gets += 1;
            var contentData = cache[contentId];

            if (contentData) {
                hits += 1;
                log('%cget (hit)', 'color: green', contentId);

            } else {
                log('%cget (miss)', 'color: red', contentId);
            }

            return contentData;
        },

        invalidate: function (contentIds) {
            if (contentIds.length < 1) {
                return;
            }

            // clean out unused cache entries before making call to restore
            var cleanCache = { };

            $.each(contentIds, function (i, contentId) {
                var contentData = cache[contentId];

                if (contentData) {
                    cleanCache[contentId] = contentData;
                }
            });

            cache = cleanCache;

            log('%cinvalidate', 'color: blue', 'keep: ' + contentIds);
        }
    };
});
