package org.jetbrains.tfsIntegration.ui;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.checkin.CheckinParameters;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;
import org.jetbrains.tfsIntegration.core.tfs.TfsExecutionUtil;

public interface QueriesTreeContext {

  @NotNull
  CheckinParameters getState();

  @NotNull
  ServerInfo getServer();

  @NotNull
  TFSTeamProjectCollection getProjectCollection();

  void queryWorkItems(@NotNull TfsExecutionUtil.Process<WorkItemsQueryResult> query);
}
