define([
        'jquery',
        'bsp-utils',
        'evaporate'],

function($, bsp_utils, evaporate) {

    bsp_utils.onDomInsert(document, '.evaporate', {

        insert: function (input) {

            $(input).on('change', function(event) {
                var $this = $(this);
                var files = event.target.files;
                var $inputSmall = $this.closest('.inputSmall');
                var isMultiple = $this.attr('multiple') ? true : false;

                for (var i = 0; i < files.length; i++) {
                    var file = files[i];

                    _beforeUpload(file, $inputSmall, i);
                    var filePath = $this.attr('data-path-start') + "/" + encodeURIComponent(file.name);

                    (function($this, file, filePath, i) {
                        window._e_.add({
                            name: filePath,
                            file: file,
                            notSignedHeadersAtInitiate: {
                                'Cache-Control': 'max-age=3600'
                            },
                            xAmzHeadersAtInitiate: {
                                'x-amz-acl': 'public-read'
                            },
                            complete: function () {
                                if (isMultiple) {
                                    _afterBulkUpload($this, $inputSmall, filePath, i);
                                } else {
                                    _afterUpload($this, $inputSmall, filePath);
                                }
                            },
                            progress: function (progress) {
                                _progress($inputSmall, i, Math.round(Number(progress*100)));
                            }

                        });
                    })($this, file, filePath, i);
                }
            });

            function _beforeUpload(file, $inputSmall, index) {
                var $fileSelector = $inputSmall.find('.fileSelector').first();

                $.ajax({
                    url: '/cms/filePreview',
                    data: { displayProgress : 'true' },
                    dataType: 'html'
                }).done(function(html) {

                    $inputSmall.append(html);
                    var $uploadPreview = $inputSmall.find('.upload-preview').eq(index);

                    if (file.type.match('image.*')) {
                        _displayImgPreview($uploadPreview.find('img').first(), file);
                    } else {
                        _displayDefaultPreview($uploadPreview);
                    }

                    var $select = $fileSelector.find('select').first();

                    if($select.find('option[value="keep"]').size() < 1) {
                        $select.prepend($('<option/>', {
                            'data-hide': '.fileSelectorItem',
                            'data-show': '.fileSelectorExisting',
                            'value': 'keep',
                            'text': 'Keep Existing'
                        }));
                    }

                    $select.val('keep');
                });
            }

            function _afterUpload($this, $inputSmall, filePath) {
                var $uploadPreview  = $inputSmall.find('.upload-preview');
                var inputName = $this.attr('data-input-name');
                var localSrc = $uploadPreview.find('img').first().attr('src');

                var params = { };
                params['isNewUpload'] = true;
                params['inputName'] = inputName;
                params['fieldName'] = $this.attr('data-field-name');
                params['typeId'] = $this.attr('data-type-id');
                params[inputName + '.path'] = filePath;
                params[inputName + '.storage'] = $this.attr('data-storage');

                $uploadPreview.removeClass('loading');

                $.ajax({
                    url: '/cms/filePreview',
                    dataType: 'html',
                    data: params
                }).done(function(html) {
                    $uploadPreview.detach();
                    $inputSmall.append(html);

                    //prevent image pop-in
                    var img = $inputSmall.find('.imageEditor-image').find('img').first();
                    var remoteSrc = img.attr('src');
                    img.attr('src', localSrc);
                    $.ajax({
                        url: remoteSrc
                    }).done(function(html) {
                        img.attr('src', remoteSrc);
                    });
                });
            }

            function _afterBulkUpload($this, $inputSmall, filePath, index) {
                var $uploadPreview  = $inputSmall.find('.upload-preview').eq(index);
                $uploadPreview.removeClass('loading');
                var inputName = "file";

                $this.detach();

                var params = { };
                params['writeInputsOnly'] = true;
                params['inputName'] = inputName;
                params[inputName + '.path'] = filePath;

                $.ajax({
                    url: '/cms/content/uploadFiles',
                    dataType: 'html',
                    data: params
                }).done(function(html) {
                    $inputSmall.prepend(html);
                });
            }

            function _progress($inputSmall, i, percentageComplete) {
                $inputSmall.find('[data-progress]').eq(i).attr('data-progress', percentageComplete);
            }

            function _displayImgPreview(img, file) {

                if(!(window.File && window.FileReader && window.FileList)) {
                    return;
                }

                var reader = new FileReader();
                reader.onload = (function(readFile) {
                    return function(event) {
                        img.attr('src', event.target.result);
                    };
                })(file);

                reader.readAsDataURL(file);
            }

            function _displayDefaultPreview(uploadPreview) {
                var $uploadPreview = $(uploadPreview);
                var $uploadPreviewWrapper = $uploadPreview.find('.upload-preview-wrapper').first();

                $uploadPreview.width(150).height(150);
                $uploadPreviewWrapper.width(150).height(150);

            }
        }
    });


});