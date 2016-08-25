// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.ChangeSet;
import com.microsoft.alm.plugin.external.models.PendingChange;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

/**
 * Use this command to get the history of any workspace item.
 *
 * history [/version:<value>] [/stopafter:<value>] [/recursive] [/user:<value>] [/format:brief|detailed|xml] [/slotmode] [/itemmode] <itemSpec>
 */
public class HistoryCommand extends Command<List<ChangeSet>> {
    private final String localPath;
    private final String version;
    private final String user;
    private final int stopAfter;
    private final boolean recursive;

    public HistoryCommand(final ServerContext context, final String localPath, final String version,
                          final int stopAfter, final boolean recursive, final String user) {
        super("history", context);
        ArgumentHelper.checkNotEmptyString(localPath);
        this.localPath = localPath;
        this.version = version;
        this.user = user;
        this.stopAfter = stopAfter;
        this.recursive = recursive;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        final ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder()
                .addSwitch("format", "xml");
        if (recursive) {
            builder.addSwitch("recursive");
        }
        if (stopAfter > 0) {
            builder.addSwitch("stopafter", Integer.toString(stopAfter));
        }
        if (StringUtils.isNotEmpty(user)) {
            builder.addSwitch("user", user);
        }
        if (StringUtils.isNotEmpty(version)) {
            builder.addSwitch("version", version);
        }
        builder.add(localPath);
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
        if (nodes != null) {
            for (int i = 0; i < nodes.getLength(); i++) {
                final Element changeset = (Element) nodes.item(i);

                // Read comment element
                final NodeList commentNodes = changeset.getElementsByTagName("comment");
                final String comment;
                if (commentNodes.getLength() == 1) {
                    comment = commentNodes.item(0).getTextContent();
                } else {
                    comment = "";
                }

                // Gather pending changes
                final List<PendingChange> changes = new ArrayList<PendingChange>(100);
                final NodeList childNodes = changeset.getElementsByTagName("item");
                for (int j = 0; j < childNodes.getLength(); j++) {
                    final Node child = childNodes.item(i);
                    // Assume this is a change
                    final NamedNodeMap attributes = child.getAttributes();
                    changes.add(new PendingChange(
                            attributes.getNamedItem("server-item").getNodeValue(),
                            attributes.getNamedItem("change-type").getNodeValue()));
                }

                final NamedNodeMap attributes = changeset.getAttributes();
                changeSets.add(new ChangeSet(
                        attributes.getNamedItem("id").getNodeValue(),
                        attributes.getNamedItem("owner").getNodeValue(),
                        attributes.getNamedItem("committer").getNodeValue(),
                        attributes.getNamedItem("date").getNodeValue(),
                        comment,
                        changes));
            }
        }
        return changeSets;
    }
}

