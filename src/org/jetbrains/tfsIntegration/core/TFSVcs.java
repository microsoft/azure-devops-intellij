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
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vcs.*;
import com.intellij.openapi.vcs.annotate.AnnotationProvider;
import com.intellij.openapi.vcs.changes.ChangeProvider;
import com.intellij.openapi.vcs.diff.DiffProvider;
import com.intellij.openapi.vcs.history.VcsHistoryProvider;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vcs.rollback.RollbackEnvironment;
import com.intellij.openapi.vcs.update.UpdateEnvironment;
import com.intellij.openapi.vcs.versionBrowser.ChangeBrowserSettings;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ultimate.PluginVerifier;
import com.intellij.ultimate.UltimateVerifier;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.checkin.CheckinParameters;
import org.jetbrains.tfsIntegration.core.tfs.TfsFileUtil;
import org.jetbrains.tfsIntegration.core.tfs.TfsRevisionNumber;
import org.jetbrains.tfsIntegration.core.tfs.Workstation;

import javax.swing.*;
import java.util.List;

public class TFSVcs extends AbstractVcs {

  public interface RevisionChangedListener {
    void revisionChanged();
  }

  public static class CheckinData {
    public CheckinParameters parameters;
    JLabel messageLabel;
  }

  // TODO make private
  @NonNls public static final String TFS_NAME = "TFS";
  public static final Logger LOG = Logger.getInstance("org.jetbrains.tfsIntegration.core.TFSVcs");
  private static final VcsKey ourKey = createKey(TFS_NAME);

  private VcsVFSListener myFileListener;
  private final VcsShowConfirmationOption myAddConfirmation;
  private final VcsShowConfirmationOption myDeleteConfirmation;
  private final VcsShowSettingOption myCheckoutOptions;
  private CommittedChangesProvider<TFSChangeList, ChangeBrowserSettings> myCommittedChangesProvider;
  private VcsHistoryProvider myHistoryProvider;
  private DiffProvider myDiffProvider;
  private TFSCheckinEnvironment myCheckinEnvironment;
  private UpdateEnvironment myUpdateEnvironment;
  private AnnotationProvider myAnnotationProvider;
  private final List<RevisionChangedListener> myRevisionChangedListeners = ContainerUtil.createLockFreeCopyOnWriteList();
  private final CheckinData myCheckinData = new CheckinData();

  public TFSVcs(@NotNull Project project, UltimateVerifier verifier) {
    super(project, TFS_NAME);
    PluginVerifier.verifyUltimatePlugin(verifier);

    final ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
    myAddConfirmation = vcsManager.getStandardConfirmation(VcsConfiguration.StandardConfirmation.ADD, this);
    myDeleteConfirmation = vcsManager.getStandardConfirmation(VcsConfiguration.StandardConfirmation.REMOVE, this);
    myCheckoutOptions = vcsManager.getStandardOption(VcsConfiguration.StandardOption.CHECKOUT, this);
  }

  public CheckinData getCheckinData() {
    return myCheckinData;
  }

  public static TFSVcs getInstance(Project project) {
    return (TFSVcs)ProjectLevelVcsManager.getInstance(project).findVcsByName(TFS_NAME);
  }

  @NonNls
  public String getDisplayName() {
    return "TFS";
  }

  public Configurable getConfigurable() {
    return new TFSProjectConfigurable(myProject);
  }


  @Override
  public void activate() {
    myFileListener = new TFSFileListener(getProject(), this);
    TfsSdkManager.activate();
  }

  @Override
  public void deactivate() {
    Disposer.dispose(myFileListener);
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

  public ChangeProvider getChangeProvider() {
    return new TFSChangeProvider(myProject);
  }

  @NotNull
  public TFSCheckinEnvironment createCheckinEnvironment() {
    if (myCheckinEnvironment == null) {
      myCheckinEnvironment = new TFSCheckinEnvironment(this);
    }
    return myCheckinEnvironment;
  }

  public RollbackEnvironment createRollbackEnvironment() {
    return new TFSRollbackEnvironment(myProject);
  }

  public boolean fileIsUnderVcs(final FilePath filePath) {
    return isVersionedDirectory(filePath.getVirtualFile());
  }

  public boolean isVersionedDirectory(final VirtualFile dir) {
    if (dir == null) {
      return false;
    }
    return (!Workstation.getInstance().findWorkspacesCached(TfsFileUtil.getFilePath(dir), false).isEmpty());
  }

  public EditFileProvider getEditFileProvider() {
    return new TFSEditFileProvider(myProject);
  }

  public UpdateEnvironment createUpdateEnvironment() {
    if (myUpdateEnvironment == null) {
      myUpdateEnvironment = new TFSUpdateEnvironment(this);
    }
    return myUpdateEnvironment;
  }

  public AnnotationProvider getAnnotationProvider() {
    if (myAnnotationProvider == null) {
      myAnnotationProvider = new TFSAnnotationProvider(this);
    }
    return myAnnotationProvider;
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

  @NotNull
  public CommittedChangesProvider<TFSChangeList, ChangeBrowserSettings> getCommittedChangesProvider() {
    if (myCommittedChangesProvider == null) {
      myCommittedChangesProvider = new TFSCommittedChangesProvider(myProject);
    }
    return myCommittedChangesProvider;
  }

  public VcsHistoryProvider getVcsHistoryProvider() {
    if (myHistoryProvider == null) {
      myHistoryProvider = new TFSHistoryProvider(myProject);
    }
    return myHistoryProvider;
  }

  public DiffProvider getDiffProvider() {
    if (myDiffProvider == null) {
      myDiffProvider = new TFSDiffProvider(myProject);
    }
    return myDiffProvider;
  }

  @Nullable
  public VcsRevisionNumber parseRevisionNumber(final String revisionNumberString) {
    return TfsRevisionNumber.tryParse(revisionNumberString);
  }

  @Nullable
  public String getRevisionPattern() {
    return ourIntegerPattern;
  }

  public void fireRevisionChanged() {
    for (RevisionChangedListener listener : myRevisionChangedListeners) {
      listener.revisionChanged();
    }
  }

  public void addRevisionChangedListener(RevisionChangedListener listener) {
    myRevisionChangedListeners.add(listener);
  }

  public void removeRevisionChangedListener(RevisionChangedListener listener) {
    myRevisionChangedListeners.remove(listener);
  }

  public static VcsKey getKey() {
    return ourKey;
  }

  public static boolean isUnderTFS(FilePath path, Project project) {
    AbstractVcs vcs = VcsUtil.getVcsFor(project, path);
    return vcs != null && TFS_NAME.equals(vcs.getName());
  }

  @Override
  public CheckoutProvider getCheckoutProvider() {
    return new TFSCheckoutProvider();
  }
}
