package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.*;
import com.intellij.util.Processor;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.Workstation;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;

import java.util.*;

public class TFSChangeProvider implements ChangeProvider {
  private static final Logger LOG = Logger.getInstance(TFSChangeProvider.class.getName());

  private Project myProject;
  private ProgressIndicator myProgress;

  private HashSet<String> filesNew = new HashSet<String>();
  private HashSet<String> filesHijacked = new HashSet<String>();
  private HashSet<String> filesChanged = new HashSet<String>();
  private HashSet<String> filesObsolete = new HashSet<String>();
  private HashSet<String> filesIgnored = new HashSet<String>();

  public TFSChangeProvider(final Project project) {
    myProject = project;
  }

  public void getChanges(final VcsDirtyScope dirtyScope, final ChangelistBuilder builder, final ProgressIndicator progress)
    throws VcsException {
    if (myProject.isDisposed()) {
      return;
    }
    logChangesContent(dirtyScope);
    myProgress = progress;

    initInternals();

    setProgressText("Processing changes");
    
    iterateOverDirtyItems(dirtyScope);

    addAddedFiles(builder);
    addHijackedFiles(builder);
    addObsoleteFiles(builder);
    addChangedFiles(builder);
    addRemovedFiles(builder);
    addIgnoredFiles(builder);
    LOG.info("-- ChangeProvider| New: " +
             filesNew.size() +
             ", modified: " +
             filesChanged.size() +
             ", hijacked:" +
             filesHijacked.size() +
             ", obsolete: " +
             filesObsolete.size() +
             ", ignored: " +
             filesIgnored.size());
  }

  private void iterateOverDirtyItems(final VcsDirtyScope dirtyScope) throws VcsException {
    // collect paths
    final List<String> paths = new ArrayList<String>();
    dirtyScope.iterate(new Processor<FilePath>() {
      public boolean process(final FilePath filePath) {
        paths.add(filePath.getPath());
        return true;
      }
    });
    try {
      List<ExtendedItem> items = getExtendedItems(paths);
      for (int i = 0; i < items.size(); i++) {
        String path = paths.get(i);
        ExtendedItem item = items.get(i);
        // TODO: check logic!
        if (item == null) {
          filesNew.add(path);
        }
        else {
          if (isObsolete(item)) {
            filesObsolete.add(path);
          }
          else if (isChanged(item)) {
            filesChanged.add(path);
          }
          else {
            filesHijacked.add(path);
          }
        }
      }
    }
    catch (Exception e) {
      LOG.error("RemoteException in iterateOverDirtyItems!", e);
      throw new VcsException(e);
    }
  }

  public boolean isModifiedDocumentTrackingRequired() {
    return true;
  }

  private static void logChangesContent(final VcsDirtyScope scope) {
    LOG.info("-- ChangeProvider: Dirty files: " +
             scope.getDirtyFiles().size() +
             ", dirty recursive directories: " +
             scope.getRecursivelyDirtyDirectories().size());
    for (FilePath path : scope.getDirtyFiles()) {
      LOG.info("                                " + path.getPath());
    }
    LOG.info("                                ---");
    for (FilePath path : scope.getRecursivelyDirtyDirectories()) {
      LOG.info("                                " + path.getPath());
    }
  }

  private void initInternals() {
    filesNew.clear();
    filesHijacked.clear();
    filesChanged.clear();
    filesObsolete.clear();
    filesIgnored.clear();
  }

  private void setProgressText(String text) {
    if (myProgress != null) {
      myProgress.setText(text);
    }
  }

  private boolean isChanged(final ExtendedItem item) {
    Set<ChangeType> changeType = changeTypeSetFromString(item.getChg());
    // todo: how to understand that file was changed? 
    return changeType.contains(ChangeType.Edit);
  }

  private boolean isObsolete(final ExtendedItem item) {
    return (item.getDid() > item.getLver() || item.getLatest() > item.getLver()) && !isChanged(item);
  }

  public static Set<ChangeType> changeTypeSetFromString(final String changeTypeString) {
    HashSet<ChangeType> result = new HashSet<ChangeType>();
    if (changeTypeString != null) {
      StringTokenizer tokenizer = new StringTokenizer(changeTypeString, " ");
      while (tokenizer.hasMoreTokens()) {
        String token = tokenizer.nextToken();
        result.add(ChangeType.valueOf(token));
      }
    }
    return result;
  }

  public static String changeTypeSetToString(final Set<ChangeType> changeType) {
    String result = "";
    for (ChangeType type : changeType) {
      result += (type.toString() + " ");
    }
    return result.trim();
  }

  enum ChangeType {
    None(1),
    Add(2),
    Edit(4),
    Encoding(8),
    Rename(16),
    Delete(32),
    Undelete(64),
    Branch(128),
    Merge(256),
    Lock(512);

    public int getIntValue() {
      return intValue;
    }
    private int intValue;

    ChangeType(final int intValue) {
      this.intValue = intValue;
    }
  }

  private ExtendedItem getExtendedItem(String fileName) throws Exception {
    ExtendedItem result = null;
    WorkspaceInfo workspaceInfo = Workstation.getInstance().findWorkspace(fileName);
    if (workspaceInfo != null) {
      result = workspaceInfo.getExtendedItem(workspaceInfo.findServerPathByLocalPath(fileName));
    }
    return result;
  }

