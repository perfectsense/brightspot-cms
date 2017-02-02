package com.psddev.cms.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 * Wraps ToolPageContext to make it easier to get the html written to the page
 */
public class TestToolPageContext extends ToolPageContext {

    StringWriter writer = new StringWriter();
    CmsTool cmsTool = null;

    public TestToolPageContext(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response) {
        super(servletContext, request, response);
    }

    public void setTool(CmsTool cmsTool) {
        this.cmsTool = cmsTool;
    }

    @Override
    public boolean include(String path, Object... attributes) throws IOException, ServletException {
        return true;
    }

    @Override
    public Writer getDelegate() {
        return new PrintWriter(writer);
    }

    public Document getDocument() {
        return Jsoup.parse(writer.toString());
    }

    @Override
    public String localize(Object context, Map<String, Object> contextOverrides, String key) throws IOException {
        return "test";
    }

    @Override
    public String localize(Object context, String key) throws IOException {
        return "test";
    }

    @Override
    public CmsTool getCmsTool() {
        return cmsTool;
    }
}
