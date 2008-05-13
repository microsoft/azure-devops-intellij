package org.jetbrains.tfsIntegration.core.tfs.operations;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.GetOperation;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ScheduleForAddition {

  public static Collection<VcsException> execute(Project project, WorkspaceInfo workspace, List<ItemPath> paths) {
    try {
      ResultWithFailures<GetOperation> serverResults =
        workspace.getServer().getVCS().scheduleForAddition(workspace.getName(), workspace.getOwnerName(), paths);
      for (GetOperation getOp : serverResults.getResult()) {
        String localPath = getOp.getTlocal(); // TODO determine GetOperation local path
        VirtualFile file = VcsUtil.getVirtualFile(localPath);
        TfsFileUtil.invalidateFile(project, file);
      }
      return BeanHelper.getVcsExceptions(serverResults.getFailures());
    }
    catch (TfsException e) {
      return Collections.singletonList(new VcsException(e));
    }

  }

}
