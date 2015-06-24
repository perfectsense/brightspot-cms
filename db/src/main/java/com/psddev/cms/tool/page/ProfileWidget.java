package com.psddev.cms.tool.page;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.servlet.ServletException;

import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.util.ClassFinder;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.TypeDefinition;

@RoutingFilter.Path(application = "cms", value = "profileWidget")
@SuppressWarnings("serial")
public class ProfileWidget extends PageServlet {

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        List<Class<? extends ProfileWidgetTab>> tabClasses = new ArrayList<>(ClassFinder.Static.findClasses(ProfileWidgetTab.class));

        Collections.sort(tabClasses, Comparator.<Class<? extends ProfileWidgetTab>, String>comparing(Class::getSimpleName).thenComparing(Class::getName));

        tabClasses.remove(SiteTab.class);
        tabClasses.add(0, SiteTab.class);

        page.writeHeader();
            page.writeStart("div", "class", "widget p-tud");
                page.writeStart("h1");
                    page.writeHtml(page.getUser().getName());
                page.writeEnd();

                page.writeStart("div", "class", "tabbed tabbed-vertical");

                    for (Class<? extends ProfileWidgetTab> c : tabClasses) {
                        TypeDefinition.getInstance(c).newInstance().writeHtml(page);
                    }

                page.writeEnd();
            page.writeEnd();
        page.writeFooter();
    }
}
