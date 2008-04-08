package org.jetbrains.tfsIntegration.core.tfs;

import com.intellij.openapi.vcs.FilePath;
import org.jetbrains.tfsIntegration.exceptions.TfsException;

import java.util.*;

// TODO: rename this class
public class WorkstationHelper {

  private WorkstationHelper() {
  }

  public interface ProcessDelegate<T> {
    List<T> executeRequest(WorkspaceInfo workspace, List<ItemPath> paths) throws TfsException;
  }

  public interface VoidProcessDelegate {
    void executeRequest(WorkspaceInfo workspace, List<ItemPath> paths) throws TfsException;
  }

  public interface OneToOneProcessDelegate<T> {
    Map<ItemPath, T> executeRequest(WorkspaceInfo workspace, List<ItemPath> paths) throws TfsException;
  }

  public static class ProcessResult<T> {
    public final List<T> results;
    public final List<FilePath> pathsForWhichWorkspaceNotFound;

    public ProcessResult(final List<T> results, final List<FilePath> pathsForWhichWorkspaceNotFound) {
      this.results = results;
      this.pathsForWhichWorkspaceNotFound = pathsForWhichWorkspaceNotFound;
    }
  }

  public static class OneToOneProcessResult<T> {
    public final Map<ItemPath, T> results;
    public final List<FilePath> pathsForWhichWorkspaceNotFound;

    public OneToOneProcessResult(final Map<ItemPath, T> results, final List<FilePath> pathsForWhichWorkspaceNotFound) {
      this.results = results;
      this.pathsForWhichWorkspaceNotFound = pathsForWhichWorkspaceNotFound;
    }
  }

  private interface WorkspaceProcessor {
    void process(WorkspaceInfo workspace, List<ItemPath> paths) throws TfsException;
  }

  /**
   * @return paths for which workspace was not found (orphan paths)
   */
  // TODO process orphan paths in every caller
  public static List<FilePath> processByWorkspaces(List<FilePath> localPaths, final VoidProcessDelegate delegate) throws TfsException {
    OneToOneProcessResult<Object> result = processByWorkspaces(localPaths, new OneToOneProcessDelegate<Object>() {
      public Map<ItemPath, Object> executeRequest(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
        delegate.executeRequest(workspace, paths);
        return Collections.emptyMap();
      }
    });
    return result.pathsForWhichWorkspaceNotFound;
  }

  public static <T> ProcessResult<T> processByWorkspaces(List<FilePath> localPaths, final ProcessDelegate<T> delegate) throws TfsException {
    final List<T> overallResults = new ArrayList<T>();
    List<FilePath> workspaceNotFoundLocalPaths = processByWorkspaces(localPaths, new WorkspaceProcessor() {
      public void process(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
        overallResults.addAll(delegate.executeRequest(workspace, paths));
      }
    });
    return new ProcessResult<T>(overallResults, workspaceNotFoundLocalPaths);
  }

  public static <T> OneToOneProcessResult<T> processByWorkspaces(List<FilePath> localPaths, final OneToOneProcessDelegate<T> delegate)
    throws TfsException {
    final Map<ItemPath, T> overallResults = new HashMap<ItemPath, T>(localPaths.size());
    List<FilePath> workspaceNotFoundLocalPaths = processByWorkspaces(localPaths, new WorkspaceProcessor() {
      public void process(final WorkspaceInfo workspace, final List<ItemPath> paths) throws TfsException {
        Map<ItemPath, T> serverPath2result = delegate.executeRequest(workspace, paths);
        for (ItemPath itemPath : paths) {
          overallResults.put(itemPath, serverPath2result.get(itemPath));
        }
      }
    });
    return new OneToOneProcessResult<T>(overallResults, workspaceNotFoundLocalPaths);
  }

  /**
   * @param localPaths
   * @param processor
   * @return local paths for which workspace was not found
   * @throws TfsException
   */
  private static List<FilePath> processByWorkspaces(List<FilePath> localPaths, WorkspaceProcessor processor) throws TfsException {
    List<FilePath> workspaceNotFoundLocalPaths = new ArrayList<FilePath>();
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
        workspaceNotFoundLocalPaths.add(localPath);
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
    return workspaceNotFoundLocalPaths;
  }

}
