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

@SuppressWarnings({"HardCodedStringLiteral"})
public class TestFileYoursDeletedTheirsRenamedModified extends TestFileConflict {

  private FilePath myBaseFile;
  private FilePath myTheirsFile;

  protected boolean canMerge() {
    return false;
  }

  protected void preparePaths() {
    myBaseFile = getChildPath(mySandboxRoot, BASE_FILENAME);
    myTheirsFile = getChildPath(mySandboxRoot, THEIRS_FILENAME);
  }

  protected void prepareBaseRevision() {
    createFileInCommand(myBaseFile, BASE_CONTENT);
  }

  protected void prepareTargetRevision() throws VcsException, IOException {
    rename(myBaseFile, THEIRS_FILENAME);
    editFiles(myTheirsFile);
    setFileContent(myTheirsFile, THEIRS_CONTENT);
  }

  protected void makeLocalChanges() throws IOException, VcsException {
    deleteFileInCommand(myBaseFile);
    // need to try commit to have conflict reported on next Get
    try {
      commitThrowException(getChanges().getChanges(), "should fail");
      Assert.fail("Should fail");
    }
    catch (VcsException e) {
      // exception expected: A newer version of base.txt exists on the server.
    }
  }

  protected void checkResolvedYoursState() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertScheduledForDeletion(myTheirsFile);

    assertFolder(mySandboxRoot, 0);
  }

  protected void checkResolvedTheirsState() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFile(myTheirsFile, THEIRS_CONTENT, false);
  }

  protected void checkResolvedMergeState() throws VcsException {
    Assert.fail("not supported");
  }

  protected void checkConflictProperties(final Conflict conflict) throws TfsException {
    Assert.assertTrue(EnumMask.fromString(ChangeType.class, conflict.getYchg()).containsOnly(ChangeType.Delete));
    Assert.assertTrue(EnumMask.fromString(ChangeType.class, conflict.getBchg()).containsOnly(ChangeType.Edit, ChangeType.Rename));
    Assert.assertNull(conflict.getSrclitem());
    Assert.assertEquals(myBaseFile, VersionControlPath.getFilePath(conflict.getTgtlitem(), false));
    Assert.assertEquals(findServerPath(myTheirsFile), conflict.getYsitem());
    Assert.assertEquals(findServerPath(myBaseFile), conflict.getYsitemsrc());
    Assert.assertEquals(findServerPath(myBaseFile), conflict.getBsitem());
    Assert.assertEquals(findServerPath(myTheirsFile), conflict.getTsitem());
  }

  @Nullable
  protected String mergeName() {
    Assert.fail("not supported");
    return null;
  }

  @Nullable
  protected String mergeContent() {
    Assert.fail("not supported");
    return null;
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