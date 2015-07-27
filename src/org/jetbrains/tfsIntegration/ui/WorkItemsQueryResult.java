package org.jetbrains.tfsIntegration.ui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItem;

import java.util.List;

public class WorkItemsQueryResult {

  @NotNull private final List<WorkItem> myWorkItems;

  public WorkItemsQueryResult(@NotNull List<WorkItem> items) {
    myWorkItems = items;
  }

  @NotNull
  public List<WorkItem> getWorkItems() {
    return myWorkItems;
  }
}
