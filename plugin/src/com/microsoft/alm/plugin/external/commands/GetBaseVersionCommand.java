// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.common.utils.SystemHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.VersionSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This command uses the merges command to get the latest changeset that 2 branched items share so that they can be properly merged.
 * <p/>
 * merges [/recursive] [/format:brief|detailed|xml] [<sourceItem>] <destinationItem>
 */
public class GetBaseVersionCommand extends Command<VersionSpec> {
    private static final Logger logger = LoggerFactory.getLogger(GetBaseVersionCommand.class);

    private final String workingFolder;
    private final String source;
    private final String destination;

    public GetBaseVersionCommand(final ServerContext context, final String workingFolder, final String source, final String destination) {
        super("merges", context);
        ArgumentHelper.checkNotEmptyString(source, "source");
        ArgumentHelper.checkNotEmptyString(destination, "destination");
        this.workingFolder = workingFolder;
        this.source = source;
        this.destination = destination;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder()
                .setWorkingDirectory(workingFolder)
                .addSwitch("format", "brief");

        builder.add(source);
        builder.add(destination);

        return builder;
    }

    /**
     * Example output from TF merges:
     * Changeset Merged In Changeset Author         Date
     * --------- ------------------- -------------- ------------------------
     * 8         208                 Jason Prickett Nov 30, 2016 11:53:58 AM
     * 222       232                 Jason Prickett Dec 7, 2016 12:00:28 PM
     * 231       232                 Jason Prickett Dec 7, 2016 12:00:28 PM
     * 233       234                 Jason Prickett Dec 7, 2016 12:01:24 PM
     * <p/>
     * Note: the changeset we return is in the first column and the last row
     *
     * @param stdout
     * @param stderr
     * @return
     */
    @Override
    public VersionSpec parseOutput(final String stdout, final String stderr) {
        throwIfError(stderr);

        // parse output for file changes/mappings
        int changeset = -1;
        for (final String line : getLines(stdout)) {
            final int changesetEndPos = line.indexOf(" ");
            if (changesetEndPos > 0) {
                final String changesetText = line.substring(0, changesetEndPos);
                final int currentChangeset = SystemHelper.toInt(changesetText, -1);
                if (currentChangeset >= 0) {
                    changeset = currentChangeset;
                }
            }
        }

        return changeset >= 0 ? VersionSpec.create(changeset) : null;
    }
}