package com.psddev.cms.tool.view;

import java.io.StringWriter;
import java.io.Writer;

import com.psddev.cms.tool.ToolPageContext;
import com.psddev.cms.view.ViewModel;

/**
 * Abstract {@link ViewModel} to help render Views for {@link com.psddev.cms.tool.Tool} pages.
 */
public abstract class ToolPageViewModel<M> extends ViewModel<M> {

    @CurrentToolPageContext
    protected ToolPageContext page;

    /**
     * Invokes {@link ToolPageContext#writeFormFields(Object)}, and returns the
     * HTML normally written to the {@link javax.servlet.http.HttpServletResponse}.
     */
    public String getFormFieldsHtml(Object object) {
        Writer oldDelegate = page.getDelegate();
        StringWriter newDelegate = new StringWriter();

        try {
            page.setDelegate(newDelegate);
            page.writeFormFields(object);
        } catch (Exception e) {
            // Ignore.
        } finally {
            page.setDelegate(oldDelegate);
        }

        return newDelegate.toString();
    }
}
