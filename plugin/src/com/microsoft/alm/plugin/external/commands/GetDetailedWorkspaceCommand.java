// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.Workspace;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * This command returns a fully populated workspace object but credentials are always required
 * <p>
 * For server workspaces a server call must be made and a collection url is required
 */
public class GetDetailedWorkspaceCommand extends Command<Workspace> {
    public static final String WORKSPACE_PREFIX = "Workspace:";
    public static final String OWNER_PREFIX = "Owner:";
    public static final String COMPUTER_PREFIX = "Computer:";
    public static final String COMMENT_PREFIX = "Comment:";
    public static final String COLLECTION_PREFIX = "Collection:";
    public static final String LOCATION_PREFIX = "Location:";
    public static final String MAPPING_PREFIX = "Working folders:";

    private final String workspaceName;
    private final String collection;
    private final AuthenticationInfo authInfo;

    /**
     * @param collectionDisplayName the collection display name, i.e. an URI with spaces represented by " " (literal
     *                              space) instead of "%20" (URL-encoded space).
     */
    public GetDetailedWorkspaceCommand(String collectionDisplayName, String workspace, AuthenticationInfo authInfo) {
        super("workspaces", null);
        ArgumentHelper.checkNotEmptyString(workspace, "workspace");
        ArgumentHelper.checkNotNull(authInfo, "authInfo");
        this.collection = collectionDisplayName;
        this.workspaceName = workspace;
        this.authInfo = authInfo;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        final ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder();
        builder.addSwitch("format", "detailed");
        builder.add(workspaceName);
        builder.addAuthInfo(authInfo);

        if (StringUtils.isNotEmpty(collection)) {
            builder.addSwitch("collection", collection);
        }
        return builder;
    }

    /**
     * Parses output for the workspace attributes
     * <p>
     * tf workspaces -format:detailed -collection:http://organization.visualstudio.com/ WorkspaceName
     * ===========================================================================================================================================================================================================
     * Workspace:   WorkspaceName
     * Owner:       John Smith
     * Computer:    computerName
     * Comment:     Workspace created through IntelliJ
     * Collection:  http://organization.visualstudio.com/
     * Permissions: Private
     * File Time:   Current
     * Location:    Local
     * File Time:   Current
     * <p>
     * Working folders:
     * <p>
     * $/WorkspaceName: /Users/JohnSmith/WorkspaceName
     *
     * @param stdout
     * @param stderr
     * @return
     */
    @Override
    public Workspace parseOutput(final String stdout, final String stderr) {
        super.throwIfError(stderr);

        // if for some reason no output is given return null
        if (StringUtils.isEmpty(stdout)) {
            return null;
        }

        final String[] output = getLines(stdout);
        String workspace = StringUtils.EMPTY;
        String owner = StringUtils.EMPTY;
        String computer = StringUtils.EMPTY;
        String comment = StringUtils.EMPTY;
        String location = StringUtils.EMPTY;
        String collection = StringUtils.EMPTY;
        final List<Workspace.Mapping> mappings = new ArrayList<Workspace.Mapping>();

        // the output should be in this order but just in case it changes we check the prefixes first
        int count = 0;
        while (count < output.length) {
            if (StringUtils.startsWith(output[count], WORKSPACE_PREFIX)) {
                workspace = StringUtils.removeStart(output[count], WORKSPACE_PREFIX).trim();
            } else if (StringUtils.startsWith(output[count], OWNER_PREFIX)) {
                owner = StringUtils.removeStart(output[count], OWNER_PREFIX).trim();
            } else if (StringUtils.startsWith(output[count], COMPUTER_PREFIX)) {
                computer = StringUtils.removeStart(output[count], COMPUTER_PREFIX).trim();
            } else if (StringUtils.startsWith(output[count], COMMENT_PREFIX)) {
                comment = StringUtils.removeStart(output[count], COMMENT_PREFIX).trim();
            } else if (StringUtils.startsWith(output[count], COLLECTION_PREFIX)) {
                collection = StringUtils.removeStart(output[count], COLLECTION_PREFIX).trim();
            } else if (StringUtils.startsWith(output[count], LOCATION_PREFIX)) {
                location = StringUtils.removeStart(output[count], LOCATION_PREFIX).trim();
            } else if (StringUtils.startsWith(output[count], MAPPING_PREFIX)) {
                count = count + 2;
                while (count < output.length && StringUtils.isNotEmpty(output[count])) {
                    Workspace.Mapping mapping = getMapping(output[count]);
                    if (mapping != null) {
                        mappings.add(mapping);
                    }
                    count++;
                }
            }
            count++;
        }
        return new Workspace(collection, workspace, computer, owner, comment, mappings, Workspace.Location.fromString(location));
    }
}