// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.operations;

import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.authentication.VsoAuthenticationProvider;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextBuilder;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.services.LocalizationService;
import com.microsoft.visualstudio.services.account.webapi.AccountHttpClient;
import com.microsoft.visualstudio.services.account.webapi.model.Account;
import com.microsoft.visualstudio.services.account.webapi.model.Profile;
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

    private Future innerOperation;

    public static class AccountLookupResults extends ResultsImpl {
        private final List<ServerContext> serverContexts = new ArrayList<ServerContext>();

        public List<ServerContext> getServerContexts() {
            return Collections.unmodifiableList(serverContexts);
        }
    }

    public AccountLookupOperation() {
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

            final Profile me = VsoAuthenticationProvider.getInstance().getAuthenticatedUserProfile();
            final ServerContext vsoDeploymentContext = ServerContextManager.getInstance().get(VsoAuthenticationProvider.VSO_AUTH_URL);
            if (!VsoAuthenticationProvider.getInstance().isAuthenticated() ||
                    vsoDeploymentContext == null || vsoDeploymentContext.getType() == ServerContext.Type.TFS) {
                // We aren't authenticated, or we couldn't find the VSO context
                logger.warn("doWork unexpected server context, expected type VSO or VSO_DEPLOYMENT. Found: {}", vsoDeploymentContext);
                throw new RuntimeException(LocalizationService.ExceptionMessageKeys.KEY_VSO_AUTH_FAILED);
            }

            final AccountHttpClient accountHttpClient = new AccountHttpClient(vsoDeploymentContext.getClient(),
                    UrlHelper.getBaseUri(VsoAuthenticationProvider.VSO_AUTH_URL));
            List<Account> accounts = accountHttpClient.getAccounts(me.getId());
            final AccountLookupResults results = new AccountLookupResults();
            for (final Account a : accounts) {
                final ServerContext accountContext =
                        new ServerContextBuilder().type(ServerContext.Type.VSO)
                                .accountUri(a)
                                .authentication(VsoAuthenticationProvider.getInstance().getAuthenticationInfo())
                                .build();
                results.serverContexts.add(accountContext);
            }
            onLookupResults(results);
            onLookupCompleted();
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
}
