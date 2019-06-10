// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.exceptions.ToolParseFailureException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;

/**
 * This command calls View to get the contents of the file at the version provided and write them to the destination
 * file.
 * <p/>
 * This command actually wraps the print command:
 * print [/version:<value>] <itemSpec>
 */
public class DownloadCommand extends Command<String> {
    private static final String FILE_NOT_FOUND_ERROR = "The specified file does not exist at the specified version";

    private final String localPath;
    private final int version;
    private final String destination;
    private final boolean ignoreFileNotFound;

    public DownloadCommand(final ServerContext context, final String localPath, final int version, final String destination, final boolean ignoreFileNotFound) {
        super("print", context, true, false);
        ArgumentHelper.checkNotEmptyString(localPath, "localPath");
        ArgumentHelper.checkNotEmptyString(destination, "destination");
        this.localPath = localPath;
        this.version = version;
        this.destination = destination;
        this.ignoreFileNotFound = ignoreFileNotFound;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        final ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder()
                .add(localPath);
        if (version > 0) {
            builder.addSwitch("version", Integer.toString(version));
        }
        return builder;
    }

    /**
     * Returns the destination of the file or throws an error if one occurred
     */
    @Override
    public String parseOutput(final String stdout, final String stderr) {
        final StringBuilder fileContents = new StringBuilder();

        // Check for "The specified file does not exist at the specified version" and write out empty string
        if (ignoreFileNotFound && StringUtils.containsIgnoreCase(stderr, FILE_NOT_FOUND_ERROR)) {
            fileContents.append(StringUtils.EMPTY);
        } else {
            super.throwIfError(stderr);
            for (final String line : getLines(stdout, true)) {
                fileContents.append(line);
                fileContents.append(System.lineSeparator());
            }
        }

        // Write the contents of stdout to the destination file
        final File file = new File(destination);
        if (file.exists()) {
            file.delete();
        }
        try {
            FileUtils.writeStringToFile(file, fileContents.toString());
        } catch (IOException e) {
            // throw any errors that occur
            throw new ToolParseFailureException(e);
        }

        // Return the path to the file
        return destination;
    }
}
