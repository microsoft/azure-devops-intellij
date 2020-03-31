// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.commands;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.ToolRunner;
import com.microsoft.alm.plugin.external.models.ItemInfo;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This command calls Info which returns local and server information about an item in the workspace.
 * <p/>
 * info [/recursive] [/version:<value>] <itemSpec>...
 */
public class InfoCommand extends Command<List<ItemInfo>> {
    private final List<String> itemPaths;
    private final String workingFolder;

    public InfoCommand(final ServerContext context, final List<String> itemPaths) {
        this(context, null, itemPaths);
    }

    public InfoCommand(final ServerContext context, final String workingFolder, final List<String> itemPaths) {
        super("info", context);
        ArgumentHelper.checkNotNullOrEmpty(itemPaths, "itemPaths");
        this.itemPaths = itemPaths;
        this.workingFolder = workingFolder;
    }

    @Override
    public ToolRunner.ArgumentBuilder getArgumentBuilder() {
        ToolRunner.ArgumentBuilder builder = super.getArgumentBuilder()
                .setWorkingDirectory(workingFolder);
        for (final String file : itemPaths) {
            builder.add(file);
        }
        return builder;
    }

    /**
     * Example of output
     * Local information:
     * Local path:  D:\tmp\TFVC_1\build.xml
     * Server path: $/TFVC_1/build.xml
     * Changeset:   18
     * Change:      none
     * Type:        file
     * Server information:
     * Server path:   $/TFVC_1/build.xml
     * Changeset:     18
     * Deletion ID:   0
     * Lock:          none
     * Lock owner:
     * Last modified: Nov 18, 2016 11:10:20 AM
     * Type:          file
     * File type:     windows-1252
     * Size:          1385
     */
    @Override
    public List<ItemInfo> parseOutput(final String stdout, final String stderr) {
        super.throwIfError(stderr);

        final List<ItemInfo> itemInfos = new ArrayList<ItemInfo>(itemPaths.size());

        final Map<String, String> propertyMap = new HashMap<String, String>(15);
        final String[] output = getLines(stdout);

        String prefix = "";
        for (final String line : output) {
            if (StringUtils.startsWithIgnoreCase(line, "local information:")) {
                // switch to local mode
                prefix = "";
                if (!propertyMap.isEmpty()) {
                    itemInfos.add(getItemInfo(propertyMap));
                }
                propertyMap.clear();
            } else if (StringUtils.startsWithIgnoreCase(line, "server information:")) {
                // switch to server mode
                prefix = "server ";
            } else if (StringUtils.isNotBlank(line)) {
                // add property
                final int colonPos = line.indexOf(":");
                if (colonPos > 0) {
                    final String key = prefix + line.substring(0, colonPos).trim().toLowerCase();
                    final String value = colonPos + 1 < line.length() ? line.substring(colonPos + 1).trim() : StringUtils.EMPTY;
                    propertyMap.put(key, value);
                }
            }
        }
        if (!propertyMap.isEmpty()) {
            itemInfos.add(getItemInfo(propertyMap));
        }
        return itemInfos;
    }

    private ItemInfo getItemInfo(Map<String, String> propertyMap) {
        return new ItemInfo(
                propertyMap.get("server path"),
                propertyMap.get("local path"),
                propertyMap.get("server changeset"),
                propertyMap.get("changeset"),
                propertyMap.get("change"),
                propertyMap.get("type"),
                propertyMap.get("server lock"),
                propertyMap.get("server lock owner"),
                propertyMap.get("server deletion id"),
                propertyMap.get("server last modified"),
                propertyMap.get("server file type")
        );
    }
}
