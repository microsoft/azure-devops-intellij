package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.*;
import com.intellij.openapi.vcs.changes.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Processor;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.Workstation;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ExtendedItem;

import java.io.File;
import java.rmi.RemoteException;
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

    iterateOverRecursiveFolders(dirtyScope);
    iterateOverDirtyDirectories(dirtyScope);
    iterateOverDirtyFiles(dirtyScope);
    processStatusExceptions();

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

  /**
   * Iterate over the project structure, find all writable files in the project,
   * and check their status against the TFS repository. If file exists in the repository
   * it is assigned "changed" status, otherwise it has "new" status.
   * @param dirtyScope dirtyScope
   */
  private void iterateOverRecursiveFolders(final VcsDirtyScope dirtyScope) {
    for (FilePath path : dirtyScope.getRecursivelyDirtyDirectories()) {
      iterateOverProjectPath(path);
    }
  }

  private void iterateOverProjectPath(FilePath path) {
    LOG.info("-- ChangeProvider - Iterating over project structure starting from scope root: " + path.getPath());
    setProgressText("Collecting writable files");

    List<String> writableFiles = new ArrayList<String>();
    collectSuspiciousFiles(path, writableFiles);

    LOG.info("-- ChangeProvider - Found: " + writableFiles.size() + " writable files.");
    setProgressText("Searching for new files");
    analyzeWritableFiles(writableFiles);
  }

  private void setProgressText(String text) {
    if (myProgress != null) {
      myProgress.setText(text);
    }
  }

  private void collectSuspiciousFiles(final FilePath filePath, final List<String> writableFiles) {
    VirtualFile vf = filePath.getVirtualFile();
    if (vf != null) {
      ProjectLevelVcsManager.getInstance(myProject).iterateVcsRoot(vf, new Processor<FilePath>() {
        public boolean process(final FilePath file) {
          String path = file.getPath();
          VirtualFile vFile = file.getVirtualFile();
          if (vFile != null) {
            if (vFile.isWritable() && !vFile.isDirectory()) {
              writableFiles.add(path);
            }
          }
          return true;
        }
      });
    }
  }

  private void analyzeWritableFiles(List<String> writableFiles) {
    final HashSet<String> newFiles = new HashSet<String>();
    if (writableFiles.isEmpty()) {
      return;
    }
    analyzeWritableFilesByStatus(writableFiles, newFiles, filesChanged, filesHijacked, filesObsolete);
    filesNew.addAll(newFiles);
  }

  private void analyzeWritableFilesByStatus(List<String> files,
                                            HashSet<String> newFiles,
                                            HashSet<String> changedFiles,
                                            HashSet<String> hijackedFiles,
                                            HashSet<String> obsoleteFiles) {
    try {
      for (String filePath : files) {
        ExtendedItem result = getExtendedItem(filePath);
        // todo: check logic!
        if (result != null) {
          if (isObsolete(result)) {
            obsoleteFiles.add(filePath);
          }
          else if (isChanged(result)) {
            changedFiles.add(filePath);
          }
          else {
            hijackedFiles.add(filePath);
          }
        }
        else {
          newFiles.add(filePath);
        }
      }
    }
    catch (RemoteException e) {
      LOG.error("RemoteException in analyzeWritableFilesByStatus!", e);
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


  /**
   * Deleted and New folders are marked as dirty too and we provide here
   * special processing for them.
   * @param dirtyScope dirtyScope
   */
  private void iterateOverDirtyDirectories(final VcsDirtyScope dirtyScope) {
    for (FilePath path : dirtyScope.getDirtyFiles()) {
      String fileName = path.getPath();
      VirtualFile file = path.getVirtualFile();

      //  make sure that:
      //  - a file is a folder which exists physically
      //  - it is under out vcs
      if (path.isDirectory() && (file != null) /* && check mapping */) {
        //String refName = discoverOldName(fileName);
        //if (!isFolderExists(refName)) {
        //  filesNew.add(fileName);
        //}
        //else {
          ////  NB: Do not put to the "Changed" list those folders which are under
          ////      the renamed one since we will have troubles in checking such
          ////      folders in (it is useless, BTW).
          ////      Simultaneously, this prevents valid processing of renamed folders
          ////      that are under another renamed folders.
          ////  Todo Inner rename.
          //if (!refName.equals(fileName) && !isUnderRenamedFolder(fileName)) {
            filesChanged.add(fileName);
          //}
        //}
      }
    }
  }

  private boolean isFolderExists( String fileName ) throws RemoteException {
    return getExtendedItem(fileName) != null;
  }

  private ExtendedItem getExtendedItem(String fileName) throws RemoteException {
    ExtendedItem result = null;
    WorkspaceInfo workspaceInfo = Workstation.getInstance().findWorkspace(fileName);
    if (workspaceInfo != null) {
      result = workspaceInfo.getExtendedItem(workspaceInfo.findServerPathByLocalPath(fileName));
    }
    return result;
  }

  private void iterateOverDirtyFiles(final VcsDirtyScope scope) {
    List<String> paths = new ArrayList<String>();
    for (FilePath path : scope.getDirtyFiles()) {
      VirtualFile file = path.getVirtualFile();
      String fileName = null; // VssUtil.getCanonicalLocalPath(path.getPath()); // todo: write findLocalPathByServerPath in WorkspaceInfo

      if (isFileTfsProcessable(file) && isProperNotification(path)) {
          if (isParentFolderNewOrUnversioned(file)) {
            filesNew.add(fileName);
          }
          else {
            paths.add(path.getPath());
          }
      }
    }
    analyzeWritableFilesByStatus(paths, filesNew, filesChanged, filesHijacked, filesObsolete);
  }

  /**
   * For the renamed or moved file we receive two change requests: one for
   * the old file and one for the new one. For renamed file old request differs
   * in filename, for the moved one - in parent path name. This request must be
   * ignored since all preliminary information is already accumulated.
   * @param filePath filePath
   * @return result
   */
  private static boolean isProperNotification( final FilePath filePath )
  {
    String oldName = filePath.getName();
    String newName = (filePath.getVirtualFile() == null) ? "" : filePath.getVirtualFile().getName();
    String oldParent = (filePath.getVirtualFileParent() == null) ? "" : filePath.getVirtualFileParent().getPath();
    String newParent = filePath.getPath().substring( 0, filePath.getPath().length() - oldName.length() - 1 );
    newParent = ""; // VssUtil.getCanonicalLocalPath( newParent ); // todo: write findLocalPathByServerPath in WorkspaceInfo

    //  Check the case when the file is deleted - its FilePath's VirtualFile
    //  component is null and thus new name is empty.
    return newParent.equals( oldParent ) &&
          ( newName.equals( oldName ) || (newName.equals("") && !oldName.equals("")) );
  }

  /**
     * Return true if:
     * - file is not null & writable
     * - file is not a folder
     * - files is under the project
   * @param file file
   * @return true if file under TFS
   */
    private boolean isFileTfsProcessable( VirtualFile file ) {
      return (file != null) && file.isWritable() && !file.isDirectory();
             // && VcsUtil.isPathUnderProject( project, file.getPath() ); // todo: file under some workspace
    }

  /**
   * Process exceptions of different kind when normal computation of file
   * statuses is cheated by the IDEA:
   * 1. "Extract Superclass" refactoring with "Rename original class" option set.
   * Refactoring renamed the original class (right) but writes new content to
   * the file with the olf name (fuck!).
   * Remedy: Find such file in the list of "Changed" files, check whether its
   * name is in the list of New files (from VFSListener), and check
   * whether its name is in the record for renamed files, then move
   * it into "New" files list.
   */
  private void processStatusExceptions() {
    // todo: implement later
    // 1.
    //for (Iterator<String> it = filesChanged.iterator(); it.hasNext();) {
    //  String fileName = it.next();
    //  if (host.isNewOverRenamed(fileName)) {
    //    it.remove();
    //    filesNew.add(fileName);
    //  }
    //}
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
      TfsContentRevision revision = new TfsContentRevision(fp, myProject ); 
      builder.processChange( new Change( revision, new CurrentContentRevision( currfp ), FileStatus.HIJACKED ));
    }
  }

  private void addObsoleteFiles(final ChangelistBuilder builder) {
    for (String fileName : filesObsolete) {
      final FilePath fp = VcsUtil.getFilePath( fileName );
      TfsContentRevision revision = new TfsContentRevision(fp, myProject );
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

      TfsContentRevision revision = new TfsContentRevision(refPath, myProject );
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

  private boolean isParentFolderNewOrUnversioned( VirtualFile file )
  {
    FileStatus status = FileStatus.NOT_CHANGED;
    VirtualFile parent = file.getParent();
    if( parent != null )
    {
      status = FileStatusManager.getInstance( myProject ).getStatus( parent );
    }
    return (status == FileStatus.ADDED) || (status == FileStatus.UNKNOWN);
  }

  private static boolean isPathUnderProcessedFolders( HashSet<String> folders, String path )
  {
    String parentPathToCheck = new File( path ).getParent();
    for( String folderPath : folders )
    {
      if( FileUtil.pathsEqual( parentPathToCheck, folderPath ))
        return true;
    }
    return false;
  }

}
