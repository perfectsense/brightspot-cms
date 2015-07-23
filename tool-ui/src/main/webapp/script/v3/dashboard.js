define([
        'jquery',
        'bsp-utils'],

    function ($, bsp_utils) {

        var settings = {
            updateUrl: '/cms/misc/updateUserDashboard',
            dragClass: 'drag-active',
            editModeClass: 'dashboard-editable',
            placeholderClass: 'widget-ghost',
            throttleInterval: 100,
            dashboardSelector: '.dashboard-columns',
            columnSelector: '.dashboard-column',
            widgetSelector: '.dashboard-widget',
            addColumnClass: 'dashboard-addColumnButton',
            addWidgetClass: 'dashboard-addWidgetButton',
            addWidgetContainerClass: 'dashboard-add-widget-container',
            removeWidgetClass: 'widget-remove',
            columnGutterClass: 'dashboard-column-gutter',
            minColumnWidth: 320
        };

        bsp_utils.onDomInsert(document, '.dashboard-columns', {

            'insert': function (dashboard) {

                settings = $.extend({}, settings, {state: {}});

                var $body = $('body');
                var $dashboard = $(dashboard);
                var $columns = $dashboard.find(settings.columnSelector);
                var $widgets = $dashboard.find(settings.widgetSelector);
                var draggableWidgetSelector = settings.widgetSelector + '[draggable="true"]';

                /**
                 * Drag events for widgets when in edit mode
                 */
                $body
                    .on('dragstart', draggableWidgetSelector, function(e) {

                        settings.state.activeWidget = this;
                        var $this = $(this);
                        var $widget = $(e.target);
                        var dataTransfer = e.originalEvent.dataTransfer;

                        settings.state.originalY = getColumnIndexFromWidget($this.closest(settings.columnSelector));
                        settings.state.originalX = getRowIndex($this);

                        $widget.toggleClass(settings.dragClass);

                        dataTransfer.setData('text/html', settings.state.activeWidget.innerHTML);
                        dataTransfer.effectAllowed = 'move';
                        dataTransfer.dropEffect = 'move';
                    })
                    .on('dragend', draggableWidgetSelector, function(e) {
                        var $widget = $(e.target);
                        $widget.toggleClass(settings.dragClass);

                        var activeWidget = $(settings.state.activeWidget);
                        if (!settings.state.dragTarget) {
                            return false;
                        }

                        var x = getRowIndex(activeWidget);
                        var y = getColumnIndexFromWidget(activeWidget);

                        var $oldColumn = $($(settings.columnSelector).get(y));
                        if ($oldColumn.find(settings.widgetSelector).size() <= 1) {
                            $oldColumn.remove();
                        }

                        $(settings.state.placeholder).replaceWith(activeWidget);
                        settings.state = {};

                        refreshEditElements();

                        $.ajax({
                            'type' : 'post',
                            'url'  : settings.updateUrl,
                            'data' :
                            {
                                'action'    : 'dashboardWidgets-move',
                                'x'         : x,
                                'y'         : y,
                                'originalX' : settings.state.originalX,
                                'originalY' : settings.state.originalY,
                                'id'        : activeWidget.attr('data-widget-id')
                            }
                        });

                    })
                    //.on('dragover', bsp_utils.throttle(settings.throttleInterval, function(e) {
                    //    if (e.preventDefault) e.preventDefault();
                    //
                    //    var dataTransfer = e.originalEvent.dataTransfer;
                    //    dataTransfer.effectAllowed = 'move';
                    //    dataTransfer.dropEffect = 'move';
                    //
                    //    return false;
                    //}))
                    .on('dragenter', draggableWidgetSelector, bsp_utils.throttle(settings.throttleInterval, function(e) {
                        e.preventDefault();
                        var $dragTarget = $(this);

                        /**
                         * Do nothing if:
                         * 1. dragging over widget placeholder
                         * 2. activeWidget does not exist
                         * 3. dragging over widget placeholder
                         */
                        if (this === settings.state.placeholder
                            || typeof settings.state.activeWidget === 'undefined'
                            || settings.state.activeWidget === null
                            || $dragTarget.hasClass(settings.placeholderClass)) {
                            return false;
                        }

                        /**
                         * If dragging over currently active widget,
                         * remove placeholder from UI
                         */
                        if (this === settings.state.activeWidget) {
                            $(settings.state.placeholder).remove();
                            settings.state.dragTarget = null;
                            return false;
                        }

                        var siblingPlaceholder = $dragTarget.siblings('.' + settings.placeholderClass);
                        var insertAfter = siblingPlaceholder.size() > 0 && siblingPlaceholder.index() < $dragTarget.index();

                        if (typeof settings.state.placeholder !== 'undefined') {
                            $(settings.state.placeholder).remove();
                        }

                        settings.state.placeholder = createPlaceholderElements(insertAfter);

                        if (insertAfter) {
                            $dragTarget.after(settings.state.placeholder);
                        } else {
                            $dragTarget.before(settings.state.placeholder);
                        }

                        settings.state.dragTarget = this;

                        return false;
                    }))
                    .on('dragleave', draggableWidgetSelector, bsp_utils.throttle(settings.throttleInterval, function(e) {
                        e.preventDefault();
                        e.stopPropagation();

                        if (this === settings.state.dragTarget
                            || this === settings.state.placeholder
                            || this === settings.state.activeWidget) {
                            return false;
                        }

                        var $dragTarget = $(this);
                        var prev = $dragTarget.prev();

                        if (prev) {

                            if (prev.hasClass(settings.placeholderClass)) {
                                prev.remove();
                            }

                        }

                        settings.state.dragTarget = null;
                    }));
                //webkit bug not fixed, cannot use drop event v. 39.0.2171.99 https://bugs.webkit.org/show_bug.cgi?id=37012
                //$widgets.on('drop'     , drop);

                /**
                 * Drag events for resizing columns
                 */
                $body
                    .on('dragstart', '.' + settings.columnGutterClass, bsp_utils.throttle(settings.throttleInterval, function(e) {

                        // using this to disable global drag handling from v3.js #331

                        var $gutter = $(this);
                        var originalEvent = e.originalEvent;
                        var dataTransfer = originalEvent.dataTransfer;

                        //hides ghost element created from drag event
                        dataTransfer.setDragImage(this, 10000, 10000);
                        dataTransfer.originalX = originalEvent.pageX;
                        dataTransfer.prevColumnWidth = $gutter.prev(settings.columnSelector).data('actualWidth');
                        dataTransfer.effectAllowed = 'none';
                        dataTransfer.nextColumnWidth = $gutter.next(settings.columnSelector).data('actualWidth');
                    }))
                    .on('drag', '.' + settings.columnGutterClass, bsp_utils.throttle(10, function(e) {

                        var $gutter = $(this);
                        var originalEvent = e.originalEvent;
                        var dataTransfer = originalEvent.dataTransfer;
                        var horizontalDiff = originalEvent.pageX - dataTransfer.originalX;
                        var prevColNewWidth = dataTransfer.prevColumnWidth + horizontalDiff;
                        var nextColNewWidth = dataTransfer.nextColumnWidth - horizontalDiff;

                        if (prevColNewWidth >= 320 && nextColNewWidth >= 320) {
                            $gutter.next(settings.columnSelector).css('flex', nextColNewWidth + ' 320 1px');
                            $gutter.prev(settings.columnSelector).css('flex', prevColNewWidth + ' 320 1px');
                            $gutter.next(settings.columnSelector).data('actualWidth', nextColNewWidth);
                            $gutter.prev(settings.columnSelector).data('actualWidth', prevColNewWidth);
                        }

                        return false;
                    }))
                    .on('dragend', '.' + settings.columnGutterClass, function(e) {
                        var params = {
                            action : 'dashboardColumns-resize',
                            y : [],
                            width : []
                        };

                        $columns.each(function(yIndex, col) {
                            params.y.push(yIndex);
                            params.width.push($(col).css('flex-grow'));
                        });

                        $.ajax({
                            'url' : '/cms/misc/updateUserDashboard',
                            'type' : 'POST',
                            'data' : $.param(params, true)
                        });

                        return false;
                    });

                /**
                 * Enables dashboard edit mode
                 */
                $body.on('click', '.dashboard-edit', function () {
                    $(this).toggleClass('toggled');
                    toggleEditMode();
                });

                /**
                 * Handles removing of widgets
                 */
                $body.on('click', '.' + settings.removeWidgetClass, function(event) {
                    event.preventDefault();
                    event.stopImmediatePropagation();
                    var $a = $(this);
                    var $widget = $a.closest(settings.widgetSelector);
                    var $column = $widget.closest(settings.columnSelector);
                    $.ajax({
                        'url' : $a.attr('href'),
                        'type': 'post',
                        'data': {
                            'x' : getRowIndex($widget),
                            'y' : getColumnIndex($column),
                            'action' : 'dashboardWidgets-remove'
                        }
                    });

                    $widget.detach();

                    //if no widgets remain in column, remove column
                    if ($column.find(settings.widgetSelector).size() === 0) {
                        $column.remove();
                    }

                    refreshEditElements();
                });

                function toggleEditMode() {

                    $dashboard.toggleClass(settings.editModeClass);
                    $widgets.prop('draggable', !$widgets.prop('draggable'));

                    if ($dashboard.hasClass(settings.editModeClass)) {
                        addAllEditElements($dashboard);
                    } else {
                        removeAllEditElements($dashboard);
                    }

                }

                function refreshEditElements() {
                    removeAllEditElements($dashboard);
                    addAllEditElements($dashboard);
                }

                function addAllEditElements(dashboard) {
                    DISABLE_DRAG_AND_DROP = true;
                    var $dashboard = $(dashboard);

                    $dashboard.find(settings.columnSelector).each(function(yIndex, col) {
                        var $col = $(col);
                        $col.data('actualWidth', $col.width());
                        insertColumnGutter($col);
                        $col.find(settings.widgetSelector).each(function(xIndex, dashboardWidget) {
                            insertRowButtons(dashboardWidget);
                            $(dashboardWidget).find('h1').first().append($('<a/>', {'class' : settings.removeWidgetClass, 'href' : settings.updateUrl}));
                        });
                    });
                }

                function removeAllEditElements(dashboard) {
                    DISABLE_DRAG_AND_DROP = false;
                    $(dashboard)
                        .find('.' + settings.addWidgetClass + ', .' + settings.addColumnClass + ', .' + settings.columnGutterClass + ', .' + settings.addWidgetContainerClass + ', .' + settings.removeWidgetClass)
                        .remove();
                }

                function getColumnIndexFromWidget(dashboardWidget) {
                    return getColumnIndex($(dashboardWidget).closest(settings.columnSelector));
                }

                function getColumnIndex(dashboardColumn) {
                    return $(dashboardColumn).closest('.dashboard-columns').find('.dashboard-column').index(dashboardColumn);
                }

                function getRowIndex(dashboardWidget) {
                    return $(dashboardWidget).closest('.dashboard-column').find('.dashboard-widget').index(dashboardWidget);
                }

                function createAddWidgetButton(x, y, isAddColumnOperation) {
                    if (typeof isAddColumnOperation === 'undefined') {
                        isAddColumnOperation = false;
                    }

                    return $('<a/>', {
                        'class': settings.addWidgetClass,
                        'href': '/cms/createWidget?x=' + x + '&y=' + y + '&action=dashboardWidgets-add&addColumn=' + isAddColumnOperation,
                        'target': 'createWidget'
                    }).append($('<span/>').text("Add Widget"));
                }

                function createAddRowContainerAndButton(x, y) {
                    return $('<div/>', {
                        'class' : settings.addWidgetContainerClass
                    }).append(createAddWidgetButton(x, y));
                }

                function createColumnGutterAndButton(y) {

                    var $gutter = $('<div/>', {
                        'class' : settings.columnGutterClass
                    });

                    //first and last columns are not draggable
                    if (y !== 0 && y !== $columns.size()) {
                        $gutter.attr('draggable', true);
                        $gutter.append($('<div/>', {
                            'class' : 'drag-handle'
                        }));
                    }
                    return $gutter.append(createAddWidgetButton(0, y, true));
                }

                function createPlaceholderElements(addButtonBefore) {
                    var container = $('<div/>');
                    var placeholderWidget = $('<div />', {'class': 'widget ' + settings.placeholderClass});

                    container.append(placeholderWidget);

                    if (addButtonBefore === true) {
                        container.prepend(createAddRowContainerAndButton(0, 0));
                    } else {
                        container.append(createAddRowContainerAndButton(0, 0));
                    }

                    return container.contents();
                }

                function insertRowButtons(dashboardWidget) {
                    var $dashboardWidget = $(dashboardWidget);
                    var xIndex = getRowIndex(dashboardWidget);
                    var yIndex = getColumnIndexFromWidget(dashboardWidget);

                    if (xIndex === 0) {
                        $dashboardWidget.before(createAddRowContainerAndButton(xIndex, yIndex));
                    }

                    $dashboardWidget.after(createAddRowContainerAndButton(xIndex + 1, yIndex));
                }

                function insertColumnGutter(dashboardColumn) {
                    var $dashboardColumn = $(dashboardColumn);
                    var columnIndex = getColumnIndex($dashboardColumn);

                    if (columnIndex === 0) {
                        $dashboardColumn.before(createColumnGutterAndButton(columnIndex));
                    }

                    $dashboardColumn.after(createColumnGutterAndButton(columnIndex + 1));
                }

                bsp_utils.onDomInsert(document, 'meta[name="widget"]', {

                    'insert': function (meta) {
                        var $meta = $(meta);
                        $meta.closest('.popup').trigger('close.popup');
                        var newDashboardWidget =
                            $('<div/>', {'class' : 'frame dashboard-widget', 'draggable' : 'true'}).append(
                                $('<a/>', { 'href' : $meta.attr('content')})
                            );

                        var x = $meta.attr('data-x');
                        var y = $meta.attr('data-y');

                        var $dashboard = $('.dashboard-columns');
                        var $columns = $dashboard.find(settings.columnSelector);
                        var $column = $($columns.get(y));

                        if ($meta.attr('data-add-column') === 'true') {
                            var newColumn = $('<div/>', {
                                'class' : 'dashboard-column',
                                'style' : 'flex: 320 320 auto'
                            });
                            $column.before(newColumn);
                            insertColumnGutter(newColumn);
                            $column = newColumn;
                        }

                        var widgetInRow = $column.find(settings.widgetSelector).get(x);

                        if (widgetInRow) {
                            $(widgetInRow).before(newDashboardWidget);
                        } else {
                            $column.append(newDashboardWidget);
                        }


                        newDashboardWidget.find('a:only-child').click();
                        refreshEditElements();
                    }

                });
            }

        });
    });