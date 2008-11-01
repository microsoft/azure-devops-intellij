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

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.AbstractVcsHelper;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.*;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;

public class TfsUtil {

  private static final String CHANGES_TOOLWINDOW_ID = "Changes";

  // TODO refactor (workspace is searched for the second time)
  @Nullable
  public static ExtendedItem getExtendedItem(final FilePath localPath) throws TfsException {
    Collection<WorkspaceInfo> workspaces = Workstation.getInstance().findWorkspaces(localPath, false);
    if (workspaces.isEmpty()) {
      return null;
    }
    final WorkspaceInfo workspace = workspaces.iterator().next();
    return workspace.getServer().getVCS()
      .getExtendedItem(workspace.getName(), workspace.getOwnerName(), localPath, RecursionType.None, DeletedState.Any);
  }


  @Nullable
  public static ExtendedItem getExtendedItem(Project project, final FilePath localPath, final String errorTabTitle) {
    try {
      return getExtendedItem(localPath);
    }
    catch (TfsException e) {
      //noinspection ThrowableInstanceNeverThrown
      AbstractVcsHelper.getInstance(project).showError(new VcsException(e.getMessage(), e), errorTabTitle);
      return null;
    }
  }

  public static VcsRevisionNumber getCurrentRevisionNumber(Project project, FilePath path) {
    ExtendedItem item = getExtendedItem(project, path, TFSVcs.TFS_NAME);
    return (item != null && item.getLver() != Integer.MIN_VALUE) ? new VcsRevisionNumber.Int(item.getLver()) : VcsRevisionNumber.NULL;

  }

  public static VcsException collectExceptions(Collection<VcsException> exceptions) {
    if (exceptions.isEmpty()) {
      throw new IllegalArgumentException("No exceptions to collect");
    }
    if (exceptions.size() == 1) {
      // TODO: VcsException does not correctly support single message case?
      return exceptions.iterator().next();
    }
    else {
      StringBuilder s = new StringBuilder();
      for (VcsException exception : exceptions) {
        if (s.length() > 0) {
          s.append("\n");
        }
        s.append(exception.getMessage());
      }
      return new VcsException(s.toString());
    }
  }

  public static List<FilePath> getLocalPaths(final List<ItemPath> paths) {
    List<FilePath> localPaths = new ArrayList<FilePath>(paths.size());
    for (ItemPath path : paths) {
      localPaths.add(path.getLocalPath());
    }
    return localPaths;
  }

  /**
   * @return Gregorian calendar that stores date "0001-01-01T00:00:00.000Z" to be used in requests to TFS server
   */
  public static Calendar getZeroCalendar() {
    final Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
    calendar.clear();
    calendar.set(1, Calendar.JANUARY, 1, 0, 0, 0);
    return calendar;
  }

  public static List<VcsException> getVcsExceptions(Collection<Failure> failures) {
    final List<VcsException> exceptions = new ArrayList<VcsException>();
    for (Failure failure : failures) {
      if (failure.getSev() != SeverityType.Warning) {
        exceptions.add(new VcsException(failure.getMessage()));
      }
    }
    return exceptions;
  }

  public static String getNameWithoutDomain(final String qualifiedName) {
    int slashIndex = qualifiedName.indexOf('\\');
    if (slashIndex > -1 && slashIndex < qualifiedName.length() - 1) {
      return qualifiedName.substring(slashIndex + 1);
    }
    else {
      return qualifiedName;
    }
  }

  @Nullable
  public static URI getHostUri(String uriText, boolean complainOnPath) {
    try {
      final URI uri = new URI(uriText).normalize();
      if (StringUtil.isEmpty(uri.getHost())) {
        return null;
      }
      if (complainOnPath && !"".equals(uri.getPath()) && !"/".equals(uri.getPath())) {
        return null;
      }
      return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), "/", null, null);
    }
    catch (URISyntaxException e) {
      return null;
    }

  }

  public static void showBalloon(final Project project, final MessageType messageType, final String messageHtml) {
    Runnable r = new Runnable() {
      public void run() {
        final ToolWindowManager manager = ToolWindowManager.getInstance(project);
        if (Arrays.asList(manager.getToolWindowIds()).contains(CHANGES_TOOLWINDOW_ID)) {
          manager.notifyByBalloon(CHANGES_TOOLWINDOW_ID, messageType, messageHtml);
        }
        else {
          final Balloon balloon = JBPopupFactory.getInstance()
            .createHtmlTextBalloonBuilder(messageHtml.replace("\n", "<br>"), messageType.getDefaultIcon(), messageType.getPopupBackground(),
                                          null).createBalloon();

          // TODO FIXME what to do if null?
          if (WindowManager.getInstance().getFrame(project) != null) {
            final JComponent component = WindowManager.getInstance().getFrame(project).getRootPane();
            final Rectangle rect = component.getVisibleRect();
            final Point p = new Point(rect.x + 30, rect.y + rect.height - 10);
            final RelativePoint point = new RelativePoint(component, p);
            balloon.show(point, Balloon.Position.under);
          }
        }
      }
    };
    Application application = ApplicationManager.getApplication();
    if (application.isDispatchThread()) {
      r.run();
    }
    else {
      SwingUtilities.invokeLater(r);
    }
  }

}
