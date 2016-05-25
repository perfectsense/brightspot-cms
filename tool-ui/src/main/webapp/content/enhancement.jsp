<%@ page session="false" import="

com.psddev.cms.db.RichTextElement,
com.psddev.cms.db.RichTextReference,
com.psddev.cms.db.ToolUi,
com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.db.Database,
com.psddev.dari.db.ObjectField,
com.psddev.dari.db.ObjectType,
com.psddev.dari.db.Reference,
com.psddev.dari.db.State,
com.psddev.dari.util.ObjectUtils,
com.psddev.dari.util.StringUtils,

java.util.Arrays,
java.util.HashSet,
java.util.Map,
java.util.Set,
java.util.UUID
" %><%

// --- Logic ---

ToolPageContext wp = new ToolPageContext(pageContext);
if (wp.requireUser()) {
    return;
}

String pageId = wp.createId();

// init enhancement object
Object object = wp.findOrReserve();
State state = State.getInstance(object);

UUID typeId = wp.param(UUID.class, "typeId");
if (typeId == null && (state == null || state.isNew())) {
    object = null;
}

//init reference
Reference ref = null;

Map<?, ?> refMap = (Map<?, ?>) ObjectUtils.fromJson(wp.paramOrDefault(String.class, "reference", "{}"));
UUID refId = ObjectUtils.to(UUID.class, ((Map<?, ?>) refMap).remove("_id"));
UUID refTypeId = ObjectUtils.to(UUID.class, ((Map<?, ?>) refMap).remove("_type"));

ObjectType refType = null;
if (state != null) {
    Class<? extends Reference> referenceClass = state.getType().as(ToolUi.class).getReferenceableViaClass();
    if (referenceClass != null) {
        refType = Database.Static.getDefault().getEnvironment().getTypeByClass(referenceClass);
    }
}

if (refType == null) {
    refType = Database.Static.getDefault().getEnvironment().getTypeById(refTypeId);
}

if (refType != null) {
    Object refObject = refType.createObject(refId);
    if (refObject instanceof Reference) {
        ref = (Reference) refObject;
    }
}

if (ref == null) {
    ref = new Reference();
}

for (Map.Entry<?, ?> entry : ((Map<?, ?>) refMap).entrySet()) {
    Object key = entry.getKey();
    ref.getState().put(key != null ? key.toString() : null, entry.getValue());
}

ref.getState().setId(wp.param(UUID.class, "refId"));
ref.setObject(object);

// Always reset the label and preview to the current object
RichTextReference rteRef = ref.as(RichTextReference.class);
rteRef.setLabel(state != null ? state.getLabel() : null);
rteRef.setPreview(state != null && state.getPreview() != null ? state.getPreview().getPublicUrl() : null);

final boolean isRichTextElement = object instanceof RichTextElement;

if (isRichTextElement) {
    ((RichTextElement) object).fromAttributes((Map<String, String>) ObjectUtils.fromJson(wp.param(String.class, "attributes")));
    ((RichTextElement) object).fromBody(wp.param(String.class, "body"));
}

Map<String, Object> stateOldValues = state.getSimpleValues();

if (object != null && wp.isFormPost() && (wp.param(boolean.class, "action-save-and-close") || wp.param(boolean.class, "action-save"))) {
    try {
        request.setAttribute("excludeFields", Arrays.asList("record"));
        wp.updateUsingParameters(ref);

        request.setAttribute("excludeFields", null);

        if (state != null && isRichTextElement) {
            try {
                state.beginWrites();
                wp.updateUsingParameters(object);
                state.validate();
                wp.publish(object);
            } finally {
                state.endWrites();
            }

            if (!state.hasAnyErrors()) {
                RichTextElement rte = (RichTextElement) object;
                Map<String, String> attributes = null;
                String body = null;
                boolean successful = false;

                try {
                    attributes = rte.toAttributes();
                    body = rte.toBody();
                    successful = true;

                } catch (RuntimeException error) {
                    wp.getErrors().add(error);
                }

                if (successful) {
                    wp.writeStart("div", "id", pageId);
                    wp.writeEnd();
                    wp.writeStart("script", "type", "text/javascript");
                    wp.writeRaw("var $page = $('#" + pageId + "');");
                    wp.writeRaw("var $source = $page.popup('source');");
                    wp.writeRaw("var rte = $source.data('rte');");
                    wp.writeRaw("var mark = $source.data('mark');");
                    wp.writeRaw("mark.attributes = " + ObjectUtils.toJson(attributes) + ";");

                    if (body != null) {
                        wp.writeRaw("var oldMarkInclusiveLeft = mark.inclusiveLeft;");
                        wp.writeRaw("var oldMarkInclusiveRight = mark.inclusiveRight;");
                        wp.writeRaw("mark.inclusiveLeft = true;");
                        wp.writeRaw("mark.inclusiveRight = true;");
                        wp.writeRaw("rte.rte.fromHTML('");
                        wp.writeRaw(StringUtils.escapeJavaScript(body));
                        wp.writeRaw("', rte.rte.markGetRange(mark), true, true);");
                        wp.writeRaw("mark.inclusiveLeft = oldMarkInclusiveLeft;");
                        wp.writeRaw("mark.inclusiveRight = oldMarkInclusiveRight;");
                    }

                    if (wp.param(boolean.class, "action-save-and-close")) {
                        wp.writeRaw("$page.popup('close');");
                        wp.writeEnd();
                        return;

                    } else {
                        wp.writeEnd();
                    }
                }
            }
        }

    } catch (Exception ex) {
        wp.getErrors().add(ex);
    }
}

