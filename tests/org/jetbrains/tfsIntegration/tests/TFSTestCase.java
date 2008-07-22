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

package org.jetbrains.tfsIntegration.tests;

import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vcs.*;
import com.intellij.openapi.vcs.update.UpdatedFiles;
import com.intellij.openapi.vcs.update.UpdateSession;
import com.intellij.openapi.vcs.versionBrowser.ChangeBrowserSettings;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.testFramework.AbstractVcsTestCase;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.core.TFSChangeList;
import org.jetbrains.tfsIntegration.core.TFSProjectConfiguration;
import org.jetbrains.tfsIntegration.core.credentials.Credentials;
import org.jetbrains.tfsIntegration.core.credentials.CredentialsManager;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.core.tfs.operations.ApplyGetOperations;
import org.jetbrains.tfsIntegration.core.tfs.version.ChangesetVersionSpec;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.webservice.WebServiceHelper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

@SuppressWarnings({"ConstantConditions", "HardCodedStringLiteral"})
public abstract class TFSTestCase extends AbstractVcsTestCase {

  protected interface RunnableWithExceptions {
    void run() throws Exception;
  }

  private static final String SERVER = "http://wmw-2003-01:8080/";
//  private static final String SERVER = "http://192.168.117.128:8080/";
  private static final String SERVER_ROOT = "$/Test";
  private static final String USER = "TFSSETUP";
  private static final String DOMAIN = "SWIFTTEAMS";
  private static final String PASSWORD = "";

  private static final String WORKSPACE_NAME_PREFIX = "__testWorkspace_";
  private static final String SANDBOX_PREFIX = "sandbox_";

  private TempDirTestFixture myTempDirFixture;
  protected WorkspaceInfo myTestWorkspace;
  protected VirtualFile mySandboxRoot;
  private Credentials myOriginalServerCredentials;

  @Before
  public void setUp() throws Exception {
    final IdeaTestFixtureFactory fixtureFactory = IdeaTestFixtureFactory.getFixtureFactory();
    myTempDirFixture = fixtureFactory.createTempDirTestFixture();

    //File pluginRoot = new File(PathManager.getHomePath(), "tfsIntegration");
    //if (!pluginRoot.isDirectory()) {
    // try standalone mode
    //Class aClass = TFSTestCase.class;
    //String rootPath = PathManager.getResourceRoot(aClass, "/" + aClass.getName().replace('.', '/') + ".class");
    //pluginRoot = new File(rootPath).getParentFile().getParentFile().getParentFile();
    //}
    File localRoot = new File(myTempDirFixture.getTempDirPath());
    localRoot.mkdir();


    initProject(localRoot);
    mySandboxRoot = createDirInCommand(myWorkingCopyDir, SANDBOX_PREFIX + Workstation.getComputerName() + "_" + localRoot.getName(), true);

    prepareServer();

    activateVCS(TFSVcs.TFS_NAME);

    createNewWorkspaceFor(localRoot);

    createServerFolder(TfsFileUtil.getFilePath(mySandboxRoot));

    ApplyGetOperations.setLocalConflictHandlingType(ApplyGetOperations.LocalConflictHandlingType.REPORT_LOCAL_CONFLICT);
  }

  private void prepareServer() throws URISyntaxException, TfsException {
    final URI serverUri = new URI(SERVER);
    ServerInfo server = null;
    for (ServerInfo s : Workstation.getInstance().getServers()) {
      if (s.getUri().equals(serverUri)) {
        server = s;
        break;
      }
    }
    final Credentials testCredentials = new Credentials(USER, DOMAIN, PASSWORD, false);
    if (server == null) {
      String serverGuid = WebServiceHelper.authenticate(serverUri, testCredentials);
      ServerInfo newServer = new ServerInfo(serverUri, serverGuid);
      Workstation.getInstance().addServer(newServer);
      myOriginalServerCredentials = null;
    }
    else {
      myOriginalServerCredentials = CredentialsManager.getInstance().getCredentials(serverUri);
    }
    CredentialsManager.getInstance().storeCredentials(serverUri, testCredentials);
  }

