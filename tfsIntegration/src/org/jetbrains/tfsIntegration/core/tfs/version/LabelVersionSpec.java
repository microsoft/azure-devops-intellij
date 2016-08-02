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

package org.jetbrains.tfsIntegration.core.tfs.version;

import org.apache.axiom.om.OMFactory;
import org.apache.axis2.databinding.utils.writer.MTOMAwareXMLStreamWriter;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

public class LabelVersionSpec extends VersionSpecBase {

  private final String myLabel;
  private final String myScope;

  public LabelVersionSpec(String label, String scope) {
    myLabel = label;
    myScope = scope;
  }

  protected void writeAttributes(QName parentQName, OMFactory factory, MTOMAwareXMLStreamWriter xmlWriter) throws XMLStreamException {
    writeVersionAttribute("", "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance", xmlWriter);
    writeVersionAttribute("", "xsi:type", "LabelVersionSpec", xmlWriter);
    writeVersionAttribute("", "label", myLabel, xmlWriter);
    if (myScope != null) {
      writeVersionAttribute("", "scope", myScope, xmlWriter);
    }
  }

  public String getPresentableString() {
    return myScope != null ? myLabel + "@" + myScope : myLabel;
  }

  public String getLabel() {
    return myLabel;
  }

  public String getScope() {
    return myScope;
  }

  public static LabelVersionSpec fromStringRepresentation(String stringRepresentation) {
    int atPos = stringRepresentation.indexOf('@');
    if (atPos != -1) {
      return new LabelVersionSpec(stringRepresentation.substring(0, atPos), stringRepresentation.substring(atPos + 1));
    }
    else {
      return new LabelVersionSpec(stringRepresentation, null);
    }
  }

}
