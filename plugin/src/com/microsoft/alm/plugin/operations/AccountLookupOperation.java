// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.alm.plugin.authentication.AuthHelper;
import com.microsoft.alm.plugin.authentication.VsoAuthenticationProvider;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextBuilder;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.exceptions.TeamServicesException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Use this operation class to lookup the accounts on VSO for a particular user.
 */
public class AccountLookupOperation extends Operation {
    private static final Logger logger = LoggerFactory.getLogger(AccountLookupOperation.class);

    private static final String ACCOUNT_ENDPOINT = "/_apis/Accounts?memberid=%s&api-version=%s&properties=%s";
    private static final String TFS_API_VERSION = "5.0-preview.1";
    private static final String TFS_SERVICE_URL_PROPERTY_NAME = "Microsoft.VisualStudio.Services.Account.ServiceUrl.00025394-6065-48CA-87D9-7F5672854EF7";

    public static class AccountLookupResults extends ResultsImpl {
        private final List<ServerContext> serverContexts = new ArrayList<ServerContext>();

        public List<ServerContext> getServerContexts() {
            return Collections.unmodifiableList(serverContexts);
        }
    }

    protected AccountLookupOperation() {
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

            final ServerContext vsoDeploymentContext = ServerContextManager.getInstance().get(VsoAuthenticationProvider.VSO_AUTH_URL);
            if (!VsoAuthenticationProvider.getInstance().isAuthenticated(VsoAuthenticationProvider.VSO_AUTH_URL) ||
                    vsoDeploymentContext == null || vsoDeploymentContext.getType() == ServerContext.Type.TFS) {
                // We aren't authenticated, or we couldn't find the VSO context
                logger.warn("doWork unexpected server context, expected type VSO or VSO_DEPLOYMENT. Found: {}", vsoDeploymentContext);
                throw new TeamServicesException(TeamServicesException.KEY_VSO_AUTH_FAILED);
            }

            //Get uris for the accounts the user has access to
            final List<String> accountUris = this.getAccountUris(vsoDeploymentContext);
            //Loop thru results and add them to the server context
            final AccountLookupResults results = new AccountLookupResults();
            for (final String accountUri : accountUris)
            {
                // Each account gets it's own context (i.e. azure.com/account1, azure.com/account2, etc..)
                final ServerContext accountContext =
                        new ServerContextBuilder().type(ServerContext.Type.VSO)
                                .accountUri(accountUri)
                                .authentication(VsoAuthenticationProvider.getInstance().getAuthenticationInfo(VsoAuthenticationProvider.VSO_AUTH_URL))
                                .userId(vsoDeploymentContext.getUserId())
                                .build();
                results.serverContexts.add(accountContext);
            }
            onLookupResults(results);
            onLookupCompleted();
        } catch (Throwable ex) {
            if (AuthHelper.isNotAuthorizedError(ex)) {
                final ServerContext context = ServerContextManager.getInstance().updateAuthenticationInfo(VsoAuthenticationProvider.VSO_AUTH_URL);
                if (context == null) {
                    //user might have canceled login dialog
                    terminate(ex);
                } else {
                    doWork(inputs);
                }
            } else {
                terminate(ex);
            }
        }
    }

    @Override
    public void cancel() {
        super.cancel();

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

    // Make a server call to get the uris for the accounts the user has access to
    private List<String> getAccountUris(final ServerContext vsoDeploymentContext) {
        // new list of account uris to return
        List<String> accountUris = new ArrayList<String>();

        // Issue account request
        final Client accountClient = vsoDeploymentContext.getClient();
        final String accountApiUrlFormat = VsoAuthenticationProvider.VSO_AUTH_URL + ACCOUNT_ENDPOINT;
        final WebTarget resourceTarget = accountClient.target(String.format(accountApiUrlFormat, vsoDeploymentContext.getUserId(), TFS_API_VERSION, TFS_SERVICE_URL_PROPERTY_NAME));
        final Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("charset", "UTF-8");
        final Invocation invocation = resourceTarget.request(
                new MediaType("application", "json", parameters))
                .buildGet();
        final String response = invocation.invoke(String.class);

        // Parse result tree
        final ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = null;
        try {
            rootNode = objectMapper.readTree(response);
        } catch (IOException e) {
            logger.error("Could not parse Account response", e);
            throw new TeamServicesException(TeamServicesException.KEY_VSO_AUTH_FAILED, e);
        }

        // Loop thru account results and add them to the list
        final List<JsonNode> nodes = rootNode.findValues(TFS_SERVICE_URL_PROPERTY_NAME);
        for (final JsonNode node : nodes)
        {
            accountUris.add(StringUtils.removeEnd(node.path("$value").asText(), "/"));
        }
        return accountUris;
    }
}
