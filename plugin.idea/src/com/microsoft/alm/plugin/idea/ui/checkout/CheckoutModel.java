// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.checkout;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.microsoft.alm.plugin.idea.ui.common.PageModelImpl;

/**
 * This class is the overall model for the Checkout dialog UI.
 * It manages the 2 other models for VSO and TFS.
 */
public class CheckoutModel extends PageModelImpl {
    private boolean vsoSelected = true;
    private boolean cloneEnabledForVso = false;
    private boolean cloneEnabledForTfs = false;
    private final CheckoutPageModel vsoModel;
    private final CheckoutPageModel tfsModel;
    private final Project project;
    private final CheckoutProvider.Listener listener;

    public final static String PROP_VSO_SELECTED = "vsoSelected";
    public final static String PROP_CLONE_ENABLED = "cloneEnabled";

    public CheckoutModel(final Project project, final CheckoutProvider.Listener listener) {
        this(project, listener, null, null);
    }

    protected CheckoutModel (final Project project, final CheckoutProvider.Listener listener,
                             final CheckoutPageModel vsoModel, final CheckoutPageModel tfsModel) {
        this.project = project;
        this.listener = listener;
        this.vsoModel = vsoModel == null ? new VsoCheckoutPageModel(this) : vsoModel;
        this.tfsModel = tfsModel == null ? new TfsCheckoutPageModel(this) : tfsModel;
        updateCloneEnabled();
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

    public boolean isCloneEnabled() {
        if(isVsoSelected()) {
            return cloneEnabledForVso;
        } else {
            return cloneEnabledForTfs;
        }
    }

    public void updateCloneEnabled() {
        if(vsoModel != null) {
            setCloneEnabledForVso(vsoModel.isConnected());
        }
        if(tfsModel != null) {
            setCloneEnabledForTfs(tfsModel.isConnected());
        }
    }

    protected void setCloneEnabledForTfs(final boolean value) {
        if(cloneEnabledForTfs != value) {
            cloneEnabledForTfs = value;
            super.setChangedAndNotify(PROP_CLONE_ENABLED);
        }
    }

    protected  void setCloneEnabledForVso(final boolean value) {
        if(cloneEnabledForVso != value) {
            cloneEnabledForVso = value;
            super.setChangedAndNotify(PROP_CLONE_ENABLED);
        }
    }

    public CheckoutPageModel getVsoModel() {
        return this.vsoModel;
    }

    public CheckoutPageModel getTfsModel() {
        return this.tfsModel;
    }

    public Project getProject() {
        return project;
    }

    public CheckoutProvider.Listener getListener() {
        return listener;
    }

    public void dispose(){
        vsoModel.dispose();
        tfsModel.dispose();
    }
}
