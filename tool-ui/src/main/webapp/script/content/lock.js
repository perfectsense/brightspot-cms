define([
    'jquery',
    'bsp-utils' ],

function($, bsp_utils) {
  $(window).load(function() {
    bsp_utils.onDomInsert(document, '.contentLock[data-content-locked-out = "true"] :input', {
      'insert': function (input) {
        $(input).trigger('input-disable', [ true ]);
      }
    });
  });

    var KEY_PREFIX = "cms.contentLock.";
    var STORAGE = window.localStorage;

    function lock(contentId) {
        STORAGE.setItem(KEY_PREFIX + contentId, +new Date());
    }

    function unlock(contentId) {
        STORAGE.removeItem(KEY_PREFIX + contentId);

        $.ajax({
            'url': CONTEXT_PATH + '/contentUnlock',
            'data': { 'id': contentId },
            'async': false,
            'cache': false
        });
    }

    setInterval(function() {
        var itemIndex;
        var itemLength = STORAGE.length;
        var key;
        var now = +new Date();
        var contentId;
        var contentIdsToUnlock = [];

        for (itemIndex = 0; itemIndex < itemLength; ++ itemIndex) {
            key = STORAGE.key(itemIndex);

            if (key &&
                    key.indexOf(KEY_PREFIX, 0) === 0 &&
                    parseInt(STORAGE.getItem(key), 10) + 5000 < now) {
                contentId = key.substring(KEY_PREFIX.length);

                contentIdsToUnlock.push(contentId);
            }
        }

        itemLength = contentIdsToUnlock.length;

        for (itemIndex = 0; itemIndex < itemLength; ++ itemIndex) {
            unlock(contentIdsToUnlock[itemIndex]);
        }
    }, 1000);

    window.bspContentLock = lock;
    window.bspContentUnlcok = unlock;

    return {
        'lock': lock,
        'unlock': unlock
    };
});
