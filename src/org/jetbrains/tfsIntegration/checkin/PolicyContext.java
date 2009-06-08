/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.jetbrains.tfsIntegration.checkin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.FilePath;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItem;

import java.util.Collection;
import java.util.Map;

/**
 * This interface encapsulates the state of check in
 */
public interface PolicyContext {

  enum WorkItemAction {
    Associate, Resolve
  }

  /**
   * @return set of files that are changed (created, modified, deleted, moved)
   */
  public Collection<FilePath> getFiles();

  /**
   * @return current project
   */
  public Project getProject();

  /**
   * @return commit message
   */
  public String getCommitMessage();

  /**
   * @return associated work items
   */
  public Map<WorkItem, WorkItemAction> getWorkItems();

}