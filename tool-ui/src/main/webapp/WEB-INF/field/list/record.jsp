<%@ page session="false" import="

com.psddev.cms.db.BulkUploadDraft,
com.psddev.cms.db.Content,
com.psddev.cms.db.Renderer,
com.psddev.cms.db.ToolUi,
com.psddev.cms.tool.PageWriter,
com.psddev.cms.tool.ToolPageContext,

com.psddev.dari.db.Database,
com.psddev.dari.db.Query,
com.psddev.dari.db.ObjectField,
com.psddev.dari.db.ObjectFieldComparator,
com.psddev.dari.db.ObjectType,
com.psddev.dari.db.State,

com.psddev.dari.util.CompactMap,
com.psddev.dari.util.CssUnit,
com.psddev.dari.util.HtmlGrid,
com.psddev.dari.util.HtmlObject,
com.psddev.dari.util.HtmlWriter,
com.psddev.dari.util.ObjectUtils,
com.psddev.dari.util.StringUtils,
com.psddev.dari.util.StorageItem,

java.io.IOException,
java.util.ArrayList,
java.util.Collections,
java.util.Date,
java.util.HashMap,
java.util.HashSet,
java.util.Iterator,
java.util.List,
java.util.Map,
java.util.Set,
java.util.UUID,
java.util.stream.Collectors
" %><%

// --- Logic ---

final ToolPageContext wp = new ToolPageContext(pageContext);

State state = State.getInstance(request.getAttribute("object"));

final ObjectField field = (ObjectField) request.getAttribute("field");
String fieldName = field.getInternalName();
List<Object> fieldValue = (List<Object>) state.getValue(fieldName);
if (fieldValue == null) {
    fieldValue = new ArrayList<Object>();
}

final List<ObjectType> validTypes = field.as(ToolUi.class).findDisplayTypes();
boolean isValueExternal = ToolUi.isValueExternal(field);

Collections.sort(validTypes, new ObjectFieldComparator("_label", false));

final String inputName = (String) request.getAttribute("inputName");
final String idName = inputName + ".id";
final String typeIdName = inputName + ".typeId";
final String publishDateName = inputName + ".publishDate";
final String dataName = inputName + ".data";
String layoutsName = inputName + ".layouts";

if ((Boolean) request.getAttribute("isFormPost")) {
    if (!isValueExternal) {
        Map<UUID, Object> existing = new HashMap<UUID, Object>();

        for (Object item : fieldValue) {
            existing.put(State.getInstance(item).getId(), item);
        }

        fieldValue.clear();

        UUID[] ids = wp.uuidParams(idName);
        UUID[] typeIds = wp.uuidParams(typeIdName);
        Date[] publishDates = wp.dateParams(publishDateName);
        List<String> datas = wp.params(String.class, dataName);

        for (int i = 0, s = Math.min(Math.min(ids.length, typeIds.length), publishDates.length); i < s; ++ i) {
            Object item = existing.get(ids[i]);
            State itemState = State.getInstance(item);

            if (item != null) {
                itemState.setTypeId(typeIds[i]);

            } else {
                ObjectType type = ObjectType.getInstance(typeIds[i]);
                item = type.createObject(null);
                itemState = State.getInstance(item);
                itemState.setResolveInvisible(true);
                itemState.setId(ids[i]);
            }

            String data = i < datas.size() ? datas.get(i) : null;

            if (!ObjectUtils.isBlank(data)) {
                itemState.putAll((Map<String, Object>) ObjectUtils.fromJson(data));

            } else {
                wp.updateUsingParameters(item);
            }

            itemState.remove(Content.PUBLISH_DATE_FIELD);
            itemState.remove(Content.UPDATE_DATE_FIELD);
            fieldValue.add(item);

            if (field.isEmbedded() && !itemState.isNew()) {
                itemState.setId(null);
                itemState.setStatus(null);
            }
        }

    } else {
        fieldValue.clear();

        for (UUID id : wp.uuidParams(inputName)) {
            Object item = Query.fromAll().where("_id = ?", id).resolveInvisible().first();
            if (item != null) {
                fieldValue.add(item);
            }
        }
    }

    if (!ObjectUtils.isBlank(field.as(Renderer.FieldData.class).getListLayouts())) {
        state.as(Renderer.Data.class).getListLayouts().put(fieldName, wp.params(String.class, layoutsName));
    }

    State.getInstance(request.getAttribute("containerObject")).as(BulkUploadDraft.class).setRunAfterSave(true);
    state.putValue(fieldName, fieldValue);
    return;
}

