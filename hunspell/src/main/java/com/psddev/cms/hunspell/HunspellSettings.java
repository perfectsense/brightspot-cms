package com.psddev.cms.hunspell;

import com.psddev.cms.db.ToolUi;
import com.psddev.cms.tool.CmsTool;
import com.psddev.dari.db.Modification;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class HunspellSettings extends Modification<CmsTool> {
    private static final String TAB_NAME = "Hunspell";

    @ToolUi.Tab(TAB_NAME)
    private Set<HunspellDictionary> dictionaries;

    public Set<HunspellDictionary> getDictionaries() {
        if (dictionaries == null) {
            dictionaries = new HashSet<>();
        }
        return dictionaries;
    }

    public HunspellDictionary getDictionary(String name) {
        return getDictionaries().stream()
                .filter(d -> Objects.equals(d.getName(), name))
                .findFirst()
                .orElse(null);
    }
}
