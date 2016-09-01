// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;

import java.util.ArrayList;
import java.util.List;

/**
 * This command finds conflicts existing in the workspace and previewing them
 * <p/>
 * tf resolve [itemspec]
 * [/auto:(AutoMerge|TakeTheirs|KeepYours|OverwriteLocal|DeleteConflict|KeepYoursRenameTheirs)]
 * [/preview] [(/overridetype:overridetype | /converttotype:converttype] [/recursive] [/newname:path] [/noprompt] [/login:username, [password]]
 */
public class FindConflictsCommand extends Command<List<String>> {
    public static final String WARNING_PREFIX = "Warning";

    private final String basePath;

    public FindConflictsCommand(final ServerContext context, final String basePath) {
        super("resolve", context);
        ArgumentHelper.checkNotNull(basePath, "basePath");
        this.basePath = basePath;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder();
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
    public List<String> parseOutput(final String stdout, final String stderr) {
        final List<String> conflicts = new ArrayList<String>();
        final String[] lines = getLines(stderr);

        for (final String line : lines) {
            final int index = line.indexOf(":");
            if (index != -1) {
                conflicts.add(line.substring(0, index));
            }
        }

        return conflicts;
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
