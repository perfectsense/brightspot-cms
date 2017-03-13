<%@ page session="false" import="

com.psddev.cms.db.BulkUploadDraft,
com.psddev.cms.db.Content,
com.psddev.cms.db.Renderer,
com.psddev.cms.db.ToolUi,
com.psddev.cms.tool.ObjectTypeOrContentTemplate,
com.psddev.cms.tool.PageWriter,
com.psddev.cms.tool.ToolPageContext,
com.psddev.cms.tool.page.content.Edit,

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
, com.psddev.cms.tool.page.UploadFiles, com.psddev.dari.util.TypeReference" %><%

// --- Logic ---

final ToolPageContext wp = new ToolPageContext(pageContext);

State state = State.getInstance(request.getAttribute("object"));

final ObjectField field = (ObjectField) request.getAttribute("field");
String fieldName = field.getInternalName();
List<Object> fieldValue = (List<Object>) state.getValue(fieldName);
if (fieldValue == null) {
    fieldValue = new ArrayList<Object>();

} else {
    for (Iterator<Object> i = fieldValue.iterator(); i.hasNext();) {
        if (i.next() == null) {
            i.remove();
        }
    }
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
                                                                    "data-dynamic-searcher-path", field.as(ToolUi.class).getDynamicInputSearcherPath(),
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
                                                                "data-dynamic-searcher-path", field.as(ToolUi.class).getDynamicInputSearcherPath(),
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
    Map<ObjectType, String> weightedTypesAndFieldsMap = new CompactMap<ObjectType, String>();
    Map<ObjectType, String> weightedColorTypesAndFieldsMap = new CompactMap<ObjectType, String>();
    Map<ObjectType, Double> weightedTypesAndTotalsMap = new CompactMap<ObjectType, Double>();
    Map<ObjectType, String> toggleTypesAndFieldsMap = new CompactMap<ObjectType, String>();
    Map<ObjectType, String> progressTypesAndFieldsMap = new CompactMap<ObjectType, String>();
    Map<ObjectType, String> weightMarkersTypesAndFieldsMap = new CompactMap<ObjectType, String>();
    int calculatedWeightsFieldCount = 0;

    for (ObjectType t : validTypes) {
        for (ObjectField f : t.getFields()) {
            ToolUi ui = f.as(ToolUi.class);
            if (ui.isBulkUpload()) {
                for (ObjectType ft : f.getTypes()) {
                    bulkUploadTypes.add(ft);
                }
            }
            if (ui.isCollectionItemWeight()) {
                weightedTypesAndFieldsMap.put(t, f.getInternalName());
                if (ui.isCollectionItemWeightCalculated()) {
                    calculatedWeightsFieldCount ++;
                }
            }
            if (ui.isCollectionItemWeightColor()) {
                weightedColorTypesAndFieldsMap.put(t, f.getInternalName());
            }
            if (ui.isCollectionItemWeightMarker()) {
                weightMarkersTypesAndFieldsMap.put(t, f.getInternalName());
            }
            if (ui.isCollectionItemToggle()) {
                toggleTypesAndFieldsMap.put(t, f.getInternalName());
            }
            if (ui.isCollectionItemProgress()) {
                progressTypesAndFieldsMap.put(t, f.getInternalName());
            }
        }
    }

    boolean displayGrid = field.as(ToolUi.class).isDisplayGrid();

    // Only display weights if all valid types have a @ToolUi.CollectionItemWeight annotated field
    int validTypesSize = validTypes.size();
    boolean displayWeights = weightedTypesAndFieldsMap.size() == validTypesSize;
    boolean weightsCalculated = calculatedWeightsFieldCount == validTypesSize;
    boolean displayAlternateListUi = displayWeights || toggleTypesAndFieldsMap.size() > 0 || progressTypesAndFieldsMap.size() > 0;

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
            "class", "inputLarge repeatableForm"
                    + (displayGrid ? " repeatableForm-previewable" : "")
                    + (displayWeights ? " repeatableForm-weighted" : "")
                    + (displayAlternateListUi ? " repeatableForm-alt" : ""),
            "data-generic-arguments", genericArgumentsString);

        if (displayWeights) {
            wp.writeStart("div",
                    "class", "repeatableForm-itemWeights",
                    "data-calculated", weightsCalculated);

            wp.writeEnd();
        }

        wp.writeStart("ol",
                "data-sortable-input-name", inputName,
                "data-sortable-valid-item-types", validTypes.stream()
                        .map(ObjectType::getId)
                        .map(UUID::toString)
                        .collect(Collectors.joining(" ")));

            for (Object item : fieldValue) {
                State itemState = State.getInstance(item);
                ObjectType itemType = itemState.getType();
                String weightFieldName = weightedTypesAndFieldsMap.get(itemType);

                if (!StringUtils.isBlank(weightFieldName)) {
                    double weight = ObjectUtils.to(double.class, itemState.get(weightFieldName));
                    Double total = weightedTypesAndTotalsMap.get(itemType);
                    weightedTypesAndTotalsMap.put(itemType, (total != null ? total : 0.0) + weight);
                }
            }

            for (Object item : fieldValue) {
                State itemState = State.getInstance(item);
                ObjectType itemType = itemState.getType();
                Date itemPublishDate = itemState.as(Content.ObjectModification.class).getPublishDate();

                boolean itemExpanded = field.as(ToolUi.class).isExpanded() || Edit.isWorkInProgressRestored(wp, item);
                boolean childExpanded = itemType.getFields().stream().anyMatch(f -> f.as(ToolUi.class).isExpanded());
                boolean expanded = itemExpanded || childExpanded;

                String progressFieldName = progressTypesAndFieldsMap.get(itemType);
                String toggleFieldName = toggleTypesAndFieldsMap.get(itemType);
                String weightFieldName = weightedTypesAndFieldsMap.get(itemType);
                String weightColorFieldName = weightedColorTypesAndFieldsMap.get(itemType);
                String weightMarkersFieldName = weightMarkersTypesAndFieldsMap.get(itemType);

                Double weight = !StringUtils.isBlank(weightFieldName) ? ObjectUtils.to(double.class, itemState.get(weightFieldName)) : null;
                Double total = weightedTypesAndTotalsMap.get(itemType);

                wp.writeStart("li",
                        "class", itemExpanded ? "expanded" : (childExpanded ? "expanded collapsed" : null),
                        "data-sortable-item-type", itemType.getId(),
                        "data-type", wp.getObjectLabel(itemType),
                        "data-label", wp.getObjectLabel(item),
                        "data-label-html", item != null ? wp.createObjectLabelHtml(item) : null,

                        // Add the image url for the preview thumbnail, plus the field name that provided the thumbnail
                        // so if that field is changed the front-end knows that the thumbnail should also be updated
                        "data-preview", wp.getPreviewThumbnailUrl(item),
                        "data-preview-field", itemType.getPreviewField(),

                        // Add additional data attributes for customizing embedded item display
                        "data-toggle-field", !StringUtils.isBlank(toggleFieldName) ? toggleFieldName : null,
                        "data-weight-field", !StringUtils.isBlank(weightFieldName) ? weightFieldName : null,
                        "data-weight-color-field", !StringUtils.isBlank(weightColorFieldName) ? weightColorFieldName : null,
                        "data-weight-markers-field", !StringUtils.isBlank(weightMarkersFieldName) ? weightMarkersFieldName : null,
                        "data-progress-field-value", !StringUtils.isBlank(progressFieldName) ? ObjectUtils.to(int.class, ObjectUtils.to(double.class, itemState.get(progressFieldName)) * 100) : null,
                        "data-toggle-field-value", !StringUtils.isBlank(toggleFieldName) ? ObjectUtils.to(boolean.class, itemState.get(toggleFieldName)) : null,
                        "data-weight-field-value", weight != null ? (total != null ? weight / total : weight) : null,
                        "data-weight-color-field-value", !StringUtils.isBlank(weightColorFieldName) ? ObjectUtils.to(String.class, itemState.get(weightColorFieldName)) : null,
                        "data-weight-markers-field-value", !StringUtils.isBlank(weightMarkersFieldName) ? ObjectUtils.to(new TypeReference<List<Double>>() {}, itemState.get(weightMarkersFieldName)) : null
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

                    if (!expanded && !itemState.hasAnyErrors()
                            && StringUtils.isBlank(toggleFieldName)
                            && StringUtils.isBlank(weightFieldName)
                            && StringUtils.isBlank(weightColorFieldName)) {
                        wp.writeElement("input",
                                "type", "hidden",
                                "name", dataName,
                                "value", ObjectUtils.toJson(itemState.getSimpleValues()),
                                "data-form-fields-url", wp.cmsUrl(
                                        "/contentFormFields",
                                        "typeId", itemType.getId(),
                                        "id", itemState.getId()));

                    } else {
                        wp.writeElement("input",
                                "type", "hidden",
                                "name", dataName,
                                "value", "");

                        wp.writeFormFields(item);
                    }
                wp.writeEnd();
            }

            for (ObjectTypeOrContentTemplate otct : wp.getObjectTypeOrContentTemplates(validTypes, true)) {
                ObjectType type = otct.getType();
                boolean expanded = field.as(ToolUi.class).isExpanded()
                        || type.getFields().stream().anyMatch(f -> f.as(ToolUi.class).isExpanded());

                String progressFieldName = progressTypesAndFieldsMap.get(type);
                String toggleFieldName = toggleTypesAndFieldsMap.get(type);
                String weightFieldName = weightedTypesAndFieldsMap.get(type);
                String weightColorFieldName = weightedColorTypesAndFieldsMap.get(type);
                String weightMarkersFieldName = weightMarkersTypesAndFieldsMap.get(type);

                wp.writeStart("script", "type", "text/template");
                    wp.writeStart("li",
                            "class", (expanded ? "expanded" : "") + (displayGrid ? " collapsed" : ""),
                            "data-sortable-item-type", type.getId(),
                            "data-type", otct.getLabel(),
                            // Add the name of the preview field so the front end knows
                            // if that field is updated it should update the thumbnail
                            "data-preview-field", type.getPreviewField(),
                            "data-toggle-field", !StringUtils.isBlank(toggleFieldName) ? toggleFieldName : null,
                            "data-weight-field", !StringUtils.isBlank(weightFieldName) ? weightFieldName : null,
                            "data-weight-color-field", !StringUtils.isBlank(weightColorFieldName) ? weightColorFieldName : null,
                            "data-weight-markers-field", !StringUtils.isBlank(weightMarkersFieldName) ? weightMarkersFieldName : null,
                            "data-progress-field-value", !StringUtils.isBlank(progressFieldName) ? 0.0 : null,
                            "data-toggle-field-value", !StringUtils.isBlank(toggleFieldName) ? true : null,
                            "data-weight-field-value", !StringUtils.isBlank(weightFieldName) ? "" : null,
                            "data-weight-color-field-value", !StringUtils.isBlank(weightColorFieldName) ? "" : null
                    );
                        wp.writeStart("a",
                                "href", wp.cmsUrl("/content/repeatableObject.jsp",
                                        "inputName", inputName,
                                        "typeId", otct.getId()));
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

            String uploadFilesPath = wp.getCmsTool().isEnableFrontEndUploader()
                    ? "/content/upload"
                    : "/content/uploadFiles";

            wp.writeStart("a",
                    "class", "action-upload",
                    "href", wp.url(uploadFilesPath + "?" + typeIdsQuery,
                            "containerId", containerObjectId,
                            "context", UploadFiles.Context.FIELD),
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

            String uploadFilesPath = wp.getCmsTool().isEnableFrontEndUploader()
                    ? "/content/upload"
                    : "/content/uploadFiles";

            writer.start("a",
                    "class", "action-upload",
                    "href", wp.url(
                            uploadFilesPath + "?" + typeIdsQuery,
                            "containerId", containerObjectId,
                            "context", UploadFiles.Context.FIELD),
                    "target", "uploadFiles");
                writer.html("Upload Files");
            writer.end();
        }
    writer.end();
}
%>
