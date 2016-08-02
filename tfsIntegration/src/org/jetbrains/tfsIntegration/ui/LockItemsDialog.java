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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.LockLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.locks.LockItemModel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class LockItemsDialog extends DialogWrapper {

  public static final int LOCK_EXIT_CODE = NEXT_USER_EXIT_CODE;
  public static final int UNLOCK_EXIT_CODE = NEXT_USER_EXIT_CODE + 1;

  private final LockItemsForm myLockItemsForm;

  private final Action myLockAction;
  private final Action myUnlockAction;

  public LockItemsDialog(final Project project, List<LockItemModel> items) {
    super(project, false);

    myLockItemsForm = new LockItemsForm(items);

    setTitle("Lock/Unlock");

    myLockAction = new LockAction();
    myUnlockAction = new UnlockAction();

    init();

    myLockItemsForm.addListener(new LockItemsTableModel.Listener() {
      public void selectionChanged() {
        updateControls();
      }
    });
    updateControls();
  }

  @Nullable
  protected JComponent createCenterPanel() {
    return myLockItemsForm.getContentPane();
  }

  @NotNull
  protected Action[] createActions() {
    return new Action[]{getLockAction(), getUnlockAction(), getCancelAction()};
  }

  private Action getLockAction() {
    return myLockAction;
  }

  private Action getUnlockAction() {
    return myUnlockAction;
  }

  private void setLockActionEnabled(boolean isEnabled) {
    myLockAction.setEnabled(isEnabled);
    myLockItemsForm.setRadioButtonsEnabled(isEnabled);
  }

  private void setUnlockActionEnabled(boolean isEnabled) {
    myUnlockAction.setEnabled(isEnabled);
  }

  private void updateControls() {
    final List<LockItemModel> items = getSelectedItems();
    setLockActionEnabled(!items.isEmpty() && canAllBeLocked(items));
    setUnlockActionEnabled(!items.isEmpty() && canAllBeUnlocked(items));
  }

  private static boolean canAllBeLocked(final List<LockItemModel> items) {
    for (LockItemModel item : items) {
      if (!item.canBeLocked()) {
        return false;
      }
    }
    return true;
  }

  private static boolean canAllBeUnlocked(final List<LockItemModel> items) {
    for (LockItemModel item : items) {
      if (!item.canBeUnlocked()) {
        return false;
      }
    }
    return true;
  }

  public List<LockItemModel> getSelectedItems() {
    return myLockItemsForm.getSelectedItems();
  }

  public LockLevel getLockLevel() {
    return myLockItemsForm.getLockLevel();
  }

  private class LockAction extends AbstractAction {
    public LockAction() {
      super("Lock");
      putValue(DEFAULT_ACTION, Boolean.TRUE);
    }

    public void actionPerformed(ActionEvent e) {
      if (myPerformAction) return;
      try {
        myPerformAction = true;
        if (getLockAction().isEnabled()) {
          close(LOCK_EXIT_CODE);
        }
      }
      finally {
        myPerformAction = false;
      }
    }
  }

  private class UnlockAction extends AbstractAction {
    public UnlockAction() {
      super("Unlock");
    }

    public void actionPerformed(ActionEvent e) {
      if (myPerformAction) return;
      try {
        myPerformAction = true;
        if (getUnlockAction().isEnabled()) {
          close(UNLOCK_EXIT_CODE);
        }
      }
      finally {
        myPerformAction = false;
      }
    }
  }

  @Override
  protected String getDimensionServiceKey() {
    return "TFS.LockItems";
  }

}
