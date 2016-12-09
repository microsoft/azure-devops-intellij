// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.MergeMapping;
import com.microsoft.alm.plugin.external.models.MergeResults;
import com.microsoft.alm.plugin.external.models.ServerStatusType;
import com.microsoft.alm.plugin.external.models.VersionSpec;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * This command merges changes from one version of a branched item into the destination branch location
 * <p/>
 * merge [/recursive] [/force] [/candidate] [/discard] [/version:<value>] [/lock:none|checkin|checkout] [/preview] [/baseless] [/nosummary]
 * [/noimplicitbaseless] [/format:brief|detailed|xml] [/noautoresolve] <source> <destination>
 */
public class MergeCommand extends Command<MergeResults> {
    private static final Logger logger = LoggerFactory.getLogger(MergeCommand.class);

    private static final String NOTHING_TO_MERGE_MSG = "There are no changes to merge.";
    private static final String CONFLICT_PREFIX = "Conflict (";
    private static final String CHANGES_SEPARATOR = ":";
    private static final String FILE_SEPARATOR = "->";
    private static final String VERSION_SEPARATOR = ";";

    private final String workingFolder;
    private final String source;
    private final String destination;
    private final VersionSpec version;
    private final boolean recursive;

    public MergeCommand(final ServerContext context, final String workingFolder, final String source, final String destination, final VersionSpec version, final boolean recursive) {
        super("merge", context);
        ArgumentHelper.checkNotEmptyString(source, "source");
        ArgumentHelper.checkNotEmptyString(destination, "destination");
        this.workingFolder = workingFolder;
        this.source = source;
        this.destination = destination;
        this.version = version;
        this.recursive = recursive;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder()
                .setWorkingDirectory(workingFolder)
                .addSwitch("format", "detailed");

        if (version != null) {
            builder.addSwitch("version", version.toString());
        }

        if (recursive) {
            builder.addSwitch("recursive");
        }

        builder.add(source);
        builder.add(destination);

        return builder;
    }

    /**
     * Example output from TF merge:
     * Conflict (merge, edit): $/tfsTest_03/src/main/java/com/microsoft/build/DemandEquals.java;C222~C222 -> $/tfsTest_03/branch_06/DemandEquals.java;C222
     * merge, edit: $/tfsTest_03/src/main/java/com/microsoft/build/Demand.java;C222~C222 -> $/tfsTest_03/branch_06/Demand.java;C213
     * merge, branch: $/tfsTest_03/src/main/java/com/microsoft/build/Demand2.java;C215 -> $/tfsTest_03/branch_06/Demand2.java
     *
     * @param stdout
     * @param stderr
     * @return
     */
    @Override
    public MergeResults parseOutput(final String stdout, final String stderr) {
        final List<MergeMapping> mappings = new ArrayList<MergeMapping>();

        if (StringUtils.contains(stdout, NOTHING_TO_MERGE_MSG)) {
            return new MergeResults(mappings);
        }

        final List<String> lines = new ArrayList<String>();
        if (StringUtils.startsWithIgnoreCase(stderr, CONFLICT_PREFIX)) {
            // The errors are not errors just conflict lines
            for (final String line : getLines(stderr)) {
                lines.add(line);
            }
        } else {
            throwIfError(stderr);
        }

        // Add in the lines from stdout
        for (final String line : getLines(stdout)) {
            lines.add(line);
        }

        // parse output for file changes/mappings
        for (final String line : lines) {
            final int conflictStartPos = line.indexOf(CONFLICT_PREFIX);
            final int changesEndPos = line.indexOf(CHANGES_SEPARATOR);
            final int sourceFileEndPos = line.indexOf(FILE_SEPARATOR);
            if (changesEndPos > 0 && sourceFileEndPos > 0) {
                final String changeTypes;
                final boolean isConflict;
                if (conflictStartPos >= 0) {
                    // If its a conflict, the change types are in parenthesis after the conflict prefix
                    // The open paren is part of the prefix, but we need to subtract the close paren from the end pos
                    changeTypes = line.substring(conflictStartPos + CONFLICT_PREFIX.length(), changesEndPos - 1);
                    isConflict = true;
                } else {
                    // Otherwise the changes are the first part of the string
                    changeTypes = line.substring(0, changesEndPos);
                    isConflict = false;
                }
                final String sourceFile = line.substring(changesEndPos + 1, sourceFileEndPos).trim();
                final String targetFile = line.substring(sourceFileEndPos + FILE_SEPARATOR.length() + 1).trim();
                mappings.add(createMapping(changeTypes, sourceFile, targetFile, isConflict));
            } else {
                logger.warn("Unknown response from 'tf merge' command: " + line);
            }
        }

        return new MergeResults(mappings);
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

    private MergeMapping createMapping(final String changeTypes, final String source, final String target, final boolean isConflict) {
        final List<ServerStatusType> serverStatusTypes = ServerStatusType.getServerStatusTypes(changeTypes);
        String[] parts = StringUtils.split(source, VERSION_SEPARATOR);
        final String sourceFilename = parts.length > 0 ? parts[0] : StringUtils.EMPTY;
        final String sourceVersionRange = parts.length > 1 ? parts[1] : StringUtils.EMPTY;
        parts = StringUtils.split(target, VERSION_SEPARATOR);
        final String targetFilename = parts.length > 0 ? parts[0] : StringUtils.EMPTY;
        final String targetVersion = parts.length > 1 ? parts[1] : StringUtils.EMPTY;
        final VersionSpec.Range range = VersionSpec.Range.create(sourceVersionRange);
        return new MergeMapping(sourceFilename, targetFilename, range,
                VersionSpec.create(targetVersion), serverStatusTypes, isConflict);
    }
}