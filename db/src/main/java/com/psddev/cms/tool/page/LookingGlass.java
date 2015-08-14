package com.psddev.cms.tool.page;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;

import com.psddev.cms.db.ToolUser;
import com.psddev.cms.db.ToolUserAction;
import com.psddev.cms.db.ToolUserDevice;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.util.ClassFinder;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.StringUtils;
import com.psddev.dari.util.TypeDefinition;

@RoutingFilter.Path(application = "cms", value = "lookingGlass")
@SuppressWarnings("serial")
public class LookingGlass extends PageServlet {

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        UUID id = page.param(UUID.class, "id");

        if (id == null) {
            ToolUser user = page.getUser();
            ToolUserDevice device = user.findRecentDevice();

            if (device == null) {
                device = user.findOrCreateCurrentDevice(page.getRequest());
            }

            id = device.getOrCreateLookingGlassId();

            page.redirect("", "id", id);
            return;
        }

        ToolUserDevice device = Query
                .from(ToolUserDevice.class)
                .where("lookingGlassId = ?", id)
                .first();

        if (device == null) {
            throw new IllegalArgumentException(String.format(
                    "No looking glass at [%s]!", id));
        }

        ToolUserAction lastAction = Query
                .from(ToolUserAction.class)
                .where("device = ?", device)
                .sortDescending("time")
                .noCache()
                .first();

        if (lastAction != null
                && "ping".equals(page.param(String.class, "action"))) {
            long end = System.currentTimeMillis() + 30000;

            while (System.currentTimeMillis() < end
                    && lastAction.getTime() == page.param(long.class, "time")) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException error) {
                    break;
                }

                lastAction = Query
                        .from(ToolUserAction.class)
                        .where("device = ?", device)
                        .sortDescending("time")
                        .noCache()
                        .first();
            }

            Map<String, Object> response = new HashMap<String, Object>();

            response.put("changed", lastAction == null || lastAction.getTime() != page.param(long.class, "time"));
            page.getResponse().setContentType("application/json");
            page.writeRaw(ObjectUtils.toJson(response));
            return;
        }

        ToolUser user = device.getUser();

        page.writeHeader();
            page.writeStart("div", "class", "message message-info");
                page.writeHtml("Mirroring ");
                page.writeObjectLabel(user);

                page.writeHtml(" in ");
                page.writeHtml(device.getUserAgentDisplay());

                if (lastAction != null) {
                    Object lastActionContent = lastAction.getContent();

                    if (lastActionContent != null) {
                        page.writeHtml(" - ");
                        page.writeStart("a",
                                "target", "_blank",
                                "href", page.objectUrl("/content/edit.jsp", lastActionContent));
                            page.writeTypeObjectLabel(lastActionContent);
                        page.writeEnd();
                    }
                }
            page.writeEnd();

            Class<?> viewClass = ObjectUtils.getClassByName(page.param(String.class, "view"));

            if (viewClass == null
                    || !LookingGlassView.class.isAssignableFrom(viewClass)) {
                viewClass = LookingGlassView.PreviewView.class;
            }

            page.writeStart("form",
                    "method", "get",
                    "action", page.cmsUrl("/lookingGlass"));
                page.writeElement("input", "type", "hidden", "name", "id", "value", id);

                page.writeStart("select",
                        "data-bsp-autosubmit", "",
                        "name", "view");
                    for (Class<? extends LookingGlassView> c : ClassFinder.Static.findClasses(LookingGlassView.class)) {
                        if (Modifier.isAbstract(c.getModifiers())) {
                            continue;
                        }

                        page.writeStart("option",
                                "selected", c.equals(viewClass) ? "selected" : null,
                                "value", c.getName());
                            page.writeHtml(StringUtils.toLabel(c.getSimpleName()));
                        page.writeEnd();
                    }
                page.writeEnd();
            page.writeEnd();

            if (lastAction != null) {
                ((LookingGlassView) TypeDefinition.getInstance(viewClass).newInstance()).renderAction(page, user, lastAction);
            }

            page.writeStart("script", "type", "text/javascript");
                page.writeRaw("(function($, win) {");
                    page.writeRaw("var ping = function() {");
                        page.writeRaw("$.ajax({");
                            page.writeRaw("'dataType': 'json',");
                            page.writeRaw("'cache': false,");

                            page.writeRaw("'url': '");
                            page.writeRaw(page.cmsUrl("/lookingGlass",
                                    "id", id,
                                    "action", "ping",
                                    "time", lastAction != null ? lastAction.getTime() : null));
                            page.writeRaw("',");

                            page.writeRaw("'success': function(response) {");
                                page.writeRaw("if (response.changed) {");
                                    page.writeRaw("win.location = win.location;");
                                page.writeRaw("} else {");
                                    page.writeRaw("ping();");
                                page.writeRaw("}");
                            page.writeRaw("},");

                            page.writeRaw("'error': function() {");
                                page.writeRaw("setTimeout(ping, 5000);");
                            page.writeRaw("}");
                        page.writeRaw("});");
                    page.writeRaw("};");

                    page.writeRaw("$(win).bind('load', function() {");
                        page.writeRaw("ping();");
                    page.writeRaw("});");
                page.writeRaw("})(jQuery, window);");
            page.writeEnd();
        page.writeFooter();
    }
}
