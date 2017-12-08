package com.psddev.cms.hunspell;

import com.psddev.dari.db.Record;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public class HunspellDictionary extends Record {

    @Required
    private String name;

    @Required
    private Locale locale;

    private Set<String> words;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public Set<String> getWords() {
        if (words == null) {
            words = new TreeSet<>();
        }
        return words;
    }

    public void setWords(Set<String> words) {
        this.words = words;
    }

    @Override
    protected void beforeCommit() {
        words = new TreeSet<>(getWords());
    }

    @Override
    protected void afterSave() {
        HunspellSpellChecker.HUNSPELLS.invalidateAll();
    }
}
