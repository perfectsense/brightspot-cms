package com.psddev.cms.tool.view;

import java.io.IOException;
import java.io.StringWriter;

import com.psddev.cms.tool.ToolPageContext;
import com.psddev.cms.view.RawView;
import com.psddev.cms.view.ViewModel;
import com.psddev.dari.util.PageContextFilter;

/**
 * Abstract {@link ViewModel} to help render Views for {@link com.psddev.cms.tool.Tool} pages.
 */
public abstract class ToolPageViewModel<M> extends ViewModel<M> {

    /**
     * Helper method to produce a {@link RawView} from
     * {@link ToolPageContext}#write... methods.
     */
    protected RawView createRawView(RawViewWriter writer) {
        StringWriter delegate = new StringWriter();

        ToolPageContext page = new ToolPageContext(
                PageContextFilter.Static.getServletContext(),
                PageContextFilter.Static.getRequest(),
                PageContextFilter.Static.getResponse());
        page.setDelegate(delegate);

        try {
            writer.write(page);
        } catch (IOException error) {
            // This should never happen.
            throw new IllegalStateException(error);
        }

        return RawView.of(delegate.toString());
    }

    @FunctionalInterface
    protected interface RawViewWriter {
        void write(ToolPageContext t) throws IOException;
    }
}
