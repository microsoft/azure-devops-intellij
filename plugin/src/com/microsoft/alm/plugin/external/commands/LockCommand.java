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
 * This command locks or unlocks an item.
 * <p/>
 * lock [/recursive] /lock:none|checkin|checkout <itemSpec>...
 */
public class LockCommand extends Command<String> {
    public static final Logger logger = LoggerFactory.getLogger(LockCommand.class);

    public enum LockLevel {
        NONE("none"),
        CHECKIN("checkin"),
        CHECKOUT("checkout");

        LockLevel(final String value) {
            this.value = value;
        }

        private final String value;

        @Override
        public String toString() {
            return value;
        }

        /**
         * Converts the string into the correct enum value.
         * We ignore the "-" in check-in and check-out to match those forms as well.
         *
         * @param level
         * @return
         */
        public static LockLevel fromString(final String level) {
            if (StringUtils.equalsIgnoreCase(StringUtils.remove(level, "-"), CHECKIN.toString())) {
                return CHECKIN;
            } else if (StringUtils.equalsIgnoreCase(StringUtils.remove(level, "-"), CHECKOUT.toString())) {
                return CHECKOUT;
            } else if (StringUtils.isEmpty(level) || StringUtils.equalsIgnoreCase(level, NONE.toString())) {
                return NONE;
            }
            throw new IllegalArgumentException("level");
        }
    }

    private static final String CANNOT_UNLOCK_PREFIX = "TF14090: Cannot unlock";
    private static final String CANNOT_UNLOCK_SUFFIX = "It is not currently locked in your workspace.";

    private final String workingFolder;
    private final LockLevel lockLevel;
    private final boolean recursive;
    private final List<String> itemSpecs;

    /**
     * Constructor
     *
     * @param context This is the server context used for collection and login info (can be null)
     */
    public LockCommand(final ServerContext context, final String workingFolder, final LockLevel lockLevel,
                       final boolean recursive, final List<String> itemSpecs) {
        super("lock", context);
        ArgumentHelper.checkNotEmptyString(workingFolder, "workingFolder");
        ArgumentHelper.checkNotNullOrEmpty(itemSpecs, "itemSpecs");
        ArgumentHelper.checkNotNull(lockLevel, "lockLevel");
        this.workingFolder = workingFolder;
        this.lockLevel = lockLevel;
        this.recursive = recursive;
        this.itemSpecs = itemSpecs;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        final ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder()
                .setWorkingDirectory(workingFolder)
                .addSwitch("lock", lockLevel.toString());

        if (recursive) {
            builder.addSwitch("recursive");
        }

        for (final String item : itemSpecs) {
            builder.add(item);
        }

        return builder;
    }


    /**
     * Parse out the full label name.
     * Example of trying to unlock 2 folders:
     * Folder333:
     * Folder333
     * TF14090: Cannot unlock $/tfsTest_03/model_branch_06. It is not currently locked in your workspace.
     */
    @Override
    public String parseOutput(final String stdout, final String stderr) {
        // If we only have errors, just throw them
        if (StringUtils.isEmpty(stdout)) {
            throwIfError(stderr);
        }

        // If we have normal output but still have errors,
        // Find any "real" errors; ignore cannot unlock errors
        if (StringUtils.isNotEmpty(stderr)) {
            final StringBuilder errors = new StringBuilder();
            for (final String line : getLines(stderr)) {
                if (StringUtils.startsWith(line, CANNOT_UNLOCK_PREFIX) && StringUtils.endsWith(line, CANNOT_UNLOCK_SUFFIX)) {
                    // Found a warning that the file could not be unlocked because it is not locked
                    // Ignoring this error
                    continue;
                }
                errors.append(line);
                errors.append("\n");
            }
            throwIfError(errors.toString());
        }

        // We aren't going to bother parsing the output further.
        // It's just a list of the local file paths that we passed in.
        return StringUtils.EMPTY;
    }

    @Override
    public int interpretReturnCode(int returnCode) {
        // We will treat Partial success (returnCode == 1) as success. Errors are still reported but we need to show what went well also.
        return super.interpretReturnCode(returnCode == 1 ? 0 : returnCode);
    }
}