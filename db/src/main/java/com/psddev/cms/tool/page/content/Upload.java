package com.psddev.cms.tool.page.content;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import com.psddev.dari.db.ObjectMethod;
import com.psddev.dari.util.UuidUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Preconditions;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(Upload.class);

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
        Exception postError = null;
        ObjectType selectedType = environment.getTypeById(page.param(UUID.class, "type"));
        UUID uploadId = UuidUtils.createSequentialUuid();
        String containerId = page.param(String.class, "containerId");

        String fileParamName = "file";

        if (page.isFormPost()) {
            database.beginWrites();

            try {
                MultipartRequest request = MultipartRequestFilter.Static.getInstance(page.getRequest());

                if (request == null) {
                    throw new IllegalStateException("Not multipart!");
                }

                Preconditions.checkNotNull(selectedType, "Param for [type] is empty.");

                ObjectField previewField = Preconditions.checkNotNull(getPreviewField(selectedType), "Preview field for type [" + selectedType.getId() + "] is null.");

                StringBuilder js = new StringBuilder();
                Object common = selectedType.createObject(page.param(UUID.class, "typeForm-" + selectedType.getId()));
                page.updateUsingParameters(common);

                List<StorageItem> newStorageItems = StorageItemFilter.getParameters(page.getRequest(), fileParamName, FileField.getStorageSetting(Optional.of(previewField)));

                List<UUID> newObjectIds = new ArrayList<>();
                if (!ObjectUtils.isBlank(newStorageItems)) {
                    for (StorageItem item : newStorageItems) {
                        if (item == null) {
                            continue;
                        }

                        Object object = selectedType.createObject(null);
                        State state = State.getInstance(object);

                        state.setValues(State.getInstance(common));

                        Site site = page.getSite();

                        if (site != null
                                && site.getDefaultVariation() != null) {
                            state.as(Variation.Data.class).setInitialVariation(site.getDefaultVariation());
                        }

                        state.put(previewField.getInternalName(), item);
                        state.as(BulkUploadDraft.class).setUploadId(uploadId);
                        state.as(BulkUploadDraft.class).setContainerId(containerId);
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

                    database.commitWrites();
                }

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

        Set<ObjectType> typesSet = new HashSet<ObjectType>();

        for (UUID typeId : page.params(UUID.class, "typeId")) {
            ObjectType type = environment.getTypeById(typeId);

            if (type != null) {
                for (ObjectType t : type.as(ToolUi.class).findDisplayTypes()) {
                    for (ObjectField field : t.getFields()) {
                        if (ObjectField.FILE_TYPE.equals(field.getInternalItemType())) {
                            typesSet.add(t);
                            break;
                        }
                    }
                }
            }
        }

        List<ObjectType> types = new ArrayList<ObjectType>(typesSet);
        Collections.sort(types, new ObjectFieldComparator("name", false));

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
                    "value", containerId);

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
                            page.writeStart("option",
                                    "data-hide", ".typeForm",
                                    "data-show", ".typeForm-" + type.getId(),
                                    "selected", type.equals(selectedType) ? "selected" : null,
                                    "value", type.getId());
                                page.writeHtml(type.getDisplayName());
                            page.writeEnd();
                        }
                    page.writeEnd();
                page.writeEnd();
            page.writeEnd();

            for (ObjectType type : types) {
                String name = "typeForm-" + type.getId();
                Object common = type.createObject(null);
                State typeState = State.getInstance(common);

                page.writeStart("div", "class", "typeForm " + name);
                    page.writeElement("input",
                            "type", "hidden",
                            "name", name,
                            "value", typeState.getId());

                    ObjectField previewField = getPreviewField(type);

                    page.writeStart("div",
                            "class", "toggleable-form",
                            "data-form-fields-data", ObjectUtils.toJson(typeState.getSimpleValues()),
                            "data-form-fields-url", page.cmsUrl(
                                    "/contentFormFields",
                                    "typeId", typeState.getTypeId(),
                                    "id", typeState.getId(),
                                    "excluded", previewField != null ? Collections.singletonList(previewField.getInternalName()) : null));
                    page.writeEnd();
                page.writeEnd();
            }

            page.writeStart("input", "type", "hidden", "name", "context", "value", page.param(Context.class, "context"));

            page.writeStart("div", "class", "buttons");
                page.writeStart("button", "name", "action-upload");
                    page.writeHtml(page.localize(Upload.class, "action.upload"));
                page.writeEnd();
            page.writeEnd();

        page.writeEnd();
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

    private static ObjectField getPreviewField(ObjectType type) {
        ObjectField previewField = type.getField(type.getPreviewField());

        if (previewField instanceof ObjectMethod) {
            previewField = null;
        }

        if (previewField == null) {
            for (ObjectField field : type.getFields()) {
                if (ObjectField.FILE_TYPE.equals(field.getInternalItemType())) {
                    previewField = field;
                    break;
                }
            }
        }

        return previewField;
    }

    public enum Context {
        FIELD,
        GLOBAL
    }
}
