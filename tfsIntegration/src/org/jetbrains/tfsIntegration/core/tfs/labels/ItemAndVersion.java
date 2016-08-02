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

package org.jetbrains.tfsIntegration.core.tfs.labels;

import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.Item;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.ItemType;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.VersionSpec;
import org.jetbrains.annotations.NotNull;

public class ItemAndVersion {

  private final Item myItem;
  private final VersionSpec myVersionSpec;

  public ItemAndVersion(final @NotNull Item item, final @NotNull VersionSpec versionSpec) {
    myItem = item;
    myVersionSpec = versionSpec;
  }

  public Item getItem() {
    return myItem;
  }

  public VersionSpec getVersionSpec() {
    return myVersionSpec;
  }

  public String getServerPath() {
    return myItem.getItem();
  }

  public boolean isDirectory() {
    return myItem.getType() == ItemType.Folder;
  }

}
