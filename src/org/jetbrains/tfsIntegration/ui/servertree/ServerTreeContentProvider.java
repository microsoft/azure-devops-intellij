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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;
import org.jetbrains.tfsIntegration.core.tfs.VersionControlPath;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Item;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.ItemType;
import org.jetbrains.tfsIntegration.ui.deferredtree.ContentProvider;
import org.jetbrains.tfsIntegration.ui.deferredtree.ContentProviderException;

import java.util.Collection;
import java.util.Collections;

public class ServerTreeContentProvider implements ContentProvider<Item> {

  static final Item ROOT;

  private final ServerInfo myServer;

  private final boolean myFoldersOnly;

  static {
    ROOT = new Item();
    ROOT.setItem(VersionControlPath.ROOT_FOLDER);
    ROOT.setType(ItemType.Folder);
  }

  public ServerTreeContentProvider(final ServerInfo server, final boolean foldersOnly) {
    myServer = server;
    myFoldersOnly = foldersOnly;
  }

  public boolean canHaveChildren(final @Nullable Item parent) {
    return parent == null || parent.getType() == ItemType.Folder;
  }

  public Collection<Item> getChildren(final @Nullable Item parent) throws ContentProviderException {
    if (parent == null) {
      return Collections.singletonList(ROOT);
    }
    else {
      try {
        return myServer.getVCS().getChildItems(parent.getItem(), myFoldersOnly);
      }
      catch (TfsException e) {
        throw new ContentProviderException(e);
      }
    }
  }

  public String getLabel(final @NotNull Item item) {
    if (ROOT == item) {
      return myServer.getUri().toString();
    }
    else {
      return item.getItem().substring(item.getItem().lastIndexOf(VersionControlPath.PATH_SEPARATOR) + 1);
    }
  }

  public boolean equals(final @NotNull Item item1, final @NotNull Item item2) {
    return item1.getItem().equals(item2.getItem());
  }

  public int getHashCode(final @NotNull Item item) {
    return item.getItem().hashCode();
  }
}
