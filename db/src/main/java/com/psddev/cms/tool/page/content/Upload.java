package com.psddev.cms.tool.page.content;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import com.psddev.cms.tool.CmsTool;
import com.psddev.dari.db.ObjectMethod;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.util.SparseSet;
import com.psddev.dari.util.UuidUtils;
import com.psddev.cms.db.BulkUploadDraft;
import com.psddev.cms.db.ImageTag;
import com.psddev.cms.db.ResizeOption;
import com.psddev.cms.db.Site;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.db.Variation;
import com.psddev.cms.tool.PageServlet;
import com.psddev.cms.tool.Search;
import com.psddev.cms.tool.SearchResultSelection;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.cms.tool.page.content.field.FileField;
import com.psddev.cms.tool.search.MixedSearchResultView;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.DatabaseEnvironment;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectFieldComparator;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.State;
import com.psddev.dari.util.MultipartRequest;
import com.psddev.dari.util.MultipartRequestFilter;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RoutingFilter;
import com.psddev.dari.util.StorageItem;
import com.psddev.dari.util.StorageItemFilter;
import com.psddev.dari.util.StringUtils;

@RoutingFilter.Path(application = "cms", value = "/content/upload")
@SuppressWarnings("serial")
public class Upload extends PageServlet {

    private static final String CONTAINER_ID_PARAMETER = "containerId";

    @Override
    protected String getPermissionId() {
        return "area/dashboard";
    }

    @Override
    protected void doService(ToolPageContext page) throws IOException, ServletException {

        if (page.requireUser()) {
            return;
        }

        if (page.paramOrDefault(Boolean.class, "preview", false)) {
            writeFilePreview(page);
        } else {
            reallyDoService(page);
        }
    }

