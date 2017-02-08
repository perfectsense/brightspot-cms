package com.psddev.cms.db;

import com.google.common.base.Preconditions;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.util.TimeZone;
import com.psddev.cms.tool.AuthenticationFilter;
import com.psddev.dari.util.PageContextFilter;
import com.psddev.dari.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Locale;

public final class Localization {

    public static final String NOT_AVAILABLE_KEY = "label.notAvailable";

    public static final String DATE_AND_TIME_SKELETON = "_dateAndTime";

    public static final String DATE_ONLY_SKELETON = "_dateOnly";

    public static final String TIME_ONLY_SKELETON = "_timeOnly";

    private static final String SKELETON_DATE_KEY = "skeleton.date";
    private static final String SKELETON_DATE_WITH_YEAR_KEY = "skeleton.dateWithYear";
    private static final String SKELETON_TIME_KEY = "skeleton.time";

    /**
     * Localizes the given {@code key} using the given {@code locale} and
     * {@code context}.
     *
     * <p>If there's isn't text associated with the given {@code key},
     * returns the given {@code defaultText}.</p>
     *
     * @param locale Nullable. Default is {@link Locale#getDefault()}.
     * @param context Nullable.
     * @param key Nonnull.
     * @param defaultText Nullable.
     * @return Nonnull.
     */
    public static String text(Locale locale, Object context, String key, String defaultText) {
        LocalizationContext localizationContext = context instanceof LocalizationContext
                ? (LocalizationContext) context
                : new LocalizationContext(context, null);

        Locale defaultLocale = Locale.getDefault();

        if (locale == null) {
            locale = defaultLocale;
        }

        boolean firstTry = true;

        while (true) {
            String localized = localizationContext.text(locale, locale, key);

            if (localized == null && !defaultLocale.equals(locale)) {
                localized = localizationContext.text(defaultLocale, locale, key);
            }

            if (localized == null) {
                Locale usLocale = Locale.US;
                String usLanguage = usLocale.getLanguage();

                if (!usLanguage.equals(defaultLocale.getLanguage())
                        && !usLanguage.equals(locale.getLanguage())) {

                    localized = localizationContext.text(usLocale, locale, key);
                }
            }

            if (localized != null) {
                return localized;
            }

            if (firstTry) {
                firstTry = false;
                ObjectTypeResourceBundle.invalidateInstances();

            } else if (StringUtils.isBlank(defaultText)) {
                return localizationContext.missingText(key);

            } else {
                return defaultText;
            }
        }
    }

    /**
     * Localizes the given {@code key} using the given {@code locale} and
     * {@code context}.
     *
     * @param locale Nullable. Default is {@link Locale#getDefault()}.
     * @param context Nullable.
     * @param key Nonnull.
     * @return Nonnull.
     */
    public static String text(Locale locale, Object context, String key) {
        return text(locale, context, key, null);
    }

    /**
     * Localizes the given {@code timeMillis} in the given {@code timeZoneId}
     * using the given {@code locale} and {@code skeleton}.
     *
     * @param locale Nullable. Default is {@link Locale#getDefault()}.
     * @param timeZoneId Nullable. Default is {@link java.util.TimeZone#getDefault()}.
     * @param timeMillis If negative, returns the localized text associated
     *                   with {@link #NOT_AVAILABLE_KEY}.
     * @param skeleton Nonnull.
     * @return Nonnull.
     */
    public static String date(Locale locale, String timeZoneId, long timeMillis, String skeleton) {
        Preconditions.checkNotNull(skeleton);

        if (locale == null) {
            locale = Locale.getDefault();
        }

        if (timeMillis < 0) {
            return text(locale, null, NOT_AVAILABLE_KEY);
        }

        if (timeZoneId == null) {
            timeZoneId = java.util.TimeZone.getDefault().getID();
        }

        Date time = new Date(timeMillis);

        if (DATE_AND_TIME_SKELETON.equals(skeleton)) {
            skeleton = dateSkeleton(locale, timeMillis) + " " + text(locale, null, SKELETON_TIME_KEY);

        } else if (DATE_ONLY_SKELETON.equals(skeleton)) {
            skeleton = dateSkeleton(locale, timeMillis);

        } else if (TIME_ONLY_SKELETON.equals(skeleton)) {
            skeleton = text(locale, null, SKELETON_TIME_KEY);
        }

        DateFormat format = DateFormat.getInstanceForSkeleton(skeleton, locale);
        format.setTimeZone(TimeZone.getTimeZone(timeZoneId));
        return format.format(new Date(timeMillis));
    }

    private static String dateSkeleton(Locale locale, long timeMillis) {
        return Instant.now().atOffset(ZoneOffset.UTC).getYear() == Instant.ofEpochMilli(timeMillis).atOffset(ZoneOffset.UTC).getYear()
                ? text(locale, null, SKELETON_DATE_KEY)
                : text(locale, null, SKELETON_DATE_WITH_YEAR_KEY);
    }

    /**
     * Localizes the given {@code key} using the current user's preferred
     * locale and the given {@code context}.
     *
     * <p>If there's isn't text associated with the given {@code key},
     * returns the given {@code defaultText}.</p>
     *
     * @param context Nullable.
     * @param key Nonnull.
     * @param defaultText Nullable.
     * @return Nonnull.
     */
    public static String currentUserText(Object context, String key, String defaultText) {
        ToolUser user = currentUser();
        Locale locale = user != null ? user.getLocale() : null;
        return text(locale, context, key, defaultText);
    }

    /**
     * Localizes the given {@code key} using the current user's preferred
     * locale and the given {@code context}.
     *
     * @param context Nullable.
     * @param key Nonnull.
     * @return Nonnull.
     */
    public static String currentUserText(Object context, String key) {
        return currentUserText(context, key, null);
    }

    private static ToolUser currentUser() {
        HttpServletRequest request = PageContextFilter.Static.getRequestOrNull();
        return request != null ? AuthenticationFilter.Static.getUser(request) : null;
    }

    /**
     * Localizes the given {@code timeMillis} in the current user's time zone
     * using the current user's preferred locale and the given {@code format}.
     *
     * @param timeMillis If negative, returns the localized text associated
     *                   with {@link #NOT_AVAILABLE_KEY}.
     * @param format Nonnull.
     * @return Nonnull.
     */
    public static String currentUserDate(long timeMillis, String format) {
        Preconditions.checkNotNull(format);

        ToolUser user = currentUser();
        Locale locale;
        String timeZoneId;

        if (user != null) {
            locale = user.getLocale();
            timeZoneId = user.getTimeZone();

        } else {
            locale = null;
            timeZoneId = null;
        }

        return date(locale, timeZoneId, timeMillis, format);
    }

    // Prevent instantiation.
    private Localization() {
    }
}
