// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.PendingChange;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

/**
 * This command returns the status of the workspace as a list of pending changes.
 * NOTE: Currently this command does not support all of the options of the command line
 * <p/>
 * status [/workspace:<value>] [/shelveset:<value>] [/format:brief|detailed|xml] [/recursive] [/user:<value>] [/nodetect] [<itemSpec>...]
 */
public class StatusCommand extends Command<List<PendingChange>> {
    private static final String CANDIDATE_TAG = "candidate-pending-changes";

    private final String localPath;

    public StatusCommand(ServerContext context, String localPath) {
        super("status", context);
        this.localPath = localPath;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder()
                .addSwitch("format", "xml")
                .addSwitch("recursive");
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
     * <pending-change server-item="$/tfsTest_01/test.txt" version="0" owner="jason" date="2016-07-13T12:36:51.060-0400" lock="none" change-type="add" workspace="MyNewWorkspace2" computer="JPRICKET-DEV2" local-item="D:\tmp\test\test.txt"/>
     * </candidate-pending-changes>
     * </status>
     */
    @Override
    public List<PendingChange> parseOutput(final String stdout, final String stderr) {
        super.throwIfError(stderr);
        final List<PendingChange> changes = new ArrayList<PendingChange>(100);
        final NodeList nodes = super.evaluateXPath(stdout, "/status/*/pending-change");

        // Convert all the xpath nodes to pending change models
        if (nodes != null) {
            for (int i = 0; i < nodes.getLength(); i++) {
                final NamedNodeMap attributes = nodes.item(i).getAttributes();
                final boolean isCandidate = StringUtils.equalsIgnoreCase(nodes.item(i).getParentNode().getNodeName(), CANDIDATE_TAG);
                final Node sourceItem = attributes.getNamedItem("source-item"); // not always present
                changes.add(new PendingChange(
                        attributes.getNamedItem("server-item").getNodeValue(),
                        attributes.getNamedItem("local-item").getNodeValue(),
                        attributes.getNamedItem("version").getNodeValue(),
                        attributes.getNamedItem("owner").getNodeValue(),
                        attributes.getNamedItem("date").getNodeValue(),
                        attributes.getNamedItem("lock").getNodeValue(),
                        attributes.getNamedItem("change-type").getNodeValue(),
                        attributes.getNamedItem("workspace").getNodeValue(),
                        attributes.getNamedItem("computer").getNodeValue(),
                        isCandidate,
                        sourceItem != null ? sourceItem.getNodeValue() : StringUtils.EMPTY));
            }
        }
        return changes;
    }
}
