
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

import com.intellij.notification.NotificationGroup;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.util.ThrowableConsumer;
import com.intellij.util.UriUtil;
import com.intellij.util.io.URLUtil;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.*;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.revision.TFSContentRevision;
import org.jetbrains.tfsIntegration.exceptions.TfsException;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.*;

public class TfsUtil {

  private static final Logger LOG = Logger.getInstance(TfsUtil.class.getName());
  @NonNls private static final String CHANGES_TOOLWINDOW_ID = Registry.is("vcs.merge.toolwindows", false) ? ToolWindowId.VCS : "Changes";
  private static final NotificationGroup NOTIFICATION_GROUP = NotificationGroup.toolWindowGroup("TFS", CHANGES_TOOLWINDOW_ID);

  @Nullable
  public static Pair<WorkspaceInfo, ExtendedItem> getWorkspaceAndExtendedItem(final FilePath localPath,
                                                                              Object projectOrComponent,
                                                                              String progressTitle) throws TfsException {
    Collection<WorkspaceInfo> workspaces = Workstation.getInstance().findWorkspaces(localPath, false, projectOrComponent);
    if (workspaces.isEmpty()) {
      return null;
    }
    final WorkspaceInfo workspace = workspaces.iterator().next();
    final ExtendedItem item = workspace.getServer().getVCS()
      .getExtendedItem(workspace.getName(), workspace.getOwnerName(), localPath, RecursionType.None, DeletedState.Any, projectOrComponent,
                       progressTitle);
    return Pair.create(workspace, item);
  }

  public static VcsRevisionNumber getCurrentRevisionNumber(FilePath path, Object projectOrComponent, String progressTitle) {
    try {
      Pair<WorkspaceInfo, ExtendedItem> workspaceAndItem = getWorkspaceAndExtendedItem(path, projectOrComponent, progressTitle);
      return workspaceAndItem != null && workspaceAndItem.second != null
             ? getCurrentRevisionNumber(workspaceAndItem.second)
             : VcsRevisionNumber.NULL;
    }
    catch (TfsException e) {
      return VcsRevisionNumber.NULL;
    }
  }

  @Nullable
  public static TFSContentRevision getCurrentRevision(Project project, FilePath path, String progressTitle) throws TfsException {
    Pair<WorkspaceInfo, ExtendedItem> workspaceAndItem = getWorkspaceAndExtendedItem(path, project, progressTitle);
    if (workspaceAndItem != null && workspaceAndItem.second != null) {
      return TFSContentRevision
        .create(project, workspaceAndItem.first, workspaceAndItem.second.getLver(), workspaceAndItem.second.getItemid());
    }
    else {
      return null;
    }
  }

  public static VcsRevisionNumber getCurrentRevisionNumber(final @NotNull ExtendedItem item) {
    return item.getLver() != Integer.MIN_VALUE ? new TfsRevisionNumber(item.getLver(), item.getItemid()) : VcsRevisionNumber.NULL;
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
    @SuppressWarnings({"HardCodedStringLiteral"})
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
  public static URI getUrl(String uriText, boolean complainOnPath, boolean trimPath) {
    int i = uriText.indexOf(URLUtil.SCHEME_SEPARATOR);

    try {
      final URI uri;
      if (i == -1) {
        uri = new URI("http", "//" + uriText, null).normalize();
      }
      else {
        uri = new URI(uriText.substring(0, i), "//" + uriText.substring(i + URLUtil.SCHEME_SEPARATOR.length()), null).normalize();
      }
      if (StringUtil.isEmpty(uri.getHost())) {
        return null;
      }

      int port = uri.getPort();
      if (port > 0xFFFF) {
        return null;
      }

      if (complainOnPath && uri.getPath() != null && !uri.getPath().isEmpty() && !"/".equals(uri.getPath())) {
        return null;
      }
      if (trimPath) {
        return new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), "/", null, null);
      }
      else {
        return uri;
      }
    }
    catch (URISyntaxException e) {
      return null;
    }

  }

  public static void showBalloon(final Project project, final MessageType messageType, final String messageHtml) {
    NOTIFICATION_GROUP.createNotification(messageHtml, messageType).notify(project);
  }

  public static String getPresentableUri(URI uri) {
    try {
      return URLDecoder.decode(uri.toString(), "UTF-8");
    }
    catch (UnsupportedEncodingException e) {
      LOG.error(e);
      return null;
    }
  }

  public static String getQualifiedUsername(String domain, String userName) {
    return domain + "\\" + userName;
  }

  public static <T, E extends Throwable> void consumeInParts(List<T> items, int maxPartSize, ThrowableConsumer<List<T>, E> consumer) throws E {
    for (int group = 0; group <= items.size() / maxPartSize; group++) {
      List<T> subList = items.subList(group * maxPartSize, Math.min((group + 1) * maxPartSize, items.size()));
      if (!subList.isEmpty()) {
        consumer.consume(subList);
      }
    }
  }

  public static String appendPath(URI serverUri, String path) {
    path = StringUtil.trimStart(path, "/");
    return UriUtil.trimTrailingSlashes(serverUri.toString()) + "/" + path.replace(" ", "%20");
  }
}
