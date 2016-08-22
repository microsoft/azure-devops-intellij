// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.exceptions.TeamServicesException;
import com.microsoft.alm.plugin.external.ToolRunner;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This command  checks in files into TFVC
 */
public class CheckinCommand extends Command<String> {
    public static final Logger logger = LoggerFactory.getLogger(CheckinCommand.class);

    private static final String CHECKIN_LINE_PREFIX = "Checking in";
    private static final String CHECKIN_FAILED_MSG = "No files checked in";
    private static final Pattern CHANGESET_NUMBER_PATTERN = Pattern.compile("#(\\d+)");

    private final List<String> files;
    private final String comment;

    public CheckinCommand(final ServerContext context, final List<String> files, final String comment) {
        super("checkin", context);
        ArgumentHelper.checkNotNullOrEmpty(files, "files");
        this.files = files;
        this.comment = comment;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder();
        for (final String file : files) {
            builder.add(file);
        }
        if (StringUtils.isNotEmpty(comment)) {
            builder.add("-comment:" + comment);
        }
        return builder;
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
            logger.error("Checkin failed with the following stdout: " + stdout);
            final String[] output = getLines(stdout);
            for (int i = 0; i < output.length; i++) {
                // finding error message by eliminating all other known output lines since we can't parse for the error line itself (it's unknown to us)
                // TODO: figure out a better way to get the error message instead of parsing
                if (StringUtils.isNotEmpty(output[i]) &&
                        !StringUtils.startsWithIgnoreCase(output[i], CHECKIN_LINE_PREFIX) &&
                        !StringUtils.startsWithIgnoreCase(output[i], CHECKIN_FAILED_MSG) &&
                        !(StringUtils.startsWith(output[i], UrlHelper.URL_SEPARATOR) && StringUtils.endsWith(output[i], ":"))) {
                    throw new RuntimeException(output[i]);
                }
            }
            // couldn't figure out error message parsing so returning generic error
            logger.error("Parsing of the stdout failed to get the error message");
            throw new TeamServicesException(TeamServicesException.KEY_ERROR_UNKNOWN);
        }

        // parse output for changeset number
        String changesetNumber = StringUtils.EMPTY;
        final Matcher matcher = CHANGESET_NUMBER_PATTERN.matcher(stdout);
        if (matcher.find()) {
            changesetNumber = matcher.group(1);
        }
        logger.info("Changeset " + changesetNumber + " was created");
        return changesetNumber;
    }
}
