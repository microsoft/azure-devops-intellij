// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.exceptions.ToolAuthenticationException;
import com.microsoft.alm.plugin.external.exceptions.WorkspaceCouldNotBeDeterminedException;
import com.microsoft.alm.plugin.external.models.Workspace;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * This command only returns a partial workspace object that allows you to get the name and server.
 * To get the entire workspace object you should call GetWorkspace with the workspace name.
 * (This is one of the only commands that expects to be a strictly local operation - no server calls - and so does not
 * take a server context object in the constructor)
 */
public class FindWorkspaceCommand extends Command<Workspace> {
    protected static final Logger logger = LoggerFactory.getLogger(FindWorkspaceCommand.class);

    protected static final String AUTH_ERROR_SERVER = "An error occurred: Access denied connecting to TFS server";
    protected static final String AUTH_ERROR_FEDERATED = "Federated authentication to this server requires a username and password.";
    private static final String WORKSPACE_COULD_NOT_BE_DETERMINED = "The workspace could not be determined from any argument paths or the current working directory.";

    private final String localPath;
    private final String collection;
    private final String workspace;
    private final AuthenticationInfo authInfo;

    public FindWorkspaceCommand(final String localPath) {
        super("workfold", null);
        ArgumentHelper.checkNotEmptyString(localPath, "localPath");
        this.localPath = localPath;
        this.collection = StringUtils.EMPTY;
        this.workspace = StringUtils.EMPTY;
        this.authInfo = null;
    }

    public FindWorkspaceCommand(final String collection, final String workspace, final AuthenticationInfo authInfo) {
        super("workfold", null);
        ArgumentHelper.checkNotEmptyString(collection, "collection");
        ArgumentHelper.checkNotEmptyString(workspace, "workspace");
        this.collection = collection;
        this.workspace = workspace;
        this.authInfo = authInfo;
        this.localPath = StringUtils.EMPTY;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        final ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder();
        if (StringUtils.isNotEmpty(localPath)) {
            // To find the workspace we set the working directory to the localPath and call workfold with no arguments
            // NOTE Calling workfold with the localPath forces it to refresh the workspace from the server. Calling it with
            //      no arguments does not refresh it from the server.
            builder.setWorkingDirectory(localPath);
            // TODO: fix CLC to not need login creds for this command. It never validates them so we pass anything
            builder.addSwitch("login", "username,pw", true);
        } else if (StringUtils.isNotEmpty(collection) && StringUtils.isNotEmpty(workspace)) {
            // need both collection and workspace name to make this call local
            builder.addSwitch("collection", UrlHelper.getCmdLineFriendlyUrl(collection));
            builder.addSwitch("workspace", workspace);
        }

        if (authInfo != null) {
            builder.addAuthInfo(authInfo);
        }

        return builder;
    }

    /**
     * Parses the output of the workfold command. (NOT XML)
     * SAMPLE
     * Access denied connecting to TFS server https://organization.visualstudio.com/ (authenticating as Personal Access Token)  <-- line is optional
     * =====================================================================================================================================================
     * Workspace:  MyNewWorkspace2
     * Collection: http://java-tfs2015:8081/tfs/
     * $/tfsTest_01: D:\tmp\test
     */
    @Override
    public Workspace parseOutput(final String stdout, final String stderr) {
        throwIfError(stderr);

        final String[] lines = getLines(stdout);

        for (int x = 0; x < lines.length; x++) {
            if (lines.length > x + 2 && lines[x].contains("==========")) {
                // Get the name and url from the next 2 lines
                final String workspaceName = getValue(lines[x + 1]);
                final String collectionURL = getValue(lines[x + 2]);
                // Finally, parse the workspace mappings
                final List<Workspace.Mapping> mappings = new ArrayList<Workspace.Mapping>(10);
                for (int i = x + 3; i < lines.length; i++) {
                    Workspace.Mapping mapping = getMapping(lines[i]);
                    if (mapping != null) {
                        mappings.add(mapping);
                    }
                }
                return new Workspace(collectionURL, workspaceName, "", "", "", mappings);
            }
        }

        return null;
    }

    /**
     * This method parses a single line of output splitting on ": " and returning the value after the colon
     * Example:  "   Name: myWorkspace"  => "myWorkspace"
     */
    private String getValue(final String line) {
        final int index = line.indexOf(": ");
        return line.substring(index + 2).trim();
    }

    /**
     * Check specifically to see if an authentication exception is thrown
     *
     * @param stderr
     */
    @Override
    protected void throwIfError(final String stderr) {
        if (StringUtils.startsWith(stderr, AUTH_ERROR_SERVER) || StringUtils.contains(stderr, AUTH_ERROR_FEDERATED)) {
            logger.warn("Authentication exception hit when running 'tf workfold'");
            throw new ToolAuthenticationException();
        } else if (StringUtils.contains(stderr, WORKSPACE_COULD_NOT_BE_DETERMINED)) {
            throw new WorkspaceCouldNotBeDeterminedException();
        }
        super.throwIfError(stderr);
    }
}
