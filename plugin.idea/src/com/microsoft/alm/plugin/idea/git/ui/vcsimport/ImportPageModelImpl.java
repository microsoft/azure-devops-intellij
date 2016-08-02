// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.ui.vcsimport;

import com.intellij.notification.NotificationListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vcs.VcsShowConfirmationOption;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ui.SelectFilesDialog;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.vcsUtil.VcsFileUtil;
import com.intellij.vcsUtil.VcsUtil;
import com.microsoft.alm.client.model.VssServiceException;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextBuilder;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.idea.common.resources.Icons;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.common.ui.common.LoginPageModelImpl;
import com.microsoft.alm.plugin.idea.common.ui.common.ModelValidationInfo;
import com.microsoft.alm.plugin.idea.common.ui.common.ServerContextLookupListener;
import com.microsoft.alm.plugin.idea.common.ui.common.ServerContextLookupPageModel;
import com.microsoft.alm.plugin.idea.common.ui.common.ServerContextTableModel;
import com.microsoft.alm.plugin.idea.common.utils.IdeaHelper;
import com.microsoft.alm.plugin.telemetry.TfsTelemetryHelper;
import com.microsoft.alm.core.webapi.CoreHttpClient;
import com.microsoft.alm.core.webapi.model.TeamProject;
import com.microsoft.alm.sourcecontrol.webapi.GitHttpClient;
import git4idea.DialogManager;
import git4idea.GitUtil;
import git4idea.actions.GitInit;
import git4idea.commands.Git;
import git4idea.commands.GitCommand;
import git4idea.commands.GitCommandResult;
import git4idea.commands.GitHandlerUtil;
import git4idea.commands.GitLineHandler;
import git4idea.commands.GitSimpleHandler;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import git4idea.util.GitFileUtils;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ListSelectionModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * This class is provided as a base for the VSO and TFS import page models. It provides the majority of the
 * functionality with a few abstract methods that must be overridden.
 */
public abstract class ImportPageModelImpl extends LoginPageModelImpl implements ImportPageModel, ServerContextLookupPageModel {
    private static final Logger logger = LoggerFactory.getLogger(ImportPageModelImpl.class);

    private ImportModel parentDialogModel;
    private boolean loading = false;
    private boolean importEnabled = false;
    //default values for Strings should be "" rather than null.
    private String teamProjectFilter = "";
    private String repositoryName = "";
    private final ServerContextTableModel teamProjectTableModel;
    private final ServerContextLookupListener teamProjectProvider;

    private final static String ACTION_NAME = "import";
    private final static String REMOTE_ORIGIN = "origin";
    private final static String PROJECT_CAPABILITY_VC = "versioncontrol";
    private final static String PROJECT_CAPABILITY_VC_TYPE = "sourceControlType";
    private final static String PROJECT_CAPABILITY_VC_GIT = "Git";

    public ImportPageModelImpl(final ImportModel importModel, final ServerContextTableModel.Column[] columns) {
        super(importModel);
        parentDialogModel = importModel;

        // Create table model (subclasses should modify the table model as needed)
        teamProjectTableModel = new ServerContextTableModel(columns);

        // Create the default teamProject provider
        teamProjectProvider = new ServerContextLookupListener(this);

        // Set default repository name
        // We test this method and so we need to check to see if we are in IntelliJ
        // ApplicationManager is null if we are not in IntelliJ
        if (ApplicationManager.getApplication() != null) {
            repositoryName = importModel.getProject().getName();
        }
    }

    /**
     * This getter allows the derived classes to access the ImportModel that owns them.
     */
    protected ImportModel getParentModel() {
        return parentDialogModel;
    }

    /**
     * This setter allows the tests to set the parent model.
     */
    protected void setParentModel(final ImportModel parentDialogModel) {
        this.parentDialogModel = parentDialogModel;
    }

    protected ServerContextLookupListener getTeamProjectProvider() {
        return this.teamProjectProvider;
    }

    protected abstract AuthenticationInfo getAuthenticationInfo();

