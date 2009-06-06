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
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vcs.*;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.openapi.vcs.rollback.RollbackProgressListener;
import com.intellij.openapi.vcs.update.SequentialUpdatesContext;
import com.intellij.openapi.vcs.update.UpdateSession;
import com.intellij.openapi.vcs.update.UpdatedFiles;
import com.intellij.openapi.vcs.versionBrowser.ChangeBrowserSettings;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.AbstractVcsTestCase;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSChangeList;
import org.jetbrains.tfsIntegration.core.TFSProjectConfiguration;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.core.configuration.Credentials;
import org.jetbrains.tfsIntegration.core.configuration.TFSConfigurationManager;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.core.tfs.conflicts.ConflictsEnvironment;
import org.jetbrains.tfsIntegration.core.tfs.operations.ApplyGetOperations;
import org.jetbrains.tfsIntegration.core.tfs.version.ChangesetVersionSpec;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItem;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.*;
import org.jetbrains.tfsIntegration.webservice.WebServiceHelper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

@SuppressWarnings({"ConstantConditions", "HardCodedStringLiteral"})
public abstract class TFSTestCase extends AbstractVcsTestCase {

  protected enum TfsServerVersion {
    TFS_2005_RTM,
    TFS_2005_SP1,
    TFS_2008
  }

  protected static final TfsServerVersion SERVER_VERSION = TfsServerVersion.TFS_2005_SP1;

  private static final String SERVER = "http://tfs-2005-01:8080/";
  private static final String SERVER_ROOT = "$/Test";
  private static final String USER = "tfssetup";
  private static final String DOMAIN = "SWIFTTEAMS";
  private static final String PASSWORD = "";
  private static final String PROXY = null; //"http://tfs-proxy-01:8081/";


  private static final String WORKSPACE_NAME_PREFIX = "__testWorkspace_";
  private static final String SANDBOX_PREFIX = "sandbox_";


  private TempDirTestFixture myTempDirFixture;
  protected WorkspaceInfo myTestWorkspace;
  protected VirtualFile mySandboxRoot;
  private Credentials myOriginalServerCredentials;
  private URI myOriginalProxyUri;

  @Before
  public void setUp() throws Exception {
    Workstation.PRESERVE_CONFIG_FILE = true;

    final IdeaTestFixtureFactory fixtureFactory = IdeaTestFixtureFactory.getFixtureFactory();
    myTempDirFixture = fixtureFactory.createTempDirTestFixture();
    myTempDirFixture.setUp();

    File localRoot = new File(myTempDirFixture.getTempDirPath());
    localRoot.mkdir();


    initProject(localRoot);
    mySandboxRoot = createDirInCommand(myWorkingCopyDir, SANDBOX_PREFIX +
                                                         Workstation.getComputerName() +
                                                         "_" +
                                                         localRoot.getName() +
                                                         "_" +
                                                         Calendar.getInstance().getTimeInMillis(), true);

    prepareServer();

    activateVCS(TFSVcs.TFS_NAME);

    createNewWorkspaceFor(localRoot);

    createServerFolder(TfsFileUtil.getFilePath(mySandboxRoot), "sandbox " + mySandboxRoot.getName() + " created");

    ApplyGetOperations.setLocalConflictHandlingType(ApplyGetOperations.LocalConflictHandlingType.ERROR);

    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    doActionSilently(VcsConfiguration.StandardConfirmation.REMOVE);

    ConflictsEnvironment.setConflictsHandler(null);
    ConflictsEnvironment.setContentMerger(null);
    ConflictsEnvironment.setNameMerger(null);
  }

