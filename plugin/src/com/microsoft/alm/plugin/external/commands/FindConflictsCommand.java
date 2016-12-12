// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.Conflict;
import com.microsoft.alm.plugin.external.models.ConflictResults;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * This command finds conflicts existing in the workspace and previewing them
 * <p/>
 * tf resolve [itemspec]
 * [/auto:(AutoMerge|TakeTheirs|KeepYours|OverwriteLocal|DeleteConflict|KeepYoursRenameTheirs)]
 * [/preview] [(/overridetype:overridetype | /converttotype:converttype] [/recursive] [/newname:path] [/noprompt] [/login:username, [password]]
 */
public class FindConflictsCommand extends Command<ConflictResults> {
    public static final String WARNING_PREFIX = "Warning";
    public static final String BOTH_CONFLICTS_SUFFIX = "The item name and content have changed";
    public static final String RENAME_CONFLICT_SUFFIX = "The item name has changed";
    public static final String MERGE_CONFLICT_SUFFIX = "The source and target both have changes";

    private final String basePath;
    private final String workingFolder;

    public FindConflictsCommand(final ServerContext context, final String basePath) {
        this(context, null, basePath);
    }

    public FindConflictsCommand(final ServerContext context, final String workingFolder, final String basePath) {
        super("resolve", context);
        ArgumentHelper.checkNotNull(basePath, "basePath");
        this.basePath = basePath;
        this.workingFolder = workingFolder;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder()
                .setWorkingDirectory(workingFolder);
        builder.add(basePath);
        builder.addSwitch("recursive");
        builder.addSwitch("preview");
        return builder;
    }

    /**
     * Outputs the conflicts found in the workspace in the following format:
     * <p/>
     * tfsTest_01/addFold/testHere2: The item content has changed
     * tfsTest_01/TestAdd.txt: The item content has changed
     *
     * @param stdout
     * @param stderr
     * @return
     */
    @Override
    public ConflictResults parseOutput(final String stdout, final String stderr) {
        final List<Conflict> conflicts = new ArrayList<Conflict>();
        final String[] lines = getLines(stderr);

        for (final String line : lines) {
            // find last use of colon because it can be included in the file path (i.e. C:\\Users\\user\\tfvc\\file.txt: The item content has changed)
            final int index = line.lastIndexOf(":");
            if (index != -1) {
                final String localPath = line.substring(0, index);
                if (StringUtils.endsWith(line, BOTH_CONFLICTS_SUFFIX)) {
                    conflicts.add(createNameAndContentConflict(localPath));
                } else if (StringUtils.endsWith(line, RENAME_CONFLICT_SUFFIX)) {
                    conflicts.add(createRenameConflict(localPath));
                } else if (StringUtils.endsWith(line, MERGE_CONFLICT_SUFFIX)) {
                    conflicts.add(createMergeConflict(localPath));
                } else {
                    conflicts.add(createContentConflict(localPath));
                }
            }
        }

        return new ConflictResults(conflicts);
    }

    private Conflict createRenameConflict(final String localPath) {
        return new Conflict(localPath, Conflict.ConflictType.RENAME);
    }

    private Conflict createContentConflict(final String localPath) {
        return new Conflict(localPath, Conflict.ConflictType.CONTENT);
    }

    private Conflict createNameAndContentConflict(final String localPath) {
        return new Conflict(localPath, Conflict.ConflictType.NAME_AND_CONTENT);
    }

    private Conflict createMergeConflict(final String localPath) {
        return new Conflict(localPath, Conflict.ConflictType.MERGE);
    }

    /**
     * Override return code in the cases where partial success (1) was seen
     * This occurs in the case where resolve is in preview and not completed
     *
     * @param returnCode
     * @return
     */
    @Override
    public int interpretReturnCode(final int returnCode) {
        return returnCode == 1 ? 0 : returnCode;
    }
}
