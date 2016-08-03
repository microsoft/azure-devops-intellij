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
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.ChangeType_type0;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.Conflict;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.ChangeTypeMask;
import org.jetbrains.tfsIntegration.core.tfs.VersionControlPath;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

@SuppressWarnings({"HardCodedStringLiteral"})
public class TestFolderYoursAddedTheirsAdded extends TestFolderConflict {

  private FilePath myBaseFolder;

  protected boolean updateToThePast() {
    return true;
  }

  protected boolean canMerge() {
    return false;
  }

  protected void preparePaths() {
    myBaseFolder = getChildPath(mySandboxRoot, BASE_FOLDERNAME);
  }

  protected void prepareBaseRevision() throws VcsException {
    createDirInCommand(myBaseFolder);
    commit(getChanges().getChanges(), "0-th revision");
    deleteFileInCommand(myBaseFolder);
  }

  protected void prepareTargetRevision() throws VcsException, IOException {
    Assert.fail("Not supported");
  }

  protected void makeLocalChanges() throws IOException, VcsException {
    createDirInCommand(myBaseFolder);
  }

  protected void checkResolvedYoursState() throws VcsException {
    Assert.fail("Not supported");
  }

  protected void checkResolvedTheirsState() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 1);
    assertFolder(myBaseFolder, 0);
  }

  protected void checkResolvedMergeState() throws VcsException {
    Assert.fail("can't merge");
  }

  @Nullable
  protected String mergeName() {
    Assert.fail("can't merge");
    return null;
  }

  protected void checkConflictProperties(final Conflict conflict) throws TfsException {
    Assert.assertTrue(
      new ChangeTypeMask(conflict.getYchg()).containsOnly(ChangeType_type0.Add, ChangeType_type0.Encoding));
    Assert.assertTrue(new ChangeTypeMask(conflict.getBchg()).containsOnly(ChangeType_type0.None));
    Assert.assertEquals(myBaseFolder, VersionControlPath.getFilePath(conflict.getSrclitem(), true));
    Assert.assertEquals(myBaseFolder, VersionControlPath.getFilePath(conflict.getTgtlitem(), true));
    Assert.assertEquals(findServerPath(myBaseFolder), conflict.getYsitem());
    Assert.assertEquals(findServerPath(myBaseFolder), conflict.getYsitemsrc());
    Assert.assertNull(conflict.getBsitem());
    Assert.assertEquals(findServerPath(myBaseFolder), conflict.getTsitem());
  }

  @Nullable
  protected String mergeContent() {
    Assert.fail("Not supported");
    return null;
  }

  /// don't test, otherwise TF14052: Cannot specify AcceptYours to resolve a namespace conflict.
  //@Test
  //public void testAcceptYours() throws VcsException, IOException {
  //  super.testAcceptYours();
  //}

  @Test
  public void testAcceptTheirs() throws VcsException, IOException {
    super.testAcceptTheirs();
  }

  @Test
  public void testAcceptMerge() throws VcsException, IOException {
    super.testAcceptMerge();
  }
}
