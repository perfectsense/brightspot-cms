package com.psddev.cms.view;

/**
 * @deprecated No replacement
 */
@Deprecated
public interface PageView {

    /**
     * @deprecated No replacement
     */
    @Deprecated
    default Object createMainView(Object model, ViewRequest request) {
        MainViewClass annotation = model.getClass().getAnnotation(MainViewClass.class);
        return annotation != null ? request.createView(annotation.value(), model) : null;
    }
}
