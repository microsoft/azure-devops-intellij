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
import com.intellij.openapi.vcs.VcsConfiguration;
import com.intellij.openapi.vcs.VcsException;

import java.io.IOException;

@SuppressWarnings({"HardCodedStringLiteral"})
public class TestComplexResolveConflicts extends TFSTestCase {
  private static final String CONTENT_1 = "Content 1";
  private static final String CONTENT_2 = "Content 2";
  private static final String CONTENT_3 = "Content 3";
  private static final String CONTENT_4 = "Content 4";

  private static final String FOLDER_NAME_1 = "Folder1";
  private static final String FOLDER_NAME_2 = "Folder2";
  private static final String FOLDER_NAME_3 = "Folder3";
  private static final String FOLDER_NAME_4 = "Folder4";

  private static final String FILE_NAME_1 = "file1.txt";
  private static final String FILE_NAME_2 = "file2.txt";
  private static final String FILE_NAME_3 = "file3.txt";
  private static final String FILE_NAME_4 = "file4.txt";

  /**
   * prepare revisions:
   * 1. Create Folder1 with File1.txt in it (rev 1)
   * 2. Create File1.txt in Folder1 (rev 2)
   * 3. Folder 1 -> Folder 2 (rev 3)
   * 4. File1.txt -> File2.txt (rev 4)
   * 5. File2.txt (Content 1) -> File2.txt (Content 2) (rev 5)
   */

  private void prepareRevisions() throws VcsException, IOException {
    doActionSilently(VcsConfiguration.StandardConfirmation.ADD);
    doActionSilently(VcsConfiguration.StandardConfirmation.REMOVE);

    FilePath folder = getChildPath(mySandboxRoot, FOLDER_NAME_1);
    createDirInCommand(folder);
    commit(getChanges().getChanges(), "rev. 1");
    assertFolder(folder, 0);

    FilePath file = getChildPath(folder, FILE_NAME_1);
    createFileInCommand(file, CONTENT_1);

    commit(getChanges().getChanges(), "rev. 2");
    assertFolder(folder, 1);
    assertFile(file, CONTENT_1, false);

    rename(folder, FOLDER_NAME_2);
    folder = getChildPath(mySandboxRoot, FOLDER_NAME_2);
    commit(getChanges().getChanges(), "rev. 3");
    assertFolder(folder, 1);

    file = getChildPath(folder, FILE_NAME_1);
    rename(file, FILE_NAME_2);
    commit(getChanges().getChanges(), "rev. 4");
    file = getChildPath(folder, FILE_NAME_2);
    assertFile(file, CONTENT_1, false);

    editFiles(file);
    setFileContent(file, CONTENT_2);
    commit(getChanges().getChanges(), "rev. 5");
    assertFile(file, CONTENT_2, false);
  }

  /**
   * Folder 2 -> Folder 3
   */
  private void renameFolder() throws VcsException, IOException {
    FilePath folder = getChildPath(mySandboxRoot, FOLDER_NAME_2);
    rename(folder, FOLDER_NAME_3);
    folder = getChildPath(mySandboxRoot, FOLDER_NAME_3);
    assertFolder(folder, 1);
  }

  /**
   * File2.txt -> File3.txt
   */
  private void renameFile() throws VcsException, IOException {
    FilePath folder = getChildPath(mySandboxRoot, FOLDER_NAME_1);
    FilePath file = getChildPath(folder, FILE_NAME_3);
    rename(file, FILE_NAME_3);
    file = getChildPath(folder, FILE_NAME_3);
    assertFile(file, CONTENT_2, false);
  }

  /**
   * file (Content 2) -> file (Content 3)
   */
  private void editFile(FilePath file) throws VcsException, IOException {
    editFiles(file);
    setFileContent(file, CONTENT_3);
    assertFile(file, CONTENT_3, true);
  }

  //@Test
  //public void testRenameFolderAcceptYours() throws VcsException, IOException {
  //  prepareRevisions();
  //  renameFolder();
  //
  //  //ConflictsEnvironment.setResolveConflictsHandler(new AcceptYoursConflictsHandler(1));
  //  updateTo(4);
  //  FilePath folderFrom = getChildPath(mySandboxRoot, FOLDER_NAME_2);
  //  FilePath folderTo = getChildPath(mySandboxRoot, FOLDER_NAME_3);
  //
  //  // TODO: write correct asserts
  //  //assertFolder(folderTo, 1);
  //  //getChanges().assertTotalItems(1);
  //  //getChanges().assertRenamedOrMoved(folderFrom, folderTo);
  //}

}
