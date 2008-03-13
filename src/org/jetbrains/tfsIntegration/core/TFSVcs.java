package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.*;
import com.intellij.openapi.vcs.changes.ChangeProvider;
import com.intellij.openapi.vcs.checkin.CheckinEnvironment;
import com.intellij.openapi.vcs.rollback.RollbackEnvironment;
import com.intellij.openapi.vcs.update.UpdateEnvironment;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.Workstation;
import org.jetbrains.tfsIntegration.stubs.exceptions.TfsException;

public class TFSVcs extends AbstractVcs {

  @NonNls public static final String TFS_NAME = "TFS";
  public static final Logger LOG = Logger.getInstance("org.jetbrains.tfsIntegration.core.TFSVcs");
  private TFSProjectConfiguration myProjectConfiguration;
  private TFSEntriesFileListener myEntriesFileListener;
  private VcsShowConfirmationOption myAddConfirmation;
  private VcsShowConfirmationOption myDeleteConfirmation;
  private VcsShowSettingOption myCheckoutOptions;

  public TFSVcs(Project project, TFSProjectConfiguration projectConfiguration) {
    super(project);
    myProjectConfiguration = projectConfiguration;
    myEntriesFileListener = new TFSEntriesFileListener(project);

    final ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
    myAddConfirmation = vcsManager.getStandardConfirmation(VcsConfiguration.StandardConfirmation.ADD, this);
    myDeleteConfirmation = vcsManager.getStandardConfirmation(VcsConfiguration.StandardConfirmation.REMOVE, this);
    myCheckoutOptions = vcsManager.getStandardOption(VcsConfiguration.StandardOption.CHECKOUT, this);
  }

  public static TFSVcs getInstance(Project project) {
    return (TFSVcs)ProjectLevelVcsManager.getInstance(project).findVcsByName(TFS_NAME);
  }

  @NonNls
  public String getName() {
    LOG.debug("getName");
    return TFS_NAME;
  }

  @NonNls
  public String getDisplayName() {
    LOG.debug("getDisplayName");
    return "TFS";
  }

  public Configurable getConfigurable() {
    LOG.debug("createConfigurable");
    return new TFSProjectConfigurable(myProject);
  }

  public Project getProject() {
    return myProject;
  }

  @Override
  public void activate() {
    super.activate();
    TFSApplicationSettings.getInstance().tfsActivated();
    VirtualFileManager.getInstance().addVirtualFileListener(myEntriesFileListener);
  }

  @Override
  public void deactivate() {
    VirtualFileManager.getInstance().removeVirtualFileListener(myEntriesFileListener);
    TFSApplicationSettings.getInstance().tfsDeactivated();
    super.deactivate();
  }

  public VcsShowConfirmationOption getAddConfirmation() {
    return myAddConfirmation;
  }

  public VcsShowConfirmationOption getDeleteConfirmation() {
    return myDeleteConfirmation;
  }

  public VcsShowSettingOption getCheckoutOptions() {
    return myCheckoutOptions;
  }

  public TFSProjectConfiguration getProjectConfiguration() {
    return myProjectConfiguration;
  }

  @Nullable
  public ChangeProvider getChangeProvider() {
    return new TFSChangeProvider(myProject);
  }

  @Nullable
  public CheckinEnvironment getCheckinEnvironment() {
    return new TFSCheckinEnvironment();
  }

  @Nullable
  public RollbackEnvironment getRollbackEnvironment() {
    return new TFSRollbackEnvironment();
  }

  public boolean fileIsUnderVcs(final FilePath filePath) {
    return isVersionedDirectory(filePath.getVirtualFile());
  }

  public boolean isVersionedDirectory(final VirtualFile dir) {
    try {
      return Workstation.getInstance().findWorkspace(dir.getPath()) != null;
    }
    catch (TfsException e) {
      LOG.info(e);
    }
    return false;
  }

  @Nullable
  public EditFileProvider getEditFileProvider() {
    return new TFSEditFileProvider();
  }

  @Nullable
  public UpdateEnvironment getUpdateEnvironment() {
    return super.getUpdateEnvironment();    //To change body of overridden methods use File | Settings | File Templates.
  }

  @Nullable
  public UpdateEnvironment getStatusEnvironment() {
    return super.getStatusEnvironment();    //To change body of overridden methods use File | Settings | File Templates.
  }

  public static void assertTrue(boolean condition, String message) {
    // TODO: inline with assert statement
    LOG.assertTrue(condition, message);
  }

  public static void assertTrue(final boolean condition) {
    // TODO: inline with assert statement
    LOG.assertTrue(condition);
  }

  public static String getProxyHost() {
    if (Boolean.parseBoolean(System.getProperty("TFS.UseFiddler"))) {
      return "127.0.0.1";
    }
    else {
      return null;
    }
  }

  public static int getProxyPort() {
    if (Boolean.parseBoolean(System.getProperty("TFS.UseFiddler"))) {
      return 8888;
    }
    else {
      return -1;
    }
  }

}
