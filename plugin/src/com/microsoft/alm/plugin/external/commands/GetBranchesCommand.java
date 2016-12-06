// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * This command gets a list of branches for the given server path
 * <p/>
 * branches [/version:<value>] <itemSpec>...
 */
public class GetBranchesCommand extends Command<List<String>> {
    public static final Logger logger = LoggerFactory.getLogger(GetBranchesCommand.class);

    private static final String CURRENT_PREFIX = ">>";
    private static final String SERVER_PATH_PREFIX = "$";
    private static final String BRANCHED_FROM_PREFIX = "Branched from";

    private final String workingFolder;
    private final String source;

    public GetBranchesCommand(final ServerContext context, final String workingFolder, final String source) {
        super("branches", context);
        ArgumentHelper.checkNotEmptyString(workingFolder, "workingFolder");
        ArgumentHelper.checkNotEmptyString(source, "source");
        this.workingFolder = workingFolder;
        this.source = source;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder()
                .setWorkingDirectory(workingFolder);
        builder.add(source);
        return builder;
    }

    /**
     * Returns the list of branched items associated with the source item provided.
     * Note that the ">>" denotes the source item and the order and indentation denote the relationship.
     * The leading underscore is not actually part of the output (it just keeps the formatting correct.
     * <p/>
     * _ $/tfsTest_03/Folder1/model
     * _ >>      $/tfsTest_03/Folder1/model_01     Branched from version 8 <<
     * _              $/tfsTest_03/Folder1/model_01_copy Branched from version 207
     * _         $/tfsTest_03/Folder1/model_02     Branched from version 8
     * _         $/tfsTest_03/Folder1/model_03     Branched from version 8
     * _         $/tfsTest_03/Folder1/model_04     Branched from version 8
     * _         $/tfsTest_03/Folder2/model_05   Branched from version 8
     * _         $/tfsTest_03/model_branch_44    Branched from version 8
     * _         $/tfsTest_03/model_branch_06    Branched from version 8
     * _         $/tfsTest_03/Folder1/model_11     Branched from version 8
     * _         $/tfsTest_03/Folder333  Branched from version 8
     */
    @Override
    public List<String> parseOutput(final String stdout, final String stderr) {
        throwIfError(stderr);

        final String[] output = getLines(stdout);
        final List<String> result = new ArrayList<String>(output.length);

        for (String line : output) {
            // First see if this line holds the current item we passed in marked with >>
            // If it is the current item, then don't add it to the list
            if (!StringUtils.startsWithIgnoreCase(line, CURRENT_PREFIX)) {
                // Parse out the server path
                final String serverPath;
                final int branchedFromIndex = StringUtils.indexOf(line, BRANCHED_FROM_PREFIX);
                if (branchedFromIndex >= 0) {
                    serverPath = line.substring(0, branchedFromIndex).trim();
                } else {
                    serverPath = line.trim();
                }

                if (StringUtils.isNotBlank(line)) {
                    result.add(serverPath);
                }
            }
        }

        return result;
    }
}
