// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TfsFileUtil;
import com.microsoft.tfs.model.connector.TfsLocalPath;
import com.microsoft.tfs.model.connector.TfvcCheckoutResult;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Makes one or more local files writable and creates "edit" pending changes for them in the current workspace.
 */
public class CheckoutCommand extends Command<TfvcCheckoutResult> {
    private final List<Path> myFilePaths;
    private final boolean myRecursive;

    private static final Pattern ITEM_NOT_FOUND_PATTERN = Pattern.compile(
            "^The item (.*?) could not be found in your workspace, or you do not have permission to access it\\.$");

    /**
     * Creates a command to check out the passed files.
     *
     * @param context   server context to extract the authentication information.
     * @param filePaths paths to the files to check out.
     * @param recursive whether the operation should be recursive.
     */
    public CheckoutCommand(
            @NotNull ServerContext context,
            @NotNull List<Path> filePaths,
            boolean recursive) {
        super("checkout", context);
        ArgumentHelper.checkNotNullOrEmpty(filePaths, "filePaths");
        myFilePaths = filePaths;
        myRecursive = recursive;
    }

    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder();
        if (myRecursive) {
            builder.addSwitch("recursive");
        }

        for (Path file : myFilePaths) {
            builder.add(file.toString());
        }

        return builder;
    }

    private static void parseStdErr(String stderr, List<TfsLocalPath> notFoundFiles, List<String> errorMessages) {
        String[] lines = stderr.split("\r\n|\n");
        for (String line : lines) {
            if (StringUtils.isEmpty(line))
                continue;

            Matcher notFoundMatcher = ITEM_NOT_FOUND_PATTERN.matcher(line);
            if (notFoundMatcher.matches()) {
                String filePath = notFoundMatcher.group(1);
                TfsLocalPath localPath = TfsFileUtil.createLocalPath(filePath);
                notFoundFiles.add(localPath);
            } else {
                errorMessages.add(line);
            }
        }
    }

    private void parseStdOut(String stdout, List<TfsLocalPath> checkedOutFiles) {
        final String[] lines = getLines(stdout);
        String path = StringUtils.EMPTY;

        for (final String line : lines) {
            if (StringUtils.isEmpty(line))
                continue;

            if (line.endsWith(":")) {
                path = line.substring(0, line.length() - 1);
            } else {
                checkedOutFiles.add(TfsFileUtil.createLocalPath(Paths.get(path, line)));
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
    @Override
    public TfvcCheckoutResult parseOutput(String stdout, String stderr) {
        List<TfsLocalPath> checkedOutFiles = Lists.newArrayList();
        List<TfsLocalPath> notFoundFiles = Lists.newArrayList();
        List<String> errorMessages = Lists.newArrayList();

        parseStdErr(stderr, notFoundFiles, errorMessages);
        parseStdOut(stdout, checkedOutFiles);

        return new TfvcCheckoutResult(checkedOutFiles, notFoundFiles, errorMessages);
    }

    @Override
    protected boolean shouldThrowBadExitCode() {
        // This command parses all the errors and reports them as an object returned; no need to check the exit code.
        return false;
    }
}