require([ 'bsp-utils', 'jquery' ], function (bsp_utils, $) {
    var $document = $(window.document),
            $body = $($document[0].body),
            $parent = $(window.parent),
            $parentDocument = $($parent[0].document),
            $parentBody = $($parentDocument[0].body),
            $editor = $parentBody.find('.BrightspotCmsInlineEditor');

    // Enable "click-through" editor IFRAME.
    $parentDocument.on('mousemove', function(event) {
        if ($($document[0].elementFromPoint(event.pageX, event.pageY)).closest('.InlineEditorMainObject, .InlineEditorControls').length > 0) {
            $editor.css('pointer-events', 'auto');
        }
    });

    $document.on('mousemove', function(event) {
        if ($body.find('.popup:visible').length === 0 &&
                $($document[0].elementFromPoint(event.pageX, event.pageY)).closest('.InlineEditorMainObject, .InlineEditorControls').length === 0) {
            $editor.css('pointer-events', 'none');
        }
    });

    $document.on('click', 'a[target]', function() {
        $editor.css('pointer-events', 'auto');
    });

    // Collapse the editor on right click because Chrome activates it on the
    // editor even with pointer-events: none.
    $document.on('contextmenu', function(event) {
        var $logo = $body.find('.InlineEditorLogo');

        $editor.css({
            'max-height': $logo.outerHeight(true),
            'max-width': $logo.outerWidth(true)
        });

        $logo.find('a').one('click', function() {
            $editor.css({
                'max-height': '',
                'max-width': ''
            });

            return false;
        });
    });

    // Make sure that the editor IFRAME is at least as high as the parent
    // document.
    function equalizeHeight() {
        $editor.height(Math.max($document.height(), $parentBody.height()));
        $editor.css({
            'border': 'none',
            'left': 0,
            'margin': 0,
            'position': 'absolute',
            'top': 0,
            'width': '100%',
            'z-index': 1000000
        });
    }

    equalizeHeight();
    setInterval(equalizeHeight, 100);

    // Allow inline editor to be closed.
    $('.InlineEditorMainObject-close').on('click', function () {
        $editor.remove();
        return false;
    });

    // Make sure that the main object controls are fixed at the top.
    $parent.scroll(function() {
        $('.InlineEditorMainObject').css('top', $parent.scrollTop());
    });
});