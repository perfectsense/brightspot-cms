package com.psddev.cms.view;

/**
 * @deprecated Use {@link ViewModel} instead.
 */
@Deprecated
public abstract class AbstractViewCreator<M> extends AbstractParameterizedView<M, ViewRequest> {

    /**
     * @deprecated Use {@link ViewModel#model} instead.
     */
    @Deprecated
    protected M model;

    /**
     * @deprecated Annotate fields in your {@link ViewModel} instead.
     */
    @Deprecated
    protected ViewRequest request;

    @Deprecated
    @Override
    public Object createView(M model, ViewRequest request) {
        this.model = model;
        this.request = request;
        return this;
    }
}
