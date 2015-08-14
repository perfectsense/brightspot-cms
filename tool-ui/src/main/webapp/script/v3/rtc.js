define([ 'jquery', 'bsp-utils', 'atmosphere' ], function($, bsp_utils, atmosphere) {
  var request = {
    url: '/_rtc',
    contentType: 'application/json',
    fallbackTransport: 'sse',
    maxReconnectOnClose: 0,
    trackMessageLength: true,
    transport: 'sse'
  };

  var socket;
  var subscribe = bsp_utils.throttle(5000, function() {
    socket = atmosphere.subscribe(request);
  });

  var restores = [ ];

  var broadcastCallbacks = { };

  var isOnline = false;
  var offlineMessages = [ ];
  var onlineMessages = {
    push: function(message) {
      socket.push(JSON.stringify($.extend(message, {
        resource: socket.getUUID()
      })));
    }
  };

  request.onOpen = function() {
    isOnline = true;

    $.each(restores, function(i, restore) {
      onlineMessages.push(restore.message);

      var callback = restore.callback;

      if (callback) {
        callback();
      }
    });

    $.each(offlineMessages, function(i, message) {
      onlineMessages.push(message);
    });

    offlineMessages = [ ];
  };

  request.onClose = function() {
    isOnline = false;
  };

  request.onError = function() {
    isOnline = false;

    subscribe();
  };

  request.onMessage = function(response) {
    var message = JSON.parse(response.responseBody);
    var callbacks = broadcastCallbacks[message.broadcast];

    if (callbacks) {
      $.each(callbacks, function(i, callback) {
        callback(message.data);
      });
    }
  };

  subscribe();

  return {
    restore: function(state, data, callback) {
      restores.push({
        callback: callback,
        message: {
          state: state,
          data: data
        }
      });
    },

    receive: function(broadcast, callback) {
      (broadcastCallbacks[broadcast] = broadcastCallbacks[broadcast] || [ ]).push(callback);
    },

    execute: function(action, data) {
      (isOnline ? onlineMessages : offlineMessages).push({
        action: action,
        data: data
      });
    }
  };
});
