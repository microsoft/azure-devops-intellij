// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
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
    public ConflictResults parseOutput(final String stdout, final String stderr) {
        final List<String> contentConflicts = new ArrayList<String>();
        final List<String> renameConflicts = new ArrayList<String>();
        final List<String> bothConflicts = new ArrayList<String>();
        final String[] lines = getLines(stderr);

        for (final String line : lines) {
            // find last ue of colon because it can be included in the file path (i.e. C:\\Users\\user\\tfvc\\file.txt: The item content has changed)
            final int index = line.lastIndexOf(":");
            if (index != -1) {
                if (StringUtils.endsWith(line, BOTH_CONFLICTS_SUFFIX)) {
                    bothConflicts.add(line.substring(0, index));
                } else if (StringUtils.endsWith(line, RENAME_CONFLICT_SUFFIX)) {
                    renameConflicts.add(line.substring(0, index));
                } else {
                    contentConflicts.add(line.substring(0, index));
                }
            }
        }

        return new ConflictResults(contentConflicts, renameConflicts, bothConflicts);
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
