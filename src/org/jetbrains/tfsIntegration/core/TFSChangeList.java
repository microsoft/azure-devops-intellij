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

import com.intellij.openapi.util.Pair;
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
import org.jetbrains.tfsIntegration.core.tfs.VersionControlServer;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;
import org.jetbrains.tfsIntegration.core.tfs.version.ChangesetVersionSpec;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.*;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

@SuppressWarnings({"AutoUnboxing"})
public class TFSChangeList implements CommittedChangeList {

  public static final boolean IDEADEV_29451_WORKAROUND = true; // report rename(a->b) as delete(a) + add(b)

  private WorkspaceInfo myWorkspace;
  private final TFSVcs myVcs;
  private int myRevisionNumber;
  private String myAuthor;
  private Date myDate;
  private @NotNull String myComment;
  private List<Change> myCachedChanges;
  private Map<FilePath, Integer/*previous revision*/> myModifiedPaths = new HashMap<FilePath, Integer>();
  private Set<FilePath> myAddedPaths = new HashSet<FilePath>();
  private Map<FilePath, Integer/*previous revision*/> myDeletedPaths = new HashMap<FilePath, Integer>();
  private Map<FilePath, Pair<FilePath, Integer/*previous revision*/>> myMovedPaths = new HashMap<FilePath, Pair<FilePath, Integer>>();
  private URI myServerUri;
  private String myWorkspaceName;

  public TFSChangeList(final TFSVcs vcs, final DataInput stream) {
    myVcs = vcs;
    readFromStream(stream);
  }

