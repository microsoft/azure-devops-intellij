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

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.tests.conflicts.Resolution;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

// Notes
// 1. On update server sends get operation to rename BaseParent -> TheirsParent. After this operation was performed and ULV(TheirsFolder)
//  sent to server conflict.srclitem = BaseParent\YoursChild, not TheirsParent\YoursChild, so TheirsParent\YoursChild is preserved  
//

@SuppressWarnings({"HardCodedStringLiteral"})
public class TestTheirsParentFolderRenamedYoursChildFolderRenamed extends TestMultipleConflicts {

  private static final ConflictingItem CHILD = new ConflictingItem() {
  };

  private FilePath myBaseParentFolder;
  private FilePath myTheirsParentFolder;
  private FilePath myMergedParentFolder;
  private FilePath myBaseChildFolderInBaseParent;
  private FilePath myBaseChildFolderInTheirsParent;
  private FilePath myBaseChildFolderInMergedParent;
  private FilePath myYoursChildFolderInBaseParent;
  private FilePath myYoursChildFolderInTheirsParent;
  private FilePath myYoursChildFolderInMergedParent;
  private FilePath myMergedChildFolderInBaseParent;
  private FilePath myMergedChildFolderInTheirsParent;
  private FilePath myMergedChildFolderInMergedParent;

  private static final String BASE_PARENT_NAME = "BaseParent";
  private static final String THEIRS_PARENT_NAME = "TheirsParent";
  private static final String MERGED_PARENT_NAME = "MergedParent";

  private static final String BASE_CHILD_NAME = "BaseChild";
  private static final String YOURS_CHILD_NAME = "YoursChild";
  private static final String MERGED_CHILD_NAME = "MergedChild";

  protected void preparePaths() {
    myBaseParentFolder = getChildPath(mySandboxRoot, BASE_PARENT_NAME);
    myTheirsParentFolder = getChildPath(mySandboxRoot, THEIRS_PARENT_NAME);
    myMergedParentFolder = getChildPath(mySandboxRoot, MERGED_PARENT_NAME);

    myBaseChildFolderInBaseParent = getChildPath(myBaseParentFolder, BASE_CHILD_NAME);
    myBaseChildFolderInTheirsParent = getChildPath(myTheirsParentFolder, BASE_CHILD_NAME);
    myBaseChildFolderInMergedParent = getChildPath(myMergedParentFolder, BASE_CHILD_NAME);

    myYoursChildFolderInBaseParent = getChildPath(myBaseParentFolder, YOURS_CHILD_NAME);
    myYoursChildFolderInTheirsParent = getChildPath(myTheirsParentFolder, YOURS_CHILD_NAME);
    myYoursChildFolderInMergedParent = getChildPath(myMergedParentFolder, YOURS_CHILD_NAME);

    myMergedChildFolderInBaseParent = getChildPath(myBaseParentFolder, MERGED_CHILD_NAME);
    myMergedChildFolderInTheirsParent = getChildPath(myTheirsParentFolder, MERGED_CHILD_NAME);
    myMergedChildFolderInMergedParent = getChildPath(myMergedParentFolder, MERGED_CHILD_NAME);
  }

  protected void prepareBaseRevision() throws VcsException {
    createDirInCommand(myBaseParentFolder);
    createDirInCommand(myBaseChildFolderInBaseParent);
  }

  protected void prepareTargetRevision() throws VcsException, IOException {
    renameFileInCommand(myBaseParentFolder, THEIRS_PARENT_NAME);
  }

  protected void makeLocalChanges() throws IOException, VcsException {
    renameFileInCommand(myBaseChildFolderInBaseParent, YOURS_CHILD_NAME);
  }

  protected void checkResolvedState(final Map<ConflictingItem, Resolution> resolution) throws VcsException {
    if (resolution.get(CHILD) == Resolution.AcceptTheirs) {
      // see Note1
      getChanges().assertTotalItems(1);
      getChanges().assertUnversioned(myYoursChildFolderInTheirsParent);

      assertFolder(mySandboxRoot, 1);
      assertFolder(myTheirsParentFolder, 2);
      assertFolder(myBaseChildFolderInTheirsParent, 0);
      assertFolder(myYoursChildFolderInTheirsParent, 0);
    }
    else if (resolution.get(CHILD) == Resolution.AcceptYours) {
      getChanges().assertTotalItems(1);
      getChanges().assertRenamedOrMoved(myBaseChildFolderInBaseParent, myYoursChildFolderInTheirsParent);

      assertFolder(mySandboxRoot, 1);
      assertFolder(myTheirsParentFolder, 1);
      assertFolder(myYoursChildFolderInTheirsParent, 0);
    }
  }

  @Nullable
  protected String mergeContent(final ConflictingItem conflictingItem) {
    Assert.fail("not supported");
    return null;
  }

  @Nullable
  protected String mergeName(final ConflictingItem conflictingItem) throws TfsException {
    return MERGED_CHILD_NAME;
  }

  protected FilePath getPath(final ConflictingItem conflictingItem) {
    if (CHILD.equals(conflictingItem)) {
      return myBaseChildFolderInBaseParent;
    }
    throw new IllegalArgumentException(conflictingItem.toString());
  }

  @Nullable
  protected String getExpectedTheirsContent(final ConflictingItem conflictingItem) {
    Assert.fail("not supported");
    return null;
  }

  @Nullable
  protected String getExpectedBaseContent(final ConflictingItem conflictingItem) {
    Assert.fail("not supported");
    return null;
  }

  @Nullable
  protected String getExpectedYoursContent(final ConflictingItem conflictingItem) {
    Assert.fail("not supported");
    return null;
  }

  @Test
  public void testAcceptTheirs() throws IOException, VcsException {
    doTest(Arrays.asList(Pair.create(CHILD, Resolution.AcceptTheirs)));
  }

  @Test
  public void testAcceptYours() throws IOException, VcsException {
    doTest(Arrays.asList(Pair.create(CHILD, Resolution.AcceptYours)));
  }
}
