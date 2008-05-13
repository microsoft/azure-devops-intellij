package org.jetbrains.tfsIntegration.tests;

import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsConfiguration;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsShowConfirmationOption;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.CurrentContentRevision;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.testFramework.AbstractVcsTestCase;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.core.credentials.Credentials;
import org.jetbrains.tfsIntegration.core.credentials.CredentialsManager;
import org.jetbrains.tfsIntegration.core.tfs.*;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.webservice.WebServiceHelper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"ConstantConditions", "HardCodedStringLiteral"})
public abstract class TFSTestCase extends AbstractVcsTestCase {

  private static final String SERVER = "http://st-tfs02:8080/";
  private static final String SERVER_ROOT = "$/hoo/__test";
  private static final String USER = "tfsuser";
  private static final String DOMAIN = "SWIFTTEAMS";
  private static final String PASSWORD = "Parol";

  private static final String WORKSPACE_NAME_PREFIX = "__testWorkspace_";
  private static final String SANDBOX_PREFIX = "sandbox_";

  private TempDirTestFixture myTempDirFixture;
  private WorkspaceInfo myTestWorkspace;
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
    mySandboxRoot = createDirInCommand(myWorkingCopyDir, SANDBOX_PREFIX + Workstation.getComputerName() + "_" + localRoot.getName());

    prepareServer();

    activateVCS(TFSVcs.TFS_NAME);

    createNewWorkspaceFor(localRoot);

    createServerFolder(TfsFileUtil.getFilePath(mySandboxRoot));
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
    final Credentials testCredentials = new Credentials(USER, DOMAIN, PASSWORD);
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
    TestChangeListBuilder changeListBuilder = new TestChangeListBuilder();
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
  //                                               Collections.singletonList(itemPath.getServerPath()), path.getPath() + "  deleted");
  //}

  protected void commit(final List<Change> changes, final String comment) {
    final List<VcsException> errors = getVcs().getCheckinEnvironment().commit(changes, comment);
    Assert.assertTrue(errors.isEmpty());
  }

  protected void rollback(final List<Change> changes) {
    final List<VcsException> errors = getVcs().getRollbackEnvironment().rollbackChanges(changes);
    Assert.assertTrue(errors.isEmpty());
  }

  protected void scheduleForAddition(VirtualFile... files) {
    final List<VcsException> errors = getVcs().getCheckinEnvironment().scheduleUnversionedFilesForAddition(Arrays.asList(files));
    Assert.assertTrue(errors.isEmpty());
  }

  protected void scheduleForDeletion(FilePath... files) {
    final List<VcsException> errors = getVcs().getCheckinEnvironment().scheduleMissingFileForDeletion(Arrays.asList(files));
    Assert.assertTrue(errors.isEmpty());
  }

  protected void scheduleForDeletion(final VirtualFile file) {
    scheduleForDeletion(TfsFileUtil.getFilePath(file));
  }

  protected void rollbackHijacked(VirtualFile... files) {
    rollbackHijacked(Arrays.asList(files));
  }

  protected void rollbackHijacked(List<VirtualFile> files) {
    final List<VcsException> errors = getVcs().getRollbackEnvironment().rollbackModifiedWithoutCheckout(files);
    Assert.assertTrue(errors.isEmpty());
  }

  protected VirtualFile createAndCommitFile(String name, String content) throws VcsException {
    Assert.assertEquals(0, getChanges(mySandboxRoot).getTotalItems());

    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    VirtualFile file = createFileInCommand(mySandboxRoot, name, content);
    commit(getChanges(mySandboxRoot).getChanges(), "");
    Assert.assertEquals(0, getChanges(mySandboxRoot).getTotalItems());
    return file;
  }

  protected void editFiles(VirtualFile... files) throws VcsException {
    // TODO: use ReadonlyStatusHandler.getInstance(myProject).ensureFilesWritable(file);
    getVcs().getEditFileProvider().editFiles(files);
  }

  protected static String getContent(VirtualFile file) {
    return new CurrentContentRevision(TfsFileUtil.getFilePath(file)).getContent();
  }

  protected static void deleteFileExternally(VirtualFile file) {
    final File ioFile = new File(file.getPath());
    Assert.assertTrue(ioFile.delete());
    VcsUtil.refreshFiles(new File[]{ioFile.getParentFile()}, new Runnable() {
      public void run() {
      }
    });
  }

  protected static void renameAndClearReadonlyExternally(final VirtualFile file, final String newName) throws IOException {
    VirtualFile parent = file.getParent();

    FileUtil.setReadOnlyAttribute(file.getPath(), false);
    final File from = new File(file.getPath());
    final File to = new File(from.getParent(), newName);
    Assert.assertTrue(from.renameTo(to));
    refreshRecursively(parent);
  }


  protected static VirtualFile getVirtualFile(final VirtualFile parent, final String newName) {
    return VcsUtil.getVirtualFile(new File(new File(parent.getPath()), newName));
  }

  protected static void clearReadonlyStatusExternally(VirtualFile... files) throws IOException {
    for (VirtualFile file : files) {
      FileUtil.setReadOnlyAttribute(file.getPath(), false);
      //VcsUtil.refreshFiles(new File[]{new File(file.getPath())}, new Runnable() {
      //  public void run() {
      //  }
      //});
      refreshRecursively(file.getParent());
    }
  }

  protected void rename(final VirtualFile file, final String newName) throws VcsException {
    final File parent = new File(file.getParent().getPath());
    renameFileInCommand(file, newName);
    // TODO: refresh needed?
    VcsUtil.refreshFiles(new File[]{parent}, new Runnable() {
      public void run() {
      }
    });
  }

  public static void refreshRecursively(final VirtualFile parent) {
    TfsFileUtil.executeInEventDispatchThread(new Runnable() {
      public void run() {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
          public void run() {
            parent.refresh(false, true);
          }
        });
      }
    });
  }


}
