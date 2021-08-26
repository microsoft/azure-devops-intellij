// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.extensions;

import com.google.common.collect.Maps;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vcs.LocalFilePath;
import com.microsoft.alm.plugin.idea.tfvc.core.tfs.TFVCUtil;
import com.microsoft.tfs.model.connector.TfsWorkspaceMapping;
import org.jetbrains.annotations.NotNull;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TfvcRootCache {

    private static final Logger ourLogger = Logger.getInstance(TfvcRootCache.class);

    private static void assertNoServiceDirectory(Path path) {
        if (TFVCUtil.isInServiceDirectory(new LocalFilePath(path.toString(), true))) {
            throw new InvalidPathException(path.toString(), "Path contains TFVC service directory name");
        }
    }

    private final Object myLock = new Object();
    private final Map<Path, CachedStatus> myData = Maps.newHashMap();

    /**
     * Tries to determine an item status from the cache.
     *
     * @param path path to determine its status.
     * @return cached status or {@link CachedStatus#UNKNOWN} it no information is available in the cache.
     */
    @NotNull
    public CachedStatus get(@NotNull Path path) {
        synchronized (myLock) {
            CachedStatus result = myData.get(path); // direct cache match
            if (result != null) {
                ourLogger.trace(String.format("%s: %s (cache hit)", path, result));
                return result;
            }

            for (Map.Entry<Path, CachedStatus> entry : myData.entrySet()) {
                Path key = entry.getKey();
                CachedStatus value = entry.getValue();
                boolean isParent = path.startsWith(key);
                boolean isChild = key.startsWith(path);

                // If any child of an item is known to be NO_ROOT, i.e. not under any mapping, then the result is
                // NO_ROOT.
                if (isChild && value == CachedStatus.NO_ROOT) {
                    ourLogger.trace(String.format("%s: %s (derived from %s being %s)", path, value, key, value));
                    return value;
                }

                // If any child of an item is known to be IS_MAPPING_ROOT, then the result is NO_ROOT, since mapping
                // roots cannot be nested.
                if (isChild && value == CachedStatus.IS_MAPPING_ROOT) {
                    ourLogger.trace(String.format("%s: %s (derived from %s being %s)", path, CachedStatus.NO_ROOT, key, value));
                    return CachedStatus.NO_ROOT;
                }

                // If any parent of an item is known to be IS_MAPPING_ROOT, then the result is UNDER_MAPPING_ROOT.
                if (isParent && value == CachedStatus.IS_MAPPING_ROOT) {
                    ourLogger.trace(String.format("%s: %s (derived from %s being %s)", path, CachedStatus.UNDER_MAPPING_ROOT, key, value));
                    return CachedStatus.UNDER_MAPPING_ROOT;
                }
            }

            return CachedStatus.UNKNOWN;
        }
    }

    /**
     * Caches the fact that there're no workspace mappings for the path or its parents.
     */
    public void putNoMappingsFor(@NotNull Path path) {
        assertNoServiceDirectory(path);
        ourLogger.trace(String.format("New without mapping roots: %s", path));
        synchronized (myLock) {
            // Destroy contradictory information: since we know the path isn't a mapping root and contains no parent
            // mapping roots, and information about the parents being roots is now invalid.
            ArrayList<Path> keysToRemove = new ArrayList<>();
            for (Map.Entry<Path, CachedStatus> entry : myData.entrySet()) {
                Path key = entry.getKey();
                if (key.equals(path))
                    continue;

                CachedStatus value = entry.getValue();
                boolean isParent = path.startsWith(key);
                if (isParent && value == CachedStatus.IS_MAPPING_ROOT) {
                    ourLogger.info(
                            String.format(
                                    "Evicting information about %s being %s because %s is %s",
                                    key,
                                    value,
                                    path,
                                    CachedStatus.NO_ROOT));
                    keysToRemove.add(key);
                }
            }

            for (Path key : keysToRemove) {
                myData.remove(key);
            }

            myData.put(path, CachedStatus.NO_ROOT);
        }
    }

    /**
     * Caches the fact that the following mappings are available on disk.
     */
    public void putMappings(@NotNull List<TfsWorkspaceMapping> mappings) {
        synchronized (myLock) {
            for (TfsWorkspaceMapping mapping : mappings) {
                Path path = Paths.get(mapping.getLocalPath().getPath());
                assertNoServiceDirectory(path);

                ourLogger.trace(String.format("New mapping root: %s", path));

                // Destroy contradictory information: since we know the path is a mapping root, then any information
                // that tells us its children are roots themselves or aren't under a root, or its parents are roots is
                // now invalid (mapping roots cannot be nested).
                ArrayList<Path> keysToRemove = new ArrayList<>();
                for (Map.Entry<Path, CachedStatus> entry : myData.entrySet()) {
                    Path key = entry.getKey();
                    if (key.equals(path))
                        continue;

                    CachedStatus value = entry.getValue();
                    boolean isParent = path.startsWith(key);
                    boolean isChild = key.startsWith(path);
                    boolean isEvicted = false;
                    if (isChild && value == CachedStatus.IS_MAPPING_ROOT || value == CachedStatus.NO_ROOT) {
                        keysToRemove.add(key);
                        isEvicted = true;
                    }

                    if (isParent && value == CachedStatus.IS_MAPPING_ROOT) {
                        keysToRemove.add(key);
                        isEvicted = true;
                    }

                    if (isEvicted) {
                        ourLogger.info(
                                String.format(
                                        "Evicting information about %s being %s because %s is %s",
                                        key,
                                        value,
                                        path,
                                        CachedStatus.IS_MAPPING_ROOT));
                    }
                }

                for (Path key : keysToRemove) {
                    myData.remove(key);
                }

                myData.put(path, CachedStatus.IS_MAPPING_ROOT);
            }
        }
    }

    public enum CachedStatus {
        /**
         * Item status isn't known from the cache.
         */
        UNKNOWN,
        /**
         * Item is known to be a TFVC workspace mapping root.
         */
        IS_MAPPING_ROOT,
        /**
         * Item is known to be under a TFVC workspace mapping root (while not itself being a mapping).
         */
        UNDER_MAPPING_ROOT,
        /**
         * Item is known to not be under a TFVC root.
         */
        NO_ROOT
    }
}
