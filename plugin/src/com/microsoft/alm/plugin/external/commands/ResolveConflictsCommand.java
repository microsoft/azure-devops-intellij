// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.Conflict;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * This command resolves conflicts based on given auto resolve type
 * <p/>
 * tf resolve [itemspec]
 * [/auto:(AutoMerge|TakeTheirs|KeepYours|OverwriteLocal|DeleteConflict|KeepYoursRenameTheirs)]
 * [/preview] [(/overridetype:overridetype | /converttotype:converttype] [/recursive] [/newname:path] [/noprompt] [/login:username, [password]]
 */
public class ResolveConflictsCommand extends Command<List<Conflict>> {
    private static final String RESOLVED_PREFIX = "Resolved ";
    private static final String RESOLVED_POST_MSG = " as ";

    private final List<String> filesToResolve;
    private final AutoResolveType resolveType;

    public enum AutoResolveType {AutoMerge, TakeTheirs, KeepYours, OverwriteLocal, DeleteConflict, KeepYoursRenameTheirs}

    public ResolveConflictsCommand(final ServerContext context, final List<String> filesToResolve, final AutoResolveType resolveType) {
        super("resolve", context);
        ArgumentHelper.checkNotNull(filesToResolve, "filesToResolve");
        ArgumentHelper.checkNotNull(resolveType, "resolveType");
        this.filesToResolve = filesToResolve;
        this.resolveType = resolveType;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder();

        for (final String file : filesToResolve) {
            builder.add(file);
        }

        builder.addSwitch("auto", resolveType.name());
        return builder;
    }

    /**
     * Outputs the resolved conflicts in the following format:
     * <p/>
     * Resolved /Users/leantk/tfvc-tfs/tfsTest_01/TestAdd.txt as KeepYours
     * Resolved /Users/leantk/tfvc-tfs/tfsTest_01/addFold/testHere2 as KeepYours
     *
     * @param stdout
     * @param stderr
     * @return
     */
    @Override
    public List<Conflict> parseOutput(final String stdout, final String stderr) {
        throwIfError(stderr);

        final List<Conflict> resolved = new ArrayList<Conflict>();
        final String[] lines = getLines(stdout);
        for (String line : lines) {
            if (StringUtils.startsWith(line, RESOLVED_PREFIX)) {
                line = StringUtils.removeStart(line, RESOLVED_PREFIX);
                final int index = line.indexOf(RESOLVED_POST_MSG);
                if (index != -1) {
                    resolved.add(new Conflict(line.substring(0, index), Conflict.ConflictType.RESOLVED));
                }
            }
        }

        return resolved;
    }
}

