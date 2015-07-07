package org.jetbrains.tfsIntegration.ui;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.checkin.CheckinParameters;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;
import org.jetbrains.tfsIntegration.core.tfs.TfsExecutionUtil;
import org.jetbrains.tfsIntegration.core.tfs.workitems.WorkItem;

import java.util.List;

public interface QueriesTreeContext {

  @NotNull
  CheckinParameters getState();

  @NotNull
  ServerInfo getServer();

  @NotNull
  TFSTeamProjectCollection getProjectCollection();

  void queryWorkItems(@NotNull TfsExecutionUtil.Process<List<WorkItem>> query);
}
