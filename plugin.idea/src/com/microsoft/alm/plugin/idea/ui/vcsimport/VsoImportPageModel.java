// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.vcsimport;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.AuthenticationListener;
import com.microsoft.alm.plugin.authentication.VsoAuthenticationProvider;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.common.ModelValidationInfo;
import com.microsoft.alm.plugin.idea.ui.common.ServerContextTableModel;
import com.microsoft.alm.plugin.operations.AccountLookupOperation;
import com.microsoft.alm.plugin.operations.Operation;
import com.microsoft.alm.plugin.operations.ServerContextLookupOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements the AbstractImportPageModel for VSO.
 */
public class VsoImportPageModel extends ImportPageModelImpl {
    private VsoAuthenticationProvider authenticationProvider = VsoAuthenticationProvider.getInstance();
    private static final Logger logger = LoggerFactory.getLogger(VsoImportPageModel.class);


    public VsoImportPageModel(final ImportModel importDialogModel) {
        super(importDialogModel, ServerContextTableModel.VSO_PROJECT_COLUMNS);

        // Set default server name for VSO
        setServerNameInternal(TfPluginBundle.message(TfPluginBundle.KEY_IMPORT_DIALOG_VSO_SERVER_NAME));

        setConnectionStatus(false);
        setAuthenticating(false);

        //TODO this check needs to be reworked because it touches the server
        if (authenticationProvider.isAuthenticated()) {
            loadTeamProjects();
        }
    }

    protected AuthenticationInfo getAuthenticationInfo() {
        return authenticationProvider.getAuthenticationInfo();
    }

    @Override
    public void signOut() {
        super.signOut();
        authenticationProvider.clearAuthenticationDetails();
    }

    @Override
    public void loadTeamProjects() {
        clearErrors();
        if (authenticationProvider.isAuthenticated()) {
            loadProjectsFromAllAccounts();
        } else {
            authenticationProvider.authenticateAsync(VsoAuthenticationProvider.VSO_ROOT, new AuthenticationListener() {
                @Override
                public void authenticating() {
                    // We are starting to authenticate, so set the boolean
                    setAuthenticating(true);
                }

                @Override
                public void authenticated(final AuthenticationInfo authenticationInfo, final Throwable throwable) {
                    // Push this event back onto the UI thread
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            // Authentication is over, so set the boolean
                            setAuthenticating(false);
                            //Log exception
                            if (throwable != null) {
                                logger.warn("Authenticating with Team Services failed", throwable);
                            }
                            //try to load the team projects
                            loadProjectsFromAllAccounts();
                        }
                    }, ModalityState.any());
                }
            });
        }
    }

    private void loadProjectsFromAllAccounts() {
        if (!authenticationProvider.isAuthenticated()) {
            addError(ModelValidationInfo.createWithResource(TfPluginBundle.KEY_LOGIN_PAGE_ERRORS_VSO_SIGN_IN_FAILED));
            signOut();
            return;
        }

        setConnectionStatus(true);
        setLoading(true);
        setUserName(authenticationProvider.getAuthenticationInfo().getUserNameForDisplay());
        clearContexts();

        final AccountLookupOperation accountLookupOperation = new AccountLookupOperation(authenticationProvider.getAuthenticationInfo(), authenticationProvider.getAuthenticationResult());
        accountLookupOperation.addListener(new Operation.Listener() {
            @Override
            public void notifyLookupStarted() {
                // nothing to do
            }

            @Override
            public void notifyLookupCompleted() {
                // nothing to do here, we are still loading team projects
            }

            @Override
            public void notifyLookupResults(final Operation.Results results) {
                final ModelValidationInfo validationInfo;
                if (results.hasError()) {
                    validationInfo = ModelValidationInfo.createWithMessage(results.getError().getMessage());
                } else if (results.isCanceled()) {
                    validationInfo = ModelValidationInfo.createWithResource(TfPluginBundle.KEY_OPERATION_ERRORS_LOOKUP_CANCELED);
                } else {
                    validationInfo = ModelValidationInfo.NO_ERRORS;
                    // Take the list of accounts and use them to query the team projects
                    getTeamProjectProvider().loadContexts(
                            accountLookupOperation.castResults(results).getServerContexts(),
                            ServerContextLookupOperation.ContextScope.PROJECT);
                }

                // If there was an error or cancellation message, send it back to the user
                if (validationInfo != ModelValidationInfo.NO_ERRORS) {
                    // Push this event back onto the UI thread
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            addError(validationInfo);
                            signOut();
                        }
                    });
                }
            }
        });
        // Start the operation
        accountLookupOperation.doWork(Operation.EMPTY_INPUTS);
    }
}
