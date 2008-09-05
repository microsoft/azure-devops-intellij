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

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.RepositoryLocation;
import com.intellij.openapi.vcs.VcsConfiguration;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsHistorySession;
import com.intellij.openapi.vcs.versionBrowser.ChangeBrowserSettings;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.tfsIntegration.core.TFSChangeList;
import org.jetbrains.tfsIntegration.core.tfs.TfsFileUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

@SuppressWarnings({"HardCodedStringLiteral"})
public class TestHistoryProvider extends TFSTestCase {

  @Test
  public void testUpdate() throws VcsException, IOException {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    doActionSilently(VcsConfiguration.StandardConfirmation.REMOVE);
    final VirtualFile root = createDirInCommand(mySandboxRoot, "root");
    final VirtualFile folder1 = createDirInCommand(root, "folder1");
    final String fileModifiedContent1 = "fileModifiedRev1";
    final VirtualFile fileModified = createFileInCommand(folder1, "fileModified", fileModifiedContent1);
    final FilePath fileModified_original = TfsFileUtil.getFilePath(fileModified);
    final String fileRenamedContent = "fileRenamedRev1";
    final VirtualFile fileRenamed = createFileInCommand(folder1, "filepathRenamed_original", fileRenamedContent);
    final FilePath filepathRenamed_original = TfsFileUtil.getFilePath(fileRenamed);
    final String fileRenamedModifiedContent1 = "fileRenamedModifiedRev1";
    final VirtualFile fileRenamedModified = createFileInCommand(folder1, "filepathRenamedModified_original", fileRenamedModifiedContent1);
    final FilePath filepathRenamedModified_original = TfsFileUtil.getFilePath(fileRenamedModified);
    final VirtualFile fileDeleted = createFileInCommand(folder1, "fileDeleted", "fileDeletedRev1");
    final FilePath filepathDeleted = TfsFileUtil.getFilePath(fileDeleted);
    final VirtualFile subfolderRenamed = createDirInCommand(folder1, "subfolderRenamed_original");
    final FilePath filepathSubfolderRenamedOriginal = TfsFileUtil.getFilePath(subfolderRenamed);
    final VirtualFile subfolderDeleted = createDirInCommand(folder1, "subfolderRenamedDeleted");
    final FilePath filepathSubfolderDeleted = TfsFileUtil.getFilePath(subfolderDeleted);
    final VirtualFile subfolderMoved = createDirInCommand(folder1, "subfolderMoved");
    final FilePath filepathSubfolderMovedOriginal = TfsFileUtil.getFilePath(subfolderMoved);

    final String comment1 = "test rev1 committed";
    commit(getChanges().getChanges(), comment1);

    editFiles(fileModified, fileRenamedModified);
    final String fileModifiedContent2 = "fileModifiedRev2";
    setFileContent(fileModified, fileModifiedContent2);
    rename(fileRenamed, "fileRenamed_renamed");
    rename(fileRenamedModified, "fileRenamedModified_renamed");
    final String fileRenamedModifiedContent2 = "fileRenamedModifiedRev2";
    setFileContent(fileRenamedModified, fileRenamedModifiedContent2);

    final VirtualFile fileCreated = createFileInCommand(root, "fileCreated", "fileCreatedContent");
    deleteFileInCommand(fileDeleted);

    deleteFileInCommand(subfolderDeleted);
    rename(subfolderRenamed, "subfolderRenamed_newname");

    VirtualFile subfolderA = createDirInCommand(root, "subfolderA");
    VirtualFile subfolderB = createDirInCommand(subfolderA, "subfolderB");
    moveFileInCommand(subfolderMoved, subfolderB);

    final String comment2 = "test rev2 committed";
    commit(getChanges().getChanges(), comment2);

    final RepositoryLocation location = getVcs().getCommittedChangesProvider().getLocationFor(TfsFileUtil.getFilePath(root));
    ChangeBrowserSettings settings = new ChangeBrowserSettings();
    final List<TFSChangeList> historyList = getVcs().getCommittedChangesProvider().getCommittedChanges(settings, location, 0);

    Assert.assertEquals(historyList.size(), 2);
    {
      final TFSChangeList changelist1 = historyList.get(1);

      Assert.assertEquals(comment1, changelist1.getComment());
      Assert.assertEquals(getUsername(), changelist1.getCommitterName());

      String dump = ChangeHelper.toString(changelist1.getChanges(), mySandboxRoot);
      Assert.assertEquals(dump, 9, changelist1.getChanges().size());
      Assert.assertTrue(dump, ChangeHelper.containsAdded(changelist1.getChanges(), root));
      Assert.assertTrue(dump, ChangeHelper.containsAdded(changelist1.getChanges(), folder1));
      Assert.assertTrue(dump, ChangeHelper.containsAdded(changelist1.getChanges(), fileModified_original));
      Assert.assertTrue(dump, ChangeHelper.containsAdded(changelist1.getChanges(), filepathRenamed_original));
      Assert.assertTrue(dump, ChangeHelper.containsAdded(changelist1.getChanges(), filepathRenamedModified_original));
      Assert.assertTrue(dump, ChangeHelper.containsAdded(changelist1.getChanges(), filepathDeleted));
      Assert.assertTrue(dump, ChangeHelper.containsAdded(changelist1.getChanges(), filepathSubfolderRenamedOriginal));
      Assert.assertTrue(dump, ChangeHelper.containsAdded(changelist1.getChanges(), filepathSubfolderDeleted));
      Assert.assertTrue(dump, ChangeHelper.containsAdded(changelist1.getChanges(), filepathSubfolderMovedOriginal));
    }

    {
      final TFSChangeList changelist2 = historyList.get(0);
      Assert.assertEquals(comment2, changelist2.getComment());
      Assert.assertEquals(getUsername(), changelist2.getCommitterName());

      String dump = ChangeHelper.toString(changelist2.getChanges(), mySandboxRoot);
      Assert.assertTrue(dump, ChangeHelper.containsAdded(changelist2.getChanges(), fileCreated));
      Assert.assertTrue(dump, ChangeHelper.containsDeleted(changelist2.getChanges(), filepathDeleted));
      Assert.assertTrue(dump, ChangeHelper.containsModified(changelist2.getChanges(), fileModified));
      ChangeHelper
        .assertModified(changelist2.getChanges(), TfsFileUtil.getFilePath(fileModified), fileModifiedContent1, fileModifiedContent2);

      Assert.assertTrue(dump, ChangeHelper.containsAdded(changelist2.getChanges(), subfolderA));
      Assert.assertTrue(dump, ChangeHelper.containsAdded(changelist2.getChanges(), subfolderB));
      Assert.assertTrue(dump, ChangeHelper.containsDeleted(changelist2.getChanges(), filepathSubfolderDeleted));

      if (TFSChangeList.IDEADEV_29451_WORKAROUND) {
        Assert.assertEquals(dump, 14, changelist2.getChanges().size());
        Assert.assertTrue(dump, ChangeHelper.containsDeleted(changelist2.getChanges(), filepathRenamed_original, fileRenamedContent));
        Assert.assertTrue(dump, ChangeHelper.containsAdded(changelist2.getChanges(), fileRenamed, fileRenamedContent));

        Assert.assertTrue(dump, ChangeHelper.containsDeleted(changelist2.getChanges(), filepathRenamedModified_original, fileRenamedModifiedContent1));
        Assert.assertTrue(dump, ChangeHelper.containsAdded(changelist2.getChanges(), fileRenamedModified, fileRenamedModifiedContent2));

        Assert.assertTrue(dump, ChangeHelper.containsDeleted(changelist2.getChanges(), filepathSubfolderRenamedOriginal));
        Assert.assertTrue(dump, ChangeHelper.containsAdded(changelist2.getChanges(), subfolderRenamed));

        Assert.assertTrue(dump, ChangeHelper.containsDeleted(changelist2.getChanges(), filepathSubfolderMovedOriginal));
        Assert.assertTrue(dump, ChangeHelper.containsAdded(changelist2.getChanges(), subfolderMoved));
      }
      else {
        Assert.assertEquals(dump, 10, changelist2.getChanges().size());

        Change renamedChange = ChangeHelper.getMoveChange(changelist2.getChanges(), filepathRenamed_original, fileRenamed);
        Assert.assertNotNull(renamedChange);
        ChangeHelper.assertContent(renamedChange, fileRenamedContent, fileRenamedContent);

        Change renamedModifiedChange = ChangeHelper
          .getMoveChange(changelist2.getChanges(), filepathRenamedModified_original, fileRenamedModified);
        Assert.assertNotNull(dump, renamedModifiedChange);
        ChangeHelper.assertContent(renamedModifiedChange, fileRenamedModifiedContent1, fileRenamedModifiedContent2);

        Assert
          .assertNotNull(dump, ChangeHelper.getMoveChange(changelist2.getChanges(), filepathSubfolderRenamedOriginal, subfolderRenamed));

        Assert.assertNotNull(dump, ChangeHelper.getMoveChange(changelist2.getChanges(), filepathSubfolderMovedOriginal, subfolderMoved));
      }
    }

    final VcsHistorySession session = getVcs().getVcsHistoryProvider().createSessionFor(TfsFileUtil.getFilePath(fileRenamedModified));
    Assert.assertEquals(2, session.getRevisionList().size());
    final VcsFileRevision firstRevision = session.getRevisionList().get(1);
    final VcsFileRevision lastRevision = session.getRevisionList().get(0);
    Assert.assertEquals(comment1, firstRevision.getCommitMessage());
    Assert.assertEquals(comment2, lastRevision.getCommitMessage());
    firstRevision.loadContent();
    lastRevision.loadContent();
    Assert.assertArrayEquals(fileRenamedModifiedContent1.getBytes(), firstRevision.getContent());
    Assert.assertArrayEquals(fileRenamedModifiedContent2.getBytes(), lastRevision.getContent());
  }

}
