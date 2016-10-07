// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.ToolVersion;
import org.apache.commons.lang.StringUtils;

/**
 * This command calls the command line doing a simple call to get the help for the add command.
 * The first line of all commands is the version info...
 * Team Explorer Everywhere Command Line Client (version 14.0.3.201603291047)
 */
public class TfVersionCommand extends Command<ToolVersion> {
    private static final String VERSION_PREFIX = "(version ";

    public TfVersionCommand() {
        super("add", null);
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        final ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder()
                .addSwitch("?");
        return builder;
    }

    @Override
    public ToolVersion parseOutput(final String stdout, final String stderr) {
        throwIfError(stderr);
        final String[] lines = getLines(stdout);
        for(final String line : lines) {
            if (StringUtils.isNotEmpty(line)) {
                final int start = line.indexOf(VERSION_PREFIX);
                if (start >= 0) {
                    return new ToolVersion(StringUtils.removeEnd(line.substring(start + VERSION_PREFIX.length()), ")"));
                }
            }
        }

        return ToolVersion.UNKNOWN;
    }
}
