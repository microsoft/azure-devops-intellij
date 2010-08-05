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

public class TestFileYoursRenamedModifiedTheirsMissing extends TestFileConflict {

  private FilePath myBaseFile;
  private FilePath myYoursFile;

  protected boolean canMerge() {
    return false;
  }

  protected void preparePaths() {
    myBaseFile = getChildPath(mySandboxRoot, BASE_FILENAME);
    myYoursFile = getChildPath(mySandboxRoot, YOURS_FILENAME);
  }

  protected void prepareBaseRevision() {
    createFileInCommand(myBaseFile, BASE_CONTENT);
  }

  protected void prepareTargetRevision() throws VcsException, IOException {
    deleteFileInCommand(myBaseFile);
  }

  protected void makeLocalChanges() throws IOException, VcsException {
    editFiles(myBaseFile);
    rename(myBaseFile, YOURS_FILENAME);
    setFileContent(myYoursFile, YOURS_CONTENT);
  }

  protected void checkResolvedYoursState() throws VcsException {
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(myBaseFile, myYoursFile, BASE_CONTENT, YOURS_CONTENT);

    assertFolder(mySandboxRoot, 1);
    assertFile(myYoursFile, YOURS_CONTENT, true);
  }

  protected void checkResolvedTheirsState() throws VcsException {
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 0);
  }

  protected void checkResolvedMergeState() throws VcsException {
    Assert.fail("can't merge");
  }

  protected void checkConflictProperties(final Conflict conflict) throws TfsException {
    Assert.assertTrue(new ChangeTypeMask(conflict.getYchg()).containsOnly(ChangeType_type0.Edit, ChangeType_type0.Rename));
    Assert.assertTrue(new ChangeTypeMask(conflict.getBchg()).containsOnly(ChangeType_type0.Delete));
    Assert.assertEquals(myYoursFile, VersionControlPath.getFilePath(conflict.getSrclitem(), false));
    Assert.assertNull(conflict.getTgtlitem());
    Assert.assertEquals(findServerPath(myYoursFile), conflict.getYsitem());
    Assert.assertEquals(findServerPath(myYoursFile), conflict.getYsitemsrc());
    Assert.assertEquals(findServerPath(myBaseFile), conflict.getBsitem());
    Assert.assertEquals(findServerPath(myBaseFile), conflict.getTsitem());
  }

  @Nullable
  protected String mergeName() throws TfsException {
    Assert.fail("not supported");
    return null;
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
