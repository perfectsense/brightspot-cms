/**
 * Created by rhseeger on 5/9/17.
 */
define([ 'jquery', 'bsp-utils' ], function ($, bsp_utils) {

    // Should this be converted to a plugin like popup.js?
    var thumbnailGridDefaults = {
        gridSelector: '.searchResult-images',
        containerSelector: 'figure',
        maxRowHeight: 200
    };

    bsp_utils.onDomInsert(document, thumbnailGridDefaults.gridSelector, {
        insert: function() {
            $('.searchResult-images').each(function() {
                var $grid = $(this);
                var options = {
                    container: 'figure',
                    object: 'img',
                    rowHeight: 200 //,
                    //maxRows: null,
                    //truncate: false
                };
                $grid.flexImages(options);
                $grid.data('onLoadCallback', function() { $grid.flexImages(options); });
            });
        }
    });

});

// We need to figure out how to call .flexImages on the grid whenever an ajax event loads more images (lazy load)