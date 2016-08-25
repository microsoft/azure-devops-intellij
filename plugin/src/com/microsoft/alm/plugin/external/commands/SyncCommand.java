// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.intellij.openapi.vcs.VcsException;
import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * This command gets either the latest version or a specified version of one or more files or folders
 * <p/>
 * tf get [itemspec] [/version:versionspec] [/all] [/overwrite] [/force] [/remap]
 * [/recursive] [/preview] [/noautoresolve] [/noprompt]
 * [/login:username,[password]]
 */
public class SyncCommand extends Command<String> {
    public static final String WARNING_PREFIX = "Warning";

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
        builder.addSwitch("force");
        return builder;
    }

    /**
     * There is no useful output from this command unless there is an error which we will throw
     *
     * @param stdout
     * @param stderr
     * @return
     */
    @Override
    public String parseOutput(final String stdout, final String stderr) {
        super.throwIfError(stderr);
        return StringUtils.EMPTY;
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
     * @param e
     * @return
     */
    public static List<VcsException> getFormattedExceptions(final Exception e) {
        final List<VcsException> exceptions = new ArrayList<VcsException>();

        final String[] lines = e.getMessage().replace("\r\n", "\n").split("\n");
        for (int i = lines.length / 2; i < lines.length; i++) {
            final VcsException exception = new VcsException((lines[i]));
            exception.setIsWarning(StringUtils.startsWith(lines[i], WARNING_PREFIX));
            exceptions.add(exception);
        }
        return exceptions;
    }
}
