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

package org.jetbrains.tfsIntegration.tests;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsConfiguration;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.tfsIntegration.core.TFSUpdateEnvironment;
import org.jetbrains.tfsIntegration.core.tfs.conflicts.ContentMerger;
import org.jetbrains.tfsIntegration.core.tfs.conflicts.NameMerger;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Conflict;
import org.jetbrains.tfsIntegration.tests.conflicts.AcceptMergeConflictsHandler;
import org.jetbrains.tfsIntegration.tests.conflicts.AcceptTheirsConflictsHandler;
import org.jetbrains.tfsIntegration.tests.conflicts.AcceptYoursConflictsHandler;
import org.jetbrains.tfsIntegration.tests.conflicts.SizeConflictsAsserter;
import org.jetbrains.tfsIntegration.ui.ConflictData;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * 1. Create file (rev 1.)
 * 2. Change content/name & check in (rev 2.)
 * 3. Change content/name (rev. 2 + pending changes)
 * 4. Update to rev 1.
 * Get Content/Name conflict, can choose
 * - Accept Yours
 * - Accept Theirs
 * - Merge
 */

@SuppressWarnings({"HardCodedStringLiteral"})
public class TestSimpleResolveConflicts extends TFSTestCase {
  private static final String CONTENT_1 = "Content 1";
  private static final String CONTENT_2 = "Content 2";
  private static final String CONTENT_3 = "Content 3";
  private static final String CONTENT_4 = "Content 4";

  private static final String NAME_1 = "file1.txt";
  private static final String NAME_2 = "file2.txt";
  private static final String NAME_3 = "file3.txt";
  private static final String NAME_4 = "file4.txt";

  @Test
  public void testContentResolveAcceptYours() throws VcsException, IOException {
    final VirtualFile file = prepareContentConflict();
    int latestRevisionNumber = getLatestRevisionNumber(file);

    TFSUpdateEnvironment.setResolveConflictsHandler(new AcceptYoursConflictsHandler(new SizeConflictsAsserter(1)));
    update(file, latestRevisionNumber - 1);
    assertFile(file, CONTENT_3, true);
    getChanges().assertTotalItems(1);
    getChanges().assertModified(file);
  }

  @Test
  public void testContentResolveAcceptTheirs() throws VcsException, IOException {
    final VirtualFile file = prepareContentConflict();
    int latestRevisionNumber = getLatestRevisionNumber(file);

    TFSUpdateEnvironment.setResolveConflictsHandler(new AcceptTheirsConflictsHandler(new SizeConflictsAsserter(1)));
    update(file, latestRevisionNumber - 1);
    assertFile(file, CONTENT_1, false);
    getChanges().assertTotalItems(0);
  }

  @Test
  public void testContentResolveAcceptMerge() throws VcsException, IOException {
    final VirtualFile file = prepareContentConflict();
    int latestRevisionNumber = getLatestRevisionNumber(file);

    TFSUpdateEnvironment.setNameConflictsHandler(new NameMerger() {
      public String mergeName(Conflict conflict) {
        Assert.fail("It must be only content conflict!");
        return null;
      }
    });
    TFSUpdateEnvironment.setContentConflictsHandler(new ContentMerger() {
      public void mergeContent(Conflict conflict, ConflictData conflictData, Project project, VirtualFile localFile, String localPath) {
        try {
          setFileContent(localFile, CONTENT_4);
        }
        catch (IOException e) {
          Assert.fail(e.getMessage());
        }
      }
    });
    TFSUpdateEnvironment.setResolveConflictsHandler(new AcceptMergeConflictsHandler(new SizeConflictsAsserter(1)));
    update(file, latestRevisionNumber - 1);
    assertFile(file, CONTENT_4, true);
    getChanges().assertTotalItems(1);
    getChanges().assertModified(file);
  }

