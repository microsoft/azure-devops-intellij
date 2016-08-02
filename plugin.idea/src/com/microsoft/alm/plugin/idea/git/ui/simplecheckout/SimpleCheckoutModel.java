// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.ui.simplecheckout;

import com.intellij.dvcs.DvcsUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareRunnable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager;
import com.intellij.openapi.vcs.impl.ProjectLevelVcsManagerImpl;
import com.intellij.openapi.vcs.impl.VcsInitObject;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.AbstractModel;
import com.microsoft.alm.plugin.idea.common.ui.common.ModelValidationInfo;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import com.microsoft.alm.plugin.services.PluginServiceProvider;
import com.microsoft.alm.plugin.services.PropertyService;
import com.microsoft.alm.plugin.telemetry.TfsTelemetryHelper;
import git4idea.GitRemoteBranch;
import git4idea.GitVcs;
import git4idea.branch.GitBrancher;
import git4idea.commands.Git;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The model for the SimpleCheckout dialog UI
 */
public class SimpleCheckoutModel extends AbstractModel {
    private final Logger logger = LoggerFactory.getLogger(SimpleCheckoutModel.class);

    public final static String DEFAULT_SOURCE_PATH = System.getProperty("user.home");
    public final static String PROP_DIRECTORY_NAME = "directoryName";
    public final static String PROP_PARENT_DIR = "parentDirectory";
    public final static String COMMANDLINE_CLONE_ACTION = "commandline-clone";
    public final static Pattern GIT_URL_PATTERN = Pattern.compile("/_git/(.*)");
    private final static String MASTER_BRANCH = "master";

    private final Project project;
    private final CheckoutProvider.Listener listener;
    private final String gitUrl;
    private final String ref;
    private String parentDirectory;
    private String directoryName;

    protected SimpleCheckoutModel(final Project project, final CheckoutProvider.Listener listener, final String gitUrl, final String ref) {
        super();
        this.project = project;
        this.listener = listener;
        this.gitUrl = gitUrl;
        this.ref = ref;

        this.parentDirectory = PluginServiceProvider.getInstance().getPropertyService().getProperty(PropertyService.PROP_REPO_ROOT);
        // use default root if no repo root is found
        if (StringUtils.isEmpty(this.parentDirectory)) {
            this.parentDirectory = DEFAULT_SOURCE_PATH;
        }

        // try and parse for the repo name to use as the directory name
        final Matcher matcher = GIT_URL_PATTERN.matcher(gitUrl);
        if (matcher.find() && matcher.groupCount() == 1) {
            this.directoryName = matcher.group(1);
        } else {
            this.directoryName = StringUtils.EMPTY;
        }
    }

    public Project getProject() {
        return project;
    }

    public String getParentDirectory() {
        return parentDirectory;
    }

    public void setParentDirectory(final String parentDirectory) {
        if (!StringUtils.equals(this.parentDirectory, parentDirectory)) {
            this.parentDirectory = parentDirectory;
            setChangedAndNotify(PROP_PARENT_DIR);
        }
    }

    public String getDirectoryName() {
        return directoryName;
    }

    public void setDirectoryName(final String directoryName) {
        if (!StringUtils.equals(this.directoryName, directoryName)) {
            this.directoryName = directoryName;
            setChangedAndNotify(PROP_DIRECTORY_NAME);
        }
    }

    public String getRepoUrl() {
        return gitUrl;
    }

