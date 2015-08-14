package com.psddev.cms.db;

import com.psddev.dari.db.Modification;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.State;

@BulkUploadDraft.FieldInternalNamePrefix("cms.bulkUpload.")
public class BulkUploadDraft extends Modification<Object> {

    @Indexed(visibility = true)
    @ToolUi.Sortable(false)
    private String containerId;

    private transient boolean runAfterSave;

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public boolean isRunAfterSave() {
        return runAfterSave;
    }

    public void setRunAfterSave(boolean runAfterSave) {
        this.runAfterSave = runAfterSave;
    }

    @Override
    protected void afterSave() {
        if (!isRunAfterSave()
                || !getState().isVisible()) {
            return;
        }

        for (Object item : Query
                .fromAll()
                .where("cms.bulkUpload.containerId = ?", getId().toString())
                .selectAll()) {
            State itemState = State.getInstance(item);

            itemState.as(BulkUploadDraft.class).setContainerId(null);
            itemState.saveImmediately();
        }
    }

    @Override
    protected void afterDelete() {
        State state = getState();

        if (state.isVisible()) {
            return;
        }

        for (Object item : Query
                .fromAll()
                .where("cms.bulkUpload.containerId = ?", state.getId().toString())
                .selectAll()) {
            State.getInstance(item).deleteImmediately();
        }
    }
}
