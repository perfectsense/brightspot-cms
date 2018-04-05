/* global clearTimeout define DOMParser navigator setTimeout window */

define([
    'jquery',
    'bsp-utils',
    'v3/RecalculateDimensions',
    'v3/spellcheck',
    'undomanager',
    'codemirror/lib/codemirror',
    'codemirror/addon/hint/show-hint',
    'codemirror/addon/dialog/dialog',
    'codemirror/addon/search/searchcursor',
    'codemirror/addon/search/search'
], function($, bsp_utils, RecalculateDimensions, spellcheckAPI, UndoManager, CodeMirror) {

    var CodeMirrorRte;

    /**
     * @class
     * CodeMirrorRte
     *
     * Interface for turning the CodeMirror plain text editor into a "rich text" editor.
     * This object provides the following:
     * - the editor area
     * - configurations for which styles and elements are allowed
     * - methods for importing and exporting HTML
     * - track changes functionality
     * - functions to add enhancements (external content) and move enhancements
     *
     * It does *not* provide a toolbar, etc.
     *
     * This should generally not be used as a stand-alone interface,
     * rather it is used as part of another interface that also provides
     * a toolbar, etc.
     *
     * All CodeMirror-specific functionality should be here - other code
     * should never directly call CodeMirror functions.
     *
     * @example
     * editor = Object.create(CodeMirrorRte);
     * editor.styles = $.extend(true, {}, editor.styles, {bold:{className:'rte2-style-bold', element:'b'}});
     * editor.init('#mytextarea');
     */
    CodeMirrorRte = {

        /**
         * List of all the class names that are used within the rich text editor
         * and some additional information on the type of HTML element it should map to.
         * You can modify this object to add styles to be supported by the editor.
         *
         * String className
         * The class that is used to style the element in the rich text editor.
         *
         * String [element]
         * The element that is created when translating rich text to HTML.
         * If not specified then this style will not have output HTML (only plain text).
         *
         * Object [elementAttr]
         * A list of attributes name/value pairs that are applied to the output HTML element.
         * Also used to match elements to styles on importing HTML.
         * If a value is Boolean true, then that means the attribute must exist (with any value).
         *
         * String [elementContainer]
         * A container elment that surrounds the element in the HTML output.
         * For example, one or more 'li' elements are contained by a 'ul' or 'ol' element.
         *
         * Boolean [line]
         * Set to true if this is a block element that applies to an entire line.
         *
         * Array [clear]
         * A list of styles that should be cleared if the style is selected.
         * Use this to make mutually-exclusive styles.
         *
         * Boolean [void]
         * Set this to true if the element is a "void" element, meaning it cannot
         * contain any text or other elements within it.
         *
         * Boolean [internal]
         * Set this to true if the style is used internally (for track changes).
         * When internal is true, then the style will not be removed by the RemoveStyle functions
         * unless the style name is explicitely provided.
         * For example, if you select a range and tell the RTE to clear all the styles,
         * it will clear the formatting styles like bold and italic, but not the internal
         * styles like trackInsert and trackDelete.
         * However, an internal style can still output HTML elements.
         *
         * Boolean [trackChanges=true]
         * Set this to false if the style should not allow track changes marks within it.
         * For example, a comment style where you do not want to track changes.
         * Defaults to true if not set.
         *
         * Function [fromHTML($el, mark)]
         * A function that extracts additional information from the HTML element
         * and adds it to the mark object for future use.
         *
         * Function [toHTML(mark)]
         * A function that reads additional information saved on the mark object,
         * and uses it to ouput HTML for the style.
         *
         * Function [onClick(mark)]
         * A function that handles clicks on the mark. It can read additional information
         * saved on the mark, and modify that information.
         * Note at this time an onClick can be used only for inline styles.
         *
         * String [enhancementType]
         * If this is an "inline enhancement" you can specify the enhancement type here.
         * Normally this is set only by the Brightspot back-end, and this value is used
         * to display a popup form that is used to modify the attributes on the element.
         * For example: "00000152-eb09-d919-abd2-ffcd34530004"
         *
         * Boolean [popup=true]
         * This value is used only for a style that has an enhancementType.
         * Set this to false if the style should not popup a form to edit the element.
         * Defaults to true.
         *
         * Boolean [toggle=false]
         * This value is used only for a style that has an enhancementType.
         * Set this to true if the style should act as a toggle (the user can click
         * a toolbar button to toggle the style on the selected characters), for example
         * a simple style like bold or italic that the user can turn on or off.
         * By default this is false for enhancementType styles, which means each
         * time the user clicks the toolbar button for the style, a new mark is inserted.
         * Note if this is set to true, then you cannot nest an enhancmentType style
         * inside the same enhancementType style.
         * Note that styles that do not have enhancementType always allow user to toggle.
         */
        styles: {

            // Special style for raw HTML.
            // This will be used when we import HTML that we don't understand.
            // Also can be used to mark text that user wants to treat as html
            html: {
                className: 'rte2-style-html',
                raw: true // do not allow other styles inside this style and do not encode the text within this style, to allow for raw html
            },

            // Special style for reprenting newlines
            newline: {
                className:'rte2-style-newline',
                internal:true
                //,raw: true
            },

            // Special style used to collapse an element.
            // It does not output any HTML, but it can be cleared.
            // You can use the class name to make CSS rules to style the collapsed area.
            // This can be used for example to collapse comments.
            collapsed: {
                className: 'rte2-style-collapsed'
            },

            // Special styles used for tracking changes
            trackInsert: {
                className: 'rte2-style-track-insert',
                element: 'ins',
                internal: true
            },
            trackDelete: {
                className: 'rte2-style-track-delete',
                element: 'del',
                internal: true,
                showFinal: false
            },

            // The following styles are used internally to show the final results of the user's tracked changes.
            // The user can toggle between showing the tracked changes (insertions and deletions) or showing
            // the final result.

            trackHideFinal: {
                // This class is used internally to hide deleted content temporarily.
                // It does not create an element for output.
                className: 'rte2-style-track-hide-final',
                internal: true
            },

            trackDisplay: {
                // This class is placed on the wrapper elemnt for the entire editor,
                // and is used to remove the colors from inserted content temporarily.
                // It does not create an element for output.
                className: 'rte2-style-track-display',
                internal:true
            },

            linebreak: {
                line:true
            },

            linebreakSingle:{
                line:true
            }

        }, // styles

        indentClassPrefix: 'rte2-indent-level-',

        /**
         * Rules for cleaning up the clipboard data when content is pasted
         * from outside the RTE, based on the type of content. These rules
         * are applied before the general clipboardSanitizeRules.
         *
         * This is an object of key/value pairs, where the key is a type identifier,
         * and value is an object which contains an isType() function and a set of rules.
         * {
         *   'typename': {
         *     isType: function(content) { return Boolean },
         *     rules: { } // same format as clipboardSanitizeRules
         *   },
         *   ...
         * }
         *
         * @example
         * {
         *    'googledocs': {
         *       isType: function(content) { return Boolean($(content).find('b[id^=docs-internal-guid]').length); }
         *       rules: { }
         *    },
         *    'msword': {
         *       isType: function(content) { return Boolean($(content).find('[class^=Mso]').length); },
         *       rules: { }
         *    }
         */
        clipboardSanitizeTypes: {},


        /**
         * Rules for cleaning up the clipboard data when content is pasted
         * from outside the RTE. These rules are *always* applied to the content.
         * See clipboardSanitizeTypes to set up rules based on the type of content
         * that is pasted in.
         *
         * This is an object of key/value pairs, where the key is a jQuery selector,
         * and value is one of the following:
         * - A string that is the style name from the styles object.
         * - An empty string to indicate the matched elements should be removed.
         * - A function that directly modifies the matched element content.
         *
         * @example
         * {'span[style*="font-style:italic"]': 'italic',
         *  'span[style*="font-weight:700"]': 'bold',
         *  'table', function($el) {
         *    // Modify $el which is the table element
         *  }
         * }
         */
        clipboardSanitizeRules: {},


        /**
         * Function for cleaning up the clipboard data when content is pasted
         * from outside the RTE.
         *
         * @param {jQuery} $content
         * The content that was pasted.
         *
         * @returns {jQuery}
         * The modified content.
         *
         * @example
         * function($content) {
         *     // Remove anything with class "badclass"
         *     $content.find('.badclass').remove();
         *     return $content;
         * }
         */
        clipboardSanitizeFunction: function($content) {
            return $content;
        },


        /**
         * Should we track changes?
         * Note: do not set this directly, use trackSet() or trackToggle()
         */
        trackChanges: false,


        /**
         * For track changes should we show the final result?
         * If true, show the orignal results marked up with changes.
         * If false, show the final results without the tracked changes.
         * Note: do not set this directly, use trackDisplaySet() because other things happen when the value is changed.
         */
        trackDisplay: true,


        /**
         * List of elements that should cause a new line when importing from HTML.
         * We don't necessarily list them all, just the ones we are likely to encounter.
         */
        newLineRegExp: /^(br)$/,


        /**
         * List of elements that do not need to be </closed>
         */
        voidElements: {
            'area': true,
            'base': true,
            'br': true,
            'col': true,
            'command': true,
            'hr': true,
            'img': true,
            'input': true,
            'keygen': true,
            'link': true,
            'meta': true,
            'param': true,
            'source': true
        },


        /**
         * When a region is marked as raw HTML, should we add a data attribute to the elements?
         */
        rawAddDataAttribute: false,


        /**
         * When a line ends in a character marked as raw HTML, should we add a BR element?
         * If true, add a BR element at the end of every line.
         * If false, add a newline if the last character in the line is marked as raw HTML.
         */
        rawBr: true,


        /**
         *
         */
        init: function(element, options) {

            var codeMirrorOptions;
            var self;

            self = this;

            if (options) {
                $.extend(true, self, options);
            }

            self.$el = $(element).first();

            codeMirrorOptions = {
                lineWrapping: true,
                dragDrop: false,
                mode:null,
                extraKeys: self.getKeys()
            };

            // Create the codemirror object
            if (self.$el.is('textarea')) {
                self.codeMirror = CodeMirror.fromTextArea(self.$el[0], codeMirrorOptions);
            } else {
                self.codeMirror = CodeMirror(self.$el[0], codeMirrorOptions);
            }

            // Create a mapping from self.styles so we can perform quick lookups on the classname
            self.classes = self.getClassNameMap();

            self.trackInit();
            self.historyInit();
            self.enhancementInit();
            self.initListListeners();
            self.dropdownInit();
            self.initEvents();
            self.clipboardInit();
            self.spellcheckInit();
            self.modeInit();
            self.initPreviewResizer();

            var $wrapper = $(self.codeMirror.getWrapperElement());
            var wrapperWidth = $wrapper.width();

            RecalculateDimensions.bind(element, function () {
                self.refresh();
            });

            $(window).resize(bsp_utils.throttle(500, function (event) {
                var newWrapperWidth = $wrapper.width();

                if (wrapperWidth !== newWrapperWidth) {
                    wrapperWidth = newWrapperWidth;

                    self.refresh();
                }
            }));
        },


        /**
         * Set up listener for lists:
         * If you are on a list line at the first character of the line and you press enter,
         * this will move the current line down. If the previous line was also a list,
         * then the new line created above should also be a list item.
         * If you are on a list line but not at the first character and you press enter,
         * this will add a new line below, and the new line should always be a list item.
         */
        initListListeners: function() {

            var editor;
            var indent;
            var isEmptyLine;
            var isFirstListItem;
            var isLastListItem;
            var isStartOfLine;
            var listType;
            var rangeFirstLine;
            var self;

            self = this;

            editor = self.codeMirror;

            // Monitor the "beforeChange" event so we can save certain information
            // about lists, to later use in the "change" event
            editor.on('beforeChange', function(instance, changeObj) {

                var listTypePrevious;
                var listTypeNext;
                var rangeBeforeChange;

                // Get the listType and set the closure variable for later use
                listType = self.blockGetListType(changeObj.from.line);
                indent = self.blockGetIndent(changeObj.from.line)

                rangeFirstLine = {from:changeObj.from, to:changeObj.from};
                rangeBeforeChange = {from:changeObj.from, to:changeObj.to};

                // Get the list type of the previous line
                listTypePrevious = '';
                if (rangeBeforeChange.from.line > 0) {
                    listTypePrevious = self.blockGetListType(rangeBeforeChange.from.line - 1);
                }

                // Get the list type of the next line
                listTypeNext = '';
                if (rangeBeforeChange.to.line < editor.lineCount() - 1) {
                    listTypeNext = self.blockGetListType(rangeBeforeChange.to.line + 1);
                }

                isFirstListItem = Boolean(listTypePrevious === '');
                isLastListItem = Boolean(listTypeNext === '');

                // Remember if this change is taking palce at the start of the line,
                // so we can tell if the user presses Enter at the first list item
                // and we should move the whole list down instead of adding a new list item.
                isStartOfLine = Boolean(rangeBeforeChange.from.ch === 0);

                // Check if the list item started out as an empty line,
                // so if user presses Enter on the last list item
                // the list style should be removed.
                isEmptyLine = Boolean(editor.getLine(rangeBeforeChange.from.line) === '');

                // Loop through all the changes that have not yet been applied, and determine if more text will be added to the line
                $.each(changeObj.text, function(i, textChange) {

                    // Check if this change has more text to add to the page
                    if (textChange.length > 0) {
                        isEmptyLine = false;
                        return false; // stop looping because we found some text and now we know the line will not be empty
                    }
                });

            });

            // Monitor the "change" event so we can adjust styles for list items
            // This will use the closure variables set in the "beforeChange" event.
            editor.on('change', function(instance, changeObj) {

                var range;

                // Check for a listType that was saved by the beforeChange event
                if (listType) {

                    // Get the current range (after the change has been applied)
                    range = self.getRange();

                    // For the new line, if user pressed enter on a blank list item and it was the last item in the list,
                    // Then do not add a new line - instead change the list item to a non-list item
                    if (isLastListItem && isEmptyLine) {

                        // Remove the indent on new line
                        self.blockRemoveIndent(range.to.line);

                        // Remove the list class on the new line
                        self.blockRemoveStyle(listType, range);

                    } else if (isFirstListItem && isStartOfLine) {

                        // If at the first character of the first list item and user presses enter,
                        // do not create a new list item above it, just move the entire list down

                    } else {

                        // Always keep the original starting line the list style.
                        // This is used in the case when you press enter and move the current
                        // line lower - so we need to add list style to the original starting point.
                        // TODO: not sure what happens when you insert multiple line
                        self.blockSetStyle(listType, rangeFirstLine);
                        self.blockSetIndent(rangeFirstLine.from.line, indent);

                        // Set list style for the new range
                        self.blockSetStyle(listType, range);

                        // Indent the new list item the proper amount
                        self.blockSetIndent(range.from.line, indent);
                    }
                }

            });
        }, // initListListeners


        /**
         * Set up some special events.
         *
         * rteCursorActivity = the cursor has changed in the editor.
         * You can use this to update a toolbar for example.
         *
         * rteChange = a change has been made to the editor content.
         * You can use this to update the character count for example.
         */
        initEvents: function() {

            var editor;
            var self;

            self = this;

            editor = self.codeMirror;

            editor.on('cursorActivity', function(instance, event) {
                self.$el.trigger('rteCursorActivity', [self]);
            });

            editor.on('beforeSelectionChange', function(instance, event) {

                // If user clicked "Clear" to add text after a mark,
                // or toggled off part of a style mark, then after the cursor moves,
                // make the styles inclusive again so a user returning to the mark
                // can enter more text inside the style.
                self.inlineMakeInclusive();

            });

            editor.on('changes', function(instance, event) {
                self.triggerChange(event);
            });

            editor.on('focus', function(instance, event) {
                self.$el.trigger('rteFocus', [self]);
            });

            editor.on('blur', function(instance, event) {
                self.$el.trigger('rteBlur', [self]);
            });
        },


        /**
         * Trigger an rteChange event.
         *
         * This can happen when user types changes into the editor, or if some kind of mark is modified.
         * When the event is triggered, it is sent additional data: the object for this rte, plus an optional
         * extra data parameter.
         *
         * @param {Object} [extra]
         * Extra data parameter to pass with the rteChange event.
         * For example, this could be the CodeMirror change event.
         */
        triggerChange: function(extra) {
            var self;
            self = this;
            self.$el.trigger('rteChange', [self, extra]);
        },


        //==================================================
        // STYLE FUNCTIONS
        // The following functions deal with inline or block styles.
        //==================================================

        /**
         * Toggle an inline or block style for a range.
         *
         * @param {String} styleKey
         * Name of the style to set (from the styles definition object)
         *
         * @param {Object} [range=current range]
         *
         * @see inlineSetStyle(), blockSetStyle()
         */
        toggleStyle: function(style, range) {

            var mark;
            var self;
            var styleObj;

            self = this;

            styleObj = self.styles[style];
            if (styleObj) {
                if (styleObj.line) {
                    mark = self.blockToggleStyle(style, range);
                } else {
                    mark = self.inlineToggleStyle(style, range);
                }
            }
            return mark;
        },


        /**
         * Set an inline or block style for a range.
         *
         * @param {String} styleKey
         * Name of the style to set (from the styles definition object)
         *
         * @param {Object} [range=current range]
         *
         * @returns {Object}
         * The mark for an inline style, or the line data for a block style.
         *
         * @see inlineSetStyle(), blockSetStyle()
         */
        setStyle: function(style, range) {

            var mark;
            var self;
            var styleObj;

            self = this;

            styleObj = self.styles[style];
            if (styleObj) {
                if (styleObj.line) {
                    mark = self.blockSetStyle(style, range);
                } else {
                    mark = self.inlineSetStyle(style, range);
                }
            }

            return mark;
        },


        /**
         * Remove both inline and block styles from a range.
         * Only removes block styles if the range spans multiple lines.
         *
         * @param {Object} [range=current range]
         *
         * @see inlineRemoveStyle(), blockRemoveStyle()
         */
        removeStyles: function(range) {

            var self;

            self = this;

            range = range || self.getRange();

            // Remove all inline styles
            self.inlineRemoveStyle('', range);

            // Remove line style if the current range is on multiple lines
            if ((range.from.ch === 0 && range.to.ch === 0) || range.from.line !== range.to.line) {
                self.blockRemoveStyle('', range);
            }
        },


        /**
         * Returns the "context" elements for the current cursor position or range.
         *
         * @param {Object} [range]
         * The range of the selection {from:{line,ch},to:{line:ch}}.
         * If not specified, uses the current selection.
         *
         * @returns {Array}
         * An array of the context elemsnets that are active within the range.
         * The value null represents the root context.
         * For example: [null, 'b']
         */
        getContext: function(range) {

            // Step through each character in the range
            // Get a list of all marks on the character
            // Pick the mark that has the right-most starting character, add that element to the context list
            // Return the context list

            // Example: what if you have xxx<B>RRR</B>RRRxxx
            // Then the context should be considered to be [B,null]
            //
            // Example: what if you have xxxRRR<B>RRR</B>RRRxxx
            // Then the context should be considered to be [null,B]
            //
            // Example: what if you have: xxxRRR<B>RRR</B>RR<I>RRR</I>RRRxxx
            // Then the context should be considered to be [null,B,I]
            //
            // Example: what if you have xxxRRR<B>RRR<I>RRR</I>RRR</B>xxx
            // Then the context should be considered [null,B,I]
            //
            // Example: what if you have xxx<B>xxx<I>RRR</I>xxx</B>xxx
            // Then the context should be considered to be [I]

            var blockStyles;
            var context;
            var contextNull;
            var editor;
            var foundBlockStyle;
            var lineNumber;
            var self;

            self = this;
            editor = self.codeMirror;
            range = range || self.getRange();
            lineNumber = range.from.line;

            // Placeholder for the context elements
            context = {};

            // Loop through all lines in the range
            editor.eachLine(range.from.line, range.to.line + 1, function(line) {

                var charFrom;
                var charNumber;
                var charTo;
                var marks;
                var rightmostMark;
                var rightmostPos;
                var styleObj;

                charFrom = (lineNumber === range.from.line) ? range.from.ch : 0;
                charTo = (lineNumber === range.to.line) ? range.to.ch : line.text.length;

                // Loop through each character in the line
                for (charNumber = charFrom; charNumber <= charTo; charNumber++) {

                    rightmostMark = undefined;
                    rightmostPos = undefined;

                    // Get all of the marks for this character and get a list of the class names
                    marks = editor.findMarksAt({ line: lineNumber, ch: charNumber });

                    // Find the mark with the rightmost starting character
                    marks.forEach(function(mark){

                        var isRightmost;
                        var pos;
                        var styleObj;

                        if (!mark.className) {
                            return;
                        }

                        // Make sure this class maps to an element
                        styleObj = self.classes[mark.className];
                        if (!styleObj) {
                            return;
                        }

                        // Make sure this style is not for internal use (like track changes)
                        if (styleObj.internal) {
                            return;
                        }

                        if (!mark.find) {
                            return;
                        }
                        pos = mark.find();
                        if (!pos) {
                            return;
                        }

                        // We need to check a couple special cases because CodeMirror still sends us the marks
                        // that are next to the position of the cursor.
                        //
                        // Marks can have an "inclusiveLeft" and "inclusiveRight" property, which means to extend the mark
                        // to the left or the right when text is added on that side.
                        //
                        // If the mark is defined to the right of the cursor, then we only include the classname if inclusiveLeft is set.
                        // If the mark is defined to the left of the cursor, then we only include the classname if inclusiveRight is set.

                        if (pos.from.line === lineNumber && pos.from.ch === charNumber && !mark.inclusiveLeft) {

                            // Don't consider this mark if we are on the left side of the range when inclusiveLeft is not set
                            return;

                        } else if (pos.to.line === lineNumber && pos.to.ch === charNumber && !mark.inclusiveRight) {

                            // Don't consider this mark if we are on the right side of the range when inclusiveRight is not set
                            return;

                        }

                        if (rightmostMark) {

                            if (pos.from.line > rightmostPos.from.line ||
                                (pos.from.line === rightmostPos.from.line && pos.from.ch >= rightmostPos.from.ch)) {

                                isRightmost = true;
                            }

                        } else {
                            isRightmost = true;
                        }

                        if (isRightmost) {
                            rightmostMark = mark;
                            rightmostPos = pos;
                        }

                    });

                    if (rightmostMark) {

                        styleObj = self.classes[rightmostMark.className];
                        if (styleObj && styleObj.element) {
                            context[styleObj.element] = styleObj;
                        }

                    } else {

                        // Use line styles if there are no inline marks

                        blockStyles = self.blockGetStyles({from:{line:lineNumber, ch:0}, to:{line:lineNumber, ch:0}});
                        foundBlockStyle = false;
                        $.each(blockStyles, function(styleKey) {
                            var styleObj = self.styles[styleKey];
                            var element = styleObj.element;

                            if (element) {
                                context[element] = styleObj;
                                foundBlockStyle = true;
                            }
                        });

                        // If there is no line style then use a null context
                        if (!foundBlockStyle) {
                            contextNull = true;
                        }
                    }
                }

                lineNumber++;
            });

            // Convert context object into an array
            if (contextNull) {
                context[''] = { };
            }

            return context;
        },


        /**
         * Determines if a particular style is allowed to be applied based on the context rules.
         *
         * @param  {String} styleKey
         * The style key that you want to check.
         * @param  {Object} range
         * The range to check. If undefined will call getRange().
         * @param  {Array} context
         * The context as returned by getContext(). If undefined, will call getContext().
         *
         * @return {Boolean}
         */
        checkContext: function(styleKey, range, context) {

            var rangeCollapsed;
            var self;
            var styles;
            var styleObj;
            var validContextInner;
            var validContextOuter;

            self = this;
            context = context || self.getContext(range);
            range = range || self.getRange();
            rangeCollapsed = range.from.line === range.to.line && range.from.ch === range.to.ch;

            // Get all the styles defined on the current range
            // Note ALL characters in the range must have the style or it won't be returned
            styles = $.extend({}, self.inlineGetStyles(range), self.blockGetStyles(range));

            styleObj = self.styles[styleKey] || {};

            if (styleObj.context) {

                // Loop through all the current contexts.
                // Note there can be multiple contexts because multiple characters can be
                // selected in the range, and each character might be in a different context.
                // For example, if the character R represents the selected range:
                // aaa<B>RRR</B>RRR<I>RRR</I>aaa
                // Then the context would be B, I, and null.
                //
                // We must check each context that is selected, to determine if
                // the style is allowed in that context.
                //
                // If the style fails for any one of the contexts, then it
                // should be invalid, and we should prevent the user from applying the style
                // across the range.

                // Loop through all the contexts for the selected range
                validContextInner = true;
                $.each(context, function(contextElement) {
                    if (contextElement === '') {
                        contextElement = null;
                    }

                    // If a different root context was specified, then use that as the root element
                    // For example, if the rte is meant to edit the content inside a '<mycontent>' element,
                    // then contextRoot would be 'mycontent', and only those elements allowed in that element
                    // would be allowed.
                    if (self.contextRoot && contextElement === null) {
                        contextElement = self.contextRoot;
                    }

                    // Is this contextElement listed among the context allowed by the current style?
                    if ($.inArray(contextElement, styleObj.context) === -1) {

                        // Special case - if this style can be toggled, then it should be considered valid
                        // if the entire range contains this style
                        if (contextElement === styleObj.element && styleObj.toggle && styles[styleKey]) {
                            return; // continue looping
                        }

                        validContextInner = false;
                        return false; // stop looping
                    }
                });

                validContextOuter = false;

                if (!rangeCollapsed) {
                    validContextOuter = true;

                    $.each(context, function (contextElement, contextOptions) {
                        if (contextOptions.context && $.inArray(styleObj.element, contextOptions.context) === -1) {
                            validContextOuter = false;
                            return false; // stop looping
                        }
                    });
                }

                // Set a class on the toolbar button to indicate we are out of context.
                // That class will be used to style the button, but also
                // to prevent clicking on the button.
                return Boolean(validContextInner || validContextOuter);

            } else {

                // No context specified for this style
                return true;
            }
        },


        //==================================================
        // INLINE STYLES
        // The following format functions deal with inline styles.
        //==================================================

        /**
         * Toggle a class within a range:
         * If all characters within the range are already set to the className, remove the class.
         * If one or more characters within the range are not set to the className, add the class.
         *
         * @param {String} styleKey
         * The format class to toggle (from the styles definition object)
         *
         * @param {Object} [range=current range]
         * The range of positions {from:{line,ch},to:{line,ch}}
         */
        inlineToggleStyle: function(styleKey, range) {

            var mark;
            var self;

            self = this;

            range = range || self.getRange();

            if (self.inlineIsStyle(styleKey, range)) {
                self.inlineRemoveStyle(styleKey, range);
            } else {
                mark = self.inlineSetStyle(styleKey, range);
            }

            return mark;
        },


        /**
         * @param {String|Object} style
         * The format to add (from the styles definition object).
         * This can be a String key into the styles object,
         * or it can be the style object itself.
         *
         * @param Object [range=current selection]
         * The range of positions {from,to}
         *
         * @param Object [options]
         * Set of key/value pairs to specify options.
         * These options will be passed as mark options when the mark is created.
         *
         * @param Object [options.triggerChange=true]
         * Set this to false if you do not want to trigger the rteChange event after setting the style.
         * For example, if you will be making multiple style changes and you will trigger the rteChange event yourself.
         */
        inlineSetStyle: function(style, range, options) {

            var className;
            var editor;
            var isEmpty;
            var line;
            var mark;
            var markOptions;
            var self;
            var styleObj;

            self = this;

            editor = self.codeMirror;

            range = range || self.getRange();

            options = options || {};

            if (typeof style === 'string') {
                styleObj = self.styles[style] || {};
            } else {
                styleObj = style;
            }
            className = styleObj.className;

            if (styleObj.singleLine && range.from.line !== range.to.line) {
                line = editor.getLine(range.from.line);
                range.to.line = range.from.line;
                range.to.ch = line.length;
            }

            markOptions = $.extend({
                className: className,
                startStyle: className + '-start',
                endStyle: className + '-end',
                inclusiveRight: true,
                addToHistory: true
            }, options);

            // Check for special case if no range is defined, we should still let the user
            // select a style to make the style active. Then typing more characters should
            // appear in that style.
            isEmpty = (range.from.line === range.to.line) && (range.from.ch === range.to.ch);
            if (isEmpty) {
                markOptions.addToHistory = false;
                markOptions.clearWhenEmpty = false;
                markOptions.inclusiveLeft = true;
            }

            if (styleObj.void) {

                markOptions.addToHistory = true;
                markOptions.readOnly = true;
                markOptions.inclusiveLeft = false;
                markOptions.inclusiveRight = false;

                // Add a space to represent the empty element because CodeMirror needs
                // a character to display for the user to display the mark.
                editor.replaceRange(' ', {line:range.from.line, ch:range.from.ch}, null);

                range.to.line = range.from.line;
                range.to.ch = range.from.ch + 1;
            }

            if (styleObj.readOnly) {
                // Set mark to atomic which means cursor cannot be moved into the mark
                // and also implies readonly
                markOptions.atomic = true;
                markOptions.addToHistory = true;
                markOptions.inclusiveLeft = false;
                markOptions.inclusiveRight = false;

                if (editor.getValue() === '') {
                    self.insert(' ');
                }
            }

            mark = self.historyCreateMark(range.from, range.to, markOptions);
            self.inlineSplitMarkAcrossLines(mark);

            // If this is a set of mutually exclusive styles, clear the other styles
            if (styleObj.clear) {
                $.each(styleObj.clear, function(i, styleKey) {
                    self.inlineRemoveStyle(styleKey, range);
                });
            }

            // If there was a fromHTML filter defined, run it not so additional info can be added to the mark
            if (styleObj.filterFromHTML) {
                styleObj.filterFromHTML(mark);
            }

            // Trigger a cursorActivity event so for example toolbar can pick up changes
            CodeMirror.signal(editor, "cursorActivity");

            if (options.triggerChange !== false) {
                self.triggerChange();
            }

            return mark;

        }, // initSetStyle


        /**
         * Remove the formatting within a region. You can specify a single class name to remove
         * or remove all the formatting.
         *
         * @param String [styleKey]
         * The format class to remove. Set to empty string to remove all formatting.
         * Also refer to options.includeTrack
         *
         * @param Object [range=current selection]
         * The range of positions {from,to}
         *
         * @param Object [options]
         *
         * @param Object [options.deleteText=false]
         * Set to true if you want to also delete the text within each class that is removed.
         *
         * @param String [except]
         * A format class that should not be removed.
         * Use this is you set styleKey to blank (to remove all classes) but you want to
         * keep one specific class. For example, for the "html" class if you want to keep the html class,
         * but remove any other style classes within.
         *
         * @param Boolean [options.includeTrack=false]
         * Set to true if you want to include the "track changes" classes.
         * Otherwise will ignore those classes.
         *
         * @param Boolean [options.triggerChange=true]
         * Set to false if you want to prevent a change event from being triggered.
         */
        inlineRemoveStyle: function(styleKey, range, options) {

            var className;
            var deleteText;
            var editor;
            var from;
            var lineNumber;
            var self;
            var to;
            var triggerChange;

            self = this;

            editor = self.codeMirror;

            if ($.type(styleKey) === 'object') {
                className = styleKey.className;
            } else if (styleKey && $.type(styleKey) === 'string') {
                className = self.styles[styleKey].className;
            }

            options = options || {};
            deleteText = options.deleteText;

            range = range || self.getRange();

            from = range.from;
            to = range.to;

            lineNumber = from.line;

            editor.eachLine(from.line, to.line + 1, function(line) {

                var fromCh;
                var marks;
                var markNew;
                var toCh;

                // Get the character ranges to search within this line.
                // If we're not on the first line, start at the beginning of the line.
                // If we're not on the last line, stop at the end of the line.
                fromCh = (lineNumber === from.line) ? from.ch : 0;
                toCh = (lineNumber === to.line) ? to.ch : line.text.length;

                // Loop through all the marks defined on this line
                marks = line.markedSpans || [];
                marks.slice(0).reverse().forEach(function(mark) {

                    var from;
                    var markerOpts;
                    var markerOptsNotInclusive;
                    var matchesClass;
                    var outsideOfSelection;
                    var selectionEndsAfter;
                    var selectionStartsBefore;
                    var styleObj;
                    var to;

                    // Check if we should remove the class
                    matchesClass = false;
                    styleObj = self.classes[mark.marker.className] || {};
                    if (className) {
                        matchesClass = Boolean(mark.marker.className === className);
                    } else {

                        // Do not remove the track changes classes unless specifically named
                        matchesClass = Boolean(options.includeTrack || styleObj.internal !== true);

                        // Do not remove the "except" class if it was specified
                        if (mark.marker.className === options.except) {
                            matchesClass = false;
                        }
                    }

                    if (!matchesClass) {
                        return;
                    }

                    markerOpts = self.historyGetOptions(mark.marker);
                    markerOpts.addToHistory = false;

                    markerOptsNotInclusive = $.extend(true, {}, markerOpts);
                    markerOptsNotInclusive.inclusiveLeft = markerOptsNotInclusive.inclusiveRight = false;

                    if (markerOpts.type === 'bookmark') {
                        return;
                    }

                    // Figure out the range for this mark
                    to = mark.to;
                    from = mark.from;
                    if (mark.to === null) {
                        to = line.text.length;
                    }
                    if (mark.from === null) {
                        from = 0;
                    }

                    // Determine if this mark is outside the selected range
                    outsideOfSelection = fromCh >= to || toCh <= from;
                    if (fromCh === toCh) {
                        outsideOfSelection = fromCh > to || toCh < from;
                    }
                    if (outsideOfSelection) {
                        return;
                    }

                    selectionStartsBefore = fromCh <= from;
                    selectionEndsAfter = toCh >= to;

                    if (selectionStartsBefore && selectionEndsAfter) {

                        // The range completely surrounds the mark.

                        // This is some text
                        //      mmmmmmm      <-- mark
                        //    rrrrrrrrrrr    <-- range
                        //                   <-- no new mark
                        //      xxxxxxx      <-- text to delete (if deleteText is true)


                        if (deleteText) {
                            mark.marker.shouldDeleteText = true;
                        }

                        // Clear the mark later

                    } else if (selectionStartsBefore && !selectionEndsAfter) {

                        // The range starts before this mark, but it ends within the mark.
                        // Create a new mark to represent the part of the mark that is not being removed.
                        // The original mark will be deleted later.
                        //
                        // This is some text
                        //      mmmmmmm      <-- mark
                        // rrrrrrrr          <-- range
                        //         nnnn      <-- new mark
                        //      xxx          <-- text to delete (if deleteText is true)

                        // Create a new marker for the text that should remain styled
                        markNew = self.historyCreateMark(
                            { line: lineNumber, ch: toCh },
                            { line: lineNumber, ch: to },
                            markerOpts
                        );

                        if (deleteText) {
                            // Create a temporary marker for the text that will be deleted
                            // It should be the part of the marked text that is outside the range
                            editor.markText(
                                { line: lineNumber, ch: from },
                                { line: lineNumber, ch: toCh },
                                markerOpts
                            ).shouldDeleteText = true;
                        }

                        // Clear the original mark later

                    } else if (!selectionStartsBefore && selectionEndsAfter) {

                        // The range starts within this mark, but it ends after the mark.
                        // Create a new mark to represent the part of the mark that is not being removed.
                        // The original mark will be deleted later.
                        //
                        // This is some text
                        //      mmmmmmm      <-- marked
                        //        rrrrrrrrrr <-- range
                        //      nn           <-- new mark
                        //        xxxxx      <-- text to delete (if deleteText is true)

                        markNew = self.historyCreateMark(
                            { line: lineNumber, ch: from },
                            { line: lineNumber, ch: fromCh },
                            markerOptsNotInclusive
                        );

                        self.inlineMakeInclusivePush(markNew);

                        if (deleteText) {
                            // Create a tempoary marker for the text that will be deleted
                            // It should be the part of the marked text that is outside the range
                            markNew = editor.markText(
                                { line: lineNumber, ch: fromCh },
                                { line: lineNumber, ch: to },
                                markerOpts
                            ).shouldDeleteText = true;
                        }

                        // Clear the original mark later

                    } else {

                        // The range is entirely inside the marker.
                        // Create two new marks - one before the range, and one after the range.
                        //
                        // This is some text
                        //      mmmmmmm      <-- marked
                        //        rrr        <-- range
                        //      nn   nn      <-- new marks
                        //        xxx        <-- text to delete (if deleteText is true)

                        // Do not allow inline enhancement styles to be split in the middle
                        if (styleObj.enhancementType && !styleObj.toggle) {
                            return;
                        }

                        markNew = self.historyCreateMark(
                            { line: lineNumber, ch: toCh },
                            { line: lineNumber, ch: to },
                            markerOpts
                        );

                        markNew = self.historyCreateMark(
                            { line: lineNumber, ch: from },
                            { line: lineNumber, ch: fromCh },
                            markerOptsNotInclusive
                        );

                        self.inlineMakeInclusivePush(markNew);

                        if (deleteText) {
                            // Create a temporary marker for the text that will be deleted
                            // It should be the part of the marked text that is outside the range
                            markNew = editor.markText(
                                { line: lineNumber, ch: fromCh },
                                { line: lineNumber, ch: toCh },
                                markerOpts
                            ).shouldDeleteText = true;
                        }

                        // Clear the original mark later
                    }

                    // Set a flag on the marker object so we can find it later for removal
                    mark.marker.shouldRemove = true;
                });

                // Go to the next line
                lineNumber++;

            });

            // Loop through all the marks and remove the ones that were marked
            editor.getAllMarks().forEach(function(mark) {

                var position;

                if (deleteText && mark.shouldDeleteText) {

                    position = mark.find();

                    if (position && !(position.from.line === position.to.line && position.from.ch === position.to.ch)) {

                        editor.replaceRange('', position.from, position.to);

                        // Trigger a change event for the editor later
                        triggerChange = true;
                    }
                }
                if (mark.shouldRemove) {

                    self.historyRemoveMark(mark);

                    // Trigger a change event for the editor later
                    triggerChange = true;
                }
            });

            // We hold off on triggering a change event until the end because
            // it seems to cause problems if we trigger a change in the middle
            // of examining all the marks
            if (triggerChange && options.triggerChange !== false) {
                self.triggerChange();
            }

            // Trigger a cursor activity event so the toolbar can update
            CodeMirror.signal(editor, "cursorActivity");

        },


        /**
         * For elements that were previously made non-inclusive,
         * make them inclusive now (after the user changed the selection);
         *
         * For example, if the user is on the right of a bold style, then
         * clicking "clear" will make the bold style non-inclusive, so the user
         * can type type outside the bold style. However, if the user moves the
         * cursor, then returns to the right of the bold style, the style should
         * be inclusive again, so the user can add to the bold text.
         *
         * This function shoudl be called every time the selection changes.
         */
        inlineMakeInclusive: function() {

            var marks;
            var self;

            self = this;

            // Get an array of the marks that were previously saved
            // when they were made non-inclusive.
            marks = self.marksToMakeInclusive || [];

            if (marks.length) {

                $.each(self.marksToMakeInclusive || [], function() {
                    var mark = this;
                    mark.inclusiveRight = true;
                });

                self.marksToMakeInclusive = [];
            }
        },


        /**
         * Add a mark to the list of marks that must be made inclusive
         * after the user moves the selection.
         * @param {Object] mark
         */
        inlineMakeInclusivePush: function(mark) {

            var self;

            self = this;

            // Ignore certain styles that are internal only like spelling errors
            if (!self.classes[mark.className]) {
                return;
            }

            if (mark.atomic) {
                return;
            }

            self.marksToMakeInclusive = self.marksToMakeInclusive || [];

            self.marksToMakeInclusive.push(mark);
        },


        /**
         * Given a styles and a cursor position, removes the style that surrounds the cursor position.
         *
         * For example, if your cursor "|" is within an italic styled area:
         * this is <i>it|alic<i> text
         *
         * Then this function will remove the italic styling and the text within, leaving you with:
         * this is  text
         */
        inlineRemoveStyledText: function(styleKey, range) {

            var mark;
            var pos;
            var self;

            self = this;

            mark = self.inlineGetMark(styleKey, range);
            if (mark) {

                pos = mark.find();

                // Delete the text within the mark
                self.codeMirror.replaceRange('', pos.from, pos.to);

                // Delete the mark
                mark.clear();

                self.triggerChange();
            }
        },


        /**
         * Determines if ALL characters in the range have a style.
         *
         * @param String className
         * The format class to add.
         *
         * @param Object [range=current selection]
         * The range of positions {from,to}
         *
         * @returns Boolean
         * True if all charcters in the range are styled with className.
         */
        inlineIsStyle: function(styleKey, range) {

            var self;
            var styles;

            self = this;

            range = range || self.getRange();

            styles = self.inlineGetStyles(range);

            return Boolean(styles[styleKey]);
        },


        /**
         * Get the mark for a particular style within a range.
         *
         * @returns {Object} mark
         * The mark object for the first style found within the range,
         * or undefined if that style is not in the range.
         */
        inlineGetMark: function(styleKey, range) {

            var className;
            var editor;
            var matchingMark;
            var self;

            self = this;

            editor = self.codeMirror;

            // Check if className is a key into our styles object
            className = self.styles[styleKey].className;

            range = range || self.getRange();

            $.each(editor.findMarks(range.from, range.to), function(i, mark) {
                if (mark.className === className) {
                    matchingMark = mark;
                    return false; // stop the loop
                }
            });

            return matchingMark;
        },


        /**
         * Determines if ANY character in the range has the className.
         *
         * @param String className
         * The format class to add.
         *
         * @param Object [range=current selection]
         * The range of positions {from,to}
         *
         * @returns Boolean
         * True if any charcter in the range is styled with className.
         */
        inlineHasStyle: function(styleKey, range) {

            var self;
            var styles;
            var value;

            self = this;

            range = range || self.getRange();

            styles = self.inlineGetStyles(range);
            value = styles[styleKey];
            return Boolean(value === true || value === false);
        },


        /**
         * Returns a list of all styles that are set for the characters in a range.
         * If a style is defined for ALL characters in the range, it will receive a value of true.
         * If a style is not defined for ALL characters in the range, it will receive a value of false.
         *
         * @param Object [range=current selection]
         * The range of positions {from,to}
         *
         * @returns Object
         * An object that contains all the styles that are set for the range.
         * If the style is defined for ALL the characters within the range,
         * then the value will be true. If the style is defined for some characters
         * in the range but not all the characters, then the value will be false.
         * If a style is not defined at all within the range, then it will not be set in the return object (undefined).
         *
         * For example, if the range of text has every character bolded, but only one character italic,
         * the return value would be as follows:
         * {bold:true, italic:false}
         */
        inlineGetStyles: function(range) {

            var classes;
            var editor;
            var lineNumber;
            var self;
            var styles;

            self = this;
            editor = self.codeMirror;
            range = range || self.getRange();
            lineNumber = range.from.line;

            styles = {};
            classes = {};

            editor.eachLine(range.from.line, range.to.line + 1, function(line) {

                var charTo;
                var charNumber;
                var charFrom;
                var isRange;

                charFrom = (lineNumber === range.from.line) ? range.from.ch : 0;
                charTo = (lineNumber === range.to.line) ? range.to.ch : line.text.length;

                isRange = Boolean(charFrom !== charTo);

                // Loop through each character in the range
                for (charNumber = charFrom; charNumber <= charTo; charNumber++) {

                    var classesForChar;
                    var marks;

                    classesForChar = {};

                    // Get all of the marks for this character and get a list of the class names
                    marks = editor.findMarksAt({ line: lineNumber, ch: charNumber });

                    marks.forEach(function(mark) {

                        var isSingleChar;
                        var markPosition;

                        if (mark.className) {

                            markPosition = mark.find();

                            // We need to check a couple special cases.
                            //
                            // If you are not selecting a range of characters but instead are looking at a cursor,
                            // CodeMirror still sends us the marks that are next to the position of the cursor.
                            //
                            // Marks can have an "inclusiveLeft" and "inclusiveRight" property, which means to extend the mark
                            // to the left or the right when text is added on that side.
                            //
                            // If the mark is defined to the right of the cursor, then we only include the classname if inclusiveLeft is set.
                            // If the mark is defined to the left of the cursor, then we only include the classname if inclusiveRight is set.

                            isSingleChar = Boolean(charTo - charFrom < 1);

                            if (isSingleChar && markPosition.from.line === lineNumber && markPosition.from.ch === charNumber && !mark.inclusiveLeft) {

                                // Don't add this to the classes if we are on the left side of the range when inclusiveLeft is not set

                            } else if (isSingleChar && markPosition.to.line === lineNumber && markPosition.to.ch === charNumber && !mark.inclusiveRight) {

                                // Don't add this to the classes if we are on the right side of the range when inclusiveRight is not set

                            } else {

                                // Add this class to the list of classes found on this character position
                                classesForChar[mark.className] = true;

                            }
                        }
                    });

                    // If this is the first character in the range, save the list of classes so we can compare against all the other characters
                    if (lineNumber === range.from.line && charNumber === range.from.ch) {
                        classes = $.extend({}, classesForChar);
                    } else {

                        // Check all the previous classes we found, and if they were not also found on the current character,
                        // then mark the class false (to indicate the class was found but is not on ALL characters in the range)

                        // We need to check for one special case - if user characters to the end of the line:
                        //  xxxx[XXX\n]
                        //
                        // Then technically the CodeMirror selection goes to the next line:
                        // xxxx[XXX
                        // ]
                        //
                        // So in that case we end up looking at the next line, which does not have the className.
                        //
                        // To prevent this from interfering with our selection, don't mark the classname as false if we're
                        // looking at a cursor position rather than a range of charcters.

                        if (isRange) {

                            // Set to false for classes that are not on this character
                            $.each(classes, function(className, value) {
                                if (!classesForChar[className]) {
                                    classes[className] = false;
                                }
                            });

                        }

                        // For any additional classes we found (that were not already in the list)
                        // add them to the list but set value false
                        $.each(classesForChar, function(className) {
                            if (!classes[className]) {
                                classes[className] = false;
                            }
                        });
                    }

                }

                lineNumber++;
            });

            // We have a list of class names used within the rich text editor (like 'rte2-style-bold')
            // but we really want the abstracted style names (like 'bold').
            // Convert the class name into the style name.
            $.each(classes, function(className, value) {
                var styleObj;
                styleObj = self.classes[className];
                if (styleObj) {
                    styles[styleObj.key] = value;
                }
            });

            return styles;
        }, // inlineGetStyles


        /**
         * Find all the inline styles that match className and change them into a "collapsed" region.
         * If user clicks on the region or moves the cursor into the region it will automatically expand again.
         */
        inlineCollapse: function(styleKey, range) {

            var className;
            var editor;
            var marks;
            var marksCollapsed;
            var self;

            self = this;
            editor = self.codeMirror;

            // Check if className is a key into our styles object
            className = self.styles[styleKey].className;

            range = range || self.getRange();

            marks = [];
            marksCollapsed = [];

            // Find the marks within the range that match the classname
            $.each(editor.findMarks(range.from, range.to), function(i, mark) {

                // Skip this mark if it is not the desired classname
                if (mark.className == className) {

                    // Save this mark so we can check it later
                    marks.push(mark);

                } else if (mark.collapsed) {

                    // Save this collapsed mark so we can see if it matches another mark
                    marksCollapsed.push(mark);

                }
            });

            $.each(marks, function(i, mark) {

                var markCollapsed;
                var markPosition;
                var $widget;
                var widgetOptions;

                // Check if this mark was previously collapsed
                // (because we saved the collapse mark as a parameter on the original mark)
                // Calling .find() on a cleared mark should return undefined
                markCollapsed = mark.markCollapsed;
                if (markCollapsed && markCollapsed.find()) {
                    return;
                }

                // Create a codemirror "widget" that will replace the mark
                $widget = $('<span/>', {
                    'class': self.styles.collapsed.className,
                    text: '\u2026' // ellipsis character
                });

                // Replace the mark with the collapse widget
                widgetOptions = {
                    inclusiveRight: false,
                    inclusiveLeft: false,
                    replacedWith: $widget[0],
                    clearOnEnter: true // If the cursor enters the collapsed region then uncollapse it
                };

                // Create the collapsed mark
                markPosition = mark.find();
                markCollapsed = editor.markText(markPosition.from, markPosition.to, widgetOptions);
                markCollapsed.collapsed = mark;
                markCollapsed.styleKey = styleKey;

                // If user clicks the widget then uncollapse it
                $widget.on('click', function() {
                    // Use the closure variable "markCollapsed" to clear the mark that we created above
                    markCollapsed.clear();
                    delete mark.markCollapsed;
                    return false;
                });

                // Save markCollapsed onto the original mark object so later we can tell
                // that the mark is already collapsed
                mark.markCollapsed = markCollapsed;

            });

        }, // inlineCollapse


        /**
         *
         */
        inlineUncollapse: function(styleKey, range) {

            var editor;
            var self;

            self = this;
            editor = self.codeMirror;

            range = range || self.getRange();

            // Find the marks within the range that match the classname
            $.each(editor.findMarks(range.from, range.to), function(i, mark) {
                if (mark.collapsed && mark.styleKey === styleKey) {
                    mark.clear();
                    delete mark.collapsed.markCollapsed;
                }
            });
        },


        /**
         *
         */
        inlineToggleCollapse: function(styleKey, range) {

            var className;
            var editor;
            var foundUncollapsed;
            var marks;
            var marksCollapsed;
            var self;

            self = this;
            editor = self.codeMirror;

            // Check if className is a key into our styles object
            className = self.styles[styleKey].className;

            range = range || self.getRange();

            marks = [];
            marksCollapsed = [];

            // Find the marks within the range that match the classname
            $.each(editor.findMarks(range.from, range.to), function(i, mark) {

                // Skip this mark if it is not the desired classname
                if (mark.className == className) {

                    // Save this mark so we can check it later
                    marks.push(mark);

                } else if (mark.collapsed) {

                    // Save this collapsed mark so we can see if it matches another mark
                    marksCollapsed.push(mark);

                }
            });

            $.each(marks, function(i, mark) {

                var markCollapsed;

                // Check if this mark was previously collapsed
                // (because we saved the collapse mark as a parameter on the original mark)
                // Calling .find() on a cleared mark should return undefined
                markCollapsed = mark.markCollapsed;
                if (markCollapsed && markCollapsed.find()) {
                    return;
                }

                foundUncollapsed = true;
                return false;
            });

            if (marks.length) {

                if (foundUncollapsed) {
                    self.inlineCollapse(styleKey, range);
                } else {
                    self.inlineUncollapse(styleKey, range);
                }
            }
        },


        /**
         * Inside any "raw" HTML mark, remove all other marks.
         */
        rawCleanup: function() {

            var editor;
            var self;

            self = this;
            editor = self.codeMirror;

            $.each(editor.getAllMarks(), function(i, mark) {

                var from;
                var marks;
                var pos;
                var styleObj;
                var to;

                // Is this a "raw" mark?
                styleObj = self.classes[mark.className] || {};
                if (styleObj.raw) {

                    // Get the start and end positions for this mark
                    pos = mark.find() || {};
                    if (!pos.from) {
                        return;
                    }

                    from = pos.from;
                    to = pos.to;

                    // Determine if there are other marks in this range
                    marks = editor.findMarks(from, to);
                    if (marks.length > 1) {

                        $.each(marks, function(i, markInside) {

                            var posInside;

                            // Skip this mark if it is the raw mark
                            if (markInside.className === mark.className) {
                                return;
                            }

                            posInside = markInside.find() || {};

                            // Make sure the mark is actually inside the raw area
                            if (posInside.from.ch === pos.to.ch || posInside.to.ch === pos.from.ch) {

                                // Don't do anything because the mark is next to the raw mark, not inside it

                            } else {

                                // Clear other styles within the raw mark
                                self.inlineRemoveStyle('', {from:from, to:to}, {includeTrack:true, except:mark.className, triggerChange:false});

                                // Return false to exit the each loop
                                return false;
                            }

                        });

                    }
                }

            });
        },


        /**
         * CodeMirror makes styles across line boundaries.
         * This function steps through all lines and splits up the styles
         * so they do not cross the end of the line.
         * This allows other functions to operate more efficiently.
         */
        inlineSplitMarkAcrossLines: function(mark) {

            var editor;
            var from;
            var fromCh;
            var lineNumber;
            var options;
            var pos;
            var self;
            var singleLine;
            var styleObj;
            var to;
            var toCh;


            self = this;
            editor = self.codeMirror;

            mark = mark.marker ? mark.marker : mark;

            // Get the style object for this mark so we can determine if this should be treated as a "singleLine" mark
            styleObj = self.classes[mark.className] || {};
            singleLine = Boolean(styleObj.singleLine);

            // Get the start and end positions for this mark
            pos = mark.find();
            if (!pos || !pos.from) {
                return;
            }

            from = pos.from;
            to = pos.to;

            options = self.historyGetOptions(mark);

            // Does this mark span multiple lines?
            if (to.line !== from.line) {

                // Loop through the lines that this marker spans and create a marker for each line
                for (lineNumber = from.line; lineNumber <= to.line; lineNumber++) {

                    fromCh = (lineNumber === from.line) ? from.ch : 0;
                    toCh = (lineNumber === to.line) ? to.ch : editor.getLine(lineNumber).length;

                    // Create a new mark on this line only
                    self.historyCreateMark(
                        { line: lineNumber, ch: fromCh },
                        { line: lineNumber, ch: toCh },
                        options);

                    // Don't create other marks if this is a singleLine mark
                    if (singleLine) {
                        break;
                    }
                }

                // Remove the old mark that went across multiple lines
                self.historyRemoveMark(mark);
            }
        },


        /**
        * Combine all marks that are overlapping or adjacent.
        * Note this assumes that the marks do not span across multiple lines.
         * @param  {Array} spans
         * An array of spans returned by CodeMirror's line.markedSpans parameter.
         * This array consists of objects where each object has a from and to character position,
         * plus a marker.
         * @return {Array}
         * Returns a modified array where the overlapping spans have been combined into a single span.
         */
        inlineCombineAdjacentMarks: function(spans) {

            var editor;
            var spansByClassName;
            var self;

            self = this;
            editor = self.codeMirror;

            // Remove any bookmarks (which are used for enhancements and markers)
            spans = $.map(spans, function(span, i) {

                if (!span.marker) {
                    return undefined;
                }
                if (span.marker.type === 'bookmark') {
                    return undefined;
                }
                return span;
            });

            // Sort the marks in order of position
            spans = spans.sort(function(a, b){
                return a.from - b.from;
            });

            // Next group the marks by class name
            // This will give us a list of classnames, and each one will contain a list of marks in order
            spansByClassName = {};
            $.each(spans, function(i, span) {
                var className;

                className = span.marker.className;

                // Skip any classname that is not in our styles list
                if (!self.classes[className]) {
                    return;
                }

                if (!spansByClassName[className]) {
                    spansByClassName[className] = [];
                }
                spansByClassName[className].push(span);
            });

            // Next go through all the classes, and combine the spans that are adjacent
            var spansAdjusted = [];
            $.each(spansByClassName, function(className, spans) {

                var i;
                var mark;
                var markNext;
                var hasAttributes;

                i = 0;

                if (self.classes[className].onClick) {
                    // Do not combine any classname that has an onClick since we dont' want to mess with those.
                    // For example, links.
                } else if (self.classes[className].initialBody) {
                    // Do not combine any classname that has initialBody (for example, a "discretionary hyphen")
                    // because we don't want to combine them

                } else {
                    // Combine spans if necessary
                    while (spans[i]) {

                        mark = spans[i];
                        markNext = spans[i + 1];

                        if (!markNext) {
                            break;
                        }

                        // Check if either mark has attributes, in which case we should not combine
                        hasAttributes = Boolean((mark.marker && !$.isEmptyObject(mark.marker.attributes)) ||
                            (markNext.marker && !$.isEmptyObject(markNext.marker.attributes)));

                        // Check if the marks overlap
                        if ((markNext.from <= mark.to) && !hasAttributes) {

                            // Extend the first mark
                            mark.to = markNext.to;

                            // Remove the next mark
                            spans.splice(i + 1, 1);

                            // Do not increment the counter in this case because
                            // we need to recheck the first mark against the following
                            // mark because it might overlap that one too
                            continue;
                        }

                        // Continue to the next mark
                        i++;
                    }
                }

                // Add these spans to the list of marks we will return
                Array.prototype.push.apply(spansAdjusted, spans);
            });

            return spansAdjusted;
        },


        //==================================================
        // BlOCK STYLES
        // The following line functions deal with block styles that apply to a single line.
        //==================================================

        /**
         * Toggle the line class within a range:
         * If all lines within the range are already set to the className, remove the class.
         * If one or more lines within the range are not set to the className, add the class.
         *
         * @param String styleKey
         * The line style to toggle.
         *
         * @param Object [range]
         */
        blockToggleStyle: function(styleKey, range) {

            var mark;
            var self;

            self = this;

            range = range || self.getRange();

            if (self.blockIsStyle(styleKey, range)) {
                self.blockRemoveStyle(styleKey, range);
            } else {
                mark = self.blockSetStyle(styleKey, range);
            }

            return mark;
        },


        blockMarkReadOnly: function (mark, range) {
            if (mark.rteReadOnly) {
                this.codeMirror.markText(
                        { line: range.from.line, ch: 0 },
                        { line: range.to.line, ch: null }, // null = end of line
                        {
                            atomic: true,
                            clearWhenEmpty: true,
                            inclusiveLeft: false,
                            inclusiveRight: false
                        });
            }
        },


        /**
         * @param String classname
         *
         * @param {String|Object} style
         * The line style to set.
         *
         * @param Object [range=current selection]
         * The range of positions {from,to}.
         *
         * @param Object [options]
         * Set of key/value pairs to specify options.
         * These options will be passed as mark options when the mark is created.
         *
         * @param Object [options.attributes]
         * An object with key/value pairs for the attributes that should be saved for this block style.
         *
         * @param Object [options.triggerChange=true]
         * Set this to false if you do not want to trigger the rteChange event after setting the style.
         * For example, if you will be making multiple style changes and you will trigger the rteChange event yourself.
         */
        blockSetStyle: function(style, range, options) {

            var className;
            var editor;
            var lineNumber;
            var mark;
            var self;
            var styleObj;

            self = this;
            editor = self.codeMirror;
            range = range || self.getRange();
            options = options || {};

            if (typeof style === 'string') {
                styleObj = self.styles[style];
            } else {
                styleObj = style;
            }
            if (!styleObj) { return; }
            className = styleObj.className;

            // Create a fake "mark" object for the line
            mark = {

                // just in case we need to distinguish this is our fake mark...
                rteLineStyle: true,
                rteReadOnly: styleObj.readOnly,

                // other code checks for className on the mark so we'll save it here too
                className: className
            };

            // Save attributes on the mark
            if (options.attributes) {
                mark.attributes = options.attributes;
            }

            for (lineNumber = range.from.line; lineNumber <= range.to.line; lineNumber++) {

                self.historyCreateLineClass(lineNumber, className);

                // Store the mark data (and attributes) for the block style
                self.blockSetLineData(styleObj.key, lineNumber, mark);
            }

            // If this is a set of mutually exclusive styles, clear the other styles
            if (styleObj.clear) {
                $.each(styleObj.clear, function(i, styleKey) {
                    var lineNumber;
                    var lineRange;

                    for (lineNumber = range.from.line; lineNumber <= range.to.line; lineNumber++) {
                        lineRange = {
                            from: {line: lineNumber, ch:0},
                            to: {line: lineNumber, ch:0}
                        };
                        if (self.blockIsStyle(styleKey, lineRange)) {
                            self.blockRemoveStyle(styleKey, lineRange);
                        }
                    }
                });
            }

            // Refresh the editor display since our line classes
            // might have padding that messes with the cursor position
            self.blockMarkReadOnly(mark, range);
            self.refresh();

            if (options.triggerChange !== false) {
                self.triggerChange();
            }

            //return mark;
            return self.blockGetLineData(styleObj.key, range.from.line) || {};
        },

        /**
         * Set the indent level for a line.
         *
         * @param {Number|Object} lineNumber
         * The line number on which to set indent.
         * Or an object that contains a range, like {from:{line:0,ch:0}, to:{line:1,ch:5}}
         *
         * @param {Number} indentLevel
         * The indent level (number from 1..n)
         *
         * @param Object [options]
         * Set of key/value pairs to specify options.
         * These options will be passed as mark options when the mark is created.
         *
         * @param Object [options.triggerChange=true]
         * Set this to false if you do not want to trigger the rteChange event after setting the style.
         * For example, if you will be making multiple style changes and you will trigger the rteChange event yourself.
         */
        blockSetIndent: function(lineNumber, level, options) {

            var editor;
            var i;
            var indentClass;
            var range;
            var self;

            self = this;
            editor = self.codeMirror;
            options = options || {};

            // Change into an object for consistency
            range = lineNumber;
            if (typeof lineNumber === 'number') {
                range = {from:{line:lineNumber, ch:0}, to:{line:lineNumber, ch:0}};
            }

            if (level < 0) {
                return;
            }

            indentClass = self.indentClassPrefix + level;

            for (i = range.from.line; i <= range.to.line; i++) {
                // Remove any existing indent level
                self.blockRemoveIndent(i);
                if (level > 0) {
                    editor.addLineClass(i, 'text', indentClass);
                }
            }

            // Refresh the editor display since our line classes
            // might have padding that messes with the cursor position
            editor.refresh();

            if (options.triggerChange !== false) {
                self.triggerChange();
            }
        },


        /**
         * Change the indent level up or down.
         *
         * @param {Number} lineNumber
         * @param {Number} delta
         * Amount by which to change the indent level. +1 or -1.
         *
         * @returns {Number}
         * The new indent level.
         */
        blockDeltaIndent: function(lineNumber, delta, options) {
            var indentLevel, self;
            self = this;
            indentLevel = self.blockGetIndent(lineNumber);
            indentLevel = indentLevel + delta;
            if (indentLevel < 0) {
                indentLevel = 0;
            }
            self.blockSetIndent(lineNumber, indentLevel, options)
            return indentLevel;
        },


       /**
         * Remove all indent from a line.
         *
         * @param Number lineNumber
         * The line number where indent should be removed.
         *
         */
        blockRemoveIndent: function(lineNumber, options) {

            var editor;
            var lineInfo;
            var self;

            self = this;
            editor = self.codeMirror;
            options = options || {};

            lineInfo = editor.lineInfo(lineNumber);
            if (lineInfo && lineInfo.textClass) {
                $.each(lineInfo.textClass.split(' '), function(i, className) {
                    if (className.indexOf(self.indentClassPrefix) === 0) {
                        editor.removeLineClass(lineNumber, 'text', className);
                    }
                });
            }

            // Refresh the editor display since our line classes
            // might have padding that messes with the cursor position
            editor.refresh();

            if (options.triggerChange !== false) {
                self.triggerChange();
            }
        },

       /**
         * Returns the indent level for a line.
         * If a line does not have a specified indent but it is a list style,
         * then the indent defaults to 1.
         * Otherwise the indent defaults to 0.
         *
         * @param Number lineNumber
         * The line number where indent should be retrieved.
         *
         * @returns Number
         * Indent level, a number from 0..n
         *
         */
        blockGetIndent: function(lineNumber) {

            var editor, indentLevel, lineInfo, self;

            self = this;
            editor = self.codeMirror;

            indentLevel = 0;

            lineInfo = editor.lineInfo(lineNumber);
            if (lineInfo && lineInfo.textClass) {
                $.each(lineInfo.textClass.split(' '), function(i, className) {

                    var styleObj;

                    // Check if the line has a specified indent level
                    if (className.indexOf(self.indentClassPrefix) === 0) {
                        // Classname looks like "prefix-#"
                        // So lets get the number part of the classname.
                        indentLevel = className.slice( self.indentClassPrefix.length );
                        indentLevel = parseInt( indentLevel, 10 ) || 0;
                        // Since we found a specified indent level, stop looping
                        return;
                    }

                    // Check if this is a style that has a container.
                    // If so it is a list and should default to indent level one (instead of zero)
                    styleObj = self.classes[className];
                    if (styleObj && styleObj.elementContainer) {
                        indentLevel = 1;
                        // Continue looping to check for a specified indent level
                    }
                });
            }

            return indentLevel;
        },


        /**
         * Return the data stored on the line, for a particular style.
         *
         * @param {String} className
         * The className that is used for the style.
         * For example, the alignLeft style uses classname 'rte2-style-alignLeft'
         *
         * @param {Number} lineNumber
         * Number of the line where data should be retrieved.
         *
         * @returns {Object|undefined}
         * The data stored on the line for a particular style,
         * or null if data is not stored for that line number.
         */
        blockGetLineData: function(styleKey, lineNumber) {

            var className;
            var data;
            var editor;
            var lineHandle;
            var self;
            var styleObj;

            self = this;
            editor = self.codeMirror;

            styleObj = self.styles[styleKey] || {};
            className = styleObj.className;

            // Get the lineMarker object so we can store additional data for the block style
            lineHandle = editor.getLineHandle(lineNumber);

            if (lineHandle && lineHandle.rteMarks) {
                data = lineHandle.rteMarks[className];
            }

            return data;
        },


        /**
         * Set data on a line, for a particular class name.
         *
         * @param {String} className
         * The className that is used for the style.
         * For example, the alignLeft style uses classname 'rte2-style-alignLeft'
         *
         * @param {Number} lineNumber
         * Number of the line where data should be stored.
         *
         * @param {Object} data
         * The data to store on the line for a particular class name.
         */
        blockSetLineData: function(styleKey, lineNumber, data) {

            var editor;
            var lineHandle;
            var self;
            var styleObj;
            var className;

            self = this;
            editor = self.codeMirror;

            styleObj = self.styles[styleKey] || {};
            className = styleObj.className;

            // Get the lineMarker object so we can store additional data for the block style
            lineHandle = editor.getLineHandle(lineNumber);

            if (lineHandle) {

                lineHandle.rteMarks = lineHandle.rteMarks || {};

                data = $.extend(true, {}, data, {

                    // Create a "find" function that will return position for the line.
                    // This will make the fake block mark more like a normal inline
                    // mark.
                    find: function() {

                        var lineNumber;

                        // Get the current line number for this line handle

                        lineNumber = editor.getLineNumber(lineHandle) || 0;

                        // Return a position for the line number
                        return {
                            from: {line:lineNumber, ch:0},
                            to:  {line:lineNumber, ch:lineHandle.text.length}
                        };
                    },

                    // Create a "clear" function that clears the style from the line
                    clear: function() {

                        var lineNumber;

                        // Get the current line number for this line handle

                        lineNumber = editor.getLineNumber(lineHandle) || 0;

                        self.blockRemoveStyle(styleKey, {
                            from: {line:lineNumber, ch:0},
                            to:  {line:lineNumber, ch:lineHandle.text.length}
                        });

                        // Return a position for the line number
                        return ;
                    }
                });

                // Save the data on the lineHandle so it will follow
                // the line around, and can be found again by looking
                // up via the class name of the style.
                lineHandle.rteMarks[className] = data;

                // Return the mark
                return data;
            }
        },


        /**
         * Remove the line class for a range.
         *
         * @param String [className]
         * The line style to remove. Set to empty string to remove all line styles.
         *
         * @param Object [range=current selection]
         * The range of positions {from,to}
         */
        blockRemoveStyle: function(styleKey, range) {

            var className;
            var editor;
            var line;
            var lineNumber;
            var self;

            self = this;
            editor = self.codeMirror;
            range = range || self.getRange();

            // styleKey can be an actual key (string) or it can be a style object,
            // in which case we'll get the key from the object
            if (typeof styleKey !== 'string') {
                styleKey = styleKey.key;
            }

            if (styleKey) {
                if (self.styles[styleKey]) {
                    className = self.styles[styleKey].className;
                } else {
                    // A style key was provided but there is no such style
                    return;
                }
            }

            for (lineNumber = range.from.line; lineNumber <= range.to.line; lineNumber++) {

                if (className) {

                    // Remove a single class from the line
                    self.historyRemoveLineClass(lineNumber, className);

                } else {

                    // Remove all classes from the line
                    line = editor.getLineHandle(lineNumber);
                    $.each((line.textClass || '').split(' '), function(i, className) {
                        self.historyRemoveLineClass(lineNumber, className);
                    });
                }
            }

            // Refresh the editor display since our line classes
            // might have padding that messes with the cursor position
            self.refresh();

            self.triggerChange();
        },



        /**
         * Determines if all lines in a range have the specified class.
         */
        blockIsStyle: function(styleKey, range) {

            var self;
            var styles;

            self = this;

            styles = self.blockGetStyles(range);
            return Boolean(styles[styleKey]);
        },


        /**
         * Return a list of all classes that are selected for all lines within a range.
         * If a class is set for one line but not others in the range, it is *not* returned.
         *
         * @param Object [range=current selection]
         */
        blockGetStyles: function(range) {

            var classes;
            var editor;
            var self;
            var styles;

            self = this;
            editor = self.codeMirror;

            range = range || self.getRange();

            // Loop through all lines in the range
            editor.eachLine(range.from.line, range.to.line + 1, function(line) {

                var classNames;
                var classesLine;

                // There is at least one classname on this line
                // Split the class string into an array of individual class names and store in an object for easy lookup
                classNames = line.textClass || '';
                classesLine = {};
                $.each(classNames.split(' '), function(i, className) {
                    if (className) {
                        classesLine[className] = true;
                    }
                });

                // Check if we are on the first line
                if (!classes) {

                    // We are on the first line so add all classes to the list
                    classes = $.extend({}, classesLine);

                } else {

                    // We are not on the first line, so remove any class from the list
                    // if it is not also on the current line
                    $.each(classes, function(className) {
                        if (!classesLine[className]) {
                            classes[className] = false;
                        }
                    });

                    // For any additional classes we found (that were not already in the list)
                    // add them to the list but set value false
                    $.each(classesLine, function(className) {
                        if (!classes[className]) {
                            classes[className] = false;
                        }
                    });
                }
            });

            // We have a list of class names used within the rich text editor (like 'rte2-ol')
            // but we really want the abstracted style names (like 'ol').
            // Convert the class name into the style name.
            styles = {};
            if (classes) {
                $.each(classes, function(className, value) {
                    var styleObj;
                    styleObj = self.classes[className];
                    if (styleObj) {
                        styles[styleObj.key] = value;
                    }
                });
            }

            return styles;
        },

        /**
         * Returns the list type for a line.
         *
         * So what is a list? For our purposes, it is an item in the styles defenition object
         * that has an "elementContainer" parameter.
         *
         * @param Number lineNumber
         * The line number to check.
         *
         * @returns String
         * The key from the style definition object.
         * For example:
         * '' = not a list
         * 'ul' = unordered list item
         * 'ol' = ordered list item
         */
        blockGetListType: function(lineNumber) {

            var classNames;
            var editor;
            var line;
            var lineInfo;
            var listType;
            var self;

            self = this;
            editor = self.codeMirror;

            listType = '';

            line = editor.getLineHandle(lineNumber);
            lineInfo = editor.lineInfo(line);
            classNames = lineInfo.textClass || "";

            $.each(classNames.split(' '), function(i, className) {

                var styleObj;

                styleObj = self.classes[className];
                if (styleObj && styleObj.elementContainer) {
                    listType = styleObj.key;
                    return false;
                }
            });

            return listType;
        },


        /**
         * Create a lineWidget to display a preview (presumably an image) for a block style.
         * This lineWidget will be displayed above the line, and will move with the line.
         * If the style is removed (or the line is removed) the lineWidget will also be removed.
         *
         * @param {String} styleKey
         * @param {Number} lineNumber
         * @param {String} previewHTML
         * @param {Object} attributes
         */
        blockSetPreview: function(styleKey, lineNumber, previewHTML, attributes) {

            var data;
            var editor;
            var $preview;
            var self;

            self = this;

            editor = self.codeMirror;

            // Make sure this style is defined on the line,
            // and get the data object attached to the lineHandle
            data = self.blockGetLineData(styleKey, lineNumber);
            if (!data) { return; }

            // If there is already a preview remove it
            self.blockRemovePreview(styleKey, lineNumber);

            // Create DOM for the preview HTML
            $preview = $('<div>', {
                'class': 'rte2-block-preview',
                'data-style-key': styleKey
            }).html(previewHTML);

            if (attributes) {
                $.each(attributes, function (key, value) {
                    $preview.attr('data-attr-' + key, value);
                });
            }

            // Save the DOM node in the line data
            data.$preview = $preview;

            // Create a line widget to show the preview
            data.previewMark = editor.addLineWidget(lineNumber, $preview[0], {rteBlockPreview:true});

            $preview.on('resize', function () {
                data.previewMark.changed();
            });
        },


        /**
         * Same as blockSetPreview() but instead of a style and line number,
         * you start with a mark.
         *
         * Note for line styles, the mark is not actually a CodeMirror mark,
         * but an object we created to mimic a CodeMirror mark.
         * But it should have a find() function that returns the location of the
         * mark even if the line has shifted.
         * Refer to blockSetLineData() for more info.
         *
         * @param {Object} mark
         * The mark created for a line style.
         *
         * @param {String} previewHTML
         */
        blockSetPreviewForMark: function(mark, previewHTML) {

            var lineNumber;
            var range;
            var self;
            var styleKey;
            var styleObj;

            self = this;

            // Get the style from the mark
            styleObj = self.classes[mark.className];
            styleKey = styleObj.key;

            // Get the line number from the mark
            range = mark.find();
            lineNumber = range.from.line;

            return self.blockSetPreview(styleKey, lineNumber, previewHTML, mark.attributes);
        },


        /**
         * Work around a problem in CodeMirror: whenever refresh() is called, CodeMirror recreates the DOM.
         * This causes weird scrolling problems in cases where the preview contains an iframe, because the iframe
         * reloads whenever it is removed and added back to the DOM.
         * To mitigate this problem, we will query the height of the preview and set that as a minimum height,
         * so even if the iframe contents shrink and grow, the scrolling of the overall document should not change.
         */
        initPreviewResizer: function() {

            var self;
            var $wrapper;

            self = this;
            $wrapper = $(self.codeMirror.getWrapperElement());

            $wrapper.on('resize', '.rte2-block-preview > iframe', function(event) {
                var $div;
                var divHeight;
                var $iframe;
                var iframeHeight;

                $iframe = $(event.target);
                // Get the height from the CSS style, not by calculating the height.
                iframeHeight = parseInt($iframe.prop('style').height, 10);

                $div = $iframe.closest('.rte2-block-preview');
                divHeight = parseInt($div.prop('style').height, 10);

                // If height is not explicitly set we won't do anything.
                if (!iframeHeight) {
                    return;
                }

                if (!divHeight || iframeHeight > divHeight) {
                    $div.css({
                        'height': iframeHeight,
                        'overflow': 'auto'
                    });
                } else if (iframeHeight === divHeight) {
                    // Heights are equal so do nothing
                } else if (iframeHeight < divHeight) {
                    // Height is shrinking so do nothing
                    // Note we could shrink the preview block, but in some cases like twitter the preview loads
                    // and is very small, then it grows, then it shrinks again.
                    // This leads to the page scroll position jumping all over the place,
                    // so instead we will just set the preview block to the tallest height
                    // we have seen and leave it at that.
                }
            });
        },


        /**
         * Remove the preview lineWidget for a block style
         * (if it exists).
         *
         * @param {String} styleKey
         * @param {Number} lineNumber
         */
        blockRemovePreview: function(styleKey, lineNumber) {

            var data;
            var editor;
            var self;

            self = this;

            editor = self.codeMirror;

            // Make sure this style is defined on the line,
            // and get the data object attached to the lineHandle
            data = self.blockGetLineData(styleKey, lineNumber);
            if (!data) { return; }
            if (!data.$preview) { return; }
            if (!data.previewMark) { return; }

            // Delete the preview dom
            data.$preview.remove();
            delete data.$preview;

            // Delete the line widget
            data.previewMark.clear();
            editor.removeLineWidget(data.previewMark);
            delete data.previewMark;
        },


        /**
         * Remove the preview lineWidget for a block style (if it exists).
         * Same as blockRemovePreview(), but starting from a className instead of the style key.
         *
         * @param {String} className
         * @param {Number} lineNumber
         */
        blockRemovePreviewForClass: function(className, lineNumber) {
            var self;
            var styleObj;

            self = this;
            styleObj = self.classes[className];
            if (styleObj) {
                self.blockRemovePreview(styleObj.key, lineNumber);
            }
        },


        blockEachLineMark: function (callback) {
            this.codeMirror.eachLine(function (line) {
                var marks = line.rteMarks;

                if (marks) {
                    $.each(marks, callback);
                }
            });
        },


        //--------------------------------------------------
        // Enhancements
        // An enhancement is a block of external content that can be added to the editor.
        //--------------------------------------------------

        /**
         * Initialize internal storage for enhancements.
         */
        enhancementInit: function() {

            var self = this;

            // Unique ID to use for enchancments.
            self.enhancementId = 0;

            // Internal storage for enhancements that have been added.
            // CodeMirror doesn't seem to have a way to get a list of enhancements,
            // so we'll have to remember them as we create them.
            self.enhancementCache = {};

            // Check when user presses Return at beginning of a line that contains an enhancement
            self.codeMirror.on('change', function(cm, changeObj) {
                self.enhancementNewlineAdjust(changeObj);
            });

        },


        /**
         * Add an enhancement into the editor.
         * Note if you change the size of the enhancement content then you must call the refresh() method
         * to update the editor display, or the cursor positions will not be accurate.
         *
         * @param Element|jQuery content
         *
         * @param Number [lineNumber=starting line of the current range]
         * The line number to add the enhancement. The enhancement is always placed at the start of the line.
         *
         * @param Object [options]
         * @param Object [options.block=false]
         * @param Object [options.index]
         * The position to insert the enhancement, if there are other enhancements on the line.
         * Can be 0 to insert the enhancement before any others, or undefined to insert after others.
         * @param Function [options.toHTML]
         * Function to return HTML content to be placed at the point of the enhancement.
         * If not provided then the enhancement will not appear in the output.
         *
         * @returns Object
         * The "mark" object that can be used later to move, remove, or update the enhancment content.
         *
         * @example
         * $content = $('<div>My Enhancement Content</div>');
         * rte.enhancementAdd($content);
         */
        enhancementAdd: function(content, lineNumber, options) {

            var editor;
            var mark;
            var range;
            var self;
            var widgetOptions;

            self = this;
            editor = self.codeMirror;

            options = options || {};

            // In case someone passes a jQuery object, convert it to a DOM element
            content = $(content)[0];

            if (lineNumber === null || typeof lineNumber === 'undefined') {
                range = self.getRange();
                lineNumber = range.from.line;
            }

            // Replace the mark with the collapse widget
            widgetOptions = {
                widget: content
            };

            if (options.block) {

                // Create the line widget.
                // We set a flag rteEnhancement on the line widget, so we can distinguish
                // it from other line widgets later
                mark = editor.addLineWidget(lineNumber, content, {above: true, rteEnhancement:true, insertAt: options.index});

                mark.deleteLineFunction = function(){

                    var content;
                    var $content;

                    content = self.enhancementGetContent(mark);
                    $content = $(content).detach();
                    self.enhancementRemove(mark);

                    setTimeout(function(){
                        self.enhancementAdd($content[0], null, options);
                    }, 100);

                };

                // If the line is deleted we don't want to delete the enhancement!
                mark.line.on('delete', mark.deleteLineFunction);

            } else {
                mark = editor.setBookmark({line:lineNumber, ch:0}, widgetOptions);
            }

            // Save the options we used along with the mark so it can be used later to move the mark
            // and to call the toHTML function.
            mark.options = options;

            // Save the mark onto the content element so it can be access later
            // using the enhancementGetMark(el) function.
            // This can be used for example to implement a toolbar within the enhancement
            // since the toolbar might need access to the mark for changing the enhancement settings.
            $(content).data('enhancementMark', mark);

            // Save the mark in an internal cache so we can use it later to output content.
            mark.options.id = self.enhancementId++;
            self.enhancementCache[ mark.options.id ] = mark;

            self.triggerChange();

            // Small delay before refreshing the editor to prevent cursor problems
            setTimeout(function(){
                self.refresh();
                mark.changed();
            }, 100);

            return mark;
        },


        /**
         * Move an enhancement block up or down a line.
         *
         * @param Object mark
         * The mark that was returned by the enhancementAdd() function.
         *
         * @param Number lineDelta
         * Direction to move the mark:
         * -1 = move the mark up one line
         * +1 = move the mark down one line
         *
         * @return Object
         * Returns a new mark that contains the enhancement content.
         */
        enhancementMove: function(mark, lineDelta) {

            var editor;
            var enhancementsOnLine;
            var index;
            var indexNew;
            var length;
            var lineLength;
            var lineNumber;
            var lineMax;
            var self;

            self = this;
            editor = self.codeMirror;

            lineMax = editor.lineCount() - 1;
            lineNumber = self.enhancementGetLineNumber(mark);

            // Get array of enhancements on the current line.
            // But only the block enhancements
            enhancementsOnLine = self.enhancementGetFromLine( self.enhancementGetLineNumber(mark), false );

            // If there are multiple enhancements on this line, determine if we need to keep the enhancement
            // on this line (and rearrange the order of enhancements) or move to another line
            length = enhancementsOnLine.length;
            if (length > 1) {
                // Determine the index for this enhancement
                $.each(enhancementsOnLine, function(i,enhancement) {
                    if (mark === enhancement) {
                        index = i;
                        return false;
                    }
                });

                // Check to make sure we found the enhancement index
                if ($.isNumeric(index)) {

                    // Determine if the index should be increased or decreased
                    index += lineDelta;

                    // Make sure the enhancement should stay on this line
                    if (index >= 0 && index < length) {

                        // Special case, if enhancement is moving to the end of the list,
                        // we must set index=undefined
                        if (index === length - 1) {
                            index = undefined;
                        }

                        return self.enhancementMoveToLine(mark, lineNumber, index);
                    }
                }
            }

            // There is only a single enhancement on this line, so move it to another line
            lineNumber += lineDelta;
            if (lineNumber < 0) {
                return mark;
            }

            if (lineNumber > lineMax) {

                // Add another line to the end of the editor.
                // Set the change event origin to "brightspotEnhancementMove" so we know this newline is being
                // added automatically rather than the user typing (so we don't try to adjust the
                // position of the enhancement - see enhancementNewlineAdjust()
                lineLength = editor.getLine(lineMax).length;
                editor.replaceRange('\n', {line:lineMax, ch:lineLength}, undefined, 'brightspotEnhancementMove');
            }

            // If the next (or previous) line is blank, then try to move to the line after that (if it is not blank)
            lineDelta = Math.sign(lineDelta) * 1;
            if (self.isLineBlank(lineNumber) && !self.isLineBlank(lineNumber + lineDelta)) {
                lineNumber += lineDelta;
            }

            // If moving to the previous line, add the enhancement to the end of any other enhancements.
            // If moving to the next line, add the enhancement before any other enhancements.
            index = (lineDelta === -1) ? undefined : 0;

            return self.enhancementMoveToLine(mark, lineNumber, index);
        },


        /**
         * Move an enhancement block to a specific line.
         *
         * @param Object mark
         * The mark that was returned by the enhancementAdd() function.
         *
         * @param Number lineNumber
         *
         * @param Number [index]
         * If the lineNumber already contains enhancements, you can specify
         * an index to position the new enhancement in relation to the others.
         * Specify 0 to position at the beginning of the line, or undefined to
         * position after the other enhancements.
         *
         * @return Object
         * Returns a new mark that contains the enhancement content.
         */
        enhancementMoveToLine: function(mark, lineNumber, index) {

            var content;
            var $content;
            var options;
            var self;

            self = this;

            // Get the options we saved previously so we can create a mark with the same options
            options = mark.options;
            options.index = index;

            // Depending on the type of mark that was created, the content is stored differently
            content = self.enhancementGetContent(mark);
            $content = $(content).detach();

            self.enhancementRemove(mark);

            mark = self.enhancementAdd($content[0], lineNumber, options);

            self.triggerChange();

            return mark;
        },


        /**
         * When a CodeMirror change event occurs, determine if a new line has been added
         * and if any enhancements on the line need to be adjusted.
         * Use case: Cursor is at the beginning of a line where an enhancement is attached
         * (above the line). User presses Return and inserts a new line. The current line
         * is moved below the new line and moves the enhancement with it. This is not
         * intuitive since the user expects the new line to be placed below the enhancement.
         * So in this case we move the enhancement above the new line that was entered.
         *
         * @param {Object} changeObj
         * A change event from CodeMirror.
         */
        enhancementNewlineAdjust: function(changeObj) {

            var lineInfo;
            var lineNumber;
            var self;

            self = this;

            // New line adjustment does not need to take place after 'brightspotPaste' events. This is triggered
            // when switching between HTML and Rich Text modes.
            if (changeObj.origin === 'brightspotEnhancementMove' || changeObj.origin === 'brightspotPaste') {
                return;
            }

            if (changeObj.from &&
                changeObj.from.ch === 0 && // change occurred at the first character of the line
                changeObj.text &&
                changeObj.text.length > 1 && // change is split into multiple lines
                changeObj.text[0] === '') { // first character of the change is a new line

                // The original line number
                lineNumber = changeObj.from.line;

                // The line info for the original line (that was pushed down) so we can get the enhancements
                lineInfo = self.codeMirror.lineInfo(lineNumber + changeObj.text.length - 1);

                if (lineInfo && lineInfo.widgets) {
                    $.each(lineInfo.widgets, function(i,mark) {
                        if (mark && mark.rteEnhancement) {
                            self.enhancementMoveToLine(mark, lineNumber);
                        }
                    });
                }
            }
        },


        /**
         * Removes an enhancement.
         *
         * @param Object mark
         * The mark that was returned when you called enhancementAdd().
         */
        enhancementRemove: function(mark) {
            var self;
            self = this;
            delete self.enhancementCache[ mark.options.id ];
            if (mark.deleteLineFunction) {
                mark.line.off('delete', mark.deleteLineFunction);
            }
            mark.clear();
            self.codeMirror.removeLineWidget(mark);

            self.triggerChange();
        },


        /**
         * Given a mark object created from enhancementAdd() this function
         * returns the DOM element for the content.
         *
         * @param Object mark
         * The mark that was returned when you called enhancementAdd.
         *
         * @returns Element
         * The DOM element for the enhancement content.
         */
        enhancementGetContent: function(mark) {

            // Get the content element for the mark depending on the type of mark
            return mark.node || mark.replacedWith;
        },


        /**
         * @returns Number
         * The line number of the mark, or 0 if the mark is not in the document.
         */
        enhancementGetLineNumber: function(mark) {

            var lineNumber;
            var position;

            lineNumber = undefined;
            if (mark.line) {
                lineNumber = mark.line.lineNo();
            } else if (mark.find) {
                position = mark.find();
                if (position) {
                    lineNumber = position.line;
                }
            }

            return lineNumber;
        },


        /**
         * Turn the enhancement into an inline element that is at the beginning of a line.
         * You must do this if you plan to float the enhancment left or right.
         *
         * @param Object mark
         * The mark that was returned when you called enhancementAdd().
         */
        enhancementSetInline: function(mark, options) {

            var content;
            var $content;
            var lineNumber;
            var self;

            self = this;

            options = options || {};

            lineNumber = self.enhancementGetLineNumber(mark);

            content = self.enhancementGetContent(mark);
            $content = $(content).detach();

            self.enhancementRemove(mark);

            return self.enhancementAdd($content[0], lineNumber, $.extend({}, mark.options || {}, options, {block:false}));
        },


        /**
         * Turn the enhancement into a block element that goes between two lines.
         *
         * @param Object mark
         * The mark that was returned when you called enhancementAdd().
         *
         * @returns Object
         * The new mark that was created.
         */
        enhancementSetBlock: function(mark, options) {

            var content;
            var $content;
            var lineNumber;
            var self;

            self = this;

            options = options || {};

            lineNumber = self.enhancementGetLineNumber(mark);

            content = self.enhancementGetContent(mark);
            $content = $(content).detach();

            self.enhancementRemove(mark);

            return self.enhancementAdd($content[0], lineNumber, $.extend({}, mark.options || {}, options, {block:true}));
        },


        /**
         * When an enhancement is imported by the toHTML() function, this function
         * is called. You can override this function to provide additional functionality
         * for your enhancements.
         *
         * If you override this function you are responsible for calling enhancementAdd()
         * to actually create the ehancement in the editor.
         *
         * @param jQuery $content
         * A jQuery object containing the enhancement content from the HTML.
         *
         * @param Number line
         * The line number where the enhancement was found.
         */
        enhancementFromHTML: function($content, line) {
            var self;
            self = this;
            self.enhancementAdd($content, line, {toHTML: function(){
                return $content.html();
            }});
        },


        /**
         * Given the content element within the enhancement, this function returns the
         * mark for the enhancement.
         *
         * @param {Element} el
         * This must be the element that was passed in to the enhancementAdd() function.
         *
         * @return {Object} mark
         * The mark object that can be used to modify the enhancement.
         */
        enhancementGetMark: function(el) {
            return $(el).data('enhancementMark');
        },


        /**
         * Returns an array of the enhancements on a line.
         * @param {Number} lineNumber
         * @param {Boolean} [inline=true]
         * Should inline (align left or right) enhancements be included?
         * Defaults to true, explicitly set this to false to exclude those enhancements.
         * @return {Array}
         */
        enhancementGetFromLine: function(lineNumber, inline) {
            var editor;
            var lineHandle;
            var self;
            var list;

            self = this;
            editor = self.codeMirror;
            list = [];

            // Get all the block enhancements
            lineHandle = editor.getLineHandle(lineNumber);
            if (lineHandle && lineHandle.widgets) {
                $.each(lineHandle.widgets, function(i,widget) {
                    var mark;
                    if (widget.node) {
                        mark = self.enhancementGetMark(widget.node);
                        if (mark) {
                            list.push(mark);
                        }
                    }
                });
            }

            // Get all the enhancements that are align left and right
            if (inline !== false) {
                $.each(self.enhancementCache, function(i, mark) {

                    // Make sure this is not a block enhancement
                    if (mark && mark.options && mark.options.block === false) {
                        if (self.enhancementGetLineNumber(mark) === lineNumber) {
                            list.push(mark);
                        }
                    }
                });
            }

            return list;
        },


        //==================================================
        // Dropdown Handlers
        //==================================================

        dropdownInit: function() {

            var clicks;
            var editor;
            var self;

            self = this;

            editor = self.codeMirror;

            self.$dropdown = $('<div/>', {
                'class': 'rte2-dropdown'
            }).hide().appendTo( editor.getWrapperElement().parentNode );

            editor.on('cursorActivity', $.debounce(250, function(instance, event) {
                self.dropdownCheckCursor();
            }));

            editor.on('focus', function() {
                self.dropdownCheckCursor();
            });

            editor.on('blur', function() {
                // Hide after a timeout in case user clicked link in the dropdown
                // which caused a blur event that hid the dropdown
                setTimeout(function(){
                    self.dropdownHide();
                }, 200);
            });

            // CodeMirror's dblclick event doesn't work reliably,
            // so have to provide our own double click detection
            clicks = 0;
            editor.on('mousedown', function(instance, event) {
                clicks++;
                if (clicks == 1) {
                    setTimeout(function(){
                        if (clicks > 1) {
                            self.dropdownDoubleClick(event);
                        }
                        clicks = 0;
                    }, 300);
                }
            });
        },


        /**
         * When user double clicks, check if there are any dropdown marks, and if so
         * imediately popu up the edit form for the first mark located.
         */
        dropdownDoubleClick: function(event) {
            var marks;
            var self;

            self = this;

            // Get all the marks within a range
            marks = self.dropdownGetMarks(true);

            if (marks.length) {
                event.preventDefault();
                event.codemirrorIgnore = true;
                self.onClickDoMark(event, marks[0]);
                return false;
            }
        },


        dropdownCheckCursor: function() {

            var marks;
            var self;

            self = this;

            if (self.readOnlyGet() || !self.codeMirror.hasFocus()) {
                self.dropdownHide();
                return;
            }

            // Get all the marks from the selected range that have onclick handlers
            marks = self.dropdownGetMarks();

            if (marks.length === 0) {
                self.dropdownHide();
            } else {
                self.dropdownShow(marks);
            }
        },


        /**
         * Get all the marks in the current range that have click events.
         *
         * @param {Boolean} [allowRange=false]
         * Set to true if you want the marks across a range of characters.
         * Defaults to false, which means it will only return marks if the selection range is a cursor position.
         */
        dropdownGetMarks: function(allowRange) {
            var editor;
            var lineStyles;
            var marks;
            var range;
            var self;

            self = this;
            editor = self.codeMirror;
            range = self.getRange();

            // Do not return marks if a range of characters is selected
            if (allowRange !== true && !(range.from.line === range.to.line && range.from.ch === range.to.ch)) {
                return [];
            }

            // Get the line styles and the "fake" marks we created
            marks = [];
            lineStyles = self.blockGetStyles();
            $.each(lineStyles, function(styleKey) {
                var mark;
                mark = self.blockGetLineData(styleKey, range.from.line);
                if (mark) {
                    marks.push(mark);
                }
            });

            // Find all the inline marks for the clicked position
            // (funky javascript to append one array onto the end of another)
            [].push.apply(marks, editor.findMarks(range.from, range.to));

            // Only keep the marks that have onClick configs
            marks = $.map(marks, function(mark, i) {
                var styleObj;
                styleObj = self.classes[mark.className];
                if (styleObj && (styleObj.onClick || styleObj.void || styleObj.readOnly || styleObj.dropdown)) {
                    // Keep in the array
                    return mark;
                } else {
                    // Remove from the array
                    return null;
                }
            });

            // Update marks to account for undo history marks getting re-created
            $.each(marks, function(i, mark) {
                while (mark.markNew) {
                    mark = mark.markNew;
                }
                marks[i] = mark;
            });

            return marks;
        },


        moveMark: function (mark, direction) {
            if (!mark || direction === 0) {
                return;
            }

            var markRange = mark.find();

            if (!markRange) {
                return;
            }

            var self = this;
            var cm = self.codeMirror;

            // Find the number of blank lines after the mark to include in the
            // move.
            var to = markRange.to.line + 1;
            var blanksAfter = -1;

            while (to < cm.lineCount() && cm.getLine(to) === '') {
                ++ to;
                ++ blanksAfter;
            }

            // Find the number of blank lines before the mark to preserve before
            // the "from" position.
            var from = markRange.from.line;
            var blanksBefore = 0;

            while (from > 0 && cm.getLine(from - 1) === '') {
                -- from;
                ++ blanksBefore;
            }

            // Improve spacing when mark is moved from first position.
            if (from === 0) {
                ++ blanksBefore;
            }

            // Make sure that the move is possible.
            var move = -1;

            if (direction < 0) {
                move = from - 1;

                // Skip over the blank lines right above the mark.
                while (move >= 0 && cm.getLine(move) === '') {
                    -- move;
                }

                if (move !== 0) {
                    -- move;
                }

            } else if (to < cm.lineCount()) {
                move = from + 1;
            }

            // Move isn't possible so restore cursor and do nothing.
            if (move < 0) {
                cm.setCursor(cm.getCursor());
                cm.focus();
                return;
            }

            // Move the mark.
            cm.operation(function () {
                var initialTop = cm.charCoords({ line: from, ch: 0 }).top;
                var html = self.toHTML(markRange);
                var cursor = cm.getCursor();

                var replaceWithNewLine = blanksBefore + blanksAfter > 0 && from !== 0;

                // Delete the existing mark.
                // Collapses one or more adjacent blank lines into single blank line
                if (replaceWithNewLine) {
                    cm.replaceRange('\n', { line: from, ch: 0 }, { line: to, ch: 0 });
                    if (move !== 0) {
                        move += 1;
                    }
                } else {
                    cm.replaceRange('', { line: from, ch: 0 }, { line: to, ch: 0 });
                }

                // Insert the blank lines found previously before the mark.
                if (move > 0) {
                    for (var i = 0; i < blanksBefore; ++ i) {
                        cm.replaceRange('\n', { line: move, ch: 0}, { line: move, ch: 0 });
                        if (direction > 0) {
                            ++ move;
                        }
                    }
                }

                // Insert an extra blank line if moving to the beginning or
                // the end of the text and there isn't already a blank line
                // there. This is done to improve the spacing display.
                if ((move === 0 && cm.getLine(move) !== '') || move === cm.lineCount()) {
                    cm.replaceRange('\n', { line: move, ch: 0 }, { line: move, ch: 0 });
                }

                // Insert the blank lines found previously after the mark.
                for (var i = 0; i < blanksAfter; ++ i) {
                    cm.replaceRange('\n', { line: move, ch: 0}, { line: move, ch: 0});
                }

                // Insert the mark at the new position.
                self.fromHTML(html, { from: { line: move, ch: 0}, to: { line: move, ch: 0} });

                // Move the cursor to the newly created mark.
                var cursorLine = move + blanksAfter + cursor.line - from + 1;

                cm.setCursor({ line: cursorLine, ch: cursor.ch });
                cm.focus();

                // Scroll the window so that the mouse is over the same
                // area as before.
                $(window).scrollTop($(window).scrollTop() + (cm.charCoords({ line: cursorLine, ch: 0 }).top - initialTop));

                // Remove all blanks lines above the mark if there aren't any
                // other texts.
                var first = move;

                for (; first >= 0; -- first) {
                    if (cm.getLine(first) !== '') {
                        break;
                    }
                }

                if (first === -1) {
                    cm.replaceRange('', { line: 0, ch: 0 }, { line: move + 1, ch: 0 });

                } else {

                    // Remove all blanks lines below the mark if there aren't
                    // any other texts.
                    var lastInitial = move + (to - from) + blanksAfter;
                    var last = lastInitial;
                    var lineCount = cm.lineCount();

                    for (; last < lineCount; ++ last) {
                        if (cm.getLine(last) !== '') {
                            break;
                        }
                    }

                    if (last === lineCount) {
                        cm.replaceRange('', { line: lastInitial, ch: 0 }, { line: lineCount, ch: 0 });
                    }
                }
            });
        },


        /**
         * @param {Array} marks
         */
        dropdownShow: function(marks) {
            var self;
            self = this;

            self.$dropdown.empty();

            $.each(marks, function(i, mark) {

                var $div;
                var label;
                var styleObj;

                // Get the label to display for this mark.
                // It defaults to the className of the style.
                // Of if the style definition has a getLabel() function
                // call that and use the return value
                styleObj = self.classes[mark.className] || {};
                label = styleObj.enhancementName || mark.className;
                if (styleObj.getLabel) {
                    label = styleObj.getLabel(mark);
                }

                $div = $('<div/>', {
                    'class': 'rte2-dropdown-item'
                }).appendTo(self.$dropdown);

                if (styleObj.dropdown) {
                    $div.append( styleObj.dropdown(mark) );
                    return;
                }

                $('<span/>', {
                    'class':'rte2-dropdown-label',
                    text: label
                }).appendTo($div);

                // Check if this is a link with an href attribute
                if (mark.attributes && mark.attributes.href) {
                    $('<a/>', {
                        'class': 'rte2-dropdown-href',
                        text: mark.attributes.href,
                        href: mark.attributes.href,
                        target: '_blank'
                    }).appendTo($div);
                }

                // Popup edit defaults to true, but if set to false do not include edit link
                if (styleObj.popup !== false && styleObj.onClick) {
                    $('<a/>', {
                        'class': 'rte2-dropdown-edit',
                        text: 'Edit'
                    }).on('click', function(event){
                        event.preventDefault();
                        self.onClickDoMark(event, mark);
                        return false;
                    }).appendTo($div);
                }

                if (styleObj.line) {
                    $div.append($('<a/>', {
                        'class': 'rte2-dropdown-move-up',
                        text: 'Move Up',
                        click: function () {
                            self.moveMark(mark, -1);
                            return false;
                        }
                    }));

                    $div.append($('<a/>', {
                        'class': 'rte2-dropdown-move-down',
                        text: 'Move Down',
                        click: function () {
                            self.moveMark(mark, 1);
                            return false;
                        }
                    }));
                }

                $('<a/>', {
                    'class': 'rte2-dropdown-clear',
                    text: styleObj.readOnly ? 'Remove' : 'Clear'
                }).on('click', function(event){

                    var pos;

                    event.preventDefault();

                    // For void element, delete the text in the mark
                    if (styleObj.readOnly || styleObj.void) {
                        if (mark.find) {
                            pos = mark.find();
                            // Delete below after the mark is cleared
                        }
                    }

                    // Remove the text before the mark is removed, to ensure undo works
                    if (pos) {
                        self.codeMirror.replaceRange('', {line:pos.from.line, ch:pos.from.ch}, {line:pos.to.line, ch:pos.to.ch});
                    }

                    mark.clear();

                    self.focus();
                    self.dropdownCheckCursor();
                    self.triggerChange();
                    return false;
                }).appendTo($div);

            });

            // Set position of the dropdown
            self.dropdownSetPosition(marks);

            self.$dropdown.show();
        },

        dropdownSetPosition: function(marks) {

            var ch;
            var editor;
            var line;
            var pos;
            var self;

            self = this;

            editor = self.codeMirror;

            line = 0;
            ch = undefined;

            // Find the largest line number and the left-most character in all the marks.
            // If any of the marks extends across multiple lines, use character 0
            $.each(marks, function(i, mark) {
                var pos;

                pos = mark.find();

                if (pos) {

                    if (pos.to.line > line) {
                        line = pos.to.line;
                    }

                    if (ch === undefined || pos.from.ch < ch) {
                        ch = pos.from.ch;
                    }

                    if (pos.from.line !== pos.to.line) {
                        ch = 0;
                    }
                }
            });

            // Get the position for the line and ch
            pos = editor.cursorCoords({line:line, ch:(ch||0)}, 'local');

            self.$dropdown.css({
                left: pos.left,
                top: pos.bottom
            });
        },

        dropdownHide: function() {
            var self;
            self = this;
            self.$dropdown.hide();
        },


        //==================================================
        // OnClick Handlers
        //==================================================

        /**
         * Do the onclick event for a mark.
         * For example, a link or inline enhancement mark might have an onclick handler
         *
         * @param Object event
         * The click event.
         *
         * @param Object mark
         * The CodeMirror mark. Note this mark must have a className, which will be used
         * to find the style object that contains the onclick handler.
         */
        onClickDoMark: function(event, mark) {

            var range;
            var self;
            var styleObj;

            self = this;

            styleObj = self.classes[mark.className];
            if (styleObj && styleObj.onClick) {

                // Make this mark the current selection
                range = self.markGetRange(mark);
                self.codeMirror.setSelection(range.from, range.to);

                styleObj.onClick(event, mark);
            }
        },

        //--------------------------------------------------
        // Track Changes
        //--------------------------------------------------

        /**
         * Set up event handlers for tracking changes.
         */
        trackInit: function() {

            var editor;
            var self;

            self = this;

            editor = self.codeMirror;

            // Monitor the "beforeChange" event so we can track changes
            editor.on('beforeChange', function(instance, changeObj) {
                self.trackBeforeChange(changeObj);
            });

            // Update the display to show current state of tracking
            self.trackDisplayUpdate();
        },


        /**
         * Turn track changes on or off.
         */
        trackSet: function(on) {
            var self;
            self = this;
            self.trackChanges = Boolean(on);
        },


        /**
         * Toggle track changes (on or off)
         */
        trackToggle: function() {
            var self;
            self = this;
            self.trackSet( !self.trackIsOn() );
        },


        /**
         * Determine if track changes is currently on.
         *
         * @returns Boolean
         * True if track changes is on.
         */
        trackIsOn: function() {
            var self;
            self = this;
            return Boolean(self.trackChanges);
        },


        /**
         * Event handler for codeMirror to implement track changes.
         * When new text is added mark it as inserted.
         * When text deleted, mark it as deleted instead of actually deleting it.
         * This code handles a lot of special cases such as selecting a region that
         * already has track changes marks, then pasting new content on top, etc.
         *
         * @param Object changeObj
         * The CodeMirror change object returned by the "beforeChange" event.
         */
        trackBeforeChange: function(changeObj) {

            var classes;
            var editor;
            var cursorPosition;
            var isEmpty;
            var self;
            var textOriginal;

            self = this;
            editor = self.codeMirror;

            // Check if track changes is on.
            // Note - even if track changes is off, there might be tracking marks already on the page,
            // so there is code later (in the else clause) for dealing with that.
            if (self.trackIsOn()) {

                // Get the cursor position because when we delete text
                cursorPosition = editor.getCursor('anchor');

                switch (changeObj.origin) {

                case '+delete':
                case 'cut':
                case 'brightspotCut':

                    // If we're deleting just a line just let it be deleted
                    // Because we don't have a good way to accept or reject a blank line
                    textOriginal = editor.getRange(changeObj.from, changeObj.to);
                    if (textOriginal === '\n') {
                        return;
                    }

                    // Determine if *every* character in the range is already marked as an insertion.
                    // In this case we can just delete the content and don't need to mark it as deleted.
                    if (self.inlineGetStyles(changeObj).trackInsert === true) {
                        return;
                    }

                    // Do not actually delete the text because we will mark it instead
                    changeObj.cancel();

                    // Move the cursor to where it was going if the text had been deleted
                    if (cursorPosition.line === changeObj.from.line && cursorPosition.ch === changeObj.from.ch) {
                        editor.setCursor(changeObj.to);
                    } else {
                        editor.setCursor(changeObj.from);
                    }

                    self.trackMarkDeleted({from: changeObj.from, to:changeObj.to});

                    break;

                case '+input':
                case 'paste':
                case 'brightspotPaste':

                    // Are we inserting at a cursor, or replacing a range?
                    if (changeObj.from.line === changeObj.to.line && changeObj.from.ch === changeObj.to.ch) {

                        // We are inserting new text at a cursor position,
                        // so we don't have to worry about replacing existing text

                        // If we are inserting just a line let it be inserted with no marking changes
                        // Because we don't have a good way to accept or reject a blank line
                        isEmpty = Boolean(changeObj.text.join('') === '');
                        if (isEmpty) {
                            return;
                        }

                        // Mark the range as inserted (before we let the insertion occur)
                        // Then when text is replaced it will already be in an area marked as new
                        if(!self.inlineGetStyles({from: changeObj.from, to:changeObj.to}).trackInsert){
                          self.inlineSetStyle('trackInsert', {from: changeObj.from, to:changeObj.to});
                        }

                        // In case we are inserting inside a deleted block,
                        // make sure the new text we are adding is not also marked as deleted
                        if(self.inlineGetStyles({from: changeObj.from, to:changeObj.to}).trackDelete){
                          self.inlineRemoveStyle('trackDelete', {from: changeObj.from, to:changeObj.to});
                        }

                        // Some text was pasted in and already marked as new,
                        // but we must remove any regions within that were previously marked deleted
                        if (changeObj.origin === 'paste') {
                          self.trackAfterPaste(changeObj.from, changeObj.to, changeObj.text);
                        }

                    } else {
                        // We are replacing existing text, so we need to handle cases where the text to be replaced
                        // already has things that we are tracking as deleted or inserted.

                        // Do not do the paste or insert
                        changeObj.cancel();

                        // this prevents rest of code from executing when text is empty
                        isEmpty = Boolean(changeObj.text.join('') === '');
                        if (isEmpty) {
                            return;
                        }

                        // Mark the whole range as "deleted" for track changes
                        // Note there might be some regions inside this that are marked as "inserted" but we'll deal with that below
                        if(!self.inlineGetStyles({from: changeObj.from, to:changeObj.to}).trackInsert && !self.inlineGetStyles({from: changeObj.from, to:changeObj.to}).trackDelete) {
                          self.trackMarkDeleted({from: changeObj.from, to:changeObj.to});
                        }

                        // // Insert the new text...
                        // // First remove the "delete" mark at the point where we are insering to make sure the new text is not also marked as deleted
                        if(self.inlineGetStyles({from: changeObj.from, to:changeObj.to}).trackDelete){
                          self.inlineRemoveStyle('trackDelete', {from: changeObj.from, to:changeObj.from});
                        }
                        // Then add a mark so the inserted text will be marked as an insert
                        if(!self.inlineGetStyles({from: changeObj.from, to:changeObj.to}).trackInsert){
                          self.inlineSetStyle('trackInsert', {from: changeObj.from, to:changeObj.from}, {inclusiveLeft:true});
                        }

                        // Finally insert the text at the starting point (before the other text in the range that was marked deleted)
                        // Note we add at the front because we're not sure if the end is valid because we might have removed some text
                        if (changeObj.origin !== 'paste') {

                            // TODO: what if the copied region has deleted text?
                            // Currently the entire content that is pasted will be marked as inserted text,
                            // but it could have deleted text within it.
                            // We need to remove that deleted text *after* the new content is pasted in.
                            editor.replaceRange(changeObj.text, changeObj.from, undefined, 'brightspotTrackInsert');
                            self.setCursor(changeObj.from.line, changeObj.from.ch+1)
                        }
                    }

                    break;

                case 'brightspotTrackInsert':

                    // Some text was pasted in and already marked as new,
                    // but we must remove any regions within that were previously marked deleted
                    self.trackAfterPaste(changeObj.from, changeObj.to, changeObj.text);

                    break;

                }

            } else {

                // Track change is NOT currently active
                // HOWEVER we must make sure any inserted text does not expand anything currently marked as a change
                // because the changes do not go away when you turn off

                classes = self.inlineGetStyles({from: changeObj.from, to: changeObj.to});
                if ('trackInsert' in classes || 'trackDelete' in classes) {

                    switch (changeObj.origin) {

                    case '+delete':
                    case 'cut':
                    case '+input':
                    case 'paste':

                        // Check if we are inserting text at a single point (rather than overwriting a range of text)
                        if (changeObj.from.line === changeObj.to.line && changeObj.from.ch === changeObj.to.ch) {

                            // In the case of inserting new content at a single point, if we are inside a tracked change,
                            // we need to ensure the new text is not also marked as an insertion or deletion.
                            // Before inserting the text, we add a single space and modify the change so it is
                            // dealing with a range of characters instead of an insertion point.
                            // This lets us remove the marks around the range.

                            // TODO: this seems to interfere with the undo history

                            editor.replaceRange(' ', changeObj.from, changeObj.to);
                            changeObj.update(changeObj.from, {line:changeObj.to.line, ch:changeObj.to.ch + 1});
                        }

                        self.inlineRemoveStyle('trackInsert', {from: changeObj.from, to: changeObj.to});
                        self.inlineRemoveStyle('trackDelete', {from: changeObj.from, to: changeObj.to});
                    }
                }
            }

            // After performing the change, clean up track changes marks
            // to remove them from styles that don't allow them (like comments)
            // after a timeout so the change event has time to complete first
            setTimeout(function(){
                self.trackAfterCleanup(changeObj);
            }, 1);
        },


        /**
         * Fix content after it has been pasted. When content is pasted the entire block is marked as a new change;
         * but if the content already contains text marked as deleted, then that deleted text should be removed.
         * Also, the "from" and "to" positions we get from the change object are positions *before* the change has been
         * made, so we must adjust the range based on the text that is added.
         *
         * @param [Object] from
         * @param [Object] to
         * @param [Array] textArray
         */
        trackAfterPaste: function(from, to, textArray) {

            var self;
            var toNew;

            self = this;

            // Figure out the new range based on the original range and the replacement text
            toNew = {
                line: from.line + textArray.length - 1,
                ch: textArray[ textArray.length - 1 ].length
            };
            if (toNew.line == from.line) {
                toNew.ch += from.ch;
            }

            // If not actually adding new content, do nothing (to prevent infinite loop in some cases)
            if (toNew.line === from.line && toNew.ch === from.ch) {
                return;
            }

            // Use a timeout so the change can be completed before we attempt to remove the deleted text
            setTimeout(function(){
                self.inlineRemoveStyle('trackDelete', {from:from, to:toNew}, {deleteText:true, triggerChange:false});
            }, 1);

        },


        /**
         * Clean up certain styles that don't allow track changes (like comments)
         * @param  {Object} range
         * The change object which contains the from/to range to be checked.
         */
        trackAfterCleanup: function(range) {

            var editor;
            var marks;
            var marksToClean;
            var self;

            self = this;
            editor = self.codeMirror;

            // Find all the marks within this range
            marks = editor.findMarks(range.from, range.to);
            marksToClean = [];

            // Determine which marks do not allow tracked changes
            $.each(marks, function(i, mark) {
                var markPos;
                var styleObj;
                if (mark.className && mark.find) {
                    styleObj = self.classes[mark.className];
                    if (styleObj && styleObj.trackChanges === false) {
                        marksToClean.push(mark);
                    }
                }
            });

            // Loop through the marks to clean, and remove track changes marks within
            $.each(marksToClean, function(i,mark) {
                var markPos;
                markPos = mark.find();
                self.inlineRemoveStyle('trackDelete', {from:markPos.from, to:markPos.to}, {deleteText:true, triggerChange:false});
                self.inlineRemoveStyle('trackInsert', {from:markPos.from, to:markPos.to}, {triggerChange:false});
            });
        },


        /**
         * For a given range, mark everything as deleted.
         * Also remove any previously inserted content within the range.
         */
        trackMarkDeleted: function(range) {

            var editor;
            var self;
            var textOriginal;
            var deleteTextFlag;

            self = this;
            editor = self.codeMirror;
            deleteTextFlag = self.inlineGetStyles(range).trackInsert || false

            // If we're deleting just a line just let it be deleted
            // Because we don't have a good way to accept or reject a blank line
            textOriginal = editor.getRange(range.from, range.to);
            if (textOriginal === '\n') {
                return;
            }

            if (range.from.line === range.to.line && range.from.ch === range.to.ch) {
                return;
            }

            // Determine if every character in the range is already marked as an insertion.
            // In this case we can just delete the content and don't need to mark it as deleted.
            if (self.inlineGetStyles(range).trackInsert !== true) {
              // if range has exising track Delete remove it before adding a new one to entire range.
              // prevents a selected deleted text from getting a double trackDelete class
                self.inlineRemoveStyle('trackDelete', range)
                self.inlineSetStyle('trackDelete', range);
            }

            // if trackInsert is on a line on its own the deleteTextFlag should be true
            // Remove any text within the range that is marked as inserted
            self.inlineRemoveStyle('trackInsert', range, {deleteText:deleteTextFlag});
        },


        /**
         * Accept all the marked changes within a range.
         */
        trackAcceptRange: function(range) {

            var editor;
            var self;

            self = this;
            editor = self.codeMirror;

            range = range || self.getRange();

            $.each(editor.findMarks(range.from, range.to), function(i, mark) {
                self.trackAcceptMark(mark);
            });

        },


        /**
         * Reject all the marked changes within a range.
         */
        trackRejectRange: function(range) {

            var editor;
            var self;

            self = this;
            editor = self.codeMirror;

            range = range || self.getRange();

            $.each(editor.findMarks(range.from, range.to), function(i, mark) {
                self.trackRejectMark(mark);
            });
        },


        /**
         * Accept a single marked change.
         */
        trackAcceptMark: function(mark) {

            var editor;
            var position;
            var self;

            self = this;
            editor = self.codeMirror;

            position = mark.find();

            if (position) {
                if (mark.className === self.styles.trackDelete.className) {
                    // For a delete mark, remove the content
                    mark.clear();
                    editor.replaceRange('', position.from, position.to);
                    self.triggerChange();
                } else if (mark.className === self.styles.trackInsert.className) {
                    // For an insert mark, leave the content and remove the mark
                    mark.clear();
                    self.triggerChange();
                }
            }
        },


        /**
         * Reject a single marked change.
         */
        trackRejectMark: function(mark) {

            var editor;
            var position;
            var self;

            self = this;
            editor = self.codeMirror;

            position = mark.find();

            if (position) {
                if (mark.className === self.styles.trackInsert.className) {
                    // For an insert mark, remove the content
                    mark.clear();
                    editor.replaceRange('', position.from, position.to);
                    self.triggerChange();
                } else if (mark.className === self.styles.trackDelete.className) {
                    // For a delete mark, leave the content and remove the mark
                    mark.clear();
                    self.triggerChange();
                }
            }

        },


        /**
         * Determine what to show in the editor:
         * The original text marked up with changes
         * Or the final text after changes have been applied
         *
         * @param Boolean showFinal
         * Set to true to show the original text with markup
         * Set to false to show the final text without markup.
         */
        trackDisplaySet: function(value) {

            var self;

            self = this;

            self.trackDisplay = value;

            self.trackDisplayUpdate();

        },


        /**
         * Toggle the trackDisplay setting (see trackDisplaySet).
         * Determines if we are showing the original text marked up with changes, or the final text after changes are applied.
         */
        trackDisplayToggle: function() {

            var self;

            self = this;

            self.trackDisplaySet( !self.trackDisplayGet() );
        },


        /*
         * Get the current value of the trackDisplay setting.
         * Determines if we are showing the original text marked up with changes, or the final text after changes are applied.
         */
        trackDisplayGet: function() {

            var self;

            self = this;

            return self.trackDisplay;
        },


        /**
         * Update the display to hide or show the markings based on the current value of trackDisplay.
         * Determines if we are showing the original text marked up with changes, or the final text after changes are applied.
         */
        trackDisplayUpdate: function() {

            var editor;
            var pos;
            var self;
            var $wrapper;

            self = this;
            editor = self.codeMirror;

            $wrapper = $(editor.getWrapperElement());

            // Set a class that can be used to style the inserted elements.
            // When this class is active the inserted elements can be style with a background color
            // but when the class is not active the inserted elements should not be styled
            // so they  will appear as in the final result
            $wrapper.toggleClass(self.styles.trackDisplay.className, self.trackDisplay);

            // Find all the marks for deleted elements
            $.each(editor.getAllMarks(), function(i, mark) {

                var styleObj;

                styleObj = self.classes[ mark.className ] || {};

                // Remove of the trackHideFinal marks that were previously set
                if (mark.className === self.styles.trackHideFinal.className) {
                    mark.clear();
                }

                // Check if this style should be hidden when in "Show Final" mode
                if (styleObj.showFinal === false && !self.trackDisplay) {

                    // Hide the deleted elements by creating a new mark that collapses (hides) the text

                    pos = mark.find();

                    editor.markText(pos.from, pos.to, {
                        className: self.styles.trackHideFinal.className,
                        collapsed: true
                    });
                }

            });
        },


        //==================================================
        // Clipboard
        //==================================================

        clipboardInit: function() {

            var editor;
            var isFirefox;
            var isWindows;
            var self;
            var $wrapper;

            self = this;

            editor = self.codeMirror;
            $wrapper = $(editor.getWrapperElement());

            // Set up copy event
            $wrapper.on('cut copy', function(e){
                self.clipboardCopy(e.originalEvent);
            });

            // Set up paste event
            // Note if using hte workaround below this will not fire on Ctrl-V paste
            $wrapper.on('paste', function(e){
                self.clipboardPaste(e.originalEvent);
            });

            // Cancel any pastes handled by CodeMirror
            editor.on('beforeChange', function(cm, change) {
                if (change.origin == 'paste') {
                    change.cancel();
                }
            });

            // Workaround for problem in Firefox clipboard not supporting styled content from Microsoft Word
            // Bug is described here: https://bugzilla.mozilla.org/show_bug.cgi?id=586587
            isFirefox = navigator.userAgent.toLowerCase().indexOf('firefox') > -1;
            isWindows = navigator.platform.toUpperCase().indexOf('WIN') > -1;
            if (isFirefox && isWindows) {

                self.clipboardUsePasteWorkaround = true;

                // Create a contenteditable div to be used for pasting data hack
                self.$clipboardDiv = $('<div/>', {'class':'rte2-clipboard'})
                    .attr('contenteditable', 'true')
                    .appendTo(self.$el)
                    .on('paste', function(e){
                        self.clipboardPaste(e.originalEvent);
                    });

                // If user presses Ctrl-v to paste, change the focus
                // to the contenteditable div we previously created,
                // so the pasted content will go there instead.
                // This is because contenteditable properly handles
                // the content that is pasted in from Microsoft Word
                // while the clipboard API does not have access to the text/html.
                $wrapper.on('keydown', function(e) {

                    if ((e.ctrlKey || e.metaKey) && e.keyCode == 86) {

                        // Problem with calling .focus() on an element is it scrolls the page!
                        // Save the scroll positions so we can scroll back to original position
                        self.clipboardX = window.scrollX;
                        self.clipboardY = window.scrollY;

                        self.$clipboardDiv.focus();

                    }
                });
            }
        },


        /**
         * Paste the contents of the clipboard into the currently selected region.
         * This can only be performed during a user-generated paste event!
         *
         * @param {Event} e
         * The paste event. This is required because you can only access the clipboardData from one of these events.
         */
        clipboardPaste: function(e) {

            var allowRaw;
            var isWorkaround;
            var self;
            var value;
            var valueHTML;
            var valueRTE;
            var valueText;

            self = this;

            // If we are using the workaround:
            // Check to see if the focus has been moved to the hidden contenteditable div
            // Normally this will happen when user types Ctrl-V to paste,
            // but not when user selects Paste from a menu
            isWorkaround = self.$clipboardDiv && self.$clipboardDiv.is(e.target);

            // Check if the browser supports clipboard API
            if (e && e.clipboardData && e.clipboardData.getData) {

                // See what type of data is on the clipboard:
                // data that was copied from the RTE? Or HTML or text data from elsewhere
                valueRTE = e.clipboardData.getData('text/brightspot-rte2');
                valueHTML = e.clipboardData.getData('text/html');
                valueText = e.clipboardData.getData('text/plain');

                if (valueRTE) {

                    // If we copied data from the RTE use is as-is
                    value = valueRTE;

                } else if (valueHTML) {

                    // For HTML copied from outside sources, clean it up first
                    value = self.clipboardSanitize(valueHTML);

                    // If we got data from outside the RTE, then don't allow raw HTML,
                    // instead strip out any HTML elements we don't understand
                    allowRaw = false;

                } else if (valueText && !isWorkaround) {

                    // If the clipboard only contains text, encode any special characters, and add line breaks.
                    // Note because of the hidden div hack, if we get text/plain in the clipboard
                    // we normally won't use it because we have to assume that there was some html that was missed.
                    // So this will only be reached if the user selects Paste from a menu using the mouse
                    // and CodeMirror handles the paste operation.

                    value = self.htmlEncode(valueText).replace(/[\n\r]/g, '<br/>');
                }
            }

            // Check if we were able to get a value from the clipboard API
            if (value) {

                self.fromHTML(value, self.getRange(), allowRaw);
                if (isWorkaround) {
                    self.focus();
                }
                e.stopPropagation();
                e.preventDefault();

            } else if (isWorkaround) {

                // We didn't find an HTML value from the clipboard.
                // If the user is on Windows and was pasting from Microsoft Word
                // we can try an alternate method of retrieving the HTML.

                // When the user typed Ctrl-V we should have previously intercepted it,
                // so the focus should be on the hidden contenteditable div.
                // However, if the user selected Paste from a menu the focus will not
                // be on the div :-(

                // We will let the paste event continue
                // and the content should be pasted into our hidden
                // contenteditable div.

                // Then after a timeout to allow the paste to complete,
                // we will get the HTML content from the contenteditable div
                // and copy it into the editor.

                self.$clipboardDiv.empty();

                setTimeout(function(){

                    // Get the content that was pasted into the hidden div
                    value = self.$clipboardDiv.html();
                    self.$clipboardDiv.empty();

                    // Clean up the pasted HTML
                    value = self.clipboardSanitize(value);

                    // Add the cleaned HTML to the editor. Do not allow raw HTML.
                    self.fromHTML(value, self.getRange(), false);

                    // Since we changed focus to the hidden div before the paste operation,
                    // put focus back on the editor
                    self.focus();


                }, 1);
            }

            if (isWorkaround) {
                // Since setting focus() on the hidden div moves the page, scroll page back to original position.
                // We seem to need a long delay for this to work successfully, but hopefully this can be improved.
                setTimeout(function(){
                    window.scrollTo(self.clipboardX, self.clipboardY);
                }, 100);
            }

        },


        /**
         * @returns {DOM}
         * Returns a DOM structure for the new HTML.
         */
        clipboardSanitize: function(html) {

            var dom;
            var $el;
            var self;

            self = this;
            dom = self.htmlParse(html);
            $el = $(dom);

            // Check if the pasted content matches a particular content type from clipboardSanitizeTypes
            $.each(self.clipboardSanitizeTypes, function(typeName, typeConf) {
                var isType;
                if (typeConf.isType && typeConf.rules) {
                    isType = typeConf.isType($el, html);
                    if (isType) {
                        // The pasted content matches this type, so apply these rules
                        self.clipboardSanitizeApplyRules($el, typeConf.rules);
                    }
                }
            });

            self.clipboardSanitizeApplyRules($el, self.clipboardSanitizeRules);

            // Anything we replaced with "linebreak" should get an extra blank line after
            // unless it's the last child element within the parent (to prevent
            // extra line breaks within table cells for example).
            $el.find('[data-rte2-sanitize=linebreak]').not(':last-child').after('<br/>');

            // Run it through the clipboard sanitize function (if it exists)
            if (self.clipboardSanitizeFunction) {
                $el = self.clipboardSanitizeFunction($el);
            }

            // Now parse the DOM and convert it to it only contains the HTML we allow.
            // This will also go inside table cells to leave us with what we consider
            // valid HTML.
            html = self.limitHTML($el[0]);

            return html;
        },


        /**
         * Internal function to apply a set of sanitize rules to the content.
         * @param {jQuery} $content
         * @param {Object} rules
         */
        clipboardSanitizeApplyRules: function($content, rules) {

            $.each(rules, function(selector, style) {

                $content.find(selector).each(function(){
                    var $match;
                    var $replacement;

                    $match = $(this);

                    if ($.isFunction(style)) {
                        style($match);
                    } else {
                        $replacement = $('<span>', {'data-rte2-sanitize': style});
                        $replacement.append( $match.contents() );
                        if ($match[0].tagName.toLowerCase() === 'td'){
                            $match.append( $replacement );
                        }else {
                            $match.replaceWith( $replacement );
                        }
                    }
                });
            });
        },


        /**
         * @param {Event} e
         * The cut/copy event. This is required because you can only access the clipboardData from one of these events.
         *
         * @param {String} value
         * The HTML text to save in the clipboard.
         */
        clipboardCopy: function(e) {

            var editor;
            var html;
            var range;
            var self;
            var text;

            self = this;
            editor = self.codeMirror;

            range = self.getRange();
            text = editor.getRange(range.from, range.to);
            html = self.toHTML(range);

            if (e && e.clipboardData && e.clipboardData.setData) {

                e.clipboardData.setData('text/plain', text);
                e.clipboardData.setData('text/html', html);

                // We set the html using mime type text/brightspot-rte
                // so we can get it back from the clipboard without browser modification
                // (since browser tends to add a <meta> element to text/html)
                // since all copies and cuts are inserts we remove del and ins tags
                e.clipboardData.setData('text/brightspot-rte2', html.replace(/<\/?[del|ins]+(>|$)/g, ""));

                // Clear the cut area
                if (e.type === 'cut' && self.trackIsOn()) {
                    self.trackMarkDeleted(range);
                    // if track insert exist without any trackDelete delete the existing text when copied
                    return false;
                }
                if (e.type === 'cut') {
                    editor.replaceRange('', range.from, range.to);
                }

                // Don't let the actual cut/copy event occur
                // (or it will overwrite the clipboard)
                e.preventDefault();
            }
        },


        //==================================================
        // Spellcheck
        //==================================================

        spellcheckWordSeparator: '\\s!"#$%&\(\)*+,-./:;<=>?@\\[\\]\\\\^_`{|}~\u21b5',

        // Which chararacters make up a word?
        // This must account for unicode characters to support multiple locales!
        // Taken from here: http://stackoverflow.com/a/22075070/101157
        // U+0027 = apostrophe
        // U+2019 = right single quote
        spellcheckWordCharacters: /[\u0027\u2019\u0041-\u005A\u0061-\u007A\u00AA\u00B5\u00BA\u00C0-\u00D6\u00D8-\u00F6\u00F8-\u02C1\u02C6-\u02D1\u02E0-\u02E4\u02EC\u02EE\u0370-\u0374\u0376\u0377\u037A-\u037D\u0386\u0388-\u038A\u038C\u038E-\u03A1\u03A3-\u03F5\u03F7-\u0481\u048A-\u0527\u0531-\u0556\u0559\u0561-\u0587\u05D0-\u05EA\u05F0-\u05F2\u0620-\u064A\u066E\u066F\u0671-\u06D3\u06D5\u06E5\u06E6\u06EE\u06EF\u06FA-\u06FC\u06FF\u0710\u0712-\u072F\u074D-\u07A5\u07B1\u07CA-\u07EA\u07F4\u07F5\u07FA\u0800-\u0815\u081A\u0824\u0828\u0840-\u0858\u08A0\u08A2-\u08AC\u0904-\u0939\u093D\u0950\u0958-\u0961\u0971-\u0977\u0979-\u097F\u0985-\u098C\u098F\u0990\u0993-\u09A8\u09AA-\u09B0\u09B2\u09B6-\u09B9\u09BD\u09CE\u09DC\u09DD\u09DF-\u09E1\u09F0\u09F1\u0A05-\u0A0A\u0A0F\u0A10\u0A13-\u0A28\u0A2A-\u0A30\u0A32\u0A33\u0A35\u0A36\u0A38\u0A39\u0A59-\u0A5C\u0A5E\u0A72-\u0A74\u0A85-\u0A8D\u0A8F-\u0A91\u0A93-\u0AA8\u0AAA-\u0AB0\u0AB2\u0AB3\u0AB5-\u0AB9\u0ABD\u0AD0\u0AE0\u0AE1\u0B05-\u0B0C\u0B0F\u0B10\u0B13-\u0B28\u0B2A-\u0B30\u0B32\u0B33\u0B35-\u0B39\u0B3D\u0B5C\u0B5D\u0B5F-\u0B61\u0B71\u0B83\u0B85-\u0B8A\u0B8E-\u0B90\u0B92-\u0B95\u0B99\u0B9A\u0B9C\u0B9E\u0B9F\u0BA3\u0BA4\u0BA8-\u0BAA\u0BAE-\u0BB9\u0BD0\u0C05-\u0C0C\u0C0E-\u0C10\u0C12-\u0C28\u0C2A-\u0C33\u0C35-\u0C39\u0C3D\u0C58\u0C59\u0C60\u0C61\u0C85-\u0C8C\u0C8E-\u0C90\u0C92-\u0CA8\u0CAA-\u0CB3\u0CB5-\u0CB9\u0CBD\u0CDE\u0CE0\u0CE1\u0CF1\u0CF2\u0D05-\u0D0C\u0D0E-\u0D10\u0D12-\u0D3A\u0D3D\u0D4E\u0D60\u0D61\u0D7A-\u0D7F\u0D85-\u0D96\u0D9A-\u0DB1\u0DB3-\u0DBB\u0DBD\u0DC0-\u0DC6\u0E01-\u0E30\u0E32\u0E33\u0E40-\u0E46\u0E81\u0E82\u0E84\u0E87\u0E88\u0E8A\u0E8D\u0E94-\u0E97\u0E99-\u0E9F\u0EA1-\u0EA3\u0EA5\u0EA7\u0EAA\u0EAB\u0EAD-\u0EB0\u0EB2\u0EB3\u0EBD\u0EC0-\u0EC4\u0EC6\u0EDC-\u0EDF\u0F00\u0F40-\u0F47\u0F49-\u0F6C\u0F88-\u0F8C\u1000-\u102A\u103F\u1050-\u1055\u105A-\u105D\u1061\u1065\u1066\u106E-\u1070\u1075-\u1081\u108E\u10A0-\u10C5\u10C7\u10CD\u10D0-\u10FA\u10FC-\u1248\u124A-\u124D\u1250-\u1256\u1258\u125A-\u125D\u1260-\u1288\u128A-\u128D\u1290-\u12B0\u12B2-\u12B5\u12B8-\u12BE\u12C0\u12C2-\u12C5\u12C8-\u12D6\u12D8-\u1310\u1312-\u1315\u1318-\u135A\u1380-\u138F\u13A0-\u13F4\u1401-\u166C\u166F-\u167F\u1681-\u169A\u16A0-\u16EA\u1700-\u170C\u170E-\u1711\u1720-\u1731\u1740-\u1751\u1760-\u176C\u176E-\u1770\u1780-\u17B3\u17D7\u17DC\u1820-\u1877\u1880-\u18A8\u18AA\u18B0-\u18F5\u1900-\u191C\u1950-\u196D\u1970-\u1974\u1980-\u19AB\u19C1-\u19C7\u1A00-\u1A16\u1A20-\u1A54\u1AA7\u1B05-\u1B33\u1B45-\u1B4B\u1B83-\u1BA0\u1BAE\u1BAF\u1BBA-\u1BE5\u1C00-\u1C23\u1C4D-\u1C4F\u1C5A-\u1C7D\u1CE9-\u1CEC\u1CEE-\u1CF1\u1CF5\u1CF6\u1D00-\u1DBF\u1E00-\u1F15\u1F18-\u1F1D\u1F20-\u1F45\u1F48-\u1F4D\u1F50-\u1F57\u1F59\u1F5B\u1F5D\u1F5F-\u1F7D\u1F80-\u1FB4\u1FB6-\u1FBC\u1FBE\u1FC2-\u1FC4\u1FC6-\u1FCC\u1FD0-\u1FD3\u1FD6-\u1FDB\u1FE0-\u1FEC\u1FF2-\u1FF4\u1FF6-\u1FFC\u2071\u207F\u2090-\u209C\u2102\u2107\u210A-\u2113\u2115\u2119-\u211D\u2124\u2126\u2128\u212A-\u212D\u212F-\u2139\u213C-\u213F\u2145-\u2149\u214E\u2183\u2184\u2C00-\u2C2E\u2C30-\u2C5E\u2C60-\u2CE4\u2CEB-\u2CEE\u2CF2\u2CF3\u2D00-\u2D25\u2D27\u2D2D\u2D30-\u2D67\u2D6F\u2D80-\u2D96\u2DA0-\u2DA6\u2DA8-\u2DAE\u2DB0-\u2DB6\u2DB8-\u2DBE\u2DC0-\u2DC6\u2DC8-\u2DCE\u2DD0-\u2DD6\u2DD8-\u2DDE\u2E2F\u3005\u3006\u3031-\u3035\u303B\u303C\u3041-\u3096\u309D-\u309F\u30A1-\u30FA\u30FC-\u30FF\u3105-\u312D\u3131-\u318E\u31A0-\u31BA\u31F0-\u31FF\u3400-\u4DB5\u4E00-\u9FCC\uA000-\uA48C\uA4D0-\uA4FD\uA500-\uA60C\uA610-\uA61F\uA62A\uA62B\uA640-\uA66E\uA67F-\uA697\uA6A0-\uA6E5\uA717-\uA71F\uA722-\uA788\uA78B-\uA78E\uA790-\uA793\uA7A0-\uA7AA\uA7F8-\uA801\uA803-\uA805\uA807-\uA80A\uA80C-\uA822\uA840-\uA873\uA882-\uA8B3\uA8F2-\uA8F7\uA8FB\uA90A-\uA925\uA930-\uA946\uA960-\uA97C\uA984-\uA9B2\uA9CF\uAA00-\uAA28\uAA40-\uAA42\uAA44-\uAA4B\uAA60-\uAA76\uAA7A\uAA80-\uAAAF\uAAB1\uAAB5\uAAB6\uAAB9-\uAABD\uAAC0\uAAC2\uAADB-\uAADD\uAAE0-\uAAEA\uAAF2-\uAAF4\uAB01-\uAB06\uAB09-\uAB0E\uAB11-\uAB16\uAB20-\uAB26\uAB28-\uAB2E\uABC0-\uABE2\uAC00-\uD7A3\uD7B0-\uD7C6\uD7CB-\uD7FB\uF900-\uFA6D\uFA70-\uFAD9\uFB00-\uFB06\uFB13-\uFB17\uFB1D\uFB1F-\uFB28\uFB2A-\uFB36\uFB38-\uFB3C\uFB3E\uFB40\uFB41\uFB43\uFB44\uFB46-\uFBB1\uFBD3-\uFD3D\uFD50-\uFD8F\uFD92-\uFDC7\uFDF0-\uFDFB\uFE70-\uFE74\uFE76-\uFEFC\uFF21-\uFF3A\uFF41-\uFF5A\uFF66-\uFFBE\uFFC2-\uFFC7\uFFCA-\uFFCF\uFFD2-\uFFD7\uFFDA-\uFFDC]+/g,

        /**
         * Set up the spellchecker and run the first spellcheck.
         */
        spellcheckInit: function() {

            var self;

            self = this;

            // Run the first spellcheck
            self.spellcheckUpdate();

            // Update the spellcheck whenever a change is made (but not too often)
            self.$el.on('rteChange', $.debounce(1000, function(){
                self.spellcheckUpdate();
            }));

            // Catch right click events to show spelling suggestions
            $(self.codeMirror.getWrapperElement()).on('contextmenu', function(event) {
                if (self.spellcheckShow()) {
                    event.preventDefault();
                }
            });
        },


        /**
         * Check the text for spelling errors and mark them.
         *
         * @returns {Promise}
         * Returns a promise that can be used to check when the spellcheck has completed.
         */
        spellcheckUpdate: function() {

            var self;
            var text;
            var wordsArray;
            var wordsArrayUnique;
            var wordsRegexp;
            var wordsUnique;

            self = this;

            // Get the text for the document
            text = self.toText() || '';

            // Split into words
            wordsRegexp = self.spellcheckWordCharacters;
            wordsArray = text.match( wordsRegexp );

            if (!wordsArray) {
                self.spellcheckClear();
                return;
            }

            // Eliminate duplicate words (but keep mixed case so we can later find and replace the words)
            wordsUnique = {};
            wordsArrayUnique = [];
            $.each(wordsArray, function(i, word){
                if (!wordsUnique[word]) {
                    wordsArrayUnique.push(word);
                    wordsUnique[word] = true;
                }
            });

            // Get spell checker results
            return spellcheckAPI.lookup(wordsArrayUnique).done(function(results) {

                self.spellcheckClear();

                // Prevent CodeMirror from updating the DOM until we finish
                self.codeMirror.operation(function() {

                    $.each(wordsArrayUnique, function(i,word) {

                        var adjacent;
                        var ch;
                        var index;
                        var indexStart;
                        var line;
                        var range;
                        var result;
                        var split;
                        var wordLength;

                        wordLength = word.length;

                        // Check if we have replacements for this word
                        result = results[word];
                        if ($.isArray(result)) {

                            // Find the location of all occurances
                            indexStart = 0;
                            while ((index = text.indexOf(word, indexStart)) > -1) {

                                // Move the starting point so we can find another occurrance of this word
                                indexStart = index + wordLength;

                                // Make sure we're at a word boundary on both sides of the word
                                // so we don't mark a string in the middle of another word

                                if (index > 0) {
                                    adjacent = text.substr(index - 1, 1);
                                    if (adjacent.match(wordsRegexp)) {
                                        continue;
                                    }
                                }

                                if (index + wordLength < text.length) {
                                    adjacent = text.substr(index + wordLength, 1);
                                    if (adjacent.match(wordsRegexp)) {
                                        continue;
                                    }
                                }

                                // Figure out the line and character for this word
                                split = text.substring(0, index).split("\n");
                                line = split.length - 1;
                                ch = split[line].length;

                                range = {
                                    from: {line:line, ch:ch},
                                    to:{line:line, ch:ch + wordLength}
                                };

                                // Add a mark to indicate this is a misspelling
                                self.spellcheckMarkText(range, result);

                            }
                        }

                    }); // $.each()

                }); // codeMirror.operation()

            }).fail(function(status){

                // A problem occurred getting the spell check results
                self.spellcheckClear();

            });
        },


        /**
         * Create a CodeMirror mark for a misspelled word.
         * Also saves the spelling suggestions on the mark so they can be displayed to the user.
         *
         * @param {Object} range
         * A range object to specify the mis-spelled word. {from:{line:#, ch:#}, to:{line:#, ch:#}}
         *
         * @param {Array} result
         * Array of spelling suggestions for the mis-spelled word.
         */
        spellcheckMarkText: function(range, result) {

            var editor;
            var mark;
            var markOptions;
            var self;

            self = this;

            editor = self.codeMirror;

            markOptions = {
                className: 'rte2-style-spelling',
                inclusiveRight: false,
                inclusiveLeft: false,
                addToHistory: false,
                clearWhenEmpty: true
            };

            mark = editor.markText(range.from, range.to, markOptions);

            // Save the spelling suggestions on the mark so we can use later (?)
            mark.spelling = result;
        },


        /**
         * Remove all the spellcheck marks.
         */
        spellcheckClear: function() {

            var editor;
            var self;

            self = this;

            editor = self.codeMirror;

            // Do not update the DOM until done with all operations
            editor.operation(function(){

                // Loop through all the marks and remove the ones that were marked
                editor.getAllMarks().forEach(function(mark) {
                    if (mark.className === 'rte2-style-spelling') {
                        mark.clear();
                    }
                });
            });
        },


        /**
         * Show spelling suggestions.
         *
         * @param {Object} [range=current selection]
         * The range that is selected. If not provided uses the current selection.
         *
         * @returns {Boolean}
         * Returns true if a misspelling was found.
         * Returns false if no misspelling was found.
         * This can be used for example with the right click event, so you can cancel
         * the event if a misspelling is shown (to prevent the normal browser context menu from appearing)
         */
        spellcheckShow: function(range) {

            var editor;
            var marks;
            var pos;
            var self;
            var suggestions;

            self = this;

            editor = self.codeMirror;

            range = range || self.getRange();

            // Is there a spelling error at the current cursor position?
            marks = editor.findMarksAt(range.from);
            $.each(marks, function(i,mark) {
                if (mark.className === 'rte2-style-spelling') {
                    pos = mark.find();

                    // Get the spelling suggestions, which we previosly
                    suggestions = mark.spelling;
                    return false;
                }
            });

            if (!pos || !suggestions || !suggestions.length) {
                return false;
            }

            // If a range is selected (rather than a single cursor position),
            // it must exactly match the range of the mark or we won't show the popup
            if (range.from.line !== range.to.line || range.from.ch !== range.to.ch) {

                if (pos.from.line === range.from.line &&
                    pos.from.ch === range.from.ch &&
                    pos.to.line === range.to.line &&
                    pos.to.ch === range.to.ch) {

                    // The showHint() function does not work if there is a selection,
                    // so change the selection to a single cursor position at the beginning
                    // of the word.
                    editor.setCursor(pos.from);

                } else {

                    // The selection is beyond the misspelling, so don't show a hint
                    return false;
                }
            }

            editor.showHint({
                completeSingle: false, // don't automatically correct if there is only one suggestion
                completeOnSingleClick: true,
                hint: function(editor, options) {
                    return {
                        list: suggestions,
                        from: pos.from,
                        to: pos.to
                    };
                }
            });

            // Return true so we can cancel the context menu that normally
            // appears for the right mouse click
            return true;

        },

        //==================================================
        // Mode Functions
        // Switch between plain text and rich text modes
        //==================================================

        modeInit: function() {
            var self = this;
            self.mode = 'rich';
        },


        /**
         * Returns the current mode.
         * @returns {String} 'plain' or 'rich'
         */
        modeGet: function() {
            var self = this;
            return self.mode === 'plain' ? 'plain' : 'rich';
        },


        modeToggle: function() {

            var self = this;
            var mode;

            mode = self.modeGet();

            if (mode === 'rich') {
                self.modeSetPlain();
            } else {
                self.modeSetRich();
            }
        },


        modeSetPlain: function() {
            var self = this;
            var editor = self.codeMirror;
            var $wrapper = $(editor.getWrapperElement());

            self.mode = 'plain';

            $wrapper.hide();

            if (self.$el.is('textarea')) {
                self.$el.show();
            }

            self.historyClear();

            // Trigger an event on the textarea to notify other code that the mode has been changed
            self.modeTriggerEvent();
        },


        modeSetRich: function() {
            var self = this;
            var editor = self.codeMirror;
            var $wrapper = $(editor.getWrapperElement());

            self.mode = 'rich';

            if (self.$el.is('textarea')) {
                self.$el.hide();
            }
            $wrapper.show();
            self.historyClear();

            // Trigger an event on the textarea to notify other code that the mode has been changed
            self.modeTriggerEvent();
        },


        /**
         * Trigger an event on the textarea to notify other code that the mode has been changed.
         */
        modeTriggerEvent: function() {
            var self = this;
            self.$el.trigger('rteModeChange', [self.modeGet()]);
        },

        //==================================================
        // Case Functions (lower case and uppper case)
        //
        // Note we can't just change the text directly in CodeMirror,
        // because that would obliterate the markers we use for styling.
        // So instead we copy the range as HTML, change the case of
        // the text nodes in the HTML, then paste the HTML back into
        // the same range.
        //==================================================

        /**
         * Toggle the case "smartly".
         * If the text is all uppercase, change to all lower case.
         * If the text is all lowercase, or a mix, then change to all uppercase.
         * @param {Object} [range=current range]
         */
        caseToggleSmart: function(range) {

            var editor;
            var self;
            var text;
            var textUpper;

            self = this;

            range = range || self.getRange();

            editor = self.codeMirror;

            // Get the text for the range
            text = editor.getRange(range.from, range.to) || '';
            textUpper = text.toUpperCase();

            if (text === textUpper) {
                return self.caseToLower(range);
            } else {
                return self.caseToUpper(range);
            }
        },


        /**
         * Change to lower case.
         * @param {Object} [range=current range]
         */
        caseToLower: function(range) {

            var self;

            self = this;

            self.caseChange(range, 'toLowerCase');
        },


        /**
         * Change to upper case.
         * @param {Object} [range=current range]
         */
        caseToUpper: function(range) {

            var self;

            self = this;

            self.caseChange(range, 'toUpperCase');
        },

        /**
         * Change to upper or lower case.
         *
         * @param {Object} [range=current range]
         *
         * @param {String} [direction=toUpperCase]
         * Can be 'toLowerCase', or 'toUpperCase'. Default is 'toUpperCase'
         *
         */
        caseChange: function(range, direction) {

            var chEnd;
            var chMax;
            var chStart;
            var editor;
            var html;
            var line;
            var lineRange;
            var lineText;
            var node;
            var self;

            self = this;
            editor = self.codeMirror;

            range = range || self.getRange();
            direction = (direction === 'toLowerCase') ? 'toLowerCase' : 'toUpperCase';

            // Loop through each line.
            // We need to change case one line at a time because of limitations in CodeMirror
            // (it wipes out line classes if we replace the text all at once)
            // So this will ensure things like enhancements and tables don't get wiped
            // out or duplicated, but other styles remain intact.
            for (line = range.from.line; line <= range.to.line; line++) {

                lineText = editor.getLine(line) || '';

                // How many characters in the current line?
                chMax = lineText.length;

                chStart = (line === range.from.line) ? range.from.ch : 0;
                chEnd = (line === range.to.line) ? range.to.ch: chMax;

                lineRange = {from:{line:line, ch:chStart}, to:{line:line, ch:chEnd}};

                // Get the HTML for the range
                html = self.toHTML(lineRange, {enhancements:false});

                // Convert the text nodes to lower case
                if (direction === 'toLowerCase') {
                    node = self.htmlToLowerCase(html);
                } else {
                    node = self.htmlToUpperCase(html);
                }

                // Save it back to the range as lower case text
                self.fromHTML(node, lineRange, true);
            }

            // Reset the selection range since it will be wiped out
            self.setSelection(range);

            return;
        },


        /**
         * Change the text nodes to lower case within some HTML.
         * @param {String|DOM} html
         */
        htmlToLowerCase: function(html) {
            var self;
            self = this;
            return self.htmlChangeCase(html, false);
        },


        /**
         * Change the text nodes to lower case within some HTML.
         * @param {String|DOM} html
         */
        htmlToUpperCase: function(html) {
            var self;
            self = this;
            return self.htmlChangeCase(html, true);
        },


        /**
         * Change the text nodes to lower or upper case within some HTML.
         * @param {String|DOM} html
         */
        htmlChangeCase: function(html, upper) {
            var node;
            var self;

            self = this;

            // Call recursive function to change all the text nodes
            node = self.htmlParse(html);
            if (node) {
                self.htmlChangeCaseProcessNode(node, upper);
            }
            return node;
        },

        /**
         * Recursive function to change case of text nodes.
         * @param {DOM} node
         * @param {Boolean} upper
         * Set to true for upper case, or false for lower case.
         */
        htmlChangeCaseProcessNode: function(node, upper) {
            var childNodes;
            var i;
            var length;
            var self;

            self = this;

            if (node.nodeType === 3) {
                if (node.nodeValue) {
                    node.nodeValue = upper ? node.nodeValue.toUpperCase() : node.nodeValue.toLowerCase();
                }
            } else {
                childNodes = node.childNodes;
                length = childNodes.length;
                for (i = 0; i < length; ++ i) {
                    self.htmlChangeCaseProcessNode(childNodes[i], upper);
                }
            }
        },

        // Other possibilities for the future?
        // caseToggle (toggle case of each character)
        // caseSentence (first word cap, others lower)
        // caseTitle (first letter of each word)


        //==================================================
        // Undo/Redo/History Functions
        // See also: https://github.com/ArthurClemens/Javascript-Undo-Manager
        //==================================================

        /**
         * Initialize the history system for undo/redo.
         * This replaces the CodeMirror undo/redo with our own,
         * since CodeMirror doesn't save marks when deleting text.
         */
        historyInit: function() {

            var self, undo;
            self = this;

            // Create the undo manager object
            undo = self.undoManager = new UndoManager();

            // Save a limited amount of undo data
            undo.setLimit(1000);

            // Replace the CodeMirror undo and redo functions with our own
            self.codeMirror.undo = function(){
                self.historyUndo();
            };

            self.codeMirror.redo = function(){
                self.historyRedo();
            };

            // Set up a history queue so multiple changes can be batched together into
            // a single undo action
            self.historyQueue = [];

            // Trigger an event to fire whenever an undo or redo event occurs,
            // just in case someone wants to listen for it.
            // Pass "this" as an argument to the event listeners.
            undo.setCallback(function(){
                self.$el.trigger('rteHistory', [self]);
            });

            // Listen for CodeMirror change events and add to our history.
            self.codeMirror.on('beforeChange', function(instance, beforeChange) {
                // Check to see if this event was canceled, which sometime happens when trackChanges is on
                if (!beforeChange.canceled) {
                    self.historyHandleCodeMirrorEvent(beforeChange);
                }
            });
        },


        /**
         * Add to the history.
         *
         * Note if the user has performed undo actions, adding to the history
         * resets the "redo" counter, so you can no longer redo the actions that
         * were undone.
         *
         * @param {Object} data
         * @param {Function} data.undo
         * @param {Function} data.redo
         *
         * @example
         * rte.historyAdd({
         *   undo: function(){
         *     // do something to remove the edit
         *   },
         *   redo: function() {
         *     // do something to add the edit back in
         *   }
         * });
         */
        historyAdd: function(data) {
            var self;
            self = this;

            // Don't add to history if we're in the middle of an undo or redo
            if (self.historyIsExecuting()) {
                return;
            }

            // Set a timeout to add this entry to the history,
            // so it can be combined with other entries
            clearTimeout(self.historyQueueTimeout);
            self.historyQueue.push(data);
            self.historyQueueTimeout = setTimeout(function(){
                self.historyProcessQueue();
            }, 1250);

        },


        /**
         * Add all the queued events to the history immediately,
         * then clear the queued events list.
         */
        historyProcessQueue: function() {

            var queue, self;
            self = this;

            clearTimeout(self.historyQueueTimeout);

            if (self.historyQueue.length === 0) {
                return;
            }

            // Clone the historyQueue
            queue = self.historyQueue.slice(0);

            // Clear the historyQueue
            self.historyQueue = [];

            // Add to the history so all the queued changes will get undo/redo
            self.undoManager.add({
                undo: function(){
                    // Note: in javascript array reverse is in-place.
                    // So when we redo we'll have to reverse it again
                    // to get back the original order.
                    $.each(queue.reverse(), function(i,data) {
                        data.undo();
                    });
                },
                redo: function(){
                    // Note: in javascript array reverse is in-place
                    $.each(queue.reverse(), function(i,data) {
                        data.redo();
                    });
                }
            });
        },


        /**
         * Perform an undo.
         */
        historyUndo: function() {
            var self;
            self = this;

            // Make sure changes that are queued up are added to the history
            self.historyProcessQueue();

            // Tell CodeMirror to avoid updating the DOM until we are done
            self.codeMirror.operation(function(){
                self.historyExecuting = true;
                self.undoManager.undo();
                self.historyExecuting = false;
            });
        },


        /**
         * Perform a redo.
         */
        historyRedo: function() {
            var self;
            self = this;

            // Make sure changes that are queued up are added to the history
            self.historyProcessQueue();

            // Make sure changes that are queued up are added to the history
            self.codeMirror.operation(function(){
                self.historyExecuting = true;
                self.undoManager.redo();
                self.historyExecuting = false;
            });
        },


        /**
         * Clear the undo history.
         */
        historyClear: function() {
            var self;
            self = this;

            // Make sure changes that are queued up are added to the history
            self.historyProcessQueue();

            self.undoManager.clear();
        },


        /**
         * Determine if there are any entries in the undo history.
         */
        historyHasUndo: function() {
            var self;
            self = this;

            // Make sure changes that are queued up are added to the history
            self.historyProcessQueue();

            return self.undoManager.hasUndo();
        },


        /**
         * Determine if there are any entries in the undo history.
         */
        historyHasRedo: function() {
            var self;
            self = this;

            // Make sure changes that are queued up are added to the history
            self.historyProcessQueue();

            return self.undoManager.hasRedo();
        },


        /**
         * @returns Boolean
         * Value to tell if a history undo or redo action is currently executing.
         * Note this only works for synchronous code, if the undo/redo action causes
         * async code, this will not be accurate.
         */
        historyIsExecuting: function() {
            var self;
            self = this;
            return Boolean(self.historyExecuting);
        },


        /**
         * Handle the beforeChange event from CodeMirror to add to the undo history.
         * @param {Object} beforeChange
         * The beforeChange event from CodeMirror.
         */
        historyHandleCodeMirrorEvent: function(beforeChange) {

            var change, marks, marksAndMore, origin, self;

            self = this;

            // Ignore changes if we're currently performing an undo or redo operation
            if (self.historyIsExecuting()) {
                return;
            }

            // Check where this change came from
            origin = beforeChange.origin || '';

            // Since we handle the "paste" event within our own clipboard handler,
            // ignore the codemirror paste event. Instead that will come through
            // in a separate brightspotPaste event.
            if (origin == 'paste') {
                return false;
            }

            // Check for brightspot events
            if (origin.indexOf('brightspot') !== -1) {
                switch (origin) {
                    // Let certain operations be added to the history
                    case 'brightspotPaste':
                    case 'brightspotCut':
                    case 'brightspotTrackInsert':
                    break;

                    default:
                    // Prevent other brightspot changes from being added to the history
                    return;
                }
            }

            // Save a list of the marks that are defined in this range.
            // We also need to get the position of each mark, because if CodeMirror
            // removes the mark along with the removed text, then we won't
            // be able to find the original position of the mark anymore.
            marks = self.codeMirror.findMarks(beforeChange.from, beforeChange.to);
            marksAndMore = [];
            $.each(marks, function(i, mark) {

                var markAndMore, pos;

                // Skip spelling marks
                if (mark.className === 'rte2-style-spelling') {
                    return;
                }

                markAndMore = {
                    mark:mark
                };

                if (mark.find) {
                    pos = mark.find();
                    markAndMore.position = {
                        from: { line: pos.from.line, ch: pos.from.ch },
                        to: { line: pos.to.line, ch: pos.to.ch }
                    };
                } else {
                    // Skip if mark has been removed from the document
                    return;
                }

                // Skip empty marks
                if (pos.from.line == pos.to.line && pos.from.ch === pos.to.ch) {
                    return;
                }

                // Retain the options from the old mark
                markAndMore.options = self.historyGetOptions(mark);

                marksAndMore.push(markAndMore);
            });

            change = {
                origin: beforeChange.origin,
                from: beforeChange.from,
                to: beforeChange.to,
                text: beforeChange.text,
                removed: self.codeMirror.getRange(beforeChange.from, beforeChange.to).split('\n'),

                // Save the marks as part of the change object so we can recreate them on undo
                marks: marksAndMore
            };

            self.historyAdd({
                undo: function() {
                    self.historyUndoCodeMirrorChange(change);
                },
                redo: function() {
                    self.historyRedoCodeMirrorChange(change);
                }
            });
        },

        /**
         * Return a list of options that we want to keep from a mark.
         * To be used when re-creating the mark.
         * @param  {Object} mark CodeMirror mark
         * @return {Object} A set of key/value pairs.
         */
        historyGetOptions: function(mark) {
            var options;
            options = {};
            $.each(mark, function(prop, value) {
                switch (prop) {
                    // List of properties that should be copied/retained from the old mark
                    // and passed as options on the new mark
                case 'atomic':
                case 'attributes':
                case 'className':
                case 'clearWhenEmpty':
                case 'endStyle':
                case 'startStyle':
                case 'historyFind':
                case 'inclusiveRight':
                case 'inclusiveLeft':
                case 'triggerChange':
                    options[prop] = value;
                }
            });
            return options;
        },

        /**
         * Perform an undo action based on a CodeMirror change event that was stored in the history.
         *
         * Change event looks like something like this:
         * {
         *   "from":{"line":1,"ch":2}, // Coordinates before the change
         *   "to":{"line":1,"ch":5}, // Coordinates before the change
         *   "text":["f"], // Text to be added
         *   "removed":["sti"], // Text that will be removed
         *   "origin":"+input"
         * }
         */
        historyUndoCodeMirrorChange: function(change) {

            var from, self, to;

            self = this;

            // Reverse the change event so we put back what was previously there
            from = change.from;
            to = {
                line: from.line + change.text.length - 1,
                ch: from.ch + change.text[0].length
            };

            if (change.text.length > 1) {
                to.ch = change.text[ change.text.length - 1 ].length;
            }

            self.codeMirror.replaceRange(change.removed.join('\n'), from, to, 'brightspotUndo');

            // Now re-add the marks that were possibly removed
            self.historyCodeMirrorChangeMarks(change);
        },


        /**
         * Perform a "redo" action based on a CodeMirror change event that was stored in the history.
         *
         * Change event looks like something like this:
         * {
         *   "from":{"line":1,"ch":2}, // Coordinates before the change
         *   "to":{"line":1,"ch":5}, // Coordinates before the change
         *   "text":["f"], // Text to be added
         *   "removed":["sti"], // Text that will be removed
         *   "origin":"+input"
         * }
         */
        historyRedoCodeMirrorChange: function(change) {
            var editor, self;
            self = this;
            editor = self.codeMirror;

            // Replace the text
            editor.replaceRange(change.text.join('\n'), change.from, change.to, 'brightspotRedo');
        },


        /**
         * When doing an undo or redo, recreate marks in the changed area
         * in case they were modified.
         */
        historyCodeMirrorChangeMarks: function(change) {

            var editor, self;
            self = this;
            editor = self.codeMirror;

            // Re-add the marks that might have been removed due to an undo action
            if (change.marks && change.marks.length) {
                $.each(change.marks, function(i, markAndMore) {

                    var mark, markNew, options, position;

                    mark = markAndMore.mark;
                    position = markAndMore.position;
                    options = markAndMore.options;

                    // There is a chance that the mark that was saved in this
                    // change event was cleared and re-created in another change event.
                    // In that case, a pointer to the new mark was saved on the old mark.
                    // If we find that pointer, update this to the new one.
                    while (mark.markNew) {
                        mark = markAndMore.mark = mark.markNew;
                    }

                    // Clear the mark because we're going to recreate it.
                    mark.clear();

                    // Recreate a new mark at the previous position.
                    markNew = editor.markText(position.from, position.to, options);

                    // Save a pointer to the new mark on the old mark,
                    // in case other history events are still pointing to the old mark
                    mark.markNew = markNew;

                    // Update the mark that is saved with the history
                    markAndMore.mark = markNew;
                });
            }
        },


        /**
         * Remove a mark and add a history event that recreates the mark if the user performs an undo.
         * @param  {Object} mark A CodeMirror mark.
         */
        historyRemoveMark: function(mark) {
            var options;
            var pos;
            var self;
            self = this;

            // Save the position of this mark
            if (mark.find) {
                pos = mark.find();
            }
            if (!pos) {
                return;
            }

            // Save the attributes and options of this mark
            options = self.historyGetOptions(mark);

            // Set up a history entry to recreate the mark
            self.historyAdd({
                // Undo should recreate the mark
                undo: function(){
                    var markNew;
                    // Find the latest copy of this mark in case other history events have changed it
                    while (mark.markNew) {
                        mark = mark.markNew;
                    }
                    mark.clear();
                    markNew = self.codeMirror.markText(pos.from, pos.to, options);
                    mark.markNew = markNew;
                    mark = markNew;
                },
                // Redo should remove the mark again
                redo: function(){
                    // Find the latest copy of this mark in case other history events have changed it
                    while (mark.markNew) {
                        mark = mark.markNew;
                    }
                    mark.clear();
                }
            });

            // Remove the mark
            mark.clear();
        },


        /**
         * Create a mark and add a history event that removes the mark if the user performans an undo.
         * @param  {[type]} range   Range of characters for the mark.
         * @param  {[type]} options Options and attributes for the mark.
         * @return {Object} The mark that was created.
         */
        historyCreateMark: function(from, to, options) {
            var mark;
            var self;
            self = this;

            // Only add a history event if the mark surrounds some characters.
            if (!(from.line === to.line && from.ch === to.ch)) {

                self.historyAdd({
                    undo: function(){
                        // Find the latest copy of this mark in case other history events have changed it
                        while (mark.markNew) {
                            mark = mark.markNew;
                        }
                        mark.clear();
                    },
                    redo: function(){
                        var markNew;
                        // Find the latest copy of this mark in case other history events have changed it
                        while (mark.markNew) {
                            mark = mark.markNew;
                        }
                        mark.clear();
                        markNew = self.codeMirror.markText(from, to, options);
                        mark.markNew = markNew;
                        mark = markNew;
                    }
                });
            }

            mark = self.codeMirror.markText(from, to, options);
            return mark;
        },


        /**
         * Remove a line class and add a history event that recreates it if the user performans an undo.
         * @param  {Number} lineNumber
         * @param  {String} className
         */
        historyRemoveLineClass: function(lineNumber, className) {
            var editor;
            var self;
            self = this;
            editor = self.codeMirror;

            self.historyAdd({
                undo: function(){
                    editor.addLineClass(lineNumber, 'text', className);
                },
                redo: function(){
                    self.blockRemovePreviewForClass(className, lineNumber);
                    editor.removeLineClass(lineNumber, 'text', className);
                }
            });

            self.blockRemovePreviewForClass(className, lineNumber);
            editor.removeLineClass(lineNumber, 'text', className);
        },


        /**
         * Create a line class and add a history event that removes it if the user performans an undo.
         * @param  {Number} lineNumber
         * @param  {String} className
         */
        historyCreateLineClass: function(lineNumber, className) {
            var editor;
            var self;
            self = this;
            editor = self.codeMirror;

            self.historyAdd({
                undo: function(){
                    editor.removeLineClass(lineNumber, 'text', className);
                },
                redo: function(){
                    editor.addLineClass(lineNumber, 'text', className);
                }
            });

            editor.addLineClass(lineNumber, 'text', className);
        },


        //==================================================
        // Miscelaneous Functions
        //==================================================


        /**
         * Give focus to the editor
         */
        focus: function() {
            var self;
            self = this;
            self.codeMirror.focus();
            setTimeout(function(){
                self.dropdownCheckCursor();
            }, 200);
        },


        isLineBlank: function(lineNumber) {
            var editor;
            var self;
            var text;

            self = this;
            editor = self.codeMirror;

            text = editor.getLine(lineNumber) || '';

            return /^\s*$/.test(text);
        },


        /**
         * If the current line is blank, move to the next non-blank line.
         * This is used to ensure new enhancements are added to the start of a paragraph.
         */
        moveToNonBlank: function() {

            var editor;
            var line;
            var max;
            var self;

            self = this;
            editor = self.codeMirror;

            line = editor.getCursor().line;
            max = editor.lineCount();

            while (line < max && self.isLineBlank(line)) {
                line++;
            }

            editor.setCursor(line, 0);

            return line;
        },


        /**
         * Returns the character count of the editor.
         * Note this counts only the plain text, not including the HTML elements that will be in the final result.
         * @returns Number
         */
        getCount: function() {
            var self;
            self = this;
            return self.toText().length;
        },


        /**
         * Gets the currently selected range.
         *
         * @returns Object
         * An object with {from,to} values for the currently selected range.
         * If a range is not selected, returns {from:0,to:0}
         */
        getRange: function(){

            var from;
            var self;
            var to;

            self = this;

            from = self.codeMirror.getCursor('from');
            to = self.codeMirror.getCursor('to');

            return {
                from: {line:from.line, ch:from.ch},
                to: {line:to.line, ch:to.ch}
            };
        },

        /**
         * Sets the selection to a range.
         */
        setSelection: function(range){

            var editor;
            var self;

            self = this;

            editor = self.codeMirror;

            editor.setSelection(range.from, range.to);
        },


        /**
         * Returns a range that represents the entire document.
         *
         * @returns Object
         * An object with {from,to} values for the entire docuemnt.
         */
        getRangeAll: function(){

            var self;
            var totalLines;

            self = this;

            totalLines = self.codeMirror.lineCount();

            return {
                from: {line: 0, ch: 0},
                to: {line: totalLines - 1, ch: self.codeMirror.getLine(totalLines - 1).length}
            };
        },


        /**
         * Rework the styles object so it is indexed by the className,
         * to make looking up class names easier.
         *
         * @returns Object
         * The styles object, but rearranged so it is indexed by the classname.
         * For each parameter in the object, a "key" parameter is added so you can
         * still determine the original key into the styles object.
         *
         * For example, if the styles object originally contains the following:
         * { 'bold': { className: 'rte2-style-bold' } }
         *
         * Then this function returns the following:
         * { 'rte2-style-bold': { key: 'bold', className: 'rte2-style-bold' } }
         */
        getClassNameMap: function() {

            var map;
            var self;

            self = this;

            map = {};

            $.each(self.styles, function(key, styleObj) {
                var className;
                className = styleObj.className;
                styleObj.key = key;
                map[ className ] = styleObj;
            });

            return map;
        },


        /**
         * Rework the styles object so it is indexed by the element,
         * to make importing elements easier.
         *
         * @returns Object
         * The styles object, but rearranged so it is indexed by the element name.
         * For each parameter in the object, a "key" parameter is added so you can
         * still determine the original key into the styles object.
         *
         * Since a single element might map to more than one style depending on the
         * attributes of the element, we use an array to hold the styles that might
         * map to that element.
         *
         * For example, if the styles object originally contains the following:
         * { 'bold': { className: 'rte2-style-bold', element:'b' } }
         *
         * Then this function returns the following:
         * { 'b': [{ key: 'bold', className: 'rte2-style-bold', element:'b'}] }
         */
        getElementMap: function() {

            var map;
            var self;

            self = this;

            map = {};

            $.each(self.styles, function(key, styleObj) {
                var element;
                element = styleObj.element;
                styleObj.key = key;

                // Create array of styles to which this element might map
                map[element] = map[element] || [];
                map[element].push(styleObj);
            });

            return map;
        },


        /**
         * Tell the editor to update the display.
         * You should call this if you modify the size of any enhancement content
         * that is in the editor.
         */
        refresh: function() {
            var self;
            self = this;

            // Set up a debounced refresh so it is not called too often
            if (!self._refresh) {
                self._refresh = $.debounce(200, function(){
                    self.codeMirror.refresh();
                });
            }

            self._refresh();
        },


        /**
         * Empty the editor and clear all marks and enhancements.
         */
        empty: function() {
            var self;
            self = this;

            // Destroy all enhancements
            $.each(self.enhancementCache, function(i, mark) {
                self.enhancementRemove(mark);
            });

            // Kill any remaining marks
            self.codeMirror.swapDoc(CodeMirror.Doc(''));
        },


        setCursor: function(line, ch) {
            var self;
            self = this;
            self.codeMirror.setCursor(line, ch);
        },

        readOnlyGet: function() {
            var self;
            self = this;
            return self.codeMirror.isReadOnly();
        },

        readOnlySet: function(readOnly) {
            var self;
            self = this;
            self.codeMirror.setOption('readOnly', readOnly);
        },

        /**
         * Returns the range for a mark.
         * @returns {Object}
         * The range of the mark (with from and to parameters)
         * or an empty object if the mark has been cleared.
         */
        markGetRange: function(mark) {
            var pos;

            pos = {};
            if (mark.find) {
                pos = mark.find() || {};
            }
            return pos;
        },


        /**
         * For a given range, returns the starting offset in pixels.
         *
         * @param {Object} range
         * @returns {Object}
         * Offset object {left, right, top, bottom }
         * Use left,top to represent the point below the first character.
         */
        getOffset: function(range) {
            var self;
            self = this;
            range = range || self.getRange();
            return self.codeMirror.charCoords({line:range.from.line, ch:range.from.ch});
        },


        /**
         * Given a CodeMirror mark, replace the text within it
         * without destroying the mark.
         * Normally if you were to use the CodeMirror functions to replace a range,
         * the mark would be destroyed.
         */
        replaceMarkText: function(mark, text) {
            var pos;
            var self;

            self = this;

            pos = self.markGetRange(mark);
            if (!pos.from) {
                return;
            }

            // Replacing the entire mark range will remove the mark so we need
            // to add text at the end of the mark, then remove the original text
            self.codeMirror.replaceRange(text, pos.to, pos.to);
            if (!(pos.from.line === pos.to.line && pos.from.ch === pos.to.ch)) {
                self.codeMirror.replaceRange('', pos.from, pos.to);
            }
        },

        /**
         * Given a CodeMirror mark, replace the text within it with HTML,
         * without destroying the mark. This is intended to be used for
         * inline enhancments where an external popup sets content in the editor.
         * It also sets up history so undo/redo actions will correctly
         * maintain the mark.
         *
         * @param  {[type]} mark [description]
         * @param  {[type]} html [description]
         * @return {[type]}      [description]
         */
        replaceMarkHTML: function(mark, html) {
            var clearWhenEmpty;
            var execute;
            var inclusiveLeft;
            var inclusiveRight;
            var markNew;
            var range;
            var reset;
            var self;
            self = this;

            // Remember the settings for inclusive left and inclusive right
            // so we can restore them later
            clearWhenEmpty = mark.clearWhenEmpty;
            inclusiveLeft = mark.inclusiveLeft;
            inclusiveRight = mark.inclusiveRight;

            // Remember the position of the mark because if the mark
            // gets removed we need to recreate it
            range = self.markGetRange(mark);

            reset = function() {
                while (mark.markNew) {
                    mark = mark.markNew;
                }
                mark.inclusiveLeft = inclusiveLeft;
                mark.inclusiveRight = inclusiveRight;
                mark.clearWhenEmpty = clearWhenEmpty;
            };

            execute = function() {

                // There is a chance that the mark that was saved in this
                // change event was cleared and re-created in another change event.
                // In that case, a pointer to the new mark was saved on the old mark.
                // If we find that pointer, update this to the new one.
                while (mark.markNew) {
                    mark = mark.markNew;
                }

                // If the mark was removed we must recreate it
                if (!mark.find || !mark.find()) {

                    // Recreate a new mark at the previous position.
                    markNew = self.codeMirror.markText(range.from, range.to, self.historyGetOptions(mark));

                    // Save a pointer to the new mark on the old mark,
                    // in case other history events are still pointing to the old mark
                    mark.markNew = markNew;

                    mark = markNew;
                }

                // Change the mark so it will expand when we insert content
                mark.inclusiveLeft = true;
                mark.inclusiveRight = true;
                mark.clearWhenEmpty = false;

                // Replace the content of the mark
                if (!self.historyIsExecuting()) {
                    self.fromHTML(html, range, true, true);
                    self.blockMarkReadOnly(mark, range);
                    reset();
                }
            };

            self.historyAdd({
                undo: function() {
                    reset();
                },
                redo: function() {
                    // Re-execute the insertion
                    execute();
                }
            });

            // Execute the insertion now
            execute();

            // After the change is made, we need to reset the mark
            self.historyAdd({
                undo: function() {
                    // reset();
                },
                redo: function() {
                    reset();
                }
            });
        },


        /**
         * Set a property on a mark in a way that supports undo history.
         * @param  {Object} mark     A CodeMirror mark.
         * @param  {String} property Name of the property, such as "attributes"
         * @param  {String} value    Value for the property.
         */
        setMarkProperty: function(mark, property, value) {

            var self;
            var valueOriginal;
            self = this;

            valueOriginal = mark[property];
            mark[property] = value;

            self.historyAdd({
                undo: function() {
                    // If mark was updated by other history events get latest mark
                    while (mark.markNew) {
                        mark = mark.markNew;
                    }
                    mark[property] = valueOriginal;
                },
                redo: function() {
                    // If mark was updated by other history events get latest mark
                    while (mark.markNew) {
                        mark = mark.markNew;
                    }
                    mark[property] = value;
                }
            });
        },

        /**
         * Replace a range of text without affecting the style marks
         * next to or surrounding the range.
         *
         * When using the CodeMirror.replaceRange() function, if the
         * range is next to another mark, adding text to the editor
         * can have the unwanted side effect of expanding the mark.
         *
         * This function takes some steps to ensure that the marks
         * are not extended.
         */
        replaceRangeWithoutStyles: function(from, to, text) {

            var editor, origin, self, toSpace;

            self = this;
            editor = self.codeMirror;
            origin = 'brightspotReplaceRangeWithoutStyles';

            // Replace the range with a single space so we can use it to split
            // any styles around or next to that range
            editor.replaceRange(' ', from, to, origin);

            toSpace = {
                line: from.line,
                ch: from.ch + 1
            };

            // Remove styles from the single character.
            // This will split any marks that surround the range.
            self.removeStyles({
                from: from,
                to: toSpace
            });

            // Insert the text for the undo action after the space so it does not extend any other marks
            editor.replaceRange(text, toSpace, null, origin);

            // Now remove the space that we added
            editor.replaceRange('', from, toSpace, origin);
        },


        /**
         * Determine if an element is a "container" for another element.
         * For example, element "li" is contained within a "ul" or "ol" element.
         *
         * @param {String} elementName
         * The name of an element such as "ul"
         *
         * @return {Boolean}
         */
        elementIsContainer: function(elementName) {
            var isContainer;
            var self;

            self = this;
            isContainer = false;
            $.each(self.styles, function(styleKey, styleObj){
                if (elementName === styleObj.elementContainer) {
                    isContainer = true;
                    return false; // Stop looping because we found one
                }
            });
            return isContainer;
        },


        /**
         * Returns a keymap based on the styles definition.
         *
         * @return {Object}
         * Keymap for CodeMirror.
         */
        getKeys: function() {

            var keymap;
            var self;

            self = this;

            keymap = {};

            // Ignore Tab key so it can be used to move focus to the next field
            keymap['Tab'] = false;
            keymap['Shift-Tab'] = false;

            // If at the start of an indented line, backspace should remove the indent or the list style
            keymap['Backspace'] = function(cm){
                var indent;
                var listType;
                var range;
                range = self.getRange();
                indent = self.blockGetIndent(range.from.line);
                listType = self.blockGetListType(range.from.line);
                if (indent > 0 && range.from.ch === 0 && range.from.line === range.to.line && range.from.ch === range.to.ch) {
                    if (listType && indent === 1) {
                        self.blockRemoveStyle(listType, range);
                    }
                    self.blockDeltaIndent(range.from.line, -1);
                } else {
                    // Do a normal backspace operation
                    cm.execCommand('delCharBefore');
                }
            };


            // Alt-Right key should increase the indent of the line.
            keymap['Alt-Right'] = function(cm){
                var range;
                range = self.getRange();
                self.blockDeltaIndent(range.from.line, 1);
            };

            // Alt-Left key should decrease the indent of the line.
            keymap['Alt-Left'] = function(cm){
                var range;
                range = self.getRange();
                self.blockDeltaIndent(range.from.line, -1);
            };

            keymap['Shift-Enter'] = function (cm) {
                // Add a carriage-return symbol and style it as 'newline'
                // so it won't be confused with any user-inserted carriage return symbols
                self.insert('\u21b5', 'newline');
            };

            keymap['Ctrl-Space'] = function (cm) {
                self.spellcheckShow();
            };

            $.each(self.styles, function(styleKey, styleObj) {

                var keys;

                keys = styleObj.keymap;

                if (keys) {

                    if (!$.isArray(styleObj.keymap)) {
                        keys = [keys];
                    }

                    $.each(keys, function(i, keyName) {
                        keymap[keyName] = function (cm) {
                            var allowed;
                            var $button;

                            // Check the context to see if this style is allowed
                            allowed = self.checkContext(styleKey);
                            if (!allowed) {
                                return false;
                            }

                            $button = self.$el.closest('.rte2-wrapper').find('> .rte2-toolbar [data-rte-style="' + styleKey + '"]').eq(0);
                            if ($button.length > 0) {
                                $button.click();
                                return false;

                            } else {
                                return self.toggleStyle(styleKey);
                            }
                        };
                    });
                }
            });

            return keymap;
        },

        /**
         * Get plain text from codemirror.
         * @returns String
         */
        toText: function() {
            var self;
            self = this;
            return self.codeMirror.getValue();
        },


        /**
         * Get content from codemirror, analyze the marked up regions,
         * and convert to HTML.
         *
         * @param {Object} [range=entire document]
         * If this parameter is provided, it is a selection range within the documents.
         * Only the selected characters will be converted to HTML.
         *
         * @param {Object} [options]
         * Set of key/value pairs to specify how the HTML is created.
         *
         * @param {Boolean} [options.enhancements=true]
         * Include enhancements in the HTML. Set this to false if enhancements should be
         * excluded.
         *
         */
        toHTML: function(range, options) {

            /**
             * Create the opening HTML element for a given style object.
             *
             * @param {Object|Function} styleObj
             * A style object as defined in this.styles, or a function that will return the opening HTML element.
             * @param {String} styleObj.element
             * The element to create. For example: "EM"
             * @param {Object} [styleObj.elementAttr]
             * An object containing additional attributes to add to the element.
             * For example: {'style': 'font-weight:bold'}
             */
            function openElement(styleObj, attributes) {

                var html = '';

                attributes = attributes || {};

                if (styleObj.markToHTML) {
                    html = styleObj.markToHTML();
                } else if (styleObj.element) {

                    html = '<' + styleObj.element;

                    $.each($.extend({}, styleObj.elementAttr, styleObj.attributes, attributes), function(attr, value) {
                        html += ' ' + attr + '="' + self.htmlEncode(value) + '"';
                    });

                    // For void elements add a closing slash when closing, like <br/>
                    if (self.voidElements[ styleObj.element ]) {
                        html += '/';
                    }

                    html += '>';
                }

                return html;
            }

            var blockElementsToClose;
            var containerActive;
            var doc;
            var enhancementsByLine;
            var html;
            var lineNo;
            var rangeWasSpecified;
            var self;

            self = this;

            options = options || {};

            rangeWasSpecified = Boolean(range);
            range = range || self.getRangeAll();

            // Clean up any "raw html" areas so they do not allow styles inside
            // Removing this for now as it causes performance problems when there are many raw marks.
            // However, that means user might be able to mark up raw areas and produce invalid HTML.
            // self.rawCleanup();

            doc = self.codeMirror.getDoc();

            // List of block styles that are currently open.
            // We need this so we can continue a list on the next line.
            containerActive = [];

             // List of block elements that are currently open
            // We need this so we can close them all at the end of the line.
            blockElementsToClose = [];

            // Go through all enhancements and figure out which line number they are on
            //
            // But do not include enhancements if a range was specified and it's a single line
            // (this is a hack to handle changing case of the text, we need to convert each
            // individual line to HTML and copy it back in to the line, to avoid duplicating
            // enhancements).

            enhancementsByLine = {};
            if (!(rangeWasSpecified && range.from.line === range.to.line)) {

                for (lineNo = range.from.line; lineNo <= range.to.line; lineNo++) {
                    enhancementsByLine[lineNo] = self.enhancementGetFromLine(lineNo);
                }
            }

            // Start the HTML!
            html = '';

            // Loop through the content one line at a time
            doc.eachLine(function(line) {

                var annotationEnd;
                var annotationStart;
                var charInRange;
                var charNum;
                var containerClosed;
                var containerData;
                var containerOnLine;
                var htmlEndOfLine;
                var htmlStartOfLine;
                var i;
                var indentLevel;
                var inlineActive;
                var inlineActiveIndex;
                var inlineActiveIndexLast;
                var inlineElementsToClose;
                var isVoid;
                var lineClasses;
                var lineNo;
                var outputChar;
                var raw;
                var rawLastChar;

                lineNo = line.lineNo();

                htmlStartOfLine = '';
                htmlEndOfLine = '';

                // List of inline styles that are currently open.
                // We need this because if we close one element we will need to re-open all the elements.
                inlineActive = [];

                // List of inline elements that are currently open
                // (in the order they were opened so they can be closed in reverse order)
                inlineElementsToClose = [];

                indentLevel = self.blockGetIndent(lineNo);
                containerOnLine = false;

                // If lineNo is in range
                if (range.from.line <= lineNo && range.to.line >= lineNo) {

                    // Get any line classes and determine which kind of line we are on (bullet, etc)
                    // From CodeMirror, the textClass property will contain multiple line styles separated by space
                    // like 'rte2-style-ol rte2-style-align-left'
                    // We reverse the array because that seems to keep the same order of elements that was
                    // imported from HTML.
                    lineClasses = (line.textClass || '').split(' ').reverse().filter(function(value){
                        return value !== '';
                    });
                    $.each(lineClasses, function() {

                        var container;
                        var containerData;
                        var containerPrevious;
                        var i;
                        var lineStyleData;
                        var styleObj;

                        // From a line style (like "rte2-style-ul"), determine the style name it maps to (like "ul")
                        styleObj = self.classes[this] || {};

                        // Get the "container" element for this style (for example: ul or ol)
                        container = styleObj.elementContainer;
                        if (container) {

                            containerOnLine = true;

                            // There are several possibilities at this point:
                            // * We are not inside another list and we need to create a new list.
                            // * We are inside a list already and at the same indent level.
                            // * We are inside a list already, but the indent level is greater,
                            //   so we need to create a new nested list.
                            // * We are inside a list already, but the indent level is less,
                            //   so we need to close previous lists until we get to the correct indent.

                            // Check if we are inside a list already, but the indent level is less,
                            // so we need to close previous lists until we get to the correct indent.
                            for (i = containerActive.length; i--; ) {

                                containerData = containerActive[i];

                                if (indentLevel < containerData.indentLevel) {

                                    // Now close the previous list that was at a higher indent
                                    html += '</' + containerData.styleObj.element + '>';
                                    html += '</' + containerData.styleObj.elementContainer + '>';
                                    containerActive.splice(i,1); // remove this container from the array

                                } else if (indentLevel === containerData.indentLevel &&
                                    container !== containerData.styleObj.elementContainer) {

                                    // Special case:
                                    // We're at the same indent level, but a different container
                                    // element (like a UL followed by an OL list).
                                    // In that case we must close the previous list, and open
                                    // a new list.
                                    html += '</' + containerData.styleObj.element + '>';
                                    html += '</' + containerData.styleObj.elementContainer + '>';

                                    // Remove this container from the array
                                    containerActive.splice(i,1);

                                     // Do not continue up the list of active containers
                                    break;

                                } else {

                                    // We're either at the same indent level with the same container,
                                    // or at a greater indent level.
                                    // Do not continue up the list of active containers.
                                    break;
                                }
                            }

                            // Check to see if we are already inside this container element and at the same indent level
                            containerPrevious = containerActive[ containerActive.length - 1 ];
                            if (containerPrevious &&
                                container === containerPrevious.styleObj.elementContainer  &&
                                indentLevel === containerPrevious.indentLevel) {

                                // We are continuting the container from the previous line
                                // so we don't need to open the container element.
                                // But we must close any existing block elements from the previous line,
                                // such as the <LI> element from the last list item,
                                // because we will be opening up a new <LI> element for this line.
                                html += '</' + containerPrevious.styleObj.element + '>';

                            } else {

                                // We are not inside the same container element,
                                // or we are at a different indent level

                                // We are not already inside this style, so create the container element
                                // and remember that we created it
                                containerActive.push({
                                    styleObj: styleObj,
                                    indentLevel:indentLevel
                                });
                                htmlStartOfLine += '<' + container + '>';
                            }

                        } else { // this style does not have a container

                            // Push this style onto a stack so when we reach the end of the line we can close the element.
                            // Note the element will be opened later.
                            if (styleObj.element) {
                                blockElementsToClose.push(styleObj);
                            }
                        }

                        if (styleObj.element) {

                            // Get any attributes that might be defined for this line style
                            lineStyleData = self.blockGetLineData(styleObj.key, lineNo) || {};

                            // Now determine which element to create for the line.
                            // For example, if it is a list then we would create an 'LI' element.
                            htmlStartOfLine += openElement(styleObj, lineStyleData.attributes);
                        }
                    }); // .each(lineClasses)

                    // Now that we have gone through all the line classes,
                    // we should know if we are in a container on this line
                    if (!containerOnLine) {

                        // We are not inside a container, so we should close all open containers
                        // that are greater than the current indent level
                        if (containerActive.length) {
                            for (i = containerActive.length; i--; ) {
                                containerData = containerActive[i];
                                if (indentLevel < containerData.indentLevel) {
                                    containerClosed = true;
                                    html += '</' + containerData.styleObj.element + '>';
                                    html += '</' + containerData.styleObj.elementContainer + '>';
                                    containerActive.splice(i,1); // remove this container from the array
                                } else {
                                    break;
                                }
                            }
                            // If we didn't close one of the containing block elements,
                            // then we need to put a line break before this text, to separate it from the
                            // list text: <li>previous_text<br/>this_text
                            if (!containerClosed) {
                                html += '<br/>';
                            }
                        }
                    }
                } // if lineNo is in range

                // Determine if there are any enhancements on this line
                if (enhancementsByLine[lineNo] && options.enhancements !== false) {

                    $.each(enhancementsByLine[lineNo], function(i,mark) {

                        var enhancementHTML;

                        // Only include the enhancement if the first character of this line is within the selected range
                        charInRange = (lineNo >= range.from.line) && (lineNo <= range.to.line);
                        if (lineNo === range.from.line && range.from.ch > 0) {
                            charInRange = false;
                        }
                        if (!charInRange) {
                            return;
                        }

                        enhancementHTML = '';
                        if (mark.options.toHTML) {
                            enhancementHTML = mark.options.toHTML();
                        }

                        if (enhancementHTML) {
                            html += enhancementHTML;
                        }
                    });
                }

                // Now add the html for the beginning of the line.
                if (htmlStartOfLine) {
                    // Kill the last <br> since it's not needed before a block element
                    html = html.replace(/<br\/?>$/, '');
                    html += htmlStartOfLine;
                }

                // Get the start/end points of all the marks on this line
                // For these objects the key is the character number,
                // and the value is an array of class names. For example:
                // {'5': 'rte2-style-subscript'}

                annotationStart = {};
                annotationEnd = {};

                if (line.markedSpans) {

                    $.each(self.toHTMLSortSpans(line), function(key, markedSpan) {

                        var className;
                        var endCh;
                        var mark;
                        var startCh;
                        var styleObj;

                        // If the from character is null, that means the mark was carried over from the previous line,
                        // so treat that as starting at the first character.
                        startCh = (markedSpan.from === null) ? 0 : markedSpan.from;
                        // If the to character is null, that means the mark carries over to the next line,
                        // so treat that as ending at the last character.
                        endCh = (markedSpan.to === null) ? line.text.length : markedSpan.to;
                        className = markedSpan.marker.className;

                        // Skip markers that do not have a className.
                        // For example an inline enhancement might cause this.
                        if (!className) {
                            return;
                        }

                        // Skip markers that do not cover any characters
                        // For example if you go to the start of a line and click Bold,
                        // then do not enter test.
                        if (startCh === endCh) {
                            return;
                        }

                        styleObj = self.classes[className] || {};

                        // Skip any marker where we don't have an element mapping
                        if (!(styleObj.element || styleObj.raw)) {
                            return;
                        }

                        // Get the mark object because it might contain additional data we need.
                        // We will pass the mark to the 'toHTML" function for the style if that exists
                        mark = markedSpan.marker || {};

                        // Create an array of styles that start on this character
                        if (!annotationStart[startCh]) {
                            annotationStart[startCh] = [];
                        }

                        // Create an array of styles that end on this character
                        if (!annotationEnd[endCh]) {
                            annotationEnd[endCh] = [];
                        }

                        // If the style has a toHTML filter function, we must call it
                        if (styleObj.toHTML) {

                            // Create a new function that converts this style to HTML
                            // based on the additional content stored with the mark.
                            annotationStart[startCh].push( $.extend(
                                true, {}, styleObj, {
                                    markToHTML: function() {
                                        return styleObj.toHTML(mark);
                                    }
                                }
                            ));

                        } else {

                            // There is no custom toHTML function for this
                            // style, so we'll just use the style object,
                            // but we'll also include any attributes that
                            // were stored on the mark
                            annotationStart[startCh].push( $.extend(true, {}, styleObj, {attributes:mark.attributes}) );
                        }

                        // Add the element to the start and end annotations for this character
                        annotationEnd[endCh].push(styleObj);

                    }); // each markedSpan

                } // if markedSpans

                // Loop through each character in the line string.
                for (charNum = 0; charNum <= line.text.length; charNum++) {

                    charInRange = true;
                    if (lineNo === range.from.line && charNum < range.from.ch) {
                        charInRange = false;
                    }
                    if (lineNo === range.to.line && charNum > (range.to.ch - 1)) {
                        charInRange = false;
                    }
                    charInRange = charInRange && (lineNo >= range.from.line) && (lineNo <= range.to.line);

                    // Special case - first element in the range.
                    // If previous characters from before the range opened any elements, include them now.
                    // For example, if there is a set of italic characters and the range begins in the middle
                    // of italicized text, then we must start by displaying <I> element.
                    if (lineNo === range.from.line && charNum === range.from.ch) {

                            $.each(inlineActive, function(i, styleObj) {
                                if (!self.voidElements[ styleObj.element ]) {
                                    inlineElementsToClose.push(styleObj);
                                    html += openElement(styleObj);
                                }
                            });
                    }

                    // Do we need to end elements at this character?
                    // Check if there is an annotation ending on this character,
                    // or if we are at the end of our range we need to check all the remaining characters on the line.
                    if (annotationEnd[charNum] ||
                        ((lineNo === range.to.line) && (range.to.ch <= charNum))) {

                        // Find out which elements are no longer active
                        $.each(annotationEnd[charNum] || [], function(i, styleObj) {

                            var element;
                            var styleToClose;

                            // If any of the styles is "raw" mode, clear the raw flag
                            if (styleObj.raw) {
                                raw = false;
                            }

                            // If any of the styles end a void element, clear the void flag
                            if (styleObj.void) {
                                isVoid = false;
                            }

                            // Find and delete the last occurrance in inlineActive
                            inlineActiveIndex = -1;
                            for (i = 0; i < inlineActive.length; i++) {
                                if (inlineActive[i].key === styleObj.key) {
                                    inlineActiveIndex = i;
                                }
                            }
                            if (inlineActiveIndex > -1) {

                                // Remove the element from the array of active elements
                                inlineActive.splice(inlineActiveIndex, 1);

                                // Save this index so we can reopen any overlapping styles
                                // For example if the overlapping marks look like this:
                                // 1<b>23<i>45</b>67</i>890
                                // Then when we reach char 6, we need to close the <i> and <b>,
                                // but then we must reopen the <i>. So our final result will be:
                                // 1<b>23<i>45</i></b><i>67</i>890
                                inlineActiveIndexLast = inlineActiveIndex - 1;

                                // Close all the active elements in the reverse order they were created
                                // Only close the style that needs to be closed plus anything after it in the active list
                                while ((styleToClose = inlineElementsToClose.pop())) {

                                    element = styleToClose.element;
                                    if (element && !self.voidElements[element]) {
                                        html += '</' + element + '>';
                                    }

                                    // Stop when we get to the style we're looking for
                                    if (styleToClose.key === styleObj.key) {
                                        break;
                                    }
                                }
                            }
                        });

                        // Re-open elements that are still active if we are still in the range.
                        if (charInRange) {

                            $.each(inlineActive, function(i, styleObj) {

                                // Only re-open elements after the last element closed
                                if (i <= inlineActiveIndexLast) {
                                    return;
                                }

                                // If it's a void element (that doesn't require a closing element)
                                // there is no need to reopen it
                                if (!self.voidElements[ styleObj.element ]) {

                                    // Add the element to the list of elements that need to be closed later
                                    inlineElementsToClose.push(styleObj);

                                    // Re-open the element
                                    html += openElement(styleObj);
                                }
                            });
                        }

                    } // if annotationEnd

                    // Check if there are any elements that start on this character
                    // Note even if this character is not in our range, we still need
                    // to remember which elements have opened, in case our range has characters
                    // in the middle of an opened element.
                    if (annotationStart[charNum]) {

                        $.each(annotationStart[charNum], function(i, styleObj) {

                            // If any of the styles is "raw" mode, set a raw flag for later
                            if (styleObj.raw) {
                                raw = true;
                            }

                            // If any of the styles is "void", set a void flag for later
                            if (styleObj.void) {
                                isVoid = true;
                            }

                            // Save this element on the list of active elements
                            inlineActive.push(styleObj);

                            // Open the new element
                            if (charInRange) {

                                // Also push it on a stack so we can close elements in reverse order.
                                if (!self.voidElements[ styleObj.element ]) {
                                    inlineElementsToClose.push(styleObj);
                                }

                                html += openElement(styleObj);
                            }
                        });
                    } // if annotationStart

                    outputChar = line.text.charAt(charNum);

                    // In some cases (at end of line) output char might be empty
                    if (outputChar) {

                        // Carriage return character within raw region should be converted to an actual newline
                        if (outputChar === '\u21b5') {
                            outputChar = '\n';
                        }

                        if (raw) {


                            // Less-than character within raw region temporily changed to a fake entity,
                            // so we can find it and do other stuff later
                            if (self.rawAddDataAttribute && outputChar === '<') {
                                outputChar = '&raw_lt;';
                            }

                            // We need to remember if the last character is raw html because
                            // if it is we will not insert a <br> element at the end of the line
                            rawLastChar = true;
                        } else if (isVoid) {

                            // For void styles, characters within should be ignored
                            outputChar = '';

                        } else {

                            outputChar = self.htmlEncode(outputChar);

                            rawLastChar = false;
                        }

                        if (charInRange) {
                            html += outputChar;
                        }
                    } // if outputchar

                } // for char


                if (range.from.line <= lineNo && range.to.line >= lineNo) {

                    // If we reached end of line, close all the open block elements
                    if (blockElementsToClose.length) {

                        $.each(blockElementsToClose.reverse(), function() {
                            var element;
                            element = this.element;
                            if (element) {
                                html += '</' + element + '>';
                            }
                        });
                        blockElementsToClose = [];

                    } else if (charInRange && rawLastChar && !self.rawBr) {
                        html += '\n';
                    } else if (charInRange && containerActive.length === 0) {
                        // No block elements so add a line break
                        html += '<br/>';
                    }

                    // Add any content that needs to go after the line
                    // for example, enhancements that are positioned below the line.
                    if (htmlEndOfLine) {
                        html += htmlEndOfLine;
                    }
                }

            });

            // When we finish with the final line close any block elements that are still open
            if (blockElementsToClose.length) {
                $.each(blockElementsToClose.reverse(), function() {
                    var element;
                    element = this.element;
                    if (element) {
                        html += '</' + element + '>';
                    }
                });
                blockElementsToClose = [];
            }
            $.each(containerActive, function(i, containerData) {
                if (containerData) {
                    html += '</' + containerData.styleObj.element + '>';
                    html += '</' + containerData.styleObj.elementContainer + '>';
                }
            });
            containerActive = [];

            // Find the raw "<" characters (which we previosly replaced with &raw_lt;)
            // and add a data-rte2-raw attribute to each HTML element.
            // This will ensure that when we re-import the HTML into the editor,
            // we will know which elements were marked as raw HTML.
            if (self.rawAddDataAttribute) {
                html = html.replace(/&raw_lt;(\w+)/g, '<$1 data-rte2-raw').replace(/&raw_lt;/g, '<');
            }

            return html;

        }, // toHTML


        /**
         * Sort CodeMirror spans in the order that elements should appear in the HTML output.
         *
         * The general logic goes like this:
         * Output elements in the order they are found.
         * If two elements start on the same character:
         * Check for context rules to see if a certain order is required
         * (like element A is allowed to be inside element B).
         * If no context rules apply, then apply the style in the reverse order
         * in which it was added to the content (like if bold was applied, then italic,
         * the output would be <i><b></b></i>.
         *
         * @param {Object] line
         * A CodeMirror line object.
         */
        toHTMLSortSpans: function(line) {

            var self;
            var spans;
            var spansByChar;

            self = this;

            // First reverse the order of the marks so the last applied will "wrap" any previous marks
            spans = line.markedSpans.slice(0).reverse();

            // Check to see if any of the spans is adjacent and needs to be combined
            spans = self.inlineCombineAdjacentMarks(spans);

            // Group the marks by starting character so we can tell if multiple marks start on the same character
            spansByChar = [];
            $.each(spans, function() {
                var char;
                var span;
                span = this;
                // If the from character is null, that means the mark was carried over from the previous line,
                // so treat that as starting at the first character.
                char = (span.from === null) ? 0 : span.from;
                spansByChar[char] = spansByChar[char] || [];
                spansByChar[char].push(span);
            });

            // Bubble sort the marks for each character based on the context
            $.each(spansByChar, function(spanIndex, spans) {

                var compare;
                var swapped;
                var temp;

                if (spans && spans.length > 1) {
                    do {
                        swapped = false;
                        for (var i=0; i < spans.length-1; i++) {
                            compare = self.toHTMLSpanCompare(spans[i], spans[i+1]);
                            if (compare === 1) {
                                temp = spans[i];
                                spans[i] = spans[i+1];
                                spans[i+1] = temp;
                                swapped = true;
                            }
                        }
                    } while (swapped);
                }
            });

            // Merge spansByChar back into a single ordered array
            spans = [];
            $.each(spansByChar, function(char, charSpans){
                if (charSpans) {
                    $.each(charSpans, function(i, span){
                        spans.push(span);
                    });
                }
            });

            return spans;
        },

        /**
         * Function that compares the context of two spans.
         * @returns {Boolean}
         * 0 if the order should not be changed
         * -1 if a should come first
         * 1 if b should come first
         */
        toHTMLSpanCompare: function(a, b) {

            var classA;
            var classB;
            var markerA;
            var markerB;
            var outsideA;
            var outsideB;
            var self;
            var styleA;
            var styleB;

            self = this;

            markerA = a.marker;
            markerB = b.marker;

            if (!(markerA && markerB)) {
                return 0;
            }

            classA = markerA.className;
            classB = markerB.className;

            styleA = self.classes[classA];
            styleB = self.classes[classB];

            if (!(styleA && styleB)) {
                return 0;
            }

            // Check if styleA is allowed to be inside styleB
            if (styleA.context) {
                if ($.inArray(styleB.element, styleA.context) !== -1) {
                    // a is allowed inside b
                    outsideB = true;
                }
            }

            // Check if styleB is allowed to be inside styleA
            if (styleB.context) {
                if ($.inArray(styleA.element, styleB.context) !== -1) {
                    // b is allowed inside a
                    outsideA = true;
                }
            }

            // Determine which style should be first
            if (outsideA && outsideB) {
                // Both are allowed inside the other, so do not change order
                return 0;
            } else if (outsideA) {
                // B is allowed inside A
                return -1;
            } else if (outsideB) {
                // A is allowed inside B
                return 1;
            } else {
                // No context or neither allowed inside the other, so do not change order
                return 0;
            }

        },


        /**
         * Import HTML content into the editor.
         *
         * @param {String|jQuery} html
         * An html string or a jquery object that contains the HTML content.
         *
         * @param {Object} [range=complete document]
         * A selected range where the HTML should be inserted.
         *
         * @param {Boolean} [allowRaw=true]
         * If this is set explicitly to false, then any elements
         * that are not recognized will be ommited.
         * By default (or if this is set to true), elements that
         * are not recognized are output and marked as raw HTML.
         */
        fromHTML: function() {
            var args;
            var self;

            self = this;
            args = arguments;

            // For performance, tell CodeMirror not to update the DOM
            // until our fromHTML() has completed.
            self.codeMirror.operation(function(){
                self._fromHTML.apply(self, args);
            });
        },
        _fromHTML: function(html, range, allowRaw, retainStyles) {

            var annotations;
            var editor;
            var enhancements;
            var el;
            var history;
            var self;
            var val;

            self = this;

            editor = self.codeMirror;

            allowRaw = (allowRaw === false ? false : true);

            // Convert HTML into a DOM element so we can parse it using the browser node functions
            el = self.htmlParse(html);

            // Text for the editor
            val = '';

            // Inline and block markers
            annotations = [];
            enhancements = [];

            function processNode(n, rawParent, indentLevel, insideAnotherBlock) {

                var elementAttributes;
                var elementClose;
                var elementName;
                var from;
                var indentChildren;
                var isContainer;
                var matchStyleObj;
                var next;
                var raw;
                var rawChildren;
                var split;
                var text;
                var to;

                indentLevel = indentLevel || 0;

                next = n.childNodes[0];

                while (next) {

                    indentChildren = indentLevel;

                    elementAttributes = {};

                    // Check if we got a text node or an element
                    if (next.nodeType === 3) {

                        // We got a text node, just add it to the value
                        text = next.textContent;

                        // Remove "zero width space" character that the previous editor sometimes used
                        text = text.replace(/\u200b|\u8203/g, '');

                        if (allowRaw) {

                            // Convert newlines to a carriage return character and annotate it
                            text = text.replace(/[\n\r]/g, function(match, offset, string){

                                var from;
                                var split;
                                var to;

                                // Create an annotation to mark the newline so we can distinguish it
                                // from any other user of the carriage return character

                                split = val.split("\n");
                                from =  {
                                    line: split.length - 1,
                                    ch: split[split.length - 1].length + offset
                                };
                                to = {
                                    line: from.line,
                                    ch: from.ch + 1
                                };
                                annotations.push({
                                    styleObj: self.styles.newline,
                                    from:from,
                                    to:to
                                });

                                return '\u21b5';
                            });

                        } else {

                            // Convert multiple white space to single space
                            text = text.replace(/[\n\r]/g, ' ').replace(/\s+/g, ' ');

                            // If text node is not within an element remove leading and trailing spaces.
                            // For example, pasting content from Word has text nodes with whitespace
                            // between elements.
                            if ($(next.parentElement).is('body')) {
                                text = text.replace(/^\s*|\s*$/g, '');
                            }
                        }

                        val += text;

                        // Set indent level for this line.
                        if (indentLevel) {
                            split = val.split("\n");
                            from =  {
                                line: split.length - 1,
                                ch: split[split.length - 1].length
                            };
                            annotations.push({from:from, indent:indentLevel});
                        }

                    } else if (next.nodeType === 1) {

                        // We got an element
                        elementName = next.tagName.toLowerCase();
                        elementAttributes = self.getAttributes(next);

                        // Determine if we need to treat this element as raw based on previous elements
                        raw = false;

                        // Check if the parent element had something unusual where
                        // all the children should also be considered raw HTML.
                        // This is used for nested lists since we can't support that in the editor.
                        if (rawParent) {

                            raw = true;

                            // Make sure any other children elements are also treated as raw elements.
                            rawChildren = true;

                        } else {

                            // When the editor writes HTML, it might place a data-rte2-raw attribute onto
                            // each element that was marked as raw HTML.
                            // If we see this attribute on importing HTML, we will again treat it as raw HTML.
                            raw = $(next).is('[data-rte2-raw]');
                        }

                        if (!raw) {

                            // Determine if the element maps to one of our defined styles
                            matchStyleObj = self.getStyleForElement(next);

                            // Create new line if this is a block element and we are not on the first line
                            if (matchStyleObj && val.length) {

                                // Special case for lists (and nested lists)
                                if (matchStyleObj.elementContainer) {

                                    // Create a new line for the first list item but not if list item
                                    // is nested within another.
                                    // Note this is not ideal because we get away from the "elementContainer"
                                    // model, and are assuming we're dealing with LI elements.
                                    if ($(next).is(':first-child') && $(next).parents('li').length === 0) {
                                        val += '\n';
                                    }

                                    if (!val.match(/\n$/)) {
                                        val += '\n';
                                    }

                                } else if (matchStyleObj.line && !insideAnotherBlock) {
                                    // If this is a block element add a new line
                                    // But not if there are multiple blocks defined on the same line,
                                    val += '\n';
                                }
                            }
                        }

                        // Figure out which line and character for the start of our element
                        split = val.split("\n");
                        from =  {
                            line: split.length - 1,
                            ch: split[split.length - 1].length
                        };

                        // Special case - is this an enhancement?
                        // Note we are treating tables as an enhancement as well.
                        if ((elementName === 'table') || ((elementName === 'span' || elementName === 'button') && $(next).hasClass('enhancement'))) {

                            // End the last line if necessary
                            // (and avoid adding a newline if this is the first line in the editor)
                            if (val.length && val[ val.length - 1] !== '\n') {
                                val += '\n';
                                from.line++;
                                from.ch = 0;
                            }

                            enhancements.push({
                                line: from.line,
                                $content: $(next)
                            });

                            // Skip past the enhancement
                            next = next.nextSibling;
                            continue;
                        }

                        // For container elements such as "ul" or "ol", we allow nesting,
                        // so increment the indent level, and start a new line.
                        isContainer = self.elementIsContainer(elementName);
                        if (isContainer) {

                            // If inside a nested list, make sure we start nested list on a new line.
                            // This will account for something like this:
                            // <ul>
                            //   <li>
                            //     some content
                            //     <ul><li>nested list needs a line break before</li></ul>
                            //   </li>
                            // </ul>
                            if (indentLevel && !val.match(/\n$/)) {
                                val += '\n';
                            }

                            // Increase the indent for anything within this container
                            indentChildren = indentLevel + 1;
                        }

                        // If we are inside a container element, start a new line if we encounter
                        // a line style (that is not the actual list element)
                        if (indentLevel && matchStyleObj && matchStyleObj.line && !matchStyleObj.elementContainer) {
                            val += '\n';
                        }

                        // Do we need to keep this element as raw HTML?
                        // Check if we have not yet matched this element
                        // Check if this is not a BR element.
                        // Check if this element is a "container" element such as a "ul" that contains an "li" element.
                        if (!matchStyleObj && !isContainer && elementName !== 'br') {
                            raw = true;
                        }

                        if (elementName === 'br') {
                            raw = false;
                        }

                        if (raw && allowRaw) {

                            matchStyleObj = self.styles.html;

                            val += '<' + elementName;

                            $.each(next.attributes, function(i, attrib){

                                var attributeName = attrib.name;
                                var attributeValue = attrib.value;

                                // Skip the data-rte2-raw attribute since that is used only to
                                // indicate which elements were previously marked as raw html
                                if (attributeName === 'data-rte2-raw') {
                                    return;
                                }

                                val += ' ' + attributeName + '="' + self.htmlEncode(attributeValue) + '"';
                            });

                            // Close void elements like <input/>
                            if (self.voidElements[ elementName ]) {
                                val += '/';
                            }

                            val += '>';

                            // End the mark for raw HTML
                            split = val.split("\n");
                            to =  {
                                line: split.length - 1,
                                ch: split[split.length - 1].length
                            };
                            annotations.push({
                                styleObj:matchStyleObj,
                                from:from,
                                to:to,
                                indent: indentLevel
                            });

                            // Remember we need to close the element later
                            if (!self.voidElements[ elementName ]) {
                                elementClose = '</' + elementName + '>';
                            }
                        }

                        // Recursively go into our element and add more text to the value.
                        // If we are already inside a block element, pass a flag so we don't add extra newlines.
                        processNode(next, rawChildren, indentChildren, (matchStyleObj && matchStyleObj.line) || insideAnotherBlock);

                        if (elementClose) {

                            // Create a new starting point for raw html annotation
                            split = val.split("\n");
                            from =  {
                                line: split.length - 1,
                                ch: split[split.length - 1].length
                            };

                            // Add the closing element
                            val += elementClose;
                            elementClose = '';
                        }

                        // Now figure out the line and character for the end of our element
                        split = val.split("\n");
                        to =  {
                            line: split.length - 1,
                            ch: split[split.length - 1].length
                        };

                        if (matchStyleObj) {
                            // Check to see if there is a fromHTML function for this style
                            if (matchStyleObj.fromHTML) {

                                // Yes, there is a fromHTML function, so as part of this annotation we will create
                                // a function that reads information from the element and saves it on the mark.

                                // Since we're in a loop we can't rely on closure variables to maintain the
                                // current values, so we're using a special javascript trick to get around that.
                                // The with statement will create a new closure for each loop.
                                with ({matchStyleObj:matchStyleObj, next:next, from:from, to:to}) {

                                    annotations.push({
                                        styleObj: $.extend({}, matchStyleObj, {
                                            filterFromHTML: function(mark){
                                                matchStyleObj.fromHTML( $(next), mark );
                                            }
                                        }),
                                        from:from,
                                        to:to,
                                        indent: indentLevel
                                    });
                                }

                            } else {

                                annotations.push({
                                    styleObj:matchStyleObj,
                                    from:from,
                                    to:to,
                                    attributes: elementAttributes,
                                    indent: indentLevel
                                });
                            }
                        }

                        // Add a new line for certain elements (like <br>)
                        if (self.newLineRegExp.test(elementName)) {
                            val += '\n';
                        } else if (matchStyleObj && matchStyleObj.line) {

                            // Special cases:
                            // If we are already inside another block do not add a newline.
                            // If this is a list item, and there is already a newline at the end,
                            // then do not add another newline.
                            if (!insideAnotherBlock && !(matchStyleObj && matchStyleObj.elementContainer && val.match(/\n$/))) {
                                val += '\n';
                            }
                        }

                    } // else if this is an element...

                    next = next.nextSibling;

                } // while there is a next sibling...

            } // function processNode

            processNode(el);

            // Replace multiple newlines at the end with single newline
            // unless the last line contains an enhancement
            var enhancementLast = enhancements[ enhancements.length - 1 ];
            if (!(enhancementLast && enhancementLast.line >= val.split('\n').length - 1)) {
                val = val.replace(/[\n\r]+$/, '\n');
            }

            if (range) {

                // Remove the styles from the range so for example we don't paste content
                // into a bold region and make all the pasted content bold.

                // There seems to be a problem if the range is a single cursor position
                // and we can't remove the styles for that.
                // So instead we'll insert a space, then remove the styles from that space character,
                // then call an undo() to remove the space from the document (and the undo history).

                if (!retainStyles) {
                    if (range.from.line === range.to.line && range.from.ch === range.to.ch) {

                        // When adding the space so we can remove styles, prevent it from going in the undo history
                        // by using an origin containing "brightspot"
                        editor.replaceRange(' ', range.from, range.to, 'brightspotFromHTMLRemoveStyles');

                        // Remove styles from the single character
                        self.removeStyles({
                            from: { line:range.from.line, ch:range.from.ch },
                            to: { line:range.from.line, ch:range.from.ch + 1}
                        });

                        // Remove the space that we added
                        editor.replaceRange('', range.from, {line:range.to.line, ch:range.to.ch + 1}, 'brightspotFromHTMLRemoveStyles');

                    } else {

                        // Remove styles from the range
                        self.removeStyles(range);

                    }
                } else {
                    // Resolves issue where a link/linklist marker is added to empty rte and the state of the rte freezes
                    // below a space is added to the marker 
                    // and then the styles are cleared from the space above
                    if (range.from.line === 0 && range.to.ch > 0){
                        var replaceRange = { line:range.to.line, ch:range.to.ch + 1 }
                        editor.replaceRange(' ', replaceRange, replaceRange, null);
                        self.removeStyles( {
                            from: replaceRange,
                            to: replaceRange
                        });
                    }
                }

            } else {

                // Replace the entire document
                self.empty();

                // Set the range at the beginning of the document
                range = {
                    from: { line:0, ch:0 },
                    to:{ line:0, ch:0 }
                };
            }

            // Add the plain text to the end of the selected range
            // Then remove the original text that was already there
            // We're using the origins 'brightspotPaste' and 'brightspotCut'
            // here to ensure that if track changes is on, the changes are registered.
            editor.replaceRange(val, range.to, range.to, 'brightspotPaste');
            editor.replaceRange('', range.from, range.to, 'brightspotCut');

            // Before we start adding styles, save the current history.
            // After we add the styles we will restore the history.
            // This will prevent lots of undo history being added,
            // so user can undo this new content all in one shot.
            history = editor.getHistory();

            // Set up all the annotations
            // We reverse the order of the annotations because the parsing was done
            // depth first, and we want to create the marks for parent elements before
            // the marks for child elements (so elements can later be created in the same order)
            $.each(annotations, function(i, annotation) {

                var annotationRange;
                var styleObj;

                styleObj = annotation.styleObj;

                // Adjust the position of the annotation based on the range where we're inserting the text
                if (range.from.line !== 0 || range.from.ch !== 0) {

                    // Only if the annotation is on the first line of new content,
                    // we should adjust the starting character in case the selected range
                    // does not start at character zero.
                    // For annotations on subsequent lines we don't need to adjust the starting character.
                    if (annotation.from.line === 0) {

                        annotation.from.ch += range.from.ch;

                        // If the annotation also ends on this line, adjust the ending character.
                        // For annotations on subsequent lines we don't need to adjust the ending character
                        // because a new line will have been created.

                        if (annotation.to.line === 0) {
                            annotation.to.ch += range.from.ch;
                        }
                    }

                    // Since we are replacing a range that is not at the start
                    // of the document, the lines for all annotations should be adjusted
                    annotation.from.line += range.from.line;
                    annotation.to.line += range.from.line;

                }
                if (styleObj) {
                    if (styleObj.line) {
                        // If this is a list item, then only set the style on the first line
                        annotationRange = {
                            from: annotation.from,
                            to: annotation.to
                        };
                        if (styleObj.elementContainer) {
                            annotationRange.to = annotation.from;
                        }
                        self.blockSetStyle(styleObj, annotationRange, {triggerChange:false, attributes:annotation.attributes});

                        if (annotation.indent) {
                            self.blockSetIndent(annotationRange, annotation.indent);
                        }

                    } else {
                        self.inlineSetStyle(styleObj, annotation, {addToHistory:false, triggerChange:false, attributes:annotation.attributes});
                    }
                } else if (annotation.indent) {
                    // Special case for extra lines within a list item,
                    // we save an annotation that contains only the line and indent.
                    self.blockSetIndent(annotation.from.line, annotation.indent);
                }
            });

            $.each(enhancements, function(i, enhancementObj) {

                // Pass off control to a user-defined function for adding enhancements
                self.enhancementFromHTML(enhancementObj.$content, enhancementObj.line + range.from.line);

            });

            editor.setHistory(history);
            self.triggerChange();

        }, // fromHTML()


        /**
         * Given a DOM element, determine which style it matches.
         *
         * @param {DOM} el
         * @returns {Object|undefined}
         * The style object that matches the element, or undefined.
         */
        getStyleForElement: function(el) {

            var elementName;
            var map;
            var matchArray;
            var matchStyleObj;
            var self;

            self = this;

            // Convert the styles object to an object that is indexed by element,
            // so we can quickly map an element to a style.
            // Note there might be more than one style for an element, in which
            // case we will use attributes to determine if we have a match.
            // Cache it so we only create the map once.
            self.stylesMap = self.stylesMap || self.getElementMap();
            map = self.stylesMap;

            // We got an element
            elementName = el.tagName.toLowerCase();

            // Determine if the element maps to one of our defined styles
            matchStyleObj = undefined;

            // If a data-rte2-sanitize attribute is found on the element, then we are getting this
            // html as pasted data from another source. Our sanitize rules have marked this element
            // as being a particular style, so we should force that style to be used.
            matchStyleObj = self.styles[ $(el).attr('data-rte2-sanitize') ];
            $(el).removeAttr('data-rte2-sanitize');
            if (matchStyleObj) {
                return matchStyleObj;
            }

            // Multiple styles might map to a particular element name (like <b> vs <b class=foo>)
            // so first we get a list of styles that map just to this element name
            matchArray = map[elementName];
            if (matchArray) {

                $.each(matchArray, function(i, styleObj) {

                    var attributesFound;

                    // Detect blocks that have containers (like "li" must be contained by "ul")
                    if (styleObj.elementContainer && styleObj.elementContainer.toLowerCase() !== el.parentElement.tagName.toLowerCase()) {
                        return;
                    }

                    attributesFound = {};

                    // If the style has attributes listed we must check to see if they match this element
                    if (styleObj.elementAttr) {

                        // Loop through all the attributes in the style definition,
                        // and see if we get a match
                        $.each(styleObj.elementAttr, function(attr, expectedValue) {

                            var attributeValue;

                            attributeValue = $(el).attr(attr);

                            // Check if the element's attribute value matches what we are looking for,
                            // or if we're just expecting the attribute to exist (no matter the value)
                            if ((attributeValue === expectedValue) ||
                                (expectedValue === true && attributeValue !== undefined)) {

                                // We got a match!
                                // But if there is more than one attribute listed,
                                // we keep looping and all of them must match!
                                attributesFound[attr] = true;
                                matchStyleObj = styleObj;

                            } else {

                                // The attribute did not match so we do not have a match.
                                matchStyleObj = false;

                                // Stop looping through the rest of the attributes.
                                return false; // break out of $.each()
                            }
                        });


                    } else {

                        // There were no attributes specified for this style so we might have a match just on the element
                        matchStyleObj = styleObj;
                    }

                    // Stop after the first style that matches
                    if (matchStyleObj) {
                        return false; // break out of $.each()
                    }

                }); // each matchArray...

            } // if (matchArray)

            return matchStyleObj;
        },


        /**
         * Fix the HTML from a paste operation so it only contains elements from our style definitions.
         *
         * @param {String|DOM} html
         * An HTML string or a DOM structure.
         *
         * @returns {String)
         */
        limitHTML: function(html) {

            var el;
            var self;
            var val;

            self = this;

            // Convert HTML into a DOM element so we can parse it using the browser node functions
            el = self.htmlParse(html);

            // Output HTML after fixing
            val = '';

            function processNode(n) {

                var elementClose;
                var elementName;
                var matchStyleObj;
                var next;
                var text;

                next = n.childNodes[0];

                while (next) {

                    elementClose = '';

                    // Check if we got a text node or an element
                    if (next.nodeType === 3) {

                        // We got a text node, just add it to the value
                        text = next.textContent;

                        // Convert multiple white space to single space
                        text = text.replace(/[\n\r]/g, ' ').replace(/\s+/g, ' ');

                        // If text node is not within an element remove leading and trailing spaces.
                        // For example, pasting content from Word has text nodes with whitespace
                        // between elements.
                        if ($(next.parentElement).is('body')) {
                            text = text.replace(/^\s*|\s*$/g, '');
                        }
                        if ($(next.parentElement).is('td,th')) {
                            if (!next.previousSibling) {
                                text = text.replace(/^\s+/, '');
                            }
                            if (!next.nextSibling) {
                                text = text.replace(/\s+$/, '');
                            }
                        }

                        val += text;

                    } else if (next.nodeType === 1) {

                        // We got an element
                        elementName = next.tagName.toLowerCase();

                        // Determine if the element maps to one of our defined styles
                        matchStyleObj = self.getStyleForElement(next);

                        // For table elements and enhancements we should keep the elements as they are
                        switch (elementName) {
                        case 'table':
                        case 'tbody':
                        case 'tr':
                        case 'td':
                        case 'th':
                            matchStyleObj = true;
                            break;

                        case 'span':
                        case 'button':
                            if ($(next).hasClass('enhancement')) {
                                matchStyleObj = true;
                            }
                            break;
                        case 'ol':
                        case 'ul':
                        case 'br':
                            matchStyleObj = true;
                            break;
                        }

                        // If we got a matching element output the start of the element as HTML
                        if (matchStyleObj) {

                            elementName = matchStyleObj === true ? elementName : matchStyleObj.element;

                            if (elementName) {

                                val += '<' + elementName;

                                $.each(next.attributes, function(i, attrib){

                                    var attributeName = attrib.name;
                                    var attributeValue = attrib.value;

                                    val += ' ' + attributeName + '="' + self.htmlEncode(attributeValue) + '"';
                                });

                                if (matchStyleObj !== true && matchStyleObj.elementAttr) {
                                    $.each(matchStyleObj.elementAttr, function(attr, value){
                                        val += ' ' + attr + '="' + self.htmlEncode(value) + '"';
                                    });
                                }

                                // Close void elements like <input/>
                                if (self.voidElements[ elementName ]) {
                                    val += '/';
                                }

                                val += '>';

                                if (!self.voidElements[ elementName ]) {
                                    elementClose = '</' + elementName + '>';
                                }

                            } else if (matchStyleObj.line) {

                                // The style doesn't have an element to output, but it indicates there should be a line break
                                elementClose = '<br/>';

                            }
                        }

                        // Recursively go into the node to get text or other children
                        processNode(next);

                        if (elementClose) {
                            val += elementClose;
                        }

                    } // else if this is an element...

                    next = next.nextSibling;

                } // while there is a next sibling...

            } // function processNode

            processNode(el);

            return '<div>' + val + '</div>';
        },


        /**
         * Add text to the editor at the current selection or cursor position.
         */
        insert: function(value, styleKey) {

            var range;
            var self;
            var mark;

            self = this;

            // Insert text and change range to be around the new text so we can add a style
            self.codeMirror.replaceSelection(value, 'around');

            range = self.getRange();
            if (styleKey) {
                mark = self.setStyle(styleKey, range);
            }

            // Now set cursor after the inserted text
            self.codeMirror.setCursor( range.to );

            return mark;
        },


        /**
         * Encode text so it is HTML safe.
         * @param {String} s
         * @return {String}
         */
        htmlEncode: function(s) {
            return String(s)
                .replace(/&/g, '&amp;')
                .replace(/"/g, '&quot;')
                .replace(/'/g, '&#39;')
                .replace(/</g, '&lt;')
                .replace(/>/g, '&gt;');
        },


        /**
         * Parse an HTML string and return a DOM structure.
         *
         * @param {String|DOM} html
         * An HTML string or a DOM structure.
         *
         * @returns {DOM}
         */
        htmlParse: function(html) {
            var dom;

            if ($.type(html) === 'string') {
                dom = new DOMParser().parseFromString(html, "text/html").body;
            } else {
                dom = html;
            }
            return dom;
        },


        find: function(){
            var self;
            self = this;
            self.codeMirror.execCommand('find');
        },


        replace: function(){
            var self;
            self = this;
            self.codeMirror.execCommand('replace');
        },


        /**
         * Get all the attributes for an element.
         *
         * @param {Element|jQuery} el
         * A DOM element, jQuery object, or a jQuery selector string.
         *
         * @returns {Object}
         * A
         */
        getAttributes: function(el) {

            var attr;
            var $el;


            attr = {};

            $el = $(el);

            if($el.length) {

                // Loop through all the attributes
                // Note in some browsers (old IE) this will return all possible attributes
                // even if the attribute is not set, so we check the values too.
                $.each($el.get(0).attributes, function(value,node) {
                    var name;
                    name = node.nodeName || node.name;
                    value = $el.attr(name);
                    if (value !== undefined && value !== false) {
                        attr[name] = value;
                    }
                });
            }

            return attr;
        },


        /**
         * Generate a message to display all the marks to the console.
         * @return {String}
         */
        logMarks: function() {
            var marks;
            var msg;
            var self;
            self = this;
            marks = self.codeMirror.getAllMarks();
            msg = '';
            $.each(marks, function(i,mark) {
                if (mark.className === 'rte2-style-spelling') {
                    return;
                }
                msg += ' ' + mark.id + ':' + mark.className;
            })
            return msg;
        }

    };

    return CodeMirrorRte;

}); // define

// Set filename for debugging tools to allow breakpoints even when using a cachebuster
//# sourceURL=richtextCodeMirror.js