  @Test
  public void testNameResolveAcceptYours() throws VcsException, IOException {
    final VirtualFile file = prepareNameConflict();
    int latestRevisionNumber = getLatestRevisionNumber(mySandboxRoot);

    TFSUpdateEnvironment.setResolveConflictsHandler(new AcceptYoursConflictsHandler(new SizeConflictsAsserter(1)));
    getChanges().assertTotalItems(1);
    FilePath pathFrom = getChildPath(mySandboxRoot, NAME_2);
    FilePath pathTo = getChildPath(mySandboxRoot, NAME_3);
    getChanges().assertRenamedOrMoved(pathFrom, pathTo);
    update(file, latestRevisionNumber - 1);
    assertFile(file, CONTENT_1, false);
    getChanges().assertTotalItems(1);
    pathFrom = getChildPath(mySandboxRoot, NAME_1);
    getChanges().assertRenamedOrMoved(pathFrom, pathTo);
  }

  @Test
  public void testNameResolveAcceptTheirs() throws VcsException, IOException {
    final VirtualFile file = prepareNameConflict();
    int latestRevisionNumber = getLatestRevisionNumber(mySandboxRoot);

    TFSUpdateEnvironment.setResolveConflictsHandler(new AcceptTheirsConflictsHandler(new SizeConflictsAsserter(1)));
    update(file, latestRevisionNumber - 1);
    FilePath path = getChildPath(mySandboxRoot, NAME_1);
    assertFile(path.getVirtualFile(), CONTENT_1, false);
    getChanges().assertTotalItems(0);
  }

  @Test
  public void testNameResolveAcceptMerge() throws VcsException, IOException {
    final VirtualFile file = prepareNameConflict();
    int latestRevisionNumber = getLatestRevisionNumber(mySandboxRoot);

    TFSUpdateEnvironment.setNameConflictsHandler(new NameMerger() {
      public String mergeName(Conflict conflict) {
        String yourServerItem = conflict.getYsitem();
        return yourServerItem.replace(NAME_3, NAME_4);
      }
    });
    TFSUpdateEnvironment.setContentConflictsHandler(new ContentMerger() {
      public void mergeContent(Conflict conflict, ConflictData conflictData, Project project, VirtualFile localFile, String localPath) {
        Assert.fail("It must be only name conflict!");
      }
    });
    TFSUpdateEnvironment.setResolveConflictsHandler(new AcceptMergeConflictsHandler(new SizeConflictsAsserter(1)));
    update(file, latestRevisionNumber - 1);
    FilePath pathTo = getChildPath(mySandboxRoot, NAME_4);
    FilePath pathFrom = getChildPath(mySandboxRoot, NAME_2);
    assertFile(pathTo.getVirtualFile(), CONTENT_1, false);
    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(pathFrom, pathTo);
  }

  private VirtualFile prepareContentConflict() throws VcsException, IOException {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    doActionSilently(VcsConfiguration.StandardConfirmation.REMOVE);

    final VirtualFile file = createFileInCommand(mySandboxRoot, NAME_1, CONTENT_1);
    commit(getChanges().getChanges(), "rev. 1");
    assertFile(file, CONTENT_1, false);

    editFiles(file);
    setFileContent(file, CONTENT_2);
    commit(getChanges().getChanges(), "rev. 2");
    assertFile(file, CONTENT_2, false);

    editFiles(file);
    setFileContent(file, CONTENT_3);
    return file;
  }

  private VirtualFile prepareNameConflict() throws VcsException, IOException {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    doActionSilently(VcsConfiguration.StandardConfirmation.REMOVE);

    final VirtualFile file = createFileInCommand(mySandboxRoot, NAME_1, CONTENT_1);
    commit(getChanges().getChanges(), "rev. 1");
    assertFile(file, CONTENT_1, false);

    rename(file, NAME_2);
    commit(getChanges().getChanges(), "rev. 2");
    assertFile(file, CONTENT_1, false);

    rename(file, NAME_3);
    return file;
  }

}