  private void prepareServer() throws URISyntaxException, TfsException {
    final URI serverUri = new URI(SERVER);

    myOriginalServerCredentials = TFSConfigurationManager.getInstance().getCredentials(serverUri);
    myOriginalProxyUri = TFSConfigurationManager.getInstance().getProxyUri(serverUri);

    boolean serverFound = false;
    for (ServerInfo s : Workstation.getInstance().getServers()) {
      if (!serverUri.equals(s.getUri())) {
        Workstation.getInstance().removeServer(s);
      }
      else {
        serverFound = true;
      }
    }

    final Credentials testCredentials = new Credentials(USER, DOMAIN, PASSWORD, false);
    TFSConfigurationManager.getInstance().storeCredentials(serverUri, testCredentials);
    if (!serverFound) {
      Pair<URI, String> uriAndGuid = WebServiceHelper.authenticate(serverUri);
      ServerInfo newServer = new ServerInfo(serverUri, uriAndGuid.second);
      Workstation.getInstance().addServer(newServer);
    }

    TFSConfigurationManager.getInstance().storeCredentials(serverUri, testCredentials);
    TFSConfigurationManager.getInstance().setProxyUri(serverUri, PROXY != null ? new URI(PROXY) : null);
  }

  @After
  public void tearDown() throws Exception {
    ConflictsEnvironment.setConflictsHandler(null);
    ConflictsEnvironment.setContentMerger(null);
    ConflictsEnvironment.setNameMerger(null);

    try {
      removeWorkspace(myTestWorkspace);
    }
    finally {
      if (myOriginalServerCredentials != null) {
        TFSConfigurationManager.getInstance().storeCredentials(new URI(SERVER), myOriginalServerCredentials);
      }
    }

    TFSConfigurationManager.getInstance().setProxyUri(new URI(SERVER), myOriginalProxyUri);

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
    server.refreshWorkspacesForCurrentOwner();
    for (WorkspaceInfo workspace : server.getWorkspacesForCurrentOwnerAndComputer()) {
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
    if (server != null) {
      server.refreshWorkspacesForCurrentOwner();
      server.deleteWorkspace(workspace);
    }
  }

  protected TestChangeListBuilder getChanges() throws VcsException {
    return getChanges(mySandboxRoot);
  }

  private TestChangeListBuilder getChanges(VirtualFile root) throws VcsException {
    ChangeListManager.getInstance(myProject).ensureUpToDate(false);
    TestChangeListBuilder changeListBuilder = new TestChangeListBuilder(mySandboxRoot, myProject);
    getVcs().getChangeProvider().getChanges(getDirtyScopeForFile(root), changeListBuilder, new EmptyProgressIndicator(), null);
    return changeListBuilder;
  }

  private void createServerFolder(FilePath path, String comment) throws VcsException {
    try {
      ItemPath itemPath = new ItemPath(path, myTestWorkspace.findServerPathsByLocalPath(path, false).iterator().next());
      final ResultWithFailures<GetOperation> addResult = myTestWorkspace.getServer().getVCS()
        .scheduleForAddition(myTestWorkspace.getName(), myTestWorkspace.getOwnerName(), Collections.singletonList(itemPath));
      if (!addResult.getFailures().isEmpty()) {
        throw TfsUtil.collectExceptions(TfsUtil.getVcsExceptions(addResult.getFailures()));
      }

      final ResultWithFailures<CheckinResult> checkinResult = myTestWorkspace.getServer().getVCS()
        .checkIn(myTestWorkspace.getName(), myTestWorkspace.getOwnerName(), Collections.singletonList(itemPath.getServerPath()), comment,
                 Collections.<WorkItem, CheckinWorkItemAction>emptyMap(), Collections.<Pair<String, String>>emptyList(), null);
      if (!checkinResult.getFailures().isEmpty()) {
        throw TfsUtil.collectExceptions(TfsUtil.getVcsExceptions(checkinResult.getFailures()));
      }
    }
    catch (TfsException e) {
      throw new VcsException(e);
    }
  }

  protected void commit(final Collection<Change> changes, final String comment) {
    final List<VcsException> errors = getVcs().getCheckinEnvironment().commit(new ArrayList<Change>(changes), comment);
    Assert.assertTrue(getMessage(errors), errors.isEmpty());
    refreshAll();
  }

  protected void commitThrowException(final Collection<Change> changes, final String comment) throws VcsException {
    final List<VcsException> errors = getVcs().getCheckinEnvironment().commit(new ArrayList<Change>(changes), comment);
    refreshAll();
    if (!errors.isEmpty()) {
      throw new VcsException(getMessage(errors));
    }
  }

  protected void commit(Change change, final String comment) {
    commit(Collections.singletonList(change), comment);
  }

  protected void rollback(final Change change) {
    rollback(Collections.singletonList(change));
  }

  protected void rollback(final Collection<Change> changes) {
    final List<VcsException> errors = new ArrayList<VcsException>();
    getVcs().getRollbackEnvironment().rollbackChanges(new ArrayList<Change>(changes), errors, RollbackProgressListener.EMPTY);
    Assert.assertTrue(getMessage(errors), errors.isEmpty());
    refreshAll();
  }

  protected void rollbackAll(TestChangeListBuilder builder) {
    final List<VcsException> errors = new ArrayList<VcsException>();
    getVcs().getRollbackEnvironment().rollbackMissingFileDeletion(builder.getLocallyDeleted(), errors, RollbackProgressListener.EMPTY);
    getVcs().getRollbackEnvironment().rollbackChanges(builder.getChanges(), errors, RollbackProgressListener.EMPTY);
    // ??? errors.addAll(getVcs().getRollbackEnvironment().rollbackIfUnchanged());
    getVcs().getRollbackEnvironment().rollbackModifiedWithoutCheckout(builder.getHijackedFiles(), errors, RollbackProgressListener.EMPTY);
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
    final List<VcsException> errors = new ArrayList<VcsException>();
    getVcs().getRollbackEnvironment().rollbackModifiedWithoutCheckout(files, errors, RollbackProgressListener.EMPTY);
    Assert.assertTrue(getMessage(errors), errors.isEmpty());
    refreshAll();
  }

  protected void refreshAll() {
    TfsFileUtil.refreshAndMarkDirty(myProject, Collections.singletonList(mySandboxRoot), false);
    ChangeListManager.getInstance(myProject).ensureUpToDate(false);
  }

  protected void editFiles(VirtualFile... files) throws VcsException {
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
    deleteFileExternally(VcsUtil.getVirtualFile(file.getIOFile()));
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

  protected VirtualFile createFileInCommand(final VirtualFile parent, final String name, @Nullable final String content) {
    final VirtualFile result = super.createFileInCommand(parent, name, content);
    assertFile(result, content, true);
    refreshAll();
    return result;
  }

  @SuppressWarnings({"MethodOverloadsMethodOfSuperclass"})
  protected VirtualFile createFileInCommand(final FilePath path, @Nullable final String content) {
    return createFileInCommand(path.getParentPath(), path.getName(), content);
  }

  protected VirtualFile createFileInCommand(final FilePath parent, final String name, @Nullable final String content) {
    return createFileInCommand(VcsUtil.getVirtualFile(parent.getIOFile()), name, content);
  }

  private VirtualFile createDirInCommand(final VirtualFile parent, final String name, boolean supressRefresh) {
    final VirtualFile result = super.createDirInCommand(parent, name);
    if (!supressRefresh) {
      refreshAll();
    }
    assertFolder(getChildPath(parent, name), 0);
    return result;
  }

  protected VirtualFile createDirInCommand(final FilePath path) {
    return createDirInCommand(path.getParentPath(), path.getName());
  }

  protected VirtualFile createDirInCommand(final VirtualFile parent, final String name) {
    return createDirInCommand(parent, name, false);
  }

  protected VirtualFile createDirInCommand(final FilePath parent, final String name) {
    return createDirInCommand(VcsUtil.getVirtualFile(parent.getIOFile()), name);
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

  protected void moveFileInCommand(final FilePath file, final FilePath newParent) {
    final VirtualFile newParentFile = VcsUtil.getVirtualFile(newParent.getIOFile());
    Assert.assertNotNull("Target folder " + newParent.getPresentableUrl() + " not exists", newParentFile);
    moveFileInCommand(file, newParentFile);
  }

  protected void deleteFileInCommand(final VirtualFile file) {
    super.deleteFileInCommand(file);
    refreshAll();
  }

  protected void deleteFileInCommand(final FilePath file) {
    deleteFileInCommand(VcsUtil.getVirtualFile(file.getIOFile()));
  }

  protected void commit() throws VcsException {
    commit(getChanges().getChanges(), "unittest");
  }

  protected void assertFileStatus(VirtualFile file, FileStatus expectedStatus) {
    Assert.assertEquals(expectedStatus, FileStatusManager.getInstance(myProject).getStatus(file));
  }


  protected static String getMessage(Collection<VcsException> errors) {
    String result = "";
    for (VcsException error : errors) {
      result += error + "\n";
    }
    return result;
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
    Assert.assertTrue("Folder " + file.getPresentableUrl() + " not exists", file.getIOFile().exists());
    Assert.assertTrue("Folder " + file.getPresentableUrl() + " is a file", file.getIOFile().isDirectory());
    Assert.assertTrue(file.getIOFile().canWrite());
    Assert.assertEquals("Wrong children number", itemsInside, file.getIOFile().list().length);
    Assert.assertEquals(itemsInside, file.getIOFile().list().length);
  }

  protected static void assertFile(FilePath file, String content, boolean writable) {
    assertFile(file.getIOFile(), content, writable);
  }

  protected static void assertFile(VirtualFile file, String content, boolean writable) {
    assertFile(new File(file.getPath()), content, writable);
  }

  protected static void assertFile(File file, String content, boolean writable) {
    Assert.assertTrue("File " + file + " not exists", file.exists());
    Assert.assertTrue("File " + file + " is folder", file.isFile());

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
      try {
        stream.close();
      }
      catch (IOException e) {
        // ignore
      }
    }

  }

  protected int getLatestRevisionNumber(VirtualFile file) throws VcsException {
    final RepositoryLocation location = getVcs().getCommittedChangesProvider().getLocationFor(TfsFileUtil.getFilePath(file));
    final List<TFSChangeList> historyList = getVcs().getCommittedChangesProvider()
      .getCommittedChanges(new ChangeBrowserSettings(), location, getVcs().getCommittedChangesProvider().getUnlimitedCountValue());
    return (int)historyList.get(0).getNumber();
  }

  protected void update(VirtualFile root, int revision) {
    TFSProjectConfiguration configuration = TFSProjectConfiguration.getInstance(myProject);
    configuration.getUpdateWorkspaceInfo(myTestWorkspace).setVersion(new ChangesetVersionSpec(revision));
    configuration.UPDATE_RECURSIVELY = true;
    final UpdatedFiles updatedFiles = UpdatedFiles.create();
    final UpdateSession session = getVcs().getUpdateEnvironment()
      .updateDirectories(new FilePath[]{TfsFileUtil.getFilePath(root)}, updatedFiles, null, new Ref<SequentialUpdatesContext>());
    Assert.assertTrue(getMessage(session.getExceptions()), session.getExceptions().isEmpty());
    refreshAll();
  }

  protected void updateTo(int revisionAge) throws VcsException {
    update(mySandboxRoot, getLatestRevisionNumber(mySandboxRoot) - revisionAge);
  }

  protected static void setFileContent(final FilePath file, String content) throws IOException {
    setFileContent(VcsUtil.getVirtualFile(file.getIOFile()), content);
  }

  protected static void setFileContent(VirtualFile file, String content) throws IOException {
    file.setBinaryContent(CharsetToolkit.getUtf8Bytes(content));
  }

  protected static FilePath getChildPath(final FilePath parent, final String childName) {
    return VcsUtil.getFilePath(new File(parent.getIOFile(), childName));
  }

  protected static FilePath getChildPath(final VirtualFile parent, final String childName) {
    return VcsUtil.getFilePath(new File(new File(parent.getPath()), childName));
  }

  protected String findServerPath(final FilePath localPath) throws TfsException {
    return myTestWorkspace.findServerPathsByLocalPath(localPath, false).iterator().next();
  }

  protected int getItemId(final FilePath path) throws TfsException {
    final ExtendedItem extendedItem = myTestWorkspace.getServer().getVCS()
      .getExtendedItem(myTestWorkspace.getName(), myTestWorkspace.getOwnerName(), path, RecursionType.None, DeletedState.NonDeleted);
    return extendedItem.getItemid();
  }

  protected void assertUserNameEqual(String userName) {
    Assert.assertEquals((DOMAIN + "\\" + USER).toUpperCase(), userName.toUpperCase());
  }

}
