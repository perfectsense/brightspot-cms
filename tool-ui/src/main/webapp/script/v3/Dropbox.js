require([ 'bsp-utils', 'jquery' ], function (bsp_utils, $) {
    bsp_utils.onDomInsert(document, '.DropboxChooserInput', {
        insert: function (input) {
            var $input = $(input);

            $input.after(Dropbox.createChooseButton({
                linkType: 'direct',
                success: function (files) {
                    $input.val(JSON.stringify(files[0]));
                }
            }));

            $input.hide();
        }
    });
});