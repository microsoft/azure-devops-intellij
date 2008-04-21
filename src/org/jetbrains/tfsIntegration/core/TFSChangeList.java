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
import org.jetbrains.tfsIntegration.core.tfs.ItemPath;
import org.jetbrains.tfsIntegration.core.tfs.version.ChangesetVersionSpec;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Changeset;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ItemType;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.RecursionType;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;

public class TFSChangeList implements CommittedChangeList {
  private int myRevision;
  private String myAuthor;
  private Date myDate;
  private @NotNull String myComment;
  private final TFSVcs myVcs;
  private List<Change> myChanges;
  private Set<FilePath> myChangedPaths = new HashSet<FilePath>();
  private Set<FilePath> myAddedPaths = new HashSet<FilePath>();
  private Set<FilePath> myDeletedPaths = new HashSet<FilePath>();

  public TFSChangeList(final TFSVcs vcs, final DataInput stream) {
    myVcs = vcs;
    readFromStream(stream);
  }

  public TFSChangeList(final TFSRepositoryLocation location,
                       final int revision,
                       final String author,
                       final Date date,
                       final String comment,
                       final TFSVcs vcs) {
    myRevision = revision;
    myAuthor = author;
    myDate = date;
    myComment = comment != null ? comment : "";
    myVcs = vcs;
    loadChanges(location);
  }

  private void loadChanges(final TFSRepositoryLocation repositoryLocation) {
    try {
      Changeset changeset = repositoryLocation.getWorkspace().getServer().getVCS().queryChangeset(myRevision);
      org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Change[] changes = changeset.getChanges().getChange();

      for (org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Change change : changes) {
        ChangeType changeType = ChangeType.fromString(change.getType());

        final FilePath localPath = repositoryLocation.getWorkspace().findLocalPathByServerPath(change.getItem().getItem());
        if (localPath != null) {
          if (changeType.contains(ChangeType.Value.Add)) {
            TFSVcs.assertTrue(changeType.contains(ChangeType.Value.Encoding));
            if (change.getItem().getType() == ItemType.File) {
              TFSVcs.assertTrue(changeType.contains(ChangeType.Value.Edit));
            }
            else {
              TFSVcs.assertTrue(!changeType.contains(ChangeType.Value.Edit));
            }
            TFSVcs.assertTrue(!changeType.contains(ChangeType.Value.Delete));
            TFSVcs.assertTrue(!changeType.contains(ChangeType.Value.Rename));
            myAddedPaths.add(localPath);
            continue;
          }

          if (changeType.contains(ChangeType.Value.Delete)) {
            TFSVcs.assertTrue(changeType.containsOnly(ChangeType.Value.Delete));
            myDeletedPaths.add(VcsUtil.getFilePathForDeletedFile(localPath.getPath(), change.getItem().getType() == ItemType.Folder));
            continue;
          }

          if (changeType.contains(ChangeType.Value.Rename)) {
            final ItemPath itemPath = new ItemPath(localPath, change.getItem().getItem());
            List<Changeset> fileHistory = repositoryLocation.getWorkspace().getServer().getVCS().queryHistory(
              repositoryLocation.getWorkspace().getName(), repositoryLocation.getWorkspace().getOwnerName(), itemPath, Integer.MIN_VALUE,
              null, new ChangesetVersionSpec(changeset.getCset()), new ChangesetVersionSpec(1),
              new ChangesetVersionSpec(change.getItem().getCs()), 2, RecursionType.None);
            TFSVcs.assertTrue(fileHistory.size() == 2);

            org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Change[] ch = fileHistory.get(1).getChanges().getChange();
            FilePath localPathBeforeRename = repositoryLocation.getWorkspace().findLocalPathByServerPath(ch[0].getItem().getItem());
            TFSVcs.assertTrue(localPathBeforeRename != null);
            boolean isDirectory = ch[0].getItem().getType() == ItemType.Folder;
            FilePath toAdd = VcsUtil.getFilePath(localPath.getPath(), isDirectory);
            //noinspection ConstantConditions
            FilePath toDelete = VcsUtil.getFilePathForDeletedFile(localPathBeforeRename.getPath(), isDirectory);
            myAddedPaths.add(toAdd);
            myDeletedPaths.add(toDelete);
            continue;
          }

          if (changeType.contains(ChangeType.Value.Edit)) {
            //TFSVcs.assertTrue(changeType.contains(ChangeType.Value.Encoding));
            myChangedPaths.add(localPath);
            continue;
          }

          TFSVcs.assertTrue(false);
        }
      }
    }
    catch (TfsException e) {
      AbstractVcsHelper.getInstance(myVcs.getProject()).showError(new VcsException(e.getMessage(), e), TFSVcs.TFS_NAME);
    }
  }