    /**
     * Overriding SignOut to do a couple additional things.
     */
    @Override
    public void signOut() {
        super.signOut();
        setConnected(false);
        setLoading(false);
        clearContexts();
    }

    @Override
    public String getRepositoryName() {
        return repositoryName;
    }

    @Override
    public void setRepositoryName(final String repositoryName) {
        if (!StringUtils.equals(this.repositoryName, repositoryName)) {
            this.repositoryName = repositoryName;
            setChangedAndNotify(PROP_REPO_NAME);
        }
    }

    @Override
    public String getTeamProjectFilter() {
        return teamProjectFilter;
    }

    @Override
    public void setTeamProjectFilter(final String teamProjectFilter) {
        if (!StringUtils.equals(this.teamProjectFilter, teamProjectFilter)) {
            this.teamProjectFilter = teamProjectFilter;
            setChangedAndNotify(PROP_PROJECT_FILTER);
            teamProjectTableModel.setFilter(teamProjectFilter);
        }
    }

    @Override
    public boolean isLoading() {
        return loading;
    }

    @Override
    public void setLoading(final boolean loading) {
        if (this.loading != loading) {
            this.loading = loading;
            setChangedAndNotify(PROP_LOADING);
        }
    }

    @Override
    public void setImportEnabled(final boolean importEnabled) {
        if (this.importEnabled != importEnabled) {
            this.importEnabled = importEnabled;
            if (getParentModel() != null) {
                getParentModel().updateImportEnabled();
            }
        }
    }

    @Override
    public void setConnected(boolean connected) {
        super.setConnected(connected);
        setImportEnabled(connected);
    }

    @Override
    public void importIntoRepository() {
        final ModelValidationInfo validationInfo = validate();
        if (validationInfo == null) {
            final ServerContext selectedContext = getSelectedContext();

            //login panel handles the context
            final ServerContext context = super.completeSignIn(selectedContext);

            //prepare for import
            final Project project = getParentModel().getProject();

            doImport(project, context, getRepositoryName());
        }
    }

    private void doImport(final Project project, final ServerContext context, final String repositoryName) {
        new Task.Backgroundable(project, TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_IMPORTING_PROJECT), true, PerformInBackgroundOption.DEAF) {
            @Override
            public void run(@NotNull final ProgressIndicator indicator) {
                // Local context can change if the creation of the repo succeeds
                ServerContext localContext = context;
                String remoteUrlForDisplay = "";

                try {
                    if (!projectSupportsGitRepos(project, context, indicator)) {
                        logger.error("doImport: the team project {} on collection {} , " +
                                        "server {} does not support Git repositories or is not a hybrid project",
                                localContext.getTeamProjectReference().getName(),
                                localContext.getTeamProjectCollectionReference().getName(), localContext.getUri());
                        return;
                    }

                    final GitRepository repo = getRepositoryForProject(project);
                    final VirtualFile rootVirtualFile = repo != null ? repo.getRoot() : project.getBaseDir();

                    final GitRepository localRepository = repo != null ? repo :
                            setupGitRepositoryForProject(project, rootVirtualFile, localContext, indicator);
                    if (localRepository == null) {
                        logger.error("doImport: current project {} is not in a Git repository", project.getName());
                        return;
                    }

                    if (!doFirstCommitIfRequired(project, localRepository, rootVirtualFile, localContext, indicator)) {
                        logger.error("doImport: failed to do first commit on the local repository at: {}", localRepository.getRoot().getUrl());
                        return;
                    }

                    final com.microsoft.alm.sourcecontrol.webapi.model.GitRepository remoteRepository =
                            createRemoteGitRepo(project, context, localContext, indicator);
                    if (remoteRepository != null) {
                        //remote repo creation succeeded, save active context with the repository information
                        localContext = new ServerContextBuilder(localContext).uri(remoteRepository.getRemoteUrl()).repository(remoteRepository).build();
                        ServerContextManager.getInstance().add(localContext);
                    } else {
                        logger.error("doImport: failed to create remote repository with name: {} on server: {}, collection: {}",
                                repositoryName, localContext.getUri(), localContext.getTeamProjectCollectionReference().getName());
                        return;
                    }

                    if (!setupRemoteOnLocalRepo(project, localRepository, remoteRepository, localContext, indicator)) {
                        logger.error("doImport: failed to setup remote origin on local repository at: {} to point to remote repository: {}",
                                localRepository.getRoot().getUrl(), remoteRepository.getRemoteUrl());
                        return;
                    }

                    if (!pushChangesToRemoteRepo(project, localRepository, remoteRepository, localContext, indicator)) {
                        logger.error("doImport: failed to push changes to remote repository: {}", remoteRepository.getRemoteUrl());
                        return;
                    }

                    //all steps completed successfully
                    remoteUrlForDisplay = remoteRepository.getRemoteUrl();

                } catch (Throwable unexpectedError) {
                    remoteUrlForDisplay = "";
                    logger.error("doImport: Unexpected error during import");
                    logger.warn("doImport", unexpectedError);
                    notifyImportError(project, TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_ERRORS_UNEXPECTED, unexpectedError.getLocalizedMessage()),
                            TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_FAILED), localContext);

                } finally {
                    if (StringUtils.isNotEmpty(remoteUrlForDisplay)) {
                        // Notify the user that we are done and provide a link to the repo
                        VcsNotifier.getInstance(project).notifyImportantInfo(TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_SUCCEEDED),
                                TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_SUCCEEDED_MESSAGE, project.getName(), remoteUrlForDisplay, repositoryName),
                                NotificationListener.URL_OPENING_LISTENER);

