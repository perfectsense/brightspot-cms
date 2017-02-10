define('jquery', [ ], function() { return $; });
define('jquery.extra', [ ], function() { });
define('jquery.handsontable.full', [ ], function() { });
define('d3', [ ], function() { return d3; });
define('moment', [ 'moment-with-locales' ], function(moment) { return moment; });

requirejs.config({
  shim: {
    'leaflet.common': [ 'leaflet' ],
    'leaflet.draw': [ 'leaflet' ],
    'l.control.geosearch': [ 'leaflet' ],
    'l.geosearch.provider.openstreetmap': [ 'l.control.geosearch' ],
    'L.Control.Locate': [ 'leaflet' ],
    'nv.d3': [ 'd3' ],
    'pixastic/actions/blurfast': [ 'pixastic/pixastic.core' ],
    'pixastic/actions/brightness': [ 'pixastic/pixastic.core' ],
    'pixastic/actions/crop': [ 'pixastic/pixastic.core' ],
    'pixastic/actions/desaturate': [ 'pixastic/pixastic.core' ],
    'pixastic/actions/fliph': [ 'pixastic/pixastic.core' ],
    'pixastic/actions/flipv': [ 'pixastic/pixastic.core' ],
    'pixastic/actions/invert': [ 'pixastic/pixastic.core' ],
    'pixastic/actions/rotate': [ 'pixastic/pixastic.core' ],
    'pixastic/actions/sepia': [ 'pixastic/pixastic.core' ],
    'pixastic/actions/sharpen': [ 'pixastic/pixastic.core' ]
  }
});

