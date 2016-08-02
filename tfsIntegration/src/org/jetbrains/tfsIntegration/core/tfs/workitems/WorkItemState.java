package org.jetbrains.tfsIntegration.core.tfs.workitems;

import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * @author Konstantin Kolosovsky.
 */
public class WorkItemState {

  private static final Map<String, WorkItemState> ourAllStates = ContainerUtil.newHashMap();

  public static final WorkItemState ACTIVE = register("Active");
  public static final WorkItemState RESOLVED = register("Resolved");
  public static final WorkItemState CLOSED = register("Closed");

  @NonNls @NotNull private final String myName;

  private WorkItemState(@NonNls @NotNull String name) {
    myName = name;
  }

  @NotNull
  public String getName() {
    return myName;
  }

  @NotNull
  public static WorkItemState from(@NotNull String stateName) {
    return register(stateName);
  }

  @NotNull
  private static synchronized WorkItemState register(@NotNull String stateName) {
    WorkItemState result = ourAllStates.get(stateName);

    if (result == null) {
      result = new WorkItemState(stateName);
      ourAllStates.put(stateName, result);
    }

    return result;
  }

  @Override
  public String toString() {
    return myName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof WorkItemState)) return false;

    WorkItemState state = (WorkItemState)o;

    return myName.equals(state.myName);
  }

  @Override
  public int hashCode() {
    return myName.hashCode();
  }
}
