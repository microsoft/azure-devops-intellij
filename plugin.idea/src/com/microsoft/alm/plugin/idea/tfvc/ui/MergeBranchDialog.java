// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.BaseDialogImpl;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MergeBranchDialog extends BaseDialogImpl {
    private static final String PROP_SERVER_CONTEXT = "serverContext";
    private static final String PROP_SOURCE_PATH = "sourcePath";
    private static final String PROP_SOURCE_IS_DIR = "sourceIsDirectory";
    private static final String PROP_BRANCH_LIST_PROVIDER = "branchListProvider";

    private MergeBranchForm mergeBranchForm;

    public interface BranchListProvider {
        List<String> getBranches(final String source);
    }

    public MergeBranchDialog(Project project,
                             final ServerContext serverContext,
                             final String sourcePath,
                             final boolean sourceIsDirectory,
                             final BranchListProvider branchListProvider) {
        super(project, TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MERGE_BRANCH_DIALOG_TITLE),
                TfPluginBundle.message(TfPluginBundle.KEY_TFVC_MERGE_BRANCH_DIALOG_OK_BUTTON),
                TfPluginBundle.KEY_TFVC_MERGE_BRANCH_DIALOG_TITLE,
                true, createProperties(serverContext, sourcePath, sourceIsDirectory, branchListProvider));

        setResizable(true);
    }

    private static Map<String, Object> createProperties(final ServerContext serverContext,
                                                        final String sourcePath,
                                                        final boolean sourceIsDirectory,
                                                        final BranchListProvider branchListProvider) {
        final Map<String, Object> properties = new HashMap<String, Object>(3);
        properties.put(PROP_SERVER_CONTEXT, serverContext);
        properties.put(PROP_SOURCE_PATH, sourcePath);
        properties.put(PROP_SOURCE_IS_DIR, sourceIsDirectory);
        properties.put(PROP_BRANCH_LIST_PROVIDER, branchListProvider);
        return properties;
    }

    public String getSourcePath() {
        return mergeBranchForm.getSourcePath();
    }

    public String getTargetPath() {
        return mergeBranchForm.getTargetPath();

    }

//  @Nullable
//  public VersionSpecBase getFromVersion() {
//    return mergeBranchForm.getFromVersion();
//  }
//
//  @Nullable
//  public VersionSpecBase getToVersion() {
//    return mergeBranchForm.getToVersion();
//  }

    @Nullable
    protected JComponent createCenterPanel() {
        mergeBranchForm = new MergeBranchForm(getProject(), (ServerContext) getProperty(PROP_SERVER_CONTEXT),
                (String) getProperty(PROP_SOURCE_PATH), (Boolean) getProperty(PROP_SOURCE_IS_DIR), getTitle(),
                (BranchListProvider) getProperty(PROP_BRANCH_LIST_PROVIDER));
        mergeBranchForm.addListener(new MergeBranchForm.Listener() {
            public void stateChanged(final boolean canFinish) {
                setOKActionEnabled(canFinish);
            }
        });

        return mergeBranchForm.getContentPanel();
    }

    protected void doOKAction() {
        mergeBranchForm.close();
        super.doOKAction();
    }

    public void doCancelAction() {
        mergeBranchForm.close();
        super.doCancelAction();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return mergeBranchForm.getPreferredFocusedComponent();
    }
}
