// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.vcsimport;

import com.intellij.notification.NotificationListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.PerformInBackgroundOption;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.VcsNotifier;
import com.intellij.openapi.vcs.VcsShowConfirmationOption;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ui.SelectFilesDialog;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.vcsUtil.VcsFileUtil;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.common.LoginPageModelImpl;
import com.microsoft.alm.plugin.idea.ui.common.ModelValidationInfo;
import com.microsoft.alm.plugin.idea.ui.common.ServerContextLookupListener;
import com.microsoft.alm.plugin.idea.ui.common.ServerContextLookupPageModel;
import com.microsoft.alm.plugin.idea.ui.common.ServerContextTableModel;
import com.microsoft.alm.plugin.telemetry.TfsTelemetryHelper;
import com.microsoft.teamfoundation.sourcecontrol.webapi.GitHttpClient;
import com.microsoft.vss.client.core.model.VssServiceException;
import git4idea.DialogManager;
import git4idea.GitLocalBranch;
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
import javax.swing.table.TableModel;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
        setConnectionStatus(false);
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
        if(this.importEnabled != importEnabled) {
            this.importEnabled = importEnabled;
            getParentModel().updateImportEnabled();
        }
    }

    protected  void setConnectionStatus(final boolean connected) {
        setConnected(connected);
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
                final String action = "import";
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
                final VirtualFile rootVirtualFile = repo != null ? repo.getRoot() : project.getBaseDir();

                final GitRepository localRepository;
                if (repo == null) {
                    //project is not in a local git repository, create one
                    indicator.setText(TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_GIT_INIT, project.getName()));
                    final GitLineHandler hInit = new GitLineHandler(project, rootVirtualFile, GitCommand.INIT);
                    GitHandlerUtil.runInCurrentThread(hInit, null, true, TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_GIT_INIT, project.getName()));
                    if (!hInit.errors().isEmpty()) {
                        //git init failed
                        notifyImportError(project,
                                TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_GIT_INIT_ERROR, project.getName(), hInit.errors().get(0).getMessage()),
                                action, context);
                        return;
                    }
                    GitInit.refreshAndConfigureVcsMappings(project, rootVirtualFile, rootVirtualFile.getPath());
                    localRepository = repositoryManager.getRepositoryForRoot(rootVirtualFile);
                } else {
                    localRepository = repo;
                }

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

                        final Collection<VirtualFile> filesToCommit = new ArrayList<VirtualFile>();
                        ApplicationManager.getApplication().invokeAndWait(new Runnable() {
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
                                if(dialog.isOK()) {
                                    //add files only if user clicked OK on the SelectFilesDialog
                                    filesToCommit.addAll(dialog.getSelectedFiles());
                                }
                            }
                        }, indicator.getModalityState());


                        indicator.setText(TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_ADDING_FILES, project.getName()));
                        GitFileUtils.addFiles(project, rootVirtualFile, filesToCommit);
                        if (filesToCommit.size() > 0) {
                            GitSimpleHandler hCommit = new GitSimpleHandler(project, rootVirtualFile, GitCommand.COMMIT);
                            hCommit.addParameters("-m", TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_ADDING_FILES, project.getName()));
                            GitHandlerUtil.runInCurrentThread(hCommit, null, true, TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_ADDING_FILES, project.getName()));
                            if (hCommit.getExitCode() != 0) {
                                //unable to commit
                                notifyImportError(project,
                                        TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_ADDING_FILES_ERROR, project.getName(), hCommit.getStderr()),
                                        action, context);
                                return;
                            }
                            VcsFileUtil.refreshFiles(project, filesToCommit);
                        } else {
                            notifyImportError(project,
                                    TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_NO_SELECTED_FILES),
                                    action, context);
                            return;
                        }

                    } catch (VcsException ve) {
                        // Log the exact exception here
                        TfsTelemetryHelper.getInstance().sendException(ve,
                                new TfsTelemetryHelper.PropertyMapBuilder()
                                        .currentOrActiveContext(context)
                                        .actionName(action)
                                        .success(false)
                                        .build());

                        notifyImportError(project,
                                TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_ADDING_FILES_ERROR, project.getName(), ve.getMessage()),
                                action, context);
                        return;
                    }
                }

                //create remote repository
                indicator.setText(TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_CREATING_REMOTE_REPO));
                final URI collectionURI = URI.create(context.getUri().toString() + "/" + context.getTeamProjectCollectionReference().getName());
                final GitHttpClient gitClient = new GitHttpClient(context.getClient(), collectionURI);
                final com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitRepository gitRepoToCreate =
                        new com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitRepository();
                gitRepoToCreate.setName(repositoryName);
                gitRepoToCreate.setProjectReference(context.getTeamProjectReference());
                com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitRepository remoteRepository = null;
                Throwable t = null;
                try {
                    remoteRepository = gitClient.createRepository(gitRepoToCreate, context.getTeamProjectReference().getId());

                    //remote repo creation succeeded, save active context with the repository information
                    context.setGitRepository(remoteRepository);
                    ServerContextManager.getInstance().setActiveContext(context);

                    t = null;
                } catch (VssServiceException vssEx) {
                    t = vssEx;
                } catch(Throwable otherEx) {
                    //handle any unexpected server exceptions as well to avoid crashing the plugin
                    t = otherEx;
                } finally {
                    if(t != null) {
                        logger.error("doImport: Failed to create remote git repository name: {} collection: {}", repositoryName, collectionURI.toString());
                        logger.warn("doImport", t);
                        final String errorMessage;
                        final String teamProjectUrl = collectionURI.toASCIIString() + "/" + context.getTeamProjectReference().getName(); //TODO: how can we reliably compute these URLs
                        if(t.getMessage().contains("Microsoft.TeamFoundation.Git.Server.GitRepositoryNameAlreadyExists")) {
                            //The REST SDK asserts for exceptions that are not handled, there could be a very large number of server exceptions to manually add code for
                            //Handling it here since we are not decided on what to do with exceptions on the REST SDK
                            errorMessage = TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_CREATING_REMOTE_REPO_ALREADY_EXISTS_ERROR,
                                    repositoryName,
                                    teamProjectUrl);
                        } else {
                            errorMessage = TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_CREATING_REMOTE_REPO_UNEXPECTED_ERROR,
                                    repositoryName,
                                    teamProjectUrl);
                        }
                        notifyImportError(project, errorMessage, action, context);
                        return;
                    }
                    if(remoteRepository == null) {
                        //We shouldn't get here if it is null, but logging just to be safe
                        logger.error("doImport: remoteRepository is null after trying to remote git repository name: {} collection: {}", repositoryName, collectionURI.toString());
                        return;
                    }
                }

                //get remotes on local repository
                indicator.setText(TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_GIT_REMOTE));
                final Collection<GitRemote> gitRemotes = localRepository.getRemotes();
                final String remoteName;
                if (gitRemotes.isEmpty()) {
                    remoteName = "origin";
                } else {
                    //check the remotes
                    int index = 0;
                    for (final GitRemote remote : gitRemotes) {
                        if (remote.getName().contains(context.getUri().getHost())) {
                            index = index + 1;
                        }
                    }
                    if (index == 0) {
                        remoteName = context.getUri().getHost();
                    } else {
                        remoteName = context.getUri().getHost() + "_" + index;
                    }
                }

                final String remoteGitUrl = remoteRepository.getRemoteUrl().replace(" ", "%20");
                //update remotes on local repository
                final GitSimpleHandler hRemote = new GitSimpleHandler(project, localRepository.getRoot(), GitCommand.REMOTE);
                hRemote.setSilent(true);
                hRemote.addParameters("add", remoteName, remoteGitUrl);
                GitHandlerUtil.runInCurrentThread(hRemote, null, true, TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_GIT_REMOTE));
                if (hRemote.getExitCode() != 0) {
                    notifyImportError(project,
                            TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_GIT_REMOTE_ERROR, remoteGitUrl, hRemote.getStderr()),
                            action, context);
                    return;
                }
                localRepository.update();

                //push current branch to remote
                indicator.setText(TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_GIT_PUSH));
                final Git git = ServiceManager.getService(Git.class);
                GitLocalBranch currentBranch = localRepository.getCurrentBranch();
                if (currentBranch != null) {
                    GitCommandResult result = git.push(localRepository, remoteName, remoteGitUrl, currentBranch.getName(), true);
                    if (!result.success()) {
                        notifyImportError(project,
                                result.getErrorOutputAsJoinedString(),
                                action, context);
                        return;
                    }
                }

                // Notify the user that we are done and provide a link to the repo
                VcsNotifier.getInstance(project).notifyImportantInfo(TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_SUCCEEDED),
                        TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_SUCCEEDED_MESSAGE, project.getName(), remoteRepository.getRemoteUrl(), repositoryName),
                        NotificationListener.URL_OPENING_LISTENER);

                // Add Telemetry for a successful import
                TfsTelemetryHelper.getInstance().sendEvent(action,
                        new TfsTelemetryHelper.PropertyMapBuilder()
                                .currentOrActiveContext(context)
                                .actionName(action)
                                .success(true)
                                .build());
            }
        }.queue();

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
    public TableModel getTableModel() {
        return teamProjectTableModel;
    }

    @Override
    public ListSelectionModel getTableSelectionModel() {
        return teamProjectTableModel.getSelectionModel();
    }

    @Override
    public ModelValidationInfo validate() {
        ModelValidationInfo result = super.validate();

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
     * This method is provided to allow the derived classes an easy way to get the selected teamProject index.
     */
    protected int getSelectedRowIndex() {
        return teamProjectTableModel.getSelectionModel().getMinSelectionIndex();
    }

    /**
     * This method is provided to allow the derived classes an easy way to get the selected team project instance.
     */
    protected ServerContext getSelectedContext() {
        return teamProjectTableModel.getServerContext(getSelectedRowIndex());
    }

    @Override
    public void dispose() {
        teamProjectProvider.terminateActiveOperation();
    }
}
