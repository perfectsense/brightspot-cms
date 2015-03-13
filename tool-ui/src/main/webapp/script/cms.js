(function($, win, undef) {

var $win = $(win),
        doc = win.document,
        $doc = $(doc),
        toolChecks = [ ],
        toolCheckActionCallbacks = [ ];

$.addToolCheck = function(check) {
    toolCheckActionCallbacks.push(check.actions);
    delete check.actions;
    toolChecks.push(check);
};

$.addToolCheck({
    'check': 'kick',
    'actions': {
        'kickIn': function(parameters) {
            win.location = win.location.protocol + '//' + win.location.host + parameters.returnPath;
        },

        'kickOut': function() {
            win.location = CONTEXT_PATH + '/logIn.jsp?forced=true&returnPath=' + encodeURIComponent(win.location.pathname + win.location.search);
        }
    }
});

// Standard behaviors.
$doc.repeatable('live', '.repeatableForm, .repeatableInputs, .repeatableLayout, .repeatableObjectId');
$doc.repeatable('live', '.repeatableText', {
    'addButtonText': '',
    'removeButtonText': '',
    'restoreButtonText': ''
});

$doc.autoSubmit('live', '.autoSubmit');
$doc.calendar('live', ':text.date');
$doc.code('live', 'textarea[data-code-type]');
$doc.dropDown('live', 'select[multiple], select[data-searchable="true"]');
$doc.editablePlaceholder('live', ':input[data-editable-placeholder]');
$doc.expandable('live', ':text.expandable, textarea', {
    'shadowClass': 'input'
});

$doc.fixedScrollable('live', '.fixedScrollable, .searchResult > .searchResultList, .popup[name="miscSearch"] .searchFiltersRest');

$doc.frame({
    'frameClassName': 'frame',
    'loadingClassName': 'loading',
    'loadedClassName': 'loaded'
});

$doc.imageEditor('live', '.imageEditor');
$doc.lazyLoad('live', '.lazyLoad');
$doc.locationMap('live', '.locationMap');
$doc.regionMap('live', '.regionMap');
$doc.objectId('live', ':input.objectId');
$doc.pageLayout('live', '.pageLayout');
$doc.pageThumbnails('live', '.pageThumbnails');
$doc.rte('live', '.richtext');
$doc.spreadsheet('live', '.spreadsheet');
$doc.tabbed('live', '.tabbed, .objectInputs');
$doc.toggleable('live', '.toggleable');
$doc.widthAware('live', '[data-widths]');
$doc.workflow('live', '.workflow');

$doc.onCreate(':text.color', function() {
    $(this).spectrum({
        'cancelText': 'Cancel',
        'chooseText': 'OK',
        'preferredFormat': 'hex6',
        'showInput': true
    });
});

// Remove placeholder text over search input when there's text.
$doc.onCreate('.searchInput', function() {
    var $container = $(this),
            $label = $container.find('> label'),
            $input = $container.find('> :text');

    $input.bind('input', $.run(function() {
        $label.toggle(!$input.val());
    }));
});

// Automatically focus on certain elements.
$doc.onCreate('[autofocus], .autoFocus', function() {
    var focus = doc.activeElement;

    if (!focus || focus === doc || focus === doc.body) {
        $(this).focus();
    }
});

// Hide non-essential items in the permissions input.
$doc.onCreate('.inputContainer .permissions select', function() {
    var $select = $(this);

    $select.bind('change', $.run(function() {
        $select.parent().find('> h2, > ul').toggle($select.find(':selected').val() === 'some');
    }));
});

// Allow dashboard widgets to move around.
$doc.onCreate('.dashboardCell', function() {
    var $cell = $(this),
            $collapse,
            $moveContainer,
            saveDashboard,
            $moveUp,
            $moveDown,
            $moveLeft,
            $moveRight;

    $collapse = $('<span/>', {
        'class': 'dashboardCollapse',
        'click': function() {
            $cell.toggleClass('dashboardCell-collapse');
            saveDashboard();
        }
    });

    $moveContainer = $('<span/>', {
        'class': 'dashboardMoveContainer',
        'click': function() {
            $(this).toggleClass('dashboardMoveContainer-open');
        }
    });

    saveDashboard = function() {
        var $dashboard = $cell.closest('.dashboard'),
                $columns,
                widgets = [ ],
                widgetsCollapse = [ ];

        $dashboard.find('.dashboardColumn:empty').remove();
        $columns = $dashboard.find('.dashboardColumn');
        $dashboard.attr('data-columns', $columns.length);

        $columns.each(function() {
            var w = widgets[widgets.length] = [ ];

            $(this).find('.dashboardCell').each(function() {
                var $cell = $(this),
                        name = $cell.attr('data-widget');

                w[w.length] = name;

                if ($cell.hasClass('dashboardCell-collapse')) {
                    widgetsCollapse[widgetsCollapse.length] = name;
                }
            });
        });

        $.ajax({
            'type': 'post',
            'url': CONTEXT_PATH + '/misc/updateUserSettings',
            'data': {
                'action': 'dashboardWidgets-position',
                'widgets': JSON.stringify(widgets),
                'widgetsCollapse': JSON.stringify(widgetsCollapse)
            }
        });
    };

    $moveUp = $('<span/>', {
        'class': 'dashboardMoveUp',
        'click': function() {
            $cell.prev().before($cell);
            saveDashboard();
        }
    });

    $moveDown = $('<span/>', {
        'class': 'dashboardMoveDown',
        'click': function() {
            $cell.next().after($cell);
            saveDashboard();
        }
    });

    $moveLeft = $('<span/>', {
        'class': 'dashboardMoveLeft',
        'click': function() {
            var $column = $cell.closest('.dashboardColumn');
                    $prevColumn = $column.prev();

            if ($prevColumn.length === 0) {
                $prevColumn = $('<div/>', {
                    'class': 'dashboardColumn'
                });

                $column.before($prevColumn);
            }

            $prevColumn.prepend($cell);
            saveDashboard();
        }
    });

    $moveRight = $('<span/>', {
        'class': 'dashboardMoveRight',
        'click': function() {
            var $column = $cell.closest('.dashboardColumn');
                    $nextColumn = $column.next();

            if ($nextColumn.length === 0) {
                $nextColumn = $('<div/>', {
                    'class': 'dashboardColumn'
                });

                $column.after($nextColumn);
            }

            $nextColumn.prepend($cell);
            saveDashboard();
        }
    });

    $moveContainer.append($moveUp);
    $moveContainer.append($moveDown);
    $moveContainer.append($moveLeft);
    $moveContainer.append($moveRight);

    $cell.append($collapse);
    $cell.append($moveContainer);
});

$doc.onCreate('.searchSuggestionsForm', function() {
    var $suggestionsForm = $(this),
            $source = $suggestionsForm.popup('source'),
            $contentForm = $source.closest('.contentForm'),
            search;

    if ($contentForm.length === 0) {
        return;
    }

    search = win.location.search;
    search += search.indexOf('?') > -1 ? '&' : '?';
    search += 'id=' + $contentForm.attr('data-object-id');

    $.ajax({
        'data': $contentForm.serialize(),
        'type': 'post',
        'url': CONTEXT_PATH + '/content/state.jsp' + search,
        'complete': function(request) {
            if ($suggestionsForm.closest('body').length === 0) {
                return;
            }

            $suggestionsForm.append($('<input/>', {
                'type': 'hidden',
                'name': 'object',
                'value': request.responseText
            }));

            $suggestionsForm.append($('<input/>', {
                'type': 'hidden',
                'name': 'field',
                'value': $source.closest('.inputContainer').attr('data-field')
            }));

            $suggestionsForm.submit();
        }
    });
});

// Mark changed inputs.
$doc.on('change', '.inputContainer', function() {
    var $container = $(this),
            changed = false;

    $container.find('input, textarea').each(function() {
        if (this.defaultValue !== this.value) {
            changed = true;
            return;
        }
    });

    if (!changed) {
        $container.find('option').each(function() {
            if (this.defaultSelected !== this.selected) {
                changed = true;
                return;
            }
        });
    }

    $container.toggleClass('state-changed', changed);
});

// Content diff with a side by side view.
$doc.onCreate('.contentDiff', function() {
    var $container = $(this),
            $tabs,
            $tabEdit,
            $tabSideBySide,
            $left = $container.find('> .contentDiffLeft'),
            $right = $container.find('> .contentDiffRight'),
            getValues;

    $tabs = $('<ul/>', {
        'class': 'tabs'
    });

    $tabEdit = $('<li/>', {
        'html': $('<a/>', {
            'text': 'Edit',
            'click': function() {
                $container.trigger('contentDiff-edit');
                return false;
            }
        })
    });

    $tabSideBySide = $('<li/>', {
        'html': $('<a/>', {
            'text': 'Side By Side',
            'click': function() {
                $container.trigger('contentDiff-sideBySide');
                return false;
            }
        })
    });

    $container.bind('contentDiff-edit', function() {
        $container.add($('.widget-publishing')).removeClass('contentDiff-sideBySide').addClass('contentDiff-edit');
        $tabs.find('li').removeClass('state-selected');
        $tabEdit.addClass('state-selected');

        $left.find('> .objectInputs > .tabs').css('height', '');
        $left.find('> .objectInputs > .inputContainer').css('height', '');
        $right.find('> .objectInputs > .tabs').css('height', '');
        $right.find('> .objectInputs > .inputContainer').css('height', '');
    });

    $container.bind('contentDiff-sideBySide', function() {
        $container.add($('.widget-publishing')).removeClass('contentDiff-edit').addClass('contentDiff-sideBySide');
        $tabs.find('li').removeClass('state-selected');
        $tabSideBySide.addClass('state-selected');

        function equalizeHeight($left, $right) {
            setTimeout(function() {
                $left.add($right).height(Math.max($left.height(), $right.height()));
            }, 100);
        }

        $left.add($right).find('.collapsed').removeClass('collapsed');

        $left.find('> .objectInputs > .tabs').each(function() {
            var $leftTabs = $(this);

            equalizeHeight($leftTabs, $right.find('> .objectInputs > .tabs'));
        });

        $left.find('> .objectInputs > .inputContainer').each(function() {
            var $leftInput = $(this);

            equalizeHeight($leftInput, $right.find('> .objectInputs > .inputContainer[data-field="' + $leftInput.attr('data-field') + '"]'));
        });
    });

    getValues = function($input) {
        return $input.
                find(':input, select, textarea').
                serialize().
                replace(new RegExp('(^|&)[^%]+%2F', 'g'), '$1%2F');
    };

    $left.find('> .objectInputs > .inputContainer').each(function() {
        var $leftInput = $(this),
                $rightInput = $right.find('> .objectInputs > .inputContainer[data-field="' + $leftInput.attr('data-field') + '"]'),
                height = Math.max($leftInput.outerHeight(true), $rightInput.outerHeight(true));

        if (getValues($leftInput) === getValues($rightInput)) {
            $leftInput.addClass('contentDiffSame');
            $rightInput.addClass('contentDiffSame');
        }
    });

    $left.find('> .objectInputs > .inputContainer > .inputSmall > textarea:not(.richtext)').each(function() {
        var $leftText = $(this),
                $rightText = $right.find('> .objectInputs > .inputContainer[data-field="' + $leftText.closest('.inputContainer').attr('data-field') + '"] textarea:not(.richtext)'),
                left = $leftText.val(),
                right = $rightText.val(),
                diffs = JsDiff.diffWords(left, right),
                $leftCopy = $('<div/>', { 'class': 'contentDiffCopy' }),
                $rightCopy = $('<div/>', { 'class': 'contentDiffCopy' });

        $.each(diffs, function(i, diff) {
            if (!diff.added) {
                $leftCopy.append(diff.removed ?
                        $('<span/>', { 'class': 'contentDiffRemoved', 'text': diff.value }) :
                        diff.value);
            }
        });

        $.each(diffs, function(i, diff) {
            if (!diff.removed) {
                $rightCopy.append(diff.added ?
                        $('<span/>', { 'class': 'contentDiffAdded', 'text': diff.value }) :
                        diff.value);
            }
        });

        $leftText.addClass('contentDiffText');
        $leftText.before($leftCopy);

        $rightText.addClass('contentDiffText');
        $rightText.before($rightCopy);
    });

    $tabs.append($tabEdit);
    $tabs.append($tabSideBySide);
    $container.prepend($tabs);
    $container.trigger($right.is('.contentDiffCurrent') ?
            'contentDiff-sideBySide' :
            'contentDiff-edit');
});

$doc.onCreate('.searchAdvancedResult', function() {
    var $result = $(this),
            checked;

    $result.find('thead tr:first th:first').append($('<div/>', {
        'html': $('<span/>', {
            'class': 'icon icon-check icon-only'
        }),

        'css': {
            'cursor': 'pointer',
            'text-align': 'center',
            'user-select': 'none',
            'vertical-align': 'middle'
        },

        'click': function() {
            var $div = $(this);

            checked = !checked;

            $div.closest('table').find(':checkbox').prop('checked', checked);
            $div.find('.icon').removeClass('icon-check icon-check-empty').addClass(checked ? 'icon-check-empty' : 'icon-check');
            return false;
        }
    }));

    $result.on('change', ':checkbox', function() {
        $result.find('.actions .action').each(function() {
            var $action= $(this),
                    text = $action.text();

            if ($result.find(':checkbox:checked').length > 0) {
                $action.text(text.replace('All', 'Selected'));
            } else {
                $action.text(text.replace('Selected', 'All'));
            }
        });
    });
});

$doc.onCreate('.contentLock', function() {
    var $container = $(this);

    if ($container.attr('data-content-locked-out') === 'true') {
        $container.find(':input, button, .event-input-disable').trigger('input-disable', [ true ]);
        $win.resize();
    }
});

$doc.onCreate('.inputContainer-listLayoutItemContainer-embedded', function() {
    var $item = $(this),
            expanded;

    $item.append($('<span/>', {
        'class': 'inputContainer-listLayoutItemContainer_expand',
        'click': function() {
            var $container = $item.offsetParent();

            expanded = !expanded;

            if (expanded) {
                $item.css({
                    'left': 10,
                    'position': 'absolute',
                    'top': 10,
                    'z-index': 1
                });

                $item.outerWidth($container.outerWidth() - 20);
                $item.outerHeight($container.outerHeight() - 20);
                $item.addClass('inputContainer-listLayoutItemContainer-embedded-expanded');

            } else {
                $item.attr('style', '');
                $item.removeClass('inputContainer-listLayoutItemContainer-embedded-expanded');
            }

            $item.trigger('resize');
        }
    }));
});

// Show stack trace when clicking on the exception message.
$doc.delegate('.exception > *', 'click', function() {
    $(this).find('> .stackTrace').toggle();
});

// Soft validation based on suggested sizes.
(function() {
    var TRIM_RE = /^\s+|\s+$/g,
            WHITESPACE_RE = /\s+/;

    function updateWordCount($container, $input, value, placeholder) {
        var minimum = +$input.attr('data-suggested-minimum'),
                maximum = +$input.attr('data-suggested-maximum'),
                cc,
                wc;

        value = (value || placeholder || '').replace(TRIM_RE, '');
        cc = value.length;
        wc = value ? value.split(WHITESPACE_RE).length : 0;

        $container.attr('data-wc-message',
                cc < minimum ? 'Too Short' :
                cc > maximum ? 'Too Long' :
                wc + 'w ' + cc + 'c');
    }

    $doc.delegate(
            '.inputSmall-text :text, .inputSmall-text textarea:not(.richtext)',
            'change.wordCount focus.wordCount input.wordCount',
            $.throttle(100, function() {

        var $input = $(this);

        updateWordCount(
                $input.closest('.inputContainer'),
                $input,
                $input.val(),
                $input.attr('data-count-placeholder') === "true" ? $input.attr('placeholder') : '');
    }));

    $doc.onCreate('.wysihtml5-sandbox', function() {
        var iframe = this,
                $iframe = $(iframe),
                $container = $iframe.closest('.rte-container'),
                $textarea = $container.find('textarea.richtext'),
                $toolbar = $container.find('.rte-toolbar');

        $(iframe.contentDocument).on('input', $.throttle(100, function() {
            if ($textarea.length > 0) {
                var $bodyClone = $(iframe.contentDocument.body).clone();

                $bodyClone.find('del, .rte').remove();
                updateWordCount(
                        $toolbar,
                        $textarea,
                        $bodyClone.text(),
                        $textarea.attr('data-count-placeholder') === "true" ? $textarea.attr('placeholder') : '');
            }
        }));
    });
})();

// Make sure that most elements are always in view.
(function() {
    var lastScrollTop = $win.scrollTop();

    $win.scroll($.throttle(100, function() {
        var scrollTop = $win.scrollTop();

        $('.leftNav, .withLeftNav > .main, .contentForm-aside').each(function() {
            var $element = $(this),
                    elementTop = $element.offset().top,
                    initialElementTop = $element.data('initialElementTop'),
                    windowHeight,
                    elementHeight,
                    alignToTop;

            if ($element.closest('.popup').length > 0) {
                return;
            }

            if (!initialElementTop) {
                initialElementTop = elementTop;
                $element.data('initialElementTop', initialElementTop);
                $element.css({
                    'position': 'relative',
                    'top': 0
                });
            }

            windowHeight = $win.height();
            elementHeight = $element.outerHeight();
            alignToTop = function() {
                $element.stop(true);
                $element.animate({
                    'top': Math.max(scrollTop, 0)
                }, 'fast');
            };

            // The element height is less than the window height,
            // so there's no need to account for the bottom alignment.
            if (initialElementTop + elementHeight < windowHeight) {
                alignToTop();

            // The user is scrolling down.
            } else {
                if (lastScrollTop < scrollTop) {
                    var windowBottom = scrollTop + windowHeight;
                    var elementBottom = elementTop + elementHeight;
                    if (windowBottom > elementBottom) {
                        $element.stop(true);
                        $element.animate({
                            'top': Math.min(windowBottom, $('body').height()) - elementHeight - initialElementTop
                        }, 'fast');
                    }

                // The user is scrolling up.
                } else if (lastScrollTop > scrollTop) {
                    if (elementTop > scrollTop + initialElementTop) {
                        alignToTop();
                    }
                }
            }
        });

        lastScrollTop = scrollTop;
    }));
})();

// Make sure that the label for the focused input is visible.
$doc.delegate(':input', 'focus', function() {
    var $input = $(this),
            $firstInput = $input.closest('form').find('.inputContainer:visible').eq(0),
            $parents = $input.parentsUntil('form');

    $parents.addClass('state-focus');

    $win.bind('scroll.focusLabel', $.run($.throttle(50, function() {
        var focusLabelHeight,
                index,
                $parent,
                headerHeight = $('.toolHeader').outerHeight(),
                labelText = '',
                $focusLabel = $('.focusLabel'),
                $parentLabel,
                parentLabelText;

        if ($focusLabel.length === 0) {
            $focusLabel = $('<div/>', { 'class': 'focusLabel' });

            $(doc.body).append($focusLabel);
        }

        focusLabelHeight = $focusLabel.outerHeight();

        $parents.each(function() {
            $(this).find('> .inputLabel label, > .repeatableLabel').css('visibility', '');
        });

        for (index = $parents.length - 1; index >= 0; -- index) {
            $parent = $($parents[index]);

            if ($parent.offset().top > $win.scrollTop() + (focusLabelHeight * 2 / 3) + headerHeight) {
                if (labelText) {
                    $focusLabel.css({
                        'left': $firstInput.offset().left,
                        'top': headerHeight,
                        'width': $firstInput.outerWidth()
                    });
                    $focusLabel.text(labelText);
                    $focusLabel.show();
                    return;

                } else {
                    break;
                }
            }

            $parentLabel = $parent.find('> .inputLabel label, > .repeatableLabel');
            parentLabelText = $parentLabel.text();

            if (parentLabelText) {
                $parentLabel.css('visibility', 'hidden');

                if (labelText) {
                    labelText += ' \u2192 ';
                }

                labelText += parentLabelText;
            }
        }

        $focusLabel.hide();
    })));
});

$doc.delegate(':input', 'blur', function() {
    $(this).parents('.state-focus').each(function() {
        var $parent = $(this);

        $parent.removeClass('state-focus');
        $parent.find('> .inputLabel label, > .repeatableLabel').css('visibility', '');
    });

    $('.focusLabel').hide();
    $win.unbind('.focusLabel');
});

// Handle file uploads from drag-and-drop.
(function() {
    var docEntered;

    // Show all drop zones when the user initiates drag-and-drop.
    $doc.bind('dragenter', function() {
        var $body,
                $cover;

        if (docEntered) {
            return;
        }

        docEntered = true;
        $body = $(doc.body);

        // Cover is required to detect mouse leaving the window.
        $cover = $('<div/>', {
            'class': 'uploadableCover',
            'css': {
                'left': 0,
                'height': '100%',
                'position': 'fixed',
                'top': 0,
                'width': '100%',
                'z-index': 1999999
            }
        });

        $cover.bind('dragenter dragover', function(event) {
            event.stopPropagation();
            event.preventDefault();
            return false;
        });

        $cover.bind('dragleave', function() {
            docEntered = false;
            $cover.remove();
            $('.uploadableDrop').remove();
            $('.uploadableFile').remove();
        });

        $cover.bind('drop', function(event) {
            event.preventDefault();
            $cover.trigger('dragleave');
            return false;
        });

        $body.append($cover);

        // Valid file drop zones.
        $('.inputContainer .action-upload, .uploadable .uploadableLink').each(function() {
            var $upload = $(this),
                    $container = $upload.closest('.inputContainer, .uploadable'),
                    overlayCss,
                    $dropZone,
                    $dropLink,
                    $fileInputContainer,
                    $fileInput;

            overlayCss = $.extend($container.offset(), {
                'height': $container.outerHeight(),
                'position': 'absolute',
                'width': $container.outerWidth()
            });

            $dropZone = $('<div/>', {
                'class': 'uploadableDrop',
                'css': overlayCss
            });

            $dropLink = $upload.clone();
            $dropLink.text("Drop Files Here");

            $fileInputContainer = $('<div/>', {
                'class': 'uploadableFile',
                'css': $.extend(overlayCss, {
                    'z-index': 2000000
                })
            });

            $fileInput = $('<input/>', {
                'type': 'file',
                'multiple': 'multiple'
            });

            // On file drop, replace the appropriate input.
            $fileInput.one('change', function() {
                var dropLinkOffset = $dropLink.offset(),
                        $frame,
                        replaceFileInput;

                $cover.hide();
                $dropLink.click();
                $fileInputContainer.hide();

                $frame = $('.frame[name="' + $dropLink.attr('target') + '"]');

                // Position the popup over the drop link.
                $frame.popup('source', $upload, {
                    'pageX': dropLinkOffset.left + $dropLink.outerWidth() / 2,
                    'pageY': dropLinkOffset.top + $dropLink.outerHeight()
                });

                // Closing the popup resets the drag-and-drop.
                $frame.popup('container').bind('close', function() {
                    $cover.trigger('dragleave');
                });

                replaceFileInput = function() {
                    var $frameFileInput = $frame.find(':file').eq(0);

                    if ($frameFileInput.length !== 1) {
                        setTimeout(replaceFileInput, 20);

                    } else {
                        $.each([ 'class', 'id', 'name', 'style' ], function(index, name) {
                            $fileInput.attr(name, $frameFileInput.attr(name) || '');
                        });

                        $frameFileInput.after($fileInput);
                        $frameFileInput.remove();
                        $frameFileInput = $fileInput;
                        $frameFileInput.change();
                    }
                };

                replaceFileInput();
            });

            $dropZone.append($dropLink);
            $body.append($dropZone);
            $fileInputContainer.append($fileInput);
            $body.append($fileInputContainer);
        });
    });

    $doc.bind('dragend', function(event) {
        if (docEntered) {
            docEntered = false;
            $('.uploadableCover').remove();
            $('.uploadableDrop').remove();
            $('.uploadableFile').remove();
        }
    });
})();

$doc.on('click', 'button[name="action-delete"], :submit[name="action-delete"]', function() {
    return confirm('Are you sure you want to permanently delete this item?');
});

$doc.on('click', 'button[name="action-trash"], :submit[name="action-trash"]', function() {
    return confirm('Are you sure you want to archive this item?');
});

$doc.on('input-disable', ':input', function(event, disable) {
    $(this).prop('disabled', disable);
});

$doc.onCreate('.inputContainer-readOnly', function() {
    $(this).find(':input').trigger('input-disable', [ true ]);
});

(function() {
    function sync() {
        var $input = $(this),
                $output = $('output[for="' + $input.attr('id') + '"]');

        $output.prop('value', $input.prop('value'));
    }

    $doc.onCreate('input[type="range"]', sync);
    $doc.on('change input', 'input[type="range"]', sync);

    function fix() {
        var $container = $(this).closest('.inputContainer'),
                $inputs = $container.find('.inputVariation input[type="range"]'),
                total,
                max;

        if ($inputs.length === 0) {
            return;
        }

        total = 0.0;
        max = 0.0;

        $inputs.each(function() {
            var $input = $(this),
                    inputMax = parseFloat($input.prop('max'));

            total += parseFloat($input.prop('value'));

            if (max < inputMax) {
                max = inputMax;
            }
        });

        $inputs.each(function() {
            var $input = $(this);

            $input.prop('value', parseFloat($input.prop('value')) / total * max);
            sync.call(this);
        });
    }

    $doc.onCreate('.inputContainer', fix);
    $doc.on('change input', '.inputVariation input[type="range"]', fix);
})();

// Key bindings.
$doc.on('keydown', ':input', function(event) {
    if (event.which === 27) {
        $(this).blur();
    }
});

$doc.on('keypress', function(event) {
    var $searchInput;

    if (event.which === 47 && $(event.target).closest(':input').length === 0) {
        $searchInput = $('.toolSearch .searchInput :text');

        $searchInput.val('');
        $searchInput.focus();
        return false;
    }
});

// Publishing widget behaviors.
$doc.onCreate('.widget-publishing', function() {
    var $widget = $(this),
            $dateInput = $widget.find('.dateInput'),
            $newSchedule = $widget.find('select[name="newSchedule"]'),
            $publishButton = $widget.find('[name="action-publish"]'),
            oldPublishText = $publishButton.text(),
            oldDate = $dateInput.val(),
            onChange;

    // Change the publish button label if scheduling.
    if ($dateInput.length === 0) {
        $publishButton.addClass('schedule');
        $publishButton.text('Schedule');

    } else {
        onChange = function() {
            if ($dateInput.val()) {
                $publishButton.addClass('schedule');
                $publishButton.text(oldDate && !$newSchedule.val() ? 'Reschedule' : 'Schedule');

            } else {
                $publishButton.removeClass('schedule');
                $publishButton.text(oldPublishText);
            }
        };

        onChange();

        $dateInput.change(onChange);
        $newSchedule.change(onChange);
    }

    // Move the widget to the top if within aside section.
    if ($widget.closest('.popup').length > 0) {
        return;
    }

    $widget.closest('.contentForm-aside').each(function() {
        var $aside = $(this),
                asideTop = $aside.offset().top;

        $win.resize($.throttle(100, $.run(function() {
            $widget.css({
                'left': $aside.offset().left,
                'position': 'fixed',
                'top': asideTop,
                'width': $widget.width(),
                'z-index': 1
            });

            // Push other areas down.
            $aside.css('padding-top', $widget.outerHeight(true));
        })));
    });
});

// Create tabs if the publishing widget contains both the workflow
// and the publish areas.
$doc.onCreate('.widget-publishing', function() {
    var $widget = $(this),
            $workflow = $widget.find('.widget-publishingWorkflow'),
            $publish = $widget.find('.widget-publishingPublish'),
            $tabs,
            $tabWorkflow,
            $tabPublish;

    if ($workflow.length === 0 || $publish.length === 0) {
        return;
    }

    $tabs = $('<ul/>', {
        'class': 'tabs'
    });

    $tabWorkflow = $('<li/>', {
        'html': $('<a/>', {
            'text': 'Workflow',
            'click': function() {
                $workflow.show();
                $tabWorkflow.addClass('state-selected');
                $publish.hide();
                $tabPublish.removeClass('state-selected');
                $win.resize();
                return false;
            }
        })
    });

    $tabPublish = $('<li/>', {
        'html': $('<a/>', {
            'text': 'Publish',
            'click': function() {
                $workflow.hide();
                $tabWorkflow.removeClass('state-selected');
                $publish.show();
                $tabPublish.addClass('state-selected');
                $win.resize();
                return false;
            }
        })
    });

    $tabs.append($tabWorkflow);
    $tabs.append($tabPublish);
    $workflow.before($tabs);

    if ($('.widget-publishingWorkflowState').length > 0) {
        $tabWorkflow.find('a').click();

    } else {
        $tabPublish.find('a').click();
    }
});

// Synchronizes main search input with the hidden one in the type select form.
$doc.on('input', '.searchFiltersRest > .searchInput > :text', function() {
    var $input = $(this),
            $otherInput = $input.closest('.searchFilters').find('.searchFiltersType > input[name="' + $input.attr('name') + '"]');

    if ($otherInput.length > 0) {
        $otherInput.val($input.val());
    }
});

$doc.ready(function() {
    $(doc.activeElement).focus();
});

$doc.ready(function() {
    $(this).trigger('create');

    // Add the name of the sub-selected item on the main nav.
    $('.toolNav .selected').each(function() {
        var $selected = $(this),
                $subList = $selected.find('> ul'),
                $subSelected = $subList.find('> .selected > a'),
                $selectedLink;

        if ($subSelected.length > 0) {
            $selectedLink = $selected.find('> a');
            $selectedLink.text($selectedLink.text() + ' \u2192 ' + $subSelected.text());
        }

        $subList.css('min-width', $selected.outerWidth());
    });

    // Don't allow main nav links to be clickable if they have any children.
    $('.toolNav li.isNested > a').click(function() {
        return false;
    });

    // Sync the search input in the tool header with the one in the popup.
    (function() {
        var previousValue;

        $('.toolSearch :text').bind('focus input', $.throttle(500, function(event) {
            var $headerInput = $(this),
                    $headerForm = $headerInput.closest('form'),
                    $searchFrame,
                    $searchInput,
                    headerInputValue = $headerInput.val();

            $headerInput.attr('autocomplete', 'off');
            $searchFrame = $('.frame[name="' + $headerForm.attr('target') + '"]');

            if ($searchFrame.length === 0 ||
                    (event.type === 'focus' &&
                    headerInputValue &&
                    $searchFrame.find('.searchResultList .message-warning').length > 0)) {
                $headerForm.submit();

            } else {
                $searchFrame.popup('open');
                $searchInput = $searchFrame.find('.searchFilters :input[name="q"]');

                if (headerInputValue !== $searchInput.val()) {
                    $searchInput.val(headerInputValue).trigger('input');
                }
            }
        }));

        $('.toolSearch button').bind('click', function() {
            $('.toolSearch :text').focus();
            return false;
        });
    }());

    // Starts all server-side tool checks.
    if (!DISABLE_TOOL_CHECKS) {
        (function() {
            var toolCheckPoll = function() {
                $.ajax({
                    'method': 'post',
                    'url': CONTEXT_PATH + '/toolCheckStream',
                    'cache': false,
                    'dataType': 'json',
                    'data': {
                        'url': win.location.href,
                        'r': JSON.stringify(toolChecks)
                    }

                }).done(function(responses) {
                    if (!responses) {
                        return;
                    }

                    $.each(responses, function(i, response) {
                        if (response) {
                            toolCheckActionCallbacks[i][response.action].call(toolChecks[i], response);
                        }
                    });

                }).done(function() {
                    setTimeout(toolCheckPoll, 100);

                }).fail(function() {
                    setTimeout(toolCheckPoll, 10000);
                });
            };

            toolCheckPoll();
        })();
    }
});

// Content locking functions.
(function() {
    var KEY_PREFIX = "cms.contentLock.",
            STORAGE = window.localStorage;

    window.bspContentLock = function(contentId) {
        STORAGE.setItem(KEY_PREFIX + contentId, +new Date());
    };

    window.bspContentUnlock = function(contentId) {
        STORAGE.removeItem(KEY_PREFIX + contentId);

        $.ajax({
            'url': CONTEXT_PATH + '/contentUnlock',
            'data': { 'id': contentId },
            'async': false,
            'cache': false
        });
    };

    window.setInterval(function() {
        var itemIndex,
                itemLength = STORAGE.length,
                key,
                now = +new Date(),
                contentId;

        for (itemIndex = 0; itemIndex < itemLength; ++ itemIndex) {
            key = STORAGE.key(itemIndex);

            if (key.indexOf(KEY_PREFIX, 0) === 0 &&
                    parseInt(STORAGE.getItem(key), 10) + 5000 < now) {
                contentId = key.substring(KEY_PREFIX.length);

                window.bspContentUnlock(contentId);
            }
        }
    }, 1000);
})();

// Content state.
$(window.document).onCreate('.contentForm, .enhancementForm, .standardForm', function() {
    var $form = $(this),
            updateContentState,
            updateContentStateThrottled,
            changed,
            idleTimeout;

    updateContentState = function(idle, wait) {
        var action,
                questionAt,
                complete,
                end,
                $dynamicTexts;

        action = $form.attr('action');
        questionAt = action.indexOf('?');
        end = +new Date() + 1000;
        $dynamicTexts = $form.find(
                '[data-dynamic-text][data-dynamic-text != ""],' +
                        '[data-dynamic-html][data-dynamic-html != ""],' +
                        '[data-dynamic-placeholder][data-dynamic-placeholder != ""]');

        $.ajax({
            'type': 'post',
            'url': CONTEXT_PATH + 'contentState?idle=' + (!!idle) + (questionAt > -1 ? '&' + action.substring(questionAt + 1) : ''),
            'cache': false,
            'dataType': 'json',

            'data': $form.serialize() + $dynamicTexts.map(function() {
                var $element = $(this);

                return '&_dti=' + ($element.closest('[data-object-id]').attr('data-object-id') || '') +
                        '&_dtt=' + ($element.attr('data-dynamic-text') ||
                        $element.attr('data-dynamic-html') ||
                        $element.attr('data-dynamic-placeholder') ||
                        '');
            }).get().join(''),

            'success': function(data) {
                $form.trigger('cms-updateContentState', [ data ]);

                $dynamicTexts.each(function(index) {
                    var $element = $(this),
                            text = data._dynamicTexts[index];

                    if (text === null) {
                        return;
                    }

                    $element.closest('.message').toggle(text !== '');

                    if ($element.is('[data-dynamic-text]')) {
                        $element.text(text);

                    } else if ($element.is('[data-dynamic-html]')) {
                        $element.html(text);

                    } else if ($element.is('[data-dynamic-placeholder]')) {
                        $element.prop('placeholder', text);
                    }
                });
            },

            'complete': function() {
                complete = true;
            }
        });

        if (wait) {
            while (!complete) {
                if (+new Date() > end) {
                    break;
                }
            }
        }
    };

    updateContentStateThrottled = $.throttle(100, updateContentState);

    updateContentStateThrottled();

    $form.bind('change input', function() {
        updateContentStateThrottled();

        clearTimeout(idleTimeout);

        changed = true;
        idleTimeout = setTimeout(function() {
            updateContentStateThrottled(true);
        }, 2000);
    });

    $(window).bind('beforeunload', function() {
        if (changed) {
            updateContentState(true, true);
        }
    });
});

}(jQuery, window));
