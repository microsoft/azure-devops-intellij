// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.exceptions.TeamServicesException;
import com.microsoft.alm.plugin.external.ToolRunner;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * This command  checks in files into TFVC
 * <p/>
 * checkin [/all] [/author:<value>] [/comment:<value>|@valuefile] [/notes:"note"="value"[;"note2"="value2"[;...]]|@notefile]
 * [/override:<value>|@valuefile] [/recursive] [/validate] [/bypass] [/force] [/noautoresolve] [/associate:<workItemID>[,<workItemID>...]]
 * [/resolve:<workItemID>[,<workItemID>...]] [/saved] [<itemSpec>...]
 */
public class CheckinCommand extends Command<String> {
    public static final Logger logger = LoggerFactory.getLogger(CheckinCommand.class);

    private static final String CHECKIN_LINE_PREFIX = "Checking in";
    private static final String CHECKIN_FAILED_MSG = "No files checked in";

    private final List<String> files;
    private final String comment;
    private final List<Integer> workItemsToAssociate;

    public CheckinCommand(final ServerContext context, final List<String> files, final String comment, final List<Integer> workItemsToAssociate) {
        super("checkin", context);
        ArgumentHelper.checkNotNullOrEmpty(files, "files");
        this.files = files;
        this.comment = comment;
        this.workItemsToAssociate = workItemsToAssociate;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder();
        for (final String file : files) {
            builder.add(file);
        }
        if (StringUtils.isNotEmpty(comment)) {
            builder.addSwitch("comment", getComment());
        }
        if (workItemsToAssociate != null && workItemsToAssociate.size() > 0) {
            builder.addSwitch("associate", getAssociatedWorkItems());
        }
        return builder;
    }

    // TODO: It would be nice if we could preserve the newlines somehow
    private String getComment() {
        // replace newlines with spaces
        return comment.replace("\r\n", " ").replace("\n", " ");
    }

    private String getAssociatedWorkItems() {
        final StringBuilder sb = new StringBuilder();
        for (Integer i : workItemsToAssociate) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(i.toString());
        }
        return sb.toString();
    }

    /**
     * Returns the files that were added
     * <p/>
     * Output example for success:
     * /Users/leantk/tfvc-tfs/tfsTest_01/addFold:
     * Checking in edit: testHere.txt
     * <p/>
     * /Users/leantk/tfvc-tfs/tfsTest_01:
     * Checking in edit: test3.txt
     * Checking in edit: TestAdd.txt
     * <p/>
     * Changeset #20 checked in.
     * <p/>
     * Output example for failure:
     * <p/>
     * /Users/leantk/tfvc-tfs/tfsTest_01:
     * Checking in edit: test3.txt
     * Checking in edit: TestAdd.txt
     * Unable to perform operation on $/tfsTest_01/TestAdd.txt. The item $/tfsTest_01/TestAdd.txt is locked in workspace new;Leah Antkiewicz.
     * No files checked in.
     * <p/>
     * No files checked in.
     */
    @Override
    public String parseOutput(final String stdout, final String stderr) {
        // check for failed checkin
        if (StringUtils.isNotEmpty(stderr)) {
            logger.error("Checkin failed with the following stdout:\n" + stdout);
            final String[] output = getLines(stdout);
            final StringBuilder errorMessage = new StringBuilder();
            for (int i = 0; i < output.length; i++) {
                // finding error message by eliminating all other known output lines since we can't parse for the error line itself (it's unknown to us)
                // TODO: figure out a better way to get the error message instead of parsing
                if (!isOutputLineExpected(output[i], new String[]{CHECKIN_LINE_PREFIX, CHECKIN_FAILED_MSG}, true)) {
                    errorMessage.append(output[i]).append("\n");
                }
            }
            if (StringUtils.isNotEmpty(errorMessage.toString())) {
                throw new RuntimeException(StringUtils.chomp(errorMessage.toString()));
            }
            // couldn't figure out error message parsing so returning generic error
            logger.error("Parsing of the stdout failed to get the error message");
            throw new TeamServicesException(TeamServicesException.KEY_ERROR_UNKNOWN);
        }

        return getChangesetNumber(stdout);
    }
}
