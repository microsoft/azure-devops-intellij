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

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.TfsFileUtil;
import org.junit.Assert;

import java.util.Collection;

@SuppressWarnings({"ConstantConditions", "HardCodedStringLiteral"})
public class ChangeHelper {

  public static boolean containsAdded(Collection<Change> changes, VirtualFile file) {
    return containsAdded(changes, TfsFileUtil.getFilePath(file));
  }


  @Nullable
  public static Change getAddChange(Collection<Change> changes, FilePath file) {
    for (Change c : changes) {
      if (c.getBeforeRevision() == null && c.getAfterRevision() != null) {
        if (c.getAfterRevision().getFile().equals(file)) {
          return c;
        }
      }
    }
    return null;
  }

  public static boolean containsAdded(Collection<Change> changes, FilePath file) {
    return getAddChange(changes, file) != null;
  }

  @Nullable
  public static Change getDeleteChange(Collection<Change> changes, FilePath file) {
    for (Change c : changes) {
      if (c.getBeforeRevision() != null && c.getAfterRevision() == null) {
        if (c.getBeforeRevision().getFile().equals(file)) {
          return c;
        }
      }
    }
    return null;
  }

  public static boolean containsDeleted(Collection<Change> changes, FilePath file) {
    return getDeleteChange(changes, file) != null;
  }

  @Nullable
  public static Change getMoveChange(Collection<Change> changes, final FilePath from, final VirtualFile to) {
    return getMoveChange(changes, from, TfsFileUtil.getFilePath(to));
  }

  @Nullable
  public static Change getMoveChange(Collection<Change> changes, final FilePath from, final FilePath to) {
    for (Change c : changes) {
      if (c.getBeforeRevision() != null && c.getAfterRevision() != null) {
        if (c.getBeforeRevision().getFile().equals(from) && c.getAfterRevision().getFile().equals(to)) {
          return c;
        }
      }
    }
    return null;
  }

  public static boolean containsModified(Collection<Change> changes, VirtualFile file) {
    return getModificationChange(changes, TfsFileUtil.getFilePath(file)) != null;
  }


  public static boolean containsModified(Collection<Change> changes, FilePath file) {
    return getModificationChange(changes, file) != null;
  }

  @Nullable
  public static Change getModificationChange(Collection<Change> changes, FilePath file) {
    for (Change c : changes) {
      if (c.getBeforeRevision() != null && c.getAfterRevision() != null) {
        if (c.getBeforeRevision().getFile().equals(file) &&
            c.getBeforeRevision().getFile().equals(file) &&
            c.getType() == Change.Type.MODIFICATION) {
          return c;
        }
      }
    }
    return null;
  }

  public static void assertModified(Collection<Change> changes, FilePath file, String contentBefore, String contentAfter)
    throws VcsException {
    Change change = getModificationChange(changes, file);
    Assert.assertNotNull(file.getIOFile().getPath() + " expected to be modified", change);
    assertContent(change, contentBefore, contentAfter);
  }

  public static void assertContent(Change change, String contentBefore, String contentAfter) throws VcsException {
    Assert.assertEquals(contentBefore, change.getBeforeRevision().getContent());
    Assert.assertEquals(contentAfter, change.getAfterRevision().getContent());
  }

  public static String toString(final Collection<Change> changes, final VirtualFile rootPath) {
    String pathPrefix = rootPath.getPath();
    StringBuilder s = new StringBuilder();
    s.append("Changes:\n");
    for (Change change : changes) {
      s.append("\t");
      if (change.getType() == Change.Type.NEW) {
        s.append("Add: ").append(getPathRemainder(change.getAfterRevision().getFile(), rootPath));
      }
      else if (change.getType() == Change.Type.MODIFICATION) {
        s.append("Modified: ").append(getPathRemainder(change.getAfterRevision().getFile(), rootPath));
      }
      else if (change.getType() == Change.Type.MOVED) {
        s.append("Rename/move: ").append(getPathRemainder(change.getBeforeRevision().getFile(), rootPath)).append(" -> ")
          .append(getPathRemainder(change.getAfterRevision().getFile(), rootPath));
      }
      else {
        s.append("Remove: ").append(getPathRemainder(change.getBeforeRevision().getFile(), rootPath));
      }
      s.append("\n");
    }
    return s.toString();
  }

  public static String getPathRemainder(VirtualFile file, VirtualFile rootPath) {
    String pathPrefix = rootPath.getPresentableUrl();
    String path = file.getPresentableUrl();

    if (pathPrefix == null || pathPrefix.length() == 0 || !path.startsWith(pathPrefix)) {
      return path.length() > 0 ? path : file.getPath();
    }
    else {
      if (path.length() == pathPrefix.length()) {
        return pathPrefix;
      }
      else {
        return path.length() > 0 ? path.substring(pathPrefix.length() + 1) : file.getPath();
      }
    }
  }

  public static String getPathRemainder(FilePath filePath, VirtualFile rootPath) {
    String path = FileUtil.toSystemIndependentName(filePath.getPath());
    String pathPrefix = FileUtil.toSystemIndependentName(rootPath.getPath());
    return path.startsWith(pathPrefix) ? path.substring(pathPrefix.length() + 1) : path;
  }


  public static void assertContains(final Collection<Change> subset, final Collection<Change> superset) throws VcsException {
    for (Change c : subset) {
      if (c.getType() == Change.Type.NEW) {
        Assert.assertTrue(containsAdded(superset, c.getAfterRevision().getFile()));
      }
      else if (c.getType() == Change.Type.DELETED) {
        Assert.assertTrue(containsDeleted(superset, c.getBeforeRevision().getFile()));
      }
      else if (c.getType() == Change.Type.MODIFICATION) {
        // TODO can't check content here since change can contain obsolete CurrentContentRevision
        Assert.assertNotNull(getModificationChange(superset, c.getBeforeRevision().getFile()));
      }
      else {
        final Change moveChange = getMoveChange(superset, c.getBeforeRevision().getFile(), c.getAfterRevision().getFile());
        Assert.assertEquals(c.getBeforeRevision().getFile().isDirectory(), moveChange.getBeforeRevision().getFile().isDirectory());

        if (!c.getBeforeRevision().getFile().isDirectory()) {
          // TODO can't check content here since change can contain obsolete CurrentContentRevision
          //Assert.assertEquals(c.getBeforeRevision().getContent(), moveChange.getBeforeRevision().getContent());
          //Assert.assertEquals(c.getAfterRevision().getContent(), moveChange.getAfterRevision().getContent());
        }
      }
    }
  }

}
