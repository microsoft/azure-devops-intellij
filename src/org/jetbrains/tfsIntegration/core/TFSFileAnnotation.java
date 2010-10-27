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

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.annotate.*;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList;
import com.intellij.ui.GuiUtils;
import com.intellij.util.EventDispatcher;
import com.intellij.util.text.DateFormatUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.TfsUtil;
import org.jetbrains.tfsIntegration.core.tfs.WorkspaceInfo;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.*;

public class TFSFileAnnotation implements FileAnnotation {
  private final TFSVcs myVcs;
  private final WorkspaceInfo myWorkspace;
  private final String myAnnotatedContent;
  private final VcsFileRevision[] myLineRevisions;

  private final EventDispatcher<AnnotationListener> myEventDispatcher = EventDispatcher.create(AnnotationListener.class);

  private final LineAnnotationAspect REVISION_ASPECT = new TFSAnnotationAspect() {
    public String getValue(int lineNumber) {
      if (lineNumber < myLineRevisions.length) {
        return myLineRevisions[lineNumber].getRevisionNumber().asString();
      }
      else {
        return "";
      }
    }
  };

  private final LineAnnotationAspect DATE_ASPECT = new TFSAnnotationAspect() {
    public String getValue(int lineNumber) {
      if (lineNumber < myLineRevisions.length) {
        return DateFormatUtil.formatPrettyDate(myLineRevisions[lineNumber].getRevisionDate());
      }
      else {
        return "";
      }
    }
  };

  private final LineAnnotationAspect AUTHOR_ASPECT = new TFSAuthorAnnotationAspect() {
    public String getValue(int lineNumber) {
      if (lineNumber < myLineRevisions.length) {
        return TfsUtil.getNameWithoutDomain(myLineRevisions[lineNumber].getAuthor());
      }
      else {
        return "";
      }
    }
  };

  private final TFSVcs.RevisionChangedListener myListener = new TFSVcs.RevisionChangedListener() {
    public void revisionChanged() {
      try {
        GuiUtils.runOrInvokeAndWait(new Runnable() {
          public void run() {
            myEventDispatcher.getMulticaster().onAnnotationChanged();
          }
        });
      }
      catch (InvocationTargetException e) {
        // ignore
      }
      catch (InterruptedException e) {
        // ignore
      }
    }
  };

  public TFSFileAnnotation(final TFSVcs vcs,
                           final WorkspaceInfo workspace,
                           final String annotatedContent,
                           final VcsFileRevision[] lineRevisions) {
    myVcs = vcs;
    myWorkspace = workspace;
    myAnnotatedContent = annotatedContent;
    myLineRevisions = lineRevisions;
    myVcs.addRevisionChangedListener(myListener);
  }

  public void addListener(AnnotationListener listener) {
    myEventDispatcher.addListener(listener);
  }

  public void removeListener(AnnotationListener listener) {
    myEventDispatcher.removeListener(listener);
  }

  public void dispose() {
    myVcs.removeRevisionChangedListener(myListener);
  }

  public String getAnnotatedContent() {
    return myAnnotatedContent;
  }

  public LineAnnotationAspect[] getAspects() {
    return new LineAnnotationAspect[]{REVISION_ASPECT, DATE_ASPECT, AUTHOR_ASPECT};
  }

  public String getToolTip(final int lineNumber) {
    if (lineNumber < myLineRevisions.length) {
      String commitMessage =
        myLineRevisions[lineNumber].getCommitMessage() == null ? "(no comment)" : myLineRevisions[lineNumber].getCommitMessage();
      return MessageFormat.format("Changeset {0}: {1}", myLineRevisions[lineNumber].getRevisionNumber().asString(), commitMessage);
    }
    else {
      return "";
    }
  }

  @Nullable
  public VcsRevisionNumber getLineRevisionNumber(final int lineNumber) {
    if (lineNumber < myLineRevisions.length) {
      return myLineRevisions[lineNumber].getRevisionNumber();
    }
    else {
      return null;
    }
  }

  /**
   * Get revision number for the line.
   */
  public VcsRevisionNumber originalRevision(int lineNumber) {
    return getLineRevisionNumber(lineNumber);
  }

  public List<VcsFileRevision> getRevisions() {
    Set<VcsFileRevision> set = new HashSet<VcsFileRevision>(Arrays.asList(myLineRevisions));
    List<VcsFileRevision> result = new ArrayList<VcsFileRevision>(set);
    Collections.sort(result, REVISION_COMPARATOR);
    return result;
  }

  public boolean revisionsNotEmpty() {
    return myLineRevisions.length > 0;
  }

  public AnnotationSourceSwitcher getAnnotationSourceSwitcher() {
    return null;
  }

  private static final Comparator<VcsFileRevision> REVISION_COMPARATOR = new Comparator<VcsFileRevision>() {
    public int compare(final VcsFileRevision revision1, final VcsFileRevision revision2) {
      return revision1.getRevisionNumber().compareTo(revision2.getRevisionNumber());
    }
  };

  private abstract class TFSAnnotationAspect extends LineAnnotationAspectAdapter {
    @Override
    protected void showAffectedPaths(int lineNum) {
      if (lineNum < myLineRevisions.length) {
        final VcsFileRevision revision = myLineRevisions[lineNum];
        final int changeset = ((VcsRevisionNumber.Int)revision.getRevisionNumber()).getValue();
        final CommittedChangeList changeList =
          new TFSChangeList(myWorkspace, changeset, revision.getAuthor(), revision.getRevisionDate(), revision.getCommitMessage(), myVcs);
        final String progress = MessageFormat.format("Loading changeset {0}...", revision.getRevisionNumber().asString());
        ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
          public void run() {
            changeList.getChanges();
          }
        }, progress, false, myVcs.getProject());
        final String title = MessageFormat.format("Changeset {0}", revision.getRevisionNumber().asString());
        AbstractVcsHelper.getInstance(myVcs.getProject()).showChangesListBrowser(changeList, title);
      }
    }
  }

  private abstract class TFSAuthorAnnotationAspect extends TFSAnnotationAspect implements MajorLineAnnotationAspect {
  }

}
