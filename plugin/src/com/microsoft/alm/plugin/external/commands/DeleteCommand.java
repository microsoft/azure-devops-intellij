// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.helpers.Path;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class DeleteCommand extends Command<List<String>> {
    private final List<String> filePaths;
    private final boolean recursive;
    private final String workspace;

    public DeleteCommand(ServerContext context, List<String> filePaths, String workspace, boolean recursive) {
        super("delete", context);
        ArgumentHelper.checkNotNullOrEmpty(filePaths, "filePaths");
        this.filePaths = filePaths;
        this.workspace = workspace;
        this.recursive = recursive;
    }

    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder();

        for (final String file : filePaths) {
            builder.add(file);
        }

        if (this.recursive) {
            builder.addSwitch("recursive");
        }

        if (StringUtils.isNotEmpty(workspace)) {
            builder.addSwitch("workspace", workspace);
        }

        return builder;
    }

    /**
     * Example
     * tf delete dir/file.txt
     * <p>
     * dir:
     * file.txt
     *
     * @param stdout
     * @param stderr
     * @return
     */
    public List<String> parseOutput(String stdout, String stderr) {
        super.throwIfError(stderr);
        final String[] lines = getLines(stdout);
        final List<String> deletedFiles = new ArrayList<String>();
        String path = StringUtils.EMPTY;

        for (final String line : lines) {
            if (line.endsWith(":")) {
                path = line.substring(0, line.length() - 1);
            } else if (StringUtils.isNotEmpty(line)) {
                deletedFiles.add(Path.combine(path, line));
            }
        }

        return deletedFiles;
    }
}