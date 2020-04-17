// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.L2.git;

import com.google.common.collect.ImmutableList;
import com.intellij.dvcs.DvcsUtil;
import com.intellij.ide.actions.ImportModuleAction;
import com.intellij.ide.util.newProjectWizard.AddModuleWizard;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.VcsKey;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManagerImpl;
import com.intellij.openapi.vcs.changes.LocalChangeListImpl;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vcs.checkout.VcsAwareCheckoutListener;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.projectImport.ProjectImportProvider;
import com.intellij.util.containers.ContainerUtil;
import git4idea.GitVcs;
import git4idea.commands.Git;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class L2GitUtil {
    public static final String COMMIT_MESSAGE = "test commit";

    /**
     * Adds a new line of text to a file and adds/commits it
     *
     * @param file
     * @param repository
     * @param project
     * @throws IOException
     * @throws IOException
     */
    public static void editAndCommitFile(final File file, final git4idea.repo.GitRepository repository, final Project project) throws IOException {
        // edits file
        final VirtualFile readmeVirtualFile = LocalFileSystem.getInstance().findFileByIoFile(file);
        Assert.assertNotNull("Git repository should have a " + file.getName() + " file", readmeVirtualFile);
        FileUtil.writeToFile(file, "\nnew line", true);

        // adds and commits the change
        final LocalChangeListImpl localChangeList = LocalChangeListImpl.createEmptyChangeListImpl(project, "TestCommit", "12345");
        final ChangeListManagerImpl changeListManager = ChangeListManagerImpl.getInstanceImpl(project);
        VcsDirtyScopeManager.getInstance(project).markEverythingDirty();
        changeListManager.ensureUpToDate(false);
        changeListManager.addUnversionedFiles(localChangeList, ImmutableList.of(readmeVirtualFile));
        final Change change = changeListManager.getChange(LocalFileSystem.getInstance().findFileByIoFile(file));
        repository.getVcs().getCheckinEnvironment().commit(ImmutableList.of(change), COMMIT_MESSAGE);
    }

    /**
     * Clones a repo and returns the Project that it belongs to
     *
     * @param project
     * @param baseDirectory
     * @param myGit
     * @param gitRepoUrl
     * @param teamProject
     * @return
     */
    public static CompletionStage<Project> cloneRepo(
            Project project,
            File baseDirectory,
            Git myGit,
            String gitRepoUrl,
            String teamProject) {
        final CustomCheckoutListener customListener = new CustomCheckoutListener(project);
        final VirtualFile virtualBaseDirectory = LocalFileSystem.getInstance().findFileByIoFile(baseDirectory);

        ProgressManager.getInstance().runProcess(new Runnable() {
            @Override
            public void run() {
                git4idea.checkout.GitCheckoutProvider.clone(project, myGit,
                        customListener,
                        virtualBaseDirectory,
                        gitRepoUrl,
                        teamProject,
                        baseDirectory.getPath());
            }
        }, new ProgressIndicatorBase());

        DvcsUtil.addMappingIfSubRoot(project, FileUtil.join(baseDirectory.getPath(), teamProject), GitVcs.NAME);
        virtualBaseDirectory.refresh(true, true, new Runnable() {
            public void run() {
                if (project.isOpen() && !project.isDisposed() && !project.isDefault()) {
                    final VcsDirtyScopeManager mgr = VcsDirtyScopeManager.getInstance(project);
                    mgr.fileDirty(virtualBaseDirectory);
                }
            }
        });

        return customListener.getNewProjectAsync();
    }
}

/**
 * Custom listener for checkouts that bypasses dialogs but still creates a Project for the repo and associates them
 */
class CustomCheckoutListener implements CheckoutProvider.Listener {
    private final Project myProject;
    private boolean myFoundProject = false;
    private File myFirstDirectory;
    private VcsKey myVcsKey;
    private final CompletableFuture<Project> newProjectFuture = new CompletableFuture<>();

    public CustomCheckoutListener(final Project project) {
        this.myProject = project;
    }

    @Override
    public void directoryCheckedOut(final File directory, final VcsKey vcs) {
        myVcsKey = vcs;
        if (!myFoundProject && directory.isDirectory()) {
            if (myFirstDirectory == null) {
                myFirstDirectory = directory;
            }
            notifyCheckoutListeners(directory, false);
        }
    }

    private void notifyCheckoutListeners(final File directory, final boolean checkoutCompleted) {
        final VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(directory);

        final AddModuleWizard wizard = ImportModuleAction.createImportWizard(null, null, file,
                ProjectImportProvider.PROJECT_IMPORT_PROVIDER.getExtensions());
        if (wizard == null) return;
        ImportModuleAction.createFromWizard(null, wizard);


        if (!checkoutCompleted) {
            final VcsAwareCheckoutListener[] vcsAwareExtensions = Extensions.getExtensions(VcsAwareCheckoutListener.EP_NAME);
            for (VcsAwareCheckoutListener extension : vcsAwareExtensions) {
                boolean processingCompleted = extension.processCheckedOutDirectory(myProject, directory, myVcsKey);
                if (processingCompleted) break;
            }
        }

        Project newProject = findProjectByBaseDirLocation(directory);
        newProjectFuture.complete(newProject);
    }

    public void checkoutCompleted() {
        if (!myFoundProject && myFirstDirectory != null) {
            notifyCheckoutListeners(myFirstDirectory, true);
        }
    }

    @Nullable
    Project findProjectByBaseDirLocation(@NotNull final File directory) {
        return ContainerUtil.find(ProjectManager.getInstance().getOpenProjects(), new Condition<Project>() {
            @Override
            public boolean value(Project project) {
                VirtualFile baseDir = project.getBaseDir();
                return baseDir != null && FileUtil.filesEqual(VfsUtilCore.virtualToIoFile(baseDir), directory);
            }
        });
    }

    public CompletionStage<Project> getNewProjectAsync() {
        return newProjectFuture;
    }
}