  @After
  public void tearDown() throws Exception {
    try {
      removeWorkspace(myTestWorkspace);
    }
    finally {
      if (myOriginalServerCredentials != null) {
        CredentialsManager.getInstance().storeCredentials(new URI(SERVER), myOriginalServerCredentials);
      }
    }

    tearDownProject();
    if (myTempDirFixture != null) {
      myTempDirFixture.tearDown();
      myTempDirFixture = null;
    }
  }

  protected TFSVcs getVcs() {
    return TFSVcs.getInstance(myProject);
  }

  protected void doActionSilently(final VcsConfiguration.StandardConfirmation op) {
    setStandardConfirmation(TFSVcs.TFS_NAME, op, VcsShowConfirmationOption.Value.DO_ACTION_SILENTLY);
  }

  protected void doNothingSilently(final VcsConfiguration.StandardConfirmation op) {
    setStandardConfirmation(TFSVcs.TFS_NAME, op, VcsShowConfirmationOption.Value.DO_NOTHING_SILENTLY);
  }

  private void createNewWorkspaceFor(File root) throws URISyntaxException, TfsException {
    final String workspaceName = WORKSPACE_NAME_PREFIX + Workstation.getComputerName();
    final ServerInfo server = Workstation.getInstance().getServer(new URI(SERVER));
    for (WorkspaceInfo workspace : server.getWorkspacesForCurrentOwner()) {
      if (workspace.getName().equals(workspaceName)) {
        removeWorkspace(workspace);
        break;
      }
    }

    myTestWorkspace = new WorkspaceInfo(server, DOMAIN + "\\" + USER, Workstation.getComputerName());
    myTestWorkspace.setName(workspaceName);
    myTestWorkspace.addWorkingFolderInfo(new WorkingFolderInfo(WorkingFolderInfo.Status.Active, VcsUtil.getFilePath(root), SERVER_ROOT));
    myTestWorkspace.saveToServer();
  }

  private static void removeWorkspace(WorkspaceInfo workspace) throws URISyntaxException, TfsException {
    final ServerInfo server = Workstation.getInstance().getServer(new URI(SERVER));
    server.deleteWorkspace(workspace);
  }

  //protected TestChangeListBuilder getChanges() throws VcsException {
  //  TestChangeListBuilder changeListBuilder = new TestChangeListBuilder();
  //  getVcs().getChangeProvider().getChanges(getAllDirtyScope(), changeListBuilder, new EmptyProgressIndicator());
  //  return changeListBuilder;
  //}

  protected TestChangeListBuilder getChanges() throws VcsException {
    return getChanges(mySandboxRoot);
  }

  private TestChangeListBuilder getChanges(VirtualFile root) throws VcsException {
    ChangeListManager.getInstance(myProject).ensureUpToDate(false);
    TestChangeListBuilder changeListBuilder = new TestChangeListBuilder(mySandboxRoot, myProject);
    getVcs().getChangeProvider().getChanges(getDirtyScopeForFile(root), changeListBuilder, new EmptyProgressIndicator());
    return changeListBuilder;
  }

  protected VirtualFile createFolderInSandbox(String name) throws TfsException {
    return createLocalAndServerFolder(mySandboxRoot, name);
  }

  private VirtualFile createLocalAndServerFolder(VirtualFile parent, String name) throws TfsException {
    FilePath filePath = VcsUtil.getFilePath(new File(new File(parent.getPath()), name));
    createServerFolder(filePath);
    doNothingSilently(VcsConfiguration.StandardConfirmation.ADD);
    return createDirInCommand(parent, name);
  }

  private void createServerFolder(FilePath path) throws TfsException {
    ItemPath itemPath = new ItemPath(path, myTestWorkspace.findServerPathByLocalPath(path));
    myTestWorkspace.getServer().getVCS()
      .scheduleForAddition(myTestWorkspace.getName(), myTestWorkspace.getOwnerName(), Collections.singletonList(itemPath));
    myTestWorkspace.getServer().getVCS().checkIn(myTestWorkspace.getName(), myTestWorkspace.getOwnerName(),
                                                 Collections.singletonList(itemPath.getServerPath()), path.getPath() + "  created");
  }

