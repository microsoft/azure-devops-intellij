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

package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.*;
import com.intellij.openapi.vcs.changes.ChangeProvider;
import com.intellij.openapi.vcs.checkin.CheckinEnvironment;
import com.intellij.openapi.vcs.diff.DiffProvider;
import com.intellij.openapi.vcs.history.VcsHistoryProvider;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vcs.rollback.RollbackEnvironment;
import com.intellij.openapi.vcs.update.UpdateEnvironment;
import com.intellij.openapi.vcs.versionBrowser.ChangeBrowserSettings;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.TfsFileUtil;
import org.jetbrains.tfsIntegration.core.tfs.Workstation;
import org.jetbrains.tfsIntegration.exceptions.TfsException;

public class TFSVcs extends AbstractVcs {

  @NonNls public static final String TFS_NAME = "TFS";
  public static final Logger LOG = Logger.getInstance("org.jetbrains.tfsIntegration.core.TFSVcs");
  private TFSProjectConfiguration myProjectConfiguration;
  private TFSFileListener myFileListener;
  private VcsShowConfirmationOption myAddConfirmation;
  private VcsShowConfirmationOption myDeleteConfirmation;
  private VcsShowSettingOption myCheckoutOptions;
  private CommittedChangesProvider<TFSChangeList, ChangeBrowserSettings> myCommittedChangesProvider;
  private VcsHistoryProvider myTFSHistoryProvider;
  private DiffProvider myDiffProvider;

  public TFSVcs(Project project, TFSProjectConfiguration projectConfiguration) {
    super(project);
    myProjectConfiguration = projectConfiguration;

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
    myFileListener = new TFSFileListener(getProject(), this);
    TFSApplicationSettings.getInstance().tfsActivated();
  }

  @Override
  public void deactivate() {
    myFileListener.dispose();
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
    return new TFSCheckinEnvironment(myProject);
  }

  @Nullable
  public RollbackEnvironment getRollbackEnvironment() {
    return new TFSRollbackEnvironment(myProject);
  }

  public boolean fileIsUnderVcs(final FilePath filePath) {
    return isVersionedDirectory(filePath.getVirtualFile());
  }

  public boolean isVersionedDirectory(final VirtualFile dir) {
    try {
      return !Workstation.getInstance().findWorkspace(TfsFileUtil.getFilePath(dir), false).isEmpty();
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
    return new TFSUpdateEnvironment(myProject);
  }

  @Nullable
  public UpdateEnvironment getStatusEnvironment() {
    return super.getStatusEnvironment();    //To change body of overridden methods use File | Settings | File Templates.
  }

  public static void assertTrue(boolean condition, @NonNls String message) {
    // TODO: inline with assert statement
    LOG.assertTrue(condition, message);
    if (!condition) {
      error(message);
    }
  }

  public static void error(@NonNls String message) {
    // TODO: inline with assert statement
    LOG.error(message);
    throw new RuntimeException("Assertion failed: " + message);
  }

  public static void assertTrue(final boolean condition) {
    assertTrue(condition, "");
  }

  @Nullable
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

  @NotNull
  public CommittedChangesProvider<TFSChangeList, ChangeBrowserSettings> getCommittedChangesProvider() {
    if (myCommittedChangesProvider == null) {
      myCommittedChangesProvider = new TFSCommittedChangesProvider(myProject);
    }
    return myCommittedChangesProvider;
  }

  public VcsHistoryProvider getVcsHistoryProvider() {
    if (myTFSHistoryProvider == null) {
      myTFSHistoryProvider = new TFSHistoryProvider(myProject);
    }
    return myTFSHistoryProvider;
  }

  public DiffProvider getDiffProvider() {
    if (myDiffProvider == null) {
      myDiffProvider = new TFSDiffProvider(myProject);
    }
    return myDiffProvider;
  }

  @Nullable
  public VcsRevisionNumber parseRevisionNumber(final String revisionNumberString) {
    try {
      int revisionNumber = Integer.parseInt(revisionNumberString);
      return new VcsRevisionNumber.Int(revisionNumber);
    }
    catch (NumberFormatException e) {
      return null;
    }
  }

  @Nullable
  public String getRevisionPattern() {
    return ourIntegerPattern;
  }
}
