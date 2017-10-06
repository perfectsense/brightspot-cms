package com.psddev.cms.hunspell;

import com.google.common.base.Preconditions;
import com.psddev.dari.db.Modification;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StringUtils;
import com.psddev.dari.util.UuidFormatException;
import com.psddev.dari.util.UuidUtils;

import java.util.Locale;

public abstract class AbstractHunspellDictionary extends Record implements HunspellDictionary {


    /**
     * Returns a SpellCheckerDictionary for a {@link Locale} when the
     * Locale's {@link Locale#PRIVATE_USE_EXTENSION} represents a Brightspot
     * SpellCheckerDictionary object.
     *
     * @param name
     *        Can't be {@code null}.
     *
     * @return {@code null} if the provided Locale's extension does not
     *         contain a scoped {@link java.util.UUID} of a SpellCheckerDictionary
     *         object.
     */
    static HunspellDictionary forName(String name) {

        Preconditions.checkArgument(!ObjectUtils.isBlank(name));

        Locale locale = Locale.forLanguageTag(name);

        String extension = locale.getExtension('x');

        if (StringUtils.isBlank(extension)) {
            return null;
        }

        if (!extension.startsWith(LOCALE_EXTENSION_PREFIX)) {
            return null;
        }

        extension = extension.substring(4).replaceAll("\\-", "");

        if (StringUtils.isBlank(extension)) {
            return null;
        }

        try {
            return Query.findById(HunspellDictionary.class, UuidUtils.fromString(extension));
        } catch (UuidFormatException e) {
            return null;
        }
    }

    /**
     * Provides a method to acquire a {@link Locale} representing the
     * SpellCheckerDictionary.
     */
    class LocaleModification extends Modification<AbstractHunspellDictionary> {

        /**
         * Returns a {@link Locale} based on {@link HunspellDictionary#getBaseLocale()}
         * or {@link Locale#getDefault()} with the {@link Locale#PRIVATE_USE_EXTENSION}
         * containing the {@link com.psddev.dari.db.State#id id} of the modified
         * object.
         *
         * @return Never {@code null}.
         */
        public Locale getDictionaryLocale() {

            HunspellDictionary hunspellDictionary = getOriginalObject();

            Locale baseLocale = hunspellDictionary.getBaseLocale();

            if (baseLocale == null) {
                baseLocale = Locale.getDefault();
            }

            StringBuilder extBuilder = new StringBuilder(getState().getId().toString().replaceAll("\\-", ""));

            for (int i = 30; i > 1; i -= 2) {
                extBuilder.insert(i, '-');
            }

            String extension = extBuilder.toString();

            // add private-use extension of the form bsp-00-00-01-58-70-3f-db-31-a5-7a-77-ff-0b-4f-00-00
            return new Locale.Builder()
                .setLocale(baseLocale)
                .clearExtensions()
                .setExtension('x', LOCALE_EXTENSION_PREFIX + extension)
                .build();
        }
    }
}