  //private void deleteServerFolder(FilePath path) throws TfsException {
  //  ItemPath itemPath = new ItemPath(path, myTestWorkspace.findServerPathByLocalPath(path));
  //  myTestWorkspace.getServer().getVCS()
  //    .scheduleForDeletion(myTestWorkspace.getName(), myTestWorkspace.getOwnerName(), Collections.singletonList(itemPath));
  //  myTestWorkspace.getServer().getVCS().checkIn(myTestWorkspace.getName(), myTestWorkspace.getOwnerName(),
  //                                               Collections.singletonList(itemPath.getSelectedPath()), path.getPath() + "  deleted");
  //}

  protected void commit(final List<Change> changes, final String comment) {
    final List<VcsException> errors = getVcs().getCheckinEnvironment().commit(changes, comment);
    Assert.assertTrue(getMessage(errors), errors.isEmpty());
    refreshAll();
  }

  protected void commit(Change change, final String comment) {
    commit(Collections.singletonList(change), comment);
  }

  protected void rollback(final Change change) {
    rollback(Collections.singletonList(change));
  }

  protected void rollback(final List<Change> changes) {
    final List<VcsException> errors = getVcs().getRollbackEnvironment().rollbackChanges(changes);
    Assert.assertTrue(getMessage(errors), errors.isEmpty());
    refreshAll();
  }

  protected void rollbackAll(TestChangeListBuilder builder) {
    final List<VcsException> errors = new ArrayList<VcsException>();
    errors.addAll(getVcs().getRollbackEnvironment().rollbackMissingFileDeletion(builder.getLocallyDeleted()));
    errors.addAll(getVcs().getRollbackEnvironment().rollbackChanges(builder.getChanges()));
    // ??? errors.addAll(getVcs().getRollbackEnvironment().rollbackIfUnchanged());
    errors.addAll(getVcs().getRollbackEnvironment().rollbackModifiedWithoutCheckout(builder.getHijackedFiles()));
    Assert.assertTrue(getMessage(errors), errors.isEmpty());
    refreshAll();
  }

  protected void scheduleForAddition(VirtualFile... files) {
    final List<VcsException> errors = getVcs().getCheckinEnvironment().scheduleUnversionedFilesForAddition(Arrays.asList(files));
    Assert.assertTrue(getMessage(errors), errors.isEmpty());
    refreshAll();
  }

  protected void scheduleForAddition(FilePath... files) {
    List<VirtualFile> vfiles = new ArrayList<VirtualFile>(files.length);
    for (FilePath f : files) {
      vfiles.add(VcsUtil.getVirtualFile(f.getIOFile()));
    }
    scheduleForAddition(vfiles.toArray(new VirtualFile[files.length]));
  }

  protected void scheduleForDeletion(FilePath... files) {
    final List<VcsException> errors = getVcs().getCheckinEnvironment().scheduleMissingFileForDeletion(Arrays.asList(files));
    Assert.assertTrue(getMessage(errors), errors.isEmpty());
    refreshAll();
  }

  protected void scheduleForDeletion(final VirtualFile file) {
    scheduleForDeletion(TfsFileUtil.getFilePath(file));
  }

  protected void rollbackHijacked(VirtualFile... files) {
    rollbackHijacked(Arrays.asList(files));
  }

  protected void rollbackHijacked(List<VirtualFile> files) {
    final List<VcsException> errors = getVcs().getRollbackEnvironment().rollbackModifiedWithoutCheckout(files);
    Assert.assertTrue(getMessage(errors), errors.isEmpty());
    refreshAll();
  }

  protected void refreshAll() {
    TfsFileUtil.refreshAndInvalidate(myProject, Collections.singletonList(mySandboxRoot), false);
    ChangeListManager.getInstance(myProject).ensureUpToDate(false);
  }

  //protected VirtualFile createAndCommitFile(String name, String content) throws VcsException {
  //  Assert.assertEquals(0, getChanges(mySandboxRoot).assertTotalItems());
  //
  //  doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
  //  VirtualFile file = createFileInCommand(mySandboxRoot, name, content);
  //  commit(getChanges(mySandboxRoot).getChanges(), "");
  //  Assert.assertEquals(0, getChanges(mySandboxRoot).assertTotalItems());
  //  return file;
  //}

  protected void editFiles(VirtualFile... files) throws VcsException {
    // TODO: use ReadonlyStatusHandler.getInstance(myProject).ensureFilesWritable(file);
    getVcs().getEditFileProvider().editFiles(files);
    refreshAll();
  }

