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
import org.jetbrains.tfsIntegration.core.tfs.TfsFileUtil;
import org.jetbrains.tfsIntegration.core.tfs.conflicts.ConflictsHandler;
import org.jetbrains.tfsIntegration.core.tfs.conflicts.ContentConflictsHandler;
import org.jetbrains.tfsIntegration.core.tfs.conflicts.NameConflictsHandler;
import org.jetbrains.tfsIntegration.core.tfs.conflicts.ResolveConflictHelper;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Conflict;
import org.jetbrains.tfsIntegration.ui.ConflictData;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

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

    TFSUpdateEnvironment.setNameConflictsHandler(new NameConflictsHandler() {
      public String mergeName(Conflict conflict) {
        Assert.fail("It must be only content conflict!");
        return null;
      }
    });
    TFSUpdateEnvironment.setContentConflictsHandler(new ContentConflictsHandler() {
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
    update(file, latestRevisionNumber - 1);
    assertFile(file, CONTENT_1, false);
    getChanges().assertTotalItems(1);
    FilePath pathTo = TfsFileUtil.getFilePath(file);
    FilePath pathFrom = addChildPath(mySandboxRoot, NAME_2);
    getChanges().assertRenamedOrMoved(pathFrom, pathTo);
  }

  @Test
  public void testNameResolveAcceptTheirs() throws VcsException, IOException {
    final VirtualFile file = prepareNameConflict();
    int latestRevisionNumber = getLatestRevisionNumber(mySandboxRoot);

    TFSUpdateEnvironment.setResolveConflictsHandler(new AcceptTheirsConflictsHandler(new SizeConflictsAsserter(1)));
    update(file, latestRevisionNumber - 1);
    FilePath path = addChildPath(mySandboxRoot, NAME_1);
    assertFile(path.getVirtualFile(), CONTENT_1, false);
    getChanges().assertTotalItems(0);
  }

  @Test
  public void testNameResolveAcceptMerge() throws VcsException, IOException {
    final VirtualFile file = prepareNameConflict();
    int latestRevisionNumber = getLatestRevisionNumber(mySandboxRoot);

    TFSUpdateEnvironment.setNameConflictsHandler(new NameConflictsHandler() {
      public String mergeName(Conflict conflict) {
        String yourServerItem = conflict.getYsitem();
        return yourServerItem.replace(NAME_3, NAME_4);
      }
    });
    TFSUpdateEnvironment.setContentConflictsHandler(new ContentConflictsHandler() {
      public void mergeContent(Conflict conflict, ConflictData conflictData, Project project, VirtualFile localFile, String localPath) {
        Assert.fail("It must be only name conflict!");
      }
    });
    TFSUpdateEnvironment.setResolveConflictsHandler(new AcceptMergeConflictsHandler(new SizeConflictsAsserter(1)));
    update(file, latestRevisionNumber - 1);
    FilePath pathTo = addChildPath(mySandboxRoot, NAME_4);
    FilePath pathFrom = addChildPath(mySandboxRoot, NAME_2);
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

  public interface ConflictsAsserter {
    void assertConflicts(final List<Conflict> conflicts);
  }

  public static class SizeConflictsAsserter implements ConflictsAsserter {
    private int mySize;

    public SizeConflictsAsserter(final int size) {
      mySize = size;
    }

    public void assertConflicts(final List<Conflict> conflicts) {
      Assert.assertTrue(conflicts.size() == mySize);
    }
  }

  public static abstract class AbstractTestConflictsHandler implements ConflictsHandler {
    private ConflictsAsserter myConflictsAsserter;

    public AbstractTestConflictsHandler(ConflictsAsserter conflictsAsserter) {
      myConflictsAsserter = conflictsAsserter;
    }

    public boolean resolveConflicts(ResolveConflictHelper resolveConflictHelper) {
      myConflictsAsserter.assertConflicts(resolveConflictHelper.getConflicts());
      return doResolveConflicts(resolveConflictHelper);
    }

    abstract boolean doResolveConflicts(ResolveConflictHelper resolveConflictHelper);
  }

  public static class AcceptYoursConflictsHandler extends AbstractTestConflictsHandler {
    public AcceptYoursConflictsHandler(ConflictsAsserter conflictsAsserter) {
      super(conflictsAsserter);
    }

    boolean doResolveConflicts(final ResolveConflictHelper resolveConflictHelper) {
      List<Conflict> conflicts = resolveConflictHelper.getConflicts();
      for (Conflict conflict : conflicts) {
        resolveConflictHelper.acceptYours(conflict);
      }
      return true;
    }
  }

  public static class AcceptTheirsConflictsHandler extends AbstractTestConflictsHandler {
    public AcceptTheirsConflictsHandler(ConflictsAsserter conflictsAsserter) {
      super(conflictsAsserter);
    }

    boolean doResolveConflicts(final ResolveConflictHelper resolveConflictHelper) {
      List<Conflict> conflicts = resolveConflictHelper.getConflicts();
      for (Conflict conflict : conflicts) {
        try {
          resolveConflictHelper.acceptTheirs(conflict);
        }
        catch (TfsException e) {
          Assert.fail(e.getMessage());
        }
        catch (IOException e) {
          Assert.fail(e.getMessage());
        }
      }
      return true;
    }
  }

  public static class AcceptMergeConflictsHandler extends AbstractTestConflictsHandler {
    public AcceptMergeConflictsHandler(ConflictsAsserter conflictsAsserter) {
      super(conflictsAsserter);
    }

    boolean doResolveConflicts(final ResolveConflictHelper resolveConflictHelper) {
      List<Conflict> conflicts = resolveConflictHelper.getConflicts();
      for (Conflict conflict : conflicts) {
        try {
          resolveConflictHelper.acceptMerge(conflict);
        }
        catch (TfsException e) {
          Assert.fail(e.getMessage());
        }
        catch (VcsException e) {
          Assert.fail(e.getMessage());
        }
      }
      return true;
    }
  }
}