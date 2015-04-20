(function($, window, undefined) {

var MAIN_TAB_NAME = 'Main',
        SELECTED_CLASS = 'state-selected';

$.plugin2('tabbed', {
    '_create': function(container) {
        var $container = $(container),
                tabs = [ ],
                tabItems = { },
                $items = $container.find('> [data-tab]'),
                containerId,
                tabParameter,
                tabParameterRe,
                $tabs,
                urlMatch;

        function addTab(name) {
            tabs.push({
                'name': name,
                'items': (tabItems[name] = [ ])
            });
        }

        // Main tab should always be first.
        addTab(MAIN_TAB_NAME);

        // Which items for which tabs?
        $items.each(function() {
            var tabName = $(this).attr('data-tab');

            if (!tabItems[tabName]) {
                addTab(tabName);
            }

            tabItems[tabName].push(this);
        });

        // Delete the main tab if there aren't any items in it.
        if (tabItems[MAIN_TAB_NAME].length === 0) {
            delete tabItems[MAIN_TAB_NAME];
            tabs.shift();
        }

        // Don't show tabs unless there are more than 2.
        if (tabs.length < 2) {
            return;
        }

        // Tab state in the URL?
        containerId = $container.attr('data-id');

        if (containerId) {
            tabParameter = encodeURIComponent($container.attr('data-id') + '/tab');
            tabParameterRe = new RegExp('([?&])' + tabParameter + '=([^=]+)');
        }

        // Create the tabs.
        $tabs = $('<ul/>', { 'class': 'tabs' });

        $.each(tabs, function(i, tab) {
            $tabs.append($('<li/>', {
                'class': $(tab.items).find('.message-error').length > 0 ? 'state-error' : '',
                'html': $('<a/>', {
                    'text': tab.name,
                    'click': function(event) {
                        var $selected = $(event.target),
                                history = window.history,
                                href,
                                text;

                        if (containerId && history && history.replaceState) {
                            href = window.location.href.replace(tabParameterRe, '');
                            text = $selected.text();

                            if (text !== MAIN_TAB_NAME) {
                                href += (href.indexOf('?') > -1 ? '&' : '?') + tabParameter + '=' + encodeURIComponent(text);
                            }

                            history.replaceState('', '', href);
                        }

                        $tabs.find('> li').removeClass(SELECTED_CLASS);
                        $selected.closest('li').addClass(SELECTED_CLASS);
                        $items.hide();
                        $(tab.items).show().trigger('tabbedShow');
                        $container.resize();
                        return false;
                    }
                })
            }));
        });

        // Select the first tab.
        $tabs.find('li:first-child').addClass(SELECTED_CLASS);
        $items.hide();
        $(tabs[0].items).show();
        $container.prepend($tabs);
        $container.resize();

        // Select the tab if it's referenced in the URL.
        if (containerId) {
            urlMatch = tabParameterRe.exec(window.location.href);

            if (urlMatch) {
                urlMatch = urlMatch[2];

                if (urlMatch) {
                    $tabs.find('> li > a').each(function() {
                        var $tab = $(this);

                        if ($tab.text() === urlMatch) {
                            $tab.click();
                            return false;
                        }
                    });
                }
            }
        }
    }
});

}(jQuery, window));
