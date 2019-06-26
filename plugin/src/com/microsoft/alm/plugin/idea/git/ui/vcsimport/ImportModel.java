// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.ui.vcsimport;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.common.ui.common.PageModelImpl;

/**
 * This class is the overall model for the Import dialog UI.
 * It manages the 2 page models for import into VSO and TFS.
 */
public class ImportModel extends PageModelImpl {

    private boolean vsoSelected = true;
    private boolean importEnabledForVso = false;
    private boolean importEnabledForTfs = false;
    private final ImportPageModel vsoImportPageModel;
    private final ImportPageModel tfsImportPageModel;
    private final Project project;

    public final static String PROP_VSO_SELECTED = "vsoSelected";
    public final static String PROP_IMPORT_ENABLED = "importEnabled";

    public ImportModel(final Project project) {
        this(project, null, null, true);
    }

    @VisibleForTesting
    public ImportModel(final Project project,
                          final ImportPageModel vsoImportPageModel,
                          final ImportPageModel tfsImportPageModel,
                          final boolean autoLoad) {
        this.project = project;
        this.vsoImportPageModel = vsoImportPageModel == null ? new VsoImportPageModel(this, autoLoad) : vsoImportPageModel;
        this.tfsImportPageModel = tfsImportPageModel == null ? new TfsImportPageModel(this) : tfsImportPageModel;
        updateImportEnabled();
    }

    public boolean isVsoSelected() {
        return vsoSelected;
    }

    public void setVsoSelected(final boolean value) {
        if (vsoSelected != value) {
            vsoSelected = value;
            super.setChangedAndNotify(PROP_VSO_SELECTED);
        }
    }

    public ImportPageModel getVsoImportPageModel() {
        return this.vsoImportPageModel;
    }

    public ImportPageModel getTfsImportPageModel() {
        return this.tfsImportPageModel;
    }

    public Project getProject() {
        return project;
    }

    public void dispose() {
        vsoImportPageModel.dispose();
        tfsImportPageModel.dispose();
    }

    public boolean isImportEnabled() {
        if (isVsoSelected()) {
            return importEnabledForVso;
        } else {
            return importEnabledForTfs;
        }
    }

    public void updateImportEnabled() {
        if (vsoImportPageModel != null) {
            setImportEnabledForVso(vsoImportPageModel.isConnected());
        }
        if (tfsImportPageModel != null) {
            setImportEnabledForTfs(tfsImportPageModel.isConnected());
        }
    }

    protected void setImportEnabledForVso(final boolean value) {
        if (importEnabledForVso != value) {
            importEnabledForVso = value;
            super.setChangedAndNotify(PROP_IMPORT_ENABLED);
        }
    }

    protected void setImportEnabledForTfs(final boolean value) {
        if (importEnabledForTfs != value) {
            importEnabledForTfs = value;
            super.setChangedAndNotify(PROP_IMPORT_ENABLED);
        }
    }

}
