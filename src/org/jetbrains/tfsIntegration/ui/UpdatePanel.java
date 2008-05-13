package org.jetbrains.tfsIntegration.ui;

import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.core.tfs.AbstractUpdatePanel;
import org.jetbrains.tfsIntegration.core.tfs.TfsPanel;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;

import javax.swing.*;

import com.intellij.openapi.vcs.FilePath;

import java.util.Collection;

public class UpdatePanel extends AbstractUpdatePanel {
    private JPanel myPanel;
    private JCheckBox myRecursiveBox;
    private JPanel myConfigureWorkspacesPanel;

    public UpdatePanel(TFSVcs vcs, Collection<FilePath> roots) {
      super(vcs);
      init(roots);
    }

    protected JPanel getRootsPanel() {
      return myConfigureWorkspacesPanel;
    }

    protected TfsPanel createRootPanel(final WorkspaceInfo workspace, final TFSVcs vcs) {
      return new UpdateOptionsPanel(workspace, vcs);
    }

    protected JComponent getPanel() {
      return myPanel;
    }

    protected JCheckBox getRecursiveBox() {
      return myRecursiveBox;
    }

}
