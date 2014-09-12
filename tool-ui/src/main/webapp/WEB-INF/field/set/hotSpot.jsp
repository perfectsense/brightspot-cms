<%@ page session="false" import="
         
com.psddev.cms.db.Content,
com.psddev.cms.db.ToolUi,
com.psddev.cms.tool.PageWriter,
com.psddev.cms.tool.Search,
com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.db.Query,
com.psddev.dari.db.ObjectField,
com.psddev.dari.db.ObjectFieldComparator,
com.psddev.dari.db.ObjectType,
com.psddev.dari.db.State,

com.psddev.dari.util.ObjectUtils,
com.psddev.dari.util.StorageItem,

com.psddev.image.HotSpot,
com.psddev.image.HotSpots,

java.util.ArrayList,
java.util.Collections,
java.util.Date,
java.util.HashMap,
java.util.LinkedHashMap,
java.util.LinkedHashSet,
java.util.List,
java.util.Map,
java.util.Set,
java.util.UUID
" %><%

// --- Logic ---

ToolPageContext wp = new ToolPageContext(pageContext);

State state = State.getInstance(request.getAttribute("object"));
if (state.getOriginalObject() instanceof HotSpots) {
    ObjectField field = (ObjectField) request.getAttribute("field");
    String fieldName = field.getInternalName();
    StorageItem fieldValue = (StorageItem) state.getByPath(fieldName);
    HotSpots hotspots = ObjectUtils.to(HotSpots.class, state.getOriginalObject());
    if (fieldValue != null) {

        List<HotSpot> hotspotList = HotSpots.Data.getHotSpots(fieldValue);

        String inputName = (String) request.getAttribute("inputName");
        String hotSpotsList = fieldName + "/hotspots";
        String hotSpotsName = inputName + ".hotspots";
        String idName = hotSpotsName + ".id";
        String typeIdName = hotSpotsName + ".typeId";

        List<ObjectType> validTypes = new ArrayList<ObjectType>();
        validTypes.addAll(ObjectType.getInstance(HotSpot.class).findConcreteTypes());

        Collections.sort(validTypes, new ObjectFieldComparator("_label", false));

        Map<String, Object> fieldValueMetadata = null;
        if (fieldValue != null) {
            fieldValueMetadata = fieldValue.getMetadata();
        }

        if (fieldValueMetadata == null) {
            fieldValueMetadata = new LinkedHashMap<String, Object>();
        }
        Map<String, Object> hotSpots = (Map<String, Object>) fieldValueMetadata.get("cms.hotspots");
        if (hotSpots == null) {
            hotSpots = new HashMap<String, Object>();
            fieldValueMetadata.put("cms.hotspots", hotSpots);
        }

        if ((Boolean) request.getAttribute("isFormPost")) {
            List<Map<String, Object>> hotSpotObjects = null;
            List<Map<String, Object>> newHotSpotObjects = new ArrayList<Map<String, Object>>();
            if(!ObjectUtils.isBlank(hotSpots) && !ObjectUtils.isBlank(hotSpots.get("objects"))) {
                hotSpotObjects = (List<Map<String, Object>>)hotSpots.get("objects");
            } else {
                hotSpotObjects = new ArrayList<Map<String, Object>>();
            }

            if (!ObjectUtils.isBlank(wp.params(String.class, idName))) {
                for (int index = 0; index < wp.params(String.class, idName).size(); index++) {
                    String hotSpotId = wp.params(String.class, idName).get(index);
                    Object item = null;
                    if (!ObjectUtils.isBlank(hotSpotObjects)) {
                        for (Map<String, Object> object : hotSpotObjects) {
                            if (object.get("_id").equals(hotSpotId)) {
                                HotSpot hotSpotObject = new HotSpot();
                                hotSpotObject.getState().putAll(object);
                                item = hotSpotObject;
                                break;
                            }
                        }
                    }

                    State itemState = null;
                    String typeId = wp.params(String.class, typeIdName).get(index);
                    if (item != null) {
                        itemState = State.getInstance(ObjectUtils.to(HotSpot.class, item));
                        itemState.setTypeId(UUID.fromString(typeId));
                    } else {
                        ObjectType type = ObjectType.getInstance(UUID.fromString(typeId));
                        item = type.createObject(null);
                        itemState = State.getInstance(item);
                        itemState.setId(UUID.fromString(hotSpotId));
                    }
                    wp.updateUsingParameters(item);
                    newHotSpotObjects.add(itemState.getSimpleValues());
                }
            }
            hotSpots.put("objects", newHotSpotObjects);
            fieldValue.getMetadata().put("cms.hotspots", hotSpots);
            state.putValue(fieldName, fieldValue);
            return;

        }
        // --- Presentation ---

        %>
        <div class="inputContainer" data-field="<%=hotSpotsList%>"  data-name="<%=hotSpotsName%>">
            <div class="inputSmall">
                <div class="inputLarge repeatableForm hotSpots">
                    <ul>
                        <%
                        if (!ObjectUtils.isBlank(hotspotList)) {
                            for (HotSpot item : hotspotList) {
                                State itemState = State.getInstance(item);
                                ObjectType itemType = itemState.getType();
                                Date itemPublishDate = itemState.as(Content.ObjectModification.class).getPublishDate();
                                %>
                                <li data-type="<%= wp.objectLabel(itemType) %>" data-label="<%= wp.objectLabel(item) %>">
                                    <input name="<%= wp.h(idName) %>" type="hidden" value="<%= itemState.getId() %>">
                                    <input name="<%= wp.h(typeIdName) %>" type="hidden" value="<%= itemType.getId() %>">
                                    <% wp.writeFormFields(item); %>
                                </li>
                                <% } %>
                        <% } %>
                        <% for (ObjectType type : validTypes) { %>
                            <script type="text/template">
                                <li data-type="<%= wp.objectLabel(type) %>">
                                    <a href="<%= wp.cmsUrl("/content/repeatableObject.jsp", "inputName", hotSpotsName, "typeId", type.getId()) %>"></a>
                                </li>
                            </script>
                        <% } %>
                    </ul>
                </div>
            </div>
        </div>
    <% }
}%>

