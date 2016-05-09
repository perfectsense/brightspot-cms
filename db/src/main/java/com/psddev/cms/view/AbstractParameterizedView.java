package com.psddev.cms.view;

import com.psddev.dari.util.TypeDefinition;

/**
 * @deprecated Use {@link ViewModel} instead.
 */
@Deprecated
public abstract class AbstractParameterizedView<M, VR> implements ViewCreator<M, Object, VR> {

    protected M model;

    protected VR request;

    @Deprecated
    @Override
    public Object createView(M model, VR request) {
        this.model = model;
        this.request = request;
        return this;
    }

    /**
     * @deprecated Use {@link ViewModel#createView(Class, Object)} instead.
     */
    @Deprecated
    public <T, V> V createView(Class<V> viewClass, T model) {

        Class<? extends ViewCreator<? super T, V, ? super VR>> viewCreatorClass = ViewCreator.findCreatorClass(model, viewClass, null, request);
        if (viewCreatorClass != null) {

            ViewCreator<? super T, ? extends V, ? super VR> viewCreator = TypeDefinition.getInstance(viewCreatorClass).newInstance();
            if (viewCreator != null) {

                return viewCreator.createView(model, request);
            }
        }

        return null;
    }

    /**
     * @deprecated Use {@link ViewModel#createView(String, Object)} instead.
     */
    @Deprecated
    public <T> Object createView(String viewType, T model) {

        Class<? extends ViewCreator<? super T, Object, ? super VR>> viewCreatorClass = ViewCreator.findCreatorClass(model, null, viewType, request);
        if (viewCreatorClass != null) {

            ViewCreator<? super T, ?, ? super VR> viewCreator = TypeDefinition.getInstance(viewCreatorClass).newInstance();
            if (viewCreator != null) {

                return viewCreator.createView(model, request);
            }
        }

        return null;
    }
}
