/*
 * Copyright 2000-2008 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
