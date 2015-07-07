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

package org.jetbrains.tfsIntegration.core.tfs.workitems;

import com.intellij.openapi.util.text.StringUtil;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.CheckinWorkItemAction;
import com.microsoft.tfs.core.clients.workitem.fields.FieldCollection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WorkItem {

  // a great many of other properties may be added here if will be needed in UI or elsewhere
  private final int myId;

  private @Nullable final String myAssignedTo;

  private final WorkItemState myState;

  private final String myTitle;

  private final int myRevision;

  private final WorkItemType myType;

  private final String myReason;

  public WorkItem(final int id,
                  @Nullable final String assignedTo,
                  final WorkItemState state,
                  final String title,
                  final int revision,
                  final WorkItemType type,
                  final String reason) {
    myAssignedTo = assignedTo;
    myId = id;
    myReason = reason;
    myRevision = revision;
    myState = state;
    myTitle = title;
    myType = type;
  }

  @NotNull
  public static WorkItem create(@NotNull com.microsoft.tfs.core.clients.workitem.WorkItem workItem) {
    int id = workItem.getID();
    String assignedTo = getStringValue(workItem, WorkItemField.ASSIGNED_TO);
    String state = getStringValue(workItem, WorkItemField.STATE);
    String title = workItem.getTitle();
    int revision = workItem.getFields().getRevision();
    String type = workItem.getType().getName();
    String reason = getStringValue(workItem, WorkItemField.REASON);

    return new WorkItem(id, assignedTo, WorkItemState.from(state), title, revision, WorkItemType.from(type), reason);
  }

  @NotNull
  private static String getStringValue(@NotNull com.microsoft.tfs.core.clients.workitem.WorkItem workItem, @NotNull WorkItemField field) {
    FieldCollection fields = workItem.getFields();
    String result = null;

    if (fields.contains(field.getSerialized())) {
      result = (String)fields.getField(field.getSerialized()).getValue();
    }

    return StringUtil.notNullize(result);
  }

  public int getId() {
    return myId;
  }

  public @Nullable String getAssignedTo() {
    return myAssignedTo;
  }

  public WorkItemState getState() {
    return myState;
  }

  public String getTitle() {
    return myTitle;
  }

  public int getRevision() {
    return myRevision;
  }

  public WorkItemType getType() {
    return myType;
  }

  public String getReason() {
    return myReason;
  }

  public boolean isActionPossible(CheckinWorkItemAction action) {
    return action == CheckinWorkItemAction.None ||
           action == CheckinWorkItemAction.Associate ||
           myState == WorkItemState.ACTIVE && (myType == WorkItemType.BUG || myType == WorkItemType.TASK);
  }

  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final WorkItem workItem = (WorkItem)o;

    if (myId != workItem.myId) return false;

    return true;
  }

  public int hashCode() {
    return myId;
  }
}
