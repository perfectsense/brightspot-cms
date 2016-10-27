define([ 'jquery', 'bsp-utils', 'v3/rtc', 'v3/color-utils' ], function ($, bsp_utils, rtc, color_utils) {

    var colorsByUuid = {},
        shortViewerDataCache = { },
        longViewerDataCache = { };

    function backgroundColor(uuid) {

        if (!colorsByUuid[uuid]) {
            colorsByUuid[uuid] = color_utils.generateFromHue(color_utils.changeHue(Math.random()));
        }

        return colorsByUuid[uuid];
    }

    // restores viewer data on the specified containers,
    // fetching from cache if available, otherwise making
    // an rtc.restore call for the data
    function restoreData($containers) {

        var contentIds = [ ];

        $($containers).each(function () {
            var container = this,
                $container = $(container),
                contentId,
                contentData,
                i;

            if (!$container.is('form')) {
                contentId = $container.attr('data-rtc-content-id');

                if (contentId) {

                    contentData = fetchData(contentId);
                    if (typeof contentData === 'object' && contentData instanceof Array) {

                        for (i = 0; i < contentData.length; i += 1) {

                            updateContainer(container, contentData[i]);
                        }

                    } else {

                        shortViewerDataCache[contentId] = [ ];
                        contentIds.push(contentId);
                    }
                }
            }
        });

        if (contentIds.length > 0) {

            rtc.restore('com.psddev.cms.tool.page.content.EditFieldUpdateState', {
                contentId: contentIds
            });
        }
    }

    // caches the specified viewer data in the specified cache object,
    // keyed by contentId then userId.
    function cacheData(cache, data) {

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
    }

    // fetches data from cache, combining short-term
    // cache into long-term cache for the requested contentId
    // as applicable, then returning the updated or existing
    // long-term cache value
    function fetchData(contentId) {

        var i,
            shortCacheData = shortViewerDataCache[contentId],
            longCacheData = longViewerDataCache[contentId];

        if (shortCacheData === undefined) {

            // no short cache,
            // return long cache data
            return longCacheData;

        } else if (longCacheData === undefined) {

            // copy short cache data to long cache,
            // delete short cache data
            longViewerDataCache[contentId] = shortCacheData;
            delete shortViewerDataCache[contentId];
            return longViewerDataCache[contentId];
        }

        // merge short cache data to long cache,
        // delete short cache data

        for (i = 0; i < shortCacheData.length; i += 1) {

            cacheData(longViewerDataCache, shortCacheData[i]);
        }

        delete shortViewerDataCache[contentId];

        return longViewerDataCache[contentId];
    }

    // pushes the observed data into short-term cache,
    // to be used by fetchData
    function observeData(data) {

        var contentId = data.contentId;

        if (contentId && $('[data-rtc-content-id="' + contentId + '"]').size() > 0) {
            cacheData(shortViewerDataCache, data);
        }
    }

    // shared-use function for updating a container element either from cached data
    // stored in dataByContentId or from an EditFieldUpdateBroadcast RTC event
    function updateContainer(containerElement, data) {

        var $container = $(containerElement),
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

        var contentId = data.contentId;

        if (!contentId) {
            return;
        }

        observeData(data);

        var $containers = $('[data-rtc-content-id="' + contentId + '"]');
        
        if ($containers.length === 0) {
            return;
        }
        
        var userId = data.userId;
        var fieldNamesByObjectId = data.fieldNamesByObjectId;

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

            restoreData(containers);
        }
    });
});
