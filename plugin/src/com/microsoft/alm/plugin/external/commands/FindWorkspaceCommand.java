// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.Workspace;


public class FindWorkspaceCommand extends Command<Workspace> {
    private final String localPath;

    public FindWorkspaceCommand(final ServerContext context, final String localPath) {
        super("workfold", context);
        ArgumentHelper.checkNotEmptyString(localPath);
        this.localPath = localPath;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        final ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder()
                .add(localPath)
                .add("-noprompt");
        return builder;
    }

    /**
     * Parses the output of the workfold command. (NOT XML)
     * SAMPLE
     * =====================================================================================================================================================
     * Workspace:  MyNewWorkspace2
     * Collection: http://java-tfs2015:8081/tfs/
     * $/tfsTest_01: D:\tmp\test
     */
    @Override
    public Workspace parseOutput(final String stdout, final String stderr) {
        super.throwIfError(stderr);

        // look for the ==== line to see if the command was successful
        final String[] lines = getLines(stdout);
        if (lines.length > 2 && lines[0].contains("==========")) {
            final String workspaceName = getValue(lines[1]).trim();
            final String collectionURL = getValue(lines[2]);
            return new Workspace(collectionURL, workspaceName, "", "", "");
        }

        return null;
    }

    /**
     * This method parses a single line of output splitting on ": " and returning the value after the colon
     * Example:  "   Name: myWorkspace"  => "myWorkspace"
     */
    private String getValue(final String line) {
        int index = line.indexOf(": ");
        return line.substring(index + 2);
    }
}
