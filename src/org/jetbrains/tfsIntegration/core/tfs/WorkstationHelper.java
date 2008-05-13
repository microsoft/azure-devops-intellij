package org.jetbrains.tfsIntegration.core.tfs;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.exceptions.TfsException;

import java.util.*;

// TODO: rename this class
public class WorkstationHelper {

  private WorkstationHelper() {
  }

  public interface ProcessDelegate<T> {
    T executeRequest(WorkspaceInfo workspace, List<ItemPath> paths) throws TfsException;
  }

  public interface ListProcessDelegate<T> {
    List<T> executeRequest(WorkspaceInfo workspace, List<ItemPath> paths) throws TfsException;
  }


  public interface VoidProcessDelegate {
    void executeRequest(WorkspaceInfo workspace, List<ItemPath> paths) throws TfsException;
  }

  public interface OneToOneProcessDelegate<T> {
    Map<ItemPath, T> executeRequest(WorkspaceInfo workspace, List<ItemPath> paths) throws TfsException;
  }

  public static class ListProcessResult<T> extends ProcessResult<List<T>> {
    public ListProcessResult(final List<T> results, final List<FilePath> orphanPaths) {
      super(results, orphanPaths);
    }
  }

  public static class ProcessResult<T> {
    public final T results;
    public final List<FilePath> orphanPaths;

    public ProcessResult(final T results, final List<FilePath> orphanPaths) {
      this.results = results;
      this.orphanPaths = orphanPaths;
    }
  }

  public static class OneToOneProcessResult<T> {
    public final Map<ItemPath, T> results;
    public final List<FilePath> orphanPaths;

    public OneToOneProcessResult(final Map<ItemPath, T> results, final List<FilePath> orphanPaths) {
      this.results = results;
      this.orphanPaths = orphanPaths;
    }
  }

  private interface WorkspaceProcessor {
    void process(WorkspaceInfo workspace, List<ItemPath> paths) throws TfsException;
  }

  /**
   * @return paths for which workspace was not found (orphan paths)
   */
  // TODO process orphan paths in every caller
  public static List<FilePath> processByWorkspaces(Collection<FilePath> localPaths, final VoidProcessDelegate delegate)
    throws TfsException {
    OneToOneProcessResult<Object> result = processByWorkspaces(localPaths, new OneToOneProcessDelegate<Object>() {
      public Map<ItemPath, Object> executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
        delegate.executeRequest(workspace, paths);
        return Collections.emptyMap();
      }
    });
    return result.orphanPaths;
  }

  public static <T> ListProcessResult<T> processByWorkspaces(Collection<FilePath> localPaths, final ListProcessDelegate<T> delegate)
    throws TfsException {
    final List<T> overallResults = new ArrayList<T>();
    List<FilePath> workspaceNotFoundLocalPaths = processByWorkspaces(localPaths, new WorkspaceProcessor() {
      public void process(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
        overallResults.addAll(delegate.executeRequest(workspace, paths));
      }
    });
    return new ListProcessResult<T>(overallResults, workspaceNotFoundLocalPaths);
  }

  public static <T> ProcessResult<T> processByWorkspaces(Collection<FilePath> localPaths, final ProcessDelegate<T> delegate)
    throws TfsException {
    final Ref<T> results = new Ref<T>();
    List<FilePath> workspaceNotFoundLocalPaths = processByWorkspaces(localPaths, new WorkspaceProcessor() {
      public void process(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
        results.set(delegate.executeRequest(workspace, paths));
      }
    });
    return new ProcessResult<T>(results.get(), workspaceNotFoundLocalPaths);
  }

  public static <T> OneToOneProcessResult<T> processByWorkspaces(Collection<FilePath> localPaths, final OneToOneProcessDelegate<T> delegate)
    throws TfsException {
    final Map<ItemPath, T> overallResults = new HashMap<ItemPath, T>(localPaths.size());
    List<FilePath> orphanPaths = processByWorkspaces(localPaths, new WorkspaceProcessor() {
      public void process(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
        Map<ItemPath, T> serverPath2result = delegate.executeRequest(workspace, paths);
        for (ItemPath itemPath : paths) {
          overallResults.put(itemPath, serverPath2result.get(itemPath));
        }
      }
    });
    return new OneToOneProcessResult<T>(overallResults, orphanPaths);
  }

  /**
   * @param localPaths paths of local items
   * @param processor operation processor
   * @return local paths for which workspace was not found
   * @throws TfsException in case error occurs
   */
  private static List<FilePath> processByWorkspaces(Collection<FilePath> localPaths, WorkspaceProcessor processor) throws TfsException {
    List<FilePath> orphanPaths = new ArrayList<FilePath>();
    Map<WorkspaceInfo, List<FilePath>> workspace2localPaths = new HashMap<WorkspaceInfo, List<FilePath>>();
    for (FilePath localPath : localPaths) {
      WorkspaceInfo workspace = Workstation.getInstance().findWorkspace(localPath);
      if (workspace != null) {
        List<FilePath> workspaceLocalPaths = workspace2localPaths.get(workspace);
        if (workspaceLocalPaths == null) {
          workspaceLocalPaths = new ArrayList<FilePath>();
          workspace2localPaths.put(workspace, workspaceLocalPaths);
        }
        workspaceLocalPaths.add(localPath);
      }
      else {
        orphanPaths.add(localPath);
      }
    }

    for (WorkspaceInfo workspace : workspace2localPaths.keySet()) {
      List<FilePath> currentLocalPaths = workspace2localPaths.get(workspace);
      List<ItemPath> currentItemPaths = new ArrayList<ItemPath>(currentLocalPaths.size());
      for (FilePath localPath : currentLocalPaths) {
        currentItemPaths.add(new ItemPath(localPath, workspace.findServerPathByLocalPath(localPath)));
      }
      processor.process(workspace, currentItemPaths);
    }
    return orphanPaths;
  }

  public static void reportOrpanPaths(final List<FilePath> orphanPaths, final Project project) {
    if (!orphanPaths.isEmpty()) {
      List<VcsException> exceptions = new ArrayList<VcsException>(orphanPaths.size());
      for (FilePath path : orphanPaths) {
        String errorMessage = "Workspace is not defined for path: " + path.getPresentableUrl();
        exceptions.add(new VcsException(errorMessage));
      }
      AbstractVcsHelper.getInstance(project).showErrors(exceptions, TFSVcs.TFS_NAME);
    }
  }

}
