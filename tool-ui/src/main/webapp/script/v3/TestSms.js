require([ 'bsp-utils' ], function (bsp_utils) {
    bsp_utils.onDomInsert(document, '.Sms', {
        insert: function (sms) {

            var $sms = $(sms);
            var $button = $sms.find('.Sms-button');
            var $response = $sms.find('.Sms-response');

            $button.on('click', function (e) {
                e.preventDefault();
                $.ajax({
                    url: CONTEXT_PATH + 'testSms',
                    type: 'POST',
                    data: {
                        number: $sms.find('textarea').val()
                    },
                    beforeSend: function () {
                        $button.hide();
                        $response.show();
                    },
                    success: function (data) {
                        $response.html(data);
                    }
                });
            });
        }
    });
});