  private List<ExtendedItem> getExtendedItems(List<String> fileNames) throws Exception {
    Map<String, WorkspaceInfo> path2workspace = new HashMap<String, WorkspaceInfo>();
    Map<WorkspaceInfo, List<String>> workspace2paths = new HashMap<WorkspaceInfo, List<String>>();
    Map<WorkspaceInfo, List<ExtendedItem>> workspace2items = new HashMap<WorkspaceInfo, List<ExtendedItem>>();
    Map<String, Integer> path2index = new HashMap<String, Integer>();
    // group paths by workspace
    for (String fileName : fileNames) {
      WorkspaceInfo workspaceInfo = Workstation.getInstance().findWorkspace(fileName);
      path2workspace.put(fileName, workspaceInfo);
      if (workspaceInfo != null) {
        List<String> workspacePaths = workspace2paths.get(workspaceInfo);
        if (workspacePaths == null) {
          workspacePaths = new ArrayList<String>();
          workspace2paths.put(workspaceInfo, workspacePaths);
        }
        workspacePaths.add(workspaceInfo.findServerPathByLocalPath(fileName));
        int index = workspacePaths.size() - 1;
        path2index.put(fileName, index);
      }
    }
    // make queries
    for (WorkspaceInfo workspaceInfo : workspace2paths.keySet()) {
      workspace2items.put(workspaceInfo, workspaceInfo.getExtendedItems(workspace2paths.get(workspaceInfo)));
    }
    // merge results
    List<ExtendedItem> result = new LinkedList<ExtendedItem>();
    for (String fileName : fileNames) {
      WorkspaceInfo workspaceInfo = path2workspace.get(fileName);
      if (workspaceInfo == null) {
        result.add(null);
      }
      else {
        result.add(workspace2items.get(workspaceInfo).get(path2index.get(fileName)));
      }
    }
    return result;
  }

  /**
   * File is either:
   * - "new" - it is not contained in the repository, but host contains
   * a record about it (that is, it was manually moved to the
   * list of files to be added to the commit.
   * - "unversioned" - it is not contained in the repository yet.
   * @param builder builder
   */
  private void addAddedFiles(final ChangelistBuilder builder) {
    for (String fileName : filesNew) {

      ////  In the case of file rename or parent folder rename we should
      ////  refer to the list of new files by the
      //String refName = discoverOldName( myVcs, fileName );
      //
      //  New file could be added AFTER and BEFORE the package rename.
      //if( host.containsNew( fileName ) || host.containsNew( refName ))
      //{
      //  FilePath path = VcsUtil.getFilePath( fileName );
      //  builder.processChange( new Change( null, new CurrentContentRevision( path ) ));
      //}
      //else
      //{
        builder.processUnversionedFile( VcsUtil.getVirtualFile( fileName ) );
      //}
    }
  }

  private void addHijackedFiles(final ChangelistBuilder builder) {
    for (String fileName : filesHijacked) {
      final FilePath fp = VcsUtil.getFilePath( fileName );
      final FilePath currfp = VcsUtil.getFilePath( fileName );
      TfsContentRevision revision = new TfsContentRevision(fp);
      builder.processChange( new Change( revision, new CurrentContentRevision( currfp ), FileStatus.HIJACKED ));
    }
  }

  private void addObsoleteFiles(final ChangelistBuilder builder) {
    for (String fileName : filesObsolete) {
      final FilePath fp = VcsUtil.getFilePath( fileName );
      TfsContentRevision revision = new TfsContentRevision(fp);
      builder.processChange( new Change( revision, new CurrentContentRevision( fp ), FileStatus.OBSOLETE ));
    }
  }

  /**
   * Add all files which were determined to be changed (somehow - modified,
   * renamed, etc) and folders which were renamed.
   * NB: adding folders information actually works only in either batch refresh
   * of statuses or when some folder is in the list of changes.
   * @param builder builder
   */
  private void addChangedFiles(final ChangelistBuilder builder) {
    for (String fileName : filesChanged) {
      final FilePath refPath = VcsUtil.getFilePath( fileName );
      final FilePath currPath = VcsUtil.getFilePath( fileName );

      TfsContentRevision revision = new TfsContentRevision(refPath);
      builder.processChange( new Change( revision, new CurrentContentRevision( currPath )));
    }

    //for( String folderName : host.renamedFolders.keySet() )
    //{
    //  String oldFolderName = host.renamedFolders.get( folderName );
    //  final FilePath refPath = VcsUtil.getFilePathForDeletedFile( oldFolderName, true );
    //  final FilePath currPath = VcsUtil.getFilePath( folderName );
    //
    //  builder.processChange( new Change( new VssContentRevision( refPath, project ), new CurrentContentRevision( currPath )));
    //}
  }

  private void addRemovedFiles(final ChangelistBuilder builder) {
    //for( String path : host.removedFolders )
    //  builder.processLocallyDeletedFile( VcsUtil.getFilePathForDeletedFile( path, true ) );
    //
    //for( String path : host.removedFiles )
    //  builder.processLocallyDeletedFile( VcsUtil.getFilePathForDeletedFile( path, false ) );
    //
    //for( String path : host.deletedFolders )
    //  builder.processChange( new Change( new CurrentContentRevision( VcsUtil.getFilePathForDeletedFile( path, true )),
    //                                                                 null, FileStatus.DELETED ));
    //
    //for( String path : host.deletedFiles )
    //{
    //  FilePath refPath = VcsUtil.getFilePathForDeletedFile( path, false ); // todo: implement TfsContentRevision
    //  VssContentRevision revision = ContentRevisionFactory.getRevision( refPath, project );
    //  builder.processChange( new Change( revision, null, FileStatus.DELETED ));
    //}
  }

  private void addIgnoredFiles(final ChangelistBuilder builder) {
    for( String path : filesIgnored )
      builder.processIgnoredFile( VcsUtil.getVirtualFile( path ) );
  }
}
