/* global define, ENABLE_PADDED_CROPS */
//# sourceURL=image.js

define([ 'jquery', 'bsp-utils', 'v3/input/ImageEditor' ], function($, bsp_utils, ImageEditor) {
    bsp_utils.onDomInsert(document, '.imageEditor', {
        insert: function(element) {
            if (ENABLE_PADDED_CROPS) {
                return;
            }

            var imageEditor = Object.create(ImageEditor);

            imageEditor.init(element);
            $(element).data('imageEditor', imageEditor);
        }
    });
});
