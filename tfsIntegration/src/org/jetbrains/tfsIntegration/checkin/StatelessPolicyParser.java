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

import jcifs.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class StatelessPolicyParser {

  public static List<PolicyDescriptor> parseDescriptors(String input) throws PolicyParseException {
    byte[] data = Base64.decode(input);

    ByteArrayInputStream is = new ByteArrayInputStream(data);
    List<PolicyDescriptor> descriptors = new ArrayList<PolicyDescriptor>();
    while (is.available() > 0) {
      try {
        String name = readString(is);
        String className = readString(is);
        readString(is); // assembly name
        String installationInstructions = readString(is);
        boolean enabled = readBoolean(is);
        int length = readInt32(is);
        readBytes(is, length); // object data

        PolicyType policyType = new PolicyType(className, name, "", installationInstructions);
        descriptors.add(new PolicyDescriptor(policyType, enabled));
      }
      catch (IOException e) {
        throw new PolicyParseException("Unexpected end of data stream");
      }
    }
    return descriptors;
  }

  private static boolean readBoolean(InputStream is) throws PolicyParseException, IOException {
    int i = is.read();
    if (i == -1) {
      throw new PolicyParseException("Unexpected end of data stream");
    }
    return i != 0;
  }

  private static String readString(InputStream is) throws PolicyParseException, IOException {
    int length = read7BitEncodedInt(is);
    byte[] buf = readBytes(is, length);
    return new String(buf, 0, length);
  }

  private static byte[] readBytes(InputStream is, int len) throws PolicyParseException, IOException {
    byte[] buf = new byte[len];
    if (is.read(buf, 0, len) != len) {
      throw new PolicyParseException("Unexpected end of data stream");
    }

    return buf;
  }

  private static int read7BitEncodedInt(InputStream is) throws PolicyParseException, IOException {
    int num3;
    int num = 0;
    int num2 = 0;
    do {
      if (num2 == 0x23) {
        throw new PolicyParseException("Unexpected end of data stream");
      }
      num3 = is.read();
      if (num3 == -1) {
        throw new PolicyParseException("Unexpected end of data stream");
      }
      num |= (num3 & 0x7f) << num2;
      num2 += 7;
    }
    while ((num3 & 0x80) != 0);
    return num;
  }

  private static int readInt32(InputStream is) throws PolicyParseException, IOException {
    byte[] buf = new byte[4];
    if (is.read(buf, 0, 4) != 4) {
      throw new PolicyParseException("Unexpected end of data stream");
    }

    int i = (buf[0] & 0xFF);
    i = i | (buf[1] & 0xFF) << 8;
    i = i | (buf[2] & 0xFF) << 16;
    i = i | (buf[3] & 0xFF) << 24;

    return i;
  }
}
