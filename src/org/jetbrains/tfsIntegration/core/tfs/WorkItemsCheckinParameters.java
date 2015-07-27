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

import com.intellij.util.containers.ContainerUtil;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.CheckinWorkItemAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItem;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class WorkItemsCheckinParameters {

  @NotNull private List<WorkItem> myWorkItems;
  @NotNull private Map<WorkItem, CheckinWorkItemAction> myActions;

  private WorkItemsCheckinParameters(@NotNull List<WorkItem> workItems, @NotNull Map<WorkItem, CheckinWorkItemAction> actions) {
    myWorkItems = workItems;
    myActions = actions;
  }

  public WorkItemsCheckinParameters() {
    this(Collections.<WorkItem>emptyList(), ContainerUtil.<WorkItem, CheckinWorkItemAction>newHashMap());
  }

  @Nullable
  public CheckinWorkItemAction getAction(@NotNull WorkItem workItem) {
    return myActions.get(workItem);
  }

  public void setAction(@NotNull WorkItem workItem, @NotNull CheckinWorkItemAction action) {
    myActions.put(workItem, action);
  }

  public void removeAction(@NotNull WorkItem workItem) {
    myActions.remove(workItem);
  }

  @NotNull
  public List<WorkItem> getWorkItems() {
    return Collections.unmodifiableList(myWorkItems);
  }

  @NotNull
  public WorkItemsCheckinParameters createCopy() {
    return new WorkItemsCheckinParameters(ContainerUtil.newArrayList(myWorkItems), ContainerUtil.newHashMap(myActions));
  }

  public void update(@NotNull List<WorkItem> workItems) {
    myWorkItems = workItems;
    myActions.clear();
  }

  public void update(@NotNull WorkItemsCheckinParameters parameters) {
    myWorkItems = parameters.myWorkItems;
    myActions = parameters.myActions;
  }

  @NotNull
  public Map<WorkItem, CheckinWorkItemAction> getWorkItemsActions() {
    return Collections.unmodifiableMap(myActions);
  }
}
