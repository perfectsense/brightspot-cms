package com.psddev.cms.db;

import com.psddev.dari.db.Recordable;

import java.util.Map;
import java.util.UUID;

public interface Overlay extends Recordable {

    OverlayProvider getOverlayProvider();

    UUID getContentId();

    Map<String, Map<String, Object>> getDifferences();

    void setDifferences(Map<String, Map<String, Object>> differences);
}
