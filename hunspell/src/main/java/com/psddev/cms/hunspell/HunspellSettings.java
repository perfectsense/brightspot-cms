package com.psddev.cms.hunspell;

import com.psddev.cms.db.ToolUi;
import com.psddev.cms.tool.CmsTool;
import com.psddev.dari.db.Modification;
import com.psddev.dari.db.Recordable;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Recordable.FieldInternalNamePrefix("hunspell.")
public class HunspellSettings extends Modification<CmsTool> {

    private static final String HEADING = "Hunspell";

    @ToolUi.Heading(HEADING)
    private Set<HunspellDictionary> dictionaries;

    public Set<HunspellDictionary> getDictionaries() {
        if (dictionaries == null) {
            dictionaries = new HashSet<>();
        }
        return dictionaries;
    }

    public Set<HunspellDictionary> findDictionaries(String name) {
        return getDictionaries().stream()
                .filter(Objects::nonNull)
                .filter(d -> HunspellSpellChecker.createDictionaryNames(d.getLocale()).contains(name))
                .collect(Collectors.toSet());
    }

    @Override
    protected void afterSave() {
        HunspellSpellChecker.HUNSPELLS.invalidateAll();
    }
}
