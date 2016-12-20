// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.TfvcLabel;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

/**
 * This command gets a list of labels. You can filter the list using the optional label filter.
 * <p/>
 * labels [/owner:<value>] [/format:brief|detailed|xml] [<labelNameFilter>]
 */
public class GetLabelsCommand extends Command<List<TfvcLabel>> {
    public static final Logger logger = LoggerFactory.getLogger(GetLabelsCommand.class);

    private static final String NO_LABELS = "No labels found.";


    private final String workingFolder;
    private final String labelNameFilter;

    public GetLabelsCommand(final ServerContext context, final String workingFolder, final String labelNameFilter) {
        super("labels", context);
        this.workingFolder = workingFolder;
        this.labelNameFilter = labelNameFilter;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder()
                .addSwitch("format", "xml");
        if (StringUtils.isNotEmpty(workingFolder)) {
            builder.setWorkingDirectory(workingFolder);
        }
        if (StringUtils.isNotEmpty(labelNameFilter)) {
            builder.add(labelNameFilter);
        }
        return builder;
    }

    /**
     * Returns the list of labels including the items in the labels.
     * <p/> <pre> <code>
     *     <?xml version="1.0" encoding="utf-8"?><labels>
     *          <label name="MyLabel+" scope="$/tfsTest_03" user="NORTHAMERICA\jpricket" date="2016-12-15T14:53:38.247-0500">
     *              <comment>adsf</comment>
     *              <item changeset="266" server-item="$/tfsTest_03/Folder1/Folder2/com.microsoft.vss.client.core/src/main/java/com/microsoft/merge_branches/merge00"/>
     *              <item changeset="266" server-item="$/tfsTest_03/Folder1/Folder2/com.microsoft.vss.client.core/src/main/java/com/microsoft/merge_branches/merge00/ContentAndNameChanges.java"/>
     *              <item changeset="266" server-item="$/tfsTest_03/Folder1/Folder2/com.microsoft.vss.client.core/src/main/java/com/microsoft/merge_branches/merge00/ContentChanges.java"/>
     *              <item changeset="266" server-item="$/tfsTest_03/Folder1/Folder2/com.microsoft.vss.client.core/src/main/java/com/microsoft/merge_branches/merge00/NameChanges.java"/>
     *              <item changeset="266" server-item="$/tfsTest_03/Folder1/Folder2/com.microsoft.vss.client.core/src/main/java/com/microsoft/merge_branches/merge00/ToDelete.java"/>
     *              <item changeset="266" server-item="$/tfsTest_03/Folder1/Folder2/com.microsoft.vss.client.core/src/main/java/com/microsoft/merge_branches"/>
     *          </label>
     *     </labels>
     * </code></pre>
     */
    @Override
    public List<TfvcLabel> parseOutput(final String stdout, final String stderr) {
        throwIfError(stderr);
        final List<TfvcLabel> labels = new ArrayList<TfvcLabel>();

        // Check for no labels (which is not in xml format)
        if (StringUtils.containsIgnoreCase(stdout, NO_LABELS)) {
            // If there aren't any labels just return the empty list.
            return labels;
        }

        final NodeList nodes = super.evaluateXPath(stdout, "/labels/label");

        // Convert all the xpath nodes to label models
        if (nodes != null) {
            for (int i = 0; i < nodes.getLength(); i++) {
                final Element label = (Element) nodes.item(i);

                // Read comment element
                final NodeList commentNodes = label.getElementsByTagName("comment");
                final String comment;
                if (commentNodes.getLength() == 1) {
                    comment = commentNodes.item(0).getTextContent();
                } else {
                    comment = "";
                }

                // Gather pending changes
                final List<TfvcLabel.Item> items = new ArrayList<TfvcLabel.Item>(100);
                final NodeList childNodes = label.getElementsByTagName("item");
                for (int j = 0; j < childNodes.getLength(); j++) {
                    final Node child = childNodes.item(j);
                    // Assume this is a change
                    final NamedNodeMap attributes = child.getAttributes();
                    items.add(new TfvcLabel.Item(
                            attributes.getNamedItem("server-item").getNodeValue(),
                            attributes.getNamedItem("changeset").getNodeValue()));
                }

                final NamedNodeMap attributes = label.getAttributes();
                labels.add(new TfvcLabel(
                        attributes.getNamedItem("name").getNodeValue(),
                        attributes.getNamedItem("scope").getNodeValue(),
                        attributes.getNamedItem("user").getNodeValue(),
                        attributes.getNamedItem("date").getNodeValue(),
                        comment,
                        items));
            }
        }
        return labels;
    }

    @Override
    public int interpretReturnCode(int returnCode) {
        // If no labels are found, a 100 is returned from the command line.
        // We already handle that case above, so we will ignore it here.
        return super.interpretReturnCode(returnCode == 100 ? 0 : returnCode);
    }
}
