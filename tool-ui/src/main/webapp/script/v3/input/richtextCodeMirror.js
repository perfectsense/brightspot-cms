define([
    'jquery',
    'bsp-utils',
    'v3/spellcheck',
    'codemirror/lib/codemirror',
    'codemirror/addon/hint/show-hint',
    'codemirror/addon/dialog/dialog',
    'codemirror/addon/search/searchcursor',
    'codemirror/addon/search/search'
], function($, bsp_utils, spellcheckAPI, CodeMirror) {
    
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
            }
        }, // styles

        
        /**
         * Rules for cleaning up the clipboard data when content is pasted
         * from outside the RTE.
         *
         * This is an object of key/value pairs, where the key is a jQuery selector,
         * and value is a style name from the styles object.
         *
         * @example
         * {'span[style*="font-style:italic"]': 'italic',
         *  'span[style*="font-weight:700"]': 'bold'}
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

            self.enhancementInit();
            self.initListListeners();
            self.dropdownInit();
            self.initEvents();
            self.clipboardInit();
            self.trackInit();
            self.spellcheckInit();
            self.modeInit();

            $(window).resize(bsp_utils.throttle(500, function () {
                self.refresh();
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

            var editor, isFirstListItem, isLastListItem, isEmptyLine, listType, rangeFirstLine, self;

            self = this;
            
            editor = self.codeMirror;

            // Monitor the "beforeChange" event so we can save certain information
            // about lists, to later use in the "change" event
            editor.on('beforeChange', function(instance, changeObj) {

                var listTypePrevious, listTypeNext, rangeBeforeChange;

                // Get the listType and set the closure variable for later use
                listType = self.blockGetListType(changeObj.from.line);

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

                isStartOfLine = Boolean(rangeBeforeChange.from.ch === 0);
                
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

                        // Set list style for the new range
                        self.blockSetStyle(listType, range);
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
            
            var editor, self;

            self = this;
            
            editor = self.codeMirror;

            editor.on('cursorActivity', function(instance, event) {
                self.$el.trigger('rteCursorActivity', [self]);
            });
            
            editor.on('changes', $.debounce(200, function(instance, event) {
                self.triggerChange();
            }));

            editor.on('focus', function(instance, event) {
                self.$el.trigger('rteFocus', [self]);
            });
            
            editor.on('blur', function(instance, event) {
                self.$el.trigger('rteBlur', [self]);
            });
        },

        
        /**
         * Trigger an rteChange event.
         * This can happen when user types changes into the editor, or if some kind of mark is modified.
         */
        triggerChange: function() {
            var self;
            self = this;
            self.$el.trigger('rteChange', [self]);
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
            
            var mark, self, styleObj;

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
            
            var mark, self, styleObj;

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
            
            var blockStyles, context, contextArray, contextMarks, contextNull, editor, foundBlockStyle, lineNumber, self;

            self = this;
            editor = self.codeMirror;
            range = range || self.getRange();
            lineNumber = range.from.line;

            // Placeholder for the context elements
            context = {};

            // Loop through all lines in the range
            editor.eachLine(range.from.line, range.to.line + 1, function(line) {

                var charFrom, charTo, isRange, marks, rightmostMark, rightmostPos, styleObj;
                
                charFrom = (lineNumber === range.from.line) ? range.from.ch : 0;
                charTo = (lineNumber === range.to.line) ? range.to.ch : line.text.length;

                isRange = Boolean(charFrom !== charTo);
                
                // Loop through each character in the line
                for (charNumber = charFrom; charNumber <= charTo; charNumber++) {

                    rightmostMark = undefined;
                    rightmostPos = undefined;
                    
                    // Get all of the marks for this character and get a list of the class names
                    marks = editor.findMarksAt({ line: lineNumber, ch: charNumber });

                    // Find the mark with the rightmost starting character
                    marks.forEach(function(mark){
                        
                        var isRightmost, markPosition, pos;
                        
                        if (!mark.className) {
                            return;
                        }
                        // Make sure this class maps to an element (and is not an internal mark like for spelling errors)
                        if (!self.classes[mark.className]) {
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
                            context[styleObj.element] = true;
                        }
                        
                    } else {

                        // Use line styles if there are no inline marks
                        
                        blockStyles = self.blockGetStyles({from:{line:lineNumber, ch:0}, to:{line:lineNumber, ch:0}});
                        foundBlockStyle = false;
                        $.each(blockStyles, function(styleKey) {
                            var element;
                            element = self.styles[styleKey].element;
                            if (element) {
                                context[element] = true;
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
            contextArray = [];
            $.each(context, function(element) {
                if (element === 'NULL') {
                }
                contextArray.push(element);
            });

            if (contextNull) {
                contextArray.push(null);
            }

            return contextArray;
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

            var mark, self;

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
            
            var className, editor, isEmpty, line, mark, markOptions, self, styleObj, $widget, widgetOptions;

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
                editor.replaceRange(' ', {line:range.from.line, ch:range.from.ch});
                
                range.to.line = range.from.line;
                range.to.ch = range.from.ch + 1;
            }

            mark = editor.markText(range.from, range.to, markOptions);
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

            var className, deleteText, editor, lineNumber, self, from, to;

            self = this;

            editor = self.codeMirror;
            
            if (styleKey) {
                className = self.styles[styleKey].className;
            }
            
            options = options || {};
            deleteText = options.deleteText;
            
            range = range || self.getRange();

            from = range.from;
            to = range.to;

            lineNumber = from.line;
            
            // Before beginning, clean up the CodeMirror marks
            // to make sure they do not span across separate lines.
            self.inlineCleanup();

            editor.eachLine(from.line, to.line + 1, function(line) {

                var fromCh, toCh, marks;

                // Get the character ranges to search within this line.
                // If we're not on the first line, start at the beginning of the line.
                // If we're not on the last line, stop at the end of the line.
                fromCh = (lineNumber === from.line) ? from.ch : 0;
                toCh = (lineNumber === to.line) ? to.ch : line.text.length;

                // Loop through all the marks defined on this line
                marks = line.markedSpans || [];
                marks.forEach(function(mark) {

                    var from, markerOpts, markerOptsNotInclusive, matchesClass, outsideOfSelection, selectionStartsBefore, selectionEndsAfter, styleObj, to;
                    
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

                    markerOpts = mark.marker;
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
                    outsideOfSelection = fromCh > to || toCh < from;
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
                        editor.markText(
                            { line: lineNumber, ch: toCh },
                            { line: lineNumber, ch: to },
                            markerOpts
                        );

                        if (deleteText) {
                            // Create a marker for the text that will be deleted
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

                        editor.markText(
                            { line: lineNumber, ch: from },
                            { line: lineNumber, ch: fromCh },
                            markerOptsNotInclusive
                        );

                        if (deleteText) {
                            // Create a marker for the text that will be deleted
                            // It should be the part of the marked text that is outside the range
                            editor.markText(
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
                        if (styleObj.enhancementType) {
                            return;
                        }
                        
                        editor.markText(
                            { line: lineNumber, ch: toCh },
                            { line: lineNumber, ch: to },
                            markerOpts
                        );

                        editor.markText(
                            { line: lineNumber, ch: from },
                            { line: lineNumber, ch: fromCh },
                            markerOptsNotInclusive
                        );
                        
                        if (deleteText) {
                            // Create a marker for the text that will be deleted
                            // It should be the part of the marked text that is outside the range
                            editor.markText(
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
                    if (position) {
                        editor.replaceRange('', position.from, position.to, '+brightspotFormatRemoveClass');

                        // Trigger a change event for the editor
                        if (options.triggerChange !== false) {
                            self.triggerChange();
                        }
                    }
                }
                if (mark.shouldRemove) {
                    
                    mark.clear();
                    
                    // Trigger a change event for the editor
                    if (options.triggerChange !== false) {
                        self.triggerChange();
                    }
                }
            });

            // Trigger a cursor activity event so the toolbar can update
            CodeMirror.signal(editor, "cursorActivity");

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
            
            var mark, pos, self, styles;

            self = this;
            
            mark = self.inlineGetMark(styleKey, range);
            if (mark) {
                
                pos = mark.find();
            
                // Delete the text within the mark
                self.codeMirror.replaceRange('', pos.from, pos.to, 'brightspotRemoveStyledText');

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
            
            var classes, className, self, styles;

            self = this;

            // Check if className is a key into our styles object
            className = self.styles[styleKey].className;
            
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

            var className, editor, matchingMark, self;

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
            
            var self, styles, value;

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
            
            var classes, classMap, isClass, editor, lineNumber, self, styles, lineStarting;

            self = this;
            editor = self.codeMirror;
            range = range || self.getRange();
            lineNumber = range.from.line;

            styles = {};
            classes = {};
            
            isClass = true;

            editor.eachLine(range.from.line, range.to.line + 1, function(line) {

                var charTo, charNumber, charFrom, once, isRange;
                
                charFrom = (lineNumber === range.from.line) ? range.from.ch : 0;
                charTo = (lineNumber === range.to.line) ? range.to.ch : line.text.length;

                isRange = Boolean(charFrom !== charTo);
                
                // Loop through each character in the range
                for (charNumber = charFrom; charNumber <= charTo; charNumber++) {

                    var classesForChar, marks;
                    
                    classesForChar = {};

                    // Get all of the marks for this character and get a list of the class names
                    marks = editor.findMarksAt({ line: lineNumber, ch: charNumber });

                    marks.forEach(function(mark) {
                        
                        var isSingleChar, markPosition;
                        
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

                            isSingleChar = Boolean(charTo - charFrom < 2);
                            
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
                var styleKey, styleObj;
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

            var className, editor, marks, marksCollapsed, self;

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

                var markCollapsed, markPosition, $widget, widgetOptions;
                
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

            var className, editor, self;

            self = this;
            editor = self.codeMirror;

            // Check if className is a key into our styles object
            className = self.styles[styleKey].className;
            
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

            var className, editor, foundUncollapsed, marks, marksCollapsed, self;

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

                var markCollapsed, markPosition, $widget, widgetOptions;
                
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
         * CodeMirror makes marks across line boundaries.
         * This function steps through all marks and splits up the styles
         * so they do not cross the end of the line.
         * This allows other functions to operate correctly.
         */
        inlineCleanup: function() {

            var doc, editor, marks, marksByClassName, self;

            self = this;
            editor = self.codeMirror;
            doc = editor.getDoc();

            // Loop through all the marks in the document,
            // find the ones that span across lines and split them
            $.each(editor.getAllMarks(), function(i, mark) {
                self.inlineSplitMarkAcrossLines(mark);
            });

            self.inlineCombineAdjacentMarks();
        },


        /**
         * Inside any "raw" HTML mark, remove all other marks.
         */
        rawCleanup: function() {
            
            var editor, self;

            self = this;
            editor = self.codeMirror;
            
            $.each(editor.getAllMarks(), function(i, mark) {

                var pos, styleObj, from, to, marks;
                
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

            var editor, to, lineNumber, pos, self, singleLine, styleObj, from;

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

            // Does this mark span multiple lines?
            if (to.line !== from.line) {

                // Loop through the lines that this marker spans and create a marker for each line
                for (lineNumber = from.line; lineNumber <= to.line; lineNumber++) {

                    var fromCh, newMark, toCh;
                    
                    fromCh = (lineNumber === from.line) ? from.ch : 0;
                    toCh = (lineNumber === to.line) ? to.ch : editor.getLine(lineNumber).length;

                    
                    // Create a new mark on this line only
                    newMark = editor.markText(
                        { line: lineNumber, ch: fromCh },
                        { line: lineNumber, ch: toCh },
                        mark
                    );

                    // Copy any additional attributes that were attached to the old mark
                    newMark.attributes = mark.attributes;

                    // Don't create other marks if this is a singleLine mark
                    if (singleLine) {
                        break;
                    }
                }

                // Remove the old mark that went across multiple lines
                mark.clear();
            }
        },


        /**
         * Combine all marks that are overlapping or adjacent.
         * Note this assumes that the marks do not span across multiple lines.
         */
        inlineCombineAdjacentMarks: function() {

            var editor, marks, self;

            self = this;
            editor = self.codeMirror;
            
            // Combine adjacent marks
            // Note this assumes that marks do not span across lines
            
            // First get a list of the marks
            marks = editor.getAllMarks();

            // Remove any bookmarks (which are used for enhancements and markers)
            marks = $.map(marks, function(mark, i) {
                
                if (mark.type === 'bookmark') {
                    return undefined;
                } else {
                    return mark;
                }
            });
            
            // Sort the marks in order of position
            marks = marks.sort(function(a, b){
                
                var posA, posB;

                posA = a.find();
                posB = b.find();

                if (posA.from.line === posB.from.line) {
                    // If marks are on same line sort by the character number
                    return posA.from.ch - posB.from.ch;
                } else {
                    // If marks are on differnt lines sort by the line number
                    return posA.from.line - posB.from.line;
                }
            });

            // Next group the marks by class name
            // This will give us a list of classnames, and each one will contain a list of marks in order
            marksByClassName = {};
            $.each(marks, function(i, mark) {
                var className;

                className = mark.className;

                // Skip any classname that is not in our styles list
                if (!self.classes[className]) {
                    return;
                }
     
                // Skip any classname that has an onClick since we dont' want to mess with those.
                // For example, links.
                if (self.classes[className].onClick) {
                    return;
                }
                
                if (!marksByClassName[className]) {
                    marksByClassName[className] = [];
                }
                marksByClassName[className].push(mark);
            });

            // Next go through all the classes, and combine the marks
            $.each(marksByClassName, function(className, marks) {

                var i, mark, markNext, markNew, pos, posNext;

                i = 0;
                while (marks[i]) {

                    mark = marks[i];
                    markNext = marks[i + 1];

                    if (!markNext) {
                        break;
                    }

                    pos = mark.find();
                    posNext = markNext.find();

                    if (pos.from.line === posNext.from.line) {

                        if (posNext.from.ch <= pos.to.ch) {

                            // The marks are overlapping or adjacent, so combine them into a new mark
                            markNew = editor.markText(
                                { line: pos.from.line, ch: pos.from.ch },
                                { line: pos.from.line, ch: Math.max(pos.to.ch, posNext.to.ch) },
                                mark
                            );

                            // Clear the original two marks
                            mark.clear();
                            markNext.clear();

                            // Replace markNext with markNew so the next loop will start with the new mark
                            // and we can check to see if the following mark needs to be combined again
                            marks[i + 1] = markNew;
                            
                        }
                    }

                    i++;
                }
            });
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
            
            var mark, self;

            self = this;

            range = range || self.getRange();
            
            if (self.blockIsStyle(styleKey, range)) {
                self.blockRemoveStyle(styleKey, range);
            } else {
                mark = self.blockSetStyle(styleKey, range);
            }
            
            return mark;
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

            var className, editor, lineHandle, lineNumber, mark, self, styleObj;

            self = this;
            editor = self.codeMirror;
            range = range || self.getRange();
            options = options || {};
            
            if (typeof style === 'string') {
                styleObj = self.styles[style];
            } else {
                styleObj = style;
            }
            className = styleObj.className;

            // Create a fake "mark" object for the line
            mark = {
                
                // just in case we need to distinguish this is our fake mark...
                rteLineStyle: true,
                
                // other code checks for className on the mark so we'll save it here too
                className: className 
            };

            // Save attributes on the mark
            if (options.attributes) {
                mark.attributes = options.attributes;
            }
            
            for (lineNumber = range.from.line; lineNumber <= range.to.line; lineNumber++) {
                
                editor.addLineClass(lineNumber, 'text', className);

                // Store the mark data (and attributes) for the block style
                self.blockSetLineData(styleObj.key, lineNumber, mark);
                
            }

            // If this is a set of mutually exclusive styles, clear the other styles
            if (styleObj.clear) {
                $.each(styleObj.clear, function(i, styleKey) {
                    self.blockRemoveStyle(styleKey, range);
                });
            }

            // Refresh the editor display since our line classes
            // might have padding that messes with the cursor position
            editor.refresh();

            if (options.triggerChange !== false) {
                self.triggerChange();
            }

            //return mark;
            return self.blockGetLineData(styleObj.key, range.from.line) || {};
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

            var data, editor, lineHandle, self, styleObj;
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
            
            var editor, lineHandle, self, styleObj, className;
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

            var className, classNames, classes, editor, line, lineNumber, self;

            self = this;
            editor = self.codeMirror;
            range = range || self.getRange();

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
                    editor.removeLineClass(lineNumber, 'text', className);
                    
                } else {
                    
                    // Remove all classes from the line
                    line = editor.getLineHandle(lineNumber);
                    $.each((line.textClass || '').split(' '), function(i, className) {
                        editor.removeLineClass(lineNumber, 'text', className);
                    });
                }
            }
            
            // Refresh the editor display since our line classes
            // might have padding that messes with the cursor position
            editor.refresh();
            
            self.triggerChange();
        },
        

        
        /**
         * Determines if all lines in a range have the specified class.
         */
        blockIsStyle: function(styleKey, range) {

            var classes, editor, self, styles;

            self = this;
            editor = self.codeMirror;

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
            
            var classes, editor, self, styles;

            self = this;
            editor = self.codeMirror;
            
            range = range || self.getRange();

            // Loop through all lines in the range
            editor.eachLine(range.from.line, range.to.line + 1, function(line) {

                var classNames, classesLine;

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
                    var styleKey, styleObj;
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

            var classNames, editor, line, lineInfo, listType, self;

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

            var editor, mark, range, self, widgetOptions;

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
                
                mark = editor.addLineWidget(lineNumber, content, {above: true});

                mark.deleteLineFunction = function(){

                    var content, $content;

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
            
            // Small delay before refreshing the editor to prevent cursor problems
            setTimeout(function(){
                self.refresh();
                mark.changed();
                self.triggerChange();
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

            var content, $content, editor, lineLength, lineNumber, lineMax, position, self;

            self = this;
            editor = self.codeMirror;

            // Get the options we saved previously so we can create a mark with the same options
            options = mark.options;
            
            lineMax = editor.lineCount() - 1;
            lineNumber = self.enhancementGetLineNumber(mark) + lineDelta;

            if (lineNumber < 0) {
                return mark;
            }
            
            if (lineNumber > lineMax) {
                
                // Add another line to the end of the editor
                lineLength = editor.getLine(lineMax).length;
                editor.replaceRange('\n', {line:lineMax, ch:lineLength}, 'brightspotEnhancementMove');
                
            }

            // If the next (or previous) line is blank, then try to move to the line after that (if it is not blank)
            lineDelta = Math.sign(lineDelta) * 1;
            if (self.isLineBlank(lineNumber) && !self.isLineBlank(lineNumber + lineDelta)) {
                lineNumber += lineDelta;
            }

            // Depending on the type of mark that was created, the content is stored differently
            content = self.enhancementGetContent(mark);
            $content = $(content).detach();

            self.enhancementRemove(mark);
            
            mark = self.enhancementAdd($content[0], lineNumber, options);
            
            return mark;
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
            
            var lineNumber, position;

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
            
            var content, $content, lineNumber, self;

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

            var content, $content, lineNumber, self;

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


        //==================================================
        // Dropdown Handlers
        //==================================================

        dropdownInit: function() {
            
            var editor, self;

            self = this;
            
            editor = self.codeMirror;

            self.$dropdown = $('<div/>', {
                'class': 'rte2-dropdown'
            }).hide().appendTo( editor.getWrapperElement() );
            
            editor.on('cursorActivity', $.debounce(250, function(instance, event) {
                self.dropdownCheckCursor();
            }));
        },


        dropdownCheckCursor: function() {

            var marks, self;
            self = this;

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
            var editor, lineStyles, marks, range, self;

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
                marks.push(mark);
            });
        
            // Find all the inline marks for the clicked position
            // (funky javascript to append one array onto the end of another)
            [].push.apply(marks, editor.findMarks(range.from, range.to));

            // Only keep the marks that have onClick configs
            marks = $.map(marks, function(mark, i) {
                var styleObj;
                styleObj = self.classes[mark.className];
                if (styleObj && (styleObj.onClick || styleObj.void)) {
                    // Keep in the array
                    return mark;
                } else {
                    // Remove from the array
                    return null;
                }
            });

            return marks;
        },

        
        /**
         * @param {Array} marks 
         */
        dropdownShow: function(marks) {
            var self;
            self = this;

            self.$dropdown.empty();
            
            $.each(marks, function(i, mark) {
                
                var $div, label, $li, styleObj;

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

                $('<span/>', {
                    'class':'rte2-dropdown-label',
                    text: label
                }).appendTo($div);

                // Popup edit defaults to true, but if set to false do not include edit link
                if (styleObj.popup !== false) {
                    $('<a/>', {
                        'class': 'rte2-dropdown-edit',
                        text: 'Edit'
                    }).on('click', function(event){
                        event.preventDefault();
                        self.onClickDoMark(event, mark);
                        return false;
                    }).appendTo($div);
                }
                
                $('<a/>', {
                    'class': 'rte2-dropdown-clear',
                    text: 'Clear'
                }).on('click', function(event){

                    var pos;
                    
                    event.preventDefault();

                    // For void element, delete the text in the mark
                    if (styleObj.void) {
                        if (mark.find) {
                            pos = mark.find();
                            // Delete below after the mark is cleared
                        }
                    }
                    
                    mark.clear();
                    if (pos) {
                        self.codeMirror.replaceRange('', {line:pos.from.line, ch:pos.from.ch}, {line:pos.to.line, ch:pos.to.ch});
                    }
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

            var ch, chMin, editor, left, line, self, top;
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
            
            var range, self, styleObj;

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
            
            var editor, self;

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
            
            var classes, editor, charPosition, cursorPosition, isEmpty, self, textOriginal, textNew;
            
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
                        self.inlineSetStyle('trackInsert', {from: changeObj.from, to:changeObj.to});

                        // In case we are inserting inside a deleted block,
                        // make sure the new text we are adding is not also marked as deleted
                        self.inlineRemoveStyle('trackDelete', {from: changeObj.from, to:changeObj.to});

                        // Some text was pasted in and already marked as new,
                        // but we must remove any regions within that were previously marked deleted
                        self.trackAfterPaste(changeObj.from, changeObj.to, changeObj.text);

                    } else {

                        // We are replacing existing text, so we need to handle cases where the text to be replaced
                        // already has things that we are tracking as deleted or inserted.

                        // Do not do the paste or insert
                        changeObj.cancel();
                        
                        // Mark the whole range as "deleted" for track changes
                        // Note there might be some regions inside this that are marked as "inserted" but we'll deal with that below
                        self.trackMarkDeleted({from: changeObj.from, to:changeObj.to});

                        // Delete text within the range if it was previously marked as a new insertion
                        // Note: after doing this, the range might not be valid since we might have removed characters within it
                        self.inlineRemoveStyle('trackInsert', {from:changeObj.to, to:changeObj.to}, {deleteText:true});

                        // Insert the new text...
                        
                        // First remove the "delete" mark at the point where we are insering to make sure the new text is not also marked as deleted
                        self.inlineRemoveStyle('trackDelete', {from: changeObj.from, to:changeObj.from});
                        
                        // Then add a mark so the inserted text will be marked as an insert
                        self.inlineSetStyle('trackInsert', {from: changeObj.from, to:changeObj.from}, {inclusiveLeft:true});

                        // Finally insert the text at the starting point (before the other text in the range that was marked deleted)
                        // Note we add at the front because we're not sure if the end is valid because we might have removed some text
                        if (changeObj.origin === 'brightspotPaste') {
                            
                            editor.replaceRange(changeObj.text, changeObj.from, undefined, '+brightspotTrackInsert');

                            // TODO: what if the copied region has deleted text?
                            // Currently the entire content that is pasted will be marked as inserted text,
                            // but it could have deleted text within it.
                            // We need to remove that deleted text *after* the new content is pasted in.
                        }
                    }
                    
                    break;

                case '+brightspotTrackInsert':

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
                            
                            editor.replaceRange(' ', changeObj.from, changeObj.to, 'brighspotTrackSpace');
                            changeObj.update(changeObj.from, {line:changeObj.to.line, ch:changeObj.to.ch + 1});
                        }

                        self.inlineRemoveStyle('trackInsert', {from: changeObj.from, to: changeObj.to});
                        self.inlineRemoveStyle('trackDelete', {from: changeObj.from, to: changeObj.to});
                    }
                }
            }
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

            var self, toNew;

            self = this;
            
            // Figure out the new range based on the original range and the replacement text
            toNew = {
                line: from.line + textArray.length - 1,
                ch: textArray[ textArray.length - 1 ].length
            };
            if (toNew.line == from.line) {
                toNew.ch += from.ch;
            }
            
            // Use a timeout so the change can be completed before we attempt to remove the deleted text
            setTimeout(function(){
                self.inlineRemoveStyle('trackDelete', {from:from, to:toNew}, {deleteText:true, triggerChange:false});
            }, 1);
            
        },

        
        /**
         * For a given range, mark everything as deleted.
         * Also remove any previously inserted content within the range.
         */
        trackMarkDeleted: function(range) {
            
            var editor, self;

            self = this;
            editor = self.codeMirror;
            
            // If we're deleting just a line just let it be deleted
            // Because we don't have a good way to accept or reject a blank line
            textOriginal = editor.getRange(range.from, range.to);
            if (textOriginal === '\n') {
                return;
            }

            // Determine if every character in the range is already marked as an insertion.
            // In this case we can just delete the content and don't need to mark it as deleted.
            if (self.inlineGetStyles(range).trackInsert !== true) {
                self.inlineSetStyle('trackDelete', range);
            }

            // Remove any text within the range that is marked as inserted
            self.inlineRemoveStyle('trackInsert', range, {deleteText:true});
        },

        
        /**
         * Accept all the marked changes within a range.
         */ 
        trackAcceptRange: function(range) {
            
            var editor, marks, self;

            self = this;
            editor = self.codeMirror;

            // First combine any of the adjacent track changes
            self.inlineCleanup();
            
            range = range || self.getRange();

            $.each(editor.findMarks(range.from, range.to), function(i, mark) {
                self.trackAcceptMark(mark);
            });
            
        },


        /**
         * Reject all the marked changes within a range.
         */ 
        trackRejectRange: function(range) {
            
            var editor, self;

            self = this;
            editor = self.codeMirror;
            
            // First combine any of the adjacent track changes
            self.inlineCleanup();
            
            range = range || self.getRange();

            $.each(editor.findMarks(range.from, range.to), function(i, mark) {
                self.trackRejectMark(mark);
            });
        },

        
        /**
         * Accept a single marked change.
         */
        trackAcceptMark: function(mark) {

            var editor, position, self;

            self = this;
            editor = self.codeMirror;
            
            position = mark.find();

            if (position) {
                if (mark.className === self.styles.trackDelete.className) {
                    // For a delete mark, remove the content
                    mark.clear();
                    editor.replaceRange('', position.from, position.to, '+brightspotTrackAcceptMark');
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

            var editor, position, self;

            self = this;
            editor = self.codeMirror;
            
            position = mark.find();

            if (position) {
                if (mark.className === self.styles.trackInsert.className) {
                    // For an insert mark, remove the content
                    mark.clear();
                    editor.replaceRange('', position.from, position.to, '+brightspotTrackRejectMark');
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
            
            var editor, pos, self, $wrapper;

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
            
            var editor, isFirefox, isWindows, self, $wrapper;
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

                    var x, y;
                    
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
            
            var allowRaw, isWorkaround, self, value, valueHTML, valueRTE, valueText;
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
            
            var dom, $el, self;
            
            self = this;
            dom = self.htmlParse(html);
            $el = $(dom);

            // Apply the clipboard sanitize rules (if any)
            if (self.clipboardSanitizeRules) {
                $el.find('p').after('<br/>');
                $.each(self.clipboardSanitizeRules, function(selector, style) {
                    $el.find(selector).wrapInner( $('<span>', {'data-rte2-sanitize': style}) );
                });
            }

            // Run it through the clipboard sanitize function (if it exists)
            if (self.clipboardSanitizeFunction) {
                $el = self.clipboardSanitizeFunction($el);
            }

            return $el[0];
        },

        
        /**
         * @param {Event} e
         * The cut/copy event. This is required because you can only access the clipboardData from one of these events.
         *
         * @param {String} value
         * The HTML text to save in the clipboard.
         */
        clipboardCopy: function(e) {
            
            var editor, html, range, self, text;
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
                e.clipboardData.setData('text/brightspot-rte2', html);

                // Clear the cut area
                if (e.type === 'cut') {
                    editor.replaceRange('', range.from, range.to, 'brightspotCut');
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

            var self, text, wordsArray, wordsArrayUnique, wordsRegexp, wordsUnique;

            self = this;
            
            // Get the text for the document
            text = self.toText() || '';

            // Split into words
            wordsRegexp = self.spellcheckWordCharacters; // new RegExp('[' + self.spellcheckWordCharacters + ']+', 'g');
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

                $.each(wordsArrayUnique, function(i,word) {
                    
                    var adjacent, range, result, index, indexStart, indexWord, range, split, wordLength;

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
                    
                });
                
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

            var editor, markOptions, self;

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

            var editor, self;

            self = this;

            editor = self.codeMirror;
            
            // Loop through all the marks and remove the ones that were marked
            editor.getAllMarks().forEach(function(mark) {
                if (mark.className === 'rte2-style-spelling') {
                    mark.clear();
                }
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

            var editor, marks, pos, range, self, suggestions;

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
            
            var editor, self, text, textUpper;

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
            
            var html, node, self;

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
            
            var chEnd, chStart, editor, html, line, lineRange, lineText, node, self;
            
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
                html = self.toHTML(lineRange);

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
            var node, self;
            
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
            var childNodes, i, length, self;
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
        // Miscelaneous Functions
        //==================================================


        /**
         * Give focus to the editor
         */
        focus: function() {
            var self;
            self = this;
            self.codeMirror.focus();
        },


        isLineBlank: function(lineNumber) {
            var editor, self, text;
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
            
            var editor, line, max, self;

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
            var count, self;
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

            var from, self, to;

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

            var editor, self;

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

            var self, totalLines;

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
            
            var self, map;

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
            
            var self, map;

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
        refresh: function(mark) {
            var self;
            self = this;
            self.codeMirror.refresh();
        },


        /**
         * Empty the editor and clear all marks and enhancements.
         */
        empty: function() {
            var editor, self;
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
            var pos, self;
            self = this;
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
            var pos, self;

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
         * Determine if an element is a "container" for another element.
         * For example, element "li" is contained within a "ul" or "ol" element.
         *
         * @param {String} elementName
         * The name of an element such as "ul"
         *
         * @return {Boolean}
         */
        elementIsContainer: function(elementName) {
            var isContainer, self;
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
            
            var keymap, self;

            self = this;

            keymap = {};

            keymap['Tab'] = false;
            keymap['Shift-Tab'] = false;

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
                            return self.toggleStyle(styleKey);
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
            var count, self;
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
         */
        toHTML: function(range) {

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

            var blockElementsToClose, blockActive, doc, enhancementsByLine, html, rangeWasSpecified, self;

            self = this;

            rangeWasSpecified = Boolean(range);
            range = range || self.getRangeAll();
            
            // Before beginning make sure all the marks are cleaned up and simplified.
            // This will ensure that none of the marks span across lines.
            self.inlineCleanup();

            // Clean up any "raw html" areas so they do not allow styles inside
            // Removing this for now as it causes performance problems when there are many raw marks.
            // However, that means user might be able to mark up raw areas and produce invalid HTML.
            // self.rawCleanup();
            
            doc = self.codeMirror.getDoc();

            // List of block styles that are currently open.
            // We need this so we can continue a list on the next line.
            blockActive = {};
            
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
                
                $.each(self.enhancementCache, function(i, mark) {

                    var lineNo;

                    lineNo = self.enhancementGetLineNumber(mark);
                
                    if (lineNo !== undefined) {
                    
                        // Create an array to hold the enhancements for this line, then add the current enhancement
                        enhancementsByLine[lineNo] = enhancementsByLine[lineNo] || [];
                
                        enhancementsByLine[lineNo].push(mark);
                    }
                });
            }
            
            // Start the HTML!
            html = '';
            
            // Loop through the content one line at a time
            doc.eachLine(function(line) {

                var annotationStart, annotationEnd, blockOnThisLine, charNum, charInRange, htmlStartOfLine, htmlEndOfLine, inlineActive, inlineActiveIndex, inlineActiveIndexLast, inlineElementsToClose, isVoid, lineNo, lineInRange, outputChar, raw, rawLastChar;

                lineNo = line.lineNo();
                
                htmlStartOfLine = '';
                htmlEndOfLine = '';
                
                // List of inline styles that are currently open.
                // We need this because if we close one element we will need to re-open all the elements.
                inlineActive = [];

                // List of inline elements that are currently open
                // (in the order they were opened so they can be closed in reverse order)
                inlineElementsToClose = [];

                // Keep track of the block styles we find only on this line,
                // so we know when to end a list.
                // For example, if the previous line was in a list,
                // and the current line contains a list item, then we keep the list open.
                // But if the current line does not contain a list item then we close the list.
                blockOnThisLine = {};

                // If lineNo is in range
                if (range.from.line <= lineNo && range.to.line >= lineNo) {

                    // Get any line classes and determine which kind of line we are on (bullet, etc)
                    // Note this does not support nesting line elements (like a list within a list)
                    // From CodeMirror, the textClass property will contain multiple line styles separated by space
                    // like 'rte2-style-ol rte2-style-align-left'
                    if (line.textClass) {
                        
                        $.each(line.textClass.split(' '), function() {
                            
                            var container, lineStyleData, styleObj;

                            // From a line style (like "rte2-style-ul"), determine the style name it maps to (like "ul")
                            styleObj = self.classes[this];
                            if (!styleObj) {
                                return;
                            }

                            // Get the "container" element for this style (for example: ul or ol)
                            container = styleObj.elementContainer;
                            if (container) {

                                // Mark that we found this container, so later we can close off any containers that are not active any more.
                                // For example this will set blockOnThisLine.ul to true.
                                blockOnThisLine[container] = true;
                                
                                // Check to see if we are already inside this container element
                                if (blockActive[container]) {

                                    // We are currently inside this style so we don't need to open the container element
                                    
                                } else {

                                    // We are not already inside this style, so create the container element
                                    // and remember that we created it
                                    blockActive[container] = true;
                                    htmlStartOfLine += '<' + container + '>';
                                }
                            }

                            // Get any attributes that might be defined for this line style
                            lineStyleData = self.blockGetLineData(styleObj.key, lineNo) || {};
                            
                            // Now determine which element to create for the line.
                            // For example, if it is a list then we would create an 'LI' element.
                            htmlStartOfLine += openElement(styleObj, lineStyleData.attributes);

                            // Also push this style onto a stack so when we reach the end of the line we can close the element
                            blockElementsToClose.push(styleObj);
                            
                        }); // .each
                    }// if line.textClass
                    
                } // if lineNo is in range
                
                // Now that we know which line styles are on this line, we can tell if we need to continue a list
                // from a previous line, or actually close the list.
                // Loop through all the blocks that are currently active (from this line and previous lines)
                // and find the ones that are not active on this line.
                $.each(blockActive, function(container) {

                    if (!blockOnThisLine[container]) {
                        delete blockActive[container];
                        if (container) {
                            html += '</' + container + '>';
                        }
                    }
                });

                // Determine if there are any enhancements on this line
                if (enhancementsByLine[lineNo]) {
                    
                    $.each(enhancementsByLine[lineNo], function(i,mark) {

                        var enhancmentHTML;

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
                html += htmlStartOfLine;
                
                // Get the start/end points of all the marks on this line
                // For these objects the key is the character number,
                // and the value is an array of class names. For example:
                // {'5': 'rte2-style-subscript'}
                
                annotationStart = {};
                annotationEnd = {};
                
                if (line.markedSpans) {
                    
                    $.each(line.markedSpans, function(key, markedSpan) {
                        
                        var className, endArray, endCh, mark, startArray, startCh, styleObj;

                        startCh = markedSpan.from;
                        endCh = markedSpan.to;
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
                                var element;
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

                            var element, styleToClose;
                            
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
                                while (styleToClose = inlineElementsToClose.pop()) {
                                    
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
                                
                                var element;

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
                    } else if (charInRange) {
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
            $.each(blockActive, function(container) {
                delete blockActive[container];
                if (container) {
                    html += '</' + container + '>';
                }
            });

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
        _fromHTML: function(html, range, allowRaw) {

            var annotations, editor, enhancements, el, history, map, self, val;

            self = this;
            
            editor = self.codeMirror;

            allowRaw = (allowRaw === false ? false : true);
            
            // Convert the styles object to an object that is indexed by element,
            // so we can quickly map an element to a style.
            // Note there might be more than one style for an element, in which
            // case we will use attributes to determine if we have a match.
            map = self.getElementMap();

            // Convert HTML into a DOM element so we can parse it using the browser node functions
            el = self.htmlParse(html);

            // Text for the editor
            val = '';

            // Inline and block markers
            annotations = [];
            enhancements = [];
            
            function processNode(n, rawParent) {
                
                var elementAttributes, elementName, elementClose, from, isContainer, matchStyleObj, next, raw, rawChildren, split, to, text;

                next = n.childNodes[0];

                while (next) {

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

                                var from, split, to;
                                
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
                            matchStyleObj = false;

                            // If a data-rte2-sanitize attribute is found on the element, then we are getting this
                            // html as pasted data from another source. Our sanitzie rules have marked this element
                            // as being a particular style, so we should force that style to be used.
                            matchStyleObj = self.styles[ $(next).attr('data-rte2-sanitize') ];
                            
                            // Multiple styles might map to a particular element name (like <b> vs <b class=foo>)
                            // so first we get a list of styles that map just to this element name
                            matchArray = map[elementName];
                            if (matchArray && !matchStyleObj) {

                                $.each(matchArray, function(i, styleObj) {

                                    var attributesFound;

                                    // Detect blocks that have containers (like "li" must be contained by "ul")
                                    if (styleObj.elementContainer && styleObj.elementContainer.toLowerCase() !== next.parentElement.tagName.toLowerCase()) {
                                        return;
                                    }

                                    attributesFound = {};

                                    // If the style has attributes listed we must check to see if they match this element
                                    if (styleObj.elementAttr) {

                                        // Loop through all the attributes in the style definition,
                                        // and see if we get a match
                                        $.each(styleObj.elementAttr, function(attr, expectedValue) {

                                            var attributeValue;

                                            attributeValue = $(next).attr(attr);

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
                                                return false;
                                            }
                                        });


                                    } else {
                                        
                                        // There were no attributes specified for this style so we might have a match just on the element
                                        matchStyleObj = styleObj;
                                    }

                                    /***

                                        // Not needed anymore (?) since we assume that rules are set up to match elements with specified
                                        // attribues. You can match <span myattr1> and <span myattr2> but if you tried to match
                                        // just <span> then that might match in a different order so it might cause problems.

                                    // Check if the element has other attributes that are unexpected
                                    if (matchStyleObj && !styleObj.elementAttrAny) {
                                        $.each(elementAttributes, function(attr, value) {
                                            if (!attributesFound[attr]) {
                                                // Oops, this element has an extra attribute that is not in our style object,
                                                // so it should not be considered a match
                                                matchStyleObj = false;
                                                return false;
                                            }
                                        });
                                    }
                                    ***/


                                    // Stop after the first style that matches
                                    if (matchStyleObj) {
                                        return false;
                                    }
                                });
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

                            enhancements.push({
                                line: from.line,
                                $content: $(next)
                            });

                            // Skip past the enhancement
                            next = next.nextSibling;
                            continue;
                        }
                        
                        // For container elements such as "ul" or "ol", do not allow nested lists within.
                        // If we find a nested list treat the whole thing as raw html
                        isContainer = self.elementIsContainer(elementName);
                        if (isContainer) {
                            
                            // If there are any nested list items, then we treat this element as raw html
                            if ($(next).find('li li').length) {
                                raw = true;
                                rawChildren = true;
                            }
                            
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
                                to:to
                            });
                            
                            // Remember we need to close the element later
                            if (!self.voidElements[ elementName ]) {
                                elementClose = '</' + elementName + '>';
                            }
                        }

                        // Recursively go into our element and add more text to the value
                        processNode(next, rawChildren);

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
                                        to:to
                                    });
                                }
                                
                            } else {
                                annotations.push({
                                    styleObj:matchStyleObj,
                                    from:from,
                                    to:to,
                                    attributes: elementAttributes
                                });
                            }
                        }

                        // Add a new line for certain elements
                        // Add a new line for custom elements
                        if (self.newLineRegExp.test(elementName) || (matchStyleObj && matchStyleObj.line)) {
                            val += '\n';
                        }

                    } // else if this is an element...
                    
                    next = next.nextSibling;
                    
                } // while there is a next sibling...
                
            } // function processNode

            processNode(el);

            // Replace multiple newlines at the end with single newline
            val = val.replace(/[\n\r]+$/, '\n');

            if (range) {

                // Remove the styles from the range so for example we don't paste content
                // into a bold region and make all the pasted content bold.
                
                // There seems to be a problem if the range is a single cursor position
                // and we can't remove the styles for that.
                // So instead we'll insert a space, then remove the styles from that space character,
                // then call an undo() to remove the space from the document (and the undo history).

                if (range.from.line === range.to.line && range.from.ch === range.to.ch) {

                    editor.replaceRange(' ', range.from, range.to, 'brightspotRemoveStyles');
                
                    // Remove styles from the single character
                    self.removeStyles({
                        from: { line:range.from.line, ch:range.from.ch },
                        to: { line:range.from.line, ch:range.from.ch + 1}
                    });

                    // Undo the insertion of the single character so it doesn't appear in the undo history
                    editor.undo();

                } else {

                    // Remove styles from the range
                    self.removeStyles(range);

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

            // Add the plain text into the selected range
            editor.replaceRange(val, range.from, range.to, 'brightspotPaste');

            // Before we start adding styles, save the current history.
            // After we add the styles we will restore the history.
            // This will prevent lots of undo history being added,
            // so user can undo this new content all in one shot.
            history = editor.getHistory();
            
            // Set up all the annotations
            // We reverse the order of the annotations because the parsing was done
            // depth first, and we want to create the marks for parent elements before
            // the marks for child elements (so elements can later be created in the same order)
            $.each(annotations.reverse(), function(i, annotation) {

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
                
                if (styleObj.line) {
                    self.blockSetStyle(styleObj, annotation, {triggerChange:false, attributes:annotation.attributes});
                } else {
                    self.inlineSetStyle(styleObj, annotation, {addToHistory:false, triggerChange:false, attributes:annotation.attributes});
                }
            });

            $.each(enhancements, function(i, enhancementObj) {
                
                // Pass off control to a user-defined function for adding enhancements
                self.enhancementFromHTML(enhancementObj.$content, enhancementObj.line);
                
            });

            editor.setHistory(history);
            self.triggerChange();
            
        }, // fromHTML()


        /**
         * Add text to the editor at the current selection or cursor position.
         */
        insert: function(value, styleKey) {
            
            var range, self;

            self = this;

            // Insert text and change range to be around the new text so we can add a style
            self.codeMirror.replaceSelection(value, 'around');
            
            range = self.getRange();
            if (styleKey) {
                self.setStyle(styleKey, range);
            }

            // Now set cursor after the inserted text
            self.codeMirror.setCursor( range.to );
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
            var dom, self;
            self = this;

            if ($.type(html) === 'string') {
                dom = new DOMParser().parseFromString(html, "text/html").body;     
            } else {
                dom = html;
            }
            return dom;
        },


        /**
         * Clear the undo history for the editor.
         * For example, you can call this after setting the initial content in the editor.
         */
        historyClear: function(){
            var self;
            self = this;
            self.codeMirror.clearHistory();
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
            
            var attr, $el, self;
            
            self = this;

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
        }
        
    };

    return CodeMirrorRte;

}); // define

// Set filename for debugging tools to allow breakpoints even when using a cachebuster
//# sourceURL=richtextCodeMirror.js
