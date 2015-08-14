package com.psddev.cms.db;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import com.psddev.dari.util.AbstractFilter;
import com.psddev.dari.util.HtmlWriter;
import com.psddev.dari.util.ObjectUtils;

/**
 * @deprecated No replacement.
 */
@Deprecated
public class FrameFilter extends AbstractFilter {

    @Override
    protected void doRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {

        if (ObjectUtils.to(boolean.class, request.getParameter("_frame"))) {
            CapturingResponse capturing = new CapturingResponse(response);

            try {
                chain.doFilter(request, capturing);

            } finally {
                @SuppressWarnings("resource")
                HtmlWriter writer = new HtmlWriter(response.getWriter());

                writer.writeTag("!doctype html");
                writer.writeStart("html");
                    writer.writeStart("body");
                        writer.writeStart("textarea", "id", "frameBody");
                            writer.writeHtml(capturing.getOutput());
                        writer.writeEnd();
                    writer.writeEnd();
                writer.writeEnd();
            }

        } else {
            chain.doFilter(request, response);
        }
    }

    private static final class CapturingResponse extends HttpServletResponseWrapper {

        private final StringWriter output;
        private final PrintWriter printWriter;

        public CapturingResponse(HttpServletResponse response) {
            super(response);

            this.output = new StringWriter();
            this.printWriter = new PrintWriter(output);
        }

        @Override
        public ServletOutputStream getOutputStream() {
            throw new IllegalStateException();
        }

        @Override
        public PrintWriter getWriter() {
            return printWriter;
        }

        public String getOutput() {
            return output.toString();
        }
    }
}
