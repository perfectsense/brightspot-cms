define([
    'jquery',
    'bsp-utils' ],

function($, bsp_utils) {
    bsp_utils.onDomInsert(document, 'meta[name=doomsday]', {
        insert: function () {
        var WARN_MINS = 10;
        var exp_time = new Date(Date.parse($('meta[name=doomsday').attr('content')));
        var warn_time = new Date((exp_time.valueOf()+(-1000*60*WARN_MINS)));
        var already_warned = false;
        console.log("logout time: (should be in UTC) " + (exp_time.toUTCString()));

        console.log("warn time: (should be in UTC) " + warn_time.toUTCString());

        setInterval(function() {
            var time = new Date(Date.now());
            console.log("current time: (should be in UTC) " + time.toUTCString());
            var message = '';
            var broadcast = false;
            if (time > exp_time) {
                broadcast = true;
                message = "You have been logged out and your work may not be saved."
                console.log("you have been logged out");
            } else if (time > warn_time) {
                broadcast = true;
                // construct warning message and redirect url

                // calculate time left.
                var time_left = new Date(exp_time.valueOf() - time.valueOf());
                var mins = time_left.getMinutes();
                var secs = time_left.getSeconds();


                console.log(time_left.toUTCString());

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
            if (broadcast) {
                if ($("body").hasClass("hasToolBroadcast"))	{
                    message = " - " + message;
                    $('span[name=logout-message]').html(message);
                    console.log(message);
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





////        // TODO remove because not necessary
//
////        var warn_time = exp_time;
////        var time = new Date(Date.now());
////        //time = time.getUTCDate();
////        console.log(time);
//
//
//        // TODO uncomment conditional if
//        if (time > exp_time) {
//            console.log("condition met");
//            var url = window.location.href;
//            if (url.indexOf('?') < 0) {
//                url += "?_renewSession=true";
//            } else {
//                url += "&_renewSession=true";
//            }
//
//            var a = document.createElement('a');
//            a.setAttribute('href',url);
//            a.innerHTML = "I'm still working."
//
//            var message = '';
////            if (time > exp_time) {
////                message = "You have been logged out, your work on this page will not be saved."
////            } else {

//
//                var DVSN = new Date((exp_time.valueOf()-(1000*60*5)));
//
//
//
//                console.log("IS THIS A REAL TIME????" + DVSN.toUTCString());
//
//
//                console.log("^ theoretical math of when you will expire");
//                var time_left = Math.floor(((exp_time.valueOf()+(1000*60*5)) - time.valueOf()) / 1000);
//                console.log("seconds left" + time_left);
//                var secs = time_left % 60;
//                var mins = Math.floor(time_left / 60);
//                message = ' Your session expires in '
//                    + mins + ':' + secs + ', <a href="'
//                    + url + '">I\'m still working.</a>';
////            }
//
//            console.log("you will be logged out soooon");
//            if ($("body").hasClass("hasToolBroadcast"))	{
//                message = " - " + message;
//                $('span[name=logout message]').append(message);
//            } else {
//                $("body").attr("class", "hasToolBroadcast");
//                var elem_bcast = document.createElement("div");
//                elem_bcast.classList.add("toolBroadcast");
//                elem_bcast.innerHTML = message;
//
//                $("body").prepend(elem_bcast);
//            }
//
//        } else {
////            console.log("didn't make it into conditional :((((");
////            console.log("exp time:" + exp_time.prototype.toUTCString());
////            console.log("warn time: " + warn_time.prototype.toUTCString());
////            console.log("time: " + time.prototype.toUTCString());
//            console.log(warn_time - time);
//        }


          // make sure reauthenticate works
          // check timing to see if you're getting user max timeout correctly
          // 5 mins?
        }
    });
});