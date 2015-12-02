package com.psddev.cms.tool.page;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.PageContext;

import com.psddev.cms.db.Content;
import com.psddev.cms.db.Directory;
import com.psddev.cms.db.Draft;
import com.psddev.cms.db.Preview;
import com.psddev.cms.db.Site;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.db.Workflow;
import com.psddev.cms.tool.AuthenticationFilter;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.PredicateParser;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.db.State;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.ErrorUtils;
import com.psddev.dari.util.HtmlWriter;
import com.psddev.dari.util.ObjectToIterable;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RoutingFilter.Path(application = "cms", value = "/contentState")
public class ContentState extends PageServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentState.class);

    private static final long serialVersionUID = 1L;

    @Override
    protected String getPermissionId() {
        return null;
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {
        Object object = page.findOrReserve();

        if (object == null) {
            return;
        }

        // Pretend to update the object.
        State state = State.getInstance(object);

        if (state.isNew()
                || object instanceof Draft
                || state.as(Content.ObjectModification.class).isDraft()
                || state.as(Workflow.Data.class).getCurrentState() != null) {
            page.setContentFormScheduleDate(object);
        }

        try {
            state.beginWrites();
            page.updateUsingParameters(object);
            page.updateUsingAllWidgets(object);
            page.publish(object);

        } catch (IOException error) {
            throw error;

        } catch (ServletException error) {
            throw error;

        } catch (RuntimeException error) {
            throw error;

        } catch (Exception error) {
            ErrorUtils.rethrow(error);

        } finally {
            state.endWrites();
        }

        // Expensive operations that should only trigger occasionally.
        boolean idle = page.param(boolean.class, "idle");

        if (idle) {
            boolean saveUser = false;

            // Automatically save newly created drafts when the user is idle.
            Content.ObjectModification contentData = state.as(Content.ObjectModification.class);
            ToolUser user = page.getUser();

            if (idle
                    && (state.isNew() || contentData.isDraft())
                    && !page.getCmsTool().isDisableAutomaticallySavingDrafts()) {
                contentData.setDraft(true);
                contentData.setUpdateDate(new Date());
                contentData.setUpdateUser(user);
                state.save();

                Set<UUID> automaticallySavedDraftIds = user.getAutomaticallySavedDraftIds();
                UUID id = state.getId();

                if (!automaticallySavedDraftIds.contains(id)) {
                    saveUser = true;

                    automaticallySavedDraftIds.add(id);
                }
            }

            // Preview for looking glass.
            Preview preview = new Preview();
            UUID currentPreviewId = user.getCurrentPreviewId();

            if (currentPreviewId == null) {
                saveUser = true;
                currentPreviewId = preview.getId();

                user.setCurrentPreviewId(currentPreviewId);
            }

            Map<String, Object> values = state.getSimpleValues();

            preview.getState().setId(currentPreviewId);
            preview.setCreateDate(new Date());
            preview.setObjectType(state.getType());
            preview.setObjectId(state.getId());
            preview.setObjectValues(values);
            preview.setSite(page.getSite());
            preview.save();
            AuthenticationFilter.Static.setCurrentPreview(page.getRequest(), page.getResponse(), preview);
            user.saveAction(page.getRequest(), object);

            if (saveUser) {
                user.save();
            }
        }

        Map<String, Object> jsonResponse = new CompactMap<String, Object>();

        // HTML display for the URL widget.
        @SuppressWarnings("unchecked")
        Set<Directory.Path> newPaths = (Set<Directory.Path>) state.getExtras().get("cms.newPaths");

        if (!ObjectUtils.isBlank(newPaths)) {
            StringWriter string = new StringWriter();
            @SuppressWarnings("resource")
            HtmlWriter html = new HtmlWriter(string);

            html.writeStart("ul");
                for (Directory.Path p : newPaths) {
                    Site s = p.getSite();

                    html.writeStart("li");
                        html.writeStart("a",
                                "target", "_blank",
                                "href", p.getPath());
                            html.writeHtml(p.getPath());
                        html.writeEnd();

                        html.writeHtml(" (");

                        if (s != null) {
                            html.writeHtml(s.getLabel());
                            html.writeHtml(" - ");
                        }

                        html.writeHtml(p.getType());
                        html.writeHtml(")");
                    html.writeEnd();
                }
            html.writeEnd();

            jsonResponse.put("_urlWidgetHtml", string.toString());
        }

        // Evaluate all dynamic texts.
        List<String> dynamicTexts = new ArrayList<String>();
        List<String> dynamicPredicates = new ArrayList<>();
        JspFactory jspFactory = JspFactory.getDefaultFactory();
        PageContext pageContext = jspFactory.getPageContext(this, page.getRequest(), page.getResponse(), null, false, 0, false);

        try {
            ExpressionFactory expressionFactory = jspFactory.getJspApplicationContext(getServletContext()).getExpressionFactory();
            ELContext elContext = pageContext.getELContext();
            List<UUID> contentIds = page.params(UUID.class, "_dti");
            int contentIdsSize = contentIds.size();
            List<String> templates = page.params(String.class, "_dtt");
            List<String> contentFieldNames = page.params(String.class, "_dtf");
            List<String> predicates = page.params(String.class, "_dtq");
            int contentFieldNamesSize = contentFieldNames.size();

            for (int i = 0, size = templates.size(); i < size; ++ i) {

                Object content = null;

                try {
                    content = i < contentIdsSize ? findContent(object, contentIds.get(i)) : null;
                } catch (RuntimeException e) {
                    // Ignore.
                }

                String dynamicText = "";
                String dynamicPredicate = "";

                if (content != null) {

                    try {

                        pageContext.setAttribute("content", content);
                        pageContext.setAttribute("tool", page);

                        ObjectField field = null;

                        String contentFieldName = i < contentFieldNamesSize ? contentFieldNames.get(i) : null;
                        if (contentFieldName != null) {
                            field = State.getInstance(content).getField(contentFieldName);
                        }
                        pageContext.setAttribute("field", field);

                        dynamicText = ((String) expressionFactory.createValueExpression(elContext, templates.get(i), String.class).getValue(elContext));

                    } catch (RuntimeException error) {
                        if (Settings.isProduction()) {
                            LOGGER.warn("Could not generate dynamic text!", error);

                        } else {
                            StringWriter string = new StringWriter();

                            error.printStackTrace(new PrintWriter(string));
                            dynamicText = string.toString();
                        }
                    }

                    try {

                        if (!ObjectUtils.isBlank(predicates.get(i))) {
                            dynamicPredicate = PredicateParser.Static.parse(predicates.get(i), content).toString();
                        }

                    } catch (RuntimeException error) {

                        LOGGER.warn("Could not generate dynamic predicate!", error);
                    }
                }

                dynamicTexts.add(dynamicText);
                dynamicPredicates.add(dynamicPredicate);
            }

        } finally {
            jspFactory.releasePageContext(pageContext);
        }

        jsonResponse.put("_dynamicTexts", dynamicTexts);
        jsonResponse.put("_dynamicPredicates", dynamicPredicates);

        // Write the JSON response.
        HttpServletResponse response = page.getResponse();

        response.setContentType("application/json");
        page.write(ObjectUtils.toJson(jsonResponse));
    }

    private Object findContent(Object object, UUID id) {
        if (id != null) {
            State state = State.getInstance(object);

            if (state.getId().equals(id)) {
                return object;
            }

            for (Map.Entry<String, Object> entry : state.entrySet()) {
                String name = entry.getKey();
                Object value = entry.getValue();
                ObjectField field = state.getField(name);
                Object found = findEmbedded(value, id, field != null && field.isEmbedded());

                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private Object findEmbedded(Object value, UUID id, boolean embedded) {
        if (value != null) {
            if (value instanceof Recordable) {
                State valueState = ((Recordable) value).getState();

                if (valueState.isNew()) {
                    ObjectType type;

                    if (embedded
                            || ((type = valueState.getType()) != null
                            && type.isEmbedded())) {
                        Object found = findContent(value, id);

                        if (found != null) {
                            return found;
                        }
                    }
                }

            } else {
                Iterable<?> valueIterable = value instanceof Map
                        ? ((Map<?, ?>) value).values()
                        : ObjectToIterable.iterable(value);

                if (valueIterable != null) {
                    for (Object item : valueIterable) {
                        Object found = findEmbedded(item, id, embedded);

                        if (found != null) {
                            return found;
                        }
                    }
                }
            }
        }

        return null;
    }
}
