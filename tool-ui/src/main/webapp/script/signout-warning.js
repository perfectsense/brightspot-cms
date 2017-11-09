define([
    'jquery',
    'bsp-utils' ],

function($, bsp_utils) {
    bsp_utils.onDomInsert(document, 'meta[name=bsp\\.tu\\.sessionExpiration]', {
        insert: function () {
        var warnMins = 5;
        var expireTime = new Date(Date.parse($('meta[name=bsp\\.tu\\.sessionExpiration]').attr('content')));
        var warnTime = new Date((expireTime.valueOf()+(-1000*60*warnMins)));

        setInterval(function() {
            var time = new Date(Date.now());
            var message = '';
            var broadcast = false;
            if (time > expireTime) {
                broadcast = true;
                message = "You have been logged out and your work may not be saved."
            } else if (time > warnTime) {
                broadcast = true;
                // construct warning message and redirect url

                // calculate time left.
                var timeLeft = new Date(expireTime.valueOf() - time.valueOf());
                var mins = timeLeft.getMinutes();
                var secs = timeLeft.getSeconds();

                var url = window.location.href;
                if (url.indexOf('?') < 0) {
                    url += "?_renewSession=true";
                } else {
                    url += "&_renewSession=true";
                }
                var a = document.createElement('a');
                a.setAttribute('href',url);
                a.innerHTML = "I'm still working."

                message = ' Your session expires in '
                    + (("0" + mins).slice(-2)) + ':'
                    + (("0" + secs).slice(-2)) + ', <a href="'
                    + url + '">I\'m still working.</a>';
            }
            // there is a message to broadcast.
            if (broadcast && !($(".widget-logIn")[0])) {
                if ($("body").hasClass("hasToolBroadcast"))	{
                    message = " - " + message;
                    $('span[name=logout-message]').html(message);
                } else {
                    $("body").attr("class", "hasToolBroadcast");
                    var broadcastElem = document.createElement("div");
                    broadcastElem.classList.add("toolBroadcast");

                    var span = document.createElement("span");
                    span.setAttribute("name","logout-message");
                    span.innerHTML = message;

                    broadcastElem.appendChild(span);

                    $("body").prepend(broadcastElem);
                }
            }

        }, 1000);
        }
    });
});