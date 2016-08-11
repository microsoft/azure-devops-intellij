// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.helpers.Path;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * This command calls Add which adds an unversioned file into TFVC
 */
public class AddCommand extends Command<List<String>> {
    private final List<String> unversionedFiles;

    public AddCommand(final ServerContext context, final List<String> unversionedFiles) {
        super("add", context);
        ArgumentHelper.checkNotNullOrEmpty(unversionedFiles, "unversionedFiles");
        this.unversionedFiles = unversionedFiles;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder();
        for (final String file : unversionedFiles) {
            builder.add(file);
        }
        return builder;
    }

    /**
     * Returns the files that were added
     * <p/>
     * Output example:
     * /Users/leantk/tfvc-tfs/tfsTest_01/.idea:
     * misc.xml
     * modules.xml
     * <p/>
     * /Users/leantk/tfvc-tfs/tfsTest_01:
     * TestAdd.txt
     */
    @Override
    public List<String> parseOutput(final String stdout, final String stderr) {
        super.throwIfError(stderr);
        final List filesAdded = new ArrayList<String>();
        final String[] output = getLines(stdout);

        // parse output for directory paths and file names to combine
        String path = StringUtils.EMPTY;
        for (int i = 0; i < output.length; i++) {
            if (output[i].endsWith(":")) {
                path = StringUtils.removeEnd(output[i], ":");
            } else if (StringUtils.isNotEmpty(output[i])) {
                filesAdded.add(Path.combine(path, output[i]));
            }
        }
        return filesAdded;
    }
}
