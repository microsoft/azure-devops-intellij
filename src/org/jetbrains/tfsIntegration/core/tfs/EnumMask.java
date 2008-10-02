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

// TODO use EnumSet
//public class EnumMask<T extends Enum<T>> {
//
//  public EnumMask(Class<? extends Enum<T>> enumClass, String maskString) {
//    final EnumSet<? extends Enum<T>> values = EnumSet.noneOf(enumClass);
//
//    if (maskString != null) {
//      StringTokenizer tokenizer = new StringTokenizer(maskString, " ");
//      while (tokenizer.hasMoreTokens()) {
//        String token = tokenizer.nextToken();
//        final Enum<T> value = Enum.valueOf(enumClass, token);
//        values.add(value);
//      }
//    }
//  }
//
//}

public class EnumMask<T extends Enum<T>> {

  private final Set<Enum<T>> myValues;

  private EnumMask(Class<T> enumClass, String maskString) {
    myValues = new HashSet<Enum<T>>();

    if (maskString != null) {
      StringTokenizer tokenizer = new StringTokenizer(maskString, " ");
      while (tokenizer.hasMoreTokens()) {
        String token = tokenizer.nextToken();
        final Enum<T> value = Enum.valueOf(enumClass, token);
        myValues.add(value);
      }
    }
  }

  public static <T extends Enum<T>> EnumMask<T> fromString(Class<T> enumClass, String maskString) {
    return new EnumMask<T>(enumClass, maskString);
  }

  public boolean contains(final Enum<T>... values) {
    return myValues.containsAll(Arrays.asList(values));
  }

  public boolean containsAny(final Enum<T>... values) {
    for (Enum<T> value : values) {
      if (myValues.contains(value)) {
        return true;
      }
    }
    return false;
  }

  public boolean containsOnly(final Enum<T>... values) {
    return myValues.size() == values.length && contains(values);
  }

  public boolean isEmpty() {
    return myValues.isEmpty();
  }

  public int size() {
    return myValues.size();
  }

  public void remove(final Enum<T>... values) {
    for (Enum<T> value : values) {
      myValues.remove(value);
    }
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Enum<T> value : myValues) {
      if (sb.length() > 0) {
        sb.append(" ");
      }
      sb.append(value.toString());
    }
    return sb.toString();
  }

}
