// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external.utils;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.external.models.Workspace;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is a static helper class to do various things with Workspaces and Workspace.Mappings.
 */
public class WorkspaceHelper {
    public static final String ONE_LEVEL_MAPPING_SUFFIX = "/*";

    /**
     * This method compares the mappings of two workspaces to see if they represent the same mappings.
     * If so, the method returns false, if not, it returns true.
     *
     * @param currentWorkspace
     * @param newWorkspace
     * @return
     */
    public static boolean areMappingsDifferent(final Workspace currentWorkspace, final Workspace newWorkspace) {
        ArgumentHelper.checkNotNull(currentWorkspace, "currentWorkspace");
        ArgumentHelper.checkNotNull(newWorkspace, "newWorkspace");
        return areMappingsDifferent(currentWorkspace.getMappings(), newWorkspace.getMappings());
    }

    /**
     * This method simply compares two lists of mappings to see if they represent the same mappings.
     * If so, the method returns false, if not, it returns true.
     *
     * @param mappings1
     * @param mappings2
     * @return
     */
    public static boolean areMappingsDifferent(final List<Workspace.Mapping> mappings1, final List<Workspace.Mapping> mappings2) {
        if (mappings1 == mappings2) {
            // They are the same list (or both null)
            return false;
        }

        // Check for simple differences: one is null, or size is different
        if (mappings1 == null || mappings2 == null || mappings1.size() != mappings2.size()) {
            return true;
        }

        // Create a map of mappings1 for quick lookup
        final Map<String, Workspace.Mapping> map = getMappingsByServerPath(mappings1);
        // Go thru every mapping of mappings2 and see if it exists in the map and is exactly the same
        for (final Workspace.Mapping mapping2 : mappings2) {
            final Workspace.Mapping mapping1 = map.get(getServerPathKey(mapping2.getServerPath()));
            if (mapping1 == null) {
                return true;
            }
            if (mapping1.isCloaked() != mapping2.isCloaked() ||
                    !StringUtils.equals(mapping1.getLocalPath(), mapping2.getLocalPath()) ||
                    !StringUtils.equals(mapping1.getServerPath(), mapping2.getServerPath())) {
                return true;
            }
        }

        // Nothing was different, so they must be the same
        return false;
    }

    /**
     * This method checks all the current mappings in currentWorkspace to see if they exist in the newWorkspace.
     * If the mapping does not exist at all, it is added to the list of mappings to remove.
     *
     * @param currentWorkspace The current workspace with the current mappings
     * @param newWorkspace     The new workspace with the way you want the mappings to look
     * @return
     */
    public static List<Workspace.Mapping> getMappingsToRemove(final Workspace currentWorkspace, final Workspace newWorkspace) {
        ArgumentHelper.checkNotNull(currentWorkspace, "currentWorkspace");
        ArgumentHelper.checkNotNull(newWorkspace, "newWorkspace");
        final List<Workspace.Mapping> mappingsToRemove = new ArrayList<Workspace.Mapping>();
        final Map<String, Workspace.Mapping> map = getMappingsByServerPath(newWorkspace);

        for (final Workspace.Mapping mapping : currentWorkspace.getMappings()) {
            if (!map.containsKey(getServerPathKey(mapping.getServerPath()))) {
                mappingsToRemove.add(mapping);
            }
        }

        return mappingsToRemove;
    }

    /**
     * For now, this method simply returns all the mappings in the new workspace.*
     *
     * @param currentWorkspace The current workspace with the current mappings
     * @param newWorkspace     The new workspace with the way you want the mappings to look
     * @return
     */
    public static List<Workspace.Mapping> getMappingsToChange(final Workspace currentWorkspace, final Workspace newWorkspace) {
        ArgumentHelper.checkNotNull(currentWorkspace, "currentWorkspace");
        ArgumentHelper.checkNotNull(newWorkspace, "newWorkspace");
        return newWorkspace.getMappings();
    }

    /**
     * This method converts the mappings from the workspace into a map of server path to mapping.
     */
    public static Map<String, Workspace.Mapping> getMappingsByServerPath(final Workspace workspace) {
        ArgumentHelper.checkNotNull(workspace, "workspace");
        return getMappingsByServerPath(workspace.getMappings());
    }

    /**
     * This method converts the mappings into a map of server path to mapping.
     */
    public static Map<String, Workspace.Mapping> getMappingsByServerPath(final List<Workspace.Mapping> mappings) {
        ArgumentHelper.checkNotNull(mappings, "mappings");
        final Map<String, Workspace.Mapping> map = new HashMap<String, Workspace.Mapping>(mappings.size());
        for (final Workspace.Mapping mapping : mappings) {
            map.put(getServerPathKey(mapping.getServerPath()), mapping);
        }
        return map;
    }

    /**
     * This method converts the server path into a suitable key for a hash map.
     */
    public static String getServerPathKey(final String serverPath) {
        ArgumentHelper.checkNotNull(serverPath, "serverPath");
        return serverPath.toLowerCase();
    }

    /**
     * This method removes the one level mapping suffix if it is present.
     * Note that it will leave the final / and just remove the *
     */
    public static String getNormalizedServerPath(final String serverPath) {
        ArgumentHelper.checkNotEmptyString(serverPath);
        if (isOneLevelMapping(serverPath)) {
            return serverPath.substring(0, serverPath.length() - 1).toLowerCase();
        }
        return serverPath.toLowerCase();
    }

    /**
     * This metho appends the one level mapping suffix to the server path.
     */
    public static String getOneLevelServerPath(final String serverPath) {
        if (StringUtils.isEmpty(serverPath)) {
            // This is a strange case, but it seems correct to simply return the suffix
            return ONE_LEVEL_MAPPING_SUFFIX;
        }

        if (!isOneLevelMapping(serverPath)) {
            // remove any remaining /'s and add the /*
            return StringUtils.stripEnd(serverPath, "/") + ONE_LEVEL_MAPPING_SUFFIX;
        }

        // It already has the /* at the end
        return serverPath;
    }

    /**
     * This method returns true if the serverPath ends with /*
     */
    public static boolean isOneLevelMapping(final String serverPath) {
        return StringUtils.endsWith(serverPath, ONE_LEVEL_MAPPING_SUFFIX);
    }
}
