define([
    'jquery',
    'jquery.extra',
    'wysihtml5-0.3.0' ],

function($) {
    var win = window;
    var undef;
    var $win = $(win),
            doc = win.document,
            $doc = $(doc),
            targetIndex = 0,
            ZERO_WIDTH_SPACE = '\u200b';

    function getContentEnhancementTarget() {
        ++ targetIndex;
        return 'contentEnhancement-' + targetIndex;
    }

    $.each(CSS_CLASS_GROUPS, function() {
        var command = 'cms-' + this.internalName;
        var prefix = command + '-';
        var regex = new RegExp(prefix + '[0-9a-z\-]+', 'g');

        wysihtml5.commands[command] = {
            'exec': function(composer, command, optionsString) {
                var options = optionsString ? $.parseJSON(optionsString) : { };
                var tag = options.tag || 'span';
                var format = tag === 'span' ? 'formatInline' : 'formatBlock';
                return wysihtml5.commands[format].exec(composer, command, tag, prefix + options.internalName, regex);
            },

            'state': function(composer, command, optionsString) {
                var options = optionsString ? $.parseJSON(optionsString) : { };
                var tag = options.tag || 'span';
                var format = tag === 'span' ? 'formatInline' : 'formatBlock';
                return wysihtml5.commands[format].state(composer, command, tag , prefix + options.internalName, regex);
            }
        };
    });

    var $createToolbarGroup = function(label) {
        var $group = $('<span/>', { 'class': 'rte-group', 'data-group-name': label });
        var $label = $('<span/>', { 'class': 'rte-group-label', 'text': label });
        var $buttons = $('<span/>', { 'class': 'rte-group-buttons' });
        $group.append($label);
        $group.append($buttons);
        return $group;
    };

    var $createToolbarCommand = function(label, command) {
        return $('<span/>', {
            'class': 'rte-button rte-button-' + command,
            'data-wysihtml5-command': command,
            'text': label
        });
    };

    var createToolbar = function(rte, inline, firstDraft, finalDraft) {
        var $container = $('<div/>', { 'class': 'rte-toolbar-container' });

        var $toolbar = $('<div/>', { 'class': 'rte-toolbar' });
        $container.append($toolbar);

        var $font = $createToolbarGroup('Font');
        $toolbar.append($font);
        $font = $font.find('.rte-group-buttons');

        $font.append($createToolbarCommand('Bold', 'bold'));
        $font.append($createToolbarCommand('Italic', 'italic'));
        $font.append($createToolbarCommand('Underline', 'underline'));
        $font.append($createToolbarCommand('Strike', 'strike'));
        $font.append($createToolbarCommand('Superscript', 'superscript'));
        $font.append($createToolbarCommand('Subscript', 'subscript'));

        $.each(CSS_CLASS_GROUPS, function() {
            var $group = $createToolbarGroup(this.displayName);
            var command = 'cms-' + this.internalName;

            if (this.dropDown) {
                $group.addClass('rte-group-dropDown');
            }

            $group.addClass('rte-group-cssClass');
            $group.addClass('rte-group-cssClass-' + this.internalName);
            $toolbar.append($group);
            $group = $group.find('.rte-group-buttons');

            $.each(this.cssClasses, function() {
                var $cssClass = $createToolbarCommand(this.displayName, command);
                $cssClass.attr('data-wysihtml5-command-value', JSON.stringify(this));
                $group.append($cssClass);
            });
        });

        if (!inline) {
            var $alignment = $createToolbarGroup('Alignment') ;
            $toolbar.append($alignment);
            $alignment = $alignment.find('.rte-group-buttons');

            $alignment.append($createToolbarCommand('Justify Left', 'textAlign').attr('data-wysihtml5-command-value', 'left'));
            $alignment.append($createToolbarCommand('Justify Center', 'textAlign').attr('data-wysihtml5-command-value', 'center'));
            $alignment.append($createToolbarCommand('Justify Right', 'textAlign').attr('data-wysihtml5-command-value', 'right'));

            var $list = $createToolbarGroup('List');
            $toolbar.append($list);
            $list = $list.find('.rte-group-buttons');

            $list.append($createToolbarCommand('Unordered List', 'insertUnorderedList'));
            $list.append($createToolbarCommand('Ordered List', 'insertOrderedList'));
        }

        var $enhancement = $createToolbarGroup('Enhancement');
        $toolbar.append($enhancement);
        $enhancement = $enhancement.find('.rte-group-buttons');

        $enhancement.append($createToolbarCommand('Link', 'link'));

        if (!inline) {
            $enhancement.append($createToolbarCommand('Enhancement', 'insertEnhancement'));
            $enhancement.append($createToolbarCommand('Marker', 'insertMarker'));
        }

        if (win.cmsRteImportOptions && win.cmsRteImportOptions.length > 0) {
            var $importGroup = $createToolbarGroup('Import');

            $importGroup.addClass('rte-group-dropDown');
            $toolbar.append($importGroup);

            $importGroup = $importGroup.find('.rte-group-buttons');

            $.each(win.cmsRteImportOptions, function(i, importOptions) {
                $importGroup.append($('<span/>', {
                    'class': 'rte-button rte-button-import',
                    'text': importOptions.name,
                    'click': function() {
                        var $button = $(this);

                        google.load('picker', '1', {
                            'callback': function() {
                                new google.picker.PickerBuilder().
                                        enableFeature(google.picker.Feature.NAV_HIDDEN).
                                        setAppId(importOptions.clientId).
                                        setOAuthToken(importOptions.accessToken).
                                        addView(google.picker.ViewId.DOCUMENTS).
                                        setCallback(function(data) {
                                            if (data[google.picker.Response.ACTION] === google.picker.Action.PICKED) {
                                                $.ajax({
                                                    'method': 'get',
                                                    'url': '/social/googleDriveFile',
                                                    'data': { 'id': data[google.picker.Response.DOCUMENTS][0][google.picker.Document.ID] },
                                                    'cache': false,
                                                    'success': function(data) {
                                                        rte.composer.setValue(data, true);
                                                        rte.composer.parent.updateOverlay();
                                                    }
                                                });
                                            }
                                        }).
                                        build().
                                        setVisible(true);
                            }
                        });
                    }
                }));
            });
        }

        var $changes = $createToolbarGroup('Changes');
        $toolbar.append($changes);
        $changes = $changes.find('.rte-group-buttons');

        $changes.append($createToolbarCommand('Track', 'changesTrack'));
        $changes.append($createToolbarCommand('Accept', 'changesAccept'));
        $changes.append($createToolbarCommand('Reject', 'changesReject'));
        $changes.append($createToolbarCommand('Preview', 'changesPreview'));

        var $comment = $createToolbarGroup('Comments');
        $toolbar.append($comment);
        $comment = $comment.find('.rte-group-buttons');

        $comment.append($createToolbarCommand('Comment', 'commentAdd'));
        $comment.append($createToolbarCommand('Collapse', 'commentCollapse'));
        $comment.append($createToolbarCommand('Remove', 'commentRemove'));

        var $misc = $createToolbarGroup('Misc');
        $toolbar.append($misc);
        $misc = $misc.find('.rte-group-buttons');

        $misc.append($('<span/>', {
            'class': 'rte-button rte-button-html',
            'data-wysihtml5-action': 'change_view',
            'text': 'HTML'
        }));

        $misc.append($createToolbarCommand('Fullscreen', 'fullscreen'));

        return $container[0];
    };

    var $createEnhancementAction = function(label, action) {
        return $('<span/>', {
            'class': 'rte-button rte-button-' + action,
            'data-action': action,
            'text': label
        });
    };

    var createEnhancement = function(rte) {
        var $enhancement = $('<div/>', { 'class': 'rte-enhancement' });

        var $toolbar = $('<div/>', { 'class': 'rte-toolbar' });
        $enhancement.append($toolbar);

        var $position = $createToolbarGroup('Position');
        $toolbar.append($position);

        $position.append($createEnhancementAction('Move Up', 'moveUp'));
        $position.append($createEnhancementAction('Move Down', 'moveDown'));

        $position.append($createEnhancementAction('Move Left', 'moveLeft'));
        $position.append($createEnhancementAction('Move Center', 'moveCenter'));
        $position.append($createEnhancementAction('Move Right', 'moveRight'));

        var $imageSize = $createToolbarGroup('Image Size');
        $imageSize.addClass('rte-group-dropDown');
        $toolbar.append($imageSize);
        $imageSize = $imageSize.find('.rte-group-buttons');

        var sizes = $(rte.container).closest('.inputContainer').attr('data-standard-image-sizes');

        if (sizes) {
            sizes = ' ' + sizes + ' ';
        }

        $imageSize.append($createEnhancementAction('None', 'imageSize-'));

        $.each(STANDARD_IMAGE_SIZES, function() {
            if (sizes.indexOf(' ' + this.internalName + ' ') > -1) {
                $imageSize.append($createEnhancementAction(this.displayName, 'imageSize-' + this.internalName));
            }
        });

        var $misc = $createToolbarGroup('Misc');
        $toolbar.append($misc);

        $misc.append($('<span/>', {
            'class': 'rte-button rte-button-enhancementSelect',
            'html': $('<a/>', {
                'href': CONTEXT_PATH + '/enhancementSelect',
                'target': getContentEnhancementTarget(),
                'text': 'Select'
            })
        }));

        $misc.append($('<span/>', {
            'class': 'rte-button rte-button-enhancementEdit',
            'html': $('<a/>', {
                'href': CONTEXT_PATH + '/content/enhancement.jsp',
                'target': getContentEnhancementTarget(),
                'text': 'Edit'
            })
        }));

        $misc.append($createEnhancementAction('Remove', 'remove'));
        $misc.append($createEnhancementAction('Restore', 'restore'));
        $misc.append($createEnhancementAction('Remove Completely', 'removeCompletely'));

        $enhancement.append($('<div/>', { 'class': 'rte-enhancement-label' }));

        return $enhancement[0];
    };

    var createMarker = function() {
        var $marker = $('<div/>', { 'class': 'rte-enhancement rte-marker' });

        var $toolbar = $('<div/>', { 'class': 'rte-toolbar' });
        $marker.append($toolbar);

        var $position = $createToolbarGroup('Position');
        $toolbar.append($position);

        $position.append($createEnhancementAction('Move Up', 'moveUp'));
        $position.append($createEnhancementAction('Move Down', 'moveDown'));

        var $misc = $createToolbarGroup('Misc');
        $toolbar.append($misc);

        $misc.append($('<span/>', {
            'class': 'rte-button rte-button-selectMarker',
            'html': $('<a/>', {
                'href': CONTEXT_PATH + '/content/marker.jsp',
                'target': getContentEnhancementTarget(),
                'text': 'Select'
            })
        }));

        $misc.append($createEnhancementAction('Remove', 'remove'));
        $misc.append($createEnhancementAction('Restore', 'restore'));
        $misc.append($createEnhancementAction('Really Remove', 'removeCompletely'));

        $marker.append($('<div/>', { 'class': 'rte-enhancement-label' }));

        return $marker[0];
    };

    // Wrap wysihtml5 to add functionality.
    var rtes = [ ],
            getEnhancementId,
            keepToolbarInView;

    (function() {
        var enhancementIndex = 0;

        getEnhancementId = function(placeholder) {
            var enhancementId = $.data(placeholder, 'rte-enhancement-id');

            if (!enhancementId) {
                ++ enhancementIndex;
                enhancementId = 'rte-enhancement-' + enhancementIndex;
                $.data(placeholder, 'rte-enhancement-id', enhancementId);
            }

            return enhancementId;
        };
    })();

    var Rte = wysihtml5.Editor.extend({

        'constructor': function(originalTextarea, config) {
            var rte = this,
                    firstDraft = $(originalTextarea).attr('data-first-draft') === 'true',
                    finalDraft = $(originalTextarea).attr('data-final-draft') === 'true';

            // Create container.
            var container = this.container = doc.createElement('div');
            container.className = 'rte-container';
            originalTextarea.parentNode.insertBefore(container, originalTextarea);

            // Create overlay.
            var overlay = this.overlay = doc.createElement('div');
            overlay.className = 'rte-overlay';
            overlay.style.position = 'relative';
            overlay.style.left = '0px';
            overlay.style.top = '0px';
            container.appendChild(overlay);

            // Changes the focus method to retain the scrolling position.
            (function() {
                var originalFocus = rte.focus;

                rte.focus = function() {
                    var scrollTop = $win.scrollTop();

                    originalFocus.apply(rte, arguments);
                    $win.scrollTop(scrollTop);
                };
            })();

            $(overlay).on('click', function() {
                rte.focus();
            });

            // Create toolbar?
            if (typeof config.toolbar === 'function') {
                config.toolbar = config.toolbar(rte, config.inline, firstDraft, finalDraft);
                container.appendChild(config.toolbar);
            }

            // Handle toolbar action clicks.
            $(overlay).delegate('[data-action]', 'click', function() {
                var $button = $(this);
                var $enhancement = $button.closest('.rte-enhancement');
                var $placeholder = $enhancement.data('$rte-placeholder');
                var $editButtonAnchor = $enhancement.find('.rte-button-editEnhancement a');
                var action = $button.attr('data-action');
                var refData, refDataString, href;

                if (action.indexOf('imageSize-') === 0) {
                    var imageSize = action.substring(10);

                    refData = $.parseJSON($placeholder.attr('data-reference') || '{}');

                    if (imageSize) {
                        $enhancement.attr('data-image-size', imageSize);
                        refData['imageSize'] = imageSize;
                    } else {
                        $enhancement.removeAttr('data-image-size');
                        delete refData['imageSize'];
                    }

                    refDataString = JSON.stringify(refData);
                    $placeholder.attr('data-reference', refDataString);

                } else if (action == 'remove') {
                    $enhancement.addClass('state-removing');
                    $placeholder.addClass('state-removing');

                } else if (action == 'restore') {
                    $enhancement.removeClass('state-removing');
                    $placeholder.removeClass('state-removing');

                } else if (action == 'removeCompletely') {
                    $enhancement.remove();
                    $placeholder.remove();

                } else if (action === 'moveCenter' || action === 'moveLeft' || action === 'moveRight') {

                    // update css attribute
                    $placeholder.removeAttr('data-alignment');
                    if (action === 'moveCenter') {
                        $placeholder.removeAttr('data-alignment');

                    } else if (action === 'moveLeft') {
                        $placeholder.attr('data-alignment', 'left');

                    } else if (action === 'moveRight') {
                        $placeholder.attr('data-alignment', 'right');
                    }

                    // update main ref attribute
                    refData = $.parseJSON($placeholder.attr('data-reference') || '{}');
                    if (action === 'moveCenter') {
                        delete refData['alignment'];

                    } else if (action === 'moveLeft') {
                        refData['alignment'] = 'left';

                    } else if (action === 'moveRight') {
                        refData['alignment'] = 'right';
                    }

                    refDataString = JSON.stringify(refData);
                    $placeholder.attr('data-reference', refDataString);

                    // update edit button link URL
                    if ($editButtonAnchor) {
                        href = $editButtonAnchor.attr('href');

                        href = (href+'&').replace(/([?&])reference=[^&]*[&]/, '$1');
                        href += 'reference=' + (refDataString || '');

                        $editButtonAnchor.attr('href', href);
                    }

                } else {
                    var oldTop = $placeholder.offset().top;
                    var $parent, $prev, $next;

                    if (action === 'moveDown') {
                        $placeholder.closest('body').find('br + br, div, h1, h2, h3, h4, h5, h6, p, button').each(function() {
                            if ($placeholder[0].compareDocumentPosition(this) & Node.DOCUMENT_POSITION_FOLLOWING) {
                                $(this).after($placeholder);
                                return false;
                            }
                        });

                        $win.scrollTop($win.scrollTop() + $placeholder.offset().top - oldTop);

                    } else if (action === 'moveUp') {
                        var precedings = [ ],
                                precedingsLength;

                        $placeholder.closest('body').find('br + br, div, h1, h2, h3, h4, h5, h6, p, button').each(function() {
                            if ($placeholder[0].compareDocumentPosition(this) & Node.DOCUMENT_POSITION_PRECEDING) {
                                precedings.push(this);
                            }
                        });

                        precedingsLength = precedings.length;

                        if (precedingsLength >= 2) {
                            $(precedings[precedingsLength - 2]).after($placeholder);

                        } else {
                            $placeholder.closest('body').prepend($placeholder);
                        }

                        $win.scrollTop($win.scrollTop() + $placeholder.offset().top - oldTop);
                    }
                }

                rte.updateOverlay();

                return false;
            });

            // Create dialogs.
            var $dialogs = $('<div/>', {
                'class': 'rte-dialogs',
                'css': {
                    'left': 0,
                    'position': 'relative',
                    'top': 0
                }
            });

            var $linkDialog = $(
                    '<div>' +
                        '<h2>Link</h2>' +
                        '<div class="rte-dialogLine">' +
                            '<input type="text" class="rte-dialogLinkHref">' +
                            '<input type="hidden" class="rte-dialogLinkId">' +
                            '<a class="rte-dialogLinkContent" target="linkById" href="' + CONTEXT_PATH + '/content/linkById.jsp?p=true">Content</a>' +
                        '</div>' +
                        '<div class="rte-dialogLine">' +
                            '<select class="rte-dialogLinkTarget">' +
                                '<option value="">Same Window</option>' +
                                '<option value="_blank">New Window</option>' +
                            '</select>' +
                            ' <select class="rte-dialogLinkRel">' +
                                '<option value="">Relation</option>' +
                                '<option value="nofollow">nofollow</option>' +
                            '</select>' +
                        '</div>' +
                        '<a class="rte-dialogLinkSave">Save</a>' +
                        ' <a class="rte-dialogLinkOpen" target="_blank">Open</a>' +
                        ' <a class="rte-dialogLinkUnlink">Unlink</a>' +
                    '</div>');

            var $lastAnchor = $();

            $linkDialog.on('click', '.rte-dialogLinkSave', function() {
                var cmsId = $linkDialog.find('.rte-dialogLinkId').val(),
                        href = $linkDialog.find('.rte-dialogLinkHref').val() || '',
                        target = $linkDialog.find('.rte-dialogLinkTarget').val(),
                        rel = $linkDialog.find('.rte-dialogLinkRel').val();

                if (cmsId) {
                    $lastAnchor.attr('data-cms-id', cmsId);
                    $lastAnchor.attr('data-cms-href', href);

                } else {
                    $lastAnchor.removeAttr('data-cms-id');
                    $lastAnchor.removeAttr('data-cms-href');
                }

                $lastAnchor.attr('href', href);

                if (target) {
                    $lastAnchor.attr('target', target);

                } else {
                    $lastAnchor.removeAttr('target');
                }

                if (rel) {
                    $lastAnchor.attr('rel', rel);

                } else {
                    $lastAnchor.removeAttr('rel');
                }

                $linkDialog.popup('close');
            });

            $linkDialog.on('keydown', '.rte-dialogLinkHref', function(event) {
                if (event.which === 13) {
                    $linkDialog.find('.rte-dialogLinkSave').click();
                    return false;

                } else {
                    return true;
                }
            });

            $linkDialog.on('input', '.rte-dialogLinkHref', function(event) {
                $linkDialog.find('.rte-dialogLinkOpen').attr('href', $(event.target).val());
            });

            function removeLink() {
                var $ins,
                        $del;

                if ($(rte.composer.element).hasClass('rte-changesTracking')) {
                    $ins = $lastAnchor.parent('ins');

                    if ($ins.length > 0) {
                        $del = $ins.prev('del');

                        if ($del.length > 0 && $del.text() === $lastAnchor.text()) {
                            $ins.after($lastAnchor.html());
                            $ins.remove();
                            $del.remove();
                            return;
                        }
                    }

                    $del = $('<del/>');

                    $lastAnchor.after($del);
                    $del.append($lastAnchor);
                    $del.after($('<ins/>', {
                        'html': $lastAnchor.html()
                    }));

                } else {
                    $lastAnchor.after($lastAnchor.html());
                    $lastAnchor.remove();
                }
            }

            $linkDialog.on('click', '.rte-dialogLinkUnlink', function() {
                removeLink();
                $linkDialog.popup('close');
            });

            $(doc.body).append($linkDialog);
            $linkDialog.popup();
            $linkDialog.popup('close');

            $linkDialog.popup('container').bind('close', function() {
                if (!$lastAnchor.attr('href')) {
                    removeLink();
                }
            });

            var openLinkDialog = function($anchor) {
                var composerOffset = $(rte.composer.iframe).offset(),
                        $href = $linkDialog.find('.rte-dialogLinkHref'),
                        $popup,
                        popupWidth,
                        anchorOffset = $anchor.offset(),
                        left,
                        leftDelta;

                $lastAnchor = $anchor;

                $linkDialog.popup('open');
                $href.val($anchor.attr('href') || 'http://');
                $linkDialog.find('.rte-dialogLinkId').val($anchor.attr('data-cms-id') || '');
                $linkDialog.find('.rte-dialogLinkTarget').val($anchor.attr('target') || '');
                $linkDialog.find('.rte-dialogLinkRel').val($anchor.attr('rel') || '');
                $linkDialog.find('.rte-dialogLinkOpen').attr('href', $href.val());
                $href.focus();

                $popup = $linkDialog.popup('container');
                popupWidth = $popup.outerWidth();
                left = anchorOffset.left + ($anchor.outerWidth() - popupWidth) / 2;

                if (left < 35) {
                    left = 35;

                } else {
                    leftDelta = left + popupWidth - $(doc).width() + 35;

                    if (leftDelta > 0) {
                        left -= leftDelta;
                    }
                }

                $popup.css({
                    'left': left,
                    'margin-left': 0,
                    'position': 'absolute',
                    'top': composerOffset.top + $anchor.offset().top + $anchor.outerHeight()
                });
            };

            (function() {
                var tempIndex = 0;

                $(config.toolbar).find('.rte-button-link').click(function() {
                    var selection,
                            tempClass,
                            $anchor,
                            $ins,
                            $del;

                    selection = rte.composer.selection;
                    $anchor = $(selection.getSelectedNode()).closest('a');

                    if ($anchor.length > 0) {
                        openLinkDialog($anchor);
                        return;
                    }

                    if (selection.getRange().collapsed) {
                        return;
                    }

                    ++ tempIndex;
                    tempClass = 'rte-link-temp-' + tempIndex;

                    wysihtml5.commands.createLink.exec(rte.composer, 'createLink', { 'class': tempClass });

                    $anchor = $('a.' + tempClass, rte.composer.element);

                    $anchor.removeClass(tempClass);

                    if ($(rte.composer.element).hasClass('rte-changesTracking')) {
                        if ($anchor.parent('ins').length > 0) {
                            openLinkDialog($anchor);
                            return;
                        }

                        $anchor.before($('<del/>', {
                            'html': $anchor.html()
                        }));

                        $ins = $('<ins/>');

                        $anchor.after($ins);
                        $ins.append($anchor);
                    }

                    openLinkDialog($anchor);

                    return false;
                });
            })();

            // Initialize wysihtml5.
            container.appendChild(originalTextarea);
            originalTextarea.className += ' rte-textarea';

            rtes[rtes.length] = this;
            this.base(originalTextarea, config);

            var getSelectedElement = function() {
                var range = rte.composer.selection.getRange(),
                        selected = rte.composer.selection.getSelectedNode(),
                        $next;

                if (range.collapsed &&
                        selected.nodeType == 3 &&
                        selected.length == range.endOffset) {
                    $next = $(selected.nextSibling);

                    if ($next.is('code, del, ins')) {
                        return $next;
                    }
                }

                while (!selected.tagName) {
                    selected = selected.parentNode;
                }

                return $(selected);
            };

            this.observe('show:dialog', function(options) {
                var $selected = getSelectedElement(),
                        selectedOffset = $selected.offset(),
                        $dialog = $(options.dialogContainer);

                $dialog.css({
                    'left': selectedOffset.left,
                    'position': 'absolute',
                    'top': selectedOffset.top + $selected.outerHeight() + 10
                });
            });

            this.observe('load', function() {
                function removeDelIns($container) {
                    $container.find('del, ins').each(function() {
                        var $delOrIns = $(this);

                        $delOrIns.after($delOrIns.html());
                        $delOrIns.remove();
                    });
                }

                $(rte.composer.element).on('cut', function(event) {
                    if (!$(rte.composer.element).hasClass('rte-changesTracking')) {
                        return;
                    }

                    var range = composer.selection.getRange();
                    var $del = $('<del/>');

                    $del.append(range.nativeRange.cloneContents());
                    removeDelIns($del);
                    setTimeout(function() {
                        rte.composer.selection.getRange().nativeRange.insertNode($del[0]);
                    }, 0);
                });

                var pasteRange;

                $(rte.composer.element).on('paste', function(event) {
                    if (!$(rte.composer.element).hasClass('rte-changesTracking')) {
                        return;
                    }

                    pasteRange = composer.selection.getRange().nativeRange;
                });

                rte.on('paste', function() {
                    if (!$(rte.composer.element).hasClass('rte-changesTracking')) {
                        return;
                    }

                    if (pasteRange) {
                        var range = composer.selection.getRange();
                        var $ins = $('<ins/>');

                        pasteRange.setEnd(range.endContainer, range.endOffset);
                        $ins.append(pasteRange.extractContents());
                        removeDelIns($ins);
                        composer.selection.getRange().nativeRange.insertNode($ins[0]);
                        pasteRange = null;
                    }
                });

                // Restore track changes state.
                if (window.sessionStorage.getItem('bsp.rte.changesTracking.' + $(rte.textarea.element).closest('.inputContainer').attr('data-name'))) {
                    wysihtml5.commands.changesTrack.exec(rte.composer);
                    rte.toolbar._updateLinkStates();
                }

                // Make sure placeholder BUTTONs are replaced with enhancement SPANs.
                var convertNodes = function(parent, oldTagName, newTagName, callback) {
                    var childNodes = parent.childNodes;

                    if (parent.childNodes) {
                        var childNodesLength = childNodes.length;

                        for (var i = 0; i < childNodesLength; ++ i) {
                            var node = childNodes[i];

                            if (node &&
                                    node.tagName &&
                                    node.tagName === oldTagName &&
                                    wysihtml5.dom.hasClass(node, 'enhancement')) {
                                var newNode = parent.ownerDocument.createElement(newTagName);
                                var nodeAttributes = node.attributes;
                                var nodeAttributesLength = nodeAttributes.length;

                                for (var j = 0; j < nodeAttributesLength; ++ j) {
                                    var attribute = nodeAttributes[j];
                                    newNode.setAttribute(attribute.name, attribute.value);
                                }

                                if (callback) {
                                    callback(newNode);
                                }

                                parent.insertBefore(newNode, node);
                                parent.removeChild(node);

                            } else {
                                convertNodes(node, oldTagName, newTagName, callback);
                            }
                        }
                    }
                };

                var textarea = this.textarea;
                var lastTextareaValue;

                if (!$(textarea.element).val()) {
                    $(textarea.element).val('<br>');
                }

                textarea._originalGetValue = textarea.getValue;
                textarea.getValue = function(parse) {
                    var value = this._originalGetValue(parse),
                            dom;

                    if (lastTextareaValue === value) {
                        return value.replace(ZERO_WIDTH_SPACE, '');

                    } else {
                        lastTextareaValue = value;
                        dom = wysihtml5.dom.getAsDom(value, this.element.ownerDocument);
                        convertNodes(dom, 'SPAN', 'BUTTON');
                        return dom.innerHTML.replace(ZERO_WIDTH_SPACE, '');
                    }
                };

                var composer = this.composer;
                var lastComposerValue;

                composer._originalGetValue = composer.getValue;
                composer.getValue = function(parse) {
                    var value = this._originalGetValue(parse);

                    if (lastComposerValue === value) {
                        return value;

                    } else {
                        lastComposerValue = value;
                        dom = wysihtml5.dom.getAsDom(value, this.element.ownerDocument);
                        convertNodes(dom, 'BUTTON', 'SPAN', function(node) {
                            node.innerHTML = 'Enhancement';
                        });
                        return dom.innerHTML;
                    }
                };

                composer.setValue(textarea.getValue());

                // Some style clean-ups.
                composer.iframe.style.overflow = 'hidden';
                composer.iframe.contentDocument.body.style.overflow = 'hidden';
                composer.iframe.contentDocument.body.className += ' rte-loaded';
                textarea.element.className += ' rte-source';

                this.on('focus', function() {

                    $(textarea.element).parentsUntil('form').addClass('state-focus');

                    for (var i = 0, length = rtes.length; i < length; ++ i) {
                        var rte = rtes[i];

                        $(rte.container).css('padding-top', 0);
                        $(rte.config.toolbar).attr('style', rte._toolbarOldStyle);
                        $(rte.overlay).css('top', 0);
                        rte._toolbarOldStyle = null;
                    }

                    $(this.overlay).css('top', $(this.config.toolbar).outerHeight());
                    keepToolbarInView();
                });

                // When we focus or blur the rich text editor, trigger an event on the textarea
                // so we can notify the caller
                
                this.on('focus', function(){
                    $(textarea.element).trigger('rtefocus');
                });
                
                this.on('blur', function(){
                    $(textarea.element).trigger('rteblur');
                });
                
                // Hack to make sure that the proper focus fires when clicking
                // on an 'empty' region.
                $(composer.iframe.contentWindow).on('focus', function() {
                    rte.focus();
                });

                this.on('blur', function() {
                    $(textarea.element).parentsUntil('form').removeClass('state-focus');
                    $(textarea.element).trigger('change');
                });

                $(composer.element).on('click', 'a', function() {
                    return false;
                });

                $(composer.element).on('dblclick', 'a', function(event) {
                    openLinkDialog($(event.target).closest('a'));
                });

                // Track changes.
                (function() {
                    var down = false,
                            downRange,
                            downLength = -1,
                            downContents,
                            tempIndex = 0;

                    function wrapCurrentSelectionRange(tag) {
                        var tempClass,
                                $element;

                        if (!wysihtml5.commands.formatInline.state(composer, null, tag)) {
                            ++ tempIndex;
                            tempClass = 'rte-wrap-temp-' + tempIndex;

                            wysihtml5.commands.formatInline.exec(composer, null, tag, tempClass, /foobar/g);

                            $element = $(tag + '.' + tempClass, composer.element);

                            if ($element.length > 0) {
                                $element.removeClass(tempClass);
                            }
                        }

                        return $element;
                    }

                    function cleanUp() {
                        var $composerElement = $(composer.element),
                                merged;

                        $composerElement.find('ins del').remove();

                        // Move <ins> elements out of <del> elements.
                        $composerElement.find('del ins').each(function() {
                            var $ins = $(this),
                                    $del = $ins.closest('del');

                            $del.after($ins);
                        });

                        // Flatten <del> and <ins> elements.
                        $composerElement.find('del:has(del), ins:has(ins)').each(function() {
                            var $element = $(this);

                            $element.text($element.text());
                        });

                        // Remove empty <del> and <ins> elements.
                        $composerElement.find('del, ins').each(function() {
                            var $current = $(this),
                                    firstLetter = $current.text()[0];

                            if (!firstLetter || firstLetter.charCodeAt(0) === 65279) {
                                $current.remove();
                            }
                        });

                        // Merge consecutive <del> and <ins> elements.
                        do {
                            merged = false;

                            $composerElement.find('del, ins').each(function() {
                                var $current = $(this),
                                        currentTagName,
                                        next,
                                        $next;

                                if ($.contains(composer.element, $current[0])) {
                                    currentTagName = this.tagName;
                                    next = this;

                                    while (!!(next = next.nextSibling) &&
                                            next.nodeType === 1 &&
                                            next.tagName === currentTagName) {
                                        merged = true;
                                        $next = $(next);

                                        $current.append($next.html());
                                        $next.remove();
                                    }
                                }
                            });
                        } while (merged);
                    }

                    $(composer.element).bind('keydown', function(event) {
                        var which,
                                selection,
                                $comment,
                                spacer,
                                isBackspace,
                                isDelete,
                                range,
                                text,
                                comment,
                                $delOrIns,
                                cursorHack;

                        if (event.metaKey) {
                            return true;
                        }

                        which = event.which;
                        selection = composer.selection;
                        $comment = $(selection.getRange().commonAncestorContainer).closest('.rte-comment');

                        if ($comment.length > 0) {
                            if (which === 13) {
                                spacer = composer.doc.createTextNode('\u00a0');

                                $comment.after(spacer);
                                selection.setBefore(spacer);
                            }

                            return true;
                        }

                        isBackspace = which === 8;
                        isDelete = which === 46;

                        if (isBackspace || isDelete) {
                            range = selection.getRange();

                            if (range.collapsed) {
                                text = range.startContainer;
                                comment = null;

                                if (isBackspace) {
                                    $delOrIns = $(text).closest('del, ins');

                                    if ($delOrIns.length > 0 &&
                                            range.startOffset === 0) {
                                        comment = $delOrIns[0].previousSibling;

                                    } else if (text.nodeType === Node.TEXT_NODE &&
                                            (range.startOffset === 0 ||
                                            (range.startOffset === 1 &&
                                            text.nodeValue.substring(0, 1) === ZERO_WIDTH_SPACE))) {
                                        comment = text.previousSibling;
                                    }

                                } else {
                                    if (text.nodeType === Node.TEXT_NODE &&
                                            text.nodeValue.length === range.startOffset) {
                                        comment = text.nextSibling;
                                    }
                                }

                                if (comment && comment.nodeType === Node.ELEMENT_NODE) {
                                    $comment = $(comment);

                                    if ($comment.hasClass('rte-comment')) {
                                        $comment.remove();
                                        return false;
                                    }
                                }
                            }
                        }

                        if (!$(composer.element).hasClass('rte-changesTracking')) {
                            $delOrIns = $(selection.getSelectedNode()).closest('del, ins');

                            if ($delOrIns.length > 0) {
                                cursorHack = $delOrIns[0].ownerDocument.createTextNode(ZERO_WIDTH_SPACE);

                                $delOrIns.after(cursorHack);
                                selection.setAfter(cursorHack);
                            }

                            return true;
                        }

                        function doDelete(direction) {
                            var rangySelection = selection.getSelection(),
                                    range,
                                    container;

                            if (selection.getRange().collapsed) {
                                rangySelection.nativeSelection.modify('extend', direction, 'character');
                            }

                            wrapCurrentSelectionRange('del');

                            if (direction === 'forward') {
                                rangySelection.collapseToEnd();

                                range = selection.getRange();
                                container = range.endContainer;

                                if (!container.nextSibling &&
                                        container.nodeType === Node.TEXT_NODE &&
                                        range.endOffset === container.data.length) {
                                    selection.setAfter($(container).closest('del')[0]);
                                    selection.getSelection().nativeSelection.modify('move', 'backward', 'character');
                                    selection.getSelection().nativeSelection.modify('move', 'forward', 'character');
                                }

                            } else {
                                rangySelection.collapseToStart();

                                range = selection.getRange();
                                container = range.startContainer;

                                if (!container.previousSibling &&
                                        container.nodeType === Node.TEXT_NODE &&
                                        range.startOffset === 0) {
                                    selection.setBefore($(container).closest('del')[0]);
                                    selection.getSelection().nativeSelection.modify('move', 'forward', 'character');
                                    selection.getSelection().nativeSelection.modify('move', 'backward', 'character');
                                }
                            }

                            cleanUp();
                        }

                        if (isBackspace) {
                            doDelete('backward');
                            return false;

                        } else if (isDelete) {
                            doDelete('forward');
                            return false;

                        // Save state for later on any other key.
                        } else if (!down) {
                            down = true;
                            downRange = selection.getRange().cloneRange();
                            downLength = $(composer.element).text().length;
                            downContents = downRange.collapsed ? null : downRange.cloneContents();
                        }

                        return true;
                    });

                    $(composer.element).bind('keyup', function(event) {
                        var selection,
                                range,
                                $del,
                                $ins;

                        if (!down ||
                                event.metaKey ||
                                !$(composer.element).hasClass('rte-changesTracking') ||
                                $(composer.selection.getRange().commonAncestorContainer).closest('.rte-comment').length > 0) {
                            return;
                        }

                        if ($(composer.element).text().length !== downLength) {
                            selection = composer.selection;
                            range = selection.getRange();

                            // Previously selected text was somehow removed so clone
                            // and wrap it in <del>.
                            if (downContents && range.collapsed) {
                                $del = $('<del/>', composer.iframe.contentDocument);

                                $del.append(downContents);
                                range.insertNode($del[0]);
                            }

                            // Wrap newly inserted text in <ins>.
                            selection.executeAndRestore(function() {
                                range.setStart(downRange.startContainer, downRange.startOffset);
                                selection.setSelection(range);
                                $ins = wrapCurrentSelectionRange('ins');
                            });

                            if ($ins) {
                                $del = $ins.closest('del');

                                if ($del.length > 0) {
                                    $del.after($ins);
                                    selection.setAfter($ins[0]);
                                }
                            }
                        }

                        down = false;
                        downRange = null;
                        downLength = -1;
                        downContents = null;

                        cleanUp();
                    });
                })();

                setInterval(function() {
                    rte.updateOverlay();
                }, 100);

                setInterval(function() {
                    $(rte.composer.element).find('.rte-comment').each(function() {
                        var $comment = $(this),
                                next = this.nextSibling,
                                nextValue;

                        if (next) {
                            if (next.nodeType === Node.TEXT_NODE) {
                                nextValue = next.nodeValue;

                                if (nextValue.length > 0 &&
                                        nextValue.substring(0, 1) !== ZERO_WIDTH_SPACE) {
                                    $comment.after(ZERO_WIDTH_SPACE);
                                }
                            }

                        } else {
                            $comment.after(ZERO_WIDTH_SPACE);
                        }
                    });

                    $(rte.composer.element).find('a[href]').each(function() {
                        var $a = $(this);
                        var href = $a.prop('href');
                        var changed = false;
                        var text = $a.text();
                        var next = this.nextSibling;
                        var $next;

                        while (next) {
                            $next = $(next);

                            if (href === $next.prop('href')) {
                                changed = true;
                                text = text + $next.text();
                                next = next.nextSibling;
                                $next.remove();

                            } else {
                                break;
                            }
                        }

                        if (changed) {
                            $a.text(text);
                        }
                    });
                }, 100);
            });
        },

        // Updates the overlay to show enhancements above the underlying
        // placeholders.
        'updateOverlay': function() {
            var rte = this,
                    textarea = rte.textarea,
                    $textareaElement,
                    textareaValue,
                    isCurrentViewTextarea = rte.currentView === textarea,
                    $overlay = $(rte.overlay),
                    composer = rte.composer,
                    composerIframe = composer.iframe,
                    composerWindow = composerIframe.contentWindow,
                    $composerBody;

            if (!composerWindow ||
                    !$(isCurrentViewTextarea ? textarea : composerIframe).is(':visible')) {
                return;
            }

            // Hide if viewing source HTML.
            if (isCurrentViewTextarea) {
                $overlay.hide();
                return;
            }

            $textareaElement = $(textarea.element);
            textareaValue = $textareaElement.val();

            if (!textareaValue || textareaValue === '' || textareaValue.trim().toLowerCase() === '<br>') {
                $(composer.element).attr('data-placeholder', $textareaElement.attr('placeholder'));

            } else {
                $(composer.element).removeAttr('data-placeholder');
            }

            $overlay.show();

            $composerBody = $(composerWindow.document.body);

            // Automatically size the iframe height to match the content.
            $(composerIframe).css('min-height', Math.max(28, $composerBody.outerHeight(true)));
            $(composerWindow).scrollTop(0);

            // Overlay enhancements on top of the placeholders in the composer.
            $overlay.children().each(function() {
                $.data(this, 'rte-visited', false);
            });

            $composerBody.find('button.enhancement').each(function() {
                var $placeholder = $(this),
                        isMarker = $placeholder.hasClass('marker'),
                        enhancementId = getEnhancementId(this),
                        $enhancement = $('#' + enhancementId),
                        $editTrigger,
                        placeholderOffset = $placeholder.offset(),
                        $enhancementLabel,
                        newLabel,
                        $oldImage,
                        oldPreview,
                        newPreview,
                        refData;

                // Create the enhancement if it doesn't exist already.
                if ($enhancement.length === 0) {
                    $enhancement = $(rte.config[isMarker ? 'marker' : 'enhancement'](rte));

                    $enhancement.attr('id', enhancementId);
                    $enhancement.css('position', 'absolute');
                    $.data($enhancement[0], '$rte-placeholder', $placeholder);
                    $overlay.append($enhancement);

                    $enhancement.find('.rte-button-enhancementSelect a').each(function() {
                        var $anchor = $(this),
                                id = $placeholder.attr('data-id');

                        $anchor.attr('href', $.addQueryParameters(
                                $anchor.attr('href'),
                                'id', id));

                        if (!id) {
                            $editTrigger = $anchor;

                        } else {
                            $enhancement.find('.rte-button-enhancementEdit a').each(function() {
                                var $edit = $(this);

                                $edit.closest('.rte-group').addClass('rte-group-enhancementSet');
                                $edit.attr('href', $.addQueryParameters(
                                        $edit.attr('href'),
                                        'id', id,
                                        'reference', $placeholder.attr('data-reference')));
                            });
                        }
                    });
                }

                $.data($enhancement[0], 'rte-visited', true);

                // Copy the enhancement label.
                $enhancementLabel = $enhancement.find('.rte-enhancement-label');

                refData = $.parseJSON($placeholder.attr('data-reference') || '{}');
                newLabel = refData.label;
                newPreview = refData.preview;

                if (newPreview) {
                    if ($enhancementLabel.find('figure img').attr('src') !== newPreview) {
                        $enhancementLabel.html($('<figure/>', {
                            'html': [
                                $('<img/>', {
                                    'src': newPreview
                                }),
                                $('<figcaption/>')
                            ]
                        }));
                    }

                    $enhancementLabel.find('figure img').attr('alt', newLabel);
                    $enhancementLabel.find('figure figcaption').text(newLabel);

                } else {
                    if (!newLabel) {
                        newLabel = 'Empty ' + (isMarker ? 'Marker' : 'Enhancement');
                    }

                    $enhancementLabel.text(newLabel);
                }

                // Position the enhancement to cover the placeholder.
                $enhancement.css('height', '');
                $placeholder.height($enhancement.height());
                $enhancement.height($placeholder.height());
                $enhancement.width($placeholder.width());

                $enhancement.css({
                    'left': placeholderOffset.left,
                    'top': placeholderOffset.top
                });

                if ($editTrigger) {
                    $editTrigger.click();
                }
            });

            // Remove orphaned enhancements.
            $overlay.children().each(function() {
                if (!$.data(this, 'rte-visited')) {
                    $(this).remove();
                }
            });
        }
    });

    // Add support for strike command.
    wysihtml5.commands.strike = {

        'exec': function(composer, command) {
            return wysihtml5.commands.formatInline.exec(composer, command, 'strike');
        },

        'state': function(composer, command) {
            return wysihtml5.commands.formatInline.state(composer, command, 'strike');
        }
    };

    var textAlignRegex = /cms-textAlign-[0-9a-z\-]+/g;
    wysihtml5.commands.textAlign = {

        'exec': function(composer, command, alignment) {
            return wysihtml5.commands.formatBlock.exec(composer, command, null, 'cms-textAlign-' + alignment, textAlignRegex);
        },

        'state': function(composer, command, alignment) {
            return wysihtml5.commands.formatBlock.state(composer, command, null, 'cms-textAlign-' + alignment, textAlignRegex);
        }
    };

    // Remove support for insertImage so that it can't be used accidentally,
    // since insertEnhancement supersedes its functionality.
    delete wysihtml5.commands.insertImage;

    var insertButton = function(composer, button) {
        var selection = composer.selection;
        var $selected = $(selection.getSelectedNode());

        if ($selected.is('body')) {
            $($selected[0].childNodes[selection.getRange().startOffset]).after(button);

        } else {
            var range = selection.getRange();

            if (range.collapsed) {
                var br = $selected[0].childNodes[range.startOffset];

                if (br && $(br).is('br')) {
                    selection.getSelection().nativeSelection.modify('move', 'forward', 'character');
                    $selected = $(selection.getSelectedNode());
                }
            }

            var precedings = [ ];

            $selected.closest('body').find('br + br, h1, h2, h3, h4, h5, h6, p, button').each(function() {
                if ($selected[0].compareDocumentPosition(this) & Node.DOCUMENT_POSITION_PRECEDING) {
                    precedings.push(this);
                }
            });

            var precedingsLength = precedings.length;

            if (precedingsLength >= 1) {
                $(precedings[precedingsLength - 1]).after(button);

            } else {
                $selected.closest('body').prepend(button);
            }
        }
    };

    wysihtml5.commands.link = {

        'exec': function() {
        },

        'state': function(composer) {
            var selection = composer.selection;

            return !selection.getRange().collapsed ||
                    $(selection.getSelectedNode()).closest('a').length > 0;
        }
    };

    // Add support for adding an enhancement.
    wysihtml5.commands.insertEnhancement = {

        'exec': function(composer, command, value) {
            var doc = composer.doc;
            var button = doc.createElement('BUTTON');

            if (value) {
                for (var i in value) {
                    button.setAttribute(i, value[i]);
                }
            }

            button.setAttribute('class', 'enhancement');
            insertButton(composer, button);
        },

        'state': function(composer) {
            return false;
        }
    };

    // Add support for adding a marker.
    wysihtml5.commands.insertMarker = {

        'exec': function(composer, command, value) {
            var doc = composer.doc;
            var button = doc.createElement('BUTTON');

            if (value) {
                for (var i in value) {
                    button.setAttribute(i, value[i]);
                }
            }

            button.setAttribute('class', 'enhancement marker');
            insertButton(composer, button);
        },

        'state': function(composer) {
            return false;
        }
    };

    // Add support for toggling all annotation comments.
    wysihtml5.commands.allComments = {

        'exec': function(composer) {
            $(composer.element).toggleClass('rte-allComments');
        },

        'state': function(composer) {
            return $(composer.element).hasClass('rte-allComments');
        }
    };

    // Changes and comment support.
    (function() {
        function iterateElements(composer, selector, callback) {
            var selection = composer.selection,
                    range = selection.getRange();

            $(range.collapsed ?
                    $(range.commonAncestorContainer).closest(selector) :
                    range.getNodes([ Node.ELEMENT_NODE ])).each(function() {
                if ($(this).is(selector)) {
                    callback.call(this);
                }
            });
        }

        function acceptOrReject(removeTag, unwrapTag) {
            return {
                'exec': function(composer) {
                    iterateElements(composer, 'del, ins', function() {
                        var $element = $(this);

                        if ($element.is(removeTag)) {
                            $element.remove();

                        } else if ($element.is(unwrapTag)) {
                            $element.after($element.html());
                            $element.remove();
                        }
                    });
                },

                'state': function(composer) {
                    var range = composer.selection.getRange();

                    return !range.collapsed ||
                            $(range.commonAncestorContainer).closest('del, ins').length > 0;
                }
            };
        }

        $.extend(wysihtml5.commands, {
            'changesTrack': {
                'exec': function(composer) {
                    $(composer.element).toggleClass('rte-changesTracking');
                },

                'state': function(composer) {
                    return $(composer.element).hasClass('rte-changesTracking');
                }
            },

            'changesAccept': acceptOrReject('del', 'ins'),
            'changesReject': acceptOrReject('ins', 'del'),

            'changesPreview': {
                'exec': function(composer) {
                    $(composer.element).toggleClass('rte-changesPreview');
                },

                'state': function(composer) {
                    return $(composer.element).hasClass('rte-changesPreview');
                }
            },

            'commentAdd': {
                'exec': function(composer) {
                    var selection = composer.selection,
                            $comment = $(selection.getRange().commonAncestorContainer).closest('.rte-comment');

                    if ($comment.length === 0) {
                        wysihtml5.commands.formatInline.exec(composer, null, 'span', 'rte rte-comment');
                    }
                },

                'state': function(composer) {
                    var range = composer.selection.getRange();
                    $(composer.config.toolbar).toggleClass(
                            'rte-toolbarContainer-inComment',
                            !range.collapsed ||
                                    $(range.commonAncestorContainer).closest('.rte-comment').length > 0);
                    return false;
                }
            },

            'commentCollapse': {
                'exec': function(composer) {
                    iterateElements(composer, '.rte-comment', function() {
                        var $selected = $(this),
                                comment = $selected.attr('data-comment');

                        if (comment) {
                            $selected.text(comment);
                            $selected.removeAttr('data-comment');

                        } else {
                            $selected.attr('data-comment', $selected.text());
                            $selected.text('\u2026');
                        }
                    });
                },

                'state': function(composer) {
                    return !!$(composer.selection.getRange().commonAncestorContainer).closest('.rte-comment').attr('data-comment');
                }
            },

            'commentRemove': {
                'exec': function(composer) {
                    iterateElements(composer, '.rte-comment', function() {
                        $(this).remove();
                    });
                },

                'state': function(composer) {
                    return $(composer.selection.getRange().commonAncestorContainer).closest('.rte-comment-removed').length > 0;
                }
            }
        });
    })();

    // Add support for toggling 'Fullscreen' mode.
    wysihtml5.commands.fullscreen = {

        'exec': function(composer) {
            $('.toolBroadcast').toggle();
            $('.toolHeader').toggle();
            $(composer.parent.container).toggleClass('rte-fullscreen');
            $(composer.element).toggleClass('rte-fullscreen');
            $(doc.body).toggleClass('rte-fullscreen');
        },

        'state': function(composer) {
            return $(composer.element).hasClass('rte-fullscreen');
        }
    };

    var iframeHtml;

    (function() {
        $.ajax({
            'url': CONTEXT_PATH + '/style/v3/rte-content.jsp',
            'cache': false,
            'async': false,

            'success': function(html) {
                iframeHtml = html;
            }
        });
    })();

    // Expose as a jQuery plugin.
    var $inputs = $();

    $.plugin2('rte', {
        '_defaultOptions': {
            'enhancement': createEnhancement,
            'iframeHtml': iframeHtml,
            'marker': createMarker,
            'placeholder': '',
            'style': false,
            'toolbar': createToolbar,
            'useLineBreaks': !RTE_LEGACY_HTML,

            'parserRules': RTE_LEGACY_HTML ? { } : {
                'tags': {
                    'font': { 'rename_tag': 'span' },
                    'style': { 'remove': true }
                }
            }
        },

        '_create': function(input) {
            var options = this.option();

            $.data(input, 'rte-options', $.extend(true, { }, options));

            if (options.initImmediately) {
                var $input = $(this),
                        rte;

                if ($input.attr('data-inline') === 'true') {
                    options.inline = true;
                }

                rte = new Rte(input, options);

                $input.bind('input-disable', function(event, disable) {
                    $input.closest('.rte-container').toggleClass('state-disabled', disable);
                    rte[disable ? 'disable' : 'enable']();
                });

                $input.parent().trigger('create');

            } else {
                $inputs = $inputs.add($(input));
            }
        },

        'enable': function() {
            var container = this.$caller[0];

            if (container) {
                $.each(rtes, function() {
                    var textarea = this.textareaElement;
                    if (textarea && $.contains(container, textarea)) {
                        this.enable();
                    }
                });
            }

            return this;
        },

        // Sets data related to the enhancement.
        'enhancement': function(data) {
            var $enhancement = this.$caller.closest('.rte-enhancement');
            var $placeholder = $enhancement.data('$rte-placeholder');

            if ($placeholder) {
                $.each(data, function(key, value) {
                    var name = 'data-' + key;
                    if (value === null || value === undef) {
                        $placeholder.removeAttr(name);
                    } else {
                        $placeholder.attr(name, value);
                    }
                });
            }

            var refData = $.parseJSON(data.reference || '{}');
            var label = refData.label;
            if (label) {
                $enhancement.find('.rte-enhancement-label').text(label);
            }

            return this;
        },

        'wysihtml5': function() {
            var caller = this.$caller[0],
                    found;

            $.each(rtes, function() {
                if (caller === this.textareaElement ||
                        caller === this.composer.iframe ||
                        caller.ownerDocument === this.composer.element.ownerDocument) {
                    found = this;
                    return;
                }
            });

            return found;
        }
    });

    setInterval(function() {
        $inputs.each(function() {
            var $input = $(this),
                    options = $.data(this, 'rte-options'),
                    rte;

            if ($input.is(':visible')) {
                $inputs = $inputs.not($input);

                if ($input.attr('data-inline') === 'true') {
                    options.inline = true;
                }

                rte = new Rte(this, options);

                $input.bind('input-disable', function(event, disable) {
                    $input.closest('.rte-container').toggleClass('state-disabled', disable);
                    rte[disable ? 'disable' : 'enable']();
                });

                $input.parent().trigger('create');
            }
        });
    }, 100);

    // Make sure that the editorial toolbar is visible as long as possible.
    $win.bind('resize.rte scroll.rte', keepToolbarInView = $.throttle(150, function() {
        var $header = $('.toolHeader'),
                headerBottom = $header.offset().top + $header.outerHeight() - ($header.css('position') === 'fixed' ? $win.scrollTop() : 0),
                windowTop = $win.scrollTop() + headerBottom,
                raf = window.requestAnimationFrame;

        $.each(rtes, function() {
            var $container = $(this.container),
                    $overlay,
                    $toolbar,
                    containerTop,
                    toolbarHeight,
                    toolbarLeft,
                    toolbarWidth;

            if (!$container.is(':visible')) {
                return;
            }

            $overlay = $(this.overlay);
            $toolbar = $(this.config.toolbar);
            containerTop = $container.offset().top;
            toolbarHeight = $toolbar.outerHeight();

            // Completely in view.
            if (windowTop < containerTop) {
                raf(function() {
                    $container.css('padding-top', 0);
                    $overlay.css('top', toolbarHeight);
                    $toolbar.attr('style', this._toolbarOldStyle);

                    this._toolbarOldStyle = null;
                });

            } else {
                this._toolbarOldStyle = this._toolbarOldStyle || $toolbar.attr('style') || ' ';

                raf(function() {
                    $container.css('padding-top', toolbarHeight);
                    $overlay.css('top', 0);
                });

                // Partially in view.
                if (windowTop < containerTop + $container.height()) {
                    toolbarLeft = $toolbar.offset().left;
                    toolbarWidth = $toolbar.width();

                    raf(function() {
                        $toolbar.css({
                            'left': toolbarLeft,
                            'position': 'fixed',
                            'top': headerBottom,
                            'width': toolbarWidth
                        });
                    });

                // Completely out of view.
                } else {
                    raf(function() {
                        $toolbar.css({
                            'top': -10000,
                            'position': 'fixed'
                        });
                    });
                }
            }
        });
    }));

    // Handle enhancement selection within a popup.
    $doc.on('click', '[data-enhancement]', function(event) {
        var $target = $(this),
                $select = $target.popup('source'),
                $group = $select.closest('.rte-group'),
                $edit = $group.find('.rte-button-enhancementEdit a'),
                enhancement = $target.attr('data-enhancement'),
                enhancementJson = $.parseJSON(enhancement);

        $group.addClass('rte-group-enhancementSet');
        $select.text('Change');
        $select.rte('enhancement', {
            'id': enhancementJson.record._ref,
            'label': enhancementJson.label,
            'preview': enhancementJson.preview,
            'reference': enhancement
        });

        if ($edit.length > 0) {
            $edit.attr('href', $.addQueryParameters(
                    $edit.attr('href'),
                    'id', enhancementJson.record._ref));
        }

        $target.popup('close');
        event.preventDefault();
        event.stopImmediatePropagation();
    });

    // Remove the enhancement if selection popup is closed without enhancement
    // being set.
    $doc.on('close', '.popup[name ^= "contentEnhancement-"]', function() {
        var $group = $(this).popup('source').closest('.rte-group'),
                $enhancement,
                $placeholder;

        if (!$group.hasClass('rte-group-enhancementSet')) {
            $enhancement = $group.closest('.rte-enhancement');
            $placeholder = $enhancement.data('$rte-placeholder');

            $enhancement.remove();

            if ($placeholder) {
                $placeholder.remove();
            }
        }
    });

    // Remember track changes state.
    $doc.on('submit', 'form', function() {
        var $form = $(this),
                storage = window.sessionStorage,
                storageIndex = 0,
                storageLength = storage.length,
                storageKey;

        for (; storageIndex < storageLength; ++ storageIndex) {
            storageKey = storage.key(storageIndex);

            if (storageKey.indexOf('bsp.rte.changesTracking.') === 0) {
                storage.removeItem(storageKey);
            }
        }

        $form.find('.wysihtml5-sandbox').each(function() {
            if ($(this.contentDocument.body).hasClass('rte-changesTracking')) {
                storage.setItem('bsp.rte.changesTracking.' + $(this).closest('.inputContainer').attr('data-name'), '1');
            }
        });
    });
});
