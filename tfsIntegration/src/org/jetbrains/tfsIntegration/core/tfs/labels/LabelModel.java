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
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.LabelItemSpec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.tfs.VersionControlPath;

import java.util.*;

public class LabelModel {

  private final List<LabelItemSpecWithItems> myLabelSpecs = new ArrayList<LabelItemSpecWithItems>();

  public void add(final @NotNull LabelItemSpecWithItems newSpec) {
    // when adding parent spec, remove all child ones (and equal)
    // removal spec should be added only if no parent removal spec exists
    for (Iterator<LabelItemSpecWithItems> iterator = myLabelSpecs.iterator(); iterator.hasNext();) {
      final LabelItemSpecWithItems existingSpec = iterator.next();
      if (VersionControlPath.isUnder(newSpec.getServerPath(), existingSpec.getServerPath())) {
        iterator.remove();
      }
    }
    myLabelSpecs.add(newSpec);

  }

  public void addAll(final List<LabelItemSpecWithItems> newSpecs) {
    // add child specs first to have them removed when parent is added 
    Collections.sort(newSpecs, ITEM_SPEC_CHILDREN_FIRST);
    for (LabelItemSpecWithItems spec : newSpecs) {
      add(spec);
    }
  }

  // item sets for child specs override those for parent ones, so include items that do not appear under child specs
  public List<ItemAndVersion> calculateItemsToDisplay() {
    Collections.sort(myLabelSpecs, ITEM_SPEC_PARENT_FIRST);
    final List<ItemAndVersion> result = new ArrayList<ItemAndVersion>();

    for (int i = 0; i < myLabelSpecs.size(); i++) {
      final LabelItemSpecWithItems labelSpec = myLabelSpecs.get(i);
      // removal label spec has no items anyway
      for (Item item : labelSpec.getItemsList()) {
        boolean appearsUnderChild = false;
        for (int j = i + 1; j < myLabelSpecs.size(); j++) {
          if (VersionControlPath.isUnder(myLabelSpecs.get(j).getServerPath(), item.getItem())) {
            appearsUnderChild = true;
            break;
          }
        }
        if (!appearsUnderChild) {
          result.add(new ItemAndVersion(item, labelSpec.getLabelItemSpec().getVersion()));
        }
      }
    }

    Collections.sort(result, ITEM_AND_VERSION_PARENT_FIRST);
    return result;
  }

  /**
   * @return sorted: parent first
   */
  public List<LabelItemSpec> getLabelItemSpecs() {
    Collections.sort(myLabelSpecs, ITEM_SPEC_PARENT_FIRST);
    List<LabelItemSpec> result = new ArrayList<LabelItemSpec>(myLabelSpecs.size());
    for (LabelItemSpecWithItems labelSpec : myLabelSpecs) {
      result.add(labelSpec.getLabelItemSpec());
    }
    return result;
  }

  private static final Comparator<LabelItemSpecWithItems> ITEM_SPEC_PARENT_FIRST = new Comparator<LabelItemSpecWithItems>() {
    public int compare(final LabelItemSpecWithItems o1, final LabelItemSpecWithItems o2) {
      return VersionControlPath.compareParentToChild(o1.getServerPath(), o2.getServerPath());
    }
  };

  private static final Comparator<LabelItemSpecWithItems> ITEM_SPEC_CHILDREN_FIRST = new Comparator<LabelItemSpecWithItems>() {
    public int compare(final LabelItemSpecWithItems o1, final LabelItemSpecWithItems o2) {
      return -VersionControlPath.compareParentToChild(o1.getServerPath(), o2.getServerPath());
    }
  };

  private static final Comparator<ItemAndVersion> ITEM_AND_VERSION_PARENT_FIRST = new Comparator<ItemAndVersion>() {
    public int compare(final ItemAndVersion o1, final ItemAndVersion o2) {
      return VersionControlPath.compareParentToChild(o1.getServerPath(), o1.isDirectory(), o2.getServerPath(), o2.isDirectory());
    }
  };

}
