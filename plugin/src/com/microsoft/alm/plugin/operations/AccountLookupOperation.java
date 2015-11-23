// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.operations;

import com.microsoft.alm.plugin.authentication.VsoAuthenticationProvider;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextBuilder;
import com.microsoft.tf.common.authentication.aad.AccountsCallback;
import com.microsoft.tf.common.authentication.aad.AzureAuthenticator;
import com.microsoft.visualstudio.services.account.webapi.model.Account;
import com.microsoft.visualstudio.services.account.webapi.model.Profile;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Use this operation class to lookup the accounts on VSO for a particular user.
 */
public class AccountLookupOperation extends Operation {
    private static final Logger logger = LoggerFactory.getLogger(AccountLookupOperation.class);

    public static class AccountLookupResults extends ResultsImpl {
        private final List<ServerContext> serverContexts = new ArrayList<ServerContext>();

        public List<ServerContext> getServerContexts() {
            return Collections.unmodifiableList(serverContexts);
        }
    }

    private final VsoAuthenticationProvider authenticationProvider;
    private Future innerOperation;

    public AccountLookupOperation(final VsoAuthenticationProvider authenticationProvider) {
        assert authenticationProvider != null;
        this.authenticationProvider = authenticationProvider;
    }

    public AccountLookupResults castResults(final Results results) {
        return (AccountLookupResults) results;
    }

    public void doWork(final Inputs inputs) {
        onLookupStarted();

        try {
            if (isCancelled()) {
                return;
            }
            final AzureAuthenticator azureAuthenticator = authenticationProvider.getAzureAuthenticator();
            final Profile me = authenticationProvider.getAuthenticatedUserProfile();
            if (isCancelled()) {
                return;
            }
            innerOperation = azureAuthenticator.getAccountsAsync(authenticationProvider.getAuthenticationResult(), me, new AccountsCallback() {
                @Override
                public void onSuccess(final List<Account> accounts) {
                    if (isCancelled()) {
                        return;
                    }

                    final AccountLookupResults results = new AccountLookupResults();
                    for (final Account a : accounts) {
                        final ServerContext accountContext =
                                new ServerContextBuilder().type(ServerContext.Type.VSO_DEPLOYMENT)
                                        .accountUri(a).authentication(authenticationProvider.getAuthenticationInfo()).build();
                        results.serverContexts.add(accountContext);
                    }
                    onLookupResults(results);
                    onLookupCompleted();
                }

                @Override
                public void onFailure(final Throwable t) {
                    if (isCancelled()) {
                        return;
                    }
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
        results.isCancelled = true;
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

    /**
     * This method returns the matching Account object from a call to Azure Authenticator getAccounts.
     * This method is synchronous and should be called on a background thread.
     *
     * @param authenticationProvider The authentication provider
     * @param accountName          The name of the account to retrieve
     * @return the Account object that matches the name or null if none could be found
     */
    public static Account getAccount(final VsoAuthenticationProvider authenticationProvider, final String accountName) {
        if (authenticationProvider == null) {
            return null;
        }

        try {
            final AzureAuthenticator azureAuthenticator = authenticationProvider.getAzureAuthenticator();
            final Profile me = authenticationProvider.getAuthenticatedUserProfile();
            if(azureAuthenticator != null && me != null) {
                List<Account> accounts = azureAuthenticator.getAccounts(authenticationProvider.getAuthenticationResult(), me);
                for (Account a : accounts) {
                    if (StringUtils.equalsIgnoreCase(a.getAccountName(), accountName)) {
                        return a;
                    }
                }
            }
        } catch (Throwable t) {
            logger.warn("Getting account failed", t);
        }

        return null;
    }
}
