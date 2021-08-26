// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.extensions;

import com.microsoft.tfs.model.connector.TfsLocalPath;
import com.microsoft.tfs.model.connector.TfsServerPath;
import com.microsoft.tfs.model.connector.TfsWorkspaceMapping;
import org.junit.Test;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class TfvcRootCacheTests {

    private final TfvcRootCache myCache = new TfvcRootCache();

    private TfsWorkspaceMapping createMapping(Path path) {
        return new TfsWorkspaceMapping(new TfsLocalPath(path.toString()), new TfsServerPath("", ""), false);
    }

    @Test
    public void emptyCacheShouldReturnUnknown() {
        Path path = Paths.get("/tmp");
        assertEquals(TfvcRootCache.CachedStatus.UNKNOWN, myCache.get(path));
    }

    @Test
    public void cacheShouldReturnTheCachedMappingRoot() {
        Path path = Paths.get("/tmp");
        myCache.putMappings(Collections.singletonList(createMapping(path)));
        assertEquals(TfvcRootCache.CachedStatus.IS_MAPPING_ROOT, myCache.get(path));
    }

    @Test
    public void cacheShouldReturnTheCachedEmptySpace() {
        Path path = Paths.get("/tmp");
        myCache.putNoMappingsFor(path);
        assertEquals(TfvcRootCache.CachedStatus.NO_ROOT, myCache.get(path));
    }

    @Test
    public void cacheKnowsAnyParentOfAnEmptySpaceIsNotARoot() {
        Path path = Paths.get("/tmp/path1");
        myCache.putNoMappingsFor(path);
        assertEquals(TfvcRootCache.CachedStatus.NO_ROOT, myCache.get(path.getParent()));
    }

    @Test
    public void cacheDoesntKnowChildOfEmptySpaceStatus() {
        Path path = Paths.get("/tmp");
        myCache.putNoMappingsFor(path);
        assertEquals(TfvcRootCache.CachedStatus.UNKNOWN, myCache.get(path.resolve("child")));
    }

    @Test
    public void cacheKnowsAnyParentOfAMappedRootIsNotARoot() {
        Path path = Paths.get("/tmp/path1");
        myCache.putMappings(Collections.singletonList(createMapping(path)));
        assertEquals(TfvcRootCache.CachedStatus.NO_ROOT, myCache.get(path.getParent()));
    }

    @Test
    public void cacheKnowsAnyChildAMappedRootIsUnderIt() {
        Path path = Paths.get("/tmp");
        myCache.putMappings(Collections.singletonList(createMapping(path)));
        assertEquals(TfvcRootCache.CachedStatus.UNDER_MAPPING_ROOT, myCache.get(path.resolve("child")));
    }

    @Test
    public void cacheDestroysTheOlderItemOnConflictingUpdate() {
        // Consider the /tmp is mapped, which means /tmp/child is under mapped root:
        Path path = Paths.get("/tmp");
        Path child = path.resolve("child");
        Path sibling = path.resolve("child2");
        myCache.putMappings(Collections.singletonList(createMapping(path)));
        assertEquals(TfvcRootCache.CachedStatus.UNDER_MAPPING_ROOT, myCache.get(child));
        assertEquals(TfvcRootCache.CachedStatus.UNDER_MAPPING_ROOT, myCache.get(sibling));

        // Now consider we had contradicting result that /tmp/child isn't mapped:
        myCache.putNoMappingsFor(child);
        assertEquals(TfvcRootCache.CachedStatus.NO_ROOT, myCache.get(child));

        // It means the cache now knows the parent isn't actually a root:
        assertEquals(TfvcRootCache.CachedStatus.NO_ROOT, myCache.get(path));

        // Any other siblings of that path aren't under mapping now, since the cache evicts any contradictory
        // information:
        assertEquals(TfvcRootCache.CachedStatus.UNKNOWN, myCache.get(sibling));

        // If we suddenly again get the information that the /tmp is a root, then the children are under it:
        myCache.putMappings(Collections.singletonList(createMapping(path)));
        assertEquals(TfvcRootCache.CachedStatus.UNDER_MAPPING_ROOT, myCache.get(child));
        assertEquals(TfvcRootCache.CachedStatus.UNDER_MAPPING_ROOT, myCache.get(sibling));
    }

    @Test(expected = InvalidPathException.class)
    public void cacheThrowsAnExceptionIfServiceDirectoryIsCached_1() {
        Path path = Paths.get("/$tf/smth");
        myCache.putNoMappingsFor(path);
    }

    @Test(expected = InvalidPathException.class)
    public void cacheThrowsAnExceptionIfServiceDirectoryIsCached_2() {
        Path path = Paths.get("/$tf/smth");
        myCache.putMappings(Collections.singletonList(createMapping(path)));
    }
}
