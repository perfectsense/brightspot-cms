package com.psddev.cms.tool;

import com.google.common.base.Preconditions;
import com.psddev.dari.db.Record;
import com.psddev.dari.util.UuidUtils;

import java.util.Locale;
import java.util.UUID;

public class MachineTranslations extends Record {

    /**
     * Creates an unique ID based on the given {@code baseName} and
     * {@code locale}'s language.
     *
     * @param baseName
     *        May be {@code null}.
     *
     * @param locale
     *        Can't be {@code null}.
     *
     * @return Never {@code null}.
     */
    public static UUID createId(String baseName, Locale locale) {
        Preconditions.checkNotNull(locale);

        StringBuilder name = new StringBuilder();

        name.append(MachineTranslations.class.getName());
        name.append('/');

        if (baseName != null) {
            name.append(baseName);
        }

        name.append('/');
        name.append(locale.getLanguage());

        return UuidUtils.createVersion3Uuid(name.toString());
    }
}
