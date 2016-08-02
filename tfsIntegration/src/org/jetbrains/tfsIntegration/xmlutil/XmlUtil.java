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
import com.intellij.util.ThrowableRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import static org.jetbrains.tfsIntegration.core.tfs.TfsUtil.forcePluginClassLoader;

public class XmlUtil {

  private static final Logger LOG = Logger.getInstance(XmlUtil.class);

  private XmlUtil() {
  }

  public static void parseFile(@NotNull File file, @Nullable final DefaultHandler handler) throws Exception {
    boolean parsed = false;
    final InputStream stream = new BufferedInputStream(new FileInputStream(file));

    try {
      forcePluginClassLoader(new ThrowableRunnable<Exception>() {
        @Override
        public void run() throws Exception {
          SAXParserFactory.newInstance().newSAXParser().parse(new InputSource(stream), handler);
        }
      });
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
