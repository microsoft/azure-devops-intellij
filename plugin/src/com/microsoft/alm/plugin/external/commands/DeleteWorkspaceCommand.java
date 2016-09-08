// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import org.apache.commons.lang.StringUtils;

/**
 * This command deletes a workspace.
 * No error is reported if the workspace does not exist.
 * <p/>
 * workspace /delete [/collection:<url>] [<workspacename;[workspaceowner]>]
 */
public class DeleteWorkspaceCommand extends Command<String> {
    private final String workspaceName;

    /**
     * Constructor
     *
     * @param context       This is the server context used for collection and login info (can be null)
     * @param workspaceName This is the current name of the workspace to delete
     */
    public DeleteWorkspaceCommand(final ServerContext context, final String workspaceName) {
        super("workspace", context);
        ArgumentHelper.checkNotEmptyString(workspaceName);
        this.workspaceName = workspaceName;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        final ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder()
                .addSwitch("delete")
                .add(workspaceName);
        return builder;
    }


    /**
     * There is no useful output from this command unless there is an error. This method parses the error and throws if
     * one exists.
     */
    @Override
    public String parseOutput(final String stdout, final String stderr) {
        // First check stderr for "not found" message
        if (StringUtils.containsIgnoreCase(stderr, "could not be found")) {
            // No workspace existed, so ignore the error
            return StringUtils.EMPTY;
        }
        // Throw if there was any other error
        super.throwIfError(stderr);

        // There is no useful output on success
        return StringUtils.EMPTY;
    }

    @Override
    public int interpretReturnCode(int returnCode) {
        // If the workspace wasn't found the exit code is 100. We want to ignore this error
        if (returnCode == 100) {
            return 0;
        }
        // Any other errors are not masked
        return returnCode;
    }
}
