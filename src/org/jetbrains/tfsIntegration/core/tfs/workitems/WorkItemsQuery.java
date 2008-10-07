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

import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.workitemtracking.clientservices.*;

import java.util.List;

public enum WorkItemsQuery {
  AllMyActive("All My Active Work Items") {
    public List<WorkItem> queryWorkItems(final ServerInfo server) throws TfsException {
      Expression_type0 expression1 = new Expression_type0();
      expression1.setColumn(WorkItemField.ASSIGNED_TO.getSerialized());
      expression1.setOperator(OperatorType.equals);
      expression1.setString(server.getVCS().readIdentity(server.getQualifiedUsername()).getDisplayName());

      Expression_type0 expression2 = new Expression_type0();
      expression2.setColumn(WorkItemField.STATE.getSerialized());
      expression2.setOperator(OperatorType.equals);
      expression2.setString(WorkItem.WorkItemState.Active.toString());

      GroupType groupType = new GroupType();
      groupType.setGroupOperator(GroupOperatorType.And);
      groupType.setExpression(new Expression_type0[]{expression1, expression2});

      Query_type01 query_type01 = new Query_type01();
      query_type01.setGroup(groupType);

      return queryWorkItems(server, query_type01);
    }},

  AllMy("All My Work Items") {
    public List<WorkItem> queryWorkItems(final ServerInfo server) throws TfsException {
      Expression_type0 expression1 = new Expression_type0();
      expression1.setColumn(WorkItemField.ASSIGNED_TO.getSerialized());
      expression1.setOperator(OperatorType.equals);
      expression1.setString(server.getVCS().readIdentity(server.getQualifiedUsername()).getDisplayName());

      Query_type01 query_type01 = new Query_type01();
      query_type01.setExpression(expression1);

      return queryWorkItems(server, query_type01);
    }},

  AllActive("All Active Work Items") {
    public List<WorkItem> queryWorkItems(final ServerInfo server) throws TfsException {
      Expression_type0 expression1 = new Expression_type0();
      expression1.setColumn(WorkItemField.STATE.getSerialized());
      expression1.setOperator(OperatorType.equals);
      expression1.setString(WorkItem.WorkItemState.Active.name());

      Query_type01 query_type01 = new Query_type01();
      query_type01.setExpression(expression1);

      return queryWorkItems(server, query_type01);
    }},

  All("All Work Items") {
    public List<WorkItem> queryWorkItems(ServerInfo server) throws TfsException {
      Expression_type0 expression1 = new Expression_type0();
      expression1.setColumn(WorkItemField.ID.getSerialized());
      expression1.setOperator(OperatorType.equalsGreater);
      expression1.setNumber(0);

      Query_type01 query_type01 = new Query_type01();
      query_type01.setExpression(expression1);

      return queryWorkItems(server, query_type01);
    }};

  private final String myName;

  WorkItemsQuery(String name) {
    myName = name;
  }

  public String toString() {
    return myName;
  }

  public abstract List<WorkItem> queryWorkItems(ServerInfo server) throws TfsException;

  protected static List<WorkItem> queryWorkItems(ServerInfo server, Query_type01 query_type01) throws TfsException {
    return server.getVCS().queryWorkItems(query_type01);
  }
}
