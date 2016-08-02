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

import org.jetbrains.annotations.NonNls;

public enum WorkItemField {
  
  ASSIGNED_TO("System.AssignedTo"),
  STATE("System.State"),
  ID("System.Id"),
  TITLE("System.Title"),
  REVISION("System.Rev"),
  TYPE("System.WorkItemType"),
  REASON("System.Reason"),
  STATE_CHANGE_DATE("Microsoft.VSTS.Common.StateChangeDate"),
  RESOLVED_DATE("Microsoft.VSTS.Common.ResolvedDate"),
  RESOLVED_BY("Microsoft.VSTS.Common.ResolvedBy"),
  RESOLVED_REASON("Microsoft.VSTS.Common.ResolvedReason"),
  CLOSED_DATE("Microsoft.VSTS.Common.ClosedDate"),
  CLOSED_BY("Microsoft.VSTS.Common.ClosedBy"),
  REVISED_DATE("System.RevisedDate"),
  CHANGED_DATE("System.ChangedDate"),
  PERSON_ID("System.PersonId"),
  BISLINKS("System.BISLinks"),
  HISTORY("System.History");


  private @NonNls final String mySerialized;

  private WorkItemField(@NonNls String serialized) {
    mySerialized = serialized;
  }

  public String getSerialized() {
    return mySerialized;
  }
}