    public ModelValidationInfo validate() {
        final String parentDirectory = getParentDirectory();
        if (parentDirectory == null || parentDirectory.isEmpty()) {
            return ModelValidationInfo.createWithResource(PROP_PARENT_DIR,
                    TfPluginBundle.KEY_CHECKOUT_DIALOG_ERRORS_PARENT_DIR_EMPTY);
        }

        final File parentDirectoryOnDisk = new File(parentDirectory);
        if (!parentDirectoryOnDisk.exists()) {
            return ModelValidationInfo.createWithResource(PROP_PARENT_DIR,
                    TfPluginBundle.KEY_CHECKOUT_DIALOG_ERRORS_PARENT_DIR_NOT_FOUND);
        }

        // We test this method and so we need to check to see if we are in IntelliJ before using VirtualFileManager
        // ApplicationManager is null if we are not in IntelliJ
        if (ApplicationManager.getApplication() != null) {
            final VirtualFile destinationParent = LocalFileSystem.getInstance().findFileByPath(parentDirectory);
            if (destinationParent == null) {
                return ModelValidationInfo.createWithResource(PROP_PARENT_DIR,
                        TfPluginBundle.KEY_CHECKOUT_DIALOG_ERRORS_PARENT_DIR_NOT_FOUND);
            }
        }

        final String directoryName = getDirectoryName();
        if (directoryName == null || directoryName.isEmpty()) {
            return ModelValidationInfo.createWithResource(PROP_DIRECTORY_NAME,
                    TfPluginBundle.KEY_CHECKOUT_DIALOG_ERRORS_DIR_NAME_EMPTY);
        }

        final File destDirectoryOnDisk = new File(parentDirectory, directoryName);
        //verify the destination directory does not exist
        if (destDirectoryOnDisk.exists() && destDirectoryOnDisk.isDirectory()) {
            return ModelValidationInfo.createWithResource(PROP_DIRECTORY_NAME,
                    TfPluginBundle.KEY_CHECKOUT_DIALOG_ERRORS_DESTINATION_EXISTS, directoryName);
        }
        //verify destination directory parent exists, we can reach this condition if user specifies a path for directory name
        if (destDirectoryOnDisk.getParentFile() == null || !destDirectoryOnDisk.getParentFile().exists()) {
            return ModelValidationInfo.createWithResource(PROP_DIRECTORY_NAME,
                    TfPluginBundle.KEY_CHECKOUT_DIALOG_ERRORS_DIR_NAME_INVALID,
                    directoryName, destDirectoryOnDisk.getParent());
        }

        return ModelValidationInfo.NO_ERRORS;
    }