// --- Presentation ---

if (object == null) {
    Set<UUID> validTypeIds = new HashSet<UUID>();
    for (ObjectType type : Database.Static.getDefault().getEnvironment().getTypes()) {
        if (type.as(ToolUi.class).isReferenceable()) {
            validTypeIds.add(type.getId());
        }
    }
    wp.include("/WEB-INF/search.jsp"
            , "newJsp", StringUtils.addQueryParameters("/content/enhancement.jsp", "reference", referenceParamWithoutObject(ref))
            , "resultJsp", StringUtils.addQueryParameters("/content/enhancementResult.jsp", "reference", referenceParamWithoutObject(ref))
            , "validTypeIds", validTypeIds.toArray(new UUID[validTypeIds.size()])
            );

} else {
    if (isRichTextElement) {

        wp.writeStart("h1");
            wp.writeHtml("Edit ");
            wp.writeTypeLabel(object);
        wp.writeEnd();

    } else {
        wp.writeStart("h1");
            wp.writeHtml("Edit ");
            wp.writeTypeLabel(object);
            wp.writeHtml(" Enhancement Options");
        wp.writeEnd();
    }

    // -1 accounts for `object` field if object is isntanceof Reference
    int refFieldCount = isRichTextElement ? 0 : -1;

    for (ObjectField f : ref.getState().getType().getFields()) {
        if (!f.as(ToolUi.class).isHidden()) {
            ++ refFieldCount;
        }
    }
    %>

    <form class="enhancementForm" action="<%= wp.url("", "typeId", state.getTypeId(), "id", state.getId()) %>" enctype="multipart/form-data" id="<%= pageId %>" method="post">
        <input type="hidden" name="<%= state.getId() %>/oldValues" value="<%= wp.h(ObjectUtils.toJson(stateOldValues)) %>">
        <% wp.include("/WEB-INF/errors.jsp"); %>

        <%
        if (refFieldCount > 0) {
            wp.writeElement("input", "type", "hidden", "name", "refId", "value", ref.getId());
            wp.writeSomeFormFields(ref, false, null, Arrays.asList("record"));
        }

        if (isRichTextElement) {
            wp.writeFormFields(object);
        }
        %>

        <div class="buttons">
            <%
                if (!isRichTextElement || ((RichTextElement) object).shouldCloseOnSave()) {
                    wp.writeStart("button",
                            "class", "action action-save",
                            "name", "action-save-and-close",
                            "value", true);
                    wp.writeHtml(wp.localize(state.getType(), "action.saveAndClose"));
                    wp.writeEnd();

                } else {
                    wp.writeStart("button",
                            "class", "action action-save",
                            "name", "action-save",
                            "value", true);
                    wp.writeHtml(wp.localize(state.getType(), "action.save"));
                    wp.writeEnd();
                }
            %>
        </div>
    </form>

    <script type="text/javascript">

        // Update the rich text editor so it points to this enhancement
        if (typeof jQuery !== 'undefined') (function($) {

            var $page = $('#<%= pageId %>');
            var $source = $page.popup('source');
            var id = '<%= state.getId() %>';
            var label = '<%= wp.js(state.getLabel()) %>';
            var preview = '<%= wp.js(state.getPreview() != null ? state.getPreview().getPublicUrl() : null) %>';
            var referenceJson = '<%= wp.js(ObjectUtils.toJson(ref.getState().getSimpleValues())) %>';
            var reference = JSON.parse(referenceJson) || {};

            // Check which RTE we are using
            if (window.DISABLE_CODE_MIRROR_RICH_TEXT_EDITOR) {

                // Using old RTE
                var $group = $source.closest('.rte-group');
                var $select = $group.find('.rte-button-enhancementSelect a');
                var $edit = $group.find('.rte-button-enhancementEdit a');

                $group.addClass('rte-group-enhancementSet');
                $select.text('Change');
                $select.rte('enhancement', {
                    'id': id,
                    'label': label,
                    'preview': preview,
                    'reference': referenceJson
                });

                if ($edit.length > 0) {
                    $edit.attr('href', $.addQueryParameters($edit.attr('href'), 'id', id, 'reference', referenceJson));
                }

            } else {

                // Using new RTE

                // Update the label in the reference data since it might have changed and we display it on the page
                reference.label = label;

                // Trigger an event on the link that opened this popup,
                // to notify about any changes to the enhancement data
                $source.trigger('enhancementUpdate', [reference]);

                // Get the rte2 object - this would normally be found on the textarea,
                // but since $source is a link within the editor we'll use it to find the wrapper element
                // and get the rte2 object from there
                var rte2 = $source.closest('.rte2-wrapper').data('rte2');
                if (rte2) {

                    // Save the enhancement data on the enhancement
                    // Enhancement will be updated automatically when the popup closes
                    rte2.enhancementSetReference($source, reference);

                }
            }

            <% if (!isRichTextElement && wp.isFormPost() && wp.getErrors().isEmpty()) { %>
                $page.popup('close');
            <% } %>

        })(jQuery);
    </script>
<% } %>
<%!

private static String referenceParamWithoutObject(Reference reference) {
    Map<String, Object> map = reference.getState().getSimpleValues();
    map.remove("record");
    return ObjectUtils.toJson(map);
}

%>
