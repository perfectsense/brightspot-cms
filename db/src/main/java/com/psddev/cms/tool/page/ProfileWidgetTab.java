package com.psddev.cms.tool.page;

import java.io.IOException;

import javax.servlet.ServletException;

import com.psddev.cms.tool.ToolPageContext;

public abstract class ProfileWidgetTab {

    public abstract void writeHtml(ToolPageContext page) throws IOException, ServletException;
}
