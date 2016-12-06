// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.alm.plugin.context.ServerContext;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.util.List;

public class MergeBranchDialog extends DialogWrapper {
    private final String sourcePath;
    private final boolean sourceIsDirectory;
    private final Project project;
    private final ServerContext serverContext;
    private MergeBranchForm mergeBranchForm;
    private final BranchListProvider branchListProvider;

    public interface BranchListProvider {
        List<String> getBranches(final String source);
    }

    public MergeBranchDialog(Project project,
                             final ServerContext serverContext,
                             final String sourcePath,
                             final boolean sourceIsDirectory,
                             final String title,
                             final BranchListProvider branchListProvider) {
        super(project, true);
        this.project = project;
        this.serverContext = serverContext;
        this.sourcePath = sourcePath;
        this.sourceIsDirectory = sourceIsDirectory;
        this.branchListProvider = branchListProvider;

        setTitle(title);
        setResizable(true);
        init();
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
        mergeBranchForm = new MergeBranchForm(project, serverContext, sourcePath, sourceIsDirectory, getTitle(), branchListProvider);

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
    protected String getDimensionServiceKey() {
        return "TFVC.MergeBranch";
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
        return mergeBranchForm.getPreferredFocusedComponent();
    }
}
