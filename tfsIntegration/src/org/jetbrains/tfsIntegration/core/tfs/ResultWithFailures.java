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

package org.jetbrains.tfsIntegration.core.tfs;

import com.intellij.util.containers.ContainerUtil;
import com.microsoft.schemas.teamfoundation._2005._06.versioncontrol.clientservices._03.Failure;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

public class ResultWithFailures<T> {

  private final Collection<T> result = new ArrayList<T>();
  private final Collection<Failure> failures = new ArrayList<Failure>();

  public ResultWithFailures(@Nullable final T[] result, @Nullable final Failure[] failures) {
    if (result != null) {
      ContainerUtil.addAll(this.result, result);
    }
    if (failures != null) {
      ContainerUtil.addAll(this.failures, failures);
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

  public static <T> ResultWithFailures<T> merge(Collection<ResultWithFailures<T>> results) {
    ResultWithFailures<T> merged = new ResultWithFailures<T>();
    for (ResultWithFailures<T> r : results) {
      merged.getResult().addAll(r.getResult());
      merged.getFailures().addAll(r.getFailures());
    }
    return merged;
  }

}
