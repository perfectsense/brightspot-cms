define([ 'bsp-utils' ], function (bsp_utils) {
  bsp_utils.onDomInsert(document, '.inputContainer-readOnly input[type="text"], .inputContainer-readonly textarea, .inputContainer-readOnly input:not([type])', {
    'insert': function (input) {
      input.readOnly = true;
    }
  });
});
