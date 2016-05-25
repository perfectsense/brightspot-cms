package com.psddev.cms.db;

import com.psddev.dari.db.Recordable;

public interface OverlayProvider extends Recordable {

    Overlay provideOverlay(Object content);
}
