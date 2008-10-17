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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.*;
import org.jetbrains.tfsIntegration.core.tfs.VersionControlServer;
import org.jetbrains.tfsIntegration.core.tfs.version.VersionSpecBase;

import java.util.List;
import java.util.Collections;

public class LabelItemSpecWithItems {

  private final LabelItemSpec myLabelItemSpec;
  private final List<Item> myItemsList;

  private LabelItemSpecWithItems(final @NotNull LabelItemSpec labelItemSpec, final @NotNull List<Item> itemsList) {
    myLabelItemSpec = labelItemSpec;
    myItemsList = itemsList;
  }

  public static LabelItemSpecWithItems createForAdd(final @NotNull ItemSpec item,
                                                    final @NotNull VersionSpecBase version,
                                                    final @NotNull List<Item> itemsList) {
    LabelItemSpec labelItemSpec = new LabelItemSpec();
    labelItemSpec.setItemSpec(item);
    labelItemSpec.setVersion(version);
    labelItemSpec.setEx(false);
    return new LabelItemSpecWithItems(labelItemSpec, itemsList);
  }

  public static LabelItemSpecWithItems createForRemove(final @NotNull ItemAndVersion item) {
    ItemSpec itemSpec = VersionControlServer
      .createItemSpec(item.getItem().getItem(), item.getItem().getType() == ItemType.Folder ? RecursionType.Full : null);

    LabelItemSpec labelItemSpec = new LabelItemSpec();
    labelItemSpec.setEx(true);
    labelItemSpec.setItemSpec(itemSpec);
    labelItemSpec.setVersion(item.getVersionSpec());
    return new LabelItemSpecWithItems(labelItemSpec, Collections.<Item>emptyList());
  }

  public LabelItemSpec getLabelItemSpec() {
    return myLabelItemSpec;
  }

  public List<Item> getItemsList() {
    return myItemsList;
  }

  public String getServerPath() {
    return getLabelItemSpec().getItemSpec().getItem();
  }

  public boolean isRemoval() {
    return getLabelItemSpec().getEx();
  }
}
