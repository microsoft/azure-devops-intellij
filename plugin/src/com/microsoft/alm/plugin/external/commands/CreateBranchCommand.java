// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This command creates a new branch.
 * <p/>
 * branch [/version:<value>] [/noget] [/lock:none|checkin|checkout] [/recursive] [/checkin] [/comment:<value>|@valuefile] [/author:<value>]
 * [/notes:"note"="value"[;"note2"="value2"[;...]]|@notefile] <oldItemSpec> <newLocalItem>
 */
public class CreateBranchCommand extends Command<String> {
    public static final Logger logger = LoggerFactory.getLogger(CreateBranchCommand.class);

    private final boolean recursive;
    private final String comment;
    private final String author;
    private final String existingItem;
    private final String newBranchedItem;

    /**
     * Constructor
     *
     * @param context This is the server context used for collection and login info (can be null)
     */
    public CreateBranchCommand(final ServerContext context, final boolean recursive,
                               final String comment, final String author,
                               final String existingItem, final String newBranchedItem) {
        super("branch", context);
        ArgumentHelper.checkNotEmptyString(existingItem, "existingItem");
        ArgumentHelper.checkNotEmptyString(newBranchedItem, "newBranchedItem");
        this.recursive = recursive;
        this.comment = comment;
        this.author = author;
        this.existingItem = existingItem;
        this.newBranchedItem = newBranchedItem;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        final ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder()
                .addSwitch("checkin");
        if (recursive) {
            builder.addSwitch("recursive");
        }
        if (StringUtils.isNotEmpty(comment)) {
            builder.addSwitch("comment", comment);
        }
        if (StringUtils.isNotEmpty(author)) {
            builder.addSwitch("author", author);
        }

        builder.add(existingItem);
        builder.add(newBranchedItem);

        return builder;
    }


    /**
     * Parse out the changeset number.
     * Example:
     * Changeset #19 checked in.
     */
    @Override
    public String parseOutput(final String stdout, final String stderr) {
        throwIfError(stderr);
        return getChangesetNumber(stdout);
    }
}