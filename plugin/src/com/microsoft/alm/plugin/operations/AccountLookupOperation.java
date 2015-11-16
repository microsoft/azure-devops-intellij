// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.operations;

import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.VsoAuthenticationProvider;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextBuilder;
import com.microsoft.tf.common.authentication.aad.AccountsCallback;
import com.microsoft.tf.common.authentication.aad.AzureAuthenticator;
import com.microsoft.visualstudio.services.account.webapi.model.Account;
import com.microsoft.visualstudio.services.account.webapi.model.Profile;
import com.microsoftopentechnologies.auth.AuthenticationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Use this operation class to lookup the accounts on VSO for a particular user.
 */
public class AccountLookupOperation extends Operation {
    private static final Logger logger = LoggerFactory.getLogger(AccountLookupOperation.class);

    public static class AccountLookupResults implements Results {
        private boolean isCanceled = false;
        private Throwable error = null;
        private final List<ServerContext> serverContexts = new ArrayList<ServerContext>();

        public List<ServerContext> getServerContexts() {
            return Collections.unmodifiableList(serverContexts);
        }

        @Override
        public Throwable getError() {
            return error;
        }

        @Override
        public boolean hasError() {
            return error != null;
        }

        @Override
        public boolean isCanceled() {
            return isCanceled;
        }
    }

    private final AuthenticationInfo authenticationInfo;
    private final AuthenticationResult authenticationResult;
    private Future innerOperation;

    public AccountLookupOperation(final AuthenticationInfo authenticationInfo, final AuthenticationResult authenticationResult) {
        assert authenticationInfo != null;
        assert authenticationResult != null;
        this.authenticationInfo = authenticationInfo;
        this.authenticationResult = authenticationResult;
    }

    public AccountLookupResults castResults(final Results results) {
        return (AccountLookupResults) results;
    }

    public void doWork(final Inputs inputs) {
        onLookupStarted();

        try {
            if (isCancelled()) { return; }
            final AzureAuthenticator azureAuthenticator = VsoAuthenticationProvider.getAzureAuthenticator();
            final Profile me = getProfile(authenticationResult);
            if (isCancelled()) { return; }
            innerOperation = azureAuthenticator.getAccountsAsync(authenticationResult, me, new AccountsCallback() {
                @Override
                public void onSuccess(final List<Account> accounts) {
                    if (isCancelled()) { return; }

                    final AccountLookupResults results = new AccountLookupResults();
                    for (final Account a : accounts) {
                        final ServerContext accountContext =
                                new ServerContextBuilder().type(ServerContext.Type.VSO_DEPLOYMENT)
                                        .accountUri(a).authentication(authenticationInfo).build();
                        results.serverContexts.add(accountContext);
                    }
                    onLookupResults(results);
                    onLookupCompleted();
                }

                @Override
                public void onFailure(final Throwable t) {
                    if (isCancelled()) { return; }
                    terminate(t);
                }
            });
        } catch (Throwable ex) {
            terminate(ex);
        }
    }

    @Override
    public void cancel() {
        super.cancel();

        if (innerOperation != null && !innerOperation.isDone()) {
            innerOperation.cancel(true);
        }

        final AccountLookupResults results = new AccountLookupResults();
        results.isCanceled = true;
        onLookupResults(results);
        onLookupCompleted();
    }

    @Override
    protected void terminate(Throwable throwable) {
        super.terminate(throwable);

        final AccountLookupResults results = new AccountLookupResults();
        results.error = throwable;
        onLookupResults(results);
        onLookupCompleted();
    }

    private static Profile getProfile(final AuthenticationResult authenticationResult) {
        try {
            final AzureAuthenticator azureAuthenticator = VsoAuthenticationProvider.getAzureAuthenticator();
            AuthenticationResult newResult = refreshAuthenticationResult(azureAuthenticator, authenticationResult);
            if (newResult == null) {
                // We couldn't refresh the token, but we will try using it anyway
                newResult = authenticationResult;
            }
            return azureAuthenticator.getUserProfile(newResult);
        } catch (IOException e) {
            logger.warn("Getting azure profile failed", e);
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }
    }

    private static AuthenticationResult refreshAuthenticationResult(final AzureAuthenticator azureAuthenticator, final AuthenticationResult authenticationResult) {
        try {
            // always refresh it -- this is the only way to ensure it is valid
            return azureAuthenticator.refreshAadAccessToken(authenticationResult);
        } catch (IOException e) {
            // refreshing failed, log exception
            logger.warn("Refreshing access token failed", e);
        }

        return null;
    }

    /**
     * This method returns the matching Account object from a call to Azure Authenticator getAccounts.
     * This method is synchronous and should be called on a background thread.
     *
     * @param authenticationResult The Authentication Result returned from authentication provider
     * @param accountName          The name of the account to retrieve
     * @return the Account object that matches the name or null if none could be found
     */
    public static Account getAccount(final AuthenticationResult authenticationResult, final String accountName) {
        if (authenticationResult == null) {
            return null;
        }

        try {
            final AzureAuthenticator azureAuthenticator = VsoAuthenticationProvider.getAzureAuthenticator();
            final Profile me = getProfile(authenticationResult);
            List<Account> accounts = azureAuthenticator.getAccounts(authenticationResult, me);
            for (Account a : accounts) {
                if (a.getAccountName().equalsIgnoreCase(accountName)) {
                    return a;
                }
            }
        } catch (IOException e) {
            logger.warn("Getting account failed", e);
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }

        return null;
    }
}
