package com.psddev.cms.tool.page.content.edit;

import com.google.common.collect.ImmutableMap;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Servlet that creates inline edit form with applicable fields.
 */
@RoutingFilter.Path(application = "cms", value = "/content/editInline")
public class EditInline extends PageServlet {

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        Object object = page.findOrReserve();

        if (object == null) {
            throw new IllegalArgumentException("No object found!");
        }

        boolean error = false;

        if (page.isFormPost() && page.param(String.class, "action-publish") != null) {

            try {
                page.include("/WEB-INF/objectPost.jsp", "object", object);
                page.publish(object);
                page.writeStart("script", "type", "text/javascript"); {
                    page.writeRaw("parent.postMessage('brightspot-updated', '*');");
                }
                page.writeEnd();
                return;

            } catch (Exception exc) {
                error = true;
            }
        }

        writeForm(page, object, page.params(String.class, "f"), error);
    }

    private void writeForm(ToolPageContext page, Object object, List<String> fields, Boolean error) throws IOException, ServletException {
        State state = State.getInstance(object);
        UUID id = state.getId();
        ObjectType type = state.getType();

        String iconName = "object";
        String buttonText = page.localize(type, "action.save");

        if (type != null) {
            ToolUi ui = type.as(ToolUi.class);
            iconName = ObjectUtils.firstNonBlank(ui.getIconName(), iconName);
            buttonText = ObjectUtils.firstNonBlank(
                    ui.getPublishButtonText(),
                    ui.isPublishable() ? page.localize(type, "action.publish") : buttonText);
        }

        page.writeHeader(null, false); {

            // Add a class name to the body so we can style the iframe contents
            page.writeStart("script", "type", "text/javascript"); {
                page.writeRaw("$('body').addClass('editInline');");
            }
            page.writeEnd();

            page.writeStart("script",
                    "type", "text/javascript",
                    "src", page.cmsUrl("/script/iframeResizer.contentWindow.js"))
                    .writeEnd();

            page.writeStart("form",
                    "class", "standardForm",
                    "method", "post",
                    "enctype", "multipart/form-data",
                    "action", page.url(""),
                    "autocomplete", "off",
                    "data-rtc-content-id", page.getCmsTool().isDisableFieldLocking() ? null : id,
                    "data-object-id", id,
                    "data-type", type != null ? type.getInternalName() : null); {

                page.writeStart("div", "class", "contentForm-main inline"); {
                    page.writeStart("div", "class", "widget widget-content"); {

                        page.writeStart("h1", "class", "breadcrumbs"); {
                            page.writeStart("span", "class", "breadcrumbItem icon icon-" + iconName); {
                                page.writeHtml(page.localize(
                                        EditInline.class,
                                        ImmutableMap.of("label", page.getTypeLabel(object)),
                                        "title.heading"));
                                page.writeHtml(": ");
                                page.writeStart("span",
                                        "class", "ContentLabel",
                                        "data-dynamic-html", "${toolPageContext.createObjectLabelHtml(content)}"); {

                                    page.write(page.createObjectLabelHtml(object));
                                }
                                page.writeEnd();
                            }
                            page.writeEnd();
                        }
                        page.writeEnd();

                        page.writeStart("div", "class", "widgetControls"); {
                            page.writeStart("a",
                                    "class", "icon icon-action-edit",
                                    "target", "_blank",
                                    "href", page.cmsUrl("/content/edit.jsp", "id", id)); {

                                page.writeHtml(page.localize(EditInline.class, "action.fullForm"));
                            }
                            page.writeEnd();
                        }
                        page.writeEnd();

                        if (error) {
                            page.writeStart("div", "class", "message message-error"); {
                                page.writeHtml(page.localize("com.psddev.cms.tool.page.content.Errors", "error.validation"));
                            }
                            page.writeEnd();
                        }

                        page.writeSomeFormFields(object, false, fields.isEmpty() ? null : fields, null);

                        page.writeStart("div", "class", "actions widget-publishingPublish"); {
                            page.writeStart("button",
                                    "class", "icon icon-action-save",
                                    "name", "action-publish",
                                    "value", "true"); {

                                page.writeHtml(buttonText);
                            }
                            page.writeEnd();
                        }
                        page.writeEnd();
                    }
                    page.writeEnd();
                }
                page.writeEnd();
            }
            page.writeEnd();
        }
        page.writeFooter();
    }
}
