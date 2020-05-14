// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.resources;

import com.intellij.CommonBundle;
import com.intellij.reference.SoftReference;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.ref.Reference;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * This class allows static access to the plugins string resources.
 */
public class TfPluginBundle {
    private static final Logger logger = LoggerFactory.getLogger(TfPluginBundle.class);

    @NonNls
    public static final String BUNDLE_NAME = "com.microsoft.alm.plugin.idea.ui.tfplugin";

    private static Reference<ResourceBundle> thisBundle;

    private static ResourceBundle getBundle() {
        ResourceBundle bundle = com.intellij.reference.SoftReference.dereference(thisBundle);
        if (bundle == null) {
            try {
                bundle = ResourceBundle.getBundle(BUNDLE_NAME);
            } catch (MissingResourceException e) {
                logger.warn("Locale could not be found for resource bundle so defaulting to english", e);
                bundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.ENGLISH);
            }
            thisBundle = new SoftReference<ResourceBundle>(bundle);
        }
        return bundle;
    }

    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE_NAME) String key,
                                 @NotNull Object... params) {
        return CommonBundle.message(getBundle(), key, params);
    }

    // Plugin general
    @NonNls
    public static final String KEY_PLUGIN_AZURE_DEVOPS = "Plugin.AzureDevOps";
    @NonNls
    public static final String KEY_TF_GIT = "Providers.TfGitCheckoutProvider";
    @NonNls
    public static final String KEY_GIT_NOT_CONFIGURED = "Plugin.Error.GitExeNotConfigured";
    @NonNls
    public static final String KEY_TFVC = "Providers.TfvcCheckoutProvider";
    @NonNls
    public static final String KEY_TFVC_NOT_CONFIGURED = "Plugin.Error.TFNotConfigured";
    @NonNls
    public static final String KEY_TFVC_NOT_CONFIGURED_DIALOG_OPEN_SETTINGS = "Plugin.Error.TFNotConfiguredDialog.OpenSettings";
    @NonNls
    public static final String KEY_TFVC_NOT_CONFIGURED_DIALOG_CANCEL = "Plugin.Error.TFNotConfiguredDialog.Cancel";
    @NonNls
    public static final String KEY_TFVC_NOTIFICATIONS = "Plugin.TfvcNotifications";

    // Git
    @NonNls
    public static final String KEY_GIT_NOTIFICATION_REMOTE = "Git.Notification.Remote";
    @NonNls
    public static final String KEY_GIT_CONFIGURE_REMOTES = "Git.Action.ConfigureRemotes";

    // Login form
    @NonNls
    public static final String KEY_LOGIN_FORM_AUTHENTICATING_VSO = "LoginForm.VSO.Authenticating";
    @NonNls
    public static final String KEY_LOGIN_FORM_AUTHENTICATING_TFS = "LoginForm.TFS.Authenticating";
    @NonNls
    public static final String KEY_LOGIN_PAGE_ERRORS_VSO_SIGN_IN_FAILED = "LoginForm.Errors.VSO.SigninFailed";
    @NonNls
    public static final String KEY_LOGIN_PAGE_ERRORS_TFS_CONNECT_FAILED = "LoginForm.Errors.TFS.ConnectFailed";
    @NonNls
    public static final String KEY_LOGIN_FORM_ERRORS_NOT_CONNECTED = "LoginForm.Errors.NotConnected";
    @NonNls
    public static final String KEY_LOGIN_FORM_TFS_ERRORS_NO_SERVER_NAME = "LoginForm.TFS.Errors.NoServerName";
    @NonNls
    public static final String KEY_LOGIN_FORM_TFS_ERRORS_INVALID_SERVER_URL = "LoginForm.TFS.Errors.InvalidServerUrl";

    // Server Context Table
    @NonNls
    public static final String KEY_SERVER_CONTEXT_TABLE_ACCOUNT_COLUMN = "ServerContextTable.AccountColumn";
    @NonNls
    public static final String KEY_SERVER_CONTEXT_TABLE_COLLECTION_COLUMN = "ServerContextTable.CollectionColumn";
    @NonNls
    public static final String KEY_SERVER_CONTEXT_TABLE_PROJECT_COLUMN = "ServerContextTable.ProjectColumn";
    @NonNls
    public static final String KEY_SERVER_CONTEXT_TABLE_REPO_COLUMN = "ServerContextTable.RepoColumn";

    // Common
    @NonNls
    public static final String KEY_ERRORS_AUTH_NOT_SUCCESSFUL = "Errors.AuthNotSuccessful";
    @NonNls
    public static final String KEY_OPERATION_LOOKUP_CANCELED = "Operation.Lookup.Canceled";
    @NonNls
    public static final String KEY_OPERATION_LOOKUP_ERRORS = "Operation.Lookup.Errors";
    @NonNls
    public static final String KEY_VSO_LOOKUP_HELP_ENTER_URL = "VsoLookupHelp.EnterUrl";
    @NonNls
    public static final String KEY_VSO_LOOKUP_HELP_VIEW_ACCOUNTS = "VsoLookupHelp.ViewAccounts";
    @NonNls
    public static final String KEY_TITLE_TEAM_SERVICES_ERROR = "Title.TeamServices.Error";
    @NonNls
    public static final String KEY_MESSAGE_TEAM_SERVICES_UNEXPECTED_ERROR = "Message.TeamServices.Unexpected.Error";
    @NonNls
    public static final String KEY_ERRORS_NOT_TFS_REPO = "Errors.NotTfsRepo";
    @NonNls
    public static final String KEY_VSO_NO_PROFILE_ERROR_HELP = "VSO.NoProfileError.Help";
    @NonNls
    public static final String KEY_TFVC_NOTIFICATION_FILE_NAME_STARTS_WITH_DOLLAR = "Tfvc.Notification.FileNameStartsWithDollar";

    // Common TFVC
    @NonNls
    public static final String KEY_ERRORS_UNABLE_TO_DETERMINE_WORKSPACE = "Errors.UnableToDetermineWorkspace";
    @NonNls
    public static final String KEY_TFVC_ACTION_ADD_TO_TFIGNORE = "Tfvc.Action.AddToTfIgnore";
    @NonNls
    public static final String KEY_TFVC_IMPORT_WORKSPACE_TITLE = "Actions.Tfvc.ImportWorkspace.Title";
    @NonNls
    public static final String KEY_TFVC_REPOSITORY_IMPORT_ERROR = "Tfvc.RepositoryImportError";
    @NonNls
    public static final String KEY_TFVC_REPOSITORY_IMPORT_SUCCESS = "Tfvc.RepositoryImportSuccess";
    @NonNls
    public static final String KEY_TFVC_WORKSPACE_NOT_DETECTED = "Tfvc.WorkspaceNotDetected";

    // TFVC Checkout
    @NonNls
    public static final String KEY_TFVC_CHECKOUT_FAILED = "Tfvc.CheckoutFailed";
    @NonNls
    public static final String KEY_TFVC_CHECKOUT_FILES = "Tfvc.CheckoutFiles";
    @NonNls
    public static final String KEY_TFVC_CHECKOUT_FILES_FAILED = "Tfvc.CheckoutFilesFailed";

    // Checkout dialog ui and models
    @NonNls
    public static final String KEY_CHECKOUT_DIALOG_TFS_TAB = "CheckoutDialog.TfsTab";
    @NonNls
    public static final String KEY_CHECKOUT_DIALOG_VSO_TAB = "CheckoutDialog.VsoTab";
    @NonNls
    public static final String KEY_CHECKOUT_DIALOG_CLONE_BUTTON = "CheckoutDialog.CloneButton";
    @NonNls
    public static final String KEY_CHECKOUT_DIALOG_CREATE_WORKSPACE_BUTTON = "CheckoutDialog.CreateWorkspaceButton";
    @NonNls
    public static final String KEY_CHECKOUT_DIALOG_TITLE = "CheckoutDialog.Title";
    @NonNls
    public static final String KEY_CHECKOUT_DIALOG_FILTER_HINT = "CheckoutDialog.FilterHint";
    @NonNls
    public static final String KEY_CHECKOUT_DIALOG_PARENT_FOLDER_DIALOG_TITLE = "CheckoutDialog.ParentFolder.DialogTitle";
    @NonNls
    public static final String KEY_CHECKOUT_DIALOG_ERRORS_PARENT_DIR_EMPTY = "CheckoutDialog.Errors.ParentDirEmpty";
    @NonNls
    public static final String KEY_CHECKOUT_DIALOG_ERRORS_PARENT_DIR_NOT_FOUND = "CheckoutDialog.Errors.ParentDirNotFound";
    @NonNls
    public static final String KEY_CHECKOUT_DIALOG_ERRORS_REPO_NOT_SELECTED = "CheckoutDialog.Errors.RepoNotSelected";
    @NonNls
    public static final String KEY_CHECKOUT_DIALOG_ERRORS_DIR_NAME_EMPTY = "CheckoutDialog.Errors.DirNameEmpty";
    @NonNls
    public static final String KEY_CHECKOUT_DIALOG_ERRORS_DESTINATION_EXISTS = "CheckoutDialog.Errors.DestinationExists";
    @NonNls
    public static final String KEY_CHECKOUT_DIALOG_ERRORS_DIR_NAME_INVALID = "CheckoutDialog.Errors.DirNameInvalid";
    @NonNls
    public static final String KEY_CHECKOUT_DIALOG_TFVC_ADVANCED = "CheckoutDialog.Tfvc.Advanced";
    @NonNls
    public static final String KEY_CHECKOUT_DIALOG_TFVC_SERVER_WORKSPACE = "CheckoutDialog.Tfvc.ServerWorkspace";
    @NonNls
    public static final String KEY_CHECKOUT_ERRORS_UNEXPECTED = "Checkout.Errors.Unexpected";
    @NonNls
    public static final String KEY_CHECKOUT_ERRORS_AUTHENTICATION_FAILED_TITLE = "CheckoutDialog.Errors.AuthenticationFailed.Title";
    @NonNls
    public static final String KEY_CHECKOUT_TFVC_CREATING_WORKSPACE = "CheckoutDialog.Tfvc.CreatingWorkspace";
    @NonNls
    public static final String KEY_CHECKOUT_TFVC_WORKSPACE_COMMENT = "CheckoutDialog.Tfvc.WorkspaceComment";
    @NonNls
    public static final String KEY_CHECKOUT_TFVC_PROGRESS_CREATING = "CheckoutDialog.Tfvc.Progress.Creating";
    @NonNls
    public static final String KEY_CHECKOUT_TFVC_PROGRESS_ADD_ROOT = "CheckoutDialog.Tfvc.Progress.AddRoot";
    @NonNls
    public static final String KEY_CHECKOUT_TFVC_PROGRESS_CREATE_FOLDER = "CheckoutDialog.Tfvc.Progress.CreateFolder";
    @NonNls
    public static final String KEY_CHECKOUT_TFVC_PROGRESS_SYNC = "CheckoutDialog.Tfvc.Progress.Sync";
    @NonNls
    public static final String KEY_CHECKOUT_TFVC_FAILED_TITLE = "CheckoutDialog.Tfvc.Failed.Title";

    // Tool exceptions:
    @NonNls
    public static final String KEY_TOOLEXCEPTION_TF_HOME_NOT_SET = "ToolException.TF.HomeNotSet";

    //User account panel
    @NonNls
    public static final String KEY_USER_ACCOUNT_PANEL_VSO_SERVER_NAME = "UserAccountPanel.VSO.ServerName";
    @NonNls
    public static final String KEY_USER_ACCOUNT_PANEL_SWITCH_SERVER = "UserAccountPanel.SwitchServer";
    @NonNls
    public static final String KEY_USER_ACCOUNT_PANEL_SIGN_OUT = "UserAccountPanel.SignOut";

    // Prompts
    @NonNls
    public static final String KEY_PROMPT_CREDENTIALS_TITLE = "Prompt.Credentials.Title";
    @NonNls
    public static final String KEY_PROMPT_CREDENTIALS_MESSAGE = "Prompt.Credentials.Message";

    // Feedback dialog
    @NonNls
    public static final String KEY_FEEDBACK_DIALOG_TITLE = "Feedback.Dialog.Title";
    @NonNls
    public static final String KEY_FEEDBACK_DIALOG_OK_FROWN = "Feedback.Dialog.OkButtonText.Frown";
    @NonNls
    public static final String KEY_FEEDBACK_DIALOG_OK_SMILE = "Feedback.Dialog.OkButtonText.Smile";
    @NonNls
    public static final String KEY_FEEDBACK_DIALOG_COMMENT_LABEL_FROWN = "Feedback.Dialog.CommentLabel.Frown";
    @NonNls
    public static final String KEY_FEEDBACK_NOTIFICATION = "Feedback.Notification";

    // Select work items dialog
    @NonNls
    public static final String KEY_WIT_SELECT_DIALOG_TITLE = "WitSelectDialog.Title";
    @NonNls
    public static final String KEY_WIT_SELECT_DIALOG_SELECT_BUTTON = "WitSelectDialog.SelectButton";
    @NonNls
    public static final String KEY_WIT_SELECT_DIALOG_ERRORS_WORK_ITEM_NOT_SELECTED = "WitSelectDialog.Errors.WorkItemNotSelected";
    @NonNls
    public static final String KEY_WIT_SELECT_DIALOG_FILTER_HINT_TEXT = "WitSelectDialog.FilterHintText";
    @NonNls
    public static final String KEY_WIT_SELECT_DIALOG_COMMIT_MESSAGE_FORMAT = "WitSelectDialog.CommitMessageFormat";
    @NonNls
    public static final String KEY_VIEW_MY_WORK_ITEMS = "WitSelectDialog.Help.ViewMyWorkItems";

    // Work item fields
    @NonNls
    public static final String KEY_WIT_FIELD_ASSIGNED_TO = "WitField.AssignedTo";
    @NonNls
    public static final String KEY_WIT_FIELD_ID = "WitField.ID";
    @NonNls
    public static final String KEY_WIT_FIELD_STATE = "WitField.State";
    @NonNls
    public static final String KEY_WIT_FIELD_TITLE = "WitField.Title";
    @NonNls
    public static final String KEY_WIT_FIELD_BRANCH = "WitField.Branch";
    @NonNls
    public static final String KEY_WIT_FIELD_WORK_ITEM_TYPE = "WitField.WorkItemType";

    // Work item association messages
    @NonNls
    public static final String KEY_WIT_ASSOCIATION_SUCCESSFUL_TITLE = "WitAssociation.Successful.Title";
    @NonNls
    public static final String KEY_WIT_ASSOCIATION_SUCCESSFUL_DESCRIPTION = "WitAssociation.Successful.Description";
    @NonNls
    public static final String KEY_WIT_ASSOCIATION_FAILED_TITLE = "WitAssociation.Failed.Title";
    @NonNls
    public static final String KEY_WIT_ASSOCIATION_FAILED_DESCRIPTION = "WitAssociation.Failed.Description";

    //Import dialog ui and models
    @NonNls
    public static final String KEY_IMPORT_DIALOG_TITLE = "ImportDialog.Title";
    @NonNls
    public static final String KEY_IMPORT_DIALOG_IMPORT_BUTTON = "ImportDialog.ImportButton";
    @NonNls
    public static final String KEY_IMPORT_DIALOG_TFS_TAB = "ImportDialog.TfsTab";
    @NonNls
    public static final String KEY_IMPORT_DIALOG_VSO_TAB = "ImportDialog.VsoTab";
    @NonNls
    public static final String KEY_IMPORT_DIALOG_FILTER_HINT = "ImportDialog.FilterHint";
    @NonNls
    public static final String KEY_IMPORT_DIALOG_ERRORS_PROJECT_NOT_SELECTED = "ImportDialog.Errors.ProjectNotSelected";
    @NonNls
    public static final String KEY_IMPORT_DIALOG_ERRORS_REPO_NAME_EMPTY = "ImportDialog.Errors.RepoNameEmpty";
    @NonNls
    public static final String KEY_IMPORT_DIALOG_VSO_SERVER_NAME = "ImportDialog.VSO.ServerName";
    @NonNls
    public static final String KEY_IMPORT_FAILED = "Import.Failed";
    @NonNls
    public static final String KEY_IMPORT_CANCELED = "Import.Canceled";
    @NonNls
    public static final String KEY_IMPORT_SUCCEEDED = "Import.Succeeded";
    @NonNls
    public static final String KEY_IMPORT_SUCCEEDED_MESSAGE = "Import.Succeeded.Message";
    @NonNls
    public static final String KEY_IMPORT_IMPORTING_PROJECT = "Import.ImportingProject";
    @NonNls
    public static final String KEY_IMPORT_GIT_INIT = "Import.GitInit";
    @NonNls
    public static final String KEY_IMPORT_GIT_INIT_ERROR = "Import.Errors.GitInit";
    @NonNls
    public static final String KEY_IMPORT_SELECT_FILES = "Import.SelectFiles";
    @NonNls
    public static final String KEY_IMPORT_SELECT_FILES_DIALOG_TITLE = "Import.SelectFilesDialog.Title";
    @NonNls
    public static final String KEY_IMPORT_ADDING_FILES = "Import.AddingFiles";
    @NonNls
    public static final String KEY_IMPORT_ADDING_FILES_ERROR = "Import.Errors.AddingFiles";
    @NonNls
    public static final String KEY_IMPORT_NO_SELECTED_FILES = "Import.Errors.NoSelectedFiles";
    @NonNls
    public static final String KEY_IMPORT_CREATING_REMOTE_REPO = "Import.CreatingRemoteRepo";
    @NonNls
    public static final String KEY_IMPORT_CREATING_REMOTE_REPO_UNEXPECTED_ERROR = "Import.Errors.CreatingRemoteRepo.UnexpectedError";
    @NonNls
    public static final String KEY_IMPORT_CREATING_REMOTE_REPO_ALREADY_EXISTS_ERROR = "Import.Errors.CreatingRemoteRepo.AlreadyExistsError";
    @NonNls
    public static final String KEY_IMPORT_CREATING_REMOTE_REPO_INVALID_NAME_ERROR = "Import.Errors.CreatingRemoteRepo.InvalidNameError";
    @NonNls
    public static final String KEY_IMPORT_CREATING_REMOTE_REPO_PERMISSION_ERROR = "Import.Errors.CreatingRemoteRepo.PermissionError";
    @NonNls
    public static final String KEY_IMPORT_GIT_REMOTE = "Import.GitRemote";
    @NonNls
    public static final String KEY_IMPORT_GIT_REMOTE_ERROR = "Import.Errors.GitRemote";
    @NonNls
    public static final String KEY_IMPORT_ORIGIN_EXISTS = "Import.OriginExists";
    @NonNls
    public static final String KEY_IMPORT_UPDATE_ORIGIN = "Import.UpdateOrigin";
    @NonNls
    public static final String KEY_IMPORT_CANCEL = "Import.Cancel";
    @NonNls
    public static final String KEY_IMPORT_PROCEED = "Import.Proceed";
    @NonNls
    public static final String KEY_IMPORT_TEAM_PROJECT_GIT_SUPPORT = "Import.TeamProject.GitSupport";
    @NonNls
    public static final String KEY_IMPORT_GIT_PUSH = "Import.GitPush";
    @NonNls
    public static final String KEY_IMPORT_ERRORS_UNEXPECTED = "Import.Errors.Unexpected";


    // CreatePullRequest dialog ui and models
    @NonNls
    public static final String KEY_CREATE_PR_DIALOG_CREATE_BUTTON = "CreatePullRequestDialog.CreateButton";
    @NonNls
    public static final String KEY_CREATE_PR_DIALOG_TITLE = "CreatePullRequestDialog.Title";
    @NonNls
    public static final String KEY_CREATE_PR_CREATED_TITLE = "CreatePullRequestDialog.Created.Title";
    @NonNls
    public static final String KEY_CREATE_PR_ALREADY_EXISTS_TITLE = "CreatePullRequestDialog.AlreadyExists.Title";
    @NonNls
    public static final String KEY_CREATE_PR_CREATED_MESSAGE = "CreatePullRequestDialog.Created.Message";
    @NonNls
    public static final String KEY_CREATE_PR_PUSH_TITLE = "CreatePullRequestDialog.Push.Title";
    @NonNls
    public static final String KEY_CREATE_PR_ERRORS_DIFF_FAILED_TITLE = "CreatePullRequestDialog.Errors.DiffFailed.Title";
    @NonNls
    public static final String KEY_CREATE_PR_ERRORS_DIFF_FAILED_MSG = "CreatePullRequestDialog.Errors.DiffFailed.Message";
    @NonNls
    public static final String KEY_CREATE_PR_ERRORS_CREATE_FAILED_TITLE = "CreatePullRequestDialog.Errors.CreateFailed.Title";
    @NonNls
    public static final String KEY_CREATE_PR_ERRORS_PUSH_FAILED_TITLE = "CreatePullRequestDialog.Errors.PushFailed.Title";
    @NonNls
    public static final String KEY_CREATE_PR_ERRORS_TITLE_EMPTY = "CreatePullRequestDialog.Errors.TitleEmpty";
    @NonNls
    public static final String KEY_CREATE_PR_ERRORS_TITLE_TOO_LONG = "CreatePullRequestDialog.Errors.TitleTooLong";
    @NonNls
    public static final String KEY_CREATE_PR_ERRORS_DESCRIPTION_EMPTY = "CreatePullRequestDialog.Errors.DescriptionEmpty";
    @NonNls
    public static final String KEY_CREATE_PR_ERRORS_DESCRIPTION_TOO_LONG = "CreatePullRequestDialog.Errors.DescriptionTooLong";
    @NonNls
    public static final String KEY_CREATE_PR_ERRORS_TARGET_NOT_SELECTED = "CreatePullRequestDialog.Errors.TargetNotSelected";
    @NonNls
    public static final String KEY_CREATE_PR_ERRORS_TARGET_IS_LOCAL_TRACKING = "CreatePullRequestDialog.Errors.TargetIsLocalTracking";
    @NonNls
    public static final String KEY_CREATE_PR_ERRORS_SOURCE_EMPTY = "CreatePullRequestDialog.Errors.SourceEmpty";
    @NonNls
    public static final String KEY_CREATE_PR_CHANGES_PANE_TITLE = "CreatePullRequestDialog.ChangesPane.Title";
    @NonNls
    public static final String KEY_CREATE_PR_COMMITS_PANE_TITLE = "CreatePullRequestDialog.CommitsPane.Title";
    @NonNls
    public static final String KEY_CREATE_PR_DEFAULT_TITLE = "CreatePullRequestDialog.Default.Title";
    @NonNls
    public static final String KEY_CREATE_PR_SANITY_CHECK_FAILED_WARNING_TITLE = "CreatePullRequestDialog.SanityCheckFailed.Title";
    @NonNls
    public static final String KEY_CREATE_PR_NO_VALID_TARGET_WARNING_MESSAGE = "CreatePullRequestDialog.NoValidTargetWarning.Message";

    //Vcs Pull Requests tab
    @NonNls
    public static final String KEY_VCS_PR_TITLE = "VcsPullRequestsTab.Title";
    @NonNls
    public static final String KEY_VCS_PR_REFRESH_TOOLTIP = "VcsPullRequestsTab.Refresh.Tooltip";
    @NonNls
    public static final String KEY_VCS_PR_REQUESTED_BY_ME = "VcsPullRequestsTab.RequestedByMe";
    @NonNls
    public static final String KEY_VCS_PR_ASSIGNED_TO_ME = "VcsPullRequestsTab.AssignedToMe";
    @NonNls
    public static final String KEY_VCS_PR_ABANDON = "VcsPullRequestsTab.Abandon";
    @NonNls
    public static final String KEY_VCS_PR_ABANDON_CONFIRMATION = "VcsPullRequestsTab.Abandon.Confirmation";
    @NonNls
    public static final String KEY_VCS_PR_ABANDON_SUCCEEDED = "VcsPullRequestsTab.Abandon.Succeeded";
    @NonNls
    public static final String KEY_VCS_PR_ABANDON_FAILED = "VcsPullRequestsTab.Abandon.Failed";
    @NonNls
    public static final String KEY_VCS_PR_ABANDON_FAILED_NO_SELECTION = "VcsPullRequestsTab.Abandon.Failed.NoSelection";
    @NonNls
    public static final String KEY_VCS_PR_MERGE_FAILED = "VcsPullRequestsTab.Merge.Failed";
    @NonNls
    public static final String KEY_VCS_PR_REVIEWER_NO_RESPONSE = "VcsPullRequestsTab.Reviewer.NoResponse";
    @NonNls
    public static final String KEY_VCS_PR_REVIEWER_WAITING = "VcsPullRequestsTab.Reviewer.Waiting";
    @NonNls
    public static final String KEY_VCS_PR_REVIEWER_APPROVED = "VcsPullRequestsTab.Reviewer.Approved";
    @NonNls
    public static final String KEY_VCS_PR_REVIEWER_APPROVED_SUGGESTIONS = "VcsPullRequestsTab.Reviewer.Approved.Suggestions";
    @NonNls
    public static final String KEY_VCS_PR_REVIEWER_REJECTED = "VcsPullRequestsTab.Reviewer.Rejected";
    @NonNls
    public static final String KEY_VCS_PR_SUMMARY = "VcsPullRequestsTab.PR.Summary";
    @NonNls
    public static final String KEY_VCS_PR_UNEXPECTED_ERRORS = "VcsPullRequestsTab.Unexpected.Errors";
    @NonNls
    public static final String KEY_VCS_PR_VIEW_DETAILS = "VcsPullRequestsTab.View.Details";
    @NonNls
    public static final String KEY_VCS_PR_VIEW_DETAILS_COUNT = "VcsPullRequestsTab.View.Details.Count";
    @NonNls
    public static final String KEY_VCS_PR_DATE_LESS_THAN_A_MINUTE_AGO = "VcsPullRequestsTab.Date.LessThanAMinuteAgo";
    @NonNls
    public static final String KEY_VCS_PR_DATE_ONE_MINUTE_AGO = "VcsPullRequestsTab.Date.OneMinuteAgo";
    @NonNls
    public static final String KEY_VCS_PR_DATE_MINUTES_AGO = "VcsPullRequestsTab.Date.MinutesAgo";
    @NonNls
    public static final String KEY_VCS_PR_DATE_ONE_HOUR_AGO = "VcsPullRequestsTab.Date.OneHourAgo";
    @NonNls
    public static final String KEY_VCS_PR_DATE_HOURS_AGO = "VcsPullRequestsTab.Date.HoursAgo";
    @NonNls
    public static final String KEY_VCS_PR_DATE_YESTERDAY = "VcsPullRequestsTab.Date.Yesterday";
    @NonNls
    public static final String KEY_VCS_PR_DATE_DAYS_AGO = "VcsPullRequestsTab.Date.DaysAgo";

    //Vcs Work Item tab
    @NonNls
    public static final String KEY_VCS_WIT_TITLE = "VcsWorkItemsTab.Title";
    @NonNls
    public static final String KEY_VCS_WIT_CREATE_WIT = "VcsWorkItemsTab.Create.WorkItem";
    @NonNls
    public static final String KEY_VCS_WIT_CREATE_BRANCH = "VcsWorkItemsTab.Create.Branch";
    @NonNls
    public static final String KEY_VCS_WIT_REFRESH_TOOLTIP = "VcsWorkItemsTab.Refresh.Tooltip";
    @NonNls
    public static final String KEY_VCS_WIT_UNEXPECTED_ERRORS = "VcsWorkItemsTab.Unexpected.Errors";
    @NonNls
    public static final String KEY_VCS_WIT_QUERY_TITLE = "VcsWorkItemsTab.Query.Title";
    @NonNls
    public static final String KEY_VCS_WIT_QUERY_SEPARATOR_MY_QUERIES = "VcsWorkItemsTab.Query.Separator.MyQueries";
    @NonNls
    public static final String KEY_VCS_WIT_QUERY_DEFAULT_QUERY = "VcsWorkItemsTab.Query.DefaultQuery";

    //Branch Creation Dialog
    @NonNls
    public static final String KEY_CREATE_BRANCH_DIALOG_CREATE_BUTTON = "CreateBranchDialog.CreateButton";
    @NonNls
    public static final String KEY_CREATE_BRANCH_DIALOG_TITLE = "CreateBranchDialog.Title";
    @NonNls
    public static final String KEY_CREATE_BRANCH_DIALOG_SUCCESSFUL_TITLE = "CreateBranchDialog.Successful.Title";
    @NonNls
    public static final String KEY_CREATE_BRANCH_DIALOG_SUCCESSFUL_DESCRIPTION = "CreateBranchDialog.Successful.Description";
    @NonNls
    public static final String KEY_CREATE_BRANCH_DIALOG_FAILED_TITLE = "CreateBranchDialog.Failed.Title";
    @NonNls
    public static final String KEY_CREATE_BRANCH_DIALOG_ERRORS_BRANCH_NAME_EMPTY = "CreateBranchDialog.Errors.BranchNameEmpty";
    @NonNls
    public static final String KEY_CREATE_BRANCH_DIALOG_ERRORS_BRANCH_NAME_EXISTS = "CreateBranchDialog.Errors.BranchNameExists";
    @NonNls
    public static final String KEY_CREATE_BRANCH_ERRORS_AUTHENTICATION_FAILED_TITLE = "CreateBranchDialog.Errors.AuthenticationFailed.Title";
    @NonNls
    public static final String KEY_CREATE_BRANCH_ERRORS_UNEXPECTED_SERVER_ERROR = "CreateBranchDialog.Errors.UnexpectedServerError";
    @NonNls
    public static final String KEY_CREATE_BRANCH_ERRORS_BRANCH_CREATE_FAILED = "CreateBranchDialog.Errors.BranchCreateFailed";

    //common toolbar
    @NonNls
    public static final String KEY_TOOLBAR_FILTER_TITLE = "Toolbar.Filter.Title";
    @NonNls
    public static final String KEY_VCS_NOT_AUTHENTICATED = "VcsTab.NoAuthenticated";
    @NonNls
    public static final String KEY_VCS_SIGN_IN = "VcsTab.SignIn";
    @NonNls
    public static final String KEY_VCS_OPEN_IN_BROWSER = "VcsTab.OpenInBrowser";
    @NonNls
    public static final String KEY_VCS_LOADING = "VcsTab.Loading";
    @NonNls
    public static final String KEY_VCS_LAST_REFRESHED_AT = "VcsTab.LastRefreshedAt";
    @NonNls
    public static final String KEY_VCS_LOADING_ERRORS = "VcsTab.LoadingErrors";
    @NonNls
    public static final String KEY_VCS_AUTO_REFRESH = "VcsTab.AutoRefresh";

    //status bar
    @NonNls
    public static final String KEY_STATUSBAR_BUILD_ERROR = "StatusBar.Build.Error";
    @NonNls
    public static final String KEY_STATUSBAR_BUILD_ERROR_AUTH = "StatusBar.Build.Error.Auth";
    @NonNls
    public static final String KEY_STATUSBAR_BUILD_UNKNOWN_STATUS = "StatusBar.Build.UnknownStatus";
    @NonNls
    public static final String KEY_STATUSBAR_BUILD_NO_BUILDS_FOUND = "StatusBar.Build.NoBuildsFound";
    @NonNls
    public static final String KEY_STATUSBAR_BUILD_SUCCEEDED = "StatusBar.Build.BuildSucceeded";
    @NonNls
    public static final String KEY_STATUSBAR_BUILD_FAILED = "StatusBar.Build.BuildFailed";
    @NonNls
    public static final String KEY_STATUSBAR_BUILD_POPUP_SIGN_IN = "StatusBar.Build.Popup.SignIn";
    @NonNls
    public static final String KEY_STATUSBAR_BUILD_POPUP_VIEW_DETAILS = "StatusBar.Build.Popup.ViewDetails";
    @NonNls
    public static final String KEY_STATUSBAR_BUILD_POPUP_REFRESH = "StatusBar.Build.Popup.Refresh";
    @NonNls
    public static final String KEY_STATUSBAR_BUILD_POPUP_QUEUE_BUILD = "StatusBar.Build.Popup.QueueBuild";
    @NonNls
    public static final String KEY_STATUSBAR_BUILD_POPUP_VIEW_BUILDS_PAGE = "StatusBar.Build.Popup.ViewBuildsPage";

    //actions
    @NonNls
    public static final String KEY_ACTIONS_OPEN_BROWSER = "Actions.OpenInBrowser.Title";
    @NonNls
    public static final String KEY_ACTIONS_OPEN_BROWSER_MSG = "Actions.OpenInBrowser.Message";
    @NonNls
    public static final String KEY_ACTIONS_IMPORT = "Actions.Import.Title";
    @NonNls
    public static final String KEY_ACTIONS_IMPORT_MSG = "Actions.Import.Message";
    @NonNls
    public static final String KEY_ACTIONS_CREATE_PULL_REQUEST = "Actions.CreatePullRequest.Title";
    @NonNls
    public static final String KEY_ACTIONS_CREATE_PULL_REQUEST_MSG = "Actions.CreatePullRequest.Message";
    @NonNls
    public static final String KEY_ACTIONS_SELECT_WORK_ITEMS_TITLE = "Actions.SelectWorkItems.Title";
    @NonNls
    public static final String KEY_ACTIONS_SELECT_WORK_ITEMS_MSG = "Actions.SelectWorkItems.Message";
    @NonNls
    public static final String KEY_ACTIONS_SELECT_WORK_ITEMS_ACTION = "Actions.SelectWorkItems.Action";
    @NonNls
    public static final String KEY_ACTIONS_ANNOTATE_MSG = "Actions.Annotate.Message";
    @NonNls
    public static final String KEY_ACTIONS_ANNOTATE_TITLE = "Actions.Annotate.Title";
    @NonNls
    public static final String KEY_ACTIONS_ANNOTATE_STATUS = "Actions.Annotate.Status";
    @NonNls
    public static final String KEY_ACTIONS_ANNOTATE_ERROR_TITLE = "Actions.Annotate.Error.Title";
    @NonNls
    public static final String KEY_ACTIONS_ANNOTATE_ERROR_MSG = "Actions.Annotate.Error.Msg";

    // TFVC Apply Label Action
    @NonNls
    public static final String KEY_ACTIONS_TFVC_LABEL_TITLE = "Actions.Tfvc.Label.Title";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_LABEL_MSG = "Actions.Tfvc.Label.Message";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_LABEL_PROGRESS_GATHERING_INFORMATION = "Actions.Tfvc.Label.Progress.GatheringInformation";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_LABEL_PROGRESS_CREATING_LABEL = "Actions.Tfvc.Label.Progress.CreatingLabel";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_LABEL_SUCCESS_CREATED = "Actions.Tfvc.Label.Success.Created";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_LABEL_SUCCESS_UPDATED = "Actions.Tfvc.Label.Success.Updated";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_LABEL_OVERWRITE = "Actions.Tfvc.Label.Overwrite";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_LABEL_OVERWRITE_OK_TEXT = "Actions.Tfvc.Label.Overwrite.OkText";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_LABEL_OVERWRITE_CANCEL_TEXT = "Actions.Tfvc.Label.Overwrite.CancelText";

    // TFVC Apply Label Dialog
    @NonNls
    public static final String KEY_TFVC_LABEL_DIALOG_TITLE = "Tfvc.Label.Dialog.Title";
    @NonNls
    public static final String KEY_TFVC_LABEL_DIALOG_APPLY_LABEL = "Tfvc.Label.Dialog.ApplyLabel";
    @NonNls
    public static final String KEY_TFVC_LABEL_DIALOG_ITEM_COLUMN = "Tfvc.Label.Dialog.ItemColumn";
    @NonNls
    public static final String KEY_TFVC_LABEL_DIALOG_VERSION_COLUMN = "Tfvc.Label.Dialog.VersionColumn";


    // TFVC Apply Label Action
    @NonNls
    public static final String KEY_ACTIONS_TFVC_LOCK_TITLE = "Actions.Tfvc.Lock.Title";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_LOCK_MSG = "Actions.Tfvc.Lock.Message";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_LOCK_PROGRESS_LOCKING = "Actions.Tfvc.Lock.Progress.Locking";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_LOCK_PROGRESS_UNLOCKING = "Actions.Tfvc.Lock.Progress.Unlocking";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_LOCK_SUCCESS_LOCKED = "Actions.Tfvc.Label.Success.Locked";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_LOCK_SUCCESS_UNLOCKED = "Actions.Tfvc.Label.Success.Unlocked";

    // TFVC Apply Label Dialog
    @NonNls
    public static final String KEY_TFVC_LOCK_DIALOG_TITLE = "Tfvc.Lock.Dialog.Title";
    @NonNls
    public static final String KEY_TFVC_LOCK_DIALOG_ITEM_COLUMN = "Tfvc.Lock.Dialog.ItemColumn";
    @NonNls
    public static final String KEY_TFVC_LOCK_DIALOG_LOCK_COLUMN = "Tfvc.Lock.Dialog.LockColumn";
    @NonNls
    public static final String KEY_TFVC_LOCK_DIALOG_LOCKED_BY_COLUMN = "Tfvc.Lock.Dialog.LockedByColumn";
    @NonNls
    public static final String KEY_TFVC_LOCK_DIALOG_LOCK_LEVEL_CHECKIN = "Tfvc.Lock.Dialog.LockLevel.Checkin";
    @NonNls
    public static final String KEY_TFVC_LOCK_DIALOG_LOCK_LEVEL_CHECKOUT = "Tfvc.Lock.Dialog.LockLevel.Checkout";
    @NonNls
    public static final String KEY_TFVC_LOCK_DIALOG_LOCK_BUTTON = "Tfvc.Lock.Dialog.LockButton";
    @NonNls
    public static final String KEY_TFVC_LOCK_DIALOG_UNLOCK_BUTTON = "Tfvc.Lock.Dialog.UnlockButton";

    //TFVC Branch Action
    @NonNls
    public static final String KEY_ACTIONS_TFVC_BRANCH_TITLE = "Actions.Tfvc.Branch.Title";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_BRANCH_MSG = "Actions.Tfvc.Branch.Message";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_BRANCH_FILE_CHOOSE_TITLE = "Actions.Tfvc.Branch.FileChoose.Title";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_BRANCH_FILE_CHOOSE_DESCRIPTION = "Actions.Tfvc.Branch.FileChoose.Description";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_BRANCH_COMMENT = "Actions.Tfvc.Branch.Comment";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_BRANCH_SYNC_PROGRESS = "Actions.Tfvc.Branch.Sync.Progress";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_BRANCH_MESSAGE_TITLE = "Actions.Tfvc.Branch.Message.Title";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_BRANCH_MESSAGE_SUCCESS = "Actions.Tfvc.Branch.Message.Success";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_BRANCH_MESSAGE_FAILURE = "Actions.Tfvc.Branch.Message.Failure";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_BRANCH_BROWSE_TITLE = "Actions.Tfvc.Branch.Browse.Title";

    // TFVC Server Tree
    @NonNls
    public static final String KEY_ACTIONS_TFVC_SERVER_TREE_CREATE_FOLDER_TITLE = "Tfvc.Server.Tree.Create.Folder.Title";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_SERVER_TREE_CREATE_FOLDER_MSG = "Tfvc.Server.Tree.Create.Folder.Msg";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_SERVER_TREE_SELECT_BUTTON = "Tfvc.Server.Tree.Select.Button";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_SERVER_TREE_ERROR = "Tfvc.Server.Tree.Error";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_SERVER_TREE_DIRECTORY_NOT_FOUND = "Tfvc.Server.Tree.Directory.Not.Found";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_SERVER_TREE_NO_ROOT_TITLE = "Tfvc.Server.Tree.No.Root.Title";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_SERVER_TREE_NO_ROOT_MSG = "Tfvc.Server.Tree.No.Root.Msg";

    //TFVC Branch Dialog
    @NonNls
    public static final String KEY_TFVC_BRANCH_DIALOG_TITLE = "Tfvc.Branch.Dialog.Title";
    @NonNls
    public static final String KEY_TFVC_BRANCH_DIALOG_OK_BUTTON = "Tfvc.Branch.Dialog.OkButton";

    //TFVC Merge Branch Action
    @NonNls
    public static final String KEY_ACTIONS_TFVC_MERGE_BRANCH_TITLE = "Actions.Tfvc.MergeBranch.Title";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_MERGE_BRANCH_MSG = "Actions.Tfvc.MergeBranch.Message";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_MERGE_BRANCH_ERRORS_NO_MAPPING_FOUND = "Actions.Tfvc.MergeBranch.Errors.NoMappingFound";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_MERGE_BRANCH_ERRORS_NO_CHANGES_TO_MERGE = "Actions.Tfvc.MergeBranch.Errors.NoChangesToMerge";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_MERGE_BRANCH_PROGRESS_MERGING = "Actions.Tfvc.MergeBranch.Progress.Merging";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_MERGE_BRANCH_SUCCESS = "Actions.Tfvc.MergeBranch.Success";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_MERGE_BRANCH_ERRORS_FOUND = "Actions.Tfvc.MergeBranch.Errors.Found";

    //TFVC Merge Branch Dialog
    @NonNls
    public static final String KEY_TFVC_MERGE_BRANCH_DIALOG_TITLE = "Tfvc.MergeBranch.Dialog.Title";
    @NonNls
    public static final String KEY_TFVC_MERGE_BRANCH_DIALOG_OK_BUTTON = "Tfvc.MergeBranch.Dialog.OkButton";
    @NonNls
    public static final String KEY_TFVC_MERGE_BRANCH_BROWSE_TITLE = "Tfvc.MergeBranch.Browse.Title";

    //TFVC Proxy Settings Action
    @NonNls
    public static final String KEY_ACTIONS_TFVC_PROXY_TITLE = "Actions.Tfvc.Proxy.Title";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_PROXY_MSG = "Actions.Tfvc.Proxy.Message";

    //TFVC Proxy Settings Dialog
    @NonNls
    public static final String KEY_TFVC_PROXY_DIALOG_TITLE = "Tfvc.Proxy.Dialog.Title";
    @NonNls
    public static final String KEY_TFVC_PROXY_DIALOG_OK_BUTTON = "Tfvc.Proxy.Dialog.OkButton";
    @NonNls
    public static final String KEY_TFVC_PROXY_DIALOG_ERRORS_INVALID_URI = "Tfvc.Proxy.Dialog.Errors.InvalidURI";
    //starters
    @NonNls
    public static final String STARTER_COMMAND_LINE_USAGE_MSG = "Starter.Usage.Message";
    @NonNls
    public static final String STARTER_ERRORS_SUB_COMMAND_NOT_RECOGNIZED = "Starter.Errors.SubCommandNotRecognized";
    @NonNls
    public static final String STARTER_ERRORS_MALFORMED_URI = "Starter.Errors.MalformedUri";
    @NonNls
    public static final String STARTER_ERRORS_INVALID_GIT_URL = "Starter.Errors.InvalidGitUrl";
    @NonNls
    public static final String STARTER_ERRORS_SIMPLECHECKOUT_INVALID_COMMAND_LINE_ARGS = "Starter.Errors.SimpleCheckout.InvalidCommandLineArgs";
    @NonNls
    public static final String STARTER_ERRORS_SIMPLECHECKOUT_URI_MISSING_GIT_URL = "Starter.Errors.SimpleCheckout.UriMissingGitUrl";

    //Device flow
    @NonNls
    public static final String KEY_DEVICE_FLOW_PROMPT_TITLE = "Authentication.DeviceFlowPromptTitle";
    @NonNls
    public static final String KEY_DEVICE_FLOW_PROMPT_CONTINUE_BUTTON = "Authentication.DeviceFlowPromptContinueButton";

    //TFVC
    @NonNls
    public static final String KEY_TFVC_ADD_SCHEDULING = "Tfvc.Add.Scheduling";
    @NonNls
    public static final String KEY_TFVC_DELETE_SCHEDULING = "Tfvc.Delete.Scheduling";
    @NonNls
    public static final String KEY_TFVC_ADD_ERROR = "Tfvc.Add.Error";
    @NonNls
    public static final String KEY_TFVC_ADD_ITEM = "Tfvc.Add.Item";
    @NonNls
    public static final String KEY_TFVC_ADD_ITEMS = "Tfvc.Add.Items";
    @NonNls
    public static final String KEY_TFVC_ADD_PROMPT = "Tfvc.Add.Prompt";
    @NonNls
    public static final String KEY_TFVC_ADD_PROGRESS = "Tfvc.Add.Progress";
    @NonNls
    public static final String KEY_TFVC_CHECKIN_SUCCESSFUL_TITLE = "Tfvc.Checkin.Successful.Title";
    @NonNls
    public static final String KEY_TFVC_CHECKIN_SUCCESSFUL_MSG = "Tfvc.Checkin.Successful.Msg";
    @NonNls
    public static final String KEY_TFVC_CHECKIN_LINK_TEXT = "Tfvc.Checkin.Link.Text";
    @NonNls
    public static final String KEY_TFVC_CHECKIN_STATUS = "Tfvc.Checkin.Status";
    @NonNls
    public static final String KEY_TFVC_UPDATE_STATUS_MSG = "Tfvc.Update.Status.Msg";
    @NonNls
    public static final String KEY_TFVC_TF_VERSION_WARNING_PROGRESS = "Tfvc.tf.VersionWarning.Progress";
    @NonNls
    public static final String KEY_TFVC_TF_VERSION_WARNING_TITLE = "Tfvc.tf.VersionWarning.Title";
    @NonNls
    public static final String KEY_TFVC_TF_CANNOT_DETERMINE_VERSION_TEXT = "Tfvc.tf.CannotDetermineVersion.Text";

    //TFVC Conflicts
    @NonNls
    public static final String KEY_TFVC_CONFLICT_COLUMN_NAME = "Tfvc.Conflict.Column.Name";
    @NonNls
    public static final String KEY_TFVC_CONFLICT_COLUMN_TYPE = "Tfvc.Conflict.Column.Type";
    @NonNls
    public static final String KEY_TFVC_CONFLICT_COLUMN_TYPE_CONTENT = "Tfvc.Conflict.Column.Type.Content";
    @NonNls
    public static final String KEY_TFVC_CONFLICT_COLUMN_TYPE_RENAME = "Tfvc.Conflict.Column.Type.Rename";
    @NonNls
    public static final String KEY_TFVC_CONFLICT_COLUMN_TYPE_NAME_AND_CONTENT = "Tfvc.Conflict.Column.Type.NameAndContent";
    @NonNls
    public static final String KEY_TFVC_CONFLICT_COLUMN_TYPE_MERGE = "Tfvc.Conflict.Column.Type.Merge";
    @NonNls
    public static final String KEY_TFVC_CONFLICT_COLUMN_TYPE_DELETE = "Tfvc.Conflict.Column.Type.Delete";
    @NonNls
    public static final String KEY_TFVC_CONFLICT_COLUMN_TYPE_DELETE_TARGET = "Tfvc.Conflict.Column.Type.DeleteTarget";
    @NonNls
    public static final String KEY_TFVC_CONFLICT_COLUMN_TYPE_RESOLVED = "Tfvc.Conflict.Column.Type.Resolved";
    @NonNls
    public static final String KEY_TFVC_CONFLICT_DIALOG_TITLE = "Tfvc.Conflict.Dialog.Title";
    @NonNls
    public static final String KEY_TFVC_CONFLICT_DIALOG_LATER = "Tfvc.Conflict.Dialog.Later";
    @NonNls
    public static final String KEY_TFVC_CONFLICT_DIALOG_MERGE = "Tfvc.Conflict.Dialog.Merge";
    @NonNls
    public static final String KEY_TFVC_CONFLICT_DIALOG_ACCEPT_THEIRS = "Tfvc.Conflict.Dialog.AcceptTheirs";
    @NonNls
    public static final String KEY_TFVC_CONFLICT_DIALOG_ACCEPT_YOURS = "Tfvc.Conflict.Dialog.AcceptYours";
    @NonNls
    public static final String KEY_TFVC_CONFLICT_LOADING_CONFLICTS = "Tfvc.Conflict.Loading.Conflicts";
    @NonNls
    public static final String KEY_TFVC_CONFLICT_LOADING_PROGRESS_BAR = "Tfvc.Conflict.Loading.ProgressBar";
    @NonNls
    public static final String KEY_TFVC_CONFLICT_RESOLVING_PROGRESS_BAR = "Tfvc.Conflict.Resolving.ProgressBar";
    @NonNls
    public static final String KEY_TFVC_CONFLICT_RESOLVING_STATUS = "Tfvc.Conflict.Resolving.Status";
    @NonNls
    public static final String KEY_TFVC_CONFLICT_RESOLVING_REFRESH = "Tfvc.Conflict.Resolving.Refresh";
    @NonNls
    public static final String KEY_TFVC_CONFLICT_LOAD_FAILED = "Tfvc.Conflict.Load.Failed";
    @NonNls
    public static final String KEY_TFVC_CONFLICT_MERGE_ORIGINAL = "Tfvc.Conflict.Merge.Original";
    @NonNls
    public static final String KEY_TFVC_CONFLICT_MERGE_SERVER = "Tfvc.Conflict.Merge.Server";
    @NonNls
    public static final String KEY_TFVC_CONFLICT_MERGE_LOADING = "Tfvc.Conflict.Merge.Loading";
    @NonNls
    public static final String KEY_TFVC_CONFLICT_MERGE_LOAD_FAILED = "Tfvc.Conflict.Merge.LoadFailed";
    @NonNls
    public static final String KEY_TFVC_CONFLICT_NAME_DIALOG = "Tfvc.Conflict.Name.DialogTitle";
    @NonNls
    public static final String KEY_TFVC_CONFLICT_NAME_KEEP_LOCAL = "Tfvc.Conflict.Name.KeepLocal";
    @NonNls
    public static final String KEY_TFVC_CONFLICT_NAME_ACCEPT_SERVER = "Tfvc.Conflict.Name.AcceptServer";
    @NonNls
    public static final String KEY_TFVC_CONFLICT_NAME_USE_SPECIFED = "Tfvc.Conflict.Name.UseSpecified";
    @NonNls
    public static final String KEY_TFVC_CONFLICT_LOAD_ERROR = "Tfvc.Conflict.Load.Error";
    @NonNls
    public static final String KEY_TFVC_CONFLICT_MERGE_ERROR = "Tfvc.Conflict.Merge.Error";
    @NonNls
    public static final String KEY_TFVC_CONFLICT_MERGE_ERROR_CANNOT_MERGE_DELETION = "Tfvc.Conflict.Merge.Error.CannotMergeDeletion";

    //TFVC Settings
    @NonNls
    public static final String KEY_TFVC_SETTINGS_TITLE = "Tfvc.Settings.Title";
    @NonNls
    public static final String KEY_TFVC_SETTINGS_DESCRIPTION = "Tfvc.Settings.Description";
    @NonNls
    public static final String KEY_TFVC_SETTINGS_FOUND_EXE = "Tfvc.Settings.FoundExe";
    @NonNls
    public static final String KEY_TFVC_SETTINGS_LINK_LABEL = "Tfvc.Settings.LinkLabel";
    @NonNls
    public static final String KEY_TFVC_SETTINGS_LINK_TEXT = "Tfvc.Settings.LinkText";
    @NonNls
    public static final String KEY_TFVC_SETTINGS_LINK_URL = "Tfvc.Settings.LinkUrl";
    @NonNls
    public static final String KEY_TFVC_SETTINGS_PATH_PLACEHOLDER_WIN = "Tfvc.Settings.Path.PlaceHolder.Win";
    @NonNls
    public static final String KEY_TFVC_SETTINGS_PATH_PLACEHOLDER_NOWIN = "Tfvc.Settings.Path.PlaceHolder.NoWin";
    public static final String KEY_TFVC_SETTINGS_PATH_EMPTY = "Tfvc.Settings.Path.Empty";
    public static final String KEY_TFVC_SETTINGS_PATH_NOT_FOUND = "Tfvc.Settings.Path.NotFound";
    @NonNls
    public static final String KEY_TFVC_SETTINGS_VISUAL_STUDIO_CLIENT = "Tfvc.Settings.VisualStudioClient";
    @NonNls
    public static final String KEY_TFVC_SETTINGS_VISUAL_STUDIO_CLIENT_TEST = "Tfvc.Settings.VisualStudioClient.Test";
    @NonNls
    public static final String KEY_TFVC_SETTINGS_VS_CLIENT_PATH_EMPTY = "Tfvc.Settings.VisualStudioClient.PathEmpty";
    @NonNls
    public static final String KEY_TFVC_SETTINGS_VS_CLIENT_PATH_NOT_FOUND = "Tfvc.Settings.VisualStudioClient.PathNotFound";
    @NonNls
    public static final String KEY_TFVC_SETTINGS_FOUND_VS_CLIENT_EXE = "Tfvc.Settings.VisualStudioClient.Found";

    //TFVC Edit Workspace Action
    @NonNls
    public static final String KEY_ACTIONS_TFVC_EDIT_WORKSPACE_TITLE = "Actions.Tfvc.EditWorkspace.Title";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_EDIT_WORKSPACE_MSG = "Actions.Tfvc.EditWorkspace.Message";

    //General Settings
    @NonNls
    public static final String KEY_SETTINGS_MENU_TITLE = "Settings.Menu.Title";
    @NonNls
    public static final String KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_TITLE = "Settings.Config.Mgt.Update.Title";
    @NonNls
    public static final String KEY_SETTINGS_PASSWORD_MGT_DIALOG_UPDATE_MSG = "Settings.Config.Mgt.Update.Msg";
    @NonNls
    public static final String KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_TITLE = "Settings.Config.Mgt.Delete.Title";
    @NonNls
    public static final String KEY_SETTINGS_PASSWORD_MGT_DIALOG_DELETE_MSG = "Settings.Config.Mgt.Delete.Msg";
    @NonNls
    public static final String KEY_SETTINGS_PASSWORD_MGT_NO_ROWS_SELECTED = "Settings.Config.Mgt.NoRowsSelected";
    @NonNls
    public static final String KEY_SETTINGS_PASSWORD_MGT_UPDATING = "Settings.Config.Mgt.Updating";
    @NonNls
    public static final String KEY_SETTINGS_PASSWORD_MGT_USER_NAME = "Settings.Config.Mgt.UserName";
    @NonNls
    public static final String KEY_SETTINGS_PASSWORD_MGT_REPO_URL = "Settings.Config.Mgt.RepoUrl";

    // Manage Workspaces
    @NonNls
    public static final String KEY_ACTIONS_TFVC_MANAGE_WORKSPACES_TITLE = "Actions.Tfvc.ManageWorkspaces.Title";
    @NonNls
    public static final String KEY_ACTIONS_TFVC_MANAGE_WORKSPACES_MSG = "Actions.Tfvc.ManageWorkspaces.Msg";
    @NonNls
    public static final String KEY_TFVC_MANAGE_WORKSPACES_DIALOG_TITLE = "Tfvc.ManageWorkspaces.Dialog.Title";
    @NonNls
    public static final String KEY_TFVC_MANAGE_WORKSPACES_RELOAD_MSG = "Tfvc.ManageWorkspaces.Reload.Msg";
    @NonNls
    public static final String KEY_TFVC_MANAGE_WORKSPACES_EDIT_MSG = "Tfvc.ManageWorkspaces.Edit.Msg";
    @NonNls
    public static final String KEY_TFVC_MANAGE_WORKSPACES_EDIT_ERROR_TITLE = "Tfvc.ManageWorkspaces.Edit.Error.Title";
    @NonNls
    public static final String KEY_TFVC_MANAGE_WORKSPACES_EDIT_ERROR_MSG = "Tfvc.ManageWorkspaces.Edit.Error.Msg";
    @NonNls
    public static final String KEY_TFVC_MANAGE_WORKSPACES_DELETE_CONFIRM_TITLE = "Tfvc.ManageWorkspaces.Delete.Confirm.Title";
    @NonNls
    public static final String KEY_TFVC_MANAGE_WORKSPACES_DELETE_CONFIRM_MSG = "Tfvc.ManageWorkspaces.Delete.Confirm.Msg";
    @NonNls
    public static final String KEY_TFVC_MANAGE_WORKSPACES_DELETE_MSG = "Tfvc.ManageWorkspaces.Delete.Msg";
    @NonNls
    public static final String KEY_TFVC_MANAGE_WORKSPACES_CLOSE_BUTTON = "Tfvc.ManageWorkspaces.Close.Button";
    @NonNls
    public static final String KEY_TFVC_MANAGE_WORKSPACES_MIXED_COLUMN = "Tfvc.ManageWorkspaces.Mixed.Column";
    @NonNls
    public static final String KEY_TFVC_MANAGE_WORKSPACES_SERVER_COLUMN = "Tfvc.ManageWorkspaces.Server.Column";
    @NonNls
    public static final String KEY_TFVC_MANAGE_WORKSPACES_COMMENT_COLUMN = "Tfvc.ManageWorkspaces.Comment.Column";
    @NonNls
    public static final String KEY_TFVC_MANAGE_WORKSPACES_RELOAD_ERROR_TITLE = "Tfvc.ManageWorkspaces.Reload.Error.Title";
    @NonNls
    public static final String KEY_TFVC_MANAGE_WORKSPACES_RELOAD_ERROR_MSG = "Tfvc.ManageWorkspaces.Reload.Error.Msg";
    @NonNls
    public static final String KEY_TFVC_MANAGE_WORKSPACES_DELETE_ERROR_TITLE = "Tfvc.ManageWorkspaces.Delete.Error.Title";
    @NonNls
    public static final String KEY_TFVC_MANAGE_WORKSPACES_DELETE_ERROR_MSG = "Tfvc.ManageWorkspaces.Delete.Error.Msg";

    //RepositoryView
    @NonNls
    public static final String KEY_TFVC_REPOSITORY_VIEW_CHANGELIST_TITLE = "Tfvc.RepositoryView.Changelist.Title";
    @NonNls
    public static final String KEY_TFVC_REPOSITORY_VIEW_COLUMN_REVISION = "Tfvc.RepositoryView.Column.Revision";

    //Workspace dialog
    public static final String KEY_WORKSPACE_DIALOG_TITLE = "WorkspaceDialog.Title";
    public static final String KEY_WORKSPACE_DIALOG_SAVE_BUTTON = "WorkspaceDialog.SaveButton";
    public static final String KEY_WORKSPACE_DIALOG_LOADING = "WorkspaceDialog.Loading";
    public static final String KEY_WORKSPACE_DIALOG_ERRORS_LOCAL_PATH_EMPTY = "WorkspaceDialog.Errors.LocalPathEmpty";
    public static final String KEY_WORKSPACE_DIALOG_ERRORS_SERVER_PATH_EMPTY = "WorkspaceDialog.Errors.ServerPathEmpty";
    public static final String KEY_WORKSPACE_DIALOG_ERRORS_SERVER_PATH_INVALID = "WorkspaceDialog.Errors.ServerPathInvalid";
    public static final String KEY_WORKSPACE_DIALOG_ERRORS_NAME_EMPTY = "WorkspaceDialog.Errors.NameEmpty";
    public static final String KEY_WORKSPACE_DIALOG_ERRORS_MAPPINGS_EMPTY = "WorkspaceDialog.Errors.MappingsEmpty";
    public static final String KEY_WORKSPACE_DIALOG_ERRORS_CONTEXT_FAILED = "WorkspaceDialog.Errors.ContextFailed";
    public static final String KEY_WORKSPACE_DIALOG_ERRORS_AUTH_FAILED = "WorkspaceDialog.Errors.AuthenticationFailed";
    public static final String KEY_WORKSPACE_DIALOG_COLUMN_HEADERS_STATUS = "WorkspaceDialog.ColumnHeaders.Status";
    public static final String KEY_WORKSPACE_DIALOG_COLUMN_HEADERS_SERVER_PATH = "WorkspaceDialog.ColumnHeaders.ServerPath";
    public static final String KEY_WORKSPACE_DIALOG_COLUMN_HEADERS_LOCAL_PATH = "WorkspaceDialog.ColumnHeaders.LocalPath";
    public static final String KEY_WORKSPACE_DIALOG_PROGRESS_TITLE = "WorkspaceDialog.Progress.Title";
    public static final String KEY_WORKSPACE_DIALOG_NOTIFY_SUCCESS_TITLE = "WorkspaceDialog.NotifySuccess.Title";
    public static final String KEY_WORKSPACE_DIALOG_NOTIFY_SUCCESS_MESSAGE = "WorkspaceDialog.NotifySuccess.Message";
    public static final String KEY_WORKSPACE_DIALOG_NOTIFY_FAILURE_TITLE = "WorkspaceDialog.NotifyFailure.Title";
    public static final String KEY_WORKSPACE_DIALOG_NOTIFY_SUCCESS_SYNC_MESSAGE = "WorkspaceDialog.NotifySuccess.SyncMessage";
    public static final String KEY_WORKSPACE_DIALOG_SAVE_PROGRESS_UPDATING = "WorkspaceDialog.Save.Progress.Updating";
    public static final String KEY_WORKSPACE_DIALOG_SAVE_PROGRESS_SYNCING = "WorkspaceDialog.Save.Progress.Syncing";
    public static final String KEY_WORKSPACE_DIALOG_SAVE_PROGRESS_DONE = "WorkspaceDialog.Save.Progress.Done";
}
