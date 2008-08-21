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

package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList;
import com.intellij.vcsUtil.VcsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.revision.TFSContentRevision;
import org.jetbrains.tfsIntegration.core.tfs.ChangeType;
import org.jetbrains.tfsIntegration.core.tfs.EnumMask;
import org.jetbrains.tfsIntegration.core.tfs.version.ChangesetVersionSpec;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Changeset;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Item;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ItemType;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.RecursionType;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;

public class TFSChangeList implements CommittedChangeList {
  private int myRevisionNumber;
  private String myAuthor;
  private Date myDate;
  private @NotNull String myComment;
  private final TFSVcs myVcs;
  private List<Change> myCachedChanges;
  private Set<FilePath> myModifiedPaths = new HashSet<FilePath>();
  private Set<FilePath> myAddedPaths = new HashSet<FilePath>();
  private Set<FilePath> myDeletedPaths = new HashSet<FilePath>();
  private Map<FilePath, FilePath> myMovedPaths = new HashMap<FilePath, FilePath>();

  public TFSChangeList(final TFSVcs vcs, final DataInput stream) {
    myVcs = vcs;
    readFromStream(stream);
  }

  public TFSChangeList(final TFSRepositoryLocation location,
                       final int revisionNumber,
                       final String author,
                       final Date date,
                       final String comment,
                       final TFSVcs vcs) {
    myRevisionNumber = revisionNumber;
    myAuthor = author;
    myDate = date;
    myComment = comment != null ? comment : "";
    myVcs = vcs;
    // TODO: load changes lazily?
    loadChanges(location);
  }

  private void loadChanges(final TFSRepositoryLocation repositoryLocation) {
    try {
      Changeset changeset = repositoryLocation.getWorkspace().getServer().getVCS().queryChangeset(myRevisionNumber);

      for (org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Change change : changeset.getChanges().getChange()) {
        processChange(repositoryLocation, changeset.getCset(), change);
      }
    }
    catch (TfsException e) {
      AbstractVcsHelper.getInstance(myVcs.getProject()).showError(new VcsException(e.getMessage(), e), TFSVcs.TFS_NAME);
    }
  }

  private void processChange(final TFSRepositoryLocation repositoryLocation,
                             int changeset,
                             final org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Change change) throws TfsException {
    final EnumMask<ChangeType> changeType = EnumMask.fromString(ChangeType.class, change.getType());

    final FilePath localPath = repositoryLocation.getWorkspace()
      .findLocalPathByServerPath(change.getItem().getItem(), change.getItem().getType() == ItemType.Folder);

    if (localPath == null) {
      // TODO: path can be out of current mapping -> no way to determine local path for it
      return;
    }

    if (changeType.contains(ChangeType.Add)) {
      TFSVcs.assertTrue(changeType.contains(ChangeType.Encoding));
      if (change.getItem().getType() == ItemType.File) {
        TFSVcs.assertTrue(changeType.contains(ChangeType.Edit));
      }
      else {
        TFSVcs.assertTrue(!changeType.contains(ChangeType.Edit));
      }
      TFSVcs.assertTrue(!changeType.contains(ChangeType.Delete));
      TFSVcs.assertTrue(!changeType.contains(ChangeType.Rename));
      myAddedPaths.add(localPath);
      return;
    }

    if (changeType.contains(ChangeType.Delete)) {
      TFSVcs.assertTrue(changeType.size() <= 2, "Unexpected change type: " + changeType);
      TFSVcs.assertTrue(changeType.containsOnly(ChangeType.Delete) || changeType.contains(ChangeType.Rename),
                        "Unexpected change type: " + changeType);
      myDeletedPaths.add(localPath);
      return;
    }

    if (changeType.contains(ChangeType.Rename)) {
      List<Changeset> fileHistory = repositoryLocation.getWorkspace().getServer().getVCS().queryHistory(
        repositoryLocation.getWorkspace().getName(), repositoryLocation.getWorkspace().getOwnerName(), change.getItem().getItem(),
        change.getItem().getDid(), null, new ChangesetVersionSpec(changeset), new ChangesetVersionSpec(1),
        new ChangesetVersionSpec(change.getItem().getCs()), 2, RecursionType.None);
      TFSVcs.assertTrue(fileHistory.size() == 2);

      final Item item = fileHistory.get(1).getChanges().getChange()[0].getItem(); // use penultimate item
      FilePath originalPath = repositoryLocation.getWorkspace()
        .findLocalPathByServerPath(item.getItem(), item.getType() == ItemType.Folder);

      if (originalPath != null) {
        myMovedPaths.put(originalPath, localPath);
      }
      else {
        // TODO: original path can be out of current mapping -> no way to determine local path for it
      }
      return;
    }

    if (changeType.contains(ChangeType.Edit)) {
      //TFSVcs.assertTrue(changeType.contains(ChangeType.Value.Encoding));
      myModifiedPaths.add(localPath);
      return;
    }

    TFSVcs.error("Unknown change: " + changeType + " for item " + change.getItem().getItem());
  }

