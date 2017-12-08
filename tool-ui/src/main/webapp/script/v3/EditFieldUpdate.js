define([ 'jquery', 'bsp-utils', 'v3/rtc', 'v3/color-utils', 'v3/EditFieldUpdateCache' ], function ($, bsp_utils, rtc, color_utils, efu_cache) {

    var colorsByUuid = { };

    function backgroundColor(uuid) {

        if (!colorsByUuid[uuid]) {
            colorsByUuid[uuid] = color_utils.generateFromHue(color_utils.changeHue(Math.random()));
        }

        return colorsByUuid[uuid];
    }

    // shared-use function for updating a container element either from cached data
    // stored in dataByContentId or from an EditFieldUpdateBroadcast RTC event
    function updateContainer($container, data) {
        var userId = data.userId;
        var fieldNamesByObjectId = data.fieldNamesByObjectId;
        var $viewersContainer = $container.find('[data-rtc-edit-field-update-viewers]');

        if ($viewersContainer.length === 0) {
            $viewersContainer = $container.find('[data-rtc-edit-field-update-other-viewers]');

            if ($viewersContainer.length > 0 &&
                ($('html').attr('data-user-id') === userId)) {

                return;
            }
        }

        if ($viewersContainer.length > 0) {
            var userAvatarHtml = data.userAvatarHtml;
            var closed = data.closed;
            var $viewers = $viewersContainer.find('> .EditFieldUpdateViewers');
            var $some;

            if ($viewers.length === 0) {
                var $none = $('<div/>', {
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
        var $containers = $('[data-rtc-content-id="' + contentId + '"]');

        if ($containers.length === 0) {
            return;
        }

        if (!window.DISABLE_EDIT_FIELD_UPDATE_CACHE) {
            efu_cache.put(data);
        }

        var currentUser = $('html').attr('data-user-id');
        var userId = data.userId;
        var fieldNamesByObjectId = data.fieldNamesByObjectId;

        $containers.each(function() {
            var $container = $(this);

            updateContainer($container, data);

            if (!$container.is('form')) {
                return;
            }

            if (currentUser === userId) {
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

    var invalidateTimeout;

    bsp_utils.onDomInsert(document, '[data-rtc-content-id]', {
        insert: function (container) {
            var $container = $(container);

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
                }, 1000);
            }

            $container.on('blur focus change', ':input', throttledUpdate);
            $container.on('content-state-differences', throttledUpdate);
        },

        afterInsert: function (containers) {

            // restores viewer data on the specified containers,
            // fetching from cache if available, otherwise making
            // an rtc.restore call for the data
            var contentIds = [ ];

            $(containers).each(function () {
                var $container = $(this);

                if (!$container.is('form')) {
                    var contentId = $container.attr('data-rtc-content-id');

                    if (contentId) {

                        if (!window.DISABLE_EDIT_FIELD_UPDATE_CACHE) {
                            var contentData = efu_cache.get(contentId);
                            var i;

                            if (typeof contentData === 'object' && contentData instanceof Array) {
                                for (i = 0; i < contentData.length; i += 1) {
                                    updateContainer($container, contentData[i]);
                                }

                            } else {

                                efu_cache.init(contentId);
                                contentIds.push(contentId);
                            }
                        } else {
                            contentIds.push(contentId);
                        }
                    }
                }
            });

            if (contentIds.length > 0) {

                if (!window.DISABLE_EDIT_FIELD_UPDATE_CACHE) {
                    if (invalidateTimeout) {
                        clearTimeout(invalidateTimeout);
                    }

                    invalidateTimeout = setTimeout(function () {
                        invalidateTimeout = null;

                        efu_cache.invalidate($('[data-rtc-content-id]').map(function () {
                            return $(this).attr('data-rtc-content-id');
                        }).get());
                    }, 5000);
                }

                rtc.restore('com.psddev.cms.tool.page.content.EditFieldUpdateState', {
                    contentId: contentIds
                });
            }
        }
    });
});
