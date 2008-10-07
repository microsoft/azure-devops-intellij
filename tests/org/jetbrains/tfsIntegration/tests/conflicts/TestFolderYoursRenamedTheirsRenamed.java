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

public class TestFolderYoursRenamedTheirsRenamed extends TestFolderConflict {

  private FilePath myBaseFolder;
  private FilePath myYoursFolder;
  private FilePath myTheirsFolder;
  private FilePath myMergedFolder;

  protected boolean canMerge() {
    return true;
  }

  protected void preparePaths() {
    myBaseFolder = getChildPath(mySandboxRoot, BASE_FOLDERNAME);
    myYoursFolder = getChildPath(mySandboxRoot, YOURS_FOLDERNAME);
    myTheirsFolder = getChildPath(mySandboxRoot, THEIRS_FOLDERNAME);
    myMergedFolder = getChildPath(mySandboxRoot, MERGED_FOLDERNAME);
  }

  protected void prepareBaseRevision() {
    createDirInCommand(myBaseFolder);
  }

  protected void prepareTargetRevision() throws VcsException, IOException {
    rename(myBaseFolder, THEIRS_FOLDERNAME);
  }

  protected void makeLocalChanges() throws IOException, VcsException {
    rename(myBaseFolder, YOURS_FOLDERNAME);
  }

  protected void checkResolvedYoursState() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myTheirsFolder, myYoursFolder);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myYoursFolder, 0);
  }

  protected void checkResolvedTheirsState() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myTheirsFolder, 0);
  }

  protected void checkResolvedMergeState() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myTheirsFolder, myMergedFolder);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myMergedFolder, 0);
  }

  protected void checkConflictProperties(final Conflict conflict) throws TfsException {
    Assert.assertTrue(EnumMask.fromString(ChangeType.class, conflict.getYchg()).containsOnly(ChangeType.Rename));
    Assert.assertTrue(EnumMask.fromString(ChangeType.class, conflict.getBchg()).containsOnly(ChangeType.Rename));
    Assert.assertEquals(VersionControlPath.toTfsRepresentation(myYoursFolder), conflict.getSrclitem());
    Assert.assertEquals(VersionControlPath.toTfsRepresentation(myYoursFolder), conflict.getTgtlitem());

    Assert.assertEquals(findServerPath(myYoursFolder), conflict.getYsitem());
    Assert.assertEquals(findServerPath(myYoursFolder), conflict.getYsitemsrc());
    Assert.assertEquals(findServerPath(myBaseFolder), conflict.getBsitem());
    Assert.assertEquals(findServerPath(myTheirsFolder), conflict.getTsitem());
  }

  @Nullable
  protected String mergeName() throws TfsException {
    return findServerPath(myMergedFolder);
  }


  @Nullable
  protected String mergeContent() {
    Assert.fail("not supported");
    return null;
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