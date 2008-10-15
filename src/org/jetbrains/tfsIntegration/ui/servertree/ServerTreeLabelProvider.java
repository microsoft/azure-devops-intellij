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

package org.jetbrains.tfsIntegration.ui.servertree;

import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.vcs.FileStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.tfsIntegration.core.tfs.VersionControlPath;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Item;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ItemType;
import org.jetbrains.tfsIntegration.ui.deferredtree.LabelProvider;
import org.jetbrains.tfsIntegration.ui.UiConstants;

import javax.swing.*;
import java.awt.*;

public class ServerTreeLabelProvider implements LabelProvider<Item> {

  @NonNls private static final Icon ICON_VIRTUAL_FOLDER = IconLoader.getIcon("/actions/newFolder.png");
  private static final Icon ICON_ERROR = IconLoader.findIcon("/icons/error.gif");

  private static final Color COLOR_DISABLED = Color.GRAY;
  private static final Color COLOR_VIRTUAL_ITEM = FileStatus.COLOR_ADDED;

  private final String myServerUri;
  private final @Nullable ServerTree.PathFilter myPathFilter;

  public ServerTreeLabelProvider(final String serverUri, final @Nullable ServerTree.PathFilter pathFilter) {
    myServerUri = serverUri;
    myPathFilter = pathFilter;
  }

  public String getLabel(final @NotNull Item item) {
    if (ServerTreeContentProvider.ROOT == item) {
      return myServerUri;
    }
    else {
      return item.getItem().substring(item.getItem().lastIndexOf(VersionControlPath.PATH_SEPARATOR) + 1);
    }
  }

  public Icon getIcon(final @NotNull Item item) {
    if (VersionControlPath.ROOT_FOLDER.equals(item.getItem())) {
      return UiConstants.ICON_TEAM_SERVER;
    }
    return item.getType() == ItemType.Folder ? UiConstants.ICON_FOLDER : UiConstants.ICON_FILE;
  }

  @Nullable
  public Color getColor(final @NotNull Item item) {
    if (myPathFilter != null && !myPathFilter.isAcceptablePath(item.getItem())) {
      return COLOR_DISABLED;
    }
    return null;
  }

  public Icon getVirtualItemIcon() {
    return ICON_VIRTUAL_FOLDER;
  }

  @Nullable
  public Color getVirtualItemColor() {
    return COLOR_VIRTUAL_ITEM;
  }

  public Icon getErrorIcon() {
    return ICON_ERROR;
  }

  public Color getErrorColor() {
    return Color.RED;
  }

}
