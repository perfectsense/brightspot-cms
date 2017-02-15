define([
    'jquery',
    'bsp-utils',
    'bsp-uploader' ],
function ($, bsp_utils, uploader) {

  bsp_utils.onDomInsert(document, 'input[type="file"]', {
    insert: function (input) {
      var $input = $(input);

      $input.wrap(function () {
        return $('<label/>', {
          'class': 'UploadChooser',
          'data-placeholder': $(this).attr('placeholder')
        });
      });

      $input.on('change', function (event) {
        var $label = $(event.target).closest('label');
        var files = event.target.files;

        if (files) {
          var length = files.length;

          if (length === 1) {
            $label.attr('data-value', files[0].name);

          } else if (length > 1) {
            $label.attr('data-value', length + ' files');

          } else {
            $label.removeAttr('data-value');
          }
        }
      });
    }
  });

  function before() {
    var plugin = this;
    var $input = plugin.el;

    if (!plugin.isMultiple) {
      var $fileSelector = $input.closest('.fileSelector');

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
    } else {
      $input.hide();
    }
  }

  function findInputWrapper ($input) {
    var $small = $input.closest('.inputSmall');
    var $wrapper = $small.next('.inputLarge');

    if ($wrapper.length === 0) {
      $wrapper = $('<div/>', {
        'class': 'inputLarge'
      });

      $small.after($wrapper);
    }

    return $wrapper;
  }

  function beforeEach(request, file, i) {
    var plugin = this;
    var $input = plugin.el;
    var file = plugin.files[i];
    var $inputWrapper = findInputWrapper($input);

    $inputWrapper.append(_createProgressHtml());
    var $uploadPreview = $inputWrapper.find('.upload-preview').eq(i);

    if (file.type.match('image.*') && !plugin.isMultiple) {
      _displayImgPreview($uploadPreview.find('img').first(), file);
    } else {
      _displayDefaultPreview($uploadPreview);
      $uploadPreview.addClass('loading');
    }
  }

  function progress(event, i) {
    var plugin = this;

    var prog = findInputWrapper(plugin.el)
      .find('[data-progress]')
      .eq(i);

    // Updates progress bar to a randomized maximum between 80 and 94.
    prog.attr('data-progress', Math.min(
      Math.round(event.loaded * 100 / event.total),
      Math.max(
        prog.attr('data-progress'),
        Math.round(Math.random() * (94 - 80) + 80)
      ))
    );
  }

  function afterEach(request, file, i) {
    var plugin = this;
    var $input = plugin.el;
    var $inputWrapper = findInputWrapper($input);

    if (request.status == 200) {
      $inputWrapper
        .find('[data-progress]')
        .eq(i)
        .attr('data-progress', 100);

      handleSuccess(plugin, request, file, i);
    } else {
      handleError(plugin, request, file, i);
    }
  }

  function after() {
    var plugin = this;

    // original file input has been replaced
    // with hidden inputs with json values
    if (plugin.isMultiple) {
      plugin.el.attr('name', '');
    }
  }

  function handleSuccess(plugin, request, file, i) {

    var $input, $inputWrapper, inputName, params, $uploadPreview, response;

    $input = plugin.el;
    $inputWrapper = findInputWrapper($input);
    $uploadPreview = $inputWrapper.find('.upload-preview').eq(i);
    response = request.responseText;

    if (!plugin.isMultiple) {

      // Handle field upload UI

      inputName = $input.attr('data-input-name');
      var jsonParamName = inputName + '.file.json';
      //var localImg = $uploadPreview.find('img');
      //var localSrc = localImg.first().attr('src');

      params = {};
      params['inputName'] = inputName;
      params['fieldName'] = $inputWrapper.parent().attr('data-field-name');
      params['typeId'] = $input.data('type-id');
      params[jsonParamName] = response;
      params[inputName + '.action'] = 'keep';

      $uploadPreview.removeClass('loading');

      $.ajax({
        url: window.CONTEXT_PATH + 'content/field/file',
        type: 'POST',
        dataType: 'html',
        data: params
      }).done(function (htmlResponse) {
        $uploadPreview.detach();
        var $response = $(htmlResponse);

        $inputWrapper.after($response);
        $inputWrapper.prev('.inputSmall').remove();
        $inputWrapper.remove();
        $response.find('.fileSelectorItem:not(.fileSelectorExisting)').hide();

        //// prevents image pop-in
        //var img = $inputWrapper.find('.imageEditor-image').find('img').first();
        //img.attr('style', 'max-width: ' + width + 'px;');
        //var remoteSrc = img.attr('src');
        //img.attr('src', localSrc);
        //$.ajax({
        //  url: remoteSrc
        //}).done(function (html) {
        //  $inputWrapper.find('img').attr('src', remoteSrc);
        //});
      });
    } else {

      // Handle Bulk Upload UI

      params = {};
      inputName = $input.attr('name');
      params['preview'] = true;
      params['inputName'] = inputName;
      params[inputName] = response;

      // gets dims image preview
      $.ajax({
        url: window.CONTEXT_PATH + 'content/upload',
        type: 'POST',
        dataType: 'html',
        data: params
      }).done(function (html) {
        var $html = $(html);

        var $img = $($html.find('img'));

        if ($img.size() > 0) {
          var src = $img.attr('src');
          $img.attr('src', '');

          $img.load(function() {
            $uploadPreview.find('.upload-progress').detach();
            $img.show();
            $uploadPreview.removeClass('loading');
            $uploadPreview.append($('<div/>', {
              'class' : 'upload-preview-label'
            }).text(file.name));
          });

          $img.hide();
          $uploadPreview.prepend($img);
          $img.attr('src', src);
        }

        $inputWrapper.prepend($('<input />', {
          'type': 'hidden',
          'name': inputName,
          'value': response
        }));
      });
    }
  }

  function handleError(plugin, request, file, i) {

    var $input, $inputWrapper, inputName, params, $uploadPreview, response;

    $input = plugin.el;
    $inputWrapper = findInputWrapper($input);
    $uploadPreview = $inputWrapper.find('.upload-preview').eq(i);
    $uploadPreview.addClass('error');
  }

  // TODO: move this to bsp-uploader
  function _displayImgPreview(el, file) {

    if (!(window.File && window.FileReader && window.FileList)) {
      return;
    }

    var reader = new FileReader();
    reader.onload = (function (readFile) {
      return function (event) {
        el.attr('src', event.target.result);
        el.closest('.upload-preview').addClass('loading');
      };
    })(file);

    reader.readAsDataURL(file);
  }

  function _displayDefaultPreview(uploadPreview) {
    var $uploadPreview = $(uploadPreview);
    var $uploadPreviewWrapper = $uploadPreview.find('.upload-progress').first();

    $uploadPreview.width(150).height(150);
    $uploadPreviewWrapper.width(150).height(150);

  }

  function _createProgressHtml() {

    return $('<div/>', {'class': 'upload-preview'}).append(
      $('<div/>', {'class': 'upload-progress'}).append(
        $('<img/>')
      ).append(
        $('<div/>', {'class': 'radial-progress', 'data-progress': '0'}).append(
          $('<div/>', {'class': 'circle'}).append(
            $('<div/>', {'class': 'mask full'}).append(
              $('<div/>', {'class': 'fill'})
            )
          ).append(
            $('<div/>', {'class': 'mask half'}).append(
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

  return bsp_utils.plugin(false, 'bsp', 'uploader', {
    '_each': function (input) {
      var plugin = Object.create(uploader);
      plugin.init($(input), {
        path : window.UPLOAD_PATH
      });

      plugin.before = before;
      plugin.beforeEach = beforeEach;
      plugin.progress = progress;
      plugin.afterEach = afterEach;
      plugin.after = after;
    }
  });

});

//# sourceURL=file.js