// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.Workspace;
import org.apache.commons.lang.StringUtils;

/**
 * This command creates a new workspace.
 * <p/>
 * workspace /new [/noprompt] [/template:<value>] [/computer:<value>] [/comment:<value>|@valuefile] [/collection:<url>] [/location:server|local]
 * [/filetime:current|checkin] [/permission:Private|PublicLimited|Public] [<workspacename;[workspaceowner]>]
 */
public class CreateWorkspaceCommand extends Command<String> {
    private final String workspaceName;
    private final String comment;
    private final Workspace.FileTime fileTime;
    private final Workspace.Permission permission;

    /**
     * Constructor
     *
     * @param context       This is the server context used for collection and login info (can be null)
     * @param workspaceName This is the current name of the workspace to update
     */
    public CreateWorkspaceCommand(final ServerContext context, final String workspaceName,
                                  final String comment, final Workspace.FileTime fileTime,
                                  final Workspace.Permission permission) {
        super("workspace", context);
        ArgumentHelper.checkNotEmptyString(workspaceName);
        this.workspaceName = workspaceName;
        this.comment = comment;
        this.fileTime = fileTime;
        this.permission = permission;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        final ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder()
                .addSwitch("new")
                .add(workspaceName);
        if (StringUtils.isNotEmpty(comment)) {
            builder.addSwitch("comment", comment);
        }
        if (fileTime != null) {
            builder.addSwitch("filetime", fileTime.toString());
        }
        if (permission != null) {
            builder.addSwitch("permission", permission.toString());
        }

        return builder;
    }


    /**
     * There is no useful output from this command unless there is an error. This method parses the error and throws if
     * one exists.
     */
    @Override
    public String parseOutput(final String stdout, final String stderr) {
        super.throwIfError(stderr);
        // There is no useful output on success
        return StringUtils.EMPTY;
    }
}
