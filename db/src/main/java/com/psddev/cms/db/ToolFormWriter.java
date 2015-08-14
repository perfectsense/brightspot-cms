package com.psddev.cms.db;

import java.io.IOException;
import java.io.StringWriter;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import com.psddev.cms.tool.CmsTool;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.FormInputProcessor;
import com.psddev.dari.db.FormLabelRenderer;
import com.psddev.dari.db.FormWriter;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.State;
import com.psddev.dari.util.HtmlWriter;
import com.psddev.dari.util.JspUtils;

public class ToolFormWriter extends FormWriter {

    private final ToolPageContext page;

    public ToolFormWriter(ToolPageContext page) throws IOException {
        super(page.getWriter());
        this.page = page;

        setLabelRenderer(new FormLabelRenderer.Default() {
            @Override
            protected void doDisplay(String inputId, String inputName, ObjectField field, HtmlWriter writer) throws IOException {
                writer.start("div", "class", "inputLabel");
                    super.doDisplay(inputId, inputName, field, writer);
                writer.end();
            }
        });
    }

    @Override
    protected void writeField(State state, ObjectField field, FormInputProcessor processor) throws IOException {
        if (processor != null) {
            start("div", "class", "inputContainer");
                super.writeField(state, field, processor);
            end();

        } else {
            StringWriter string = new StringWriter();
            HtmlWriter html = new HtmlWriter(string);
            UUID id = page.isFormFieldsDisabled() ? UUID.randomUUID() : state.getId();

            try {
                JspUtils.include(
                        page.getRequest(),
                        page.getResponse(),
                        html,
                        page.toolPath(CmsTool.class, "/WEB-INF/field.jsp"),
                        "object", state.getOriginalObject(),
                        "field", field,
                        "inputName", id + "/" + field.getInternalName(),
                        "isFormPost", false);

            } catch (ServletException ex) {
                html.start("p");
                    html.string("Unable to display [");
                    html.string(field.getLabel());
                    html.string("] field!");
                html.end();

                html.object(ex);
            }

            write(string.toString());
        }
    }

    @Override
    protected void updateField(State state, HttpServletRequest request, ObjectField field, FormInputProcessor processor) {
        if (processor != null) {
            super.updateField(state, request, field, processor);

        } else {
            StringWriter string = new StringWriter();
            HtmlWriter html = new HtmlWriter(string);

            try {
                JspUtils.include(
                        page.getRequest(),
                        page.getResponse(),
                        html,
                        page.toolPath(CmsTool.class, "/WEB-INF/field.jsp"),
                        "object", state.getOriginalObject(),
                        "field", field,
                        "inputName", state.getId() + "/" + field.getInternalName(),
                        "isFormPost", true);

            } catch (IOException ex) {
                throw new RuntimeException(ex);

            } catch (ServletException ex) {
                Throwable cause = ex.getCause();
                throw cause instanceof RuntimeException
                        ? (RuntimeException) cause
                        : new RuntimeException(cause);
            }
        }
    }
}