require([
  'jquery',
  'jquery.extra',

  'v3/plugin/auto-expand',
  'v3/plugin/auto-submit',
  'bsp-uploader',
  'bsp-utils',
  'iframeResizer',
  'jquery.mousewheel',
  'velocity',

  'v3/input/carousel',
  'input/code',
  'input/color',
  'v3/color-utils',
  'v3/input/file',
  'input/focus',
  'input/grid',
  'v3/input/image',
  'v3/input/image2',
  'input/location',
  'v3/input/object',
  'input/query',
  'v3/input/read-only',
  'input/region',
  'v3/input/richtext',
  'v3/input/richtext2',
  'v3/input/secret',
  'input/table',
  'input/workflow',

  'jquery.calendar',
  'v3/jquery.dropdown',
  'jquery.editableplaceholder',
  'v3/plugin/popup',
  'v3/plugin/fixed-scrollable',
  'v3/jquery.frame',
  'v3/infinitescroll',
  'jquery.lazyload',
  'jquery.pagelayout',
  'jquery.pagethumbnails',
  'v3/jquery.repeatable',
  'jquery.sortable',
  'v3/searchcarousel',
  'jquery.tabbed',
  'v3/taxonomy',
  'jquery.toggleable',
  'nv.d3',

  'v3/Dropbox',
  'v3/EditFieldUpdate',

  'v3/dashboard',
  'v3/sticky',
  'v3/content/diff',
  'v3/content/edit',
  'content/lock',
  'v3/content/publish',
  'content/layout-element',
  'v3/content/state',
  'v3/csrf',
  'v3/search',
  'v3/search-fields',
  'v3/search-filters',
  'v3/search-result-check',
  'v3/tabs' ],

function() {
  var $ = arguments[0];
  var bsp_autoExpand = arguments[2];
  var bsp_autoSubmit = arguments[3];
  var bsp_utils = arguments[5];
  var win = window;
  var undef;
  var $win = $(win),
      doc = win.document,
      $doc = $(doc);

  $.addToolCheck = function() {
  };

  // Standard behaviors.
  $doc.repeatable('live', '.repeatableForm, .repeatableInputs, .repeatableLayout, .repeatableObjectId');
  $doc.repeatable('live', '.repeatableText', {
    'addButtonText': '',
    'removeButtonText': '',
    'restoreButtonText': ''
  });

  bsp_autoExpand.live(document, 'input[type="text"].expandable, input:not([type]).expandable, textarea');
  bsp_autoSubmit.live(document, '.autoSubmit');

  $doc.calendar('live', ':text.date');
  $doc.dropDown('live', 'select');
  $doc.editablePlaceholder('live', ':input[data-editable-placeholder]');

  bsp_utils.onDomInsert(document, '.ExternalPreviewFrame', {
    insert: function (frame) {
      var $frame = $(frame);

      $frame.iFrameResize({
        resizedCallback: function () {
          setTimeout(function () {
            $frame.resize();
          }, 0);
        }
      });
    }
  });

  bsp_fixedScrollable.live(document, [
    '.fixedScrollable',
    '.searchResult-list',
    '.searchResultTaxonomyColumn ul',
    '.searchFiltersRest',
    '.popup[data-popup-source-class~="objectId-select"] .searchResultList',
    '.popup[data-popup-source-class~="rte2-enhancement-toolbar-change"] .searchResultList',
    '.searchResult-actions-body',
    '.ToolUserWorksInProgress-body'
  ].join(','));

  $doc.frame({
    'frameClassName': 'frame',
    'loadingClassName': 'loading',
    'loadedClassName': 'loaded'
  });

  bsp_utils.onDomInsert(document, '.CodeMirror', {
    'insert': function(cm) {
      bsp_utils.addDomInsertBlacklist(cm);
    }
  });

  bsp_utils.onDomInsert(document, '[data-bsp-autosubmit], .autoSubmit', {
    'insert': function(item) {
      var $form = $(item).closest('form');
      var $targetFrame = $('.frame[name=' + $form.attr('target') + ']:not(.loading):not(.loaded)');

      if ($targetFrame.length > 0) {
        $form.submit();
      }
    }
  });

  $doc.lazyLoad('live', '.lazyLoad');
  $doc.locationMap('live', '.locationMap');
  $doc.objectId('live', ':input.objectId');
  $doc.pageLayout('live', '.pageLayout');
  $doc.pageThumbnails('live', '.pageThumbnails');
  $doc.regionMap('live', '.regionMap');

  if (window.DISABLE_CODE_MIRROR_RICH_TEXT_EDITOR) {
    $doc.rte('live', '.richtext');

  } else {
    $doc.rte2('live', '.richtext');
  }

  $doc.tabbed('live', '.tabbed, .objectInputs');
  $doc.toggleable('live', '.toggleable');

  // Remove placeholder text over search input when there's text.
  $doc.onCreate('.searchInput', function() {
    var $container = $(this),
        $label = $container.find('> label'),
        $input = $container.find('> :text');

    $input.bind('input', $.run(function() {
      $label.toggle(!$input.val());
    }));
  });

  // Hide non-essential items in the permissions input.
  $doc.on('change', '.inputContainer .permissions select', function () {
    var $select = $(this);

    $select.parent().find('> h2, > ul').toggle($select.find(':selected').val() === 'some');
  });

  bsp_utils.onDomInsert(document, '.inputContainer .permissions select', {
    afterInsert: function (selects) {
      var $hide = $();

      $(selects).each(function () {
        var $select = $(this);

        if ($select.val() !== 'some') {
          $hide = $hide.add($select.parent().find('> h2, > ul'));
        }
      });

      $hide.hide();
    }
  });

  $doc.onCreate('.searchSuggestionsForm', function() {
    var $suggestionsForm = $(this),
        $source = $suggestionsForm.popup('source'),
        $contentForm = $source.closest('.contentForm'),
        search;

    if ($contentForm.length === 0) {
      return;
    }

    search = win.location.search;
    search += search.indexOf('?') > -1 ? '&' : '?';
    search += 'id=' + $contentForm.attr('data-content-id');

    $.ajax({
      'data': $contentForm.serialize(),
      'type': 'post',
      'url': CONTEXT_PATH + '/content/state.jsp' + search,
      'complete': function(request) {
        if ($suggestionsForm.closest('body').length === 0) {
          return;
        }

        $suggestionsForm.append($('<input/>', {
          'type': 'hidden',
          'name': 'object',
          'value': request.responseText
        }));

        $suggestionsForm.append($('<input/>', {
          'type': 'hidden',
          'name': 'field',
          'value': $source.closest('.inputContainer').attr('data-field')
        }));

        $suggestionsForm.submit();
      }
    });
  });

  $doc.on('click', '.searchResultTaxonomyExpand', function() {
    var $this = $(this);
    var selectedClass = 'state-selected';
    $this.closest('ul').find('.' + selectedClass).removeClass(selectedClass);
    $this.closest('li').addClass(selectedClass);
  });

  $doc.onCreate('.searchAdvancedResult', function() {
    var $result = $(this),
        checked;

    $result.find('thead tr:first th:first').append($('<div/>', {
      'html': $('<span/>', {
        'class': 'icon icon-check icon-only'
      }),

      'css': {
        'cursor': 'pointer',
        'text-align': 'center',
        'user-select': 'none',
        'vertical-align': 'middle'
      },

      'click': function() {
        var $div = $(this);

        checked = !checked;

        $div.closest('table').find(':checkbox').prop('checked', checked);
        $div.find('.icon').removeClass('icon-check icon-check-empty').addClass(checked ? 'icon-check-empty' : 'icon-check');
        return false;
      }
    }));

    $result.on('change', ':checkbox', function() {
      $result.find('.actions .action').each(function() {
        var $action= $(this),
            text = $action.text();

        if ($result.find(':checkbox:checked').length > 0) {
          $action.text(text.replace('All', 'Selected'));
        } else {
          $action.text(text.replace('Selected', 'All'));
        }
      });
    });
  });

  // Show stack trace when clicking on the exception message.
  $doc.delegate('.exception > *', 'click', function() {
    $(this).find('> .stackTrace').toggle();
  });

  // Soft validation based on suggested sizes.
  (function() {
    var TRIM_RE = /^\s+|\s+$/g,
        WHITESPACE_RE = /\s+/;

    function updateWordCount($container, $input, value) {
      var minimum = +$input.attr('data-suggested-minimum'),
          maximum = +$input.attr('data-suggested-maximum'),
          cc,
          wc;

      value = (value || '').replace(TRIM_RE, '');
      cc = value.length;
      wc = value ? value.split(WHITESPACE_RE).length : 0;

      var $wordCount = $container.find('> .inputWordCount');

      if ($wordCount.length === 0) {
        $wordCount = $('<div/>', {
          'class': 'inputWordCount'
        });

        $container.append($wordCount);
      }

      $wordCount.toggleClass('inputWordCount-warning', cc < minimum || cc > maximum);
      $wordCount.text(
          cc < minimum ? 'Too Short' :
          cc > maximum ? 'Too Long' :
          wc + 'w ' + cc + 'c');
    }

    $doc.delegate(
        '.inputSmall-text :text, .inputSmall-text textarea:not(.richtext)',
        'change.wordCount focus.wordCount input.wordCount',
        $.throttle(100, function() {

      var $input = $(this);

      // Skip textarea created inside CodeMirror editor
      if ($input.closest('.CodeMirror').length) { return; }

      updateWordCount(
          $input.closest('.inputContainer'),
          $input,
          $input.val() || $input.prop('placeholder'));
    }));

    // For original rich text editor, special handling for the word count
    $doc.onCreate('.wysihtml5-sandbox', function() {
      var iframe = this,
          $iframe = $(iframe),
          $container = $iframe.closest('.rte-container'),
          $textarea = $container.find('textarea.richtext'),
          $toolbar = $container.find('.rte-toolbar');

      $(iframe.contentDocument).on('input', $.throttle(100, function() {
        if ($textarea.length > 0) {
          var $bodyClone = $(iframe.contentDocument.body).clone();

          $bodyClone.find('del, .rte').remove();
          updateWordCount(
              $toolbar,
              $textarea,
              $bodyClone.text());
        }
      }));
    });

    // For new rich text editor, special handling for the word count.
    // Note this counts only the text content not the final output which includes extra HTML elements.
    var rteWordCountInterval;

    function rteWordCount(rte) {
      var $input, $container, html, $html, text;

      $input = rte.$el;
      $container = $input.closest('.rte2-wrapper').find('> .rte2-toolbar');

      html = rte.toHTML();
      $html = $(new DOMParser().parseFromString(html, "text/html").body);
      $html.find('del,.rte-comment').remove();
      $html.find('br,p,div,ul,ol,li').after('\n');
      text = $html.text();

      updateWordCount($container, $input, text);
    }

    $doc.on('rteChange', function (event, rte) {
      if (rteWordCountInterval) {
        clearInterval(rteWordCountInterval);
      }

      rteWordCountInterval = setTimeout(function () {
        rteWordCount(rte);
      }, 1000);
    });

  })();

  // Add function to calculate z-index
  $.fn.zIndex = function (zIndex) {
    if (zIndex !== undefined) {
      return this.css("zIndex", zIndex);
    }

    if (this.length) {
      var elem = $(this[ 0 ]), position, value;
      while (elem.length && elem[ 0 ] !== document) {
        // Ignore z-index if position is set to a value where z-index is ignored by the browser
        // This makes behavior of this function consistent across browsers
        // WebKit always returns auto if the element is positioned
        position = elem.css("position");
        if (position === "absolute" || position === "relative" || position === "fixed") {
          // IE returns 0 when zIndex is not specified
          // other browsers return a string
          // we ignore the case of nested elements with an explicit value of 0
          // <div style="z-index: -10;"><div style="z-index: 0;"></div></div>
          value = parseInt(elem.css("zIndex"), 10);
          if (!isNaN(value) && value !== 0) {
            return value;
          }
        }
        elem = elem.parent();
      }
    }
    return 0;
  };

  // Handle file uploads from drag-and-drop.
  (function() {
    var docEntered;

    // Show all drop zones when the user initiates drag-and-drop.
    $doc.bind('dragenter', function() {
      var $body,
          $cover;

      if (docEntered) {
        return;
      }

      docEntered = true;
      $body = $(doc.body);

      // Cover is required to detect mouse leaving the window.
      $cover = $('<div/>', {
        'class': 'uploadableCover',
        'css': {
          'left': 0,
          'height': '100%',
          'position': 'fixed',
          'top': 0,
          'width': '100%',
          'z-index': 1999999
        }
      });

      $cover.bind('dragenter dragover', function(event) {
        event.stopPropagation();
        event.preventDefault();
        return false;
      });

      $cover.bind('dragleave', function() {
        docEntered = false;
        $cover.remove();
        $('.uploadableDrop').remove();
        $('.uploadableFile').remove();
      });

      $cover.bind('drop', function(event) {
        event.preventDefault();
        $cover.trigger('dragleave');
        return false;
      });

      $body.append($cover);

      // Valid file drop zones.
      $('.inputContainer .action-upload, .uploadable .uploadableLink, .fileSelector').each(function() {
        var $upload = $(this),
            $container = $upload.closest('.inputContainer, .uploadable'),
            overlayCss,
            $dropZone,
            $dropLink,
            $fileInputContainer,
            $fileInput,
            $fileSelector,
            $fileSelectorSelect,
            $fileSelectorInput,
            $fileSelectorLabel;

        // Only add drag and drop if the element is visible.
        // For example, an element might be hidden in another tab,
        // so we do not want drag and drop region for that element.
        if (!$(this).is(':visible')) {
          return;
        }

        // Check if this is a FileSelector widget, such as used to upload a single new file
        if ($(this).is('.fileSelector')) {
          $fileSelector = $(this);
          $fileSelectorSelect = $(this).find('> select[name $= "file.action"]');
          $fileSelectorLabel = $(this).find('> .dropDown-input > .dropDown-label');
          $fileSelectorInput = $(this).find('> input[name $= "file.file"]');
          // The existing drag and drop code used a link to display the "Drop Files Here" text,
          // so we'll create a link that can be used
          $upload = $('<a>', {href: '#'}).on('click', function(){ return false; });
        }

        overlayCss = $.extend($container.offset(), {
          'height': $container.outerHeight(),
          'position': 'absolute',
          'width': $container.outerWidth(),
           // Calculate the z-index for the element and use the same value for the drag-and-drop overlay.
           // This should ensure that things like popups will show the drag and drop overlay,
           // and still be on top of other drag-and-drop overlays that are in a lower stacking context.
          'z-index': $(this).zIndex() || 'auto'
        });

        $dropZone = $('<div/>', {
          'class': 'uploadableDrop',
          'css': overlayCss
        });

        $dropLink = $upload.clone();
        $dropLink.text("Drop Files Here");

        $fileInputContainer = $('<div/>', {
          'class': 'uploadableFile',
          'css': $.extend(overlayCss, {
            'z-index': 2000000
          })
        });

        $fileInput = $('<input/>', {
          'type': 'file',
          'multiple': 'multiple'
        });

        // For a file selector, don't allow multiple files to be dropped
        if ($fileSelector && !$fileSelectorInput.attr('multiple')) {
          $fileInput.removeAttr('multiple');
        }

        // On file drop, replace the appropriate input.
        $fileInput.one('change', function() {
          var dropLinkOffset = $dropLink.offset(),
              $frame,
              replaceFileInput;

          $cover.hide();
          $fileInputContainer.hide();

          // Are we dealing with a FileSelector control, or an upload link?
          if ($fileSelector) {

            // Change the FileSelector control to "New Upload"
            $fileSelectorSelect.val('newUpload').trigger('change');
            $fileSelectorLabel.trigger('dropDown-update');

            // Copy attributes from the original file input to the replacement file input
            $.each([ 'class', 'id', 'name', 'style', 'data-input-name', 'data-type-id' ], function(index, name) {

              var val;

              val = $fileSelectorInput.attr(name) || '';

              // For the class name we need to remove bsp-onDomInsert-inserted*
              // so we can re-create the bsp-uploader plugin on the new file input
              if (name === 'class') {
                val = val.replace(/\bbsp-onDomInsert-inserted-\d+\b/g, ' ');
              }

              $fileInput.attr(name, val);
            });

            // Determine if the front-end uploader should be used
            if ($fileSelectorInput.attr('data-bsp-uploader') !== undefined) {
              $fileInput.attr('data-bsp-uploader', '');
            }

            // Replace the original file input with the new one
            $fileSelectorInput.replaceWith($fileInput);
            $fileSelectorInput = $fileInput;

            // Trigger a change event so the upload plugin will run and upload the image
            // We must do this after a timeout to give time for the upload plugin to initialize
            setTimeout(function(){
              $fileSelectorInput.change();
            }, 100);

            // Clear the drag and drop regions
            $cover.trigger('dragleave');

          } else {

            // We are dealing with an upload link, so click it to make the popup appear
            $dropLink.click();

            $frame = $('.frame[name="' + $dropLink.attr('target') + '"]');

            // Position the popup over the drop link.
            $frame.popup('source', $upload, {
              'pageX': dropLinkOffset.left + $dropLink.outerWidth() / 2,
              'pageY': dropLinkOffset.top + $dropLink.outerHeight()
            });

            // Closing the popup resets the drag-and-drop.
            $frame.popup('container').bind('close', function() {
              $cover.trigger('dragleave');
            });

            replaceFileInput = function() {
              var $frameFileInput = $frame.find(':file').eq(0);

              if ($frameFileInput.length !== 1) {
                setTimeout(replaceFileInput, 20);

              } else {
                $.each([ 'class', 'id', 'name', 'style' ], function(index, name) {
                  $fileInput.attr(name, $frameFileInput.attr(name) || '');
                });

                $frameFileInput.after($fileInput);
                $frameFileInput.remove();
                $frameFileInput = $fileInput;
                $frameFileInput.change();
              }
            };

            replaceFileInput();

            //re-initialize uploader plugin, if necessary (not necessary for uploadFile-legacy servlet)
            if ($dropLink.attr('href').indexOf('uploadFiles') === -1) {
              $fileInput.attr('data-bsp-uploader', ' ');
            }
          }
        });

        $dropZone.append($dropLink);
        $body.append($dropZone);
        $fileInputContainer.append($fileInput);
        $body.append($fileInputContainer);
      });
    });

    $doc.bind('dragend', function(event) {
      if (docEntered) {
        docEntered = false;
        $('.uploadableCover').remove();
        $('.uploadableDrop').remove();
        $('.uploadableFile').remove();
      }
    });
  })();

  $doc.on('click', 'button[name="action-delete"], :submit[name="action-delete"]', function() {
    return confirm('Are you sure you want to permanently delete this item?');
  });

  $doc.on('click', 'button[name="action-trash"], :submit[name="action-trash"]', function() {
    return confirm('Are you sure you want to archive this item?');
  });

  $doc.on('click', '[data-confirm-message]', function () {
    return confirm($(this).attr('data-confirm-message'));
  });

  (function() {
    function sync() {
      var $input = $(this),
          $output = $('output[for="' + $input.attr('id') + '"]');

      $output.prop('value', $input.prop('value'));
    }

    $doc.onCreate('input[type="range"]', sync);
    $doc.on('change input', 'input[type="range"]', sync);

    function fix() {
      var $container = $(this).closest('.inputContainer'),
          $inputs = $container.find('.inputVariation input[type="range"]'),
          total,
          max;

      if ($inputs.length === 0) {
        return;
      }

      total = 0.0;
      max = 0.0;

      $inputs.each(function() {
        var $input = $(this),
            inputMax = parseFloat($input.prop('max'));

        total += parseFloat($input.prop('value'));

        if (max < inputMax) {
          max = inputMax;
        }
      });

      $inputs.each(function() {
        var $input = $(this);

        $input.prop('value', parseFloat($input.prop('value')) / total * max);
        sync.call(this);
      });
    }

    $doc.onCreate('.inputContainer', fix);
    $doc.on('change input', '.inputVariation input[type="range"]', fix);
  })();

  // Key bindings.
  $doc.on('keydown', ':input', function(event) {
    if (event.which === 27) {
      $(this).blur();
    }
  });

  $doc.on('keypress', function(event) {
    var $searchInput;

    if (event.which === 47 && $(event.target).closest(':input').length === 0) {
      $searchInput = $('.toolSearch .searchInput :text');

      $searchInput.val('');
      $searchInput.focus();
      return false;
    }
  });

  // Synchronizes main search input with the hidden one in the type select form.
  $doc.on('input', '.searchFiltersRest > .searchInput > :text', function() {
    var $input = $(this),
        $otherInput = $input.closest('.searchFilters').find('.searchFiltersType > input[name="' + $input.attr('name') + '"]');

    if ($otherInput.length > 0) {
      $otherInput.val($input.val());
    }
  });

  $doc.on('open', '.popup[name="miscSearch"]', function() {
    $(document.body).addClass('toolSearchOpen');
  });

  $doc.on('close', '.popup[name="miscSearch"]', function() {
    $(document.body).removeClass('toolSearchOpen');
  });

  $doc.on('open', [
    '.popup[data-popup-source-class~="objectId-select"]',
    '.popup[data-popup-source-class~="rte2-enhancement-toolbar-change"]'

  ].join(','), function(event) {
    var $popup = $(event.target);
    var isEnhancement = $popup.is('.popup[data-popup-source-class~="rte2-enhancement-toolbar-change"]');
    var $input = $popup.popup('source');
    var $withLeftNav = $input.closest('.withLeftNav');

    $.data($popup[0], 'objectSelect-$withLeftNav', $withLeftNav);
    $withLeftNav.addClass('objectSelectOpen');

    var $container = $input;
    var fieldsLabel = '';
    var isAdd;

    while (true) {
      $container = $container.parent().closest('.inputContainer');

      if ($container.length > 0) {
        fieldsLabel = $container.find('> .inputLabel > label').text() + (fieldsLabel ? ' \u2192 ' + fieldsLabel : '');
        isAdd = $container.find('> .plugin-repeatable').length > 0;

      } else {
        break;
      }
    }

    var label = (isEnhancement ? 'Select Enhancement for ' : (isAdd ? 'Add to ' : 'Select ')) + fieldsLabel;
    var objectLabelHtml = $input.closest('.contentForm').find('.ContentLabel').html();

    bsp_utils.onDomInsert($popup[0], '> .content > .frame > h1', {
      insert: function (heading) {
        var $heading = $(heading);

        $heading.text(label);

        if (objectLabelHtml) {
          $heading.append(' - ');
          $heading.append(objectLabelHtml);
        }
      }
    });
  });

  $doc.on('close', [
    '.popup[data-popup-source-class~="objectId-select"]',
    '.popup[data-popup-source-class~="rte2-enhancement-toolbar-change"]'

  ].join(','), function (event) {
    var $withLeftNav = $.data(event.target, 'objectSelect-$withLeftNav');

    if ($withLeftNav) {
      $withLeftNav.removeClass('objectSelectOpen');
    }
  });

  var OBJECT_EDIT_POPUP_SELECTORS = [
    '.popup[data-popup-source-class~="objectId-create"]',
    '.popup[data-popup-source-class~="objectId-edit"]',
    '.popup[data-popup-source-class~="rte2-enhancement-toolbar-edit"]'
  ];

  function findObjectEditParentContent($popup) {
    var $parentPopup = $popup.popup('source').closest('.popup');

    if ($parentPopup.length > 0) {
      if (OBJECT_EDIT_POPUP_SELECTORS.filter(function (s) { return $parentPopup.is(s); }).length > 0) {
        return $parentPopup.find('> .content');
      }
    }

    return $('.toolContent');
  }

  $doc.on('open', OBJECT_EDIT_POPUP_SELECTORS, function(event) {
    var $target = $(event.target);
    var $popup = $target.popup('container');

    // Since the edit popup might contain other popups within it,
    // only run this code when the edit popup is opened
    // (not when the internal popups are opened)
    if (OBJECT_EDIT_POPUP_SELECTORS.filter(function (s) { return $popup.is(s); }).length === 0) {
      return;
    }

    if ($.isNumeric($.data($popup[0], 'popup-objectId-edit-scrollTop'))) {
      return;
    }

    var scrollTop = $win.scrollTop();

    $.data($popup[0], 'popup-objectId-edit-scrollTop', scrollTop);

    var $parentContent = findObjectEditParentContent($popup);

    $parentContent.css({
      height: $win.height() + scrollTop,
      'margin-top': (0 - scrollTop + parseInt($parentContent.css('margin-top'), 10)),
      overflow: 'hidden'
    });

    $win.scrollTop(0);
    $win.resize();
  });

  // Add a close button to the content edit publishing widget within a popup.
  bsp_utils.onDomInsert(document, OBJECT_EDIT_POPUP_SELECTORS.map(function (s) {
    return s + ' > .content > .frame > .content-edit > .contentForm > .contentForm-aside > .widget-publishing > .widget-controls';

  }).join(', '), {
    insert: function (controls) {
      $(controls).append($('<a/>', {
        'class': 'widget-publishing-close',
        'click': function () {
          $(this).popup('close');
          return false;
        }
      }));
    }
  });

  // Add back to link above the content edit form within a popup.
  bsp_utils.onDomInsert(document, OBJECT_EDIT_POPUP_SELECTORS.map(function (s) {
    return s + ' > .content';

  }).join(', '), {
    insert: function (frame) {
      var $frame = $(frame);
      var $source = $frame.popup('source');
      var text = $source.closest('.contentForm').find('> .contentForm-main > .widget > h1, > .withLeftNav > .main > .widget > h1').eq(0).clone().find('option:not(:selected)').remove().end().text();

      if (!text) {
        text = $source.closest('.frame').find('> h1').text();
      }

      text = text ? 'Back to ' + text : 'Back';

      $frame.prepend($('<a/>', {
        'class': 'popup-objectId-edit-heading',
        'text': text,
        'click': function() {
          $(this).popup('close');
          return false;
        }
      }));
    }
  });

  $doc.on('close', OBJECT_EDIT_POPUP_SELECTORS.join(', '), function(event) {
    var $target = $(event.target);
    var $popup = $target.popup('container')

    // Since the edit popup might contain other popups within it,
    // only run this code when the edit popup is opened
    // (not when the internal popups are opened)
    if (OBJECT_EDIT_POPUP_SELECTORS.filter(function (s) { return $popup.is(s); }).length === 0) {
      return;
    }

    if ($.data($popup[0], 'popup-close-cancelled')) {
      return;
    }

    var $parentContent = findObjectEditParentContent($popup);

    $parentContent.css({
      height: '',
      'margin-top': '',
      overflow: ''
    });

    $win.resize();

    var scrollTop = $.data($popup[0], 'popup-objectId-edit-scrollTop');

    $.removeData($popup[0], 'popup-objectId-edit-scrollTop');

    if ($.isNumeric(scrollTop)) {
      $win.scrollTop(scrollTop);
    }
  });

  $doc.ready(function() {
    (function() {
      var $nav = $('.toolNav');

      var $split = $('<div/>', {
        'class': 'toolNav-split',
      });

      var $left = $('<ul/>', {
        'class': 'toolNav-splitLeft'
      });

      var $right = $('<div/>', {
        'class': 'toolNav-splitRight'
      });

      $split.append($left);
      $split.append($right);

      $nav.find('> li').each(function() {
        var $item = $(this);

        if ($item.is(':first-child')) {
          $item.find('> ul > li').each(function() {
            var $subItem = $(this).find('> a');

            $left.append($('<li/>', {
              'html': $('<a/>', {
                'href': $subItem.attr('href'),
                'text': $subItem.text(),
              }),

              'mouseover': function() {
                $left.find('> li').removeClass('state-hover');
                $(this).addClass('state-hover');
                $right.hide();
              }
            }));
          });

        } else {
          var $sub = $item.find('> ul');

          $right.append($sub);
          $sub.hide();

          $left.append($('<li/>', {
            'class': 'toolNav-splitLeft-nested',
            'text': $item.text(),
            'mouseover': function() {
              $left.find('> li').removeClass('state-hover');
              $(this).addClass('state-hover');
              $right.show();
              $right.find('> ul').hide();
              $sub.show();
            }
          }));
        }
      });

      var $toggle = $('<div/>', {
        'class': 'toolNav-toggle',
        'click': function() {
          if ($split.is(':visible')) {
            $split.popup('close');

          } else {
            $split.popup('source', $toggle);
            $split.popup('open');
            $left.find('> li:first-child').trigger('mouseover');
          }
        }
      });

      $nav.before($toggle);

      $split.popup();
      $split.popup('close');
      $split.popup('container').addClass('toolNav-popup');
    })();

    $(this).trigger('create');

    // Sync the search input in the tool header with the one in the popup.
    (function() {
      var previousValue;

      $('.toolSearch :text').bind('focus input', $.throttle(500, function(event) {
        var $headerInput = $(this),
            $headerForm = $headerInput.closest('form'),
            $searchFrame,
            $searchInput,
            headerInputValue = $headerInput.val();

        $headerInput.attr('autocomplete', 'off');
        $searchFrame = $('.frame[name="' + $headerForm.attr('target') + '"]');

        if ($searchFrame.length === 0 ||
            (event.type === 'focus' &&
            headerInputValue &&
            $searchFrame.find('.searchResultList .message-warning').length > 0)) {
          $headerForm.submit();

        } else {
          $searchFrame.popup('open');
          $searchInput = $searchFrame.find('.searchFilters :input[name="q"]');

          if (headerInputValue !== $searchInput.val()) {
            $searchInput.val(headerInputValue).trigger('input');
          }
        }
      }));

      $('.toolSearch button').bind('click', function() {
        $('.toolSearch :text').focus();
        return false;
      });
    }());
  });
});
