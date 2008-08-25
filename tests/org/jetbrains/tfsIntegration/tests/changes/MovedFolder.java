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

package org.jetbrains.tfsIntegration.tests.changes;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import org.jetbrains.tfsIntegration.core.tfs.TfsFileUtil;
import org.jetbrains.tfsIntegration.tests.TFSTestCase;
import org.junit.Test;

public class MovedFolder extends TFSTestCase {

  // moved folder (up to date sandbox root\Folder_Original -> up to date sandbox root\subfolder\subfolder2\Folder_Moved)

  @Test
  public void testMovedFolderFromUpToDateToUpToDate() throws VcsException {
    final String foldernameOriginal = "Folder_Original";
    FilePath folderOriginal = TfsFileUtil.getFilePath(createDirInCommand(mySandboxRoot, foldernameOriginal));
    FilePath subfolder = TfsFileUtil.getFilePath(createDirInCommand(mySandboxRoot, "subfolder"));
    FilePath subfolder2 = TfsFileUtil.getFilePath(createDirInCommand(subfolder.getVirtualFile(), "subfolder2"));

    assertFolder(mySandboxRoot, 2);
    assertFolder(subfolder, 1);
    assertFolder(subfolder2, 0);
    assertFolder(folderOriginal, 0);

    commit(getChanges().getChanges(), "test");
    assertFolder(mySandboxRoot, 2);
    assertFolder(subfolder, 1);
    assertFolder(subfolder2, 0);
    assertFolder(folderOriginal, 0);

    final String foldernameMoved = "Folder_Moved";
    moveFileInCommand(folderOriginal, subfolder2.getVirtualFile());
    rename(getChildPath(subfolder2, foldernameOriginal), foldernameMoved);
    FilePath folderMoved = getChildPath(subfolder2, foldernameMoved);

    assertFolder(mySandboxRoot, 1);
    assertFolder(subfolder, 1);
    assertFolder(subfolder2, 1);
    assertFolder(folderMoved, 0);

    getChanges().assertTotalItems(1);
    getChanges().assertRenamedOrMoved(folderOriginal, folderMoved);

    rollback(getChanges().getChanges());
    getChanges().assertTotalItems(0);

    assertFolder(mySandboxRoot, 2);
    assertFolder(subfolder, 1);
    assertFolder(subfolder2, 0);
    assertFolder(folderOriginal, 0);

    moveFileInCommand(folderOriginal, subfolder2.getVirtualFile());
    rename(getChildPath(subfolder2, foldernameOriginal), foldernameMoved);

    assertFolder(mySandboxRoot, 1);
    assertFolder(subfolder, 1);
    assertFolder(subfolder2, 1);
    assertFolder(folderMoved, 0);

    commit(getChanges().getChanges(), "test");
    assertFolder(mySandboxRoot, 1);
    assertFolder(subfolder, 1);
    assertFolder(subfolder2, 1);
    assertFolder(folderMoved, 0);

    final int latestRevision = getLatestRevisionNumber(mySandboxRoot);

    update(mySandboxRoot, latestRevision - 1);
    getChanges().assertTotalItems(0);
    assertFolder(mySandboxRoot, 2);
    assertFolder(subfolder, 1);
    assertFolder(subfolder2, 0);
    assertFolder(folderOriginal, 0);

    update(mySandboxRoot, latestRevision);
    getChanges().assertTotalItems(0);
    assertFolder(mySandboxRoot, 1);
    assertFolder(subfolder, 1);
    assertFolder(subfolder2, 1);
    assertFolder(folderMoved, 0);
  }


}