  public String getCommitterName() {
    return myAuthor;
  }

  public Date getCommitDate() {
    return myDate;
  }

  public long getNumber() {
    return myRevisionNumber;
  }

  public AbstractVcs getVcs() {
    return myVcs;
  }

  public Collection<Change> getChanges() {
    if (myCachedChanges == null) {
      try {
        myCachedChanges = new ArrayList<Change>();
        for (FilePath path : myAddedPaths) {
          myCachedChanges.add(new Change(null, new TFSContentRevision(path, myRevisionNumber)));
        }
        for (FilePath path : myDeletedPaths) {
          myCachedChanges.add(new Change(new TFSContentRevision(path, myRevisionNumber - 1), null));
        }
        for (FilePath path : myModifiedPaths) {
          TFSContentRevision beforeRevision = new TFSContentRevision(path, myRevisionNumber - 1);
          TFSContentRevision afterRevision = new TFSContentRevision(path, myRevisionNumber);
          myCachedChanges.add(new Change(beforeRevision, afterRevision));
        }
        for (Map.Entry<FilePath, FilePath> e : myMovedPaths.entrySet()) {
          TFSContentRevision beforeRevision = new TFSContentRevision(e.getKey(), myRevisionNumber - 1);
          TFSContentRevision afterRevision = new TFSContentRevision(e.getValue(), myRevisionNumber);
          myCachedChanges.add(new Change(beforeRevision, afterRevision));
        }
      }
      catch (TfsException e) {
        AbstractVcsHelper.getInstance(myVcs.getProject()).showError(new VcsException(e.getMessage(), e), TFSVcs.TFS_NAME);
      }
    }
    return myCachedChanges;
  }

  @NotNull
  public String getName() {
    return myComment;
  }

  @NotNull
  public String getComment() {
    return myComment;
  }

  public void writeToStream(final DataOutput stream) throws IOException {
    stream.writeInt(myRevisionNumber);
    stream.writeUTF(myAuthor);
    stream.writeLong(myDate.getTime());
    stream.writeUTF(myComment);
    writePaths(stream, myModifiedPaths);
    writePaths(stream, myAddedPaths);
    writePaths(stream, myDeletedPaths);
    writePaths(stream, myMovedPaths);
  }

  private void readFromStream(final DataInput stream) {
    try {
      myRevisionNumber = stream.readInt();
      myAuthor = stream.readUTF();
      myDate = new Date(stream.readLong());
      myComment = stream.readUTF();
      readPaths(stream, myModifiedPaths);
      readPaths(stream, myAddedPaths);
      readPaths(stream, myDeletedPaths);
      readPaths(stream, myMovedPaths);
    }
    catch (Exception e) {
      //noinspection ThrowableInstanceNeverThrown
      AbstractVcsHelper.getInstance(myVcs.getProject()).showError(new VcsException(e.getMessage(), e), TFSVcs.TFS_NAME);
    }
  }

  private static void writePaths(final DataOutput stream, final Collection<FilePath> paths) throws IOException {
    stream.writeInt(paths.size());
    for (FilePath path : paths) {
      writePath(stream, path);
    }
  }

  private static void writePaths(final DataOutput stream, final Map<FilePath, FilePath> paths) throws IOException {
    stream.writeInt(paths.size());
    for (Map.Entry<FilePath, FilePath> e : paths.entrySet()) {
      writePath(stream, e.getKey());
      writePath(stream, e.getValue());
    }
  }

  private static void writePath(final DataOutput stream, final FilePath path) throws IOException {
    stream.writeUTF(path.getPath());
    stream.writeBoolean(path.isDirectory());
  }

  private static void readPaths(final DataInput stream, final Collection<FilePath> paths) throws IOException {
    int count = stream.readInt();
    for (int i = 0; i < count; i++) {
      paths.add(readPath(stream));
    }
  }

  private static void readPaths(final DataInput stream, final Map<FilePath, FilePath> paths) throws IOException {
    int count = stream.readInt();
    for (int i = 0; i < count; i++) {
      paths.put(readPath(stream), readPath(stream));
    }
  }

  private static FilePath readPath(final DataInput stream) throws IOException {
    return VcsUtil.getFilePath(stream.readUTF(), stream.readBoolean());
  }

  // NOTE: equals() and hashCode() used by IDEA to maintain changed files tree state
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final TFSChangeList that = (TFSChangeList)o;

    return myRevisionNumber == that.myRevisionNumber &&
           !(myAuthor != null ? !myAuthor.equals(that.myAuthor) : that.myAuthor != null) &&
           !(myDate != null ? !myDate.equals(that.myDate) : that.myDate != null) &&
           myComment.equals(that.myComment);
  }

  public int hashCode() {
    int result;
    result = myRevisionNumber;
    result = 31 * result + (myAuthor != null ? myAuthor.hashCode() : 0);
    result = 31 * result + (myDate != null ? myDate.hashCode() : 0);
    return result;
  }

}
