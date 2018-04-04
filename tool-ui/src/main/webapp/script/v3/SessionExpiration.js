define([
    'jquery',
    'bsp-utils' ],

    function($, bsp_utils) {
        bsp_utils.onDomInsert(document, 'meta[name=bsp-tu-sessionExpiration]', {
            insert: function () {
                var warnMins = 5
                var expireTime = new Date(getLatestSessionExpiration())
                var expireMillis = Math.max(0, expireTime.getTime() - Date.now())
                var goTime = Math.max(0, expireMillis + (-1000 * 60 * warnMins))

                var url = window.location.href

                setTimeout(function() {
                    // construct warning message and redirect url
                    var message = constructMessage(Math.max(0, expireTime.getTime() - Date.now()), url)

                    if ($(".widget-logIn").size() == 0) {
                        var span = $("<span>").attr("name", "logout-message")
                                              .html(message)

                        if ($("body").hasClass("hasToolBroadcast"))	{
                            if ($("span[name=logout-message]").size() == 0) {
                                $(".toolBroadcast").append(" - ").append(span)
                            }
                        } else {
                            $("body").addClass("hasToolBroadcast")
                            var broadcastElem = $("<div>").addClass("toolBroadcast")
                            broadcastElem.append(span)
                            $("body").prepend(broadcastElem)
                        }
                    }

                    $("span[name=logout-message]").click(function(e) {
                        e.preventDefault()
                        $.post( url, { _renewSession: "true" } , function(success) { location.reload() })
                    })

                    var warningInterval = setInterval(function() {
                        expireTime = new Date(getLatestSessionExpiration(expireTime))

                        var message = constructMessage(Math.max(0, expireTime.getTime() - Date.now()),
                            url,
                            warningInterval)

                        $("span[name=logout-message]").html(message)

                    }, 1000)
                }, goTime)

                function getLatestSessionExpiration (expireTime) {
                    if (!expireTime) {
                        var expireTime = new Date(Date.parse($('meta[name=bsp-tu-sessionExpiration]').attr('content')))
                    } else {
                        expireTime = new Date(expireTime)
                    }

                    var maxSessionExpiration = new Date(localStorage.getItem("sessionExpiration"))
                    // if local storage not set or needs updating
                    if (!maxSessionExpiration || expireTime > maxSessionExpiration) {
                        localStorage.setItem("sessionExpiration", expireTime)
                    // if local storage has been updated in another window update here
                    } else if (maxSessionExpiration > expireTime) {
                        expireTime = maxSessionExpiration
                    }

                    return expireTime
                }

                function constructMessage(expireMillis, url, intervalFunc) {
                    if (expireMillis <= 0) {
                        message = "You have been logged out and your work may not be saved."
                        if (intervalFunc) {
                            clearInterval(intervalFunc)
                        }
                    } else {
                        // calculate time left.
                        timeLeft = new Date(expireMillis)

                        message = ' You will be logged out in '
                            + (("0" + timeLeft.getMinutes()).slice(-2)) + ':'
                            + (("0" + timeLeft.getSeconds()).slice(-2))

                        if (url) {
                            message += ', <a href="' + url + '">I\'m still working.</a>'
                        }
                    }
                    return message
                }

            }
        })
    })
