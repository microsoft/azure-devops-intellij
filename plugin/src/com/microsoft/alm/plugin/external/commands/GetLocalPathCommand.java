// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import org.apache.commons.lang.StringUtils;

/**
 * Use this command to get the local path of a file
 * It will return the original path if the file was renamed
 */
public class GetLocalPathCommand extends Command<String> {
    private final String serverPath;
    private final String workspace;

    public GetLocalPathCommand(final ServerContext context, final String serverPath, final String workspace) {
        super("resolvePath", context);
        ArgumentHelper.checkNotEmptyString(serverPath);
        this.serverPath = serverPath;
        this.workspace = workspace;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        final ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder()
                .add(serverPath);
        if (StringUtils.isNotEmpty(workspace)) {
            builder.add("-workspace:" + workspace);
        }
        return builder;
    }

    /**
     * Returns the path to the file locally
     */
    @Override
    public String parseOutput(final String stdout, final String stderr) {
        super.throwIfError(stderr);
        return stdout;
    }
}
