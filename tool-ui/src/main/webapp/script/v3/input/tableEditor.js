/* jshint undef: true, unused: true, browser: true, jquery: true, devel: true */
/* global define document */

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
            colDelete: {},
            mergeBelow: {},
            mergeRight: {},
            unMerge: {}
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
            cellClear: 'Clear Cell',
            mergeBelow: 'Merge Cell Below',
            mergeRight: 'Merge Cell Right',
            unMerge: 'Unmerge Cells'
        },


        /**
         * Number of columns to show in the table sizer when creating a new table.
         * @type {Number}
         */
        sizerCols: 20,

        
        /**
         * Number of rows to show in the table sizer when creating a new table.
         * @type {Number}
         */
        sizerRows: 10,

        /**
         * HTML to display above the table sizer when creating a new table.
         * If an element with class 'sizerCount' appears within the HTML,
         * the currently selected table size will be inserted like "(5 x 8)"
         * @type {String}
         */
        sizerMessage: 'Click to set the starting columns and rows for the table: <span class="sizerCount"></span><br/>',
        
        
        /**
         * @param {Element|jQuery|Selector} el
         * Element where the table should be rendered.
         *
         * @param {Object} options
         * Set of key/value options.
         *
         * @param {String} options.tableHtml
         * @param {Element} options.tableEl
         * @param {Boolean} options.readOnly
         * 
         */
        init: function(el, options) {
            var newTable;
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
                
                // Set a flag so we know to display the sizer control
                newTable = true;
                
                // We start by creating a small table, just in case the user submits the form
                // before selecting a table size, it will use this blank table as a fallback
                self.$table = self.sizerCreateTable(2, 1);
            }

            self.$wrapper = $('<div/>', {
                'class': self.className + 'Wrapper'
            }).appendTo(self.$el);

            self.$tableWrapper = $('<div/>', {
                'class': self.className + 'TableWrapper'
            }).appendTo(self.$wrapper);
                
            self.$table.appendTo(self.$tableWrapper);

            self.activeClass = self.className + 'Active';
            
            self.readOnlySet(options.readOnly);
            
            self.initEvents();
            self.contextInit();
            if (newTable) {
                self.initSizer();
            }
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

                $cell = $(event.target).closest('td, th, table');
                
                // Make sure user clicked on a table cell
                if ($cell.is('td, th')) {
                    self.selectedSet($cell);
                    self.contextShow();
                }

            });
        },


        /**
         * For a new table, hide the table initially and give the user a way to select the table size.
         * After the size is selected replace the table with one of the selected size.
         * @return {[type]}
         */
        initSizer: function() {
            var self;
            var $table;
            self = this;
            
            // Hide the table we already created.
            self.$tableWrapper.hide();
            
            // Create the sizer control: wrapper, a message, and a table for selecting row and column
            self.$sizer = $('<div/>', {'class': self.className + 'Sizer'}).appendTo(self.$wrapper);

            $('<div/>', {
                'class': self.className + 'SizerMessage',
                html: self.sizerMessage
            }).appendTo(self.$sizer);
            
            $table = self.sizerCreateTable(self.sizerCols, self.sizerRows).appendTo(self.$sizer);
            
            // Set up events for the sizer control
            $table.on('mouseover', 'td', function(event) {
                var pos;
                pos = self.sizerGetPos(event.target);
                self.sizerHighlight(pos.col + 1, pos.row + 1);
            });
            $table.on('click', 'td', function(event) {
                var pos;
                pos = self.sizerGetPos(event.target);
                self.sizerSetSize(pos.col + 1, pos.row + 1);
            });
            
            self.$sizerTable = $table;
        },

        
        /**
         * When the user mouses over the size table, we set an active class on cells to show
         * the table size that would be created.
         * @param  {Number} col
         * Number of columns to highlight.
         * @param  {Number} row
         * Number of rows to highlight.
         */
        sizerHighlight: function(col, row) {
            var self;
            
            self = this;
            self.$sizerTable.find('td').removeClass('active').each(function(){
                var pos;
                pos = self.sizerGetPos(this);
                if (pos.col < col && pos.row < row) {
                    $(this).addClass('active');
                }
            });
            
            // Update the sizer count
            self.$sizer.find('.sizerCount').html('(' + col + ' x ' + row + ')');
        },


        /**
         * When the user clicks a cell in the size table, we create a table sized based on
         * which row,col the user clicked. Then we show the new table and remove the sizer control.
         * @param  {Number} col
         * Number of columns to create in the new table.
         * @param  {Number} row
         * Number of rows to create in the new table.
         */
        sizerSetSize: function(col, row) {
            var self;
            var $table;
            
            self = this;

            // Replace the table with a new one of the selected size
            $table = self.sizerCreateTable(col, row);
            self.$table.replaceWith($table);
            self.$table = $table;
            self.$tableWrapper.show();
            self.$sizer.remove();
        },


        /**
         * Create an empty table element with the specified rows and columns.
         * @param  {Number} rows
         * @param  {Number} cols
         */
        sizerCreateTable: function(cols, rows) {
            var col;
            var row;
            var $table;
            var $tr;
            
            $table = $('<table>');
            for (row = 0; row < rows; row++) {
                $tr = $('<tr>').appendTo($table);
                for (col = 0; col < cols; col++) {
                    $tr.append('<td>&nbsp;</td>');
                }
            }
            return $table;
        },


        /**
         * Returns the row,col position of a td in the sizer table.
         * @param  {Element} td The TD element.
         * @return {Object} An object with row and col parameters.
         */
        sizerGetPos: function(td) {
            var $td;
            $td = $(td);
            return {row: $td.closest('tr').index(), col: $td.index()};
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
            var $activeTableClass;
            var self;
            
            self = this;
            $activeTableClass = self.$table.find('.' + self.activeClass);
            $activeTableClass.removeClass(self.activeClass);
            if ($activeTableClass.attr('class') === ""){
                $activeTableClass.removeAttr('class');
            }
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
            }).appendTo(document.body);
        },

        
        /**
         * Before displaying the context menu put the appropriate links inside it.
         */
        contextUpdate: function() {
            var $el;
            var canMerge;
            var self;
            self = this;

            self.$context.empty();
            $el = self.selectedGet();
            self.tableAnalyze($el.closest('table'));
            canMerge = self.checkMerge($el);
            
            $.each(self.contextMenu, function(key, options){

                var item;
                var $item;

                // Omit merge commands if necessary
                if (key === 'mergeRight' && !canMerge.col) {return;}
                if (key === 'mergeBelow' && !canMerge.row) {return;}
                if (key === 'unMerge' && !canMerge.unmerge) {return;}

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
            var $el;
            var offset;
            var self;
            self = this;
            $el = self.selectedGet();
            offset = $el.offset();

            if (offset) {
                self.$context.css({
                    left: offset.left,
                    top: offset.top + $el.outerHeight() - 1,
                    'z-index': $el.zIndex() + 1
                });
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

            // Do not do anything when in readonly mode
            if (self.readOnlyGet()) {
                return;
            }
            
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
            case 'mergeBelow':
                self.rowMerge();
                break;
            case 'mergeRight':
                self.colMerge();
                break;
            case 'unMerge':
                self.unMerge();
                break;
            }
        },

        
        /**
         * Show the context menu for a cell.
         */
        contextShow: function() {
            var self;
            self = this;

            // Do not show the context menu when in readonly mode
            if (self.readOnlyGet()) {
                return;
            }
            
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
            var $cell;
            var cellsFound;
            var cols;
            var i;
            var pos;
            var $row;
            var $rowNew;
            var $rowNext;
            var rowNumber;
            var rowSpan;
            var self;
            self = this;
            
            options = options || {};
            
            $cell = options.cell ? $(options.cell) : self.selectedGet();
            
            self.tableAnalyze($cell.closest('table'));

            pos = self.lookupPos($cell);
            rowNumber = pos.row;
            
            $row = $cell.closest('tr');

            // Special case: if this cell has a rowspan then insert the new row after the rowspan
            if (!options.above) {
                rowSpan = $cell[0].rowSpan;
                if (rowSpan && rowSpan > 1) {
                    // Get the row that ends this rowspan
                    $rowNext = $row.nextAll().slice(rowSpan - 2, rowSpan - 1);
                    if ($rowNext.length) {
                        $row = $rowNext;
                        rowNumber += rowSpan - 1;
                    }
                }
            }
            
            // Special case: if the new row will be within some other rowspan of another column,
            // then we must account for that: create one less cell in the new row,
            // and add to the rowspan of the other cell.
            cols = self.colCount($cell);
            rowNumber += options.above ? 0 : 1;
            if (self.matrix[rowNumber]) {
                cellsFound = [];
                $.each(self.matrix[rowNumber], function(colNumber, cell) {
                    var posCell;
                    posCell = self.lookupPos(cell);
                    // Check if the cell covering this position is actually in another row,
                    // because that would mean it has a rowspan that covers our row
                    // and we should not add a cell for this position for our new row. 
                    if (posCell.row !== rowNumber) {

                        // Do not create a cell for this position
                        cols--;
                        
                        // Increase the rowspan so it will also cover the new row we are adding.
                        // However, make sure we only increase the rowspan once because the cell might also
                        // have a colspan that makes it appear in another position!
                        if ($.inArray(cell, cellsFound) === -1) {
                            $(cell).attr('rowSpan', cell.rowSpan + 1);
                            cellsFound.push(cell);
                        }
                    }
                });
            }
        
            $rowNew = $('<tr/>');
            for (i=0; i<cols; i++) {
                $rowNew.append( self.cellCreate() );
            }
            
            if (options.above) {
                $rowNew.insertBefore($row);
            } else {
                $rowNew.insertAfter($row);
            }
            
            self.triggerChange();
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
            var $cell;
            var $row;
            var rowIndex;
            var self;
            var $table;
            
            self = this;
            options = options || {};
            $cell = options.cell ? $(options.cell) : self.selectedGet();
            $row = $cell.closest('tr');
            rowIndex = $row[0].rowIndex;
            $table = $cell.closest('table');
            
            // Do not remove the last row
            if (self.rowCount($cell) <= 1) {
                return;
            }
            
            self.tableAnalyze($table);
            
            // Find all the cells in this row (including cells from other rows that have a rowspan)
            $.each(self.matrix[rowIndex], function(colNumber, cell) {
                if (cell.rowSpan && cell.rowSpan > 2) {
                    $(cell).attr('rowspan', cell.rowSpan - 1);
                }
            });
                        
            $row.remove();
            
            self.triggerChange();
        },


        /**
         * Return the number of rows in the table.
         *
         * @param {Element} [cell=selected cell]
         * A cell (td or th) within the table.
         * If not specified, defaults to the last clicked cell.
         */
        rowCount: function(cell) {
            var $cell;
            var self;
            var $table;
            self = this;
            $cell = cell ? $(cell) : self.selectedGet();
            $table = $cell.closest('table');
            return $table.find('> * > tr').length;
        },

    
        /**
         * Merge a cell with the cell below.
         * @param  {[type]} cell [description]
         * @return {[type]}      [description]
         */
        rowMerge: function(cell) {
            var $cell;
            var $table;
            var canMerge;
            var cellBelow;
            var pos;
            var rowspan;
            var self;
            
            self = this;
            cell = cell || self.selectedGet();
            $cell = $(cell);
            rowspan = $cell[0].rowSpan;
            $table = $cell.closest('table');
            
            self.tableAnalyze($table);
            canMerge = self.checkMerge($cell);
            if (canMerge.row) {
                
                pos = self.lookupPosComputed($cell);
                
                cellBelow = self.lookupCellComputed(pos.row + rowspan, pos.col);
                if (!cellBelow) {
                    return;
                }

                self.mergeContents($cell, $(cellBelow));
                $cell.attr('rowspan', rowspan + cellBelow.rowSpan);
                $(cellBelow).remove();
            }
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
            var $cell;
            var cellsFound;
            var colCount;
            var colNumber;
            var colSpan;
            var posCell;
            var self;
            var $table;
            self = this;
            options = options || {};
            $cell = options.cell ? $(options.cell) : self.selectedGet();
            $table = $cell.closest('table');

            // Make sure each row has the correct number of cells
            self.tableAnalyze($table)
            self.tableNormalize($table);
            
            // Get total number of columns across the entire table
            colCount = self.colCount($cell);
            
            // Determine which column number we will be adding
            posCell = self.lookupPosComputed($cell[0]);
            colNumber = posCell.col;
            colSpan = $cell[0].colSpan;
            if (!options.left) {
                colNumber += colSpan;
            }
            
            cellsFound = [];
                        
            // Loop through all the table rows so we can add a column to each
            $table.find('> * > tr').each(function(rowNumber, tr) {
                var cell;
                var $td;
                var $tr;
                $tr = $(tr);

                // Find the actual cell that corresponds to this row/col
                cell = self.matrix[rowNumber][colNumber];
                if (cell) {
                    
                    // Check if the cell covering this position is actually in another column,
                    // because that would mean it has a colspan that covers our cell
                    // and we should not add a cell for this position 
                    posCell = self.lookupPosComputed(cell);
                    if (posCell.col !== colNumber) {
                        // Make sure we don't modify the colspan more than once
                        if ($.inArray(cell, cellsFound) === -1) {
                            $(cell).attr('colSpan', cell.colSpan + 1);
                            cellsFound.push(cell);
                        }
                        return;
                    }
                }
                
                // We are not inside a colspan range, so create a new cell
                $td = self.cellCreate();
                if (!cell || colNumber >= colCount) {
                    $td.appendTo($tr);
                } else {
                    $td.insertBefore(cell);
                }
            });
            
            self.triggerChange();
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
            var $cell;
            var colCount;
            var colNumber;
            var pos;
            var self;
            var $table;
            self = this;
            options = options || {};
            $cell = options.cell ? $(options.cell) : self.selectedGet();
            $table = $cell.closest('table');

            // Analyze the table to compute the COLSPAN and ROWSPAN
            self.tableAnalyze($table);

            // Make sure each row has the correct number of cells before we begin
            self.tableNormalize($table);
            
            pos = self.lookupPosComputed($cell);

            // Special case - do not delete the last column
            colCount = self.colCount($cell);
            if (colCount === 1) {
                return;
            }
            
            colNumber = pos.col;

            $table.find('> * > tr').each(function(rowIndex, tr) {
                
                var cell;
                var $cell;
                var colspan;

                // Find the cell for this colNumber
                cell = self.lookupCellComputed(rowIndex, colNumber);
                if (cell) {
                    $cell = $(cell);
                    colspan = cell.colSpan;
                    if (colspan && colspan > 1) {
                        if (colspan > 2) {
                            $cell.attr('colspan', colspan - 1);
                        } else {
                            $cell.removeAttr('colspan');
                        }
                    } else {
                        $cell.remove();
                    }
                }
            });
            
            self.triggerChange();
        },

        
        /**
         * Merge a cell with the cell to the right.
         * @param  {Element|jQuery} cell A table cell.
         * @return {[type]}      [description]
         */
        colMerge: function(cell) {
            var canMerge;
            var $cell;
            var $table;
            var cellRight;
            var colspan;
            var pos;
            var self;
            
            self = this;
            cell = cell || self.selectedGet();
            $cell = $(cell);
            colspan = $cell[0].colSpan;
            $table = $cell.closest('table');
            
            self.tableAnalyze($table);
            canMerge = self.checkMerge($cell);

            if (canMerge.col) {

                // Find the actual cell we are merging with
                pos = self.lookupPosComputed($cell);
                cellRight = self.lookupCellComputed(pos.row, pos.col + colspan);
                if (!cellRight) {
                    return;
                }

                $cell.attr('colspan', colspan + cellRight.colSpan);
                self.mergeContents($cell, $(cellRight));
                $(cellRight).remove();
            }
        },


        /**
         * Return the number of columns in the table.
         *
         * @param {Element} [cell=selected cell]
         * A cell (td or th) within the table.
         * If not specified, defaults to the last clicked cell.
         */
        colCount: function(cell) {
            var $cell;
            var count;
            var self;
            var $table;
            self = this;
            $cell = cell ? $(cell) : self.selectedGet();
            $table = $cell.closest('table');

            count = 0;
            $table.find('> * > tr').each(function() {
                var $tr;
                var cellCount;
                $tr = $(this);
                cellCount = 0;
                $tr.find('> td, > th').each(function(cellIndex, cell) {
                    cellCount += cell.colSpan;
                });
                // cellCount = $tr.find('> td, > th').length;
                count = Math.max(count, cellCount);
            });
            
            return count;
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
            var $cell;
            var self;
            self = this;
            options = options || {};
            $cell = options.cell ? $(options.cell) : self.selectedGet();

            $cell.html(content);
            
            self.triggerChange();
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
            var $cell;
            var self;
            self = this;
            options = options || {};
            $cell = options.cell ? $(options.cell) : self.selectedGet();
            return options.html ? $cell.html() : $cell.contents();
        },

        
        /**
         * Set or clear readonly mode for the table editor.
         * 
         * @param  {Boolean} [flag=false]
         * True to make the table editor readonly. False to turn off readonly mode.
         * Default is false.
         */
        readOnlySet: function(flag) {
            var self;
            self = this;
            self.readOnly = Boolean(flag);
        },


        /**
         * Determine if the editor is in readonly mode.
         * @return {Boolean} True if the table editor is in readonly mode.
         */
        readOnlyGet: function() {
            var self;
            self = this;
            return Boolean(self.readOnly)
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
            var $cloned;
            var self;
            self = this;
            options = options || {};

            // Remove the active class first
            self.activeClear();
            
            $cloned = $('<div/>').append( self.$table.clone() );

            // Remove any empty 'class' attributes because jquery leaves those around after setting and removing the active classes
            $cloned.find('[class=""]').removeAttr('class');
            return options.html ? $cloned.html() : self.$table;
        },
        
        
        /**
         * Analyze the table, and update data structures to represent the table cells
         * to account for COLSPAN and ROWSPAN attributes.
         * These data structures will be used by other functions to locate cells by row and column:
         *
         * self.matrix = an array where you can start from the calculated (row,col) position and
         * find the cell that corresponds to that position.
         * 
         * self.lookup = an object where you can take a cell's (row,col), and find
         * the calculated (col) position which accounts for ROWSPAN.
         * For example, for '<tr><td>First</td><td>Second</td></tr>', if you take the second cell,
         * and pass in the position row:2, col:2, then look it up using self.lookup[2][2], that
         * would give you the calculated column (which might be (2) because of a ROWSPAN on
         * other cells).
         *
         * @param {Element|jQuery} [table]
         * The table to analyze. If not specified uses the main table of the editor.
         * However since the table editor supports nested tables, the table should be specified.
         */
        tableAnalyze: function(table) {
            
            var lookup;
            var matrix;
            var self;
            var $table;
            self = this;
            $table = $(table || self.$table);
            
            // Create a matrix to start from a calculated row/column and lead to a cell.
            self.matrix = matrix = [];
            
            // Create a lookup table to start from a (row/col) and lead to a calculated (row/col) 
            self.lookup = lookup = {};
            
            // Loop through all the rows in the table
            $table.find('> * > tr').each(function(rowIndex, tr){
                
                // Create new row in the lookup table
                lookup[rowIndex] = [];
                
                // Loop through all the cells in the row
                $.each(tr.cells, function(cellIndex, cell) {
                    
                    var rowSpan;
                    var colFirst;
                    var colSpan;
                    var k;
                    var l;
                    
                    rowSpan = cell.rowSpan || 1;
                    colSpan = cell.colSpan || 1;
                    
                    // Initalize the matrix in this row if needed.
                    if(!matrix[rowIndex]) {
                        matrix[rowIndex] = [];
                    }
                    
                    // Find next available column in this row
                    // This will account for a rowspan carried over from previous row
                    for (k = 0; k < matrix[rowIndex].length + 1; k++) {
                        if (! matrix[rowIndex][k]) {
                            colFirst = k;
                            break;
                        }
                    }
                    
                    // Save the calculated column in the lookup table
                    lookup[rowIndex][cellIndex] = colFirst;

                    // Create a matrix entry for each row that this cell covers.
                    // If the cell has a ROWSPAN then create an entry for this row
                    // plus additional rows.
                    for (k = rowIndex; k < rowIndex + rowSpan; k++) {
                        
                        // Create a row in the matrix if it doesn't exist already
                        if (!matrix[k]) {
                            matrix[k] = [];
                        }
                        
                        // If the cell has a COLSPAN then create an entry for this column
                        // plus additional columns
                        for (l = colFirst; l < colFirst + colSpan; l++) {
                            matrix[k][l] = cell ; // {cell: cell, rowIndex: rowIndex};
                        }
                    }
                }); // each cell
            }); // each TR
        },
        

        /**
         * Normalize all the rows in a table so they are not missing any columns.
         *
         * @param {Element} [table=selected cell]
         * The table to normalize, or a cell (td or th) within the table.
         * If not specified, defaults to the last clicked cell.
         */
        tableNormalize: function(table) {
            var colCount;
            var self;
            var $table;
            self = this;
            $table = $(table || self.selectedGet()).closest('table');
            
            // Get total number of columns across the entire table
            colCount = self.colCount($table);
            
            $table.find('> * > tr').each(function(rowIndex, tr) {
                var i;
                var $tds;
                var $tr;
                $tr = $(this);
                $tds = $tr.find('> td, > th');

                // If this row is missing cells add them first
                for (i = $tds.length; i < colCount; i++) {
                    // Determine if this cell is covered by a rowspan from a previous row
                    if (!self.matrix[rowIndex][i]) {
                        // There is no cell for this row,col so create one
                        $tr.append( self.cellCreate() );
                    }
                }
            });
        },


        /**
         * Given a table cell, return the (row,col) within the table.
         * Note this does not account for ROWSPAN and COLSPAN.
         * @param  {Element|jQuery} cell A table cell (TD or TH element).
         * @return {Object} An object containing (row,col) numbers.
         */
        lookupPos: function(cell) {
            
            // In case cell was passed in as jquery, convert back to an element
            cell = $(cell)[0];
            
            return {row: cell.parentNode.rowIndex, col: cell.cellIndex};
        },
        
        
        /**
         * Given a table cell, return the computed (row,col) within the table.
         * Note you must call tableAnalyze() to set up the lookup tables before calling this function.
         * @param  {Element|jQuery} cell Table cell.
         * @return {Object} An object containing {row,col} numbers.
         */
        lookupPosComputed: function(cell) {
            var pos;
            var self;
            self = this;
            
            // In case cell was passed in as jquery, convert back to an element
            cell = $(cell)[0];
            
            pos = {};
            
            // Start with the row,col based on the HTML
            pos = self.lookupPos(cell);
            
            // Look up the calculated column to account for any ROWSPAN on previous rows
            if (self.lookup[pos.row]) {
                pos.col = self.lookup[pos.row][pos.col];
            }
            
            return pos;
        },
        
        
        /**
         * Given a (row,col) in the table, return the corresponding table cell.
         * Note the (row,col) must be computed - already accounting for any
         * ROWSPAN or COLSPAN.
         * Note you must call tableAnalyze() to set up the lookup tables before calling this function.
         * 
         * @param  {Number} row The table cell row.
         * @param  {Number} col The table cell column.
         * @return {Element} The table cell element, or undefined.
         */
        lookupCellComputed: function(row, col) {
            var self;
            self = this;
            return self.matrix[row] ? self.matrix[row][col] : undefined;
        },


        /**
         * Unmerge a cell - remove the ROWSPAN and COLSPAN but add back empty cells to keep the same table structure.
         * @param  {Element|jQuery} cell The table cell to be unmerged.
         */
        unMerge: function(cell) {
            
            var self;
            var $cell;
            var $table;
            var pos;
            var rowNumber;
            var rowspan;
            var colspan;
            var colNumber;
            var gotFirstCell;
            var $tr;
            var $insertPoint;
            
            self = this;
            cell = cell || self.selectedGet();
            $cell = $(cell);
            $tr = $cell.closest('tr');
            $table = $cell.closest('table');
            
            self.tableAnalyze($table);
            
            rowspan = $cell[0].rowSpan;
            colspan = $cell[0].colSpan;
            
            // Get the position for this table cell (accounting for rowspan and colspan)
            pos = self.lookupPosComputed($cell);
            
            // Loop through all the rows that this cell was covering
            for (rowNumber = pos.row; rowNumber < pos.row + rowspan; rowNumber++) {

                // Find the point after which we should insert cells.
                // We cannot just count the TD elements in the current row, because previous rows might have rowspans
                // that span into this row, so there might be fewer TD elements than actual columns in the table.
                $insertPoint = undefined;
                $tr.find('>td,>th').each(function(cellIndex, cell) {
                    var posCell;
                    posCell = self.lookupPosComputed(cell);
                    if (posCell.col <= pos.col) {
                        $insertPoint = $(cell);
                    }
                });
                
                // Now create one or more cells to make up for the rowspan and colspan being removed
                for (colNumber = pos.col; colNumber < pos.col + colspan; colNumber++) {
                    
                    // Skip the very first cell across all rows, because the original cell
                    // will remain (after we remove the rowspan and colspan attributes)
                    if (!gotFirstCell) {
                        gotFirstCell = true;
                    } else {
                        // Add a blank cell at the proper position
                        if ($insertPoint) {
                            $insertPoint.after( self.cellCreate() );
                        } else {
                            // If we didn't find an insert point, add the blank cell at the beginning of the row
                            $tr.prepend( self.cellCreate() );
                        }
                    }
                }
                
                $tr = $tr.next();
            }
            
            // Finally remove the rowspan and colspan for this cell
            $cell.removeAttr('rowspan').removeAttr('colspan');
        },
        
        
        /**
         * Check if a cell can be merged.
         * Note that tableAnalyze() must have been called before this function.
         * @param  {Element|jQuery} cell The table cell to be checked.
         * @return {Object}
         * An object with the following properties:
         * row (can be merged with the row below)
         * col (can be merged with the column to the right)
         * unmerge (can be unmerged)
         */
        checkMerge: function(cell) {
            var $cell;
            var cell2;
            var colspan;
            var pos;
            var pos2;
            var rowspan;
            var self;
            var status;

            self = this;
            $cell = $(cell);
            rowspan = $cell[0].rowSpan;
            colspan = $cell[0].colSpan;

            status = {
                unmerge: Boolean(rowspan > 1 || colspan > 1),
                row: false,
                col: false
            };
        
            pos = self.lookupPosComputed($cell);
            
            // Determine if this cell can be merged with the one below
            cell2 = self.lookupCellComputed(pos.row + rowspan, pos.col);
            if (cell2) {
                // Make sure cell to be merged starts in the same column
                // and has same width (including colspan)
                pos2 = self.lookupPosComputed(cell2);
                if (pos.col === pos2.col && cell2.colSpan === colspan) {
                    status.row = true;
                }
            }

            // Determine if this cell can be merged with the one to the right
            cell2 = self.lookupCellComputed(pos.row, pos.col + colspan);
            if (cell2) {
                // Make sure cell to be merged starts in the same row
                // and has same height (including rowspan)
                pos2 = self.lookupPosComputed(cell2);
                if (pos.row === pos2.row && cell2.rowSpan === rowspan) {
                    status.col = true;
                }
            }
            
            return status;
        },
        
        
        /**
         * Merge the contents of one cell into another.
         * Adds a newline if both cells have existing content.
         * @param  {Element|jQuery} $cell
         * The cell that will remain after the merge.
         * @param  {Element|jQuery} $cellToMerge
         * The cell to be merged (which will be removed after the merge).
         */
        mergeContents: function ($cell, $cellToMerge) {
            var html;
            var htmlToMerge;
            var re;
            
            // In case a plain element is passed in convert to jQuery
            $cell = $($cell);
            $cellToMerge = $($cellToMerge);
            
            // Get the content of the cells so we can check if it is empty
            html = $cell.html();
            htmlToMerge = $cellToMerge.html();
            
            // Check if the cell is empty (contains just space)
            re = /^\s*$|^&nbsp;$/;
            html = html.replace(re, '');
            htmlToMerge = htmlToMerge.replace(re, '');

            if (htmlToMerge) {
                if (html) {
                    // If both cells contain content, then make sure we get a line break between
                    $cell.append('<br/>')
                } else {
                    // If original cell is empty, remove spaces if necessary
                    $cell.empty();
                }
                
                // Move the content of the merged cell into the original cell
                $cell.append( $cellToMerge.contents() );
            }
        },
        
        
        /**
         * Trigger a change event. To be used whenever the table is modified.
         */  
        triggerChange: function() {
            var self;
            self = this;
            self.$table.trigger('tableEditorChange', [self]);
        }
    };
    
    return module;

});

// Set filename for debugging tools to allow breakpoints even when using a cachebuster
//# sourceURL=tableEditor.js
