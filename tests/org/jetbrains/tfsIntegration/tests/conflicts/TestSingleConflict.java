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

package org.jetbrains.tfsIntegration.tests.conflicts;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.io.ReadOnlyAttributeUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.conflicts.*;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Conflict;
import org.jetbrains.tfsIntegration.tests.TFSTestCase;
import org.jetbrains.tfsIntegration.ui.ContentTriplet;
import org.junit.Assert;

import java.io.IOException;
import java.util.Collection;

@SuppressWarnings({"HardCodedStringLiteral"})
abstract class TestSingleConflict extends TFSTestCase {

  protected abstract boolean canMerge();

  protected boolean updateToThePast() {
    return false;
  }

  protected abstract void preparePaths();

  protected abstract void prepareBaseRevision() throws VcsException;

  protected abstract void prepareTargetRevision() throws VcsException, IOException;

  protected abstract void makeLocalChanges() throws IOException, VcsException;

  protected abstract void checkConflictProperties(Conflict conflict) throws TfsException;

  protected abstract void checkResolvedYoursState() throws VcsException;

  protected abstract void checkResolvedTheirsState() throws VcsException;

  protected abstract void checkResolvedMergeState() throws VcsException;

  @Nullable
  protected String getExpectedBaseContent() {
    Assert.fail("not supported");
    return null;
  }

  @Nullable
  protected String getExpectedYoursContent() {
    Assert.fail("not supported");
    return null;
  }

  @Nullable
  protected String getExpectedTheirsContent() {
    Assert.fail("not supported");
    return null;
  }

  @Nullable
  protected abstract String mergeContent();

  @Nullable
  protected abstract String mergeName() throws TfsException;

  private void doTest(final Resolution resolution) throws VcsException, IOException {
    ConflictsEnvironment.setResolveConflictsHandler(new ConflictsHandlerImpl(resolution));
    ConflictsEnvironment.setNameMerger(new NameMergerImpl());
    ConflictsEnvironment.setContentMerger(new ContentMergerImpl());

    try {
      preparePaths();
      prepareBaseRevision();
      commit(getChanges().getChanges(), "Base revision");
      if (updateToThePast()) {
        makeLocalChanges();
        updateTo(1);
      }
      else {
        prepareTargetRevision();
        commit(getChanges().getChanges(), "Target revision");
        updateTo(1);
        makeLocalChanges();
        updateTo(0);
      }

      if (resolution == Resolution.AcceptYours) {
        checkResolvedYoursState();
      }
      else if (resolution == Resolution.AcceptTheirs) {
        checkResolvedTheirsState();
      }
      else {
        if (canMerge()) {
          checkResolvedMergeState();
        }
      }

    }
    finally {
      ConflictsEnvironment.setResolveConflictsHandler(null);
      ConflictsEnvironment.setNameMerger(null);
      ConflictsEnvironment.setContentMerger(null);
    }
  }

  protected void testAcceptYours() throws VcsException, IOException {
    doTest(Resolution.AcceptYours);
  }

  protected void testAcceptTheirs() throws VcsException, IOException {
    doTest(Resolution.AcceptTheirs);
  }

  protected void testAcceptMerge() throws VcsException, IOException {
    doTest(Resolution.Merge);
  }

  private class ConflictsHandlerImpl implements ConflictsHandler {
    private final Resolution myResolution;

    public ConflictsHandlerImpl(final Resolution resolution) {
      myResolution = resolution;
    }

    public void resolveConflicts(final ResolveConflictHelper resolveConflictHelper) throws TfsException {
      Collection<Conflict> conflicts = resolveConflictHelper.getConflicts();
      Assert.assertEquals("Expected conflicts count differs: ", 1, conflicts.size());
      for (Conflict conflict : conflicts) {
        checkConflictProperties(conflict);
        try {
          if (myResolution == Resolution.AcceptYours) {
            resolveConflictHelper.acceptYours(conflict);
          }
          else if (myResolution == Resolution.AcceptTheirs) {
            resolveConflictHelper.acceptTheirs(conflict);
          }
          else {
            Assert.assertEquals(canMerge(), ResolveConflictHelper.canMerge(conflict));
            if (canMerge()) {
              resolveConflictHelper.acceptMerge(conflict);
            }
            else {
              resolveConflictHelper.skip(conflict);
            }
          }
        }
        catch (VcsException e) {
          Assert.fail(e.getMessage());
        }
        catch (IOException e) {
          Assert.fail(e.getMessage());
        }
      }
    }
  }

  private class NameMergerImpl implements NameMerger {
    @Nullable
    public String mergeName(final WorkspaceInfo workspace, final Conflict conflict) {
      try {
        return TestSingleConflict.this.mergeName();
      }
      catch (TfsException e) {
        Assert.fail(e.getMessage());
        return null;
      }
    }
  }

  private class ContentMergerImpl implements ContentMerger {
    public void mergeContent(final Conflict conflict,
                             final ContentTriplet contentTriplet,
                             final Project project,
                             final VirtualFile targetFile,
                             final String localPathToDisplay) throws IOException, VcsException {
      Assert.assertEquals(getExpectedBaseContent(), contentTriplet.baseContent);
      Assert.assertEquals(getExpectedYoursContent(), contentTriplet.localContent);
      Assert.assertEquals(getExpectedTheirsContent(), contentTriplet.serverContent);

      ReadOnlyAttributeUtil.setReadOnlyAttribute(targetFile, false);
      setFileContent(targetFile, TestSingleConflict.this.mergeContent());
    }
  }
}
