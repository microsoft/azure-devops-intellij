// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.exceptions.ToolException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * This command calls View to get the contents of the file at the version provided and write them to the destination
 * file.
 */
public class DownloadCommand extends Command<String> {
    private final String localPath;
    private final int version;
    private final String destination;

    public DownloadCommand(final ServerContext context, final String localPath, final int version, final String destination) {
        super("print", context);
        ArgumentHelper.checkNotEmptyString(localPath);
        ArgumentHelper.checkNotEmptyString(destination);
        this.localPath = localPath;
        this.version = version;
        this.destination = destination;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        final ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder()
                .add(localPath)
                .addSwitch("proxy", "http://fail:0001");  // HACK to avoid the timeout that happens when you don't specify a proxy
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
        //TODO: Throw an error if one occurred --- the HACK above causes an error every time
        //TODO: super.throwIfError(stderr);
        // Write the contents of stdout to the destination file
        File file = new File(destination);
        if (file.exists()) {
            file.delete();
        }
        try {
            FileUtils.writeStringToFile(file, stdout);
        } catch (IOException e) {
            // throw any errors that occur
            throw new ToolException(ToolException.KEY_TF_PARSE_FAILURE, e);
        }

        // Return the path to the file
        return destination;
    }
}
