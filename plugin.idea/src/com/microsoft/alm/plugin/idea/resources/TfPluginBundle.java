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
    @NonNls public static final String BUNDLE_NAME = "com.microsoft.alm.plugin.idea.ui.tfplugin";

    private static Reference<ResourceBundle> thisBundle;

    private static ResourceBundle getBundle() {
        ResourceBundle bundle = com.intellij.reference.SoftReference.dereference(thisBundle);
        if(bundle == null) {
            bundle = ResourceBundle.getBundle(BUNDLE_NAME);
            thisBundle = new SoftReference<ResourceBundle>(bundle);
        }
        return bundle;
    }

    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE_NAME) String key,
                                 @NotNull Object... params) {
        return CommonBundle.message(getBundle(), key, params);
    }

    // Login form
    @NonNls public static final String KEY_LOGIN_FORM_AUTHENTICATING_VSO = "LoginForm.VSO.Authenticating";
    @NonNls public static final String KEY_LOGIN_FORM_AUTHENTICATING_TFS = "LoginForm.TFS.Authenticating";
    @NonNls public static final String KEY_LOGIN_PAGE_ERRORS_VSO_SIGN_IN_FAILED = "LoginForm.Errors.VSO.SigninFailed";
    @NonNls public static final String KEY_LOGIN_PAGE_ERRORS_TFS_CONNECT_FAILED = "LoginForm.Errors.TFS.ConnectFailed";
    @NonNls public static final String KEY_LOGIN_FORM_ERRORS_NOT_CONNECTED = "LoginForm.Errors.NotConnected";
    @NonNls public static final String KEY_LOGIN_FORM_TFS_ERRORS_NO_SERVER_NAME = "LoginForm.TFS.Errors.NoServerName";
    @NonNls public static final String KEY_LOGIN_FORM_TFS_ERRORS_INVALID_SERVER_URL = "LoginForm.TFS.Errors.InvalidServerUrl";

    // Server Context Table
    @NonNls public static final String KEY_SERVER_CONTEXT_TABLE_ACCOUNT_COLUMN = "ServerContextTable.AccountColumn";
    @NonNls public static final String KEY_SERVER_CONTEXT_TABLE_COLLECTION_COLUMN = "ServerContextTable.CollectionColumn";
    @NonNls public static final String KEY_SERVER_CONTEXT_TABLE_PROJECT_COLUMN = "ServerContextTable.ProjectColumn";
    @NonNls public static final String KEY_SERVER_CONTEXT_TABLE_REPO_COLUMN = "ServerContextTable.RepoColumn";

    // Common
    @NonNls public static final String KEY_OPERATION_ERRORS_LOOKUP_CANCELED = "Operation.Errors.LookupCanceled";

    // Checkout dialog ui and models
    @NonNls public static final String KEY_CHECKOUT_DIALOG_TFS_TAB = "CheckoutDialog.TfsTab";
    @NonNls public static final String KEY_CHECKOUT_DIALOG_VSO_TAB = "CheckoutDialog.VsoTab";
    @NonNls public static final String KEY_CHECKOUT_DIALOG_CLONE_BUTTON = "CheckoutDialog.CloneButton";
    @NonNls public static final String KEY_CHECKOUT_DIALOG_TITLE = "CheckoutDialog.Title";
    @NonNls public static final String KEY_CHECKOUT_DIALOG_FILTER_HINT = "CheckoutDialog.FilterHint";
    @NonNls public static final String KEY_CHECKOUT_DIALOG_PARENT_FOLDER_DIALOG_TITLE = "CheckoutDialog.ParentFolder.DialogTitle";
    @NonNls public static final String KEY_CHECKOUT_DIALOG_ERRORS_PARENT_DIR_EMPTY = "CheckoutDialog.Errors.ParentDirEmpty";
    @NonNls public static final String KEY_CHECKOUT_DIALOG_ERRORS_PARENT_DIR_NOT_FOUND = "CheckoutDialog.Errors.ParentDirNotFound";
    @NonNls public static final String KEY_CHECKOUT_DIALOG_ERRORS_REPO_NOT_SELECTED = "CheckoutDialog.Errors.RepoNotSelected";
    @NonNls public static final String KEY_CHECKOUT_DIALOG_ERRORS_DIR_NAME_EMPTY = "CheckoutDialog.Errors.DirNameEmpty";
    @NonNls public static final String KEY_CHECKOUT_DIALOG_ERRORS_DESTINATION_EXISTS = "CheckoutDialog.Errors.DestinationExists";
    @NonNls public static final String KEY_CHECKOUT_DIALOG_ERRORS_DIR_NAME_INVALID = "CheckoutDialog.Errors.DirNameInvalid";
    @NonNls public static final String KEY_CHECKOUT_DIALOG_PAT_TOKEN_DESC = "CheckoutDialog.PAT.TokenDesc";
    @NonNls public static final String KEY_CHECKOUT_ERRORS_UNEXPECTED="Checkout.Errors.Unexpected";

    //User account panel
    @NonNls public static final String KEY_USER_ACCOUNT_PANEL_VSO_SERVER_NAME = "UserAccountPanel.VSO.ServerName";
    @NonNls public static final String KEY_USER_ACCOUNT_PANEL_SWITCH_SERVER = "UserAccountPanel.SwitchServer";
    @NonNls public static final String KEY_USER_ACCOUNT_PANEL_SIGN_OUT = "UserAccountPanel.SignOut";

    // Prompts
    @NonNls public static final String KEY_PROMPT_CREDENTIALS_TITLE = "Prompt.Credentials.Title";
    @NonNls public static final String KEY_PROMPT_CREDENTIALS_MESSAGE = "Prompt.Credentials.Message";

    // Feedback dialog
    @NonNls public static final String KEY_FEEDBACK_DIALOG_TITLE = "Feedback.Dialog.Title";
    @NonNls public static final String KEY_FEEDBACK_DIALOG_OK_FROWN = "Feedback.Dialog.OkButtonText.Frown";
    @NonNls public static final String KEY_FEEDBACK_DIALOG_OK_SMILE = "Feedback.Dialog.OkButtonText.Smile";
    @NonNls public static final String KEY_FEEDBACK_DIALOG_COMMENT_LABEL_FROWN = "Feedback.Dialog.CommentLabel.Frown";
    @NonNls public static final String KEY_FEEDBACK_NOTIFICATION = "Feedback.Notification";

    //Import dialog ui and models
    @NonNls public static final String KEY_IMPORT_DIALOG_TITLE = "ImportDialog.Title";
    @NonNls public static final String KEY_IMPORT_DIALOG_IMPORT_BUTTON = "ImportDialog.ImportButton";
    @NonNls public static final String KEY_IMPORT_DIALOG_TFS_TAB = "ImportDialog.TfsTab";
    @NonNls public static final String KEY_IMPORT_DIALOG_VSO_TAB = "ImportDialog.VsoTab";
    @NonNls public static final String KEY_IMPORT_DIALOG_FILTER_HINT = "ImportDialog.FilterHint";
    @NonNls public static final String KEY_IMPORT_DIALOG_ERRORS_PROJECT_NOT_SELECTED = "ImportDialog.Errors.ProjectNotSelected";
    @NonNls public static final String KEY_IMPORT_DIALOG_ERRORS_REPO_NAME_EMPTY = "ImportDialog.Errors.RepoNameEmpty";
    @NonNls public static final String KEY_IMPORT_DIALOG_VSO_SERVER_NAME = "ImportDialog.VSO.ServerName";
    @NonNls public static final String KEY_IMPORT_FAILED = "Import.Failed";
    @NonNls public static final String KEY_IMPORT_SUCCEEDED = "Import.Succeeded";
    @NonNls public static final String KEY_IMPORT_SUCCEEDED_MESSAGE = "Import.Succeeded.Message";
    @NonNls public static final String KEY_IMPORT_IMPORTING_PROJECT = "Import.ImportingProject";
    @NonNls public static final String KEY_IMPORT_GIT_INIT = "Import.GitInit";
    @NonNls public static final String KEY_IMPORT_GIT_INIT_ERROR = "Import.Errors.GitInit";
    @NonNls public static final String KEY_IMPORT_SELECT_FILES = "Import.SelectFiles";
    @NonNls public static final String KEY_IMPORT_SELECT_FILES_DIALOG_TITLE = "Import.SelectFilesDialog.Title";
    @NonNls public static final String KEY_IMPORT_ADDING_FILES = "Import.AddingFiles";
    @NonNls public static final String KEY_IMPORT_ADDING_FILES_ERROR = "Import.Errors.AddingFiles";
    @NonNls public static final String KEY_IMPORT_NO_SELECTED_FILES = "Import.Errors.NoSelectedFiles";
    @NonNls public static final String KEY_IMPORT_CREATING_REMOTE_REPO = "Import.CreatingRemoteRepo";
    @NonNls public static final String KEY_IMPORT_CREATING_REMOTE_REPO_UNEXPECTED_ERROR = "Import.Errors.CreatingRemoteRepo.UnexpectedError";
    @NonNls public static final String KEY_IMPORT_CREATING_REMOTE_REPO_ALREADY_EXISTS_ERROR= "Import.Errors.CreatingRemoteRepo.AlreadyExistsError";
    @NonNls public static final String KEY_IMPORT_GIT_REMOTE = "Import.GitRemote";
    @NonNls public static final String KEY_IMPORT_GIT_REMOTE_ERROR = "Import.Errors.GitRemote";
    @NonNls public static final String KEY_IMPORT_GIT_PUSH = "Import.GitPush";
    @NonNls public static final String KEY_IMPORT_ERRORS_UNEXPECTED="Import.Errors.Unexpected";


    // CreatePullRequest dialog ui and models
    @NonNls public static final String KEY_CREATE_PR_DIALOG_CREATE_BUTTON = "CreatePullRequestDialog.CreateButton";
    @NonNls public static final String KEY_CREATE_PR_DIALOG_TITLE = "CreatePullRequestDialog.Title";
    @NonNls public static final String KEY_CREATE_PR_CREATED_TITLE = "CreatePullRequestDialog.Created.Title";
    @NonNls public static final String KEY_CREATE_PR_CREATED_MESSAGE = "CreatePullRequestDialog.Created.Message";
    @NonNls public static final String KEY_CREATE_PR_PUSH_TITLE = "CreatePullRequestDialog.Push.Title";
    @NonNls public static final String KEY_CREATE_PR_ERRORS_DIFF_FAILED_TITLE = "CreatePullRequestDialog.Errors.DiffFailed.Title";
    @NonNls public static final String KEY_CREATE_PR_ERRORS_DIFF_FAILED_MSG = "CreatePullRequestDialog.Errors.DiffFailed.Message";
    @NonNls public static final String KEY_CREATE_PR_ERRORS_CREATE_FAILED_TITLE = "CreatePullRequestDialog.Errors.CreateFailed.Title";
    @NonNls public static final String KEY_CREATE_PR_ERRORS_PUSH_FAILED_TITLE = "CreatePullRequestDialog.Errors.PushFailed.Title";
    @NonNls public static final String KEY_CREATE_PR_ERRORS_TITLE_EMPTY = "CreatePullRequestDialog.Errors.TitleEmpty";
    @NonNls public static final String KEY_CREATE_PR_ERRORS_TITLE_TOO_LONG = "CreatePullRequestDialog.Errors.TitleTooLong";
    @NonNls public static final String KEY_CREATE_PR_ERRORS_DESCRIPTION_EMPTY = "CreatePullRequestDialog.Errors.DescriptionEmpty";
    @NonNls public static final String KEY_CREATE_PR_ERRORS_DESCRIPTION_TOO_LONG = "CreatePullRequestDialog.Errors.DescriptionTooLong";
    @NonNls public static final String KEY_CREATE_PR_ERRORS_TARGET_NOT_SELECTED = "CreatePullRequestDialog.Errors.TargetNotSelected";
    @NonNls public static final String KEY_CREATE_PR_ERRORS_TARGET_IS_LOCAL_TRACKING = "CreatePullRequestDialog.Errors.TargetIsLocalTracking";
    @NonNls public static final String KEY_CREATE_PR_ERRORS_NO_ACTIVE_SERVER_CONTEXT = "CreatePullRequestDialog.Errors.NoActiveServerContext";
    @NonNls public static final String KEY_CREATE_PR_ERRORS_SOURCE_EMPTY = "CreatePullRequestDialog.Errors.SourceEmpty";
    @NonNls public static final String KEY_CREATE_PR_CHANGES_PANE_TITLE = "CreatePullRequestDialog.ChangesPane.Title";
    @NonNls public static final String KEY_CREATE_PR_COMMITS_PANE_TITLE = "CreatePullRequestDialog.CommitsPane.Title";
    @NonNls public static final String KEY_CREATE_PR_DEFAULT_TITLE= "CreatePullRequestDialog.Default.Title";


    //actions
    @NonNls public static final String KEY_ACTIONS_OPEN_BROWSER = "Actions.OpenInBrowser.Title";
    @NonNls public static final String KEY_ACTIONS_OPEN_BROWSER_MSG = "Actions.OpenInBrowser.Message";
    @NonNls public static final String KEY_ACTIONS_IMPORT = "Actions.Import.Title";
    @NonNls public static final String KEY_ACTIONS_IMPORT_MSG = "Actions.Import.Message";
    @NonNls public static final String KEY_ACTIONS_CREATE_PULL_REQUEST = "Actions.CreatePullRequest.Title";
    @NonNls public static final String KEY_ACTIONS_CREATE_PULL_REQUEST_MSG = "Actions.CreatePullRequest.Message";
}