  protected void editFiles(FilePath... files) throws VcsException {
    VirtualFile[] vf = new VirtualFile[files.length];
    for (int i = 0; i < files.length; i++) {
      vf[i] = VcsUtil.getVirtualFile(files[i].getIOFile());
    }
    editFiles(vf);
  }

  protected static String getContent(VirtualFile file) {
    return new CurrentContentRevision(TfsFileUtil.getFilePath(file)).getContent();
  }

  protected static String getContent(FilePath file) {
    return new CurrentContentRevision(file).getContent();
  }

  protected void deleteFileExternally(FilePath file) {
    deleteFileExternally(file.getVirtualFile());
  }

  protected void deleteFileExternally(VirtualFile file) {
    final File ioFile = new File(file.getPath());
    Assert.assertTrue(FileUtil.delete(ioFile));
    refreshAll();
  }

  protected void clearReadonlyAndRenameExternally(final VirtualFile file, final String newName) throws IOException {
    FileUtil.setReadOnlyAttribute(file.getPath(), false);
    final File from = new File(file.getPath());
    final File to = new File(from.getParent(), newName);
    Assert.assertTrue(from.renameTo(to));
    refreshAll();
  }


  protected static VirtualFile getVirtualFile(final VirtualFile parent, final String newName) {
    return VcsUtil.getVirtualFile(new File(new File(parent.getPath()), newName));
  }

  protected void clearReadonlyStatusExternally(VirtualFile... files) throws IOException {
    for (VirtualFile file : files) {
      FileUtil.setReadOnlyAttribute(file.getPath(), false);
    }
    refreshAll();
  }

  protected void clearReadonlyStatusExternally(FilePath... files) throws IOException {
    for (FilePath file : files) {
      FileUtil.setReadOnlyAttribute(file.getPath(), false);
//VcsUtil.refreshFiles(new File[]{new File(file.getPath())}, new Runnable() {
//  public void run() {
//  }
//});
      refreshAll();
    }
  }

  protected void rename(final FilePath file, final String newName) throws VcsException {
    rename(VcsUtil.getVirtualFile(file.getIOFile()), newName);
  }

  protected void rename(final VirtualFile file, final String newName) throws VcsException {
    renameFileInCommand(file, newName);
    refreshAll();
  }

  /*public void refreshRecursively(final VirtualFile parent) {
    TfsFileUtil.refreshRecursively(parent);
    ChangeListManager.getInstance(myProject).ensureUpToDate(false);
  }*/

  protected VirtualFile createFileInCommand(final VirtualFile parent, final String name, @Nullable final String content) {
    final VirtualFile result = super.createFileInCommand(parent, name, content);
    assertFile(result, content, true);
    refreshAll();
    return result;
  }

  protected VirtualFile createFileInCommand(final FilePath parent, final String name, @Nullable final String content) {
    return createFileInCommand(parent.getVirtualFile(), name, content);
  }

  private VirtualFile createDirInCommand(final VirtualFile parent, final String name, boolean supressRefresh) {
    final VirtualFile result = super.createDirInCommand(parent, name);
    if (!supressRefresh) {
      refreshAll();
    }
    assertFolder(VcsUtil.getFilePath(new File(new File(parent.getPath()), name)), 0);
    return result;
  }

  protected VirtualFile createDirInCommand(final VirtualFile parent, final String name) {
    return createDirInCommand(parent, name, false);
  }

  protected VirtualFile createDirInCommand(final FilePath parent, final String name) {
    return createDirInCommand(parent.getVirtualFile(), name);
  }

  protected void renameFileInCommand(final VirtualFile file, final String newName) {
    super.renameFileInCommand(file, newName);
    refreshAll();
  }

  protected void renameFileInCommand(final FilePath file, final String newName) {
    renameFileInCommand(VcsUtil.getVirtualFile(file.getIOFile()), newName);
  }

  protected void moveFileInCommand(final VirtualFile file, final VirtualFile newParent) {
    super.moveFileInCommand(file, newParent);
    refreshAll();
  }

  protected void moveFileInCommand(final FilePath file, final VirtualFile newParent) {
    moveFileInCommand(VcsUtil.getVirtualFile(file.getIOFile()), newParent);
  }

