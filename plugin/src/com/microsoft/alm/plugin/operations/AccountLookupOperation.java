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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Use this operation class to lookup the accounts on VSO for a particular user.
 */
public class AccountLookupOperation extends Operation {

    public static class AccountLookupResults implements LookupResults {
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

    public AccountLookupOperation(final AuthenticationInfo authenticationInfo, final AuthenticationResult authenticationResult) {
        assert authenticationInfo != null;
        assert authenticationResult != null;
        this.authenticationInfo = authenticationInfo;
        this.authenticationResult = authenticationResult;
    }

    public AccountLookupResults castResults(final LookupResults results) {
        return (AccountLookupResults) results;
    }

    public void doLookup(final LookupInputs inputs) {
        onLookupStarted();
        // TODO handle cancellation

        try {
            final AzureAuthenticator azureAuthenticator = VsoAuthenticationProvider.getAzureAuthenticator();
            final Profile me = azureAuthenticator.getUserProfile(authenticationResult);
            azureAuthenticator.getAccountsAsync(authenticationResult, me, new AccountsCallback() {
                @Override
                public void onSuccess(final List<Account> accounts) {
                    if (authenticationResult == null) {
                        // User canceled login
                        return;
                    }

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
                    final AccountLookupResults results = new AccountLookupResults();
                    results.error = t;
                    onLookupResults(results);
                    onLookupCompleted();
                }
            });
        } catch (IOException ioe) {
            final AccountLookupResults results = new AccountLookupResults();
            results.error = ioe;
            onLookupResults(results);
            onLookupCompleted();
        }
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
            final Profile me = azureAuthenticator.getUserProfile(authenticationResult);
            List<Account> accounts = azureAuthenticator.getAccounts(authenticationResult, me);
            for (Account a : accounts) {
                if (a.getAccountName().equalsIgnoreCase(accountName)) {
                    return a;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }

        return null;
    }
}
