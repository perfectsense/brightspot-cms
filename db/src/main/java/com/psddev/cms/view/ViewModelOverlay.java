package com.psddev.cms.view;

import java.util.Map;
import java.util.function.Supplier;

public interface ViewModelOverlay {

    Map<String, Supplier<Object>> create(Object model);
}
