// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.operations;

import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.authentication.VsoAuthenticationProvider;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextBuilder;
import com.microsoft.alm.plugin.context.ServerContextManager;
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
           if (isCancelled()) {
                return;
            }

            final Profile me = authenticationProvider.getAuthenticatedUserProfile();
            final ServerContext activeContext = ServerContextManager.getInstance().getActiveContext();
            if(activeContext == ServerContext.NO_CONTEXT || activeContext.getType() == ServerContext.Type.TFS) {
                //active context will be a valid VSO context at this point
                logger.error("doWork unexpected server context, expected type VSO or VSO_DEPLOYMENT. Found: {}", activeContext);
            }

            final AccountHttpClient accountHttpClient = new AccountHttpClient(activeContext.getClient(),
                    UrlHelper.getBaseUri(authenticationProvider.VSO_AUTH_URL));
            List<Account> accounts = accountHttpClient.getAccounts(me.getId());
            final AccountLookupResults results = new AccountLookupResults();
            for (final Account a : accounts) {
                final ServerContext accountContext =
                        new ServerContextBuilder().type(ServerContext.Type.VSO)
                                .accountUri(a).authentication(authenticationProvider.getAuthenticationInfo()).build();
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
