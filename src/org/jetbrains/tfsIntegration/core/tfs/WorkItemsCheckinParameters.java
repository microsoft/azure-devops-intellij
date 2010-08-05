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

import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.CheckinWorkItemAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItem;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItemsQuery;

import java.util.*;

public class WorkItemsCheckinParameters {
  private List<WorkItem> myWorkItems;
  private Map<WorkItem, CheckinWorkItemAction> myActions;
  private WorkItemsQuery myQuery;

  private WorkItemsCheckinParameters(final List<WorkItem> workItems,
                               final Map<WorkItem, CheckinWorkItemAction> actions,
                               final WorkItemsQuery query) {
    myWorkItems = workItems;
    myActions = actions;
    myQuery = query;
  }

  public WorkItemsCheckinParameters() {
    this(Collections.<WorkItem>emptyList(), new HashMap<WorkItem, CheckinWorkItemAction>(), null);
  }

  @Nullable
  public CheckinWorkItemAction getAction(final @NotNull WorkItem workItem) {
    return myActions.get(workItem);
  }

  public void setAction(final @NotNull WorkItem workItem, final @NotNull CheckinWorkItemAction action) {
    myActions.put(workItem, action);
  }

  public void removeAction(final @NotNull WorkItem workItem) {
    myActions.remove(workItem);
  }

  public WorkItemsQuery getQuery() {
    return myQuery;
  }

  public List<WorkItem> getWorkItems() {
    return Collections.unmodifiableList(myWorkItems);
  }

  public WorkItemsCheckinParameters createCopy() {
    return new WorkItemsCheckinParameters(new ArrayList<WorkItem>(myWorkItems), new HashMap<WorkItem, CheckinWorkItemAction>(myActions), myQuery);
  }

  public void update(final WorkItemsQuery query, final List<WorkItem> workItems) {
    myQuery = query;
    myWorkItems = workItems;
    myActions.clear();
  }

  public Map<WorkItem, CheckinWorkItemAction> getWorkItemsActions() {
    return Collections.unmodifiableMap(myActions);
  }
}
