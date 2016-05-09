/* jshint undef: true, unused: true, browser: true, jquery: true, devel: true */
/* global define */

define(['jquery'], function($) {

    var module;

    /**
     * Table editor module.
     *
     * @example
     * // Create default table
     * var t = Object.create(module);
     * t.init($('#placeholder'));
     * 
     * @example
     * // Create table from existing HTML
     * var t = Object.create(module);
     * t.init($('#placeholder'), {tableHtml: '<table>...</table>'});
     *
     * @example
     * // Create table from existing table
     * var t = Object.create(module);
     * t.init($('#placeholder'), {tableEl: $('#mytable')});
     */
    module = {

        /**
         * Base class name for styling various elements.
         */
        className: 'bspTableEdit',

        
        /**
         * A set of key/value settings to customize the context menu for each cell.
         *
         * The following keys are standard:
         * rowAddAbove, rowAddBelow, rowDelete, colAddLeft, colAddRight, colDelete, cellClear
         * In addition you can add your own custom keys such as "edit"
         *
         * The value must be an object, and may contain the following parameters:
         *
         * {String} [name]: the name to display in the context menu
         *
         * {Function} [handler]: a function to handle clicks on the content menu.
         * TODO what is passed in to the handler
         */
        contextMenu: {
            cellClear: {},
            rowAddAbove: {},
            rowAddBelow: {},
            rowDelete: {},
            colAddLeft: {},
            colAddRight: {},
            colDelete: {}
        },

        
        /**
         * Default names for the standard context menu actions.
         */
        contextMenuNames: {
            rowAddAbove: 'Add Row Above',
            rowAddBelow: 'Add Row Below',
            colAddLeft: 'Add Column Left',
            colAddRight: 'Add Column Right',
            rowDelete: 'Delete Row',
            colDelete: 'Delete Column',
            cellClear: 'Clear Cell'
        },

        
        /**
         * @param {Element|jQuery|Selector} el
         * Element where the table should be rendered.
         *
         * @param {Object} options
         * Set of key/value options.
         *
         * @param {String} options.tableHtml
         * @param {Element} options.tableEl
         * 
         */
        init: function(el, options) {
            
            var self;
            self = this;
            self.$el = $(el);

            if (options) {
                $.extend(self, options);
            }
            
            if (options.tableEl) {
                self.$table = $(options.tableEl);
            } else if (options.tableHtml) {
                self.$table = $(options.tableHtml);
            } else {
                self.$table = $('<table><tr><td>&nbsp;</td><td>&nbsp;</td></tr></table>');
            }

            self.$wrapper = $('<div/>', {
                'class': self.className + 'Wrapper'
            }).appendTo(self.$el);

            self.$tableWrapper = $('<div/>', {
                'class': self.className + 'TableWrapper'
            }).appendTo(self.$wrapper);
                
            self.$table.appendTo(self.$tableWrapper);

            self.activeClass = self.className + 'Active';
            
            self.initEvents();
            self.contextInit();
        },


        /**
         * Set up the click event for table cells.
         */
        initEvents: function() {

            var self;

            self = this;
            
            self.$tableWrapper.on('click', function(event) {

                var $cell;
                
                event.preventDefault();
                event.stopPropagation();

                $cell = $(event.target).closest('td, th');

                self.selectedSet($cell);
                self.contextShow();

            });
        },

        
        /**
         * Set the table cell (td or th) that is currently selected.
         */
        selectedSet: function(el) {
            var self;
            self = this;
            self.$selected = $(el);
            self.activeSet(el);
        },

        
        /**
         * Return thte table cell (td or th) that is currently selected.
         */
        selectedGet: function() {
            var self;
            self = this;
            return self.$selected || self.$table.find('td,th').eq(0);
        },

        
        /**
         * Clear the active class from all cells.
         */
        activeClear: function() {
            var self;
            self = this;
            self.$table.find('.' + self.activeClass).removeClass(self.activeClass);
            delete self.$active;
        },

        
        /**
         * Set the active class on a cell.
         * 
         */
        activeSet: function(cell) {
            var self;
            self = this;
            self.activeClear();
            $(cell).addClass(self.activeClass);
            self.$active = $(cell);
        },


        /**
         * Returns the active cell.
         * @returns {jQuery|undefined}
         */
        activeGet: function() {
            var self;
            self = this;
            return self.$active;
        },

        
        /**
         * Create the context menu
         */
        contextInit: function() {
            
            var self;
            
            self = this;
            
            self.$context = $('<div/>', {
                'class': self.className + 'Context'
            }).hide().on('click', function(event) {
                event.preventDefault();
                event.stopPropagation();
            }).appendTo(self.$wrapper);
        },

        
        /**
         * Before displaying the context menu put the appropriate links inside it.
         */
        contextUpdate: function() {
            
            var $el, self;
            
            self = this;

            self.$context.empty();
            $el = self.selectedGet();
            
            $.each(self.contextMenu, function(key, options){

                var item, $item;

                // Set up default values for this item
                item = {
                    name: self.contextMenuNames[key] || key,
                    handler: function() {
                        self.contextHandler(key);
                    }
                };

                // Make the options override the defaults
                $.extend(item, options);

                $item = $('<a/>', {
                    'class': 'tableModuleContextItem',
                    href: '#',
                    text: item.name
                }).on('click', function(event) {
                    event.stopPropagation();
                    event.preventDefault();
                    self.activeClear();
                    self.contextHide();

                    // Call the handler function for the context menu item.
                    // Pass in this tableEditor object, plus the active cell
                    item.handler(self, $el);
                });

                $('<div/>').html($item).appendTo(self.$context);
            });
        },


        /**
         * Update the position of the context menu so it appears below the active cell.
         */
        contextPosition: function() {
            
            var $el, pos, self;
            
            self = this;

            $el = self.selectedGet();
            pos = $el.position();
            if (pos) {
                self.$context.css({left:pos.left, top:pos.top + $el.outerHeight() - 1});
            }
        },

        
        /**
         * Handle the context menu for the built-in functionality.
         *
         * @param {String} key
         * Context menu key for any of the built-in functions.
         */
        contextHandler: function(key) {
            
            var self;
            self = this;

            switch (key) {
                
            case 'rowAddAbove':
                self.rowAdd({above:true});
                break;
            case 'rowAddBelow': 
                self.rowAdd();
                break;
            case 'colAddLeft': 
                self.colAdd({left:true});
                break;
            case 'colAddRight': 
                self.colAdd();
                break;
            case 'rowDelete': 
                self.rowDelete();
                break;
            case 'colDelete': 
                self.colDelete();
                break;
            case 'cellClear':
                self.cellClear();
                break;
            }
        },

        
        /**
         * Show the context menu for a cell.
         */
        contextShow: function() {
            
            var self;
            
            self = this;

            // Update the links that are in the context menu
            self.contextUpdate();
            
            // Position the context menu based on the selected cell
            self.contextPosition();

            self.$context.show();
            
            // Hide the context menu automatically if user clicks outside
            $(document).on('mouseup.contextHide', function(event) {
                if (!self.$context.is(event.target) && self.$context.has(event.target).length === 0) {
                    self.activeClear();
                    self.contextHide();
                }
            });
        },

        
        /**
         * Hide the context menu.
         */
        contextHide: function() {
            var self;
            self = this;
            self.$context.hide();

            // Remove event that hides the context menu
            $(document).off('mouseup.contextHide');
        },


        contextIsVisible: function() {
            var self;
            self = this;
            return self.$context.is(':visible');
        },

        
        /**
         * Add a row.
         *
         * @param {Object} options
         * A set of key/value pairs to set options.
         *
         * @param {Boolean} [options.above=false]
         * Set to true if you want to add the row above the current row.
         * Defaults to false (add the row below the current row).
         *
         * @param {Element} [options.cell=selected cell]
         * A cell (td or th) within the table. Used to position the new row.
         * If not specified, defaults to the last clicked cell.
         */
        rowAdd: function(options) {
            
            var $cell, cols, i, $row, $rowNew, self;
            
            self = this;
            
            options = options || {};
            
            $cell = options.cell ? $(options.cell) : self.selectedGet();
            
            $row = $cell.closest('tr');

            $rowNew = $('<tr/>');
            cols = self.colCount($cell);
            for (i=0; i<cols; i++) {
                $rowNew.append( self.cellCreate() );
            }
            
            if (options.above) {
                $rowNew.insertBefore($row);
            } else {
                $rowNew.insertAfter($row);
            }
        },

        
        /**
         * Delete a row.
         *
         * @param {Object} options
         * A set of key/value pairs to set options.
         *
         * @param {Element} [options.cell=selected cell]
         * A cell (td or th) within the table. Used to position the new row.
         * If not specified, defaults to the last clicked cell.
         */
        rowDelete: function(options) {
            
            var $cell, $row, self;
            
            self = this;
            
            options = options || {};
            
            $cell = options.cell ? $(options.cell) : self.selectedGet();
            
            // Do not remove the last row
            if (self.rowCount($cell) <= 1) {
                return;
            }
            
            $row = $cell.closest('tr');
            $row.remove();
        },


        /**
         * Return the number of rows in the table.
         *
         * NOTE: does not support ROWSPAN attributes.
         *
         * @param {Element} [cell=selected cell]
         * A cell (td or th) within the table.
         * If not specified, defaults to the last clicked cell.
         */
        rowCount: function(cell) {
            var $cell, self, $table;
            self = this;
            $cell = cell ? $(cell) : self.selectedGet();
            $table = $cell.closest('table');
            return $table.find('> * > tr').length;
        },

        
        /**
         * Add a column.
         *
         * @param {Object} options
         * A set of key/value pairs to set options.
         *
         * @param {Boolean} [options.left=false]
         * Set to true if you want to add the column to the left if the current column.
         * Defaults to false (add the column to the right of the current column).
         *
         * @param {Element} [options.cell=selected cell]
         * A cell (td or th) within the table. Used to position the new row.
         * If not specified, defaults to the last clicked cell.
         */
        colAdd: function(options) {
            
            var $cell, colCount, colNumber, self, $table;
            
            self = this;
            
            options = options || {};
            
            $cell = options.cell ? $(options.cell) : self.selectedGet();

            // Make sure each row has the correct number of cells
            self.colNormalize({cell:$cell});
            
            // Get total number of columns across the entire table
            colCount = self.colCount($cell);
            
            colNumber = $cell.index();
            if (!options.left) {
                colNumber++;
            }

            $table = $cell.closest('table');
            $table.find('> * > tr').each(function() {
                
                var $td, $tds, $tr;

                $tr = $(this);
                $tds = $tr.find('> td, > th');

                // Now add a new cell at the insertion point
                $td = self.cellCreate();
                if (colNumber >= colCount) {
                    $td.appendTo($tr);
                } else {
                    $td.insertBefore( $tr.find('> td, > th').eq(colNumber) );
                }
            });
        },

        
        /**
         * Delete a column.
         *
         * @param {Object} options
         * A set of key/value pairs to set options.
         *
         * @param {Element} [options.cell=selected cell]
         * A cell (td or th) within the table.
         * If not specified, defaults to the last clicked cell.
         */
        colDelete: function(options) {
            
            var $cell, colCount, colNumber, self, $table;
            
            self = this;
            
            options = options || {};
            
            $cell = options.cell ? $(options.cell) : self.selectedGet();

            // Make sure each row has the correct number of cells
            self.colNormalize({cell:$cell});

            // Special case - do not delete the last column
            colCount = self.colCount($cell);
            if (colCount === 1) {
                return;
            }
            
            colNumber = $cell.index();

            $table = $cell.closest('table');
            $table.find('> * > tr').each(function() {
                
                var $tds, $tr;

                $tr = $(this);
                $tds = $tr.find('> td, > th');

                // Delete the cell for the column
                $tds.eq(colNumber).remove();
            });
        },

        
        /**
         * Return the number of columns in the table.
         *
         * NOTE: does not support COLSPAN attributes.
         *
         * @param {Element} [cell=selected cell]
         * A cell (td or th) within the table.
         * If not specified, defaults to the last clicked cell.
         */
        colCount: function(cell) {
            
            var $cell, count, self, $table;
            self = this;
            $cell = cell ? $(cell) : self.selectedGet();
            $table = $cell.closest('table');

            count = 0;
            $table.find('> * > tr').each(function() {
                var $tr, cellCount;
                $tr = $(this);
                cellCount = $tr.find('> td, > th').length;
                count = Math.max(count, cellCount);
            });
            
            return count;
        },


        /**
         * Normalize all the rows in a table so they have same number of cells
         * and are not missing any columns.
         *
         * NOTE: does not support COLSPAN attributes.
         *
         * @param {Object} options
         * A set of key/value pairs to set options.
         *
         * @param {Element} [options.cell=selected cell]
         * A cell (td or th) within the table.
         * If not specified, defaults to the last clicked cell.
         */
        colNormalize: function(options) {
            
            var $cell, colCount, self, $table;
            
            self = this;
            
            options = options || {};
            
            $cell = options.cell ? $(options.cell) : self.selectedGet();

            // Get total number of columns across the entire table
            colCount = self.colCount($cell);
            
            $table = $cell.closest('table');
            $table.find('> * > tr').each(function() {
                
                var i, $tds, $tr;

                $tr = $(this);
                $tds = $tr.find('> td, > th');

                // If this row is missing cells add them first
                for (i = $tds.length; i < colCount; i++) {
                    $tr.append( self.cellCreate() );
                }
                
            });
        },

        /**
         * Creates a new cell to be inserted into the table.
         * @returns {jQuery}
         */
        cellCreate: function() {
            return $('<td/>', {html:'&nbsp;'});
        },

        
        /**
         * Clear the content of a cell.
         *
         * @param {String|Element|jQuery} content
         * Content to replace in the cell.
         *
         * @param {Object} [options]
         * Set of key/value options.
         *
         * @param {Object} [options.cell=selected cell]
         * The cell to set. 
         */
        cellClear: function(options) {
            var self;
            self = this;
            self.cellSet('&nbsp;', options);
        },

        
        /**
         * Set the content of a cell.
         *
         * @param {String|Element|jQuery} content
         * Content to replace in the cell.
         *
         * @param {Object} [options]
         * Set of key/value options.
         *
         * @param {Object} [options.cell=selected cell]
         * The cell to set. 
         */
        cellSet: function(content, options) {
            
            var $cell, self;
            
            self = this;
            
            options = options || {};
            
            $cell = options.cell ? $(options.cell) : self.selectedGet();

            $cell.html(content);
        },

        
        /**
         * Get the contents of a cell.
         *
         * @param {Object} [options]
         * Set of key/value options.
         *
         * @param {Object} [options.cell=selected cell]
         * The cell to retrieve. 
         *
         * @param {Boolean} [options.html=false]
         * Set to true if you want to return an HTML string.
         * Defaults to false and returns a jQuery object of the cell contents.
         *
         * @returns {jQuery|String}
         */
        cellGet: function(options) {
            
            var $cell, self;
            
            self = this;
            
            options = options || {};
            
            $cell = options.cell ? $(options.cell) : self.selectedGet();

            return options.html ? $cell.html() : $cell.contents();
        },

        
        /**
         * Get the entire contents of the table.
         *
         * @param {Object} [options]
         * Set of key/value options.
         *
         * @param {Boolean} [options.html=false]
         * Set to true if you want to return an HTML string.
         * Defaults to false and returns a jQuery object pointing to the table.
         *
         * @returns {jQuery|String}
         */
        tableGet: function(options) {
            
            var $cloned, self;
            
            self = this;
            
            options = options || {};

            // Remove the active class first
            self.activeClear();
            
            $cloned = $('<div/>').append( self.$table.clone() );

            // Remove any empty 'class' attributes because jquery leaves those around after setting and removing the active classes
            $cloned.find('[class=""]').removeAttr('class');
            return options.html ? $cloned.html() : self.$table;
        }
    };
    
    return module;

});

// Set filename for debugging tools to allow breakpoints even when using a cachebuster
//# sourceURL=tableEditor.js
