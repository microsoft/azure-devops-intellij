// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.checkout;

import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.AuthenticationListener;
import com.microsoft.alm.plugin.authentication.VsoAuthenticationProvider;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.alm.plugin.idea.ui.common.ModelValidationInfo;
import com.microsoft.alm.plugin.idea.ui.common.ServerContextTableModel;
import com.microsoft.alm.plugin.idea.utils.IdeaHelper;
import com.microsoft.alm.plugin.operations.AccountLookupOperation;
import com.microsoft.alm.plugin.operations.Operation;
import com.microsoft.alm.plugin.operations.ServerContextLookupOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * This class implements the abstract CheckoutPageModel for VSO.
 */
class VsoCheckoutPageModel extends CheckoutPageModelImpl {
    private static final Logger logger = LoggerFactory.getLogger(VsoCheckoutPageModel.class);
    private VsoAuthenticationProvider authenticationProvider = VsoAuthenticationProvider.getInstance();

    public VsoCheckoutPageModel(CheckoutModel checkoutModel) {
        super(checkoutModel, ServerContextTableModel.VSO_REPO_COLUMNS);

        // Set default server name for VSO
        setServerNameInternal(TfPluginBundle.message(TfPluginBundle.KEY_USER_ACCOUNT_PANEL_VSO_SERVER_NAME));

        setConnectionStatus(false);
        setAuthenticating(false);

        if (authenticationProvider.isAuthenticated()) {
            loadReposFromAllAccounts();
        }
    }

    @Override
    protected AuthenticationInfo getAuthenticationInfo() {
        return authenticationProvider.getAuthenticationInfo();
    }

    @Override
    public void signOut() {
        super.signOut();
        authenticationProvider.clearAuthenticationDetails();
    }

    @Override
    public void loadRepositories() {
        clearErrors();
        if (authenticationProvider.isAuthenticated()) {
            loadReposFromAllAccounts();
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
                            IdeaHelper.runOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    // Authentication is over, so set the boolean
                                    setAuthenticating(false);
                                    //Log exception
                                    if (throwable != null) {
                                        logger.warn("Authenticating with Team Services failed", throwable);
                                    }
                                    //try to load the repos
                                    loadReposFromAllAccounts();
                                }
                            });
                        }
                    }
            );
        }
    }

    private void loadReposFromAllAccounts() {
        if (!authenticationProvider.isAuthenticated()) {
            addError(ModelValidationInfo.createWithResource(TfPluginBundle.KEY_LOGIN_PAGE_ERRORS_VSO_SIGN_IN_FAILED));
            signOut();
            return;
        }

        setConnectionStatus(true);
        setLoading(true);
        setUserName(authenticationProvider.getAuthenticationInfo().getUserNameForDisplay());
        clearContexts();

        final AccountLookupOperation accountLookupOperation = new AccountLookupOperation(authenticationProvider);
        accountLookupOperation.addListener(new Operation.Listener() {
            @Override
            public void notifyLookupStarted() {
                // nothing to do
            }

            @Override
            public void notifyLookupCompleted() {
                // nothing to do here, we are still loading repos
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

                    //successfully logged in to VSO and obtained list of accounts, save the context so user doesn't have to login again
                    final List<ServerContext> accountContexts = accountLookupOperation.castResults(results).getServerContexts();

                    // Take the list of accounts and use them to query the repos
                    getRepositoryProvider().loadContexts(accountContexts,
                            ServerContextLookupOperation.ContextScope.REPOSITORY);
                }

                // If there was an error or cancellation message, send it back to the user
                if (validationInfo != ModelValidationInfo.NO_ERRORS) {
                    // Push this event back onto the UI thread
                    IdeaHelper.runOnUIThread(new Runnable() {
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
        accountLookupOperation.doWorkAsync(Operation.EMPTY_INPUTS);
    }

}
