define([
    'jquery',
    'bsp-utils' ],

function($, bsp_utils) {
    bsp_utils.onDomInsert(document, 'meta[name=bsp\\.tu\\.sessionExpiration]', {
        insert: function () {
        var warnMins = 5;
        var expireTime = new Date(Date.parse($('meta[name=bsp\\.tu\\.sessionExpiration]').attr('content')));
        var warnTime = new Date((expireTime.valueOf()+(-1000*60*warnMins)));
        var goTime = Math.max(0, warnTime.getTime() - Date.now());
        var warnTime = Math.max(0, expireTime.getTime() - Date.now());

        setTimeout(function() {
            // construct warning message and redirect url
            var expireMillis = Math.max(0, expireTime.getTime() - Date.now());
            var message = '';

            var url = window.location.href;
            if (url.indexOf('?') < 0) {
                url += "?_renewSession=true";
            } else {
                url += "&_renewSession=true";
            }

            if (!($(".widget-logIn")[0])) {
                var span = $("<span>").attr("name", "logout-message");

                if ($("body").hasClass("hasToolBroadcast"))	{
                    if (!($("span[name=logout-message]")[0])) {
                        $(".toolBroadcast").append(" - ").append(span);
                    }
                } else {
                    $("body").addClass("hasToolBroadcast");
                    var broadcastElem = $("<div>").addClass("toolBroadcast");
                    broadcastElem.append(span);
                    $("body").prepend(broadcastElem);
                }
            }

            setInterval(function() {
                expireMillis -= 1000;

                if (expireMillis <= 0) {
                    message = "You have been logged out and your work may not be saved.";
                } else {
                    // calculate time left.
                    var timeLeft = new Date(expireMillis);
                    var mins = timeLeft.getMinutes();
                    var secs = timeLeft.getSeconds();

                    message = ' You will be logged out in '
                        + (("0" + mins).slice(-2)) + ':'
                        + (("0" + secs).slice(-2)) + ', <a href="'
                        + url + '">I\'m still working.</a>';
                }

                $("span[name=logout-message]").html(message);

            }, 1000);
        }, goTime);
        }
    });
});