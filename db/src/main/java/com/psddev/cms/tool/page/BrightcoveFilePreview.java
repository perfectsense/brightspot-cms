package com.psddev.cms.tool.page;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.State;
import com.psddev.dari.util.BrightcoveStorageItem;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.StorageItem;

public class BrightcoveFilePreview implements FileContentType {

    @Override
    public boolean isSupported(StorageItem storageItem) {
        return storageItem instanceof BrightcoveStorageItem;
    }

    @Override
    public boolean isPreferred(StorageItem storageItem) {
        return true;
    }

    private static final String BRIGHTCOVE_EXPERIENCES_JS_PATH = "http://admin.brightcove.com/js/BrightcoveExperiences.js";

    @Override
    public void writePreview(ToolPageContext page) throws IOException, ServletException {
        HttpServletRequest request = page.getRequest();
        State state = State.getInstance(request.getAttribute("object"));
        ObjectField field = (ObjectField) request.getAttribute("field");
        String fieldName = field.getInternalName();
        StorageItem fieldValue = (StorageItem) state.getValue(fieldName);
        String playerKey = ((BrightcoveStorageItem) fieldValue).getPreviewPlayerKey();
        String playerId = ((BrightcoveStorageItem) fieldValue).getPreviewPlayerId();

        if (!ObjectUtils.isBlank(playerKey) && !ObjectUtils.isBlank(playerId)) {
            page.writeStart("script", "type", "text/javascript", "src", BRIGHTCOVE_EXPERIENCES_JS_PATH);
            page.writeEnd();

            page.writeStart("script", "type", "text/javascript");
                page.writeRaw(
                        "// Store reference to the player \n" +
                        "var player; \n" +
                        "// Store reference to the modules in the player \n" +
                        "var modVP;\n" +
                        "var modExp;\n" +
                        "var modCon;\n" +
                        "// This method is called when the player loads with the ID of the player\n" +
                        "// We can use that ID to get a reference to the player, and then the modules\n" +
                        "// The name of this method can vary but should match the value you specified\n" +
                        "// in the player publishing code for templateLoadHandler.\n" +
                        "var myTemplateLoaded = function(experienceID) {\n" +
                        "   // Get a reference to the player itself\n" +
                        "   player = brightcove.api.getExperience(experienceID);\n" +
                        "   // Get a reference to individual modules in the player\n" +
                        "   modVP = player.getModule(brightcove.api.modules.APIModules.VIDEO_PLAYER);\n" +
                        "   modExp = player.getModule(brightcove.api.modules.APIModules.EXPERIENCE);\n" +
                        "   modCon = player.getModule(brightcove.api.modules.APIModules.CONTENT);\n" +
                        "   if(modVP.loadVideoByID(<%=((BrightcoveStorageItem)fieldValue).getBrightcoveId()%>) === null) {\n" +
                        "      if(typeof(console) !== 'undefined') { console.log(\"Video with id=<%=((BrightcoveStorageItem)fieldValue).getBrightcoveId()%> could not be found\"); }\n" +
                        "    }\n" +
                        "};");
            page.writeEnd();

            page.writeStart("object", "id", "myExperience", "class", "BrightcoveExperience");
                page.writeTag("param", "name", "bgcolor", "value", "#FFFFFF");
                page.writeTag("param", "name", "width", "value", "480");
                page.writeTag("param", "name", "height", "value", "270");
                page.writeTag("param", "name", "playerId", "value", playerId);
                page.writeTag("param", "name", "playerKey", "value", playerKey);
                page.writeTag("param", "name", "isVid", "value", "true");
                page.writeTag("param", "name", "isUI", "value", "true");
                page.writeTag("param", "name", "dynamicStreaming", "value", "true");
                page.writeTag("param", "name", "includeAPI", "value", "true");
                page.writeTag("param", "name", "templateLoadHandler", "value", "myTemplateLoaded");
            page.writeEnd();

            page.writeStart("script", "type", "text/javascript");
                page.writeRaw("brightcove.createExperiences();");
            page.writeEnd();
        } else {
            page.writeStart("p");
                page.write("No Brightcove player is configured for previewing videos");
            page.writeEnd();
        }
    }

    @Override
    public void setMetadata(ToolPageContext page, State state, StorageItem fieldValue) throws IOException, ServletException {

    }
}
