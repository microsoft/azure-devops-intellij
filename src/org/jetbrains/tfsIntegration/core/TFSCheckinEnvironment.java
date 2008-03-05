package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.vcs.checkin.CheckinEnvironment;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NonNls;

import java.util.List;

public class TFSCheckinEnvironment implements CheckinEnvironment {
  
  @Nullable
  public RefreshableOnComponent createAdditionalOptionsPanel(final CheckinProjectPanel panel) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Nullable
  public String getDefaultMessageFor(final FilePath[] filesToCheckin) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public String prepareCheckinMessage(final String text) {
    return text;
  }

  @Nullable
  @NonNls
  public String getHelpId() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public String getCheckinOperationName() {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  public boolean showCheckinDialogInAnyCase() {
    return false;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Nullable
  public List<VcsException> commit(final List<Change> changes, final String preparedComment) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Nullable
  public List<VcsException> scheduleMissingFileForDeletion(final List<FilePath> files) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Nullable
  public List<VcsException> scheduleUnversionedFilesForAddition(final List<VirtualFile> files) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }
}
