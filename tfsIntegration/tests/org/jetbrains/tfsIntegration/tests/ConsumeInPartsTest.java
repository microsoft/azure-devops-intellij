/*
 * Copyright 2000-2008 JetBrains s.r.o.
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

package org.jetbrains.tfsIntegration.tests;

import com.intellij.util.ArrayUtil;
import com.intellij.util.ThrowableConsumer;
import gnu.trove.Equality;
import junit.framework.TestCase;
import org.jetbrains.tfsIntegration.core.tfs.TfsUtil;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ConsumeInPartsTest extends TestCase {

  private static class TestConsumer implements ThrowableConsumer<List<String>, RuntimeException> {
    private final List<String[]> myResults = new ArrayList<String[]>();

    public void consume(List<String> strings) {
      myResults.add(ArrayUtil.toStringArray(strings));
    }

    public void assertEquals(String[][] expected) {
      String[][] actual = myResults.toArray(new String[myResults.size()][]);
      boolean equals = ArrayUtil.equals(actual, expected, new Equality<String[]>() {
        public boolean equals(String[] o1, String[] o2) {
          return Arrays.equals(o1, o2);
        }
      });

      StringBuilder message = new StringBuilder("Expected: ");
      toString(expected, message);
      message.append("\nActual: ");
      toString(actual, message);
      Assert.assertTrue(message.toString(), equals);
    }

    private void toString(String[][] data, StringBuilder s) {
      for (String[] item : data) {
        s.append("{");
        boolean first = true;
        for (String i : item) {
          if (!first) {
            s.append(",");
          }
          first = false;
          s.append(i);
        }
        s.append("}");
      }
    }
  }

  public void testEmpty() {
    TestConsumer consumer = new TestConsumer();
    List<String> items = Arrays.asList();

    TfsUtil.consumeInParts(items, 5, consumer);
    consumer.assertEquals(new String[][]{});
  }

  public void test1() {
    TestConsumer consumer = new TestConsumer();
    List<String> items = Arrays.asList("a");

    TfsUtil.consumeInParts(items, 5, consumer);
    consumer.assertEquals(new String[][]{{"a"}});
  }

  public void test2() {
    TestConsumer consumer = new TestConsumer();
    List<String> items = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");

    TfsUtil.consumeInParts(items, 5, consumer);
    consumer.assertEquals(new String[][]{{"1", "2", "3", "4", "5"}, {"6", "7", "8", "9", "10"}});
  }

  public void test3() {
    TestConsumer consumer = new TestConsumer();
    List<String> items = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11");

    TfsUtil.consumeInParts(items, 5, consumer);
    consumer.assertEquals(new String[][]{{"1", "2", "3", "4", "5"}, {"6", "7", "8", "9", "10"}, {"11"}});
  }

  public void test4() {
    TestConsumer consumer = new TestConsumer();
    List<String> items = Arrays.asList("1", "2", "3", "4", "5");

    TfsUtil.consumeInParts(items, 10, consumer);
    consumer.assertEquals(new String[][]{{"1", "2", "3", "4", "5"}});
  }

  public void test5() {
      TestConsumer consumer = new TestConsumer();
      List<String> items = Arrays.asList("1", "2", "3", "4", "5");

      TfsUtil.consumeInParts(items, 1, consumer);
      consumer.assertEquals(new String[][]{{"1"}, {"2"}, {"3"}, {"4"}, {"5"}});
    }

}
