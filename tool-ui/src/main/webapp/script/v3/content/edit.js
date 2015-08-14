define([ 'jquery', 'v3/rtc' ], function($, rtc) {

  function removeStatuses(userId) {
    $('.inputStatus[data-user-id="' + userId + '"]').each(function() {
      var $status = $(this);
      var $container = $status.closest('.inputContainer');

      if ($container.is('.inputContainer-updated')) {
        return;
      }

      if (!$container.is('.inputContainer-readOnly')) {
        $container.removeClass('inputContainer-pending');
      }

      $status.remove();
    });
  }

  function updateStatus($container, userId, message) {
    if ($container.length === 0 || $container.is('.inputContainer-updated')) {
      return;
    }

    if (!$container.is('.inputContainer-readOnly')) {
      $container.addClass('inputContainer-pending');
    }

    $container.find('> .inputLabel').after($('<div/>', {
      'class': 'inputStatus',
      'data-user-id': userId,
      'html': [
        message + ' - ',
        $('<a/>', {
          'text': 'Unlock',
          'click': function() {
            if (confirm('Are you sure you want to forcefully unlock this field?')) {
              rtc.execute('com.psddev.cms.tool.page.content.EditFieldUpdateAction', {
                contentId: $container.closest('form').attr('data-o-id'),
                unlockObjectId: $container.closest('.objectInputs').attr('data-id'),
                unlockFieldName: $container.attr('data-field')
              });
            }

            return false;
          }
        })
      ]
    }));
  }

  rtc.receive('com.psddev.cms.tool.page.content.EditFieldUpdateBroadcast', function(data) {
    var userId = data.userId;
    var userName = data.userName;
    var fieldNamesByObjectId = data.fieldNamesByObjectId;

    removeStatuses(userId);

    if (!fieldNamesByObjectId) {
      return;
    }

    $.each(fieldNamesByObjectId, function (objectId, fieldNames) {
      var $inputs = $('.objectInputs[data-id="' + objectId + '"]');

      $.each(fieldNames, function (i, fieldName) {
        updateStatus(
            $inputs.find('> .inputContainer[data-field="' + fieldName + '"]'),
            userId,
            'Pending edit from ' + userName);
      });
    });
  });

  rtc.receive('com.psddev.cms.tool.page.content.PublishBroadcast', function(data) {
    var newValues = data.values;
    var contentId = data.contentId;
    var oldValues = $('input[name="' + contentId + '/oldValues"]').val();

    if (oldValues) {
      var userId = data.userId;
      var userName = data.userName;

      removeStatuses(userId);

      $.each($.parseJSON(oldValues), function(fieldName, oldValue) {
        if (JSON.stringify(oldValue) !== JSON.stringify(newValues[fieldName])) {
          var $container = $('.objectInputs[data-id="' + contentId + '"] > .inputContainer[data-field="' + fieldName + '"]');
          var $form = $container.closest('form');

          if ($form.length > 0 && !$.data($form[0], 'content-edit-submit')) {
            updateStatus($container, userId, 'Updated by ' + userName + ' at ' + new Date(data.date));
            $container.addClass('inputContainer-updated');
          }
        }
      });
    }
  });

  $('.contentForm').each(function() {
    var $form = $(this);
    var contentId = $form.attr('data-o-id');

    function update() {
      var fieldNamesByObjectId = { };

      $form.find('.inputContainer.state-changed, .inputContainer.state-focus').each(function () {
        var $container = $(this);
        var objectId = $container.closest('.objectInputs').attr('data-id');

        (fieldNamesByObjectId[objectId] = fieldNamesByObjectId[objectId] || [ ]).push($container.attr('data-field'));
      });

      if (fieldNamesByObjectId) {
        rtc.execute('com.psddev.cms.tool.page.content.EditFieldUpdateAction', {
          contentId: contentId,
          fieldNamesByObjectId: fieldNamesByObjectId
        });
      }
    }

    rtc.restore('com.psddev.cms.tool.page.content.EditFieldUpdateState', {
      contentId: contentId
    }, update);

    var updateTimeout;

    $(document).on('blur focus change', '.contentForm :input', function() {
      if (updateTimeout) {
        clearTimeout(updateTimeout);
      }

      updateTimeout = setTimeout(function() {
        updateTimeout = null;
        update();
      }, 50);
    });

    $form.on('submit', function() {
      $.data($form[0], 'content-edit-submit', true);
    })
  });
});
