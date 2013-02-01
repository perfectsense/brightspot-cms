package com.psddev.cms.db;

import com.psddev.dari.db.Record;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.SparseSet;

public class ToolRole extends Record {

    @Indexed(unique = true)
    @Required
    private String name;

    @ToolUi.FieldDisplayType("permissions")
    private String permissions;
    
    @Indexed
    @ToolUi.Hidden
    private boolean defaultRole = false;

    private transient SparseSet permissionsCache;

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

    
    /**
     * Returns {@true} if this role is the default for new user creation. 
     */
    public boolean getDefaultRole() {
        return defaultRole;
    }

    /**
     * Make this the default role (or not) for new user creation.
     */
    public void setDefaultRole(boolean defaultRole) {
        this.defaultRole = defaultRole;
    }
}
