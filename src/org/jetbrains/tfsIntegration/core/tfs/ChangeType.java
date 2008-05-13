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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

public class ChangeType {

  public enum Value {
    None(1),
    Add(2),
    Edit(4),
    Encoding(8),
    Rename(16),
    Delete(32),
    Undelete(64),
    Branch(128),
    Merge(256),
    Lock(512);

    private int intValue;

    public int getIntValue() {
      return intValue;
    }

    Value(final int intValue) {
      this.intValue = intValue;
    }
  }

  private Set<Value> myValues;

  private ChangeType(Set<Value> values) {
    myValues = values;
  }

  public static ChangeType fromString(final String changeTypeString) {
    Set<Value> values = new HashSet<Value>();
    if (changeTypeString != null) {
      StringTokenizer tokenizer = new StringTokenizer(changeTypeString, " ");
      while (tokenizer.hasMoreTokens()) {
        String token = tokenizer.nextToken();
        values.add(Value.valueOf(token));
      }
    }
    return new ChangeType(values);
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Value value : myValues) {
      if (sb.length() > 0) {
        sb.append(" ");
      }
      sb.append(value.toString());
    }
    return sb.toString();
  }

  public boolean contains(final Value... values) {
    return myValues.containsAll(Arrays.asList(values));
  }

  public boolean containsOnly(final Value... values) {
    return myValues.size() == values.length && contains(values);
  }

  public boolean isEmpty() {
    return myValues.isEmpty();
  }

  public int size() {
    return myValues.size();
  }

}
