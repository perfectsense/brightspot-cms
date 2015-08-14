package com.psddev.cms.tool;

import com.psddev.dari.util.Settings;
import com.psddev.dari.util.SettingsBackedObject;

public interface ToolPermissionProvider extends SettingsBackedObject {

    /** Setting key for default permission provider . */
    String DEFAULT_SETTING_KEY = "cms/tool/defaultPermissionProvider";

    /** Setting key for permission provider configuration. */
    String SETTING_KEY = "cms/tool/permissionProvider";

    /** Get the configured permission provider. */
    static ToolPermissionProvider getDefault() {
        String defaultPermissionProvider = Settings.get(String.class, DEFAULT_SETTING_KEY);
        return defaultPermissionProvider == null ? null
                : Settings.newInstance(ToolPermissionProvider.class, SETTING_KEY + '/' + defaultPermissionProvider);
    }

    /** Called by ToolPageContext#hasPermission, so do NOT execute that method within this method. */
    boolean hasPermission(ToolPageContext page, String permissionId);

}
