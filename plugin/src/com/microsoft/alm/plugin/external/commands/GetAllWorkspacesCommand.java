// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.Server;
import com.microsoft.alm.plugin.external.models.Workspace;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This command calls Workspaces to get all the user's workspaces
 * <p>
 * workspaces  [-owner:<value>] [-computer:<value>] [-collection:<url>] [-format:brief|detailed|xml]
 * [-updateUserName:<user@domain>|<domain\\user>] [-updateComputerName:<value>] workspaceName
 */
public class GetAllWorkspacesCommand extends Command<List<Server>> {
    private static final String COLLECTION_PREFIX = "Collection: ";

    private final AuthenticationInfo authInfo;
    private final String serverUrl;

    /**
     * Use this constructor if you want to get all the local workspaces stored
     * in the cache irregardless of the server
     */
    public GetAllWorkspacesCommand() {
        this(null);
    }

    /**
     * Use this constructor if you want to get all the local workspaces
     * from a specific server using a server call
     *
     * @param context
     */
    public GetAllWorkspacesCommand(final ServerContext context) {
        super("workspaces", context);
        authInfo = null;
        serverUrl = StringUtils.EMPTY;
    }

    /**
     * Use this constructor if you want to get all the local workspaces
     * from a specific server using a server call but don't have a server context
     *
     * @param authInfo
     * @param serverUrl
     */
    public GetAllWorkspacesCommand(final AuthenticationInfo authInfo, final String serverUrl) {
        super("workspaces", null);
        ArgumentHelper.checkNotNull(authInfo, "authInfo");
        ArgumentHelper.checkNotEmptyString(serverUrl, "serverUrl");
        this.authInfo = authInfo;
        this.serverUrl = serverUrl;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        final ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder();

        // if you have one you should have the other based on the constructor checks
        if (authInfo != null && StringUtils.isNotEmpty(serverUrl)) {
            builder.addAuthInfo(authInfo);
            builder.addSwitch("collection", serverUrl);
        }

        return builder;
    }

    /**
     * Returns a list of servers that the user has created workspaces from. The servers contain a list of
     * local workspaces that were created on the local machine
     * <p>
     * Collection: https://account3.visualstudio.com/
     * Workspace      Owner      Computer     Comment
     * -------------- ---------- ------------ ------------------------------------------------------------------------------------------------------------------------------------------------
     * TFVC_11        John Smith computerName Workspace created through IntelliJ
     * TFVC_11111     John Smith computerName Workspace created through IntelliJ
     * TFVC_11dfdfdf  John Smith computerName Workspace created through IntelliJ
     * <p>
     * Collection: https://demo.visualstudio.com/
     * Workspace Owner      Computer     Comment
     * --------- ---------- ------------ ---------------------------------------------------------------------------------------------------------------------------------------------------
     * newWorksp John Smith computerName
     *
     * @param stdout
     * @param stderr
     * @return
     */
    @Override
    public List<Server> parseOutput(final String stdout, final String stderr) {
        super.throwIfError(stderr);

        final String[] output = getLines(stdout);
        final List<Server> servers = new ArrayList<Server>();
        int count = 0;

        while (count < output.length) {
            if (StringUtils.startsWith(output[count], COLLECTION_PREFIX)) {
                final List<Workspace> workspaces = new ArrayList<Workspace>();
                final String serverName = StringUtils.removeStart(output[count], COLLECTION_PREFIX);

                // move past collection name and column headers
                count = count + 2;

                // double check we aren't out of bounds for the array
                if (count >= output.length) {
                    break;
                }

                // get the size of the columns from the number of dashes
                final String[] dashes = StringUtils.split(output[count], " ");
                int lastIndexName = dashes.length > 0 ? dashes[0].length() : 0;
                int lastIndexOwner = dashes.length > 1 ? lastIndexName + dashes[1].length() + 1 : 0;
                int lastIndexComputer = dashes.length > 2 ? lastIndexOwner + dashes[2].length() + 1 : 0;

                // move past dashes
                count++;

                // loop through the following lines to get the associated workspaces until an empty line is met
                while (count < output.length && !StringUtils.isEmpty(output[count])) {
                    final Workspace workspace = new Workspace(serverName,
                            StringUtils.substring(output[count], 0, lastIndexName).trim(),
                            StringUtils.substring(output[count], lastIndexOwner, lastIndexComputer).trim(),
                            StringUtils.substring(output[count], lastIndexName, lastIndexOwner).trim(),
                            StringUtils.substring(output[count], lastIndexComputer).trim(),
                            Collections.<Workspace.Mapping>emptyList());
                    workspaces.add(workspace);
                    count++;
                }
                servers.add(new Server(serverName, workspaces));
            }
            count++;
        }
        return servers;
    }
}