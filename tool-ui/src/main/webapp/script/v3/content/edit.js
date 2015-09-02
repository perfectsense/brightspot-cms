define([ 'jquery', 'v3/rtc' ], function($, rtc) {

  rtc.receive('com.psddev.cms.tool.page.content.EditFieldUpdateBroadcast', function(data) {
    var userId = data.userId;
    var userName = data.userName;
    var fieldNamesByObjectId = data.fieldNamesByObjectId;

    $('.inputPending[data-user-id="' + userId + '"]').each(function() {
      var $pending = $(this);

      $pending.closest('.inputContainer').removeClass('inputContainer-pending');
      $pending.remove();
    });

    if (!fieldNamesByObjectId) {
      return;
    }

    var contentId = data.contentId;

    $.each(fieldNamesByObjectId, function (objectId, fieldNames) {
      var $inputs = $('form[data-rtc-content-id="' + contentId + '"] .objectInputs[data-id="' + objectId + '"]');

      if ($inputs.length === 0) {
        return;
      }

      $.each(fieldNames, function (i, fieldName) {
        var $container = $inputs.find('> .inputContainer[data-field-name="' + fieldName + '"]');
        var nested = false;

        $container.find('.objectInputs').each(function() {
          if (fieldNamesByObjectId[$(this).attr('data-id')]) {
            nested = true;
            return false;
          }
        });

        if (!nested) {
          $container.addClass('inputContainer-pending');

          $container.find('> .inputLabel').after($('<div/>', {
            'class': 'inputPending',
            'data-user-id': userId,
            'html': [
              'Pending edit from ' + userName + ' - ',
              $('<a/>', {
                'text': 'Unlock',
                'click': function() {
                  if (confirm('Are you sure you want to forcefully unlock this field?')) {
                    rtc.execute('com.psddev.cms.tool.page.content.EditFieldUpdateAction', {
                      contentId: $container.closest('form').attr('data-rtc-content-id'),
                      unlockObjectId: $container.closest('.objectInputs').attr('data-id'),
                      unlockFieldName: $container.attr('data-field-name')
                    });
                  }

                  return false;
                }
              })
            ]
          }));
        }
      });
    });
  });

  rtc.receive('com.psddev.cms.tool.page.content.PublishBroadcast', function(data) {
    var newValues = data.values;
    var newValuesId = newValues._id;
    var oldValues = $('input[name="' + newValuesId + '/oldValues"]').val();

    if (oldValues) {
      var contentId = data.contentId;
      var userId = data.userId;
      var userName = data.userName;

      function removeUpdated() {
        var $updated = $(this);

        $updated.closest('.inputContainer').removeClass('inputContainer-updated');
        $updated.remove();
      }

      $('.inputUpdated[data-user-id="' + userId + '"]').each(removeUpdated);

      function compare(objectId, oldValues, newValues) {
        $.each(oldValues, function(fieldName, oldValue) {
          var oldValueId = oldValue ? oldValue._id : null;
          var newValue = newValues[fieldName];

          if (oldValueId) {
            compare(oldValueId, oldValue, newValue);

          } else if (JSON.stringify(oldValue) !== JSON.stringify(newValue)) {
            var $container = $('[data-rtc-content-id="' + contentId + '"] .objectInputs[data-id="' + objectId + '"] > .inputContainer[data-field-name="' + fieldName + '"]');
            var $form = $container.closest('form');

            if ($form.length > 0 && !$.data($form[0], 'content-edit-submit')) {
              $container.addClass('inputContainer-updated');

              $container.find('> .inputLabel').after($('<div/>', {
                'class': 'inputUpdated',
                'data-user-id': userId,
                'html': [
                  'Updated by ' + userName + ' at ' + new Date(data.date) + ' - ',
                  $('<a/>', {
                    'text': 'Ignore',
                    'click': function() {
                      if (confirm('Are you sure you want to ignore updates to this field and edit it anyway?')) {
                        $(this).closest('.inputUpdated').each(removeUpdated);
                      }

                      return false;
                    }
                  })
                ]
              }));
            }
          }
        });
      }

      compare(newValuesId, $.parseJSON(oldValues), newValues);
    }
  });

  $('.contentForm').each(function() {
    var $form = $(this);
    var contentId = $form.attr('data-rtc-content-id');

    function update() {
      var fieldNamesByObjectId = { };

      $form.find('.inputContainer.state-changed, .inputContainer.state-focus').each(function () {
        var $container = $(this);
        var objectId = $container.closest('.objectInputs').attr('data-id');

        (fieldNamesByObjectId[objectId] = fieldNamesByObjectId[objectId] || [ ]).push($container.attr('data-field-name'));
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
