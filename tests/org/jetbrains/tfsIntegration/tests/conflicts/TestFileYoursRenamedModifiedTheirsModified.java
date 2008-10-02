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

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.ChangeType;
import org.jetbrains.tfsIntegration.core.tfs.EnumMask;
import org.jetbrains.tfsIntegration.core.tfs.VersionControlPath;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Conflict;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class TestFileYoursRenamedModifiedTheirsModified extends TestConflicts {

  private FilePath myBaseFile;
  private FilePath myYoursFile;
  private FilePath myMergedFile;

  protected boolean canMerge() {
    return true;
  }

  protected void preparePaths() {
    myBaseFile = getChildPath(mySandboxRoot, BASE_FILENAME);
    myYoursFile = getChildPath(mySandboxRoot, YOURS_FILENAME);
    myMergedFile = getChildPath(mySandboxRoot, MERGED_FILENAME);
  }

  protected void prepareBaseRevision() {
    createFileInCommand(myBaseFile, BASE_CONTENT);
  }

  protected void prepareTargetRevision() throws VcsException, IOException {
    editFiles(myBaseFile);
    setFileContent(myBaseFile, THEIRS_CONTENT);
  }

  protected void makeLocalChanges() throws IOException, VcsException {
    rename(myBaseFile, YOURS_FILENAME);
    editFiles(myYoursFile);
    setFileContent(myYoursFile, YOURS_CONTENT);
  }

  protected void checkResolvedYoursState() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myBaseFile, myYoursFile, THEIRS_CONTENT, YOURS_CONTENT);

    assertFolder(mySandboxRoot, 1);
    assertFile(myYoursFile, YOURS_CONTENT, true);
  }

  protected void checkResolvedTheirsState() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFile(myBaseFile, THEIRS_CONTENT, false);
  }

  protected void checkResolvedMergeState() throws VcsException {
    getChanges().assertTotalItems(1);

    getChanges().assertRenamedOrMoved(myBaseFile, myMergedFile, THEIRS_CONTENT, MERGED_CONTENT);

    assertFolder(mySandboxRoot, 1);
    assertFile(myMergedFile, MERGED_CONTENT, true);
  }

  protected void checkConflictProperties(final Conflict conflict) throws TfsException {
    Assert.assertTrue(EnumMask.fromString(ChangeType.class, conflict.getYchg()).containsOnly(ChangeType.Edit, ChangeType.Rename));
    Assert.assertTrue(EnumMask.fromString(ChangeType.class, conflict.getBchg()).containsOnly(ChangeType.Edit));
    Assert.assertEquals(VersionControlPath.toTfsRepresentation(myYoursFile), conflict.getSrclitem());
    Assert.assertEquals(VersionControlPath.toTfsRepresentation(myYoursFile), conflict.getTgtlitem());
    Assert.assertEquals(findServerPath(myYoursFile), conflict.getYsitem());
    Assert.assertEquals(findServerPath(myYoursFile), conflict.getYsitemsrc());
    Assert.assertEquals(findServerPath(myBaseFile), conflict.getBsitem());
    Assert.assertEquals(findServerPath(myBaseFile), conflict.getTsitem());
  }

  @Nullable
  protected String mergeName() throws TfsException {
    return findServerPath(myMergedFile);
  }


  @Nullable
  protected String mergeContent() {
    return MERGED_CONTENT;
  }

  @Nullable
  protected String getExpectedBaseContent() {
    return BASE_CONTENT;
  }

  @Nullable
  protected String getExpectedYoursContent() {
    return YOURS_CONTENT;
  }

  @Nullable
  protected String getExpectedTheirsContent() {
    return THEIRS_CONTENT;
  }

  @Test
  public void testAcceptYours() throws VcsException, IOException {
    super.testAcceptYours();
  }

  @Test
  public void testAcceptTheirs() throws VcsException, IOException {
    super.testAcceptTheirs();
  }

  @Test
  public void testAcceptMerge() throws VcsException, IOException {
    super.testAcceptMerge();
  }
}