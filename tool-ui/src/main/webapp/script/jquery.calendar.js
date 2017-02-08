define([ 'moment-with-locales', 'moment-timezone-with-data-2010-2020' ], function (moment) {

(function($, win, undef) {

var options = {
    'dayLabels': [ 'Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat' ],
    'monthLabels': [ 'January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December' ]
};

var padZero = function(value) {
    return value < 10 ? '0' + value : value;
};

var getCalendar;

var updateCalendarView = function(viewDate) {

    var $calendar = getCalendar();
    var selectedDate = $calendar.data('selectedDate');
    $calendar.data('viewDate', viewDate);

    var viewMonth = viewDate.month();
    $calendar.find('> .monthYear > .month').val(viewMonth);
    $calendar.find('> .monthYear > .year').val(viewDate.year());

    var dayDate = viewDate.clone();
    dayDate.date(1);
    dayDate.date(dayDate.date() - dayDate.day());
    $calendar.find('> .week > .day').each(function() {

        var $day = $(this);
        $day.data('date', dayDate.clone());

        var dayNumber = dayDate.date();
        $day.text(dayNumber);

        if (viewMonth === dayDate.month()) {
            $day.removeClass('offMonth');
        } else {
            $day.addClass('offMonth');
        }

        if (dayDate.month() === selectedDate.month() && dayNumber === selectedDate.date()) {
            $day.addClass('selected');
        } else {
            $day.removeClass('selected');
        }

        dayDate.date(dayNumber + 1);
    });
};

function formatDate(date) {
    var $html = $('html');
    return moment(date).locale($html.attr('lang')).tz($html.attr('data-time-zone')).format('llll');
}

var updateInput = function() {
    var $calendar = getCalendar();
    var selectedDate = $calendar.data('selectedDate');
    var $input = $calendar.data('$input');

    $input.val(selectedDate.valueOf());
    $input.change();
    $calendar.data('$calendarButton').toggleClass('calendarButton-empty', !$input.val());
    $calendar.data('$calendarButton').text(formatDate(selectedDate));
};

getCalendar = function() {

    var $calendar = $('#calendar');
    if ($calendar.length === 0) {

        $calendar = $('<div/>', { 'id': 'calendar' });

        // Month and year.
        var $monthYear = $('<div/>', { 'class': 'monthYear' });
        var $month = $('<select/>', {
            'class': 'month',
            'change': function() {
                var newDate = $calendar.data('viewDate').clone();
                newDate.date(1);
                newDate.month($month.val());
                updateCalendarView(newDate);
            }
        });

        $monthYear.append($month);

        $.each(options.monthLabels, function(index, value) {
            $month.append($('<option/>', {
                'value': index,
                'text': value
            }));
        });

        var $year = $('<input/>', {
            'type': 'text',
            'class': 'year',
            'bind': {
                'input': function() {
                    var newYear = parseInt($year.val(), 10) || 0;
                    if (newYear < 1900) {
                        $year.addClass('invalid');
                    } else {
                        var newDate = $calendar.data('viewDate').clone();
                        newDate.date(1);
                        newDate.year(newYear);
                        updateCalendarView(newDate);
                        $calendar.data('selectedDate').year(newYear);
                        $year.removeClass('invalid');
                    }
                }
            }
        });

        $monthYear.append(' ');
        $monthYear.append($year);

        // Month navigation.
        var $previousButton = $('<span/>', { 'class': 'previousButton' });
        $previousButton.bind('click.calendar', function() {
            var previous = $calendar.data('viewDate').clone();
            previous.date(1);
            previous.month(previous.month() - 1);
            updateCalendarView(previous);
        });

        var $nextButton = $('<span/>', { 'class': 'nextButton' });
        $nextButton.bind('click.calendar', function() {
            var next = $calendar.data('viewDate').clone();
            next.date(1);
            next.month(next.month() + 1);
            updateCalendarView(next);
        });

        // Days.
        var $dayLabels = $('<div/>', { 'class': 'dayLabels' });
        $.each(options.dayLabels, function(day, label) {
            $dayLabels.append($('<span/>', { 'class': 'day day' + day, 'text': label }));
        });

        $calendar.append($monthYear);
        $calendar.append($previousButton);
        $calendar.append($nextButton);
        $calendar.append($dayLabels);

        var week, $week, day;
        for (week = 0; week < 6; ++ week) {
            $week = $('<div/>', { 'class': 'week week' + week });
            for (day = 0; day < 7; ++ day) {
                $week.append($('<span/>', { 'class': 'day day' + day }));
            }
            $calendar.append($week);
        }

        // Time.
        var $time = $('<div/>', { 'class': 'time' });

        if (COMMON_TIMES.length > 0) {
            var $timeSelect = $('<select/>', {
                'class': 'timeSelect',

                'change': function() {
                    var $selected = $(this).find(':selected'),
                            custom = $selected.prop('value') === '_custom',
                            selectedDate;

                    $hour.add($minute).add($meridiem).toggle(custom);

                    if (!custom) {
                        selectedDate = $calendar.data('selectedDate');

                        selectedDate.hour(parseInt($selected.attr('data-hour'), 10));
                        selectedDate.minute(parseInt($selected.attr('data-minute'), 10));
                    }
                }
            });

            $.each(COMMON_TIMES, function() {
                $timeSelect.append($('<option/>', {
                    'text': this.displayName,
                    'data-hour': this.hour,
                    'data-minute': this.minute
                }));
            });

            $timeSelect.append($('<option/>', {
                'text': 'Custom:',
                'value': '_custom'
            }));
        }

        var $meridiem = $('<select/>', { 'class': 'meridiem' });
        var $hour = $('<select/>', { 'class': 'hour' });
        var hourChange = function() {
            $calendar.data('selectedDate').hour(($meridiem.val() === 'PM' ? 12 : 0) + parseInt($hour.val(), 10));
        };

        $meridiem.append($('<option/>', { 'text': 'AM', 'value': 'AM' }));
        $meridiem.append($('<option/>', { 'text': 'PM', 'value': 'PM' }));
        $meridiem.change(hourChange);

        var hour, hourString;
        for (hour = 0; hour < 12; ++ hour) {
            hourString = hour === 0 ? 12 : padZero(hour);
            $hour.append($('<option/>', { 'text': hourString, 'value': hour }));
        }
        $hour.change(hourChange);

        var $minute = $('<select/>', { 'class': 'minute' });
        var minute, minuteString;
        for (minute = 0; minute < 60; ++ minute) {
            minuteString = padZero(minute);
            $minute.append($('<option/>', { 'text': minuteString, 'value': minute }));
        }
        $minute.change(function() {
            $calendar.data('selectedDate').minute($minute.val());
        });

        $time.append($timeSelect);
        $time.append($hour);
        $time.append($minute);
        $time.append($meridiem);
        $calendar.append($time);

        // For changing the input.
        var $actions = $('<div/>', {
            'class': 'calendar_actions'
        });

        $actions.append($('<span/>', {
            'class': 'calendar_set',
            'text': 'Set',
            'click': function() {
                var $input = $calendar.data('$input');

                updateInput();
                $calendar.popup('close');
            }
        }));

        $actions.append($('<span/>', {
            'class': 'calendar_clear',
            'text': 'Clear',
            'click': function() {
                var $input = $calendar.data('$input');

                $calendar.data('selectedDate', moment().tz($('html').attr('data-time-zone')));
                $input.val('');
                $input.change();
                $calendar.data('$calendarButton').toggleClass('calendarButton-empty', !$input.val());
                $calendar.data('$calendarButton').text($input.attr('placeholder') || $input.attr('data-emptylabel') || 'N/A');
                $calendar.popup('close');
            }
        }));

        $calendar.append($actions);

        $("body").append($calendar);
        $calendar.popup();
        $calendar.popup('container').attr('id', 'calendarPopup');
        $calendar.popup('close');

        $calendar.find('.day').click(function() {
            var date = $(this).data('date');
            $calendar.data('selectedDate', date);
            $calendar.find('.timeSelect').change();
            updateCalendarView(date);
        });
    }

    return $calendar;
};

$.plugin2('calendar', {
    '_create': function(input) {
        var $input = $(input);
        var $calendarButton = $('<span/>', {
            'class': 'calendarButton'
        });

        var inputValue = parseInt($input.val(), 10);

        if (inputValue) {
            $calendarButton.text(formatDate(moment(inputValue).tz($('html').attr('data-time-zone'))));

        } else {
            $calendarButton.text($input.attr('placeholder') || $input.attr('data-emptylabel') || 'N/A');
        }

        $input.bind('input-disable', function(event, disable) {
            $calendarButton.toggleClass('state-disabled', disable);
        });

        $calendarButton.toggleClass('calendarButton-empty', !$input.val());
        $calendarButton.click(function() {
            if ($calendarButton.is('.state-disabled')) {
                return;
            }

            var $calendar = getCalendar();

            $calendar.data('$input', $input);
            $calendar.data('$calendarButton', $calendarButton);

            // Switch to the correct month using the input date.
            var inputValue = parseInt($input.val(), 10);
            var inputDate = (inputValue ? moment(inputValue) : moment()).tz($('html').attr('data-time-zone'));
            $calendar.data('selectedDate', inputDate);
            updateCalendarView(inputDate);

            // Set the time.
            var hour = inputDate.hour();
            var meridiem = 'AM';
            if (hour >= 12) {
                hour -= 12;
                meridiem = 'PM';
            }
            $calendar.find('select.hour').val(hour);
            $calendar.find('select.minute').val(inputDate.minute());
            $calendar.find('select.meridiem').val(meridiem);
            $calendar.find('select.timeSelect').change();

            // Update empty label.
            $calendar.find('.empty').text($input.attr('placeholder') || $input.attr('data-emptylabel') || 'N/A');

            // Open the calendar in a popup.
            $calendar.popup('source', $calendarButton);
            $calendar.popup('open');
        });

        $input.hide();
        $input.after($calendarButton);
    }
});

}(jQuery, window));

    return {};
});