    private static void reallyDoService(ToolPageContext page) throws IOException, ServletException {
        Database database = Database.Static.getDefault();
        DatabaseEnvironment environment = database.getEnvironment();
        Set<ObjectType> uploadableTypes;
        Set<SmartUploadableType> smartUploadableTypes = new LinkedHashSet<>();
        CmsTool cms = page.getCmsTool();
        boolean isEffectivelySmartUpload = !cms.isUseOldUploader() && cms.isEnableSmartUploader();

        if (isEffectivelySmartUpload) {
            smartUploadableTypes = getSmartUploadableTypes(page);

            // Even if it is enabled via CmsTool settings, the Smart Uploader
            // cannot (and will not) be used if there are is no ObjectType with
            // content types specified. If this is the case, we will fallback to
            // the normal Front End Uploader experience.
            isEffectivelySmartUpload = !smartUploadableTypes.isEmpty();
        }

        if (cms.isUseOldUploader()) {
            uploadableTypes = getUploadableTypes(
                    page,
                    type -> type.getFields().stream().anyMatch(field -> !(field instanceof ObjectMethod) && ObjectField.FILE_TYPE.equals(field.getInternalType()))
            );

        } else {
            uploadableTypes = getUploadableTypes(
                    page,
                    type -> getEffectiveRestrictedUploadField(type) != null
            );
        }

        ObjectType selectedType = environment.getTypeById(page.param(UUID.class, "type"));
        Exception postError = null;

        if (page.isFormPost()) {
            database.beginWrites();

            try {
                MultipartRequest request = MultipartRequestFilter.Static.getInstance(page.getRequest());

                if (request == null) {
                    throw new IllegalStateException("Not multipart!");
                }

                StringBuilder js = new StringBuilder();
                List<UUID> newObjectIds = new ArrayList<>();

                if (isEffectivelySmartUpload) {
                    try {
                        for (ObjectType type : page.params(UUID.class, "typeId").stream()
                                .map(environment::getTypeById)
                                .collect(Collectors.toList())) {

                            createObjectsFromUpload(page, type, js, smartUploadableTypes, newObjectIds);
                        }

                    } catch (IllegalArgumentException uploadError) {
                        page.getErrors().add(uploadError);
                    }

                } else {
                    createObjectsFromUpload(page, selectedType, js, null, newObjectIds);
                }

                database.commitWrites();

                if (page.getErrors().isEmpty()) {

                    if (Context.FIELD.equals(page.param(Context.class, "context"))) {
                        page.writeStart("div", "id", page.createId()).writeEnd();

                        page.writeStart("script", "type", "text/javascript");
                            page.write("if (typeof jQuery !== 'undefined') (function($, win, undef) {");
                                page.write("var $page = $('#" + page.getId() + "'),");
                                page.write("$init = $page.popup('source').repeatable('closestInit'),");
                                    page.write("$addButton = $init.find('.addButton').eq(0),");
                                    page.write("$input;");
                                page.write("if ($addButton.length > 0) {");
                                    page.write(js.toString());
                                    page.write("$page.popup('close');");
                                page.write("}");
                            page.write("})(jQuery, window);");
                        page.writeEnd();
                    } else {

                        SearchResultSelection selection = page.getUser().resetCurrentSelection();
                        newObjectIds.forEach(selection::addItem);
                        database.commitWrites();

                        Search search = new Search();
                        search.setAdditionalPredicate(selection.createItemsQuery().getPredicate().toString());
                        search.setLimit(10);

                        page.writeStart("script", "type", "text/javascript");
                            page.write("if (typeof jQuery !== 'undefined') (function($, win, undef) {");
                                page.write("window.location = '");
                                page.write(page.cmsUrl("/searchAdvancedFull",
                                        "search", ObjectUtils.toJson(search.getState().getSimpleValues()),
                                        "view", MixedSearchResultView.class.getCanonicalName()));
                                page.write("';");
                            page.write("})(jQuery, window);");
                        page.writeEnd();

                    }

                    return;
                }

            } catch (Exception error) {
                postError = error;

            } finally {
                database.endWrites();
            }
        }

        List<ObjectType> types = new ArrayList<>(isEffectivelySmartUpload
                ? smartUploadableTypes.stream().map(SmartUploadableType::getType).collect(Collectors.toList())
                : uploadableTypes);
        types.sort(new ObjectFieldComparator("name", false));

        page.writeStart("h1");
            page.writeHtml(page.localize(Upload.class, "title"));
        page.writeEnd();

        page.writeStart("form",
                "method", "post",
                "enctype", "multipart/form-data",
                "action", page.url(null));

            page.writeElement("input",
                    "type", "hidden",
                    "name", CONTAINER_ID_PARAMETER,
                    "value", page.param(String.class, "containerId"));

            for (ObjectType type : types) {
                page.writeElement("input", "type", "hidden", "name", "typeId", "value", type.getId());
            }

            if (postError != null) {
                page.writeStart("div", "class", "message message-error");
                    page.writeObject(postError);
                page.writeEnd();

            } else if (!page.getErrors().isEmpty()) {
                page.writeStart("div", "class", "message message-error");
                    for (Throwable error : page.getErrors()) {
                        page.writeHtml(error.getMessage());
                    }
                page.writeEnd();
            }

            page.writeStart("div", "class", "inputContainer bulk-upload-files");
                page.writeStart("div", "class", "inputLabel");
                    page.writeStart("label", "for", page.createId());
                        page.writeHtml(page.localize(Upload.class, "label.files"));
                    page.writeEnd();
                page.writeEnd();
                page.writeStart("div", "class", "inputSmall");
                    page.writeElement("input",
                            "id", page.getId(),
                            page.getCmsTool().isEnableFrontEndUploader() ? "data-bsp-uploader" : "", "",
                            "type", "file",
                            "name", "file",
                            "multiple", "multiple");
                page.writeEnd();
            page.writeEnd();

            if (isEffectivelySmartUpload) {
                page.writeStart("div", "class", "objectInputs");
                    for (ObjectType type : types) {
                        String displayName = type.getDisplayName();

                        // Still show tab if there is only one smart uploadable type.
                        if (types.size() == 1) {
                            page.writeStart("div", "class", "tabs-wrapper");
                                page.writeStart("ul", "class", "tabs");
                                    page.writeStart("li", "class", "state-selected");
                                        page.writeStart("a", "href", "#").writeHtml(displayName).writeEnd();
                                    page.writeEnd();
                                page.writeEnd();
                            page.writeEnd();
                        }

                        page.writeStart("div", "data-tab", displayName);
                            Object common = type.createObject(null);

                            page.writeElement("input",
                                    "type", "hidden",
                                    "name", "typeForm-" + type.getId(),
                                    "value", State.getInstance(common).getId());

                            ObjectField uploadableField = getEffectiveRestrictedUploadField(type);

                            page.writeSomeFormFields(
                                    common,
                                    false,
                                    null,
                                    uploadableField != null ? Collections.singletonList(uploadableField.getInternalName()) : null);
                        page.writeEnd();
                    }
                page.writeEnd();

            } else {
                page.writeStart("div", "class", "inputContainer");
                    page.writeStart("div", "class", "inputLabel");
                        page.writeStart("label", "for", page.createId());
                            page.writeHtml(page.localize(Upload.class, "label.type"));
                        page.writeEnd();
                    page.writeEnd();
                    page.writeStart("div", "class", "inputSmall");
                        page.writeStart("select",
                                "class", "toggleable",
                                "data-root", "form",
                                "id", page.getId(),
                                "name", "type");
                            for (ObjectType type : types) {
                                UUID typeId = type.getId();

                                page.writeStart("option",
                                        "data-hide", ".typeForm",
                                        "data-show", ".typeForm-" + typeId,
                                        "selected", type.equals(selectedType) ? "selected" : null,
                                        "value", typeId);
                                    page.writeHtml(type.getDisplayName());
                                page.writeEnd();
                            }
                        page.writeEnd();
                    page.writeEnd();

                    page.writeStart("div", "class", "inputLarge");
                        for (ObjectType type : types) {
                            String name = "typeForm-" + type.getId();
                            Object common = type.createObject(null);

                            page.writeStart("div", "class", "typeForm " + name);
                                page.writeElement("input",
                                        "type", "hidden",
                                        "name", name,
                                        "value", State.getInstance(common).getId());

                                ObjectField uploadableField = getEffectiveRestrictedUploadField(type);

                                page.writeSomeFormFields(
                                        common,
                                        false,
                                        null,
                                        uploadableField != null ? Collections.singletonList(uploadableField.getInternalName()) : null);
                            page.writeEnd();
                        }
                    page.writeEnd();
                page.writeEnd();
            }

            page.writeElement("input", "type", "hidden", "name", "context", "value", page.param(Context.class, "context"));

            page.writeStart("div", "class", "buttons");
                page.writeStart("button", "name", "action-upload");
                    page.writeHtml(page.localize(Upload.class, "action.upload"));
                page.writeEnd();
            page.writeEnd();

        page.writeEnd();
    }

