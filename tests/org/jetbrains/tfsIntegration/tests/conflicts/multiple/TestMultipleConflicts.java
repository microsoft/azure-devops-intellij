/*
 * Copyright 2000-2008 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.tfsIntegration.tests.conflicts.multiple;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.io.ReadOnlyAttributeUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.conflicts.*;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Conflict;
import org.jetbrains.tfsIntegration.tests.TFSTestCase;
import org.jetbrains.tfsIntegration.tests.conflicts.Resolution;
import org.jetbrains.tfsIntegration.ui.ContentTriplet;
import org.junit.Assert;

import java.io.IOException;
import java.util.*;

@SuppressWarnings({"HardCodedStringLiteral", "AutoUnboxing"})
abstract class TestMultipleConflicts extends TFSTestCase {

  interface ConflictingItem {
  }

  protected abstract void preparePaths();

  protected abstract void prepareBaseRevision() throws VcsException;

  protected abstract void prepareTargetRevision() throws VcsException, IOException;

  protected abstract void makeLocalChanges() throws IOException, VcsException;

  protected abstract void checkResolvedState(Map<ConflictingItem, Resolution> resolution) throws VcsException;

  @Nullable
  protected abstract String mergeContent(ConflictingItem conflictingItem);

  @Nullable
  protected abstract String mergeName(ConflictingItem conflictingItem) throws TfsException;

  protected abstract FilePath getPath(ConflictingItem conflictingItem);

  @Nullable
  protected abstract String getExpectedTheirsContent(final ConflictingItem conflictingItem);

  @Nullable
  protected abstract String getExpectedBaseContent(final ConflictingItem conflictingItem);

  @Nullable
  protected abstract String getExpectedYoursContent(final ConflictingItem conflictingItem);


  protected void doTest(List<Pair<ConflictingItem, Resolution>> resolutionPath) throws VcsException, IOException {
    try {
      preparePaths();
      prepareBaseRevision();

      List<Pair<Integer/*item id*/, Resolution>> itemId2Resolution = new ArrayList<Pair<Integer, Resolution>>(resolutionPath.size());
      final Map<Integer, ConflictingItem> id2item = new HashMap<Integer, ConflictingItem>();
      for (Pair<ConflictingItem, Resolution> pair : resolutionPath) {
        final int itemId = getItemId(getPath(pair.first));
        itemId2Resolution.add(Pair.create(itemId, pair.second));
        id2item.put(itemId, pair.first);
      }

      final ConflictsHandlerImpl conflictsHandler = new ConflictsHandlerImpl(itemId2Resolution);
      ConflictsEnvironment.setConflictsHandler(conflictsHandler);
      ConflictsEnvironment.setNameMerger(new NameMergerImpl(id2item));
      ConflictsEnvironment.setContentMerger(new ContentMergerImpl(id2item));


      commit(getChanges().getChanges(), "Base revision");
      prepareTargetRevision();
      commit(getChanges().getChanges(), "Target revision");
      updateTo(1);
      makeLocalChanges();
      updateTo(0);

      checkResolvedState(toMap(resolutionPath));
    }
    catch (TfsException e) {
      throw new VcsException(e);
    }
    finally {
      ConflictsEnvironment.setConflictsHandler(null);
      ConflictsEnvironment.setNameMerger(null);
      ConflictsEnvironment.setContentMerger(null);
    }
  }

  private static class ConflictsHandlerImpl implements ConflictsHandler {
    private final List<Pair<Integer, Resolution>> myResolution;

    public ConflictsHandlerImpl(final List<Pair<Integer, Resolution>> resolution) {
      myResolution = resolution;
    }

    public void resolveConflicts(final ResolveConflictHelper resolveConflictHelper) throws TfsException {
      Assert.assertEquals("Expected conflicts count differs: ", myResolution.size(), resolveConflictHelper.getConflicts().size());
      for (Pair<Integer, Resolution> resolutionPair : myResolution) {
        Conflict conflict = findConflict(resolveConflictHelper.getConflicts(), resolutionPair.first);
        try {
          if (resolutionPair.second == Resolution.AcceptYours) {
            resolveConflictHelper.acceptYours(conflict);
          }
          else if (resolutionPair.second == Resolution.AcceptTheirs) {
            resolveConflictHelper.acceptTheirs(conflict);
          }
          else {
            resolveConflictHelper.acceptMerge(conflict);
          }
        }
        catch (VcsException e) {
          Assert.fail(e.getMessage());
        }
        catch (IOException e) {
          Assert.fail(e.getMessage());
        }
      }
      Assert.assertEquals("All conflicts should have been resolved", 0, resolveConflictHelper.getConflicts().size());
    }

    private static Conflict findConflict(Collection<Conflict> conflicts, int itemId) {
      for (Conflict conflict : conflicts) {
        if (conflict.getBitemid() == itemId) {
          return conflict;
        }
      }
      Assert.fail("Conflict with item id " + itemId + " not found");
      //noinspection ConstantConditions
      return null;
    }
  }

  private class NameMergerImpl implements NameMerger {
    private final Map<Integer /*itemId*/, ConflictingItem> myId2item;

    public NameMergerImpl(final Map<Integer, ConflictingItem> id2item) {
      myId2item = id2item;
    }

    @Nullable
    public String mergeName(final WorkspaceInfo workspace, final Conflict conflict) {
      try {
        return TestMultipleConflicts.this.mergeName(myId2item.get(conflict.getBitemid()));
      }
      catch (TfsException e) {
        Assert.fail(e.getMessage());
        return null;
      }
    }
  }

  private class ContentMergerImpl implements ContentMerger {
    private final Map<Integer /*itemId*/, ConflictingItem> myId2item;

    public ContentMergerImpl(final Map<Integer, ConflictingItem> id2item) {
      myId2item = id2item;
    }

    public void mergeContent(final Conflict conflict,
                             final ContentTriplet contentTriplet,
                             final Project project,
                             final VirtualFile targetFile,
                             final String localPathToDisplay) throws IOException, VcsException {
      Assert.assertEquals(getExpectedBaseContent(myId2item.get(conflict.getBitemid())), contentTriplet.baseContent);
      Assert.assertEquals(getExpectedYoursContent(myId2item.get(conflict.getBitemid())), contentTriplet.localContent);
      Assert.assertEquals(getExpectedTheirsContent(myId2item.get(conflict.getBitemid())), contentTriplet.serverContent);

      ReadOnlyAttributeUtil.setReadOnlyAttribute(targetFile, false);
      setFileContent(targetFile, TestMultipleConflicts.this.mergeContent(myId2item.get(conflict.getBitemid())));
    }

  }

  protected static <K, V> Map<K, V> toMap(List<Pair<K, V>> list) {
    Map<K, V> map = new HashMap<K, V>(list.size());
    for (Pair<K, V> pair : list) {
      map.put(pair.first, pair.second);
    }
    return map;
  }

}