require(['jquery'], function($) {
    $(window).load(function() {

        var $button = $('.Sms-button');
        var $response = $('.Sms-response');
        var $number = $button.parent().find('.inputSmall').find('textarea');

        $button.on('click', function (e) {
            e.preventDefault();
            $.ajax({
                url: CONTEXT_PATH + 'testSms',
                type: 'POST',
                data: {
                    number: $number.val()
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
    });
});
