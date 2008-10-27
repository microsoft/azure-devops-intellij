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

package org.jetbrains.tfsIntegration.core.tfs;

import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.util.diff.Diff;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AnnotationBuilder {

  public interface ContentProvider {
    String getContent(VcsFileRevision revision) throws VcsException;
  }

  private final String myAnnotatedContent;
  private final VcsFileRevision[] myLineRevisions;

  // index of the stored record is the line number in the old revision of the file (which changes while we analyse different revisions),
  // stored Integer is the corresponding line number in the revision which is being annotated. It is in range [0..myLineRevisions.sise() - 1]
  // and may be null if this line in old file does not appear in annotated file.
  private final List<Integer> myLineNumbers;

  /**
   * @param revisions       sorted list containing revisions of the annotated file.
   *                        First element of the list (with zero index) must contatin revision which is being annotated,
   *                        other list elements (if any) must give all file revisions which are older than the annotated one.
   * @param contentProvider delegate providing file content. {@link ContentProvider#getContent(VcsFileRevision)} method of provided object
   *                        is called only for specified <code>revisions</code>
   */
  public AnnotationBuilder(List<VcsFileRevision> revisions, ContentProvider contentProvider) throws VcsException {
    if (revisions == null || revisions.size() < 1) {
      throw new IllegalArgumentException();
    }

    final Iterator<VcsFileRevision> iterator = revisions.iterator();
    VcsFileRevision revision = iterator.next();
    myAnnotatedContent = contentProvider.getContent(revision);
    if (myAnnotatedContent == null) {
      throw new VcsException(MessageFormat.format("Null content for revision {0}", revision.getRevisionNumber().toString()));
    }
    String[] lines = splitLines(myAnnotatedContent);

    myLineRevisions = new VcsFileRevision[lines.length];
    myLineNumbers = new ArrayList<Integer>(lines.length);
    for (int i = 0; i < lines.length; i++) {
      myLineNumbers.add(i);
    }

    while (iterator.hasNext()) {
      final VcsFileRevision previousRevision = iterator.next();
      final String previousContent = contentProvider.getContent(previousRevision);
      if (previousContent == null) {
        throw new VcsException(MessageFormat.format("Null content for revision {0}", previousRevision.getRevisionNumber().toString()));
      }
      final String[] previousLines = splitLines(previousContent);
      final Diff.Change change = Diff.buildChanges(previousLines, lines);

      analyseAllChanges(change, revision);
      if (allLinesAnnotated()) {
        break;
      }
      lines = previousLines;
      revision = previousRevision;
    }

    fillAllNotAnnotated(revisions.get(revisions.size() - 1));
  }

  private void analyseAllChanges(final Diff.Change change, final VcsFileRevision revision) {
    Diff.Change ch = change;
    while (ch != null) {
      findLinesInsertedByChange(ch, revision);
      ch = ch.link;
    }

    recalculateLineNumbers(change);
  }

  private void findLinesInsertedByChange(final Diff.Change change, VcsFileRevision revision) {
    if (change.inserted > 0) {
      for (int line = change.line1; line < change.line1 + change.inserted; line++) {
        Integer origLine = myLineNumbers.get(line);
        if (origLine != null) {
          if (myLineRevisions[origLine.intValue()] == null) {
            myLineRevisions[origLine.intValue()] = revision;
          }
        }
      }
    }
  }

  private void recalculateLineNumbers(final Diff.Change change) {
    Diff.Change ch = change;
    int removedLinesCount = 0;
    while (ch != null) {
      for (int i = 0; i < ch.inserted; i++) {
        myLineNumbers.remove(ch.line1 - removedLinesCount);
      }
      removedLinesCount += ch.inserted;
      ch = ch.link;
    }

    ch = change;
    while (ch != null) {
      for (int i = 0; i < ch.deleted; i++) {
        myLineNumbers.add(ch.line0, null);
      }
      ch = ch.link;
    }
  }

  private boolean allLinesAnnotated() {
    for (VcsFileRevision revision : myLineRevisions) {
      if (revision == null) {
        return false;
      }
    }
    return true;
  }

  private void fillAllNotAnnotated(final VcsFileRevision vcsFileRevision) {
    for (int i = 0; i < myLineRevisions.length; i++) {
      if (myLineRevisions[i] == null) {
        myLineRevisions[i] = vcsFileRevision;
      }
    }
  }

  private static String[] splitLines(String string) {
    string = StreamUtil.convertSeparators(string);

    boolean spaceAdded = false;
    if (string.endsWith("\n")) {
      string += " ";
      spaceAdded = true;
    }

    final String[] temp = string.split("\n");

    if (spaceAdded) {
      final String[] result = new String[temp.length - 1];
      System.arraycopy(temp, 0, result, 0, result.length);
      return result;
    } else {
      return temp;
    }
  }

  public String getAnnotatedContent() {
    return myAnnotatedContent;
  }

  /**
   * @return array containing {@link VcsFileRevision} objects for annotated file.
   *         Array index means the line number (array size is equal to lines amount),
   *         stored {@link VcsFileRevision} means revision since which this line appears in the file.
   */
  public VcsFileRevision[] getLineRevisions() {
    return myLineRevisions;
  }
}