  protected void deleteFileInCommand(final VirtualFile file) {
    super.deleteFileInCommand(file);
    refreshAll();
  }

  protected void deleteFileInCommand(final FilePath file) {
    deleteFileInCommand(file.getVirtualFile());
  }

  protected void commit() throws VcsException {
    commit(getChanges().getChanges(), "unittest");
  }

  protected void assertFileStatus(VirtualFile file, FileStatus expectedStatus) {
    Assert.assertTrue(FileStatusManager.getInstance(myProject).getStatus(file) == expectedStatus);
  }


  protected static String getMessage(Collection<VcsException> errors) {
    String result = "";
    for (VcsException error : errors) {
      result += error + "\n";
    }
    return result;
  }

  protected String getUsername() {
    return DOMAIN + "\\" + USER;
  }

  protected static FilePath replaceInPath(FilePath path, FilePath folderFrom, FilePath folderTo) {
    String from = folderFrom.getIOFile().getPath();
    String to = folderTo.getIOFile().getPath();

    String result = path.getIOFile().getPath().replace(from, to);
    return VcsUtil.getFilePath(result);
  }

  protected static void assertFolder(VirtualFile file, int itemsInside) {
    Assert.assertTrue(new File(file.getPath()).isDirectory());
    Assert.assertEquals(itemsInside, new File(file.getPath()).list().length);
  }

  protected static void assertFolder(FilePath file, int itemsInside) {
    Assert.assertTrue(file.getIOFile().isDirectory());
    Assert.assertTrue(file.getIOFile().canWrite());
    Assert.assertEquals(itemsInside, file.getIOFile().list().length);
    Assert.assertEquals(itemsInside, file.getIOFile().list().length);
  }

  protected static void assertFile(FilePath file, String content, boolean writable) {
    assertFile(file.getIOFile(), content, writable);
  }

  protected static void assertNotExists(FilePath file) {
    Assert.assertFalse(file.getIOFile().exists());
  }

  protected static void assertFile(VirtualFile file, String content, boolean writable) {
    assertFile(new File(file.getPath()), content, writable);
  }

  protected static void assertFile(File file, String content, boolean writable) {
    Assert.assertTrue(file.isFile());
    if (writable) {
      Assert.assertTrue("File " + file + " expected to be writable", file.canWrite());
    }
    else {
      Assert.assertFalse("File " + file + " expected to be read only", file.canWrite());
    }
    InputStream stream = null;
    try {
      stream = new FileInputStream(file);
      Assert.assertEquals("File content differs", content, StreamUtil.readText(stream));
    }
    catch (IOException e) {
      Assert.fail(e.getMessage());
    }
    finally {
      if (stream != null) {
        try {
          stream.close();
        }
        catch (IOException e) {
          // ingore
        }
      }
    }
  }

  protected int getLatestRevisionNumber(VirtualFile file) throws VcsException {
    final RepositoryLocation location = getVcs().getCommittedChangesProvider().getLocationFor(TfsFileUtil.getFilePath(file));
    ChangeBrowserSettings settings = new ChangeBrowserSettings();

    final List<TFSChangeList> historyList = getVcs().getCommittedChangesProvider().getCommittedChanges(settings, location, 0);
    return (int)historyList.get(0).getNumber();
  }

  protected void update(VirtualFile root, int revision) {
    TFSProjectConfiguration configuration = TFSProjectConfiguration.getInstance(myProject);
    configuration.getUpdateWorkspaceInfo(myTestWorkspace).setVersion(new ChangesetVersionSpec(revision));
    configuration.UPDATE_RECURSIVELY = true;
    final UpdatedFiles updatedFiles = UpdatedFiles.create();
    final UpdateSession session =
      getVcs().getUpdateEnvironment().updateDirectories(new FilePath[]{TfsFileUtil.getFilePath(root)}, updatedFiles, null);
    Assert.assertTrue(getMessage(session.getExceptions()), session.getExceptions().isEmpty());
    refreshAll();
  }

  protected static void setFileContent(final FilePath file, String content) throws IOException {
    setFileContent(VcsUtil.getVirtualFile(file.getIOFile()), content);
  }

  protected static void setFileContent(VirtualFile file, String content) throws IOException {
    file.setBinaryContent(CharsetToolkit.getUtf8Bytes(content));
  }

}
