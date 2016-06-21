// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.resources;

import com.intellij.CommonBundle;
import com.intellij.reference.SoftReference;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.lang.ref.Reference;
import java.util.ResourceBundle;

/**
 * This class allows static access to the plugins string resources.
 */
public class TfPluginBundle {
    @NonNls
    public static final String BUNDLE_NAME = "com.microsoft.alm.plugin.idea.ui.tfplugin";

    private static Reference<ResourceBundle> thisBundle;

    private static ResourceBundle getBundle() {
        ResourceBundle bundle = com.intellij.reference.SoftReference.dereference(thisBundle);
        if (bundle == null) {
            bundle = ResourceBundle.getBundle(BUNDLE_NAME);
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
    public static final String KEY_TF_GIT = "Providers.TfGitCheckoutProvider";
    @NonNls
    public static final String KEY_GIT_NOT_CONFIGURED = "Plugin.Error.GitExeNotConfigured";

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

    // Common Git
    @NonNls
    public static final String KEY_GIT_HISTORY_ERRORS_NO_HISTORY_FOUND = "Git.History.Errors.NoHistoryFound";
    
    // Checkout dialog ui and models
    @NonNls
    public static final String KEY_CHECKOUT_DIALOG_TFS_TAB = "CheckoutDialog.TfsTab";
    @NonNls
    public static final String KEY_CHECKOUT_DIALOG_VSO_TAB = "CheckoutDialog.VsoTab";
    @NonNls
    public static final String KEY_CHECKOUT_DIALOG_CLONE_BUTTON = "CheckoutDialog.CloneButton";
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
    public static final String KEY_CHECKOUT_ERRORS_UNEXPECTED = "Checkout.Errors.Unexpected";
    @NonNls
    public static final String KEY_CHECKOUT_ERRORS_AUTHENTICATION_FAILED_TITLE = "CheckoutDialog.Errors.AuthenticationFailed.Title";

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
    public static final String KEY_FEEDBACK_DIALOG_ERRORS_INVALID_EMAIL = "Feedback.Dialog.Errors.InvalidEmail";
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

    // Work item association meesages
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
}
