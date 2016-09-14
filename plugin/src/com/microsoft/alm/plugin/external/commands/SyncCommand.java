// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.intellij.openapi.vcs.VcsException;
import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.SyncResults;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This command gets either the latest version or a specified version of one or more files or folders
 * <p/>
 * tf get [itemspec] [/version:versionspec] [/all] [/overwrite] [/force] [/remap]
 * [/recursive] [/preview] [/noautoresolve] [/noprompt]
 * [/login:username,[password]]
 */
public class SyncCommand extends Command<SyncResults> {
    private static final Logger logger = LoggerFactory.getLogger(SyncCommand.class);

    private static final String UP_TO_DATE_MSG = "All files up to date.";
    private static final String WARNING_PREFIX = "Warning";
    private static final String CONFLICT_MESSAGE = "you have a conflicting";
    private static final String SUMMARY_PREFIX = "---- Summary:";
    private static final String NEW_FILE_PREFIX = "Getting ";
    private static final String UPDATED_FILE_PREFIX = "Replacing ";
    private static final String DELETED_FILE_PREFIX = "Deleting ";

    private final List<String> updatePaths;
    private final boolean recursive;

    public SyncCommand(final ServerContext context, final List<String> updatePaths, final boolean recursive) {
        super("get", context);
        ArgumentHelper.checkNotNullOrEmpty(updatePaths, "updatePaths");
        this.updatePaths = updatePaths;
        this.recursive = recursive;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder();
        for (final String file : updatePaths) {
            builder.add(file);
        }
        if (recursive) {
            builder.addSwitch("recursive");
        }
        return builder;
    }

    /**
     * Example output from TF Get:
     * D:\tmp\test:
     * Getting addFold
     * Getting addFold-branch
     * <p/>
     * D:\tmp\test\addFold-branch:
     * Getting testHereRename.txt
     * <p/>
     * D:\tmp\test\addFold:
     * Getting testHere3
     * Getting testHereRename7.txt
     * <p/>
     * D:\tmp\test:
     * Getting Rename2.txt
     * Getting test3.txt
     * Conflict test_renamed.txt - Unable to perform the get operation because you have a conflicting rename, edit
     * Getting TestAdd.txt
     * <p/>
     * ---- Summary: 1 conflicts, 0 warnings, 0 errors ----
     * Conflict D:\tmp\test\test_renamed.txt - Unable to perform the get operation because you have a conflicting rename, edit
     *
     * @param stdout
     * @param stderr
     * @return
     */
    @Override
    public SyncResults parseOutput(final String stdout, final String stderr) {
        final List<String> updatedFiles = new ArrayList<String>();
        final List<String> newFiles = new ArrayList<String>();
        final List<String> deletedFiles = new ArrayList<String>();
        final List<VcsException> exceptions = new ArrayList<VcsException>();

        if (StringUtils.contains(stdout, UP_TO_DATE_MSG)) {
            return new SyncResults();
        }

        // make note that conflicts exist but to get conflicts use resolve command
        final boolean conflictsExist = StringUtils.contains(stderr, CONFLICT_MESSAGE);

        // parse the exception to get individual exceptions instead of 1 large one
        exceptions.addAll(parseException(stderr));

        // parse output for file changes
        final String[] lines = getLines(stdout);
        String path = StringUtils.EMPTY;
        for (final String line : lines) {
            if (StringUtils.isNotEmpty(line) || StringUtils.startsWith(line, SUMMARY_PREFIX)) {
                if (isFilePath(line)) {
                    path = getFilePath(line, StringUtils.EMPTY, StringUtils.EMPTY);
                } else if (StringUtils.startsWith(line, NEW_FILE_PREFIX)) {
                    newFiles.add((new File(path, line.replaceFirst(NEW_FILE_PREFIX, StringUtils.EMPTY)).getPath()));
                } else if (StringUtils.startsWith(line, UPDATED_FILE_PREFIX)) {
                    updatedFiles.add((new File(path, line.replaceFirst(UPDATED_FILE_PREFIX, StringUtils.EMPTY)).getPath()));
                } else if (StringUtils.startsWith(line, DELETED_FILE_PREFIX)) {
                    deletedFiles.add((new File(path, line.replaceFirst(DELETED_FILE_PREFIX, StringUtils.EMPTY)).getPath()));
                } else {
                    // TODO: check for other cases to cover here but no need to hinder user if case not covered
                    logger.warn("Unknown response from 'tf get' command: " + line);
                }
            }
        }

        return new SyncResults(conflictsExist, updatedFiles, newFiles, deletedFiles, exceptions);
    }

    /**
     * An error will be in the following form where there are duplicates for each error. The duplicates only differ
     * by the fact that the first reference of the error refers to only the file name and the second reference refers
     * to the entire path of the file. All the file name errors are the first listed followed by all the path errors.
     * Only make exceptions out of the path errors and ignore the file name errors so we see no duplicates. Example
     * of an error message will look like this:
     * <p/>
     * Warning - Unable to refresh testHereRename.txt because you have a pending edit.
     * Conflict TestAdd.txt - Unable to perform the get operation because you have a conflicting edit
     * Warning - Unable to refresh /Users/user/tfvc-tfs/tfsTest_01/addFold/testHereRename.txt because you have a pending edit.
     * Conflict /Users/user/tfvc-tfs/tfsTest_01/TestAdd.txt - Unable to perform the get operation because you have a conflicting edit
     *
     * @param stderr
     * @return
     */
    private List<VcsException> parseException(final String stderr) {
        final List<VcsException> exceptions = new ArrayList<VcsException>();

        final String[] exceptionLines = getLines(stderr);
        for (int i = exceptionLines.length / 2; i < exceptionLines.length; i++) {
            // skip empty lines and don't treat conflicts as exceptions
            if (StringUtils.isNotEmpty(exceptionLines[i]) && !StringUtils.contains(exceptionLines[i], CONFLICT_MESSAGE)) {
                //TODO: what if warning is that file was skipped (but only shows up when force was used)
                final VcsException exception = new VcsException((exceptionLines[i]));
                exception.setIsWarning(StringUtils.startsWith(exceptionLines[i], WARNING_PREFIX));
                exceptions.add(exception);
            }
        }
        return exceptions;
    }

    /**
     * Override return code in the cases where partial success (1) was seen
     * This occurs in the case where conflicts exists
     *
     * @param returnCode
     * @return
     */
    @Override
    public int interpretReturnCode(final int returnCode) {
        return returnCode == 1 ? 0 : returnCode;
    }
}