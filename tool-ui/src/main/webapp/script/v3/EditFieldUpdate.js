define([ 'jquery', 'bsp-utils', 'v3/rtc', 'v3/color-utils' ], function ($, bsp_utils, rtc, color_utils) {

    var colorsByUuid = {},
        VIEWERS_CACHE = (function() {

            var viewerDataCache = { },
                hitCount = 0,
                missCount = 0,
                fetchCount = 0,
                putCount = 0,
                cleanCallCount = 0,

                debugViewersCache = function() {
                    return true || window.LOG_VIEWERS_REPORTS && typeof console !== "undefined";
                },

                report = function(force) {

                    if (debugViewersCache() && !force && !((putCount + fetchCount) % 15 === 0)) {
                        return;
                    }

                    var total = hitCount + missCount,
                        ratio = (total === 0 && hitCount === 0) ? 0.0 : (total === 0 ? 1.0 : (hitCount === 0 ? 0.0 : hitCount / total));

                    ratio *= 100;

                    console.log(
                        "putCount: ", putCount,
                        ", fetchCount: ", fetchCount,
                        ", ratio: ", ratio + "%",
                        "size: ", Object.keys(viewerDataCache).length
                    );
                },

                // caches the specified viewer data in the specified cache object,
                // keyed by contentId then userId.
                cacheData = function(cache, data) {

                    putCount += 1;

                    var contentId = data.contentId,
                        userId = data.userId,
                        contentData,
                        userDataIndex = undefined,
                        i;

                    contentData = cache[contentId];

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

                    report();
                },

                // fetches data from cache
                fetchData = function(contentId) {

                    fetchCount += 1;

                    report();

                    return viewerDataCache[contentId];
                },

                containsKey = function(key) {
                    return viewerDataCache[key];
                };

            return {

                putEmpty: function(key) {

                    if (!viewerDataCache[key]) {

                        if (debugViewersCache()) {
                            console.log("%cSEED", "color: green", key);
                        }

                        viewerDataCache[key] = [ ];
                    }
                },

                put: function(data) {

                    if (data && data.contentId) {

                        // only cache data that's existed or been
                        // pre-seeded to ensure that data in the
                        // cache was intentionally placed there
                        // starting with a restore
                        if (containsKey(data.contentId)) {

                            if (debugViewersCache()) {
                                console.log("PUT", data.contentId);
                            }

                            cacheData(viewerDataCache, data);
                        } else {

                            if (debugViewersCache()) {
                                console.log("SKIP", data.contentId);
                            }
                        }
                    }
                },

                fetch: function(contentId) {

                    var result = fetchData(contentId);

                    if (result) {

                        hitCount += 1;
                        if (debugViewersCache()) {
                            console.log("%cCACHE HIT", "color: blue", contentId);
                        }

                    } else {

                        missCount += 1;

                        if (debugViewersCache()) {
                            console.log("%cCACHE MISS", "color: red", contentId);
                        }
                    }

                    return result;
                },

                clearUnused: function() {

                    cleanCallCount += 1;

                    if (!(cleanCallCount % 20 === 0)) {
                        return;
                    }

                    if (debugViewersCache()) {
                        console.log("CLEAR");
                    }

                    // clean out unused cache entries before making call to restore
                    var cleanCache = { };

                    $('[data-rtc-content-id]').each(function() {
                        var contentId = $(this).attr('data-rtc-content-id'),
                            cachedData = fetchData(contentId);

                        if (cachedData) {
                            cleanCache[contentId] = cachedData;
                        }
                    });

                    viewerDataCache = cleanCache;
                }
            };
        })();

    window.VIEWERS_CACHE = VIEWERS_CACHE;

    function backgroundColor(uuid) {

        if (!colorsByUuid[uuid]) {
            colorsByUuid[uuid] = color_utils.generateFromHue(color_utils.changeHue(Math.random()));
        }

        return colorsByUuid[uuid];
    }

    // shared-use function for updating a container element either from cached data
    // stored in dataByContentId or from an EditFieldUpdateBroadcast RTC event
    function updateContainer(container, data) {

        var $container = $(container),
            userId = data.userId,
            closed = data.closed,
            userAvatarHtml = data.userAvatarHtml,
            fieldNamesByObjectId = data.fieldNamesByObjectId,
            $viewersContainer = $container.find('[data-rtc-edit-field-update-viewers]'),
            $viewers = $viewersContainer.find('> .EditFieldUpdateViewers'),
            $some,
            $none;

        if ($viewersContainer.length > 0) {

            if ($viewers.length === 0) {
                $none = $('<div/>', {
                    'class': 'EditFieldUpdateViewers-none',
                    html: $viewersContainer.html()
                });

                $some = $('<div/>', {
                    'class': 'EditFieldUpdateViewers-some'
                });

                $viewers = $('<div/>', {
                    'class': 'EditFieldUpdateViewers',
                    html: [
                        $none,
                        $some
                    ]
                });

                $viewers.append($none);
                $viewers.append($some);
                $viewersContainer.html($viewers);

            } else {
                $some = $viewers.find('> .EditFieldUpdateViewers-some');
            }

            var $viewer = $some.find('> .EditFieldUpdateViewers-viewer[data-user-id="' + userId + '"]');

            if ($viewer.length > 0) {
                if (closed) {
                    $viewer.remove();
                }

            } else if (!closed) {
                $viewer = $('<div/>', {
                    'class': 'EditFieldUpdateViewers-viewer',
                    'data-user-id': userId,
                    html: userAvatarHtml
                });

                $viewer.find('.ToolUserAvatar').css({
                    'background-color': backgroundColor(userId)
                });

                $some.append($viewer);
            }

            function checkFieldNames(id) {
                var fieldNames = fieldNamesByObjectId[id];
                return fieldNames && fieldNames.length > 0;
            }

            if (fieldNamesByObjectId && Object.keys(fieldNamesByObjectId).filter(checkFieldNames).length > 0) {
                $viewer.attr('data-editing', true);

            } else {
                $viewer.removeAttr('data-editing');
            }

            if ($some.find('> .EditFieldUpdateViewers-viewer').length > 0) {
                $viewers.attr('data-some', true);

            } else {
                $viewers.removeAttr('data-some');
            }
        }
    }

    rtc.receive('com.psddev.cms.tool.page.content.EditFieldUpdateBroadcast', function(data) {

        var contentId = data.contentId,
            $containers,
            userId,
            fieldNamesByObjectId;

        if (!contentId) {
            return;
        }

        $containers = $('[data-rtc-content-id="' + contentId + '"]');
        
        if ($containers.length === 0) {

            return;
        }

        VIEWERS_CACHE.put(data);
        
        userId = data.userId;
        fieldNamesByObjectId = data.fieldNamesByObjectId;

        $containers.each(function() {

            updateContainer(this, data);

            var $container = $(this);

            // logic below is scoped to containers within a form - the Edit page.
            // this excludes containers that appear in search results.
            if (!$container.is('form')) {
                return;
            }

            $container.find('.inputPending[data-user-id="' + userId + '"]').each(function() {
                var $pending = $(this);

                $pending.closest('.inputContainer').removeClass('inputContainer-pending');
                $pending.remove();
            });

            if (!fieldNamesByObjectId) {
                return;
            }

            var userName = data.userName;

            $.each(fieldNamesByObjectId, function (objectId, fieldNames) {
                var $inputs = $container.find('.objectInputs[data-id="' + objectId + '"]');

                if ($inputs.length === 0) {
                    return;
                }

                $.each(fieldNames, function (i, fieldName) {
                    var $container = $inputs.find('> .inputContainer[data-field-name="' + fieldName + '"]');
                    var nested = false;

                    $container.find('.objectInputs').each(function() {
                        if (fieldNamesByObjectId[$(this).attr('data-id')]) {
                            nested = true;
                            return false;
                        }
                    });

                    if (!nested) {
                        $container.addClass('inputContainer-pending');

                        $container.find('> .inputLabel').after($('<div/>', {
                            'class': 'inputPending',
                            'data-user-id': userId,
                            'html': [
                                'Pending edit from ' + userName + ' - ',
                                $('<a/>', {
                                    'text': 'Unlock',
                                    'click': function() {
                                        if (confirm('Are you sure you want to forcefully unlock this field?')) {
                                            rtc.execute('com.psddev.cms.tool.page.content.EditFieldUpdateAction', {
                                                contentId: $container.closest('form').attr('data-rtc-content-id'),
                                                unlockObjectId: $container.closest('.objectInputs').attr('data-id'),
                                                unlockFieldName: $container.attr('data-field-name')
                                            });
                                        }

                                        return false;
                                    }
                                })
                            ]
                        }));
                    }
                });
            });
        })
    });

    bsp_utils.onDomInsert(document, '[data-rtc-content-id]', {
        insert: function (container) {
            var $container = $(container);

            // logic below is scoped to containers within a form - the Edit page.
            // this excludes containers that appear in search results.
            if (!$container.is('form')) {
                return;
            }

            var contentId = $container.attr('data-rtc-content-id');
            
            if (!contentId) {
                return;
            }

            var oldFieldNamesByObjectId = null;

            function update() {
                var fieldNamesByObjectId = {};

                $container.find('.inputContainer.state-changed, .inputContainer.state-focus').each(function () {
                    var $container = $(this);
                    var objectId = $container.closest('.objectInputs').attr('data-id');

                    (fieldNamesByObjectId[objectId] = fieldNamesByObjectId[objectId] || []).push($container.attr('data-field-name'));
                });

                if (fieldNamesByObjectId && JSON.stringify(fieldNamesByObjectId) !== JSON.stringify(oldFieldNamesByObjectId)) {
                    oldFieldNamesByObjectId = fieldNamesByObjectId;

                    rtc.execute('com.psddev.cms.tool.page.content.EditFieldUpdateAction', {
                        contentId: contentId,
                        fieldNamesByObjectId: fieldNamesByObjectId
                    });
                }
            }

            rtc.initialize('com.psddev.cms.tool.page.content.EditFieldUpdateState', {
                contentId: contentId

            }, function () {
                oldFieldNamesByObjectId = null;
                update();
            });

            var updateTimeout;

            function throttledUpdate() {
                if (updateTimeout) {
                    clearTimeout(updateTimeout);
                }

                updateTimeout = setTimeout(function () {
                    updateTimeout = null;
                    update();
                }, 50);
            }

            $container.on('blur focus change', ':input', throttledUpdate);
            $container.on('content-state-differences', throttledUpdate);
        },

        afterInsert: function (containers) {

            // restores viewer data on the specified containers,
            // fetching from cache if available, otherwise making
            // an rtc.restore call for the data

            var contentIds = [ ],
                i,
                id;

            $(containers).each(function () {
                var $container = $(this),
                    contentId,
                    contentData,
                    i;

                if (!$container.is('form')) {
                    contentId = $container.attr('data-rtc-content-id');

                    if (contentId) {

                        contentData = VIEWERS_CACHE.fetch(contentId);
                        if (typeof contentData === 'object' && contentData instanceof Array) {

                            for (i = 0; i < contentData.length; i += 1) {

                                updateContainer($container, contentData[i]);
                            }

                        } else {

                            VIEWERS_CACHE.putEmpty(contentId);
                            contentIds.push(contentId);
                        }
                    }
                }
            });

            if (contentIds.length > 0) {

                VIEWERS_CACHE.clearUnused();

                rtc.restore('com.psddev.cms.tool.page.content.EditFieldUpdateState', {
                    contentId: contentIds
                });
            }
        }
    });
});