    // Returns type-field mapping of types that are explicitly set to be
    // uploadable ONLY if the corresponding file field has mime types
    // specified.
    private static Set<SmartUploadableType> getSmartUploadableTypes(ToolPageContext page) {
        Set<SmartUploadableType> smartUploadableTypes = new LinkedHashSet<>();

        for (ObjectType type : page.params(UUID.class, "typeId").stream()
                .map(id -> Database.Static.getDefault().getEnvironment().getTypeById(id))
                .filter(Objects::nonNull)
                .map(type -> type.as(ToolUi.class).findDisplayTypes())
                .flatMap(Collection::stream)
                .filter(type -> {
                    ObjectField field = getEffectiveRestrictedUploadField(type);
                    return field != null && field.getMimeTypes() != null;
                })
                .collect(Collectors.toList())) {

            ObjectField field = getEffectiveRestrictedUploadField(type);

            if (field == null) {
                continue;
            }

            List<String> mimeTypes = Arrays.stream(field.getMimeTypes().split(" "))
                    .filter(s -> s.startsWith("+"))
                    .collect(Collectors.toList());

            if (mimeTypes.isEmpty()) {
                continue;
            }

            // If there is any collision between mime types, skip the ambiguity.
            Set<SmartUploadableType> smartUploadableTypesToRemove = smartUploadableTypes.stream()
                    .filter(t -> !Collections.disjoint(
                            Arrays.stream(t.getField().getMimeTypes().split(" "))
                                    .filter(mt -> mt.startsWith("+"))
                                    .collect(Collectors.toList()),
                            mimeTypes))
                    .collect(Collectors.toSet());

            if (smartUploadableTypesToRemove.isEmpty()) {
                smartUploadableTypes.add(new SmartUploadableType(type, field));

            } else {
                smartUploadableTypes.removeAll(smartUploadableTypesToRemove);
            }
        }

        return smartUploadableTypes;
    }

    private static Set<ObjectType> getUploadableTypes(ToolPageContext page, Predicate<ObjectType> typePredicate) {
        return page.params(UUID.class, "typeId").stream()
                .map(id -> Database.Static.getDefault().getEnvironment().getTypeById(id))
                .filter(Objects::nonNull)
                .map(type -> type.as(ToolUi.class).findDisplayTypes())
                .flatMap(Collection::stream)
                .filter(typePredicate)
                .collect(Collectors.toSet());
    }

    private static void writeFilePreview(ToolPageContext page) throws IOException, ServletException {
        String inputName = ObjectUtils.firstNonBlank(page.param(String.class, "inputName"), (String) page.getRequest().getAttribute("inputName"), "file");
        StorageItem storageItem = StorageItemFilter.getParameter(page.getRequest(), inputName, null);

        HttpServletResponse response = page.getResponse();
        response.setContentType("text/html");

        String contentType = storageItem.getContentType();

        if (StringUtils.isBlank(contentType)) {
            return;
        }

        if (contentType.startsWith("image/")) {
            ImageTag.Builder imageTagBuilder = new ImageTag.Builder(storageItem);
            imageTagBuilder.setWidth(150);
            imageTagBuilder.setHeight(110);
            imageTagBuilder.setResizeOption(ResizeOption.ONLY_SHRINK_LARGER);

            page.writeStart("div");
            page.write(imageTagBuilder.toHtml());
            page.writeEnd();

        }
    }

