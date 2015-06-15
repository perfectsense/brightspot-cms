define([
    'jquery',
    'bsp-utils',
    'evaporate'],

function ($, bsp_utils, evaporate) {

  bsp_utils.onDomInsert(document, '.evaporate', {

    insert: function (input) {

      var state = {};

      $(input).on('change', function (event) {
        var $this = $(this);
        var files = event.target.files;
        var $inputSmall = $this.closest('.inputSmall');
        var settingsMeta = $inputSmall.find('meta[name="evaporateSettings"]');

        if (settingsMeta.size() === 0) {
          return;
        }

        state.fieldName = settingsMeta.attr('data-field-name');
        state.pathStart = settingsMeta.attr('data-path-start');
        state.storage = settingsMeta.attr('data-storage');
        state.typeId = settingsMeta.attr('data-type-id');

        var isMultiple = $this.attr('multiple') ? true : false;

        if (isMultiple) {
          $this.hide();
        }

        for (var i = 0; i < files.length; i++) {
          var file = files[i];

          _beforeUpload(file, $inputSmall, i);
          var filePath = state.pathStart + encodeURIComponent(file.name);

          (function ($this, file, filePath, i) {
            new Evaporate(JSON.parse(settingsMeta.attr('content'))).add({
              name: filePath,
              file: file,
              notSignedHeadersAtInitiate: {
                'Cache-Control': 'max-age=3600'
              },
              xAmzHeadersAtInitiate: {
                'x-amz-acl': 'public-read'
              },
              signParams: {
                storageSetting: state.storage
              },
              progress: function (progress) {
                _progress($inputSmall, i, Math.round(Number(progress * 100)));
              },
              complete: function () {
                _progress($inputSmall, i, 100);
                if (isMultiple) {
                  _afterBulkUpload($this, $inputSmall, filePath, i);
                } else {
                  _afterUpload($this, $inputSmall, filePath);
                }
              }

            });
          })($this, file, filePath, i);
        }
      });

      function _beforeUpload(file, $inputSmall, index) {
        var $fileSelector = $inputSmall.find('.fileSelector').first();

          $inputSmall.append(_createProgressHtml());
          var $uploadPreview = $inputSmall.find('.upload-preview').eq(index);

          if (file.type.match('image.*')) {
            _displayImgPreview($uploadPreview.find('img').first(), file);
          } else {
            _displayDefaultPreview($uploadPreview);
            $uploadPreview.addClass('loading');
          }

          var $select = $fileSelector.find('select').first();

          if ($select.find('option[value="keep"]').size() < 1) {
            $select.prepend($('<option/>', {
              'data-hide': '.fileSelectorItem',
              'data-show': '.fileSelectorExisting',
              'value': 'keep',
              'text': 'Keep Existing'
            }));
          }

          $select.val('keep');
      }

      function _afterUpload($this, $inputSmall, filePath) {
        var $uploadPreview = $inputSmall.find('.upload-preview');
        var inputName = $this.attr('data-input-name');
        var localImg = $uploadPreview.find('img');
        var localSrc = localImg.first().attr('src');

        var params = {};
        params['inputName'] = inputName;
        params['fieldName'] = state.fieldName;
        params['typeId'] = state.typeId;
        params[inputName + '.path'] = filePath;
        params[inputName + '.storage'] = state.storage;
        params[inputName + '.originalWidth'] = localImg.prop('naturalWidth');

        $uploadPreview.removeClass('loading');

        (function(width) {
          $.ajax({
            url: window.CONTEXT_PATH + 'storageItemField',
            dataType: 'html',
            data: params
          }).done(function (html) {
            $uploadPreview.detach();
            var $response = $(html);

            //$inputSmall.replaceWith(html);

            $inputSmall.find('.fileSelectorItem').hide();
            $inputSmall.find('meta[name="evaporateSettings"]').replaceWith($response.find('meta[name="evaporateSettings"]'));

            var $existingPreview = $inputSmall.find('.filePreview');

            if ($existingPreview.size() === 0) {
              $inputSmall.find('.fileSelector').after($response.find('.filePreview'));
            } else {
              $inputSmall.find('.filePreview').replaceWith($response.find('.filePreview'));
            }

            // prevents image pop-in
            var img = $inputSmall.find('.imageEditor-image').find('img').first();
            img.attr('style', 'max-width: ' + width + 'px;');
            var remoteSrc = img.attr('src');
            img.attr('src', localSrc);
            $.ajax({
              url: remoteSrc
            }).done(function (html) {
              $inputSmall.find('img').attr('src', remoteSrc);
            });
          });
        })(localImg.width());
      }

      function _afterBulkUpload($this, $inputSmall, filePath, index) {
        var $uploadPreview = $inputSmall.find('.upload-preview').eq(index);
        $uploadPreview.removeClass('loading');
        var inputName = "file";

        $this.detach();

        var params = {};
        params['writeInputsOnly'] = true;
        params['inputName'] = inputName;
        params[inputName + '.path'] = filePath;

        $.ajax({
          url: window.CONTEXT_PATH + 'content/uploadFiles',
          dataType: 'html',
          data: params
        }).done(function (html) {
          $inputSmall.prepend(html);
        });
      }

      function _progress($inputSmall, i, percentageComplete) {
        $inputSmall.find('[data-progress]').eq(i).attr('data-progress', percentageComplete);
      }

      function _displayImgPreview(img, file) {

        if (!(window.File && window.FileReader && window.FileList)) {
          return;
        }

        var reader = new FileReader();
        reader.onload = (function (readFile) {
          return function (event) {
            img.attr('src', event.target.result);
            img.closest('.upload-preview').addClass('loading');
          };
        })(file);

        reader.readAsDataURL(file);
      }

      function _createProgressHtml() {
        return $('<div/>', {'class' : 'upload-preview'}).append(
                 $('<div/>', {'class' : 'upload-progress'}).append(
                   $('<img/>')
                 ).append(
                   $('<div/>', {'class' : 'radial-progress', 'data-progress' : '0' }).append(
                     $('<div/>', {'class' : 'circle'}).append(
                       $('<div/>', {'class' : 'mask full'}).append(
                         $('<div/>', {'class' : 'fill'})
                       )
                     ).append(
                       $('<div/>', {'class' : 'mask half'}).append(
                         $('<div/>', {'class': 'fill'})
                       ).append(
                         $('<div/>', {'class': 'fill fix'})
                       )
                     )
                   ).append(
                     $('<div/>', {'class': 'inset'}).append(
                       $('<div/>', {'class': 'percentage'}).append(
                         $('<span/>')
                       )
                     )
                   )
                 )
               );

      }

      function _displayDefaultPreview(uploadPreview) {
        var $uploadPreview = $(uploadPreview);
        var $uploadPreviewWrapper = $uploadPreview.find('.upload-progress').first();

        $uploadPreview.width(150).height(150);
        $uploadPreviewWrapper.width(150).height(150);

      }
    }
  });

});