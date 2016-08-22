// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.external.utils.WorkspaceHelper;
import org.apache.commons.lang.StringUtils;

/**
 * This command allows you to add or remove workspace mappings
 * <p/>
 * workfold /cloak [/collection:<url>] [/workspace:<value>] <serverFolder>|<localFolder>
 * workfold /decloak [/collection:<url>] [/workspace:<value>] <serverFolder>|<localFolder>
 * workfold /unmap [/collection:<url>] [/workspace:<value>] <serverFolder>|<localFolder>
 * workfold [/map] [/collection:<url>] [/workspace:<value>] <serverFolder> <localFolder>
 */
public class UpdateWorkspaceMappingCommand extends Command<String> {
    private final Workspace.Mapping mapping;
    private final boolean removeMapping;
    private final String workspaceName;

    private static final String SUB_MAP = "-map";
    private static final String SUB_CLOAK = "-cloak";
    private static final String SUB_UNMAP = "-unmap";
    private static final String SUB_DECLOAK = "-decloak";

    /**
     * Constructor
     *
     * @param context       This is the server context used for collection and login info (can be null)
     * @param workspaceName This is the name of the workspace to update
     * @param mapping       This is the mapping object with all the properties needed
     * @param removeMapping This indicates whether you are adding or removing this mapping
     */
    public UpdateWorkspaceMappingCommand(final ServerContext context, final String workspaceName, final Workspace.Mapping mapping, final boolean removeMapping) {
        super("workfold", context);
        ArgumentHelper.checkNotEmptyString(workspaceName);
        ArgumentHelper.checkNotNull(mapping, "mapping");
        this.workspaceName = workspaceName;
        this.mapping = mapping;
        this.removeMapping = removeMapping;
    }

    // There are 4 possible subcommands: map, cloak, unmap, and decloak (the latter 2 remove the mapping)
    private String getSubCommand() {
        if (mapping.isCloaked() && removeMapping) {
            return SUB_DECLOAK;
        } else if (!mapping.isCloaked() && removeMapping) {
            return SUB_UNMAP;
        } else if (mapping.isCloaked() && !removeMapping) {
            return SUB_CLOAK;
        } else { // if (!mapping.isCloaked() && !removeMapping) {
            return SUB_MAP;
        }
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        final String subCommand = getSubCommand();
        final ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder()
                .add(subCommand)
                .add("-workspace:" + workspaceName);
        if (removeMapping) {
            // When removing a mapping, it cannot contain the "/*" notation at the end
            builder.add(WorkspaceHelper.getNormalizedServerPath(mapping.getServerPath()));
        } else {
            builder.add(mapping.getServerPath());
        }
        if (StringUtils.equalsIgnoreCase(subCommand, SUB_MAP)) {
            builder.add(mapping.getLocalPath());
        }
        return builder;
    }

    /**
     * There is no output from this command unless there is an error. This method parses the error and throws if
     * one exists.
     */
    @Override
    public String parseOutput(final String stdout, final String stderr) {
        super.throwIfError(stderr);
        // There is no output on success
        return StringUtils.EMPTY;
    }
}