    private static void createObjectsFromUpload(
            ToolPageContext page,
            ObjectType type,
            StringBuilder js,
            Set<SmartUploadableType> smartUploadableTypes,
            List<UUID> newObjectIds) throws IOException, ServletException {

        Object common = type.createObject(page.param(UUID.class, "typeForm-" + type.getId()));
        page.updateUsingParameters(common);
        ObjectField uploadableField = getEffectiveRestrictedUploadField(type);

        if (uploadableField == null) {
            return;
        }

        for (StorageItem file : StorageItemFilter.getParameters(
                page.getRequest(),
                "file",
                FileField.getStorageSetting(Optional.of(uploadableField)))) {

            if (file == null) {
                continue;
            }

            if (smartUploadableTypes != null) {
                String fileMimeType = file.getContentType();
                boolean validMimeType = false;
                boolean hasType = false;
                Set<SmartUploadableType> compatibleTypes = new LinkedHashSet<>();

                for (SmartUploadableType smartUploadableType : smartUploadableTypes) {

                    if (hasMimeType(smartUploadableType.getField(), fileMimeType)) {
                        validMimeType = true;
                        compatibleTypes.add(smartUploadableType);

                        if (smartUploadableType.getType().equals(type)) {
                            hasType = true;
                        }
                    }
                }

                if (!validMimeType) {
                    throw new IllegalArgumentException("Invalid mime type(s)!");
                }

                if (!hasType) {
                    continue;
                }

                // File should be mapped to field with most specific mime type.
                if (compatibleTypes.size() > 1 && compatibleTypes.stream()
                        .filter(t -> t.getType().equals(type))
                        .map(SmartUploadableType::getField)
                        .anyMatch(f -> Arrays.stream(f.getMimeTypes().split(" ")).anyMatch(mt -> mt.startsWith("+") && mt.endsWith("/")))) {

                    continue;
                }
            }

            Object object = type.createObject(null);
            State state = State.getInstance(object);
            state.setValues(State.getInstance(common));
            Site site = page.getSite();

            if (site != null && site.getDefaultVariation() != null) {
                state.as(Variation.Data.class).setInitialVariation(site.getDefaultVariation());
            }

            state.put(uploadableField.getInternalName(), file);
            state.as(BulkUploadDraft.class).setUploadId(UuidUtils.createSequentialUuid());
            state.as(BulkUploadDraft.class).setContainerId(page.param(String.class, "containerId"));
            page.publish(state);
            newObjectIds.add(state.getId());

            js.append("$addButton.repeatable('add', function() {");
            js.append("var $added = $(this);");
            js.append("$input = $added.find(':input.objectId').eq(0);");
            js.append("$input.attr('data-label', '").append(StringUtils.escapeJavaScript(state.getLabel())).append("');");
            js.append("$input.attr('data-label-html', '").append(StringUtils.escapeJavaScript(page.createObjectLabelHtml(state))).append("');");
            js.append("$input.attr('data-preview', '").append(StringUtils.escapeJavaScript(page.getPreviewThumbnailUrl(object))).append("');");
            js.append("$input.val('").append(StringUtils.escapeJavaScript(state.getId().toString())).append("');");
            js.append("$input.change();");
            js.append("});");
        }
    }

    private static boolean hasMimeType(ObjectField field, String mimeType) {
        String mimeTypes = field.getMimeTypes();
        return new SparseSet(StringUtils.isBlank(mimeTypes) ? "+/" : mimeTypes).contains(mimeType);
    }

    private static ObjectField getEffectiveRestrictedUploadField(ObjectType type) {
        ToolUi ui = type.as(ToolUi.class);

        if (!ui.isRestrictedUpload()) {
            return null;
        }

        ObjectField field = type.getField(ui.getRestrictedUploadField());

        // Make sure the specified field is valid.
        if (field != null && ObjectField.FILE_TYPE.equals(field.getInternalType())) {
            return field;
        }

        // Check the preview field. If invalid, find the first file field.
        field = type.getField(type.getPreviewField());

        return field == null || field instanceof ObjectMethod || !ObjectField.FILE_TYPE.equals(field.getInternalItemType())
                ? type.getFields().stream()
                        .filter(f -> ObjectField.FILE_TYPE.equals(f.getInternalType()))
                        .findFirst()
                        .orElse(null)
                : field;
    }

    public enum Context {
        FIELD,
        GLOBAL
    }

    @Recordable.Abstract
    private static class SmartUploadableType extends Record {

        private ObjectType type;
        private ObjectField field;

        public SmartUploadableType(ObjectType type, ObjectField field) {
            this.type = type;
            this.field = field;
        }

        public ObjectType getType() {
            return type;
        }

        public void setType(ObjectType type) {
            this.type = type;
        }

        public ObjectField getField() {
            return field;
        }

        public void setField(ObjectField field) {
            this.field = field;
        }
    }
}
