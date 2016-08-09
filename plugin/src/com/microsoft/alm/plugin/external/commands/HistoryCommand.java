// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.ChangeSet;
import com.microsoft.alm.plugin.external.models.PendingChange;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

public class HistoryCommand extends Command<List<ChangeSet>> {
    private final String localPath;
    private final String version;
    private final String user;
    private final int stopAfter;
    private final boolean recursive;

    public HistoryCommand(ServerContext context, String localPath, String version, int stopAfter, boolean recursive, String user) {
        super("history", context);
        this.localPath = localPath;
        this.version = version;
        this.user = user;
        this.stopAfter = stopAfter;
        this.recursive = recursive;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder()
                .add("-format:xml");
        if (recursive) {
            builder.add("-recursive");
        }
        if (stopAfter > 0) {
            builder.add("-stopafter:" + stopAfter);
        }
        if (user != null && user.length() > 0) {
            builder.add("-user:" + user);
        }
        if (version != null && version.length() > 0) {
            builder.add("-version:" + version);
        }
        if (localPath != null) {
            builder.add(localPath);
        }
        return builder;
    }

    /**
     * Parses the output of the status command when formatted as xml.
     * SAMPLE
     * <?xml version="1.0" encoding="utf-8"?>
     * <history>
     * <changeset id="4" owner="john" committer="john" date="2016-06-07T11:18:18.790-0400">
     * <comment>add readme</comment>
     * <item change-type="add" server-item="$/tfs01/readme.txt"/>
     * </changeset>
     * <changeset id="3" owner="jeff" committer="jeff" date="2016-06-07T11:13:51.747-0400">
     * <comment>initial checkin</comment>
     * <item change-type="add" server-item="$/tfs01/com.microsoft.core"/>
     * <item change-type="add" server-item="$/tfs01/com.microsoft.core/.classpath"/>
     * </changeset>
     * </history>
     */
    @Override
    public List<ChangeSet> parseOutput(final String stdout, final String stderr) {
        super.throwIfError(stderr);
        final List<ChangeSet> changeSets = new ArrayList<ChangeSet>(100);
        final NodeList nodes = super.evaluateXPath(stdout, "/history/changeset");

        // Convert all the xpath nodes to changeset models
        for (int i = 0; i < nodes.getLength(); i++) {
            // Gather pending changes
            String comment = "";
            final List<PendingChange> changes = new ArrayList<PendingChange>(100);
            final NodeList childNodes = nodes.item(i).getChildNodes();
            for (int j = 0; j < childNodes.getLength(); j++) {
                // The child node may be a comment or a change
                if (StringUtils.equalsIgnoreCase(nodes.item(i).getLocalName(), "comment")) {
                    // Save the comment
                    comment = nodes.item(i).getTextContent();
                } else {
                    // Assume this is a change
                    final NamedNodeMap attributes = nodes.item(i).getAttributes();
                    changes.add(new PendingChange(
                            attributes.getNamedItem("server-item").getNodeValue(),
                            attributes.getNamedItem("change-type").getNodeValue()));
                }
            }

            final NamedNodeMap attributes = nodes.item(i).getAttributes();
            changeSets.add(new ChangeSet(
                    attributes.getNamedItem("id").getNodeValue(),
                    attributes.getNamedItem("owner").getNodeValue(),
                    attributes.getNamedItem("committer").getNodeValue(),
                    attributes.getNamedItem("date").getNodeValue(),
                    comment,
                    changes));
        }

        return changeSets;
    }
}