                        // Add Telemetry for a successful import
                        TfsTelemetryHelper.getInstance().sendEvent(ACTION_NAME,
                                new TfsTelemetryHelper.PropertyMapBuilder()
                                        .currentOrActiveContext(localContext)
                                        .actionName(ACTION_NAME)
                                        .success(true)
                                        .build());
                    }
                }
            }

        }.queue();

    }

    private GitRepository getRepositoryForProject(final Project project) {
        //find if the project belongs to a local git repository
        final GitRepositoryManager repositoryManager = GitUtil.getRepositoryManager(project);
        final List<GitRepository> localRepositories = repositoryManager.getRepositories();
        final GitRepository repo;
        if (localRepositories.size() == 1) {
            //found one repository for the project
            repo = localRepositories.get(0);
        } else {
            // either none or multiple repositories were found
            // try to find repository for project root
            repo = repositoryManager.getRepositoryForFile(project.getBaseDir());
        }
        return repo;
    }

    private GitRepository setupGitRepositoryForProject(final Project project, final VirtualFile rootVirtualFile,
                                                       final ServerContext localContext, final ProgressIndicator indicator) {
        //project is not in a local git repository, create one
        indicator.setText(TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_GIT_INIT, project.getName()));
        final GitLineHandler hInit = new GitLineHandler(project, rootVirtualFile, GitCommand.INIT);
        GitHandlerUtil.runInCurrentThread(hInit, null, true, TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_GIT_INIT, project.getName()));
        if (!hInit.errors().isEmpty()) {
            //git init failed
            final String error = hInit.errors().get(0).getMessage();
            logger.error("setupGitRepositoryForProject: git init failed on project: {} at root: {} with error: {}",
                    project.getName(), rootVirtualFile.getUrl(), error);
            notifyImportError(project,
                    TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_GIT_INIT_ERROR, project.getName(), error),
                    ACTION_NAME, localContext);
            return null;
        }
        GitInit.refreshAndConfigureVcsMappings(project, rootVirtualFile, rootVirtualFile.getPath());
        final GitRepositoryManager repositoryManager = GitUtil.getRepositoryManager(project);
        return repositoryManager.getRepositoryForRoot(rootVirtualFile);
    }

    private boolean projectSupportsGitRepos(final Project project, final ServerContext localContext,
                                            final ProgressIndicator indicator) {
        if (localContext.getType() != ServerContext.Type.TFS) {
            return true; //VSO - team services supports hybrid projects, so git and tfvc are both supported
        }

        //TFS on premise server
        final CoreHttpClient client = new CoreHttpClient(localContext.getClient(), localContext.getCollectionURI());
        final TeamProject tp = client.getProject(localContext.getTeamProjectReference().getId().toString(), true);
        HashMap<String, HashMap<String, String>> capabilities = tp.getCapabilities();

        //TODO: make the containsKey and get case insensitive
        if (capabilities != null && capabilities.containsKey(PROJECT_CAPABILITY_VC)) {
            final HashMap<String, String> vcCapabilities = capabilities.get(PROJECT_CAPABILITY_VC);
            if (vcCapabilities != null && vcCapabilities.containsKey(PROJECT_CAPABILITY_VC_TYPE)) {
                final String sourceControlType = vcCapabilities.get(PROJECT_CAPABILITY_VC_TYPE);
                if (StringUtils.equalsIgnoreCase(sourceControlType, PROJECT_CAPABILITY_VC_GIT)) {
                    //sourceControlType=git, so git is supported
                    return true;
                }
            }
        }

        // Capability sourceControlType=TFvc, we are not sure if hybrid projects are supported or not on this server
        // Hybrid projects were introduced in TFS 2015 Update1
        // They are not officially supported by TFS 2015 RTM, creating repo will succeed but web access does not show the git repos
        // warn user to verify their project is a hybrid project
        final List<String> proceed = new ArrayList<String>();
        IdeaHelper.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                final boolean response = IdeaHelper.showConfirmationDialog(project,
                        TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_TEAM_PROJECT_GIT_SUPPORT, tp.getName()),
                        TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_DIALOG_TITLE), Icons.VSLogo,
                        TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_PROCEED),
                        TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_CANCEL)
                );

                if (response) {
                    proceed.add("true");
                }

            }
        }, true, indicator.getModalityState());

        if (proceed.size() == 0) {
            //user chose to cancel import
            logger.warn("projectSupportsGitRepos: User chose to cancel import into team project: {}",
                    localContext.getTeamProjectReference().getName());
            notifyImportError(project,
                    TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_CANCELED),
                    TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_IMPORT),
                    localContext);
            return false;
        }

        return true;
    }

    private boolean doFirstCommitIfRequired(final Project project, final GitRepository localRepository,
                                            final VirtualFile rootVirtualFile, final ServerContext localContext,
                                            final ProgressIndicator indicator) {
        //Do first commit if there are no commits in the repository
        if (localRepository.isFresh()) {
            try {
                final ChangeListManager changeListManager = ChangeListManager.getInstance(project);
                final ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
                final List<VirtualFile> trackedFiles = changeListManager.getAffectedFiles();
                final Collection<VirtualFile> untrackedFiles = ContainerUtil.filter(localRepository.getUntrackedFilesHolder().retrieveUntrackedFiles(),
                        new Condition<VirtualFile>() {
                            @Override
                            public boolean value(VirtualFile file) {
                                return !changeListManager.isIgnoredFile(file) && !vcsManager.isIgnored(file);
                            }
                        });
                trackedFiles.removeAll(untrackedFiles);

                final List<VirtualFile> allFiles = new ArrayList<VirtualFile>();
                allFiles.addAll(trackedFiles);
                allFiles.addAll(untrackedFiles);

                final List<VirtualFile> filesToCommit = new ArrayList<VirtualFile>();
                IdeaHelper.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        final SelectFilesDialog dialog = SelectFilesDialog.init(project,
                                allFiles,
                                TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_SELECT_FILES),
                                VcsShowConfirmationOption.STATIC_SHOW_CONFIRMATION,
                                true,
                                false,
                                false);
                        dialog.setTitle(TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_SELECT_FILES_DIALOG_TITLE));
                        DialogManager.show(dialog);
                        if (dialog.isOK()) {
                            //add files only if user clicked OK on the SelectFilesDialog
                            filesToCommit.addAll(dialog.getSelectedFiles());
                        }
                    }
                }, true, indicator.getModalityState());

                indicator.setText(TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_ADDING_FILES, project.getName()));
                GitFileUtils.addFiles(project, rootVirtualFile, filesToCommit);
                if (filesToCommit.size() > 0) {
                    final GitSimpleHandler hCommit = new GitSimpleHandler(project, rootVirtualFile, GitCommand.COMMIT);
                    hCommit.addParameters("-m", TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_ADDING_FILES, project.getName()));
                    GitHandlerUtil.runInCurrentThread(hCommit, null, true, TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_ADDING_FILES, project.getName()));
                    if (hCommit.getExitCode() != 0) {
                        //unable to commit
                        logger.error("doFirstCommitIfRequired: git commit failed for project: {}, repoRoot: {} with error: {}",
                                project.getName(), rootVirtualFile.getUrl(), hCommit.getStderr());
                        notifyImportError(project,
                                TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_ADDING_FILES_ERROR, project.getName(), hCommit.getStderr()),
                                ACTION_NAME, localContext);
                        return false;
                    }
                    VfsUtil.markDirtyAndRefresh(false, true, false, ArrayUtil.toObjectArray(filesToCommit, VirtualFile.class));
                    VcsFileUtil.markFilesDirty(project, getFilePaths(filesToCommit));
                } else {
                    logger.error("doFirstCommitIfRequired: No files to do first commit in project: {}, repoRoot: {}",
                            project.getName(), rootVirtualFile.getUrl());
                    notifyImportError(project,
                            TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_NO_SELECTED_FILES),
                            ACTION_NAME, localContext);
                    return false;
                }

            } catch (VcsException ve) {
                logger.error("doFirstCommitIfRequired: VcsException occurred when trying to do a commit on project: {}, repoRoot: {}",
                        project.getName(), rootVirtualFile.getUrl());
                logger.warn("doFirstCommitIfRequired", ve);
                // Log the exact exception here
                TfsTelemetryHelper.getInstance().sendException(ve,
                        new TfsTelemetryHelper.PropertyMapBuilder()
                                .currentOrActiveContext(localContext)
                                .actionName(ACTION_NAME)
                                .success(false)
                                .build());

                notifyImportError(project,
                        TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_ADDING_FILES_ERROR, project.getName(), ve.getMessage()),
                        ACTION_NAME, localContext);
                return false;
            }
        }
        return true;
    }

    private com.microsoft.alm.sourcecontrol.webapi.model.GitRepository createRemoteGitRepo(final Project project,
                                                                                           final ServerContext context, final ServerContext localContext, final ProgressIndicator indicator) {
        //create remote repository
        indicator.setText(TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_CREATING_REMOTE_REPO));
        final GitHttpClient gitClient = context.getGitHttpClient();
        final String collectionUrl = gitClient.getBaseUrl().toString();
        final com.microsoft.alm.sourcecontrol.webapi.model.GitRepository gitRepoToCreate =
                new com.microsoft.alm.sourcecontrol.webapi.model.GitRepository();
        gitRepoToCreate.setName(repositoryName);
        gitRepoToCreate.setProjectReference(localContext.getTeamProjectReference());
        com.microsoft.alm.sourcecontrol.webapi.model.GitRepository remoteRepository = null;
        Throwable t = null;
        try {
            remoteRepository = gitClient.createRepository(gitRepoToCreate, context.getTeamProjectReference().getId());
            t = null;
        } catch (VssServiceException vssEx) {
            t = vssEx;
        } catch (Throwable otherEx) {
            //handle any unexpected server exceptions as well to avoid crashing the plugin
            t = otherEx;
        } finally {
            if (t != null) {
                logger.error("doImport: Failed to create remote git repository name: {} collection: {}", repositoryName, collectionUrl);
                logger.warn("doImport", t);
                final String errorMessage;
                if (t.getMessage().contains("Microsoft.TeamFoundation.Git.Server.GitRepositoryNameAlreadyExists")) {
                    //The REST SDK asserts for exceptions that are not handled, there could be a very large number of server exceptions to manually add code for
                    //Handling it here since we are not decided on what to do with exceptions on the REST SDK
                    errorMessage = TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_CREATING_REMOTE_REPO_ALREADY_EXISTS_ERROR,
                            repositoryName, localContext.getTeamProjectURI().toString());
                } else if (t.getMessage().contains("Microsoft.TeamFoundation.Git.Server.InvalidGitRepositoryNameException")) {
                    //name is not valid
                    errorMessage = TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_CREATING_REMOTE_REPO_INVALID_NAME_ERROR,
                            repositoryName);
                } else if (t.getMessage().contains("Microsoft.TeamFoundation.Git.Server.GitNeedsPermissionException")) {
                    // Git permission issue importing
                    errorMessage = TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_CREATING_REMOTE_REPO_PERMISSION_ERROR,
                            repositoryName, localContext.getTeamProjectURI().toString());
                } else {
                    errorMessage = TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_CREATING_REMOTE_REPO_UNEXPECTED_ERROR,
                            repositoryName, localContext.getTeamProjectURI().toString());
                }
                notifyImportError(project, errorMessage, ACTION_NAME, localContext);
            }

            if (remoteRepository == null) {
                //We shouldn't get here if it is null, but logging just to be safe
                logger.error("doImport: remoteRepository is null after trying to remote git repository name: {} collection: {}", repositoryName, collectionUrl);
            }
        }
        return remoteRepository;
    }

    private boolean setupRemoteOnLocalRepo(final Project project, final GitRepository localRepository,
                                           final com.microsoft.alm.sourcecontrol.webapi.model.GitRepository remoteRepository,
                                           final ServerContext localContext, final ProgressIndicator indicator) {
        //get remotes on local repository
        indicator.setText(TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_GIT_REMOTE));
        final Collection<GitRemote> gitRemotes = localRepository.getRemotes();
        final List<String> remoteParams = new ArrayList<String>();

        if (!gitRemotes.isEmpty()) {
            for (GitRemote remote : gitRemotes) {
                if (StringUtils.equalsIgnoreCase(remote.getName(), REMOTE_ORIGIN)) {
                    //remote named origin exits, ask user if they want to overwrite it and proceed or cancel
                    IdeaHelper.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            final boolean replaceOrigin = IdeaHelper.showConfirmationDialog(project,
                                    TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_ORIGIN_EXISTS),
                                    TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_DIALOG_TITLE),
                                    Icons.VSLogoSmall,
                                    TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_UPDATE_ORIGIN),
                                    TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_CANCEL));
                            if (replaceOrigin) {
                                remoteParams.add("set-url");
                            }
                        }
                    }, true, indicator.getModalityState());
                    if (remoteParams.size() == 0) {
                        //user chose to cancel import
                        logger.warn("setupRemoteOnLocalRepo: User chose to cancel import for project: {}, local repo: {}",
                                project.getName(), localRepository.getGitDir().getUrl());
                        notifyImportError(project,
                                TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_CANCELED),
                                TfPluginBundle.message(TfPluginBundle.KEY_ACTIONS_IMPORT),
                                localContext);
                        return false;
                    }
                    break;
                }
            }
        }

        final String remoteGitUrl = UrlHelper.getCmdLineFriendlyUrl(remoteRepository.getRemoteUrl());
        //update remotes on local repository
        final GitSimpleHandler hRemote = new GitSimpleHandler(project, localRepository.getRoot(), GitCommand.REMOTE);
        hRemote.setSilent(true);
        if (remoteParams.size() == 1) {
            hRemote.addParameters(remoteParams.get(0), REMOTE_ORIGIN, remoteGitUrl);
        } else {
            hRemote.addParameters("add", REMOTE_ORIGIN, remoteGitUrl);
        }

        GitHandlerUtil.runInCurrentThread(hRemote, null, true, TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_GIT_REMOTE));
        if (hRemote.getExitCode() != 0) {
            logger.error("setupRemoteOnLocalRepo: git remote failed for project: {}, local repo: {}, error: {}, output: {}",
                    project.getName(), localRepository.getRoot().getUrl(), hRemote.getStderr(), hRemote.getStdout());
            notifyImportError(project,
                    TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_GIT_REMOTE_ERROR, remoteGitUrl, hRemote.getStderr()),
                    ACTION_NAME, localContext);
            return false;
        }
        return true;
    }

    private boolean pushChangesToRemoteRepo(final Project project, final GitRepository localRepository,
                                            final com.microsoft.alm.sourcecontrol.webapi.model.GitRepository remoteRepository,
                                            final ServerContext localContext, final ProgressIndicator indicator) {
        localRepository.update();
        final String remoteGitUrl = UrlHelper.getCmdLineFriendlyUrl(remoteRepository.getRemoteUrl());

        //push all branches in local Git repo to remote
        indicator.setText(TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_GIT_PUSH));
        final Git git = ServiceManager.getService(Git.class);
        final GitCommandResult result = git.push(localRepository, REMOTE_ORIGIN, remoteGitUrl, "*", true);
        if (!result.success()) {
            logger.error("pushChangesToRemoteRepo: push to remote: {} failed with error: {}, outuput: {}",
                    remoteGitUrl, result.getErrorOutputAsJoinedString(), result.getOutputAsJoinedString());
            notifyImportError(project,
                    result.getErrorOutputAsJoinedString(),
                    ACTION_NAME, localContext);
            return false;
        }

        return true;
    }

    private List<FilePath> getFilePaths(final List<VirtualFile> virtualFiles) {
        assert virtualFiles != null;
        final List<FilePath> filePaths = new ArrayList<FilePath>(virtualFiles.size());
        for (VirtualFile vf : virtualFiles) {
            filePaths.add(VcsUtil.getFilePath(vf));
        }

        return filePaths;
    }

    private void notifyImportError(final Project project, final String message, final String action, ServerContext context) {
        // Add Telemetry for a failed import
        TfsTelemetryHelper.getInstance().sendEvent(action,
                new TfsTelemetryHelper.PropertyMapBuilder()
                        .currentOrActiveContext(context)
                        .actionName(action)
                        .success(false)
                        .message(message)
                        .build());

        VcsNotifier.getInstance(project).notifyError(TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_FAILED), message, NotificationListener.URL_OPENING_LISTENER);
    }

    @Override
    public void appendContexts(List<ServerContext> serverContexts) {
        teamProjectTableModel.addServerContexts(serverContexts);
    }

    /**
     * This method is provided to allow the derived classes an easy way to add to the list of repositories.
     */
    protected void addContext(final ServerContext serverContext) {
        teamProjectTableModel.addServerContexts(Collections.singletonList(serverContext));
    }


    @Override
    public ServerContextTableModel getTableModel() {
        return teamProjectTableModel;
    }

    @Override
    public ListSelectionModel getTableSelectionModel() {
        return teamProjectTableModel.getSelectionModel();
    }

    @Override
    public ModelValidationInfo validate() {
        final ModelValidationInfo result = super.validate();

        if (result == ModelValidationInfo.NO_ERRORS) {
            if (getSelectedContext() == null) {
                return ModelValidationInfo.createWithResource(PROP_PROJECT_TABLE,
                        TfPluginBundle.KEY_IMPORT_DIALOG_ERRORS_PROJECT_NOT_SELECTED);
            }

            final String directoryName = getRepositoryName();
            if (directoryName == null || directoryName.isEmpty()) {
                return ModelValidationInfo.createWithResource(PROP_REPO_NAME,
                        TfPluginBundle.KEY_IMPORT_DIALOG_ERRORS_REPO_NAME_EMPTY);
            }
        } else {
            return result;
        }

        return ModelValidationInfo.NO_ERRORS;
    }

    /**
     * This method is provided to allow the derived classes an easy way to clear the list of repositories.
     */
    @Override
    public void clearContexts() {
        teamProjectTableModel.clearRows();
    }

    /**
     * This method is provided to allow the derived classes an easy way to get the selected team project instance.
     */
    protected ServerContext getSelectedContext() {
        return teamProjectTableModel.getSelectedContext();
    }

    @Override
    public void dispose() {
        teamProjectProvider.terminateActiveOperation();
    }
}
