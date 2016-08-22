// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import org.apache.commons.lang.StringUtils;

/**
 * This command updates the workspace properties comment, name, filetime, and permission.
 * Any properties that are omitted will not be changed.
 * <p/>
 * workspace [/collection:<url>] [/comment:<value>|@valuefile] [/newname:<value>] [/filetime:current|checkin]
 * [/permission:Private|PublicLimited|Public] [<workspacename;[workspaceowner]>]
 */
public class UpdateWorkspaceCommand extends Command<String> {
    private final String currentWorkspaceName;
    private final String newName;
    private final String newComment;
    private final FileTime newFileTime;
    private final Permission newPermission;

    public enum FileTime {CURRENT, CHECKIN}

    public enum Permission {PRIVATE, PUBLIC_LIMITED, PUBLIC}

    /**
     * Constructor
     *
     * @param context              This is the server context used for collection and login info (can be null)
     * @param currentWorkspaceName This is the current name of the workspace to update
     */
    public UpdateWorkspaceCommand(final ServerContext context, final String currentWorkspaceName,
                                  final String newName, final String newComment, final FileTime newFileTime,
                                  final Permission newPermission) {
        super("workspace", context);
        ArgumentHelper.checkNotEmptyString(currentWorkspaceName);
        this.currentWorkspaceName = currentWorkspaceName;
        this.newName = newName;
        this.newComment = newComment;
        this.newFileTime = newFileTime;
        this.newPermission = newPermission;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        final ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder()
                .add(currentWorkspaceName);
        if (StringUtils.isNotEmpty(newName)) {
            builder.add("-newname:" + newName);
        }
        if (StringUtils.isNotEmpty(newComment)) {
            builder.add("-comment:" + newComment);
        }
        if (newFileTime != null) {
            builder.add("-filetime:" + getFileTimeValue());
        }
        if (newPermission != null) {
            builder.add("-permission:" + getPermissionValue());
        }

        return builder;
    }

    private String getFileTimeValue() {
        if (newFileTime == FileTime.CHECKIN) {
            return "checkin";
        } else {
            return "current";
        }
    }

    private String getPermissionValue() {
        if (newPermission == Permission.PRIVATE) {
            return "Private";
        } else if (newPermission == Permission.PUBLIC) {
            return "Public";
        } else {
            return "PublicLimited";
        }
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