// --- Presentation ---

UUID containerObjectId = State.getInstance(request.getAttribute("containerObject")).getId();

{
    Set<ObjectType> types = field.getTypes();
    final StringBuilder typeIdsCsv = new StringBuilder();
    StringBuilder typeIdsQuery = new StringBuilder();
    boolean previewable = field.as(ToolUi.class).isDisplayGrid();

    if (types != null && !types.isEmpty()) {
        for (ObjectType type : types) {
            typeIdsCsv.append(type.getId()).append(",");
            typeIdsQuery.append("typeId=").append(type.getId()).append("&");

            if (!ObjectUtils.isBlank(type.getPreviewField())) {
                previewable = true;
            }
        }

        typeIdsCsv.setLength(typeIdsCsv.length() - 1);
        typeIdsQuery.setLength(typeIdsQuery.length() - 1);
    }

    PageWriter writer = wp.getWriter();
    Map<String, List<String>> layouts = field.as(Renderer.FieldData.class).getListLayouts();

    if (layouts != null && !layouts.isEmpty()) {
        String containerId = wp.createId();

        Map<String, HtmlGrid> grids = new HashMap<String, HtmlGrid>();

        writer.writeStart("style", "type", "text/css");
            writer.writeCommonGridCss();

            for (String layoutName : layouts.keySet()) {
                HtmlGrid grid = HtmlGrid.Static.find(application, layoutName);

                if (grid == null) {
                    throw new IllegalArgumentException(String.format(
                            "[%s] isn't a valid layout! Check your CSS and makes sure it's defined properly.",
                            layoutName));
                }

                List<CssUnit> frColumns = new ArrayList<CssUnit>();
                List<List<String>> frTemplate = new ArrayList<List<String>>();

                for (CssUnit column : grid.getColumns()) {
                    String unit = column.getUnit();

                    if ("px".equals(unit)) {
                        column = new CssUnit(column.getNumber() / 200, "fr");

                    } else if ("em".equals(unit)) {
                        column = new CssUnit(column.getNumber() / 20, "fr");
                    }

                    frColumns.add(column);
                }

                for (List<String> t : grid.getTemplate()) {
                    frTemplate.add(new ArrayList<String>(t));
                }

                grid = new HtmlGrid(frColumns, grid.getRows(), frTemplate);

                grids.put(layoutName, grid);
                writer.writeGridCss("." + layoutName, grid);
            }
        writer.writeEnd();

        writer.start("div",
                "class", "inputLarge repeatableLayout",
                "id", containerId);
            writer.start("ol");

                List<String> fieldLayoutNames = state.as(Renderer.Data.class).getListLayouts().get(fieldName);

                if (fieldLayoutNames != null) {
                    int itemIndex = 0;

                    for (String fieldLayoutName : fieldLayoutNames) {
                        writer.start("li");

                            List<String> layoutNames = new ArrayList<String>(layouts.keySet());
                            Collections.sort(layoutNames);

                            writer.start("div", "class", "repeatableLabel");
                                writer.start("select",
                                        "class", "toggleable",
                                        "data-root", "li",
                                        "name", layoutsName);
                                    for (String layoutName : layoutNames) {
                                        writer.start("option",
                                                "data-hide", ".layout",
                                                "data-show", "." + layoutName,
                                                "selected", layoutName.equals(fieldLayoutName) ? "selected" : null,
                                                "value", layoutName);
                                            writer.html(StringUtils.toLabel(layoutName));
                                        writer.end();
                                    }
                                writer.end();
                            writer.end();

                            writer.start("div", "class", "layouts");
                                for (String layoutName : layoutNames) {
                                    HtmlGrid grid = grids.get(layoutName);
                                    List<HtmlObject> values = new ArrayList<HtmlObject>();

                                    for (int i = 0, size = grid.getAreas().size(); i < size; ++ i) {
                                        String itemClass = i < layouts.get(layoutName).size() ? layouts.get(layoutName).get(i) : null;
                                        final StringBuilder itemTypeIdsCsv = new StringBuilder();
                                        final Set<ObjectType> itemTypes = itemClass != null ? Database.Static.getDefault().getEnvironment().getTypesByGroup(itemClass) : null;

                                        if (itemTypes == null || itemTypes.isEmpty()) {
                                            itemTypeIdsCsv.append(typeIdsCsv);

                                        } else {
                                            for (Iterator<ObjectType> j = itemTypes.iterator(); j.hasNext(); ) {
                                                ObjectType type = j.next();

                                                if (type.isAbstract() || type.as(ToolUi.class).isHidden()) {
                                                    j.remove();

                                                } else {
                                                    itemTypeIdsCsv.append(type.getId()).append(",");
                                                }
                                            }
                                            itemTypeIdsCsv.setLength(itemTypeIdsCsv.length() - 1);
                                        }

                                        final State itemState;

                                        if (!layoutName.equals(fieldLayoutName)) {
                                            itemState = null;

                                        } else {
                                            itemState = itemIndex < fieldValue.size() ? State.getInstance(fieldValue.get(itemIndex)) : null;
                                            ++ itemIndex;
                                        }

                                        final boolean embedded = !isValueExternal;

                                        values.add(new HtmlObject() {
                                            public void format(HtmlWriter writer) throws IOException {
                                                StorageItem preview = itemState != null ? itemState.getPreview() : null;

                                                writer.start("div", "class", "inputContainer-listLayoutItemContainer" + (embedded ? " inputContainer-listLayoutItemContainer-embedded" : ""));
                                                    writer.start("div", "class", "inputContainer-listLayoutItem");
                                                        if (embedded) {
                                                            List<Object> validObjects = new ArrayList<Object>();

                                                            for (ObjectType type : itemTypes) {
                                                                if (itemState != null && type.equals(itemState.getType())) {
                                                                    validObjects.add(itemState.getOriginalObject());

                                                                } else {
                                                                    Object itemObj = type.createObject(null);
                                                                    State.getInstance(itemObj).setResolveInvisible(true);
                                                                    validObjects.add(itemObj);
                                                                }
                                                            }

                                                            Collections.sort(validObjects, new ObjectFieldComparator("_type/_label", false));

                                                            String validObjectClass = wp.createId();
                                                            Map<UUID, String> showClasses = new HashMap<UUID, String>();

                                                            wp.writeStart("div", "class", "inputSmall");
                                                                wp.writeStart("select",
                                                                        "class", "toggleable",
                                                                        "data-root", ".inputContainer-listLayoutItem",
                                                                        "name", idName);
                                                                    wp.writeStart("option",
                                                                            "data-hide", "." + validObjectClass,
                                                                            "value", "");
                                                                        wp.writeHtml("None");
                                                                    wp.writeEnd();

                                                                    for (Object validObject : validObjects) {
                                                                        State validState = State.getInstance(validObject);
                                                                        String showClass = wp.createId();

                                                                        showClasses.put(validState.getId(), showClass);

                                                                        wp.writeStart("option",
                                                                                "data-hide", "." + validObjectClass,
                                                                                "data-show", "." + showClass,
                                                                                "selected", itemState != null && validObject.equals(itemState.getOriginalObject()) ? "selected" : null,
                                                                                "value", validState.getId());
                                                                            wp.writeTypeLabel(validObject);
                                                                        wp.writeEnd();
                                                                    }
                                                                wp.writeEnd();
                                                            wp.writeEnd();

                                                            for (Object validObject : validObjects) {
                                                                State validState = State.getInstance(validObject);
                                                                Date validObjectPublishDate = validState.as(Content.ObjectModification.class).getPublishDate();

                                                                wp.writeStart("div",
                                                                        "class", "inputLarge " + validObjectClass + " " + showClasses.get(validState.getId()));
                                                                    wp.writeElement("input",
                                                                            "name", typeIdName,
                                                                            "type", "hidden",
                                                                            "value", validState.getTypeId());

                                                                    wp.writeElement("input",
                                                                            "name", publishDateName,
                                                                            "type", "hidden",
                                                                            "value", validObjectPublishDate != null ? validObjectPublishDate.getTime() : null);

                                                                    if (validState.equals(itemState)) {
                                                                        try {
                                                                            wp.writeFormFields(validObject);

                                                                        } catch (ServletException error) {
                                                                            throw new IOException(error);
                                                                        }

                                                                    } else {
                                                                        wp.writeStart("a",
                                                                                "class", "lazyLoad",
                                                                                "href", wp.cmsUrl("/contentFormFields",
                                                                                        "typeId", validState.getTypeId(),
                                                                                        "id", validState.getId()));
                                                                            wp.writeHtml("Loading...");
                                                                        wp.writeEnd();
                                                                    }
                                                                wp.writeEnd();
                                                            }

                                                        } else {
                                                            writer.writeElement("input",
                                                                    "type", "text",
                                                                    "class", "objectId",
                                                                    "data-searcher-path", field.as(ToolUi.class).getInputSearcherPath(),
                                                                    "data-label", itemState != null ? itemState.getLabel() : null,
                                                                    "data-typeIds", itemTypeIdsCsv,
                                                                    "data-pathed", ToolUi.isOnlyPathed(field),
                                                                    "data-dynamic-predicate", field.getPredicate(),
                                                                    "data-preview", preview != null ? preview.getUrl() : null,
                                                                    "name", inputName,
                                                                    "value", itemState != null ? itemState.getId() : null);
                                                        }
                                                    writer.end();
                                                writer.end();
                                            }
                                        });
                                    }

                                    writer.start("div", "class", "layout " + layoutName);
                                        writer.grid(values, grid, true);
                                    writer.end();
                                }
                            writer.end();

                        writer.end();
                    }
                }

                writer.writeStart("script", "type", "text/template");
                    writer.start("li");

                        List<String> layoutNames = new ArrayList<String>(layouts.keySet());
                        Collections.sort(layoutNames);

                        writer.start("div", "class", "repeatableLabel");
                            writer.start("select",
                                    "class", "toggleable",
                                    "data-root", "li",
                                    "name", layoutsName);
                                for (String layoutName : layoutNames) {
                                    writer.start("option",
                                            "data-hide", ".layout",
                                            "data-show", "." + layoutName,
                                            "value", layoutName);
                                        writer.html(StringUtils.toLabel(layoutName));
                                    writer.end();
                                }
                            writer.end();
                        writer.end();

                        writer.start("div", "class", "layouts");
                            for (String layoutName : layoutNames) {
                                HtmlGrid grid = grids.get(layoutName);
                                List<HtmlObject> values = new ArrayList<HtmlObject>();

                                for (int i = 0, size = grid.getAreas().size(); i < size; ++ i) {
                                    String itemClass = i < layouts.get(layoutName).size() ? layouts.get(layoutName).get(i) : null;
                                    final StringBuilder itemTypeIdsCsv = new StringBuilder();
                                    final Set<ObjectType> itemTypes = itemClass != null ? Database.Static.getDefault().getEnvironment().getTypesByGroup(itemClass) : null;

                                    if (itemTypes == null || itemTypes.isEmpty()) {
                                        itemTypeIdsCsv.append(typeIdsCsv);

                                    } else {
                                        for (Iterator<ObjectType> j = itemTypes.iterator(); j.hasNext(); ) {
                                            ObjectType type = j.next();

                                            if (type.isAbstract() || type.as(ToolUi.class).isHidden()) {
                                                j.remove();

                                            } else {
                                                itemTypeIdsCsv.append(type.getId()).append(",");
                                            }
                                        }
                                        itemTypeIdsCsv.setLength(itemTypeIdsCsv.length() - 1);
                                    }

                                    final boolean embedded = !isValueExternal;

                                    values.add(new HtmlObject() {
                                        public void format(HtmlWriter writer) throws IOException {
                                            writer.start("div", "class", "inputContainer-listLayoutItemContainer" + (embedded ? " inputContainer-listLayoutItemContainer-embedded" : ""));
                                                writer.start("div", "class", "inputContainer-listLayoutItem");
                                                    if (embedded) {
                                                        List<Object> validObjects = new ArrayList<Object>();

                                                        for (ObjectType type : itemTypes) {
                                                            Object itemObj = type.createObject(null);
                                                            State.getInstance(itemObj).setResolveInvisible(true);
                                                            validObjects.add(itemObj);
                                                        }

                                                        Collections.sort(validObjects, new ObjectFieldComparator("_type/_label", false));

                                                        String validObjectClass = wp.createId();
                                                        Map<UUID, String> showClasses = new HashMap<UUID, String>();

                                                        wp.writeStart("div", "class", "inputSmall");
                                                            wp.writeStart("select",
                                                                    "class", "toggleable",
                                                                    "data-root", ".inputContainer-listLayoutItem",
                                                                    "name", idName);
                                                                wp.writeStart("option",
                                                                        "data-hide", "." + validObjectClass,
                                                                        "value", "");
                                                                    wp.writeHtml("None");
                                                                wp.writeEnd();

                                                                for (Object validObject : validObjects) {
                                                                    State validState = State.getInstance(validObject);
                                                                    String showClass = wp.createId();

                                                                    showClasses.put(validState.getId(), showClass);

                                                                    wp.writeStart("option",
                                                                            "data-hide", "." + validObjectClass,
                                                                            "data-show", "." + showClass,
                                                                            "value", validState.getId());
                                                                        wp.writeTypeLabel(validObject);
                                                                    wp.writeEnd();
                                                                }
                                                            wp.writeEnd();
                                                        wp.writeEnd();

                                                        for (Object validObject : validObjects) {
                                                            State validState = State.getInstance(validObject);
                                                            Date validObjectPublishDate = validState.as(Content.ObjectModification.class).getPublishDate();

                                                            wp.writeStart("div",
                                                                    "class", "inputLarge " + validObjectClass + " " + showClasses.get(validState.getId()));
                                                                wp.writeElement("input",
                                                                        "name", typeIdName,
                                                                        "type", "hidden",
                                                                        "value", validState.getTypeId());

                                                                wp.writeElement("input",
                                                                        "name", publishDateName,
                                                                        "type", "hidden",
                                                                        "value", validObjectPublishDate != null ? validObjectPublishDate.getTime() : null);

                                                                wp.writeStart("a",
                                                                        "class", "lazyLoad",
                                                                        "href", wp.cmsUrl("/contentFormFields",
                                                                                "typeId", validState.getTypeId(),
                                                                                "id", validState.getId()));
                                                                    wp.writeHtml("Loading...");
                                                                wp.writeEnd();
                                                            wp.writeEnd();
                                                        }

                                                    } else {
                                                        writer.writeElement("input",
                                                                "type", "text",
                                                                "class", "objectId",
                                                                "data-searcher-path", field.as(ToolUi.class).getInputSearcherPath(),
                                                                "data-typeIds", itemTypeIdsCsv,
                                                                "data-pathed", ToolUi.isOnlyPathed(field),
                                                                "data-dynamic-predicate", field.getPredicate(),
                                                                "name", inputName);
                                                    }
                                                writer.end();
                                            writer.end();
                                        }
                                    });
                                }

                                writer.start("div", "class", "layout " + layoutName);
                                    writer.grid(values, grid, true);
                                writer.end();
                            }
                        writer.end();

                    writer.end();
                writer.writeEnd();
            writer.end();
        writer.end();

        return;
    }
}

if (!isValueExternal) {
    Set<ObjectType> bulkUploadTypes = new HashSet<ObjectType>();

    for (ObjectType t : validTypes) {
        for (ObjectField f : t.getFields()) {
            if (f.as(ToolUi.class).isBulkUpload()) {
                for (ObjectType ft : f.getTypes()) {
                    bulkUploadTypes.add(ft);
                }
            }
        }
    }

    boolean displayGrid = field.as(ToolUi.class).isDisplayGrid();

    StringBuilder genericArgumentsString = new StringBuilder();
    List<ObjectType> genericArguments = field.getGenericArguments();

    if (genericArguments != null && !genericArguments.isEmpty()) {
        for (ObjectType type : genericArguments) {
            genericArgumentsString.append(type.getId());
            genericArgumentsString.append(",");
        }

        genericArgumentsString.setLength(genericArgumentsString.length() - 1);
    }

    wp.writeStart("div",
            "class", "inputLarge repeatableForm" + (displayGrid ? " repeatableForm-previewable" : ""),
            "foo", "bar",
            "data-generic-arguments", genericArgumentsString);

        wp.writeStart("ol",
                "data-sortable-input-name", inputName,
                "data-sortable-valid-item-types", validTypes.stream()
                        .map(ObjectType::getId)
                        .map(UUID::toString)
                        .collect(Collectors.joining(" ")));

            for (Object item : fieldValue) {
                State itemState = State.getInstance(item);
                ObjectType itemType = itemState.getType();
                Date itemPublishDate = itemState.as(Content.ObjectModification.class).getPublishDate();

                wp.writeStart("li",
                        "data-sortable-item-type", itemType.getId(),
                        "data-type", wp.getObjectLabel(itemType),
                        "data-label", wp.getObjectLabel(item),
                        
                        // Add the image url for the preview thumbnail, plus the field name that provided the thumbnail
                        // so if that field is changed the front-end knows that the thumbnail should also be updated
                        "data-preview", wp.getPreviewThumbnailUrl(item),
                        "data-preview-field", itemType.getPreviewField()
                        
                        );
                    wp.writeElement("input",
                            "type", "hidden",
                            "name", idName,
                            "value", itemState.getId());

                    wp.writeElement("input",
                            "type", "hidden",
                            "name", typeIdName,
                            "value", itemType.getId());

                    wp.writeElement("input",
                            "type", "hidden",
                            "name", publishDateName,
                            "value", itemPublishDate != null ? itemPublishDate.getTime() : null);

                    if (!itemState.hasAnyErrors()) {
                        wp.writeElement("input",
                                "type", "hidden",
                                "name", dataName,
                                "value", ObjectUtils.toJson(itemState.getSimpleValues()),
                                "data-form-fields-url", wp.cmsUrl(
                                        "/contentFormFields",
                                        "typeId", itemType.getId(),
                                        "id", itemState.getId()));

                    } else {
                        wp.writeFormFields(item);
                    }
                wp.writeEnd();
            }

            for (ObjectType type : validTypes) {
                wp.writeStart("script", "type", "text/template");
                    wp.writeStart("li",
                            "class", displayGrid ? "collapsed" : null,
                            "data-sortable-item-type", type.getId(),
                            "data-type", wp.getObjectLabel(type),
                            // Add the name of the preview field so the front end knows
                            // if that field is updated it should update the thumbnail
                            "data-preview-field", type.getPreviewField());
                        wp.writeStart("a",
                                "href", wp.cmsUrl("/content/repeatableObject.jsp",
                                        "inputName", inputName,
                                        "typeId", type.getId()));
                        wp.writeEnd();
                    wp.writeEnd();
                wp.writeEnd();
            }
        wp.writeEnd();

        if (!bulkUploadTypes.isEmpty() && !field.as(ToolUi.class).isReadOnly()) {
            StringBuilder typeIdsQuery = new StringBuilder();

            for (ObjectType type : bulkUploadTypes) {
                typeIdsQuery.append("typeId=").append(type.getId()).append("&");
            }

            typeIdsQuery.setLength(typeIdsQuery.length() - 1);

            wp.writeStart("a",
                    "class", "action-upload",
                    "href", wp.url("/content/uploadFiles?" + typeIdsQuery, "containerId", containerObjectId),
                    "target", "uploadFiles");
                wp.writeHtml("Upload Files");
            wp.writeEnd();
        }
    wp.writeEnd();

} else {
    Set<ObjectType> valueTypes = field.getTypes();
    final StringBuilder typeIdsCsv = new StringBuilder();
    StringBuilder typeIdsQuery = new StringBuilder();

    if (valueTypes != null && !valueTypes.isEmpty()) {
        for (ObjectType valueType : valueTypes) {
            typeIdsCsv.append(valueType.getId()).append(",");
            typeIdsQuery.append("typeId=").append(valueType.getId()).append("&");
        }

        typeIdsCsv.setLength(typeIdsCsv.length() - 1);
        typeIdsQuery.setLength(typeIdsQuery.length() - 1);
    }

    boolean displayGrid = field.as(ToolUi.class).isDisplayGrid();

    PageWriter writer = wp.getWriter();

    writer.start("div", "class", "inputSmall repeatableObjectId" + (displayGrid ? " repeatableObjectId-previewable" : ""));

        if (fieldValue == null || fieldValue.isEmpty()) {

            String dynamicPlaceholderText = field.as(ToolUi.class).getPlaceholderDynamicText();
            String placeholder = field.as(ToolUi.class).getPlaceholder();

            if (!ObjectUtils.isBlank(dynamicPlaceholderText)) {
                writer.start("span", "class", "objectId-placeholder", "id", field.getId() + "-placeholder", "data-dynamic-text", dynamicPlaceholderText).end();
            } else if (!ObjectUtils.isBlank(placeholder)) {
                writer.start("span", "class", "objectId-placeholder", "id", field.getId() + "-placeholder");
                    writer.html(placeholder);
                writer.end();
            }
        }

        writer.start("ol",
                    "data-sortable-input-name", inputName,
                    "data-sortable-valid-item-types", validTypes.stream()
                            .map(ObjectType::getId)
                            .map(UUID::toString)
                            .collect(Collectors.joining(" ")));

            if (fieldValue != null) {
                for (Object item : fieldValue) {
                    writer.start("li",
                            "data-sortable-item-type", State.getInstance(item).getTypeId());

                        wp.writeObjectSelect(field, item, "name", inputName);
                    writer.end();
                }
            }
            writer.writeStart("script", "type", "text/template");
                writer.start("li");
                    wp.writeObjectSelect(field, null, "name", inputName);
                writer.end();
            writer.writeEnd();
        writer.end();

        if (displayGrid && !field.as(ToolUi.class).isReadOnly()) {
            writer.start("a",
                    "class", "action-upload",
                    "href", wp.url("/content/uploadFiles?" + typeIdsQuery, "containerId", containerObjectId),
                    "target", "uploadFiles");
                writer.html("Upload Files");
            writer.end();
        }
    writer.end();
}
%>
