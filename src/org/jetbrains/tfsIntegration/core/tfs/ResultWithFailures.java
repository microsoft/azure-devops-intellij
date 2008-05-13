package org.jetbrains.tfsIntegration.core.tfs;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Failure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class ResultWithFailures<T> {

  private final Collection<T> result = new ArrayList<T>();
  private final Collection<Failure> failures = new ArrayList<Failure>();

  public ResultWithFailures(@Nullable final T[] result, @Nullable final Failure[] failures) {
    if (result != null) {
      this.result.addAll(Arrays.asList(result));
    }
    if (failures != null) {
      this.failures.addAll(Arrays.asList(failures));
    }
  }

  public ResultWithFailures() {
  }

  public Collection<T> getResult() {
    return result;
  }

  public Collection<Failure> getFailures() {
    return failures;
  }

}
