package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.vcs.CheckinProjectPanel;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.checkin.CheckinEnvironment;
import com.intellij.openapi.vcs.ui.RefreshableOnComponent;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.io.ReadOnlyAttributeUtil;
import com.intellij.vcsUtil.VcsUtil;
import org.apache.axis2.databinding.ADBBean;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Failure;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.GetOperation;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
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
    List<VcsException> exceptions = new ArrayList<VcsException>();
    try {
      WorkstationHelper.ProcessResult<ADBBean> processResult =
        WorkstationHelper.processByWorkspaces(files, new WorkstationHelper.ProcessDelegate<ADBBean>() {
          public List<ADBBean> executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
            return workspace.getServer().getVCS().scheduleForDeletion(workspace.getName(), workspace.getOwnerName(), paths);
          }
        });

      for (ADBBean resultBean : processResult.results) {
        if (resultBean instanceof GetOperation) {
          GetOperation getOp = (GetOperation)resultBean;
          String localPath = getOp.getSlocal(); // TODO determine GetOperation local path
          ReadOnlyAttributeUtil.setReadOnlyAttribute(VcsUtil.getVirtualFile(localPath), false);
        }
        else {
          Failure failure = (Failure)resultBean;
          String errorMessage = MessageFormat.format("Failed to delete {0}: {1}", BeanHelper.getSubjectPath(failure), failure.getMessage());
          exceptions.add(new VcsException(errorMessage));
        }
      }
    }
    catch (TfsException e) {
      exceptions.add(new VcsException(e));
    }
    catch (IOException e) {
      exceptions.add(new VcsException(e));
    }
    return exceptions;
  }

  @Nullable
  public List<VcsException> scheduleUnversionedFilesForAddition(final List<VirtualFile> files) {
    List<VcsException> exceptions = new ArrayList<VcsException>();
    try {
      WorkstationHelper.ProcessResult<ADBBean> processResult =
        WorkstationHelper.processByWorkspaces(TfsFileUtil.getFilePaths(files), new WorkstationHelper.ProcessDelegate<ADBBean>() {
          public List<ADBBean> executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
            return workspace.getServer().getVCS().scheduleForAddition(workspace.getName(), workspace.getOwnerName(), paths);
          }
        });

      for (ADBBean resultBean : processResult.results) {
        if (resultBean instanceof GetOperation) {
          GetOperation getOp = (GetOperation)resultBean;
          String localPath = getOp.getSlocal(); // TODO determine GetOperation local path
          ReadOnlyAttributeUtil.setReadOnlyAttribute(VcsUtil.getVirtualFile(localPath), false);
        }
        else {
          Failure failure = (Failure)resultBean;
          String errorMessage = MessageFormat.format("Failed to add {0}: {1}", BeanHelper.getSubjectPath(failure), failure.getMessage());
          exceptions.add(new VcsException(errorMessage));
        }
      }
    }
    catch (TfsException e) {
      exceptions.add(new VcsException(e));
    }
    catch (IOException e) {
      exceptions.add(new VcsException(e));
    }
    return exceptions;
  }

}
