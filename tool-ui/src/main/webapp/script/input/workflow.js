define([
    'jquery',
    'bsp-utils',
    'v3/color-utils' ],

function($, bsp_utils, color_utils) {
    var $win = $(window);
    var getTransitionColor;

    (function() {
        var hue;

        getTransitionColor = function($transition) {
            var transitionColor = $.data($transition[0], 'workflow-transitionColor');

            if (!transitionColor) {
                hue = color_utils.changeHue(hue);
                transitionColor = color_utils.generateFromHue(hue);
                $.data($transition[0], 'workflow-transitionColor', transitionColor);
            }

            return transitionColor;
        };
    })();

    bsp_utils.onDomInsert(document, '.workflow', {
        'insert': function(textarea) {
            var $textarea = $(textarea),
                    definition = $.parseJSON($textarea.val()) || { },
                    $visual,
                    drawArrow,
                    $arrows,
                    $transitions = { },
                    addTransitionTarget,
                    appendTransitionAdd,
                    addState,
                    nextStateId = 0,
                    $stateAdd,
                    $stateInitial,
                    $stateFinal;

            $visual = $('<div/>', {
                'class': 'workflowVisual'
            });

            drawArrow = function(context, color, $source, targetX, targetY) {
                var sourcePosition = $source.position(),
                        sourceX = sourcePosition.left,
                        sourceY,
                        sourceControlY,
                        sourceControlX,
                        targetControlY,
                        targetControlX,
                        $target,
                        targetPosition,
                        sourceDirection,
                        targetDirection,
                        arrowSize;

                if (typeof targetY === 'undefined') {
                    $target = targetX;
                    targetPosition = $target.position();
                    targetX = targetPosition.left;
                    targetY = targetPosition.top;
                }

                if (sourceX > targetX) {
                    sourceY = sourcePosition.top + $source.outerHeight() * 0.9;
                    sourceDirection = -1;
                    targetDirection = 1;

                    if ($target) {
                        targetX += $target.outerWidth();
                        targetY += $target.outerHeight() * 0.9;
                    }

                } else {
                    sourceX += $source.outerWidth();
                    sourceY = sourcePosition.top + $source.outerHeight() * 0.1;
                    sourceDirection = 1;
                    targetDirection = -1;

                    if ($target) {
                        targetY += $target.outerHeight() * 0.1;
                    }
                }

                sourceControlX = sourceX + sourceDirection * 100;
                sourceControlY = sourceY;
                targetControlX = targetX + targetDirection * 100;
                targetControlY = targetY;

                context.strokeStyle = color;
                context.fillStyle = color;

                context.lineWidth = 1.0;
                context.beginPath();
                context.moveTo(sourceX, sourceY);
                context.bezierCurveTo(sourceControlX, sourceControlY, targetControlX, targetControlY, targetX, targetY);
                context.stroke();

                arrowSize = targetX > targetControlX ? 5 : -5;
                context.beginPath();
                context.moveTo(targetX, targetY);
                context.lineTo(targetX - 2 * arrowSize, targetY - arrowSize);
                context.lineTo(targetX - 2 * arrowSize, targetY + arrowSize);
                context.closePath();
                context.fill();

                return {
                    'sourceX': sourceX,
                    'sourceY': sourceY,
                    'targetX': targetX,
                    'targetY': targetY
                };
            };

            $visual.append($arrows = $('<canvas/>', {
                'css': {
                    'left': 0,
                    'position': 'absolute',
                    'top': 0,
                    'z-index': -1
                }
            }));

            $arrows.bind('redraw', function(event, $source, targetX, targetY) {
                var context = this.getContext('2d'),
                        width = $arrows.outerWidth(),
                        height = $arrows.outerHeight(),
                        rightBoundary,
                        bottomBoundary,
                        sourceLeftOffset,
                        sourceTopOffset;

                definition = { 'states': [ ], 'transitions': [ ] };
                context.clearRect(0, 0, $visual.width(), $visual.height());

                $visual.find('.workflowState').each(function() {
                    var $source = $(this),
                            sourceId = $source.attr('data-id'),
                            sourcePosition = $source.position(),
                            targets = $.data(this, 'workflow-targets');

                    if (!(sourceId === 'initial' ||
                            sourceId === 'final')) {

                        sourceLeftOffset = parseFloat(sourcePosition.left / width);
                        rightBoundary = parseFloat((width - $source.outerWidth()) / width);
                        sourceTopOffset = parseFloat(sourcePosition.top / height);
                        bottomBoundary = parseFloat((height - $source.outerHeight()) / height);

                        definition.states.push({
                            'id': sourceId,
                            'displayName': $source.find(':input[type="text"]').val(),
                            'name': $source.find(':input[type="hidden"]').val() || $source.find(':input[type="text"]').val(),
                            'left': sourceLeftOffset,
                            'top': sourceTopOffset
                        });


                        if (sourceLeftOffset > rightBoundary) {
                            $source.css('left', rightBoundary * 100 + '%');
                        }

                        if (sourceTopOffset > bottomBoundary) {
                            $source.css('top', bottomBoundary * 100 + '%');
                        }
                    }

                    if (targets) {
                        $.each(targets, function(index, targetData) {
                            var targetId = targetData.target,
                                    $target = $visual.find('[data-id="' + targetId + '"]'),
                                    transition,
                                    $transition,
                                    $transitionInput,
                                    transitionColor,
                                    bound,
                                    boundOffset;

                            if ($target.length === 0) {
                                return;
                            }

                            transition = sourceId + '/' + targetId;
                            $transition = $transitions[transition];

                            if (!$transition) {
                                $transition = $('<div/>', {
                                    'class': 'workflowTransition'
                                });

                                $transition.append($('<input/>', {
                                    'type': 'text',
                                    'class': 'expandable workflowTransitionName',
                                    'value': targetData.displayName || targetData.name || ''
                                }));

                                $transition.append($('<input/>', {
                                    'type': 'hidden',
                                    'value': targetData.name || ''
                                }));

                                $transition.append($('<a/>', {
                                    'class': 'workflowTransitionRemove',
                                    'text': 'Remove',
                                    'click': function() {
                                        var targets = $.data($source[0], 'workflow-targets');

                                        if (targets) {
                                            $.data($source[0], 'workflow-targets', $.map(targets, function(target) {
                                                return target.source === sourceId && target.target === targetId ? null : target;
                                            }));
                                        }

                                        $transitions[transition] = undefined;
                                        $transition.remove();
                                        $arrows.trigger('redraw');

                                        return false;
                                    }
                                }));

                                $visual.append($transition);
                                $transitions[transition] = $transition;
                                $visual.trigger('create');
                            }

                            $transitionInput = $transition.find(':input[type="text"]');
                            transitionColor = getTransitionColor($transition);
                            bound = drawArrow(context, transitionColor, $source, $target);
                            boundOffset = bound.sourceX < bound.targetX ? 0.3 : 0.7;

                            $transition.css({
                                'left': (bound.sourceX + bound.targetX - $transitionInput.outerWidth()) / 2,
                                'top': (bound.sourceY + bound.targetY - $transitionInput.outerHeight()) / 2
                            });

                            $transitionInput.css({
                                'border-color': transitionColor
                            });

                            definition.transitions.push({
                                'displayName': $transitionInput.val(),
                                'name': $transition.find(':input[type="hidden"]').val() || $transitionInput.val(),
                                'source': sourceId,
                                'target': targetId
                            });
                        });
                    }
                });

                $textarea.val(JSON.stringify(definition));

                if ($source) {
                    drawArrow(context, 'black', $source, targetX, targetY);
                }
            });

            $visual.delegate(':input', 'input', function() {
                $arrows.trigger('redraw');
            });

            addTransitionTarget = function(targetData) {
                var $source = $visual.find('[data-id="' + targetData.source + '"]'),
                        targets;

                if ($source.length > 0) {
                    targets = $.data($source[0], 'workflow-targets') || [ ];
                    targets.push(targetData);
                    $.data($source[0], 'workflow-targets', targets);
                }
            };

            appendTransitionAdd = function($state) {
                $state.append($('<div/>', {
                    'class': 'workflowTransitionAddContainer',
                    html: $('<a/>', {
                        'class': 'workflowTransitionAdd',
                        'text': 'Add ' + ($textarea.attr('data-transition-label') || 'Transition'),
                        'click': function() {
                            if ($visual.is('.workflowVisual-addingTransition')) {
                                return true;
                            }

                            $visual.addClass('workflowVisual-addingTransition');

                            $win.bind('mousemove.workflows', $.throttle(50, function(event) {
                                var visualOffset = $visual.offset();

                                $arrows.trigger('redraw', [
                                        $state,
                                        event.pageX - visualOffset.left,
                                        event.pageY - visualOffset.top ]);
                            }));

                            $win.bind('click.workflows', function(event) {
                                var $target = $(event.target).closest('.workflowState');

                                if ($target.length > 0) {
                                    addTransitionTarget({
                                        'source': $state.attr('data-id'),
                                        'target': $target.attr('data-id')
                                    });
                                }

                                $visual.removeClass('workflowVisual-addingTransition');
                                $win.unbind('.workflows');
                                $arrows.trigger('redraw');

                                return false;
                            });

                            return false;
                        }
                    })
                }));
            };

            addState = function(stateData) {
                var $state;

                $visual.append($state = $('<div/>', {
                    'class': 'workflowState',
                    'data-id': stateData.id,
                    'css': {
                        'left': Math.min((stateData.left * 100), 99.9) + '%',
                        'top': Math.min((stateData.top * 100), 99.9) + '%'
                    },
                    'mousedown': function(event) {
                        $.drag(this, event, function(event, data) {
                            data.delta = $state.position();
                            data.delta.left -= event.pageX;
                            data.delta.top -= event.pageY;

                        }, function(event, data) {
                            $state.css({
                                'left': event.pageX + data.delta.left,
                                'top': event.pageY + data.delta.top
                            });

                            $arrows.trigger('redraw');

                        }, function(event) {

                        });
                    }
                }));

                $state.append($('<input/>', {
                    'type': 'text',
                    'class': 'expandable workflowStateName',
                    'value': stateData.displayName || stateData.name || ''
                }));

                $state.append($('<input/>', {
                    'type': 'hidden',
                    'value': stateData.name || ''
                }));

                $state.append($('<a/>', {
                    'class': 'workflowStateRemove',
                    'text': 'Remove',
                    'click': function() {
                        $state.remove();
                        $arrows.trigger('redraw');

                        return false;
                    }
                }));

                appendTransitionAdd($state);
                $visual.css('height', 100 * $visual.find('.workflowState').length);
                $arrows.attr({
                    'width': $visual.width(),
                    'height': $visual.height()
                });

                return $state;
            };

            $visual.append($stateAdd = $('<a/>', {
                'class': 'workflowStateAdd',
                'text': 'Add ' + ($textarea.attr('data-state-label') || 'State'),
                'click': function() {
                    addState({
                        'id': nextStateId,
                        'left': 0.30,
                        'top': 0.01
                    });

                    ++ nextStateId;
                    $arrows.trigger('redraw');

                    return false;
                }
            }));

            $visual.append($stateInitial = $('<div/>', {
                'class': 'workflowState',
                'data-id': 'initial',
                'text': $textarea.attr('data-state-initial') || 'Initial State'
            }));

            appendTransitionAdd($stateInitial);

            $visual.append($stateFinal = $('<div/>', {
                'class': 'workflowState',
                'data-id': 'final',
                'text': $textarea.attr('data-state-final') || 'Final State'
            }));

            if (definition.states) {
                $.each(definition.states, function() {
                    var id = parseInt(this.id, 10);

                    addState(this);

                    if (id >= nextStateId) {
                        nextStateId = id + 1;
                    }
                });
            }

            $textarea.after($visual);
            $visual.trigger('create');
            $textarea.hide();

            $arrows.attr({
                'width': $visual.width(),
                'height': $visual.height()
            });

            if (definition.transitions) {
                $.each(definition.transitions, function() {
                    addTransitionTarget(this);
                });

                $arrows.trigger('redraw');
            }

            $(window).resize(function () {
                window.requestAnimationFrame(function () {
                    $arrows.attr({
                        'width': $visual.width(),
                        'height': $visual.height()
                    });

                    $arrows.trigger('redraw');
                });
            });
        }
    });
});
