// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.Workspace;
import com.microsoft.alm.plugin.external.utils.WorkspaceHelper;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

/**
 * This command returns a fully filled out workspace object.
 */
public class GetWorkspaceCommand extends Command<Workspace> {
    private final String workspaceName;

    public GetWorkspaceCommand(final ServerContext context, final String workspaceName) {
        super("workspaces", context);
        ArgumentHelper.checkNotEmptyString(workspaceName);
        this.workspaceName = workspaceName;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        final ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder()
                .add(workspaceName)
                .addSwitch("format", "xml");
        return builder;
    }

    /**
     * Parses the output of the workspaces command.
     * SAMPLE
     * <?xml version="1.0" encoding="utf-8"?>
     * <workspaces>
     * <workspace name="MyNewWorkspace2" owner="username" computer="machine" comment="description" server="http://server:8080/tfs/">
     * <working-folder server-item="$/TeamProject" local-item="D:\project1" type="map" depth="full"/>
     * </workspace>
     * </workspaces>
     */
    @Override
    public Workspace parseOutput(final String stdout, final String stderr) {
        super.throwIfError(stderr);

        final NodeList workspaceNodeList = super.evaluateXPath(stdout, "/workspaces/workspace");
        if (workspaceNodeList != null && workspaceNodeList.getLength() == 1) {
            final NamedNodeMap workspaceAttributes = workspaceNodeList.item(0).getAttributes();
            // Get all the mappings for the workspace
            final NodeList mappingsNodeList = super.evaluateXPath(stdout, "/workspaces/workspace/working-folder");
            final List<Workspace.Mapping> mappings = new ArrayList<Workspace.Mapping>(mappingsNodeList.getLength());
            for (int i = 0; i < mappingsNodeList.getLength(); i++) {
                final NamedNodeMap mappingAttributes = mappingsNodeList.item(i).getAttributes();
                final String localPath = getXPathAttributeValue(mappingAttributes, "local-item");
                final String depth = getXPathAttributeValue(mappingAttributes, "depth");
                final boolean isCloaked = !StringUtils.equals(getXPathAttributeValue(mappingAttributes, "type"), "map");
                String serverPath = getXPathAttributeValue(mappingAttributes, "server-item");
                if (!StringUtils.equals(depth, "full")) {
                    // The normal way to denote one level mappings (not full mappings) is to end the server path
                    // with a /*. This indicates that the mapping is not recursive to all subfolders.
                    serverPath = WorkspaceHelper.getOneLevelServerPath(serverPath);
                }
                mappings.add(new Workspace.Mapping(serverPath, localPath, isCloaked));
            }

            // Get owner name (display name may not be available
            String owner = getXPathAttributeValue(workspaceAttributes, "owner-display-name");
            if (StringUtils.isEmpty(owner)) {
                owner = getXPathAttributeValue(workspaceAttributes, "owner");
            }


            final Workspace workspace = new Workspace(
                    getXPathAttributeValue(workspaceAttributes, "server"),
                    getXPathAttributeValue(workspaceAttributes, "name"),
                    getXPathAttributeValue(workspaceAttributes, "computer"),
                    owner,
                    getXPathAttributeValue(workspaceAttributes, "comment"),
                    mappings);
            return workspace;
        }

        return null;
    }
}
