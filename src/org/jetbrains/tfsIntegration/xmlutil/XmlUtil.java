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

package org.jetbrains.tfsIntegration.xmlutil;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;

public class XmlUtil {

  private static final Logger LOG = Logger.getInstance(XmlUtil.class);

  private XmlUtil() {
  }

  public static void parseFile(@NotNull File file, @Nullable DefaultHandler handler)
    throws IOException, ParserConfigurationException, SAXException {
    boolean parsed = false;
    InputStream stream = new BufferedInputStream(new FileInputStream(file));

    try {
      SAXParserFactory.newInstance().newSAXParser().parse(new InputSource(stream), handler);
      parsed = true;
    }
    finally {
      if (!parsed) {
        try {
          stream.close();
        }
        catch (Throwable t) {
          LOG.info(t);
        }
      }
      else {
        stream.close();
      }
    }
  }
}
