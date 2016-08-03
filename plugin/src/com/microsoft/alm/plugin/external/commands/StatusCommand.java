// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.PendingChange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;


public class StatusCommand extends Command<List<PendingChange>> {
    private final String localPath;

    public StatusCommand(ServerContext context, String localPath) {
        super("status", context);
        this.localPath = localPath;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder()
                .add("-format:xml")
                .add("-recursive")
                .add("-noprompt");
        if (localPath != null) {
            builder.add(localPath);
        }
        return builder;
    }

    /**
     * Parses the output of the status command when formatted as xml.
     * SAMPLE
     * <?xml version="1.0" encoding="utf-8"?>
     * <status>
     * <pending-changes/>
     * <candidate-pending-changes>
     * <pending-change server-item="$/tfsTest_01/test.txt" version="0" owner="NORTHAMERICA\jpricket" date="2016-07-13T12:36:51.060-0400" lock="none" change-type="add" workspace="MyNewWorkspace2" computer="JPRICKET-DEV2" local-item="D:\tmp\test\test.txt"/>
     * <p/>
     * </candidate-pending-changes>
     * </status>
     *
     * @param stdoutReader
     * @param stderrReader
     * @return
     * @throws ParseException
     * @throws IOException
     */
    @Override
    public List<PendingChange> parseOutput(Reader stdoutReader, Reader stderrReader) throws ParseException, IOException {
        final BufferedReader outputReader = new BufferedReader(stdoutReader);

        //TODO Use XPath or some other xml parsing class
        try {
            // If stderror has any output, throw an exception with the details
            super.throwIfError(stderrReader);

            final List<PendingChange> changes = new ArrayList<PendingChange>(100);
            String line = outputReader.readLine();
            while (line != null) {
                final int index = line.indexOf("<pending-change ");
                if (index >= 0) {
                    changes.add(new PendingChange(
                            getValue(line, "server-item"),
                            getValue(line, "local-item"),
                            getValue(line, "version"),
                            getValue(line, "owner"),
                            getValue(line, "date"),
                            getValue(line, "lock"),
                            getValue(line, "change-type"),
                            getValue(line, "workspace"),
                            getValue(line, "computer")
                    ));
                }
                line = outputReader.readLine();
            }
            return changes;
        } finally {
            outputReader.close();
        }
    }

    private String getValue(final String line, final String attributeName) {
        final String marker = " " + attributeName + "=\"";
        final int index = line.indexOf(marker);
        if (index >= 0) {
            final int endIndex = line.indexOf("\"", index + marker.length());
            if (endIndex > index) {
                return line.substring(index + marker.length(), endIndex);
            }
        }
        return "";
    }
}
