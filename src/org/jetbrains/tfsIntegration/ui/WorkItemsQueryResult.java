package org.jetbrains.tfsIntegration.ui;

import com.microsoft.tfs.core.clients.workitem.query.WorkItemLinkInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItem;

import java.util.List;

public class WorkItemsQueryResult {

  @NotNull private final List<WorkItem> myWorkItems;
  @Nullable private final List<WorkItemLinkInfo> myLinks;

  public WorkItemsQueryResult(@NotNull List<WorkItem> workItems) {
    this(workItems, null);
  }

  public WorkItemsQueryResult(@NotNull List<WorkItem> items, @Nullable List<WorkItemLinkInfo> links) {
    myWorkItems = items;
    myLinks = links;
  }

  @NotNull
  public List<WorkItem> getWorkItems() {
    return myWorkItems;
  }

  @Nullable
  public List<WorkItemLinkInfo> getLinks() {
    return myLinks;
  }
}
