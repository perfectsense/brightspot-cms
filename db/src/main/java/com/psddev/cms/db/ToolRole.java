package com.psddev.cms.db;

import com.psddev.cms.tool.CmsTool;
import com.psddev.cms.tool.Dashboard;
import com.psddev.cms.tool.DashboardContainer;
import com.psddev.cms.tool.ToolEntityTfaRequired;
import com.psddev.dari.db.Application;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.SparseSet;

@ToolUi.IconName("object-toolRole")
@ToolRole.BootstrapPackages({ "Users and Roles", "Application" })
public class ToolRole extends Record implements ToolEntity {

    @Indexed(unique = true)
    @Required
    private String name;

    @ToolUi.FieldDisplayType("permissions")
    private String permissions;

    private transient SparseSet permissionsCache;

    @DisplayName("Dashboard")
    @ToolUi.Tab("Dashboard")
    private DashboardContainer dashboardContainer;

    @Deprecated
    @DisplayName("Legacy Dashboard")
    @ToolUi.Tab("Dashboard")
    @ToolUi.Note("Deprecated. Please use the Dashboard field above instead.")
    @Embedded
    private Dashboard dashboard;

    @ToolUi.DisplayName("Common Content Settings")
    @ToolUi.Tab("Dashboard")
    private CmsTool.CommonContentSettings roleCommonContentSettings;

    @ToolUi.Tab("Advanced")
    @DisplayName("Two-Factor Authentication Required?")
    @ToolUi.Placeholder("Default")
    private ToolEntityTfaRequired tfaRequired;

    /** Returns the name. */
    public String getName() {
        return name;
    }

    /** Sets the name. */
    public void setName(String name) {
        this.name = name;
    }

    /** Returns the permissions. */
    public String getPermissions() {
        return permissions;
    }

    /** Sets the permissions. */
    public void setPermissions(String permissions) {
        this.permissions = permissions;
        this.permissionsCache = null;
    }

    /**
     * Returns {@code true} if this role is allowed access to the
     * resources identified by the given {@code permissionId}.
     */
    public boolean hasPermission(String permissionId) {
        if (permissionsCache == null) {
            permissionsCache = new SparseSet(ObjectUtils.isBlank(permissions) ? "+/" : permissions);
        }
        return permissionsCache.contains(permissionId);
    }

    public DashboardContainer getDashboardContainer() {
        if (dashboardContainer == null && dashboard != null) {
            DashboardContainer.OneOff oneOff = new DashboardContainer.OneOff();
            oneOff.setDashboard(dashboard);
            return oneOff;

        } else {
            return dashboardContainer;
        }
    }

    public void setDashboardContainer(DashboardContainer dashboardContainer) {
        this.dashboardContainer = dashboardContainer;
    }

    @Deprecated
    public Dashboard getDashboard() {
        return dashboard;
    }

    @Deprecated
    public void setDashboard(Dashboard dashboard) {
        this.dashboard = dashboard;
    }

    public CmsTool.CommonContentSettings getRoleCommonContentSettings() {
        return roleCommonContentSettings;
    }

    public void setRoleCommonContentSettings(CmsTool.CommonContentSettings roleCommonContentSettings) {
        this.roleCommonContentSettings = roleCommonContentSettings;
    }

    @Override
    public Iterable<? extends ToolUser> getUsers() {
        return Query.from(ToolUser.class).where("role = ?", this).iterable(0);
    }

    public boolean isTfaRequired() {
        if (tfaRequired == null) {
            return Application.Static.getInstance(CmsTool.class).isTfaRequired();
        } else {
            return ToolEntityTfaRequired.REQUIRED.equals(tfaRequired);
        }
    }
}
