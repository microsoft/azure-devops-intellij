// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.exceptions.TeamServicesException;
import com.microsoft.alm.plugin.external.ToolRunner;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * This command  undoes the changes to the files passed in.
 * It returns a list of all files undone.
 */
public class UndoCommand extends Command<List<String>> {
    public static final Logger logger = LoggerFactory.getLogger(UndoCommand.class);

    private static final String UNDO_LINE_PREFIX = "Undoing edit:";
    private static final String NO_PENDING_CHANGES_WERE_FOUND = "No pending changes were found for ";

    private final List<String> files;

    public UndoCommand(final ServerContext context, final List<String> files) {
        super("undo", context);
        ArgumentHelper.checkNotNullOrEmpty(files, "files");
        this.files = files;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder();
        for (final String file : files) {
            builder.add(file);
        }
        return builder;
    }

    /**
     * Returns the files that were undone
     * <p/>
     * Output example:
     * /path/path:
     * Undoing edit: file1.java
     * No pending changes were found for /path/path/file2.java.
     * <p/>
     */
    @Override
    public List<String> parseOutput(final String stdout, final String stderr) {
        final String[] output = getLines(stdout);

        // check for failure
        if (StringUtils.isNotEmpty(stderr)) {
            String[] errorLines = stderr.split("\n");
            boolean hasUnknownError = false;

            for (String errorLine : errorLines) {
                // No pending changes isn't an error: we'll just return empty list for this case.
                if (errorLine.startsWith(NO_PENDING_CHANGES_WERE_FOUND)) {
                    logger.warn(errorLine);
                } else {
                    hasUnknownError = true;
                    break;
                }
            }

            if (hasUnknownError) {
                logger.error("Undo failed with the following stderr: " + stderr);
                for (String s : output) {
                    // finding error message by eliminating all other known output lines since we can't parse for the error line itself (it's unknown to us)
                    if (StringUtils.isNotEmpty(s) && isOutputLineExpected(s, new String[]{UNDO_LINE_PREFIX}, true)) {
                        throw new RuntimeException(s);
                    }
                }
                // couldn't figure out error message parsing so returning generic error
                logger.error("Parsing of the stdout failed to get the error message");
                throw new TeamServicesException(TeamServicesException.KEY_ERROR_UNKNOWN);
            }
        }

        final List<String> filesUndone = new ArrayList<>();

        // parse output for directory paths and file names to combine
        String path = StringUtils.EMPTY;
        for (String s : output) {
            if (isFilePath(s)) {
                path = s;
            } else if (StringUtils.isNotEmpty(s)) {
                filesUndone.add(getFilePath(path, s, "")); //TODO: Need to pass in the path root
            }
        }

        return filesUndone;
    }
}
