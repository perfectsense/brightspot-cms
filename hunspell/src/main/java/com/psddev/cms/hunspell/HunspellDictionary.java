package com.psddev.cms.hunspell;

import java.util.Locale;
import java.util.Set;

/**
 * Additional dictionary for {@link HunspellSpellChecker HunspellSpellCheckers}.
 */
public interface HunspellDictionary {

    String LOCALE_EXTENSION_PREFIX = "bsp-";

    Locale getBaseLocale();

    boolean isSupported(Locale locale);

    Set<String> getAllWords();
}
