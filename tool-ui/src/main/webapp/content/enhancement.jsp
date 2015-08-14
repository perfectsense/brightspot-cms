<%@ page session="false" import="

com.psddev.cms.db.RichTextReference,
com.psddev.cms.db.ToolUi,
com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.db.Database,
com.psddev.dari.db.ObjectField,
com.psddev.dari.db.ObjectType,
com.psddev.dari.db.Query,
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

String objectFormId = wp.createId();
String objectPreviewId = wp.createId();

String editObjectFormId = wp.createId();
String viewObjectPreviewId = wp.createId();

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

if (object != null && wp.isFormPost()) {
    try {
        request.setAttribute("excludeFields", Arrays.asList("record"));
        wp.updateUsingParameters(ref);

        request.setAttribute("excludeFields", null);
        if (wp.param(boolean.class, "isEditObject")) {
            wp.updateUsingParameters(object);
            wp.publish(object);
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
    wp.writeFormHeading(object);

    int refFieldCount = -1;

    for (ObjectField f : ref.getState().getType().getFields()) {
        if (!f.as(ToolUi.class).isHidden()) {
            ++ refFieldCount;
        }
    }
    %>

    <form class="enhancementForm" action="<%= wp.url("", "typeId", state.getTypeId(), "id", state.getId()) %>" enctype="multipart/form-data" id="<%= pageId %>" method="post">
        <% wp.include("/WEB-INF/errors.jsp"); %>

        <%
        if (refFieldCount > 0) {
            wp.writeElement("input", "type", "hidden", "name", "refId", "value", ref.getId());
            wp.writeSomeFormFields(ref, false, null, Arrays.asList("record"));
        }
        %>

        <%-- Object Preview --%>
        <p id="<%= editObjectFormId %>">
            <a target="_top" class="action action-edit" href="javascript:;">Edit Enhancement</a>
        </p>
        <div id="<%= objectPreviewId %>">
            <div class="rte-enhancement-label">
                <% if (state.getPreview() != null) { %>
                    <figure style="height:300px;">
                        <img src="<%= state.getPreview().getPublicUrl() %>" style="max-height:100%;max-width:100%;"/>
                        <figcaption>
                            <%= wp.h(state.getLabel()) %>
                        </figcaption>
                    </figure>
                <% } else { %>
                    <%= wp.h(state.getLabel()) %>
                <% } %>
            </div>
        </div>

        <%-- Object Edit Form --%>
        <p id="<%= viewObjectPreviewId %>" style="display:none;">
            <a target="_top" class="action action-cancel" href="javascript:;">Cancel Editing</a>
        </p>
        <div id="<%= objectFormId %>" style="display:none;">
            <% wp.writeFormFields(object); %>
        </div>

        <div class="buttons">
            <button class="action action-save">Save</button>
        </div>
    </form>

    <script type="text/javascript">
        if (typeof jQuery !== 'undefined') (function($) {
            var $objectForm = $('#<%= objectFormId %>');
            var $objectPreview = $('#<%= objectPreviewId %>');
            var $editObjectForm = $('#<%= editObjectFormId %>');
            var $viewObjectForm = $('#<%= viewObjectPreviewId %>');

            $objectForm.append($('<input/>', {
                'type': 'hidden',
                'name': 'isEditObject',
                'value': 'false'}));

            $viewObjectForm.click(function(evt) {
                $viewObjectForm.hide();
                $editObjectForm.show();

                $objectForm.hide();
                $objectPreview.show();

                $objectForm.find('input[name="isEditObject"]').val(false);
                $(window).resize();
            });

            $editObjectForm.click(function(evt) {
                $editObjectForm.hide();
                $viewObjectForm.show();

                $objectPreview.hide();
                $objectForm.show();;

                $objectForm.find('input[name="isEditObject"]').val(true);
                $(window).resize();
            });

            <% if (refFieldCount == 0 || state.isNew()) { %>
                $editObjectForm.click();
            <% } %>

            <% if (refFieldCount == 0) { %>
                $viewObjectForm.remove();
            <% } %>
        })(jQuery);

        // Update the rich text editor so it points to this enhancement
        if (typeof jQuery !== 'undefined') (function($) {
        
            var $source = $('#<%= pageId %>').popup('source');
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

                // Get the rte2 object - this would normally be found on the textarea,
                // but since $source is a link within the editor we'll use it to find the wrapper element
                // and get the rte2 object from there
                var rte2 = $source.closest('.rte2-wrapper').data('rte2');
                if (rte2) {
                    // Update the label in the reference data since it might have changed and we display it on the page
                    reference.label = label;
                    // Save the enhancement data on the enhancement
                    rte2.enhancementSetReference($source, reference);
                    // Enhancement will be updated automaticaly when the popup closes
                }
            }
                
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
