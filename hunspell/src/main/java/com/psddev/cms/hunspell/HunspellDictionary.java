package com.psddev.cms.hunspell;

import com.psddev.cms.db.ToolUi;
import com.psddev.dari.db.Record;
import com.psddev.dari.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class HunspellDictionary extends Record {

    private transient String oldName;
    private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    @ToolUi.ReadOnly
    private String name;

    @Required
    @Indexed(unique = true)
    private Locale locale;

    private List<String> words;

    public String getName() {
        return name;
    }

    public Locale getLocale() {
        return Optional.ofNullable(locale).orElse(DEFAULT_LOCALE);
    }

    public List<String> getWords() {
        if (words == null) {
            words = new ArrayList<>();
        }
        return words;
    }

    @Override
    protected void beforeSave() {
        if (oldName == null) {
            oldName = name;
        }
    }

    @Override
    protected void beforeCommit() {
        name = HunspellSpellChecker.DICTIONARY_BASE_NAME + "_" + getLocale().toString();
        words = new HashSet<>(words).stream().sorted().collect(Collectors.toList());

        if (!StringUtils.equals(oldName, name)) {
            HunspellSpellChecker.inValidateDictionary(oldName);
        }
        HunspellSpellChecker.inValidateDictionary(name);
    }

    @Override
    protected void afterCreate() {
        locale = DEFAULT_LOCALE;
    }
}
