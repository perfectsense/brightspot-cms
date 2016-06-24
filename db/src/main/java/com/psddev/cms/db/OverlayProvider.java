package com.psddev.cms.db;

import com.psddev.dari.db.Recordable;

public interface OverlayProvider extends Recordable {

    boolean shouldOverlay(Object content);

    Overlay provideOverlay(Object content);
}
