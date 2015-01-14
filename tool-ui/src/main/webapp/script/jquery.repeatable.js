/** Inputs that can be repeated. */
(function($, win, undef) {

var $win = $(win),
        cacheNonce = 0,
        OBJECT_FORM_DATA = "object-form-data",
        PREVIOUS_OBJECT_DATA = "object-nav-previous",
        NEXT_OBJECT_DATA = "object-nav-next";


$.plugin2('repeatable', {
    '_defaultOptions': {
        'addButtonText': 'Add',
        'removeButtonText': 'Remove',
        'restoreButtonText': 'Restore',
        'sortableOptions': {
            'delay': 300
        }
    },

    'loadFormFields': function($item) {

        var $input = $item.find('> input[data-form-fields-url]');

        if ($input.length > 0) {

            $item.addClass('embeddedForm-loading');
            var url = $input.attr('data-form-fields-url');
            var data = $input.val();

            $input.removeAttr('data-form-fields-url');
            $input.val('');
            $item.toggleClass('collapsed');

            $.ajax({
                'type': 'POST',
                'cache': false,
                'url': url,
                'data': { 'data': data },
                'complete': function(response) {
                    $item.append(response.responseText);
                    $item.removeClass('embeddedForm-loading');
                    $item.trigger('create');
                    $item.trigger('load');
                    $item.resize();
                    $item.find(':input:first').change();
                }
            });
        } else {
            $item.toggleClass('collapsed');
            $item.resize();
            $item.find(':input:first').change();
        }
    },

    '_bindPreviewInput': function($item) {

        // [data-preview-field] provides a dynamic connection between the preview thumbnail provided and an object field
        var previewField = $item.attr('data-preview-field');
        if(previewField) {

            // final path segment is a StorageItem
            // remove final segment to get path to the StorageItem's parent
            var lastSlashAt = previewField.lastIndexOf("/");

            if(lastSlashAt !== -1) {
                previewField = previewField.substr(0, lastSlashAt);
            }

            // get the current $item's field path
            var myField = $item.closest('[data-field]').attr('data-field');

            // splice current $item's path with preview field's relative path to StorageItem parent
            var $previewFieldEl = $item.find('[data-field="' + myField + '/' + previewField + '"]').first();

            // embedded objects rendered in the page include the parent path prefix
            // newly-added embedded objects do not include the parent path prefix
            // check both [data-field] values
            if($previewFieldEl.size() === 0) {
                $previewFieldEl = $item.find('[data-field="' + previewField + '"]').first();
            }

            if($previewFieldEl.size() > 0) {

                var $previewFieldInput = $previewFieldEl.find('[name="' + $previewFieldEl.attr('data-name') + '"]').first();

                // on change of the input described by [data-preview-field], update the [data-preview] attribute on the $item
                $previewFieldInput.bind('change', function() {
                    $item.attr('data-preview', $previewFieldInput.attr('data-preview'));
                    $item.find('> .embedded-object-preview > figure > img').attr('src', $previewFieldInput.attr('data-preview'));
                });
            }
        }
    },

    'popEmbeddedEdit': function($item, $source, event) {

        var plugin = this;

        var $objectInputs = $.data($item[0], OBJECT_FORM_DATA);

        if(!$objectInputs) {

            // use .first() to avoid pulling nested .objectInputs containers from embedded objects
            $objectInputs = $item.find('.objectInputs').first();

            if($objectInputs.size() === 0) {

                $item.one('load', function() {
                    plugin.popEmbeddedEdit($item, $source, event);
                });

                if(!$item.hasClass('embeddedForm-loading')) {
                    plugin.loadFormFields($item);
                }

                return;
            }

            plugin._bindPreviewInput($item);

            $.data($item[0], OBJECT_FORM_DATA, $objectInputs);
            $objectInputs.popup({'parent': $objectInputs.closest('form')[0]});
            $objectInputs.trigger('resize');
        }

        // remove class .collapsed
        $item.removeClass('collapsed');

        // store the previous [data-preview] sibling of the selected $item in jQuery data
        var $previous = $item.prevAll().filter('[data-embedded-popup]').first();
        $.data($item[0], PREVIOUS_OBJECT_DATA, $previous);

        // find or create a "previous object" navigation control on the popup
        var $navPrevious = $objectInputs.find('> .previousObject');
        if($navPrevious.size() === 0) {
            $navPrevious = $('<span />', {
                'class': 'previousObject',
                'click': function() {
                    var $previousObj = $.data($item[0], PREVIOUS_OBJECT_DATA);
                    $objectInputs.popup('close');
                    plugin.popEmbeddedEdit($previousObj, $previousObj.find('.embedded-object-edit-popup'));
                }
            });
            $objectInputs.append($navPrevious);
        }

        // store the next [data-preview] sibling of the selected $item in jQuery data
        var $next = $item.nextAll().filter('[data-embedded-popup]').first();
        $.data($item[0], NEXT_OBJECT_DATA, $next);

        // find or create a "next object" navigation control on the popup
        var $navNext = $objectInputs.find('> .nextObject');
        if($navNext.size() === 0) {
            $navNext = $('<span />', {
                'class': 'nextObject',
                'click': function() {
                    var $nextObj = $.data($item[0], NEXT_OBJECT_DATA);
                    $objectInputs.popup('close');
                    plugin.popEmbeddedEdit($nextObj, $nextObj.find('.embedded-object-edit-popup'));
                }
            });
            $objectInputs.append($navNext);
        }

        // add classes to the .popup to indicate that the selected $item has previous and next siblings
        $objectInputs.popup('container').toggleClass('hasPrevious', $previous.size() > 0);
        $objectInputs.popup('container').toggleClass('hasNext', $next.size() > 0);

        // open the popup to display the embedded object inputs
        $objectInputs.popup('source', $source, event);
        $objectInputs.popup('open');
    },

    '_create': function(container) {
        var $container = $(container),
                options = this.option(),
                plugin = this;

        $container.addClass('event-input-disable');

        $container.bind('input-disable', function(event, disable) {
            $(event.target).closest('.inputContainer').toggleClass('state-disabled', disable);
        });

        // Helper for creating extra stuff on an item.
        var createExtra = function() {

            var $item = $(this);

            var type = $item.attr('data-type');

            if (type) {
                var label = $item.attr('data-label');
                var $labelHtml = $item.find(" > .repeatableLabel");
                $labelHtml.removeClass('repeatableLabel');
                if ($item.find('.message-error').length === 0) {
                    $item.addClass('collapsed');
                }
                var $label = $('<div/>', {
                    'class': 'repeatableLabel',
                    'text': type + (label ? ': ' + label : ''),
                    'data-object-id': $item.find('> input[type="hidden"][name$=".id"]').val(),
                    'data-dynamic-text': '${content.state.getType().label}: ${content.label}',
                    'click': function() {
                        plugin.loadFormFields($item);
                    }
                });
                if ($labelHtml.size() !== 0) {
                    $label.append($labelHtml);
                    $label.find(':input').click(function(e) {
                        e.stopPropagation();
                    });
                }
                $item.prepend($label);
            }

            // embedded object preview
            if ($item.is('[data-embedded-popup]')) {

                if($item.is('[data-preview]')) {

                    var preview = $item.attr('data-preview');
                    // generate preview thumbnail with click handler to pop up embedded object edit form
                    $item.prepend($('<div />', {
                            class: 'embedded-object-preview',
                            html: $('<figure />', {
                                'class': 'embedded-object-edit-popup',
                                'html': [
                                    $('<img/>', {
                                        'src': preview
                                    }),
                                    $('<figcaption />', {
                                        'html': $label
                                    })
                                ],
                                'click': function(e) {
                                    plugin.popEmbeddedEdit($item, $(this), null);
                                }})
                        })
                    );

                    $item.append($('<span/>', {
                        'class': 'embedded-object-edit embedded-object-edit-popup',
                        'html': $('<span/>', {
                            'class': 'embedded-object-edit-popup',
                            'text': 'Edit',
                            'click': function(e) {
                                plugin.popEmbeddedEdit($item, $(this), null);
                            }
                        })
                    }));
                } else {
                    $label.addClass('embedded-object-edit-popup');
                    $label.click(function(e) {
                        plugin.popEmbeddedEdit($item, $label, null);
                    });
                }
            }

            $item.find(':input[name$=".toggle"]').hide();
            $item.append($('<span/>', {
                'class': 'removeButton',
                'text': options.removeButtonText
            }));
        };

        // List of inputs is contained in ul or ol (latter is sortable).
        var $list = $container.find('> ul:first');
        if ($list.length === 0) {
            $list = $container.find('> ol:first');
            if ($list.length === 0) {
                return;
            } else {
                $list.sortable(options.sortableOptions);
            }
        }

        var $templates = $();

        $list.find('> li.template, > script[type="text/template"]').each(function() {
            var $template = $(this);

            if ($template.is('li.template')) {
                $templates = $templates.add($template);

            } else {
                $templates = $templates.add($($template.text()));
            }

            $template.remove();
        });

        $list.find('> li').each(createExtra);

        var $addButtonContainer = $('<div/>', { 'class': 'addButtonContainer' });
        $container.append($addButtonContainer);

        // Enable single input mode when there's only one template and one input.
        var $singleInput;
        if (!options.addButtonText && $templates.length == 1) {
            var $inputs = $templates.find(':input');
            var $toggle = $templates.find(':input[name$=".toggle"]');
            $inputs = $inputs.not($toggle);
            if ($inputs.length == 1) {
                $singleInput = $inputs.clone();
                $singleInput.removeAttr('id');
                $singleInput.keydown(function(event) {
                    if (event.which == 13) {
                        $addButtonContainer.find('.addButton').trigger('click');
                        return false;
                    }
                });
                $addButtonContainer.append($('<input/>', {
                    'name': $toggle.attr('name'),
                    'type': 'hidden',
                    'value': $toggle.attr('value')
                }));
                $addButtonContainer.append($singleInput);
            }
        }

        // Create an add link for each template.
        var idIndex = 0;
        $templates.each(function() {
            var $template = $(this);
            $addButtonContainer.append($('<span/>', {
                'class': 'addButton',
                'text': options.addButtonText ? options.addButtonText + ' ' + ($template.attr('data-type') || 'Item') : '',
                'click': function(event, customCallback) {

                    // Don't allow blank text in single input mode.
                    if ($singleInput && !$singleInput.val()) {
                        return false;
                    }

                    $container.find(".objectId-placeholder").hide();

                    var $addedItem = $template.clone();
                    $addedItem.removeClass('template');
                    $addedItem.find(':input[name$=".toggle"]').attr('checked', 'checked');

                    var callback = function() {

                        $list.append($addedItem);
                        $addedItem.each(createExtra);
                        $addedItem.removeClass('collapsed');

                        // Copy value in single input to the newly added item.
                        if ($singleInput) {
                            $addedItem.find(':input:not([name$=".toggle"])').val($singleInput.val());
                            $singleInput.val('');
                        }

                        // So that IDs don't conflict.
                        $addedItem.find('*[id]').attr('id', function(index, attr) {
                            idIndex += 1;
                            var newAttr = attr + 'r' + idIndex;
                            $addedItem.find('*[for=' + attr + ']').attr('for', newAttr);
                            $addedItem.find('*[data-show=#' + attr + ']').attr('data-show', '#'+newAttr);
                            return newAttr;
                        });

                        $addedItem.change();
                        $addedItem.trigger('create');
                        $win.resize();

                        var $select = $addedItem.find('.objectId-select');

                        if ($select.length > 0 &&
                                $select.closest('.repeatableObjectId').length > 0) {
                            // $select.click();
                        }

                        if (customCallback) {
                            customCallback.call($addedItem[0]);
                        }
                    };

                    // Load an external form if the template consists of a single link without any other inputs.
                    var $templateLink;
                    if ($addedItem.find(':input').length === 0 && ($templateLink = $addedItem.find('a')).length > 0) {
                        ++ cacheNonce;

                        $.ajax({
                            'cache': false,
                            'url': $templateLink.attr('href'),
                            'data': { '_nonce': cacheNonce },
                            'complete': function(response) {
                                $addedItem.html(response.responseText);
                                callback();
                            }
                        });
                    } else {
                        callback();
                    }

                    return false;
                }
            }));
        });

        // On remove link click:
        // - Add toBeRemoved class on the item.
        // - Disable all inputs.
        // - Change remove link text.
        $list.delegate('> li > .removeButton', 'click', function() {

            var $removeButton = $(this);
            var $item = $removeButton.closest('li');
            var $inputs = $item.find(':input');

            if ($item.is('.toBeRemoved')) {
                $item.removeClass('toBeRemoved');
                $inputs.removeAttr('disabled');
                if (options.removeButtonText) {
                    $removeButton.text(options.removeButtonText);
                }

            } else {
                $item.addClass('toBeRemoved');
                $inputs.attr('disabled', 'disabled');
                if (options.restoreButtonText) {
                    $removeButton.text(options.restoreButtonText);
                }
            }

            $item.change();
        });
    },

    'add': function(callback) {
        this.$caller.closest('.addButton').trigger('click', [ callback ]);

        return this.$caller;
    }
});

}(jQuery, window));
