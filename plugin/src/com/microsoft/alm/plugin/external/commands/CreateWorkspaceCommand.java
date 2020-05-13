// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.exceptions.WorkspaceAlreadyExistsException;
import com.microsoft.alm.plugin.external.models.Workspace;
import org.apache.commons.lang.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This command creates a new workspace.
 * <p/>
 * workspace /new [/noprompt] [/template:<value>] [/computer:<value>] [/comment:<value>|@valuefile] [/collection:<url>] [/location:server|local]
 * [/filetime:current|checkin] [/permission:Private|PublicLimited|Public] [<workspacename;[workspaceowner]>]
 */
public class CreateWorkspaceCommand extends Command<String> {
    private static final String WORKSPACE_EXISTS_ERROR = "An error occurred: The workspace %s;.* already exists on computer .*";

    private final String workspaceName;
    private final String comment;
    private final Workspace.FileTime fileTime;
    private final Workspace.Permission permission;
    private final Workspace.Location location;

    /**
     * Creates a command that will checkout a project to a workspace of selected kind when called.
     *
     * @param context       This is the server context used for collection and login info (can be null)
     * @param workspaceName This is the current name of the workspace to update
     * @param location      workspace location type: either {@link Workspace.Location#LOCAL} or
     *                      {@link Workspace.Location#SERVER}.
     */
    public CreateWorkspaceCommand(
            ServerContext context,
            String workspaceName,
            String comment,
            Workspace.FileTime fileTime,
            Workspace.Permission permission,
            Workspace.Location location) {
        super("workspace", context);
        ArgumentHelper.checkNotEmptyString(workspaceName, "workspaceName");
        this.workspaceName = workspaceName;
        this.comment = comment;
        this.fileTime = fileTime;
        this.permission = permission;
        this.location = location;
    }

    /**
     * Creates a command that will checkout a project to a local workspace when called.
     *
     * @param context       This is the server context used for collection and login info (can be null)
     * @param workspaceName This is the current name of the workspace to update
     */
    public CreateWorkspaceCommand(
            ServerContext context,
            String workspaceName,
            String comment,
            Workspace.FileTime fileTime,
            Workspace.Permission permission) {
        this(context, workspaceName, comment, fileTime, permission, Workspace.Location.LOCAL);
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        final ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder()
                .addSwitch("new")
                .addSwitch("location", location.toParameterString())
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
        throwIfError(stderr);
        // There is no useful output on success
        return StringUtils.EMPTY;
    }

    /**
     * Parse for specific error messages from CreateWorkspace command
     *
     * @param stderr
     */
    @Override
    protected void throwIfError(final String stderr) {
        if (StringUtils.isNotEmpty(stderr)) {
            final Pattern pattern = Pattern.compile(String.format(WORKSPACE_EXISTS_ERROR, workspaceName));
            final Matcher matcher = pattern.matcher(stderr);
            if (matcher.find())
                throw new WorkspaceAlreadyExistsException(workspaceName);
        }
        super.throwIfError(stderr);
    }
}