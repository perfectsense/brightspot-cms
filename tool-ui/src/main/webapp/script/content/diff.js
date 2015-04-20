define([
    'jquery',
    'bsp-utils',
    'diff' ],

function($, bsp_utils) {
    bsp_utils.onDomInsert(document, '.contentDiff', {
        'insert': function(container) {
            var $container = $(container),
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

            $container.closest('form').submit(function() {
                $left.find(':input').prop('disabled', true);
            });
        }
    });
});