  public TFSChangeList(final WorkspaceInfo workspace,
                       final int revisionNumber,
                       final String author,
                       final Date date,
                       final String comment,
                       final TFSVcs vcs) {
    myWorkspace = workspace;
    myRevisionNumber = revisionNumber;
    myAuthor = author;
    myDate = date;
    myComment = comment != null ? comment : "";
    myVcs = vcs;

    myWorkspaceName = myWorkspace.getName();
    myServerUri = myWorkspace.getServer().getUri();
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
        if (myWorkspace != null) { // otherwise paths were read from stream
          loadChanges();
        }

        myCachedChanges = new ArrayList<Change>();
        for (FilePath path : myAddedPaths) {
          myCachedChanges.add(new Change(null, TFSContentRevision.create(myVcs.getProject(), path, myRevisionNumber)));
        }
        for (Map.Entry<FilePath, Integer> entry : myDeletedPaths.entrySet()) {
          myCachedChanges.add(new Change(TFSContentRevision.create(myVcs.getProject(), entry.getKey(), entry.getValue()), null));
        }
        for (Map.Entry<FilePath, Integer> entry : myModifiedPaths.entrySet()) {
          TFSContentRevision beforeRevision = TFSContentRevision.create(myVcs.getProject(), entry.getKey(), entry.getValue());
          TFSContentRevision afterRevision = TFSContentRevision.create(myVcs.getProject(), entry.getKey(), myRevisionNumber);
          myCachedChanges.add(new Change(beforeRevision, afterRevision));
        }
        for (Map.Entry<FilePath, Pair<FilePath, Integer>> entry : myMovedPaths.entrySet()) {
          TFSContentRevision beforeRevision = TFSContentRevision.create(myVcs.getProject(), entry.getKey(), entry.getValue().second);
          TFSContentRevision afterRevision = TFSContentRevision.create(myVcs.getProject(), entry.getValue().first, myRevisionNumber);

          if (IDEADEV_29451_WORKAROUND) {
            myCachedChanges.add(new Change(beforeRevision, null));
            myCachedChanges.add(new Change(null, afterRevision));
          }
          else {
            myCachedChanges.add(new Change(beforeRevision, afterRevision));
          }
        }
      }
      catch (TfsException e) {
        AbstractVcsHelper.getInstance(myVcs.getProject()).showError(new VcsException(e.getMessage(), e), TFSVcs.TFS_NAME);
      }
    }
    return myCachedChanges;
  }

  public Collection<Change> getChangesWithMovedTrees() {
    return getChanges();
  }

  @NotNull
  public String getName() {
    return myComment;
  }

  @NotNull
  public String getComment() {
    return myComment;
  }

  void writeToStream(final DataOutput stream) throws IOException {
    stream.writeUTF(myServerUri.toString());
    stream.writeUTF(myWorkspaceName);
    stream.writeInt(myRevisionNumber);
    stream.writeUTF(myAuthor);
    stream.writeLong(myDate.getTime());
    stream.writeUTF(myComment);
    writePathsInts(stream, myModifiedPaths);
    writePaths(stream, myAddedPaths);
    writePathsInts(stream, myDeletedPaths);
    writeMoved(stream, myMovedPaths);
  }

  private void loadChanges() {
    try {
      Changeset changeset = myWorkspace.getServer().getVCS().queryChangeset(myRevisionNumber);

      for (org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Change change : changeset.getChanges().getChange()) {
        processChange(changeset.getCset(), change);
      }
    }
    catch (TfsException e) {
      AbstractVcsHelper.getInstance(myVcs.getProject()).showError(new VcsException(e.getMessage(), e), TFSVcs.TFS_NAME);
    }
  }

  private void processChange(int changeset, final org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Change change)
    throws TfsException {
    final EnumMask<ChangeType> changeType = EnumMask.fromString(ChangeType.class, change.getType());

    final FilePath localPath =
      myWorkspace.findLocalPathByServerPath(change.getItem().getItem(), change.getItem().getType() == ItemType.Folder);

    if (localPath == null) {
      // TODO: path can be out of current mapping -> no way to determine local path for it
      return;
    }

    if (changeType.containsAny(ChangeType.Add, ChangeType.Undelete, ChangeType.Branch)) {
      if (changeType.contains(ChangeType.Add)) {
        TFSVcs.assertTrue(changeType.contains(ChangeType.Encoding));
        if (change.getItem().getType() == ItemType.File) {
          TFSVcs.assertTrue(changeType.contains(ChangeType.Edit));
        }
        else {
          TFSVcs.assertTrue(!changeType.contains(ChangeType.Edit));
        }
      }
      TFSVcs.assertTrue(!changeType.contains(ChangeType.Delete));
      myAddedPaths.add(localPath);
      return;
    }

    if (changeType.contains(ChangeType.Delete)) {
      TFSVcs.assertTrue(changeType.size() <= 3, "Unexpected change type: " + changeType); // can be merge
      //TFSVcs.assertTrue(changeType.containsOnly(ChangeType.Delete) || changeType.contains(ChangeType.Rename),
      //                  "Unexpected change type: " + changeType);

      Item item = getPreviousVersion(change.getItem(), changeset);
      myDeletedPaths.put(localPath, item.getCs());
      return;
    }

    if (changeType.contains(ChangeType.Rename)) {
      if (change.getItem().getDid() != Integer.MIN_VALUE) {
        // the item was deleted in some previous checkin
        return;
      }
      Item item = getPreviousVersion(change.getItem(), changeset);
      FilePath originalPath = myWorkspace
        .findLocalPathByServerPath(item.getItem(), item.getType() == ItemType.Folder);

      if (originalPath != null) {
        myMovedPaths.put(originalPath, Pair.create(localPath, item.getCs()));
      }
      else {
        // TODO: original path can be out of current mapping -> no way to determine local path for it
      }
      return;
    }

    if (changeType.containsAny(ChangeType.Edit, ChangeType.Merge)) {
      Item item = getPreviousVersion(change.getItem(), changeset);
      //TFSVcs.assertTrue(changeType.contains(ChangeType.Value.Encoding));
      myModifiedPaths.put(localPath, item.getCs());
      return;
    }

    TFSVcs.error("Unknown change: " + changeType + " for item " + change.getItem().getItem());
  }

  private void readFromStream(final DataInput stream) {
    try {
      myServerUri = new URI(stream.readUTF());
      myWorkspaceName = stream.readUTF();
      myRevisionNumber = stream.readInt();
      myAuthor = stream.readUTF();
      myDate = new Date(stream.readLong());
      myComment = stream.readUTF();
      readPathsInts(stream, myModifiedPaths);
      readPaths(stream, myAddedPaths);
      readPathsInts(stream, myDeletedPaths);
      readMoved(stream, myMovedPaths);
    }
    catch (IOException e) {
      //noinspection ThrowableInstanceNeverThrown
      AbstractVcsHelper.getInstance(myVcs.getProject()).showError(new VcsException(e), TFSVcs.TFS_NAME);
    }
    catch (URISyntaxException e) {
      //noinspection ThrowableInstanceNeverThrown
      AbstractVcsHelper.getInstance(myVcs.getProject()).showError(new VcsException(e), TFSVcs.TFS_NAME);
    }
  }

  private static void writePaths(final DataOutput stream, final Collection<FilePath> paths) throws IOException {
    stream.writeInt(paths.size());
    for (FilePath path : paths) {
      writePath(stream, path);
    }
  }

  private static void writePathsInts(final DataOutput stream, final Map<FilePath, Integer> paths) throws IOException {
    stream.writeInt(paths.size());
    for (Map.Entry<FilePath, Integer> e : paths.entrySet()) {
      writePath(stream, e.getKey());
      stream.writeInt(e.getValue());
    }
  }

  private static void writeMoved(final DataOutput stream, final Map<FilePath, Pair<FilePath, Integer>> paths) throws IOException {
    stream.writeInt(paths.size());
    for (Map.Entry<FilePath, Pair<FilePath, Integer>> e : paths.entrySet()) {
      writePath(stream, e.getKey());
      writePath(stream, e.getValue().first);
      stream.writeInt(e.getValue().second);
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

  private static void readPathsInts(final DataInput stream, final Map<FilePath, Integer> paths) throws IOException {
    int count = stream.readInt();
    for (int i = 0; i < count; i++) {
      paths.put(readPath(stream), stream.readInt());
    }
  }

  private static void readMoved(final DataInput stream, final Map<FilePath, Pair<FilePath, Integer>> paths) throws IOException {
    int count = stream.readInt();
    for (int i = 0; i < count; i++) {
      paths.put(readPath(stream), Pair.create(readPath(stream), stream.readInt()));
    }
  }

  private static FilePath readPath(final DataInput stream) throws IOException {
    return VcsUtil.getFilePath(stream.readUTF(), stream.readBoolean());
  }

  private Item getPreviousVersion(Item item, int changeset) throws TfsException {
    ItemSpec itemSpec = VersionControlServer.createItemSpec(item.getItem(), item.getDid(), RecursionType.None);
    List<Changeset> shortHistory = myWorkspace.getServer().getVCS().queryHistory(myWorkspace.getName(), myWorkspace.getOwnerName(),
                                                                                 itemSpec, null, new ChangesetVersionSpec(changeset),
                                                                                 new ChangesetVersionSpec(1),
                                                                                 new ChangesetVersionSpec(item.getCs()), 2);
    TFSVcs.assertTrue(shortHistory.size() == 2);
    return shortHistory.get(1).getChanges().getChange()[0].getItem(); // use penultimate item
  }

  // NOTE: toString() is used by IDEA for context menu 'Copy' action in Repository view
  public String toString() {
    return myComment;
  }

  // NOTE: equals() and hashCode() used by IDEA to maintain changed files tree state
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final TFSChangeList that = (TFSChangeList)o;

    if (myRevisionNumber != that.myRevisionNumber) return false;
    if (myAuthor != null ? !myAuthor.equals(that.myAuthor) : that.myAuthor != null) return false;
    if (myDate != null ? !myDate.equals(that.myDate) : that.myDate != null) return false;
    if (myServerUri != null ? !myServerUri.equals(that.myServerUri) : that.myServerUri != null) return false;
    if (myWorkspaceName != null ? !myWorkspaceName.equals(that.myWorkspaceName) : that.myWorkspaceName != null) return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = myRevisionNumber;
    result = 31 * result + (myAuthor != null ? myAuthor.hashCode() : 0);
    result = 31 * result + (myDate != null ? myDate.hashCode() : 0);
    result = 31 * result + (myServerUri != null ? myServerUri.hashCode() : 0);
    result = 31 * result + (myWorkspaceName != null ? myWorkspaceName.hashCode() : 0);
    return result;
  }


}
