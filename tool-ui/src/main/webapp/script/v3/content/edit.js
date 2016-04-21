define([ 'jquery', 'bsp-utils', 'v3/rtc' ], function($, bsp_utils, rtc) {

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
    var $oldValuesInput = $('input[name="' + newValuesId + '/oldValues"]');

    $oldValuesInput.off('change.publish-broadcast');
    $oldValuesInput.on('change.publish-broadcast', update);

    function update() {
      var oldValues = $oldValuesInput.val();

      if (oldValues) {
        var userId = data.userId;
        var userName = data.userName;

        function removeUpdated() {
          var $updated = $(this);
          var $container = $updated.closest('.inputContainer');

          $updated.remove();

          if ($container.find('> .inputUpdated').length === 0) {
            $container.removeClass('inputContainer-updated');
          }
        }

        $('.inputUpdated[data-user-id="' + userId + '"]').each(removeUpdated);

        function compare(objectId, oldValues, newValues) {
          $.each(oldValues, function (fieldName, oldValue) {
            var oldValueId = oldValue ? oldValue._id : null;
            var newValue = newValues[fieldName];

            if (oldValueId) {
              compare(oldValueId, oldValue, newValue);

            } else if (JSON.stringify(oldValue) !== JSON.stringify(newValue)) {
              var $container = $('[data-rtc-content-id="' + newValuesId + '"] .objectInputs[data-id="' + objectId + '"] > .inputContainer[data-field-name="' + fieldName + '"]');
              var $form = $container.closest('form');

              if ($form.length > 0) {
                $container.addClass('inputContainer-updated');

                $container.find('> .inputLabel').after($('<div/>', {
                  'class': 'inputUpdated',
                  'data-user-id': userId,
                  'html': [
                    'Updated by ' + userName + ' at ' + new Date(data.date) + ' - ',
                    $('<a/>', {
                      'text': 'Ignore',
                      'click': function () {
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
    }

    update();
  });

  bsp_utils.onDomInsert(document, '.contentForm', {
    insert: function (form) {
      var $form = $(form);
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

      // Tab navigation from textarea or record input to RTE.
      $(document).on('keydown', '.contentForm :text, .contentForm textarea, .objectId-select', function (event) {
        if (event.which === 9) {
          var $container = $(this).closest('.inputContainer');
          var rte2 = $container.next('.inputContainer').find('> .inputSmall > .rte2-wrapper').data('rte2');

          if (rte2) {
            rte2.rte.focus();
            return false;
          }
        }

        return true;
      });
    }
  });

  // Add the new item to the search results.
  bsp_utils.onDomInsert(document, '.contentForm', {
    insert: function (form) {
      var $form = $(form);

      if ($form.attr('data-new') === 'false') {
        var $source = $form.popup('source');

        if ($source) {
          var $forms = $source.closest('.searchControls').find('> .searchFilters > form');

          if ($forms.length > 0) {
            var contentId = $form.attr('data-content-id');
            var added = false;

            $forms.each(function () {
              var $form = $(this);

              if ($form.find('input[type="hidden"][name="ni"][value="' + contentId + '"]').length === 0) {
                added = true;

                $form.append($('<input/>', {
                  type: 'hidden',
                  name: 'ni',
                  value: contentId
                }));
              }
            });

            if (added) {
              $forms.first().submit();
            }
          }
        }
      }
    }
  });
});
