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

import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.CheckinWorkItemAction;
import com.microsoft.schemas.teamfoundation._2005._06.workitemtracking.clientservices._03.*;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.exceptions.OperationFailedException;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WorkItemSerialize {

  public static final List<WorkItemField> FIELDS = Arrays.asList(WorkItemField.ID, WorkItemField.STATE,
                                                                 WorkItemField.TITLE, WorkItemField.REVISION, WorkItemField.TYPE,
                                                                 WorkItemField.REASON, WorkItemField.ASSIGNED_TO);

  private static final String SERVER_DATE_TIME = "ServerDateTime";

  private enum Reason {
    Fixed, Completed
  }

  public static WorkItem createFromFields(String[] workItemFieldsValues) throws OperationFailedException {
    try {
      int id = Integer.parseInt(workItemFieldsValues[FIELDS.indexOf(WorkItemField.ID)]);
      WorkItem.WorkItemState state = WorkItem.WorkItemState.valueOf(workItemFieldsValues[FIELDS.indexOf(WorkItemField.STATE)]);
      String title = workItemFieldsValues[FIELDS.indexOf(WorkItemField.TITLE)];
      int revision = Integer.parseInt(workItemFieldsValues[FIELDS.indexOf(WorkItemField.REVISION)]);
      WorkItemType type = WorkItemType.fromString(workItemFieldsValues[FIELDS.indexOf(WorkItemField.TYPE)]);
      String reason = workItemFieldsValues[FIELDS.indexOf(WorkItemField.REASON)];
      @Nullable final String assignedTo;
      if (workItemFieldsValues.length > FIELDS.indexOf(WorkItemField.ASSIGNED_TO)) {
        assignedTo = workItemFieldsValues[FIELDS.indexOf(WorkItemField.ASSIGNED_TO)];
      } else {
        assignedTo = null;
      }
      return new WorkItem(id, assignedTo, state, title, revision, type, reason);
    }
    catch (Exception e) {
      // TODO remove this
      throw new OperationFailedException("Cannot load work items: unexpected properties encountered");
    }
  }

  @Nullable
  public static Columns_type0 generateColumnsForUpdateRequest(WorkItemType type,
                                                              String reason,
                                                              CheckinWorkItemAction action,
                                                              String identity) {
    if (action != CheckinWorkItemAction.Resolve) {
      // needed for 'resolved' work items only
      return null;
    }

    final List<Column_type0> columns = new ArrayList<Column_type0>();
    if (type == WorkItemType.Bug) {
      columns.add(createColumn(WorkItemField.STATE, null, WorkItem.WorkItemState.Resolved.name()));
      columns.add(createColumn(WorkItemField.REASON, null, Reason.Fixed.name()));
      columns.add(createColumn(WorkItemField.STATE_CHANGE_DATE, SERVER_DATE_TIME, ""));
      columns.add(createColumn(WorkItemField.RESOLVED_DATE, SERVER_DATE_TIME, ""));
      columns.add(createColumn(WorkItemField.RESOLVED_BY, null, identity));
      columns.add(createColumn(WorkItemField.RESOLVED_REASON, null, reason));
    }
    else if (type == WorkItemType.Task) {
      columns.add(createColumn(WorkItemField.STATE, null, WorkItem.WorkItemState.Closed.name()));
      columns.add(createColumn(WorkItemField.REASON, null, Reason.Completed.name()));
      columns.add(createColumn(WorkItemField.STATE_CHANGE_DATE, SERVER_DATE_TIME, ""));
      columns.add(createColumn(WorkItemField.CLOSED_DATE, SERVER_DATE_TIME, ""));
      columns.add(createColumn(WorkItemField.CLOSED_BY, null, identity));
    }
    else {
      throw new IllegalArgumentException("Unexpected work item type " + type);
    }

    Columns_type0 columnsArray = new Columns_type0();
    columnsArray.setColumn(columns.toArray(new Column_type0[columns.size()]));
    return columnsArray;
  }

  /**
   * @throws IllegalArgumentException if this work item is not related to checkin
   */
  public static InsertText_type0 generateInsertTextForUpdateRequest(CheckinWorkItemAction action, int changeSetId) {
    if (action == CheckinWorkItemAction.None) {
      throw new IllegalArgumentException("Unexpected action type " + action);
    }
    InsertText_type0 insertText = new InsertText_type0();
    insertText.setFieldDisplayName("History");
    insertText.setFieldName(WorkItemField.HISTORY.getSerialized());
    // force invariant changeset formatting
    String text = MessageFormat
      .format("{0} with changeset {1}.", action == CheckinWorkItemAction.Resolve ? "Resolved" : "Associated", String.valueOf(changeSetId));
    insertText.setString(text);
    return insertText;
  }

  public static InsertResourceLink_type0 generateInsertResourceLinkforUpdateRequest(int changeSetId) {
    InsertResourceLink_type0 insertResourceLink = new InsertResourceLink_type0();
    insertResourceLink.setFieldName(WorkItemField.BISLINKS.getSerialized());
    insertResourceLink.setLinkType("Fixed in Changeset");
    insertResourceLink.setComment("Source control changeset " + changeSetId);
    insertResourceLink.setLocation("vstfs:///VersionControl/Changeset/" + changeSetId);
    return insertResourceLink;
  }

  public static ComputedColumns_type0 generateComputedColumnsForUpdateRequest(WorkItemType type, CheckinWorkItemAction action) {
    if (action == CheckinWorkItemAction.None) {
      throw new IllegalArgumentException("Unexpected action type " + action);
    }

    List<ComputedColumn_type0> computedColumns = new ArrayList<ComputedColumn_type0>();

    computedColumns.add(createComputedColumn(WorkItemField.REVISED_DATE));
    computedColumns.add(createComputedColumn(WorkItemField.CHANGED_DATE));
    computedColumns.add(createComputedColumn(WorkItemField.PERSON_ID));

    if (CheckinWorkItemAction.Resolve.equals(action)) {
      computedColumns.add(createComputedColumn(WorkItemField.STATE_CHANGE_DATE));

      if (type == WorkItemType.Bug) {
        computedColumns.add(createComputedColumn(WorkItemField.RESOLVED_DATE));
      }
      else if (type == WorkItemType.Task) {
        computedColumns.add(createComputedColumn(WorkItemField.CLOSED_DATE));
      }
      else {
        throw new IllegalArgumentException("Unexpected work item type " + type);
      }
    }

    ComputedColumns_type0 computedColumnsArray = new ComputedColumns_type0();
    computedColumnsArray.setComputedColumn(computedColumns.toArray(new ComputedColumn_type0[computedColumns.size()]));
    return computedColumnsArray;
  }

  private static Column_type0 createColumn(WorkItemField field, String type, String value) {
    Column_type0 column = new Column_type0();
    column.setColumn(field.getSerialized());
    column.setType(type);
    column.setValue(value);
    return column;
  }

  private static ComputedColumn_type0 createComputedColumn(WorkItemField field) {
    ComputedColumn_type0 column = new ComputedColumn_type0();
    column.setColumn(field.getSerialized());
    return column;
  }

}
