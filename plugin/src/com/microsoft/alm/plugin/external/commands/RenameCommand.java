// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import org.apache.commons.lang.StringUtils;

/**
 * This command renames or moves a file
 * <p/>
 * tf rename [/lock:(none|checkout|checkin)] [/login:username,[password]] olditem newitem
 */
public class RenameCommand extends Command<String> {
    private final String oldName;
    private final String newName;

    public RenameCommand(final ServerContext context, final String oldName, final String newName) {
        super("rename", context);
        ArgumentHelper.checkNotEmptyString(oldName);
        ArgumentHelper.checkNotEmptyString(newName);

        this.oldName = oldName;
        this.newName = newName;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder();

        builder.add(oldName);
        builder.add(newName);

        return builder;
    }

    /**
     * There is no useful output from this command unless there is an error which we will throw
     */
    @Override
    public String parseOutput(final String stdout, final String stderr) {
        throwIfError(stderr);

        return StringUtils.EMPTY;
    }
}
