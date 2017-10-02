package com.psddev.cms.hunspell;

import com.psddev.cms.db.ToolUi;
import com.psddev.dari.db.Record;
import com.psddev.dari.util.ObjectUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class HunspellDictionary extends Record {

    private String label;

    @ToolUi.ReadOnly
    private String name;

    @Required
    private Locale locale;

    private List<String> words;

    @Override
    public String getLabel() {
        return ObjectUtils.firstNonBlank(label, name, super.getLabel());
    }

    public String getName() {
        return name;
    }

    public Locale getLocale() {
        return locale;
    }

    public List<String> getWords() {
        if (words == null) {
            words = new ArrayList<>();
        }
        return words;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLocale(Locale locale) {
        this.locale = locale;
    }

    public void setWords(List<String> words) {
        this.words = words;
    }

    @Override
    protected void beforeCommit() {
        name = HunspellSpellChecker.DICTIONARY_BASE_NAME + "_" + locale.toString();
        words = new HashSet<>(getWords()).stream().sorted().collect(Collectors.toList());
        HunspellSpellChecker.inValidateDictionaries();
    }
}
