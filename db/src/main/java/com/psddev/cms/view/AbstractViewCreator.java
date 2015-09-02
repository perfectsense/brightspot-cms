package com.psddev.cms.view;

/**
 * A base implementation of a ViewCreator that facilitates sub-classes to act
 * as both ViewCreator and view interface implementation. Ex.
 *
 * <blockquote><pre>
 * public interface ArticleView {
 *
 * &nbsp;   String getTitle();
 *
 * &nbsp;   public static class FromArticle extends AbstractViewCreator&lt;Article&gt; implements ArticleView {
 * &nbsp;       public String getTitle() {
 * &nbsp;           return model.getTitle();
 * &nbsp;       }
 * &nbsp;   }
 * }
 * </pre></blockquote>

 * Sub-classes are required to implement an interface of the view for which
 * they want to create.
 *
 * Sub-classes will have direct instance variable access to the model and view
 * request to implement the methods defined by the view interface.
 *
 * @param <M> the model type from which the view creator can create views.
 */
public abstract class AbstractViewCreator<M> implements ViewCreator<M, Object> {

    protected M model;

    protected ViewRequest request;

    @Override
    public Object createView(M model, ViewRequest request) {
        this.model = model;
        this.request = request;
        return this;
    }
}
