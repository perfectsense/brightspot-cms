define([
    'jquery',
    'bsp-utils' ],

function($, bsp_utils) {
    bsp_utils.onDomInsert(document, 'meta[name=doomsday]', {
        insert: function () {
        var WARN_MINS = 5;
        var exp_time = new Date(Date.parse($('meta[name=doomsday').attr('content')));
        var warn_time = new Date((exp_time.valueOf()+(-1000*60*WARN_MINS)));
        var already_warned = false;

        setInterval(function() {
            var time = new Date(Date.now());
            var message = '';
            var broadcast = false;
            if (time > exp_time) {
                broadcast = true;
                message = "You have been logged out and your work may not be saved."
            } else if (time > warn_time) {
                broadcast = true;
                // construct warning message and redirect url

                // calculate time left.
                var time_left = new Date(exp_time.valueOf() - time.valueOf());
                var mins = time_left.getMinutes();
                var secs = time_left.getSeconds();

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
                    var elem_bcast = document.createElement("div");
                    elem_bcast.classList.add("toolBroadcast");

                    var span = document.createElement("span");
                    span.setAttribute("name","logout-message");
                    span.innerHTML = message;

                    elem_bcast.appendChild(span);

                    $("body").prepend(elem_bcast);
                }
            }

        }, 1000);
        }
    });
});