  public String getCommitterName() {
    return myAuthor;
  }

  public Date getCommitDate() {
    return myDate;
  }

  public long getNumber() {
    return myRevision;
  }

  public AbstractVcs getVcs() {
    return myVcs;
  }

  public Collection<Change> getChanges() {
    if (myChanges == null) {
      buildChanges();
    }
    return myChanges;

  }

  @NotNull
  public String getName() {
    return myComment;
  }

  @NotNull
  public String getComment() {
    return myComment;
  }

  private void buildChanges() {
    myChanges = new ArrayList<Change>();
    for (FilePath path : myAddedPaths) {
      myChanges.add(new Change(null, new TFSContentRevision(path, myRevision)));
    }
    for (FilePath path : myDeletedPaths) {
      myChanges.add(new Change(new TFSContentRevision(path, myRevision - 1), null));
    }
    for (FilePath path : myChangedPaths) {
      TFSContentRevision beforeRevision = new TFSContentRevision(path, myRevision - 1);
      TFSContentRevision afterRevision = new TFSContentRevision(path, myRevision);
      myChanges.add(new Change(beforeRevision, afterRevision));
    }


  }

  public void writeToStream(final DataOutput stream) throws IOException {
    stream.writeInt(myRevision);
    stream.writeUTF(myAuthor);
    stream.writeLong(myDate.getTime());
    stream.writeUTF(myComment);
    writePaths(stream, myChangedPaths);
    writePaths(stream, myAddedPaths);
    writePaths(stream, myDeletedPaths);

  }

  private void readFromStream(final DataInput stream) {
    try {
      myRevision = stream.readInt();
      myAuthor = stream.readUTF();
      myDate = new Date(stream.readLong());
      myComment = stream.readUTF();
      readPaths(stream, myChangedPaths);
      readPaths(stream, myAddedPaths);
      readPaths(stream, myDeletedPaths);
    }
    catch (Exception e) {
      AbstractVcsHelper.getInstance(myVcs.getProject()).showError(new VcsException(e.getMessage(), e), TFSVcs.TFS_NAME);
    }
  }

  private static void writePaths(final DataOutput stream, final Set<FilePath> paths) throws IOException {
    stream.writeInt(paths.size());
    for (FilePath s : paths) {
      stream.writeUTF(s.getPath());
    }
  }

  private static void readPaths(final DataInput stream, final Set<FilePath> paths) throws IOException {
    int count = stream.readInt();
    for (int i = 0; i < count; i++) {
      paths.add(VcsUtil.getFilePath(stream.readUTF()));
    }
  }

  // equals() and hashCode() used by IDEA to maintain changed files tree state

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final TFSChangeList that = (TFSChangeList)o;

    if (myRevision != that.myRevision) return false;
    if (myAuthor != null ? !myAuthor.equals(that.myAuthor) : that.myAuthor != null) return false;
    if (myDate != null ? !myDate.equals(that.myDate) : that.myDate != null) return false;
    if (!myComment.equals(that.myComment)) return false;

    return true;
  }

  public int hashCode() {
    int result;
    result = myRevision ^ (myRevision >> 32);
    result = 31 * result + (myAuthor != null ? myAuthor.hashCode() : 0);
    result = 31 * result + (myDate != null ? myDate.hashCode() : 0);
    result = 31 * result + (myComment.hashCode());
    return result;
  }


}