    public void cloneRepo() {
        final ModelValidationInfo validationInfo = validate();
        if (validationInfo == null) {
            final Task.Backgroundable createCloneTask = new Task.Backgroundable(project, TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_DIALOG_TITLE),
                    true, PerformInBackgroundOption.DEAF) {
                final AtomicBoolean cloneResult = new AtomicBoolean();

                @Override
                public void run(@NotNull final ProgressIndicator progressIndicator) {
                    progressIndicator.setText(TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_DIALOG_TITLE));
                    // get context from manager, and store in active context
                    final ServerContext context = ServerContextManager.getInstance().getUpdatedContext(gitUrl, true);

                    if (context == null) {
                        logger.warn("No context could be found");
                        VcsNotifier.getInstance(project).notifyError(TfPluginBundle.message(TfPluginBundle.KEY_CHECKOUT_ERRORS_AUTHENTICATION_FAILED_TITLE), TfPluginBundle.message(TfPluginBundle.KEY_ERRORS_AUTH_NOT_SUCCESSFUL, gitUrl));
                        return;
                    }

                    final String gitRepositoryStr = context.getUsableGitUrl();
                    final Git git = ServiceManager.getService(Git.class);
                    logger.info("Cloning repo " + gitRepositoryStr);
                    cloneResult.set(git4idea.checkout.GitCheckoutProvider.doClone(project, git, getDirectoryName(), getParentDirectory(), gitRepositoryStr));

                    // Add Telemetry for the clone call along with it's success/failure
                    TfsTelemetryHelper.getInstance().sendEvent(COMMANDLINE_CLONE_ACTION, new TfsTelemetryHelper.PropertyMapBuilder()
                            .currentOrActiveContext(context)
                            .actionName(COMMANDLINE_CLONE_ACTION)
                            .success(cloneResult.get()).build());
                }

                @Override
                public void onSuccess() {
                    logger.info("Simple clone was a success");
                    // if clone was successful then complete the checkout process which gives the option to open the project
                    if (cloneResult.get()) {
                        final VirtualFile destinationParent = LocalFileSystem.getInstance().findFileByIoFile(
                                new File(getParentDirectory()));
                        final File projectDirectory = new File(parentDirectory, directoryName);

                        DvcsUtil.addMappingIfSubRoot(project, FileUtil.join(new String[]{parentDirectory, directoryName}), "Git");
                        destinationParent.refresh(true, true, new Runnable() {
                            public void run() {
                                if (project.isOpen() && !project.isDisposed() && !project.isDefault()) {
                                    VcsDirtyScopeManager mgr = VcsDirtyScopeManager.getInstance(project);
                                    mgr.fileDirty(destinationParent);
                                }

                            }
                        });

                        listener.directoryCheckedOut(projectDirectory, GitVcs.getKey());
                        listener.checkoutCompleted();

                        // the project has changed since a new project was created above during the directoryCheckedOut process
                        // finding the new project based on the repo path
                        final Project currentProject = IdeaHelper.getProject(projectDirectory.getPath());

                        // check if ref is not master and if currentProject is not null
                        // if currentProject is null that means the user chose not to create the project so not checking the branch out
                        if (StringUtils.isNotEmpty(ref) && !StringUtils.equals(ref, MASTER_BRANCH) && currentProject != null) {
                            logger.info("Non-master branch detected to checkout");
                            checkoutBranch(ref, currentProject, projectDirectory);
                        }
                    }
                }

                private void checkoutBranch(final String ref, final Project lastOpenedProject, final File projectDirectory) {
                    // adds a post initialization step to the project to checkout the given branch
                    final ProjectLevelVcsManagerImpl manager = (ProjectLevelVcsManagerImpl) ProjectLevelVcsManager.getInstance(lastOpenedProject);

                    // add step to refresh the root mapping so the new root is found for the repo
                    // TODO: refactor to use existing call instead of calling twice. Current call happens too late currently
                    // TODO: so that's why we need to call this beforehand so we can checkout the branch
                    manager.addInitializationRequest(VcsInitObject.MAPPINGS, new Runnable() {
                        @Override
                        public void run() {
                            manager.setDirectoryMapping(projectDirectory.getPath(), "Git");
                            manager.fireDirectoryMappingsChanged();
                        }
                    });

                    // step to checkout the desired branch
                    manager.addInitializationRequest(VcsInitObject.AFTER_COMMON, new DumbAwareRunnable() {
                        public void run() {
                            final GitRepositoryManager gitRepositoryManager = ServiceManager.getService(lastOpenedProject, GitRepositoryManager.class);
                            ArgumentHelper.checkNotNull(gitRepositoryManager, "GitRepositoryManager");
                            ArgumentHelper.checkNotNullOrEmpty(gitRepositoryManager.getRepositories(), "gitRepositoryManager.getRepositories()");
                            // TODO: use more direct manner to get repo but right now due to timing we can't
                            final GitRepository gitRepository = gitRepositoryManager.getRepositories().get(0);
                            ArgumentHelper.checkNotNull(gitRepository, "GitRepository");
                            String fullRefName = StringUtils.EMPTY;

                            // find remote red name from given name
                            for (final GitRemoteBranch remoteBranch : gitRepository.getInfo().getRemoteBranches()) {
                                final String remoteBranchName = remoteBranch.getName().replaceFirst(remoteBranch.getRemote().getName() + "/", StringUtils.EMPTY);
                                if (ref.equals(remoteBranchName)) {
                                    fullRefName = remoteBranch.getName();
                                }
                            }

                            if (StringUtils.isNotEmpty(fullRefName)) {
                                final String remoteRef = fullRefName;
                                // Checking out a branch using the brancher has to start on the UI thread but moves to the background
                                IdeaHelper.runOnUIThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        logger.info("Checking out branch " + remoteRef);
                                        final GitBrancher brancher = ServiceManager.getService(lastOpenedProject, GitBrancher.class);
                                        brancher.checkoutNewBranchStartingFrom(ref, remoteRef,
                                                Collections.singletonList(gitRepository), null);
                                    }
                                });
                            } else {
                                throw new IllegalArgumentException(String.format("Ref %s was not found remotely so could not be checked out.", fullRefName));
                            }
                        }
                    });
                }
            };
            createCloneTask.queue();
        }
    }
}