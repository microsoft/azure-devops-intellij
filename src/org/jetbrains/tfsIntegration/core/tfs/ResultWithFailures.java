package org.jetbrains.tfsIntegration.core.tfs;

import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.Failure;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;

public class ResultWithFailures<T> {

  private Collection<T> result = new ArrayList<T>();
  private Collection<Failure> failures = new ArrayList<Failure>();

  public ResultWithFailures(T[] result, final Failure[] failures) {
    this.result.addAll(Arrays.asList(result));
    this.failures.addAll(Arrays.asList(failures));
  }

  public ResultWithFailures() {}

  public Collection<T> getResult() {
    return result;
  }

  public Collection<Failure> getFailures() {
    return failures;
  }

}
