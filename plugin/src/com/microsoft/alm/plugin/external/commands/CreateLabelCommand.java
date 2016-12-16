// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * This command creates a label.
 * <p/>
 * label [/owner:<value>] [/version:<value>] [/comment:<value>|@valuefile] [/child:fail|replace|merge]
 * [/recursive] <labelName>[@<scope>] <itemSpec>...
 */
public class CreateLabelCommand extends Command<String> {
    public static final Logger logger = LoggerFactory.getLogger(CreateLabelCommand.class);

    public static final String LABEL_CREATED = "CREATED";
    public static final String LABEL_UPDATED = "UPDATED";

    private static final String CREATED_LABEL_PREFIX = "Created label ";
    private static final String UPDATED_LABEL_PREFIX = "Updated label ";


    private final String workingFolder;
    private final String name;
    private final String comment;
    private final boolean recursive;
    private final List<String> itemSpecs;

    /**
     * Constructor
     *
     * @param context This is the server context used for collection and login info (can be null)
     */
    public CreateLabelCommand(final ServerContext context, final String workingFolder, final String name,
                              final String comment, final boolean recursive,
                              final List<String> itemSpecs) {
        super("label", context);
        ArgumentHelper.checkNotEmptyString(workingFolder, "workingFolder");
        ArgumentHelper.checkNotEmptyString(name, "name");
        ArgumentHelper.checkNotNullOrEmpty(itemSpecs, "itemSpecs");
        this.workingFolder = workingFolder;
        this.name = name;
        this.comment = comment;
        this.recursive = recursive;
        this.itemSpecs = itemSpecs;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        final ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder()
                .setWorkingDirectory(workingFolder)
                .add(name);
        if (recursive) {
            builder.addSwitch("recursive");
        }

        if (StringUtils.isNotEmpty(comment)) {
            builder.addSwitch("comment", comment);
        }

        for (final String item : itemSpecs) {
            builder.add(item);
        }

        return builder;
    }


    /**
     * Parse out the full label name.
     * Example:
     * Created label MyLabel@$/tfsTest_03
     */
    @Override
    public String parseOutput(final String stdout, final String stderr) {
        throwIfError(stderr);

        // Check for the created prefix
        int prefixIndex = StringUtils.indexOf(stdout, CREATED_LABEL_PREFIX);
        if (prefixIndex >= 0) {
            return LABEL_CREATED;
        }

        // Check for the updated prefix
        prefixIndex = StringUtils.indexOf(stdout, UPDATED_LABEL_PREFIX);
        if (prefixIndex >= 0) {
            return LABEL_UPDATED;
        }

        // This should not happen, so not localizing the string
        throw new RuntimeException("Unable to parse output: " + stdout);
    }
}