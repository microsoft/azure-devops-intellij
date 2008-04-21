package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import org.jetbrains.tfsIntegration.core.revision.TFSContentRevision;

import java.io.IOException;
import java.util.Date;

public class TFSFileRevision implements VcsFileRevision {
  private Date myDate;
  private byte[] myContent;
  private String myCommitMessage;
  private String myAuthor;
  private FilePath myFilePath;
  private int myChangeset;

  public TFSFileRevision(final FilePath filePath, final Date date, final String commitMessage, final String author, final int changeset) {
    myDate = date;
    myCommitMessage = commitMessage;
    myAuthor = author;
    myChangeset = changeset;
    myFilePath = filePath;
  }

  public VcsRevisionNumber getRevisionNumber() {
    return new VcsRevisionNumber.Int(myChangeset);
  }

  public String getBranchName() {
    // TODO
    return null;
  }

  public Date getRevisionDate() {
    return myDate;
  }

  public String getAuthor() {
    return myAuthor;
  }

  public String getCommitMessage() {
    return myCommitMessage;
  }

  public void loadContent() throws VcsException {
    // TODO: encoding
    final String content = new TFSContentRevision(myFilePath, myChangeset).getContent();
    myContent = (content != null) ? content.getBytes() : null;
  }

  public byte[] getContent() throws IOException {
    return myContent;
  }


}
