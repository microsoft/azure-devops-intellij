// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.idea.tfvc.core.TfvcDeleteResult;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import com.microsoft.tfs.model.connector.TfsLocalPath;
import com.microsoft.tfs.model.connector.TfsPath;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang.StringUtils;

import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeleteCommand extends Command<TfvcDeleteResult> {
    private final List<String> filePaths;
    private final boolean recursive;
    private final String workspace;

    private static final Pattern FILE_NOT_FOUND_PATTERN =
            Pattern.compile("^No matching items found in (.*?) in your workspace, or you do not have permission to access them.$");
    private static final String NO_FILES_TO_DELETE = "No arguments matched any files to delete.";

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

    private static void parseStdErr(String stderr, List<TfsPath> notFoundFiles, List<String> errorMessages) {
        String[] lines = stderr.split("\r\n|\n");
        for (String line : lines) {
            if (StringUtils.isEmpty(line))
                continue;

            if (line.equals(NO_FILES_TO_DELETE)) // skip this warning, we'll report each item separately
                continue;

            Matcher notFoundMatcher = FILE_NOT_FOUND_PATTERN.matcher(line);
            if (notFoundMatcher.matches()) {
                String filePath = notFoundMatcher.group(1);
                TfsLocalPath localPath = TfsFileUtil.createLocalPath(filePath);
                notFoundFiles.add(localPath);
            } else {
                errorMessages.add(line);
            }
        }
    }

    private void parseStdOut(String stdout, List<java.nio.file.Path> deletedPaths) {
        final String[] lines = getLines(stdout);
        String path = StringUtils.EMPTY;

        for (final String line : lines) {
            if (StringUtils.isEmpty(line))
                continue;

            if (line.endsWith(":")) {
                path = line.substring(0, line.length() - 1);
            } else {
                deletedPaths.add(Paths.get(path, line));
            }
        }
    }

    /**
     * Example command line: {@code tf delete dir/file.txt}
     * <p>
     * Example stdout:<br>
     * <pre>{@code dir:
     * file.txt}</pre>
     * <p>
     * Example stderr:<br>
     * <pre>{@code The item C:\FullPath\dir\file.txt could not be found in your workspace, or you do not have permission to access it.}</pre>
     */
    public TfvcDeleteResult parseOutput(String stdout, String stderr) {
        List<java.nio.file.Path> deletedPaths = Lists.newArrayList();
        List<TfsPath> notFoundFiles = Lists.newArrayList();
        List<String> errorMessages = Lists.newArrayList();

        parseStdErr(stderr, notFoundFiles, errorMessages);
        parseStdOut(stdout, deletedPaths);

        return new TfvcDeleteResult(deletedPaths, notFoundFiles, errorMessages);
    }

    @Override
    protected boolean shouldThrowBadExitCode() {
        // This command parses all the errors and reports them as an object returned; no need to check the exit code.
        return false;
    }
}