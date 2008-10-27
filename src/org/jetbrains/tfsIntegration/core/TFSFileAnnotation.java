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

import com.intellij.openapi.editor.EditorGutterAction;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.annotate.AnnotationListener;
import com.intellij.openapi.vcs.annotate.FileAnnotation;
import com.intellij.openapi.vcs.annotate.LineAnnotationAspect;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList;
import com.intellij.util.text.SyncDateFormat;
import org.jetbrains.tfsIntegration.core.tfs.TfsFileUtil;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;

import java.awt.*;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class TFSFileAnnotation implements FileAnnotation {

  private final TFSVcs myTFSVcs;
  private final WorkspaceInfo myWorkspace;
  private final String myAnnotatedContent;
  private final VcsFileRevision[] myLineRevisions;

  private final List<AnnotationListener> myListeners = new ArrayList<AnnotationListener>();

  private static final SyncDateFormat DATE_FORMAT = new SyncDateFormat(SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT));

  private final LineAnnotationAspect REVISION_ASPECT = new RevisionAnnotationAspect();

  private final LineAnnotationAspect DATE_ASPECT = new LineAnnotationAspect() {
    public String getValue(int lineNumber) {
      if (lineNumber < myLineRevisions.length) {
        return DATE_FORMAT.format(myLineRevisions[lineNumber].getRevisionDate());
      }
      return "";
    }
  };

  private final LineAnnotationAspect AUTHOR_ASPECT = new LineAnnotationAspect() {
    public String getValue(int lineNumber) {
      if (lineNumber < myLineRevisions.length) {
        return getNameWithoutDomain(myLineRevisions[lineNumber].getAuthor());
      }
      return "";
    }

    private String getNameWithoutDomain(final String name) {
      int slashIndex = name.indexOf('\\');
      if (slashIndex > -1 && slashIndex < name.length() - 1) {
        return name.substring(slashIndex + 1);
      }
      else {
        return name;
      }

    }
  };

  private TFSVcs.RevisionChangedListener myListener = new TFSVcs.RevisionChangedListener() {
    public void revisionChanged() {
      TfsFileUtil.executeInEventDispatchThread(new Runnable() {
        public void run() {
          final AnnotationListener[] listeners = myListeners.toArray(new AnnotationListener[myListeners.size()]);
          for (AnnotationListener listener : listeners) {
            listener.onAnnotationChanged();
          }
        }
      });
    }
  };

  public TFSFileAnnotation(final TFSVcs tfsVcs,
                           final WorkspaceInfo workspace,
                           final String annotatedContent,
                           final VcsFileRevision[] lineRevisions) {
    myTFSVcs = tfsVcs;
    myWorkspace = workspace;
    myAnnotatedContent = annotatedContent;
    myLineRevisions = lineRevisions;
    tfsVcs.addRevisionChangedListener(myListener);
  }

  public void addListener(AnnotationListener listener) {
    myListeners.add(listener);
  }

  public void removeListener(AnnotationListener listener) {
    myListeners.remove(listener);
  }

  public void dispose() {
    myTFSVcs.removeRevisionChangedListener(myListener);
  }

  public String getAnnotatedContent() {
    return myAnnotatedContent;
  }

  public LineAnnotationAspect[] getAspects() {
    return new LineAnnotationAspect[]{REVISION_ASPECT, DATE_ASPECT, AUTHOR_ASPECT};
  }

  public String getToolTip(final int lineNumber) {
    if (lineNumber < myLineRevisions.length) {
      return MessageFormat.format("Changeset {0}: {1}", myLineRevisions[lineNumber].getRevisionNumber().asString(),
                                  myLineRevisions[lineNumber].getCommitMessage());
    }
    return "";
  }

  public VcsRevisionNumber getLineRevisionNumber(final int lineNumber) {
    return myLineRevisions[lineNumber].getRevisionNumber();
  }

  public List<VcsFileRevision> getRevisions() {
    HashSet<VcsFileRevision> hashSet = new HashSet<VcsFileRevision>(Arrays.asList(myLineRevisions));
    List<VcsFileRevision> result = new ArrayList<VcsFileRevision>(hashSet);
    Collections.sort(result, REVISION_COMPARATOR);
    return result;
  }

  private static final Comparator<VcsFileRevision> REVISION_COMPARATOR = new Comparator<VcsFileRevision>() {
    public int compare(final VcsFileRevision revision1, final VcsFileRevision revision2) {
      return revision1.getRevisionNumber().compareTo(revision2.getRevisionNumber());
    }
  };

  private class RevisionAnnotationAspect implements LineAnnotationAspect, EditorGutterAction {
    public String getValue(int lineNumber) {
      if (lineNumber < myLineRevisions.length) {
        return myLineRevisions[lineNumber].getRevisionNumber().asString();
      }
      return "";
    }

    public Cursor getCursor(final int lineNumber) {
      if (lineNumber < myLineRevisions.length) {
        return Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
      }
      else {
        return Cursor.getDefaultCursor();
      }
    }

    public void doAction(int lineNumber) {
      if (lineNumber < myLineRevisions.length) {
        final VcsFileRevision revision = myLineRevisions[lineNumber];
        final int revisonNumber = Integer.parseInt(revision.getRevisionNumber().asString());
        final CommittedChangeList changeList =
          new TFSChangeList(myWorkspace, revisonNumber, revision.getAuthor(), revision.getRevisionDate(), revision.getCommitMessage(),
                            myTFSVcs);
        final String progress = MessageFormat.format("Loading changeset {0}...", revision.getRevisionNumber().asString());
        ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
          public void run() {
            changeList.getChanges();
          }
        }, progress, false, myTFSVcs.getProject());
        final String title = MessageFormat.format("Changeset {0}", revision.getRevisionNumber().asString());
        AbstractVcsHelper.getInstance(myTFSVcs.getProject()).showChangesListBrowser(changeList, title);
      }
    }
  }

}
