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
import org.apache.axis2.databinding.utils.BeanUtil;
import org.apache.axis2.databinding.utils.writer.MTOMAwareXMLStreamWriter;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.VersionSpec;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public abstract class VersionSpecBase extends VersionSpec {

  public void serialize(final QName parentQName, final OMFactory factory, final MTOMAwareXMLStreamWriter xmlWriter)
    throws XMLStreamException {

    String prefix;
    String namespace;

    prefix = parentQName.getPrefix();
    namespace = parentQName.getNamespaceURI();

    if (namespace != null) {
      String writerPrefix = xmlWriter.getPrefix(namespace);

      if (writerPrefix != null) {
        xmlWriter.writeStartElement(namespace, parentQName.getLocalPart());
      }
      else {
        if (prefix == null) {
          prefix = genPrefix(namespace);
        }

        xmlWriter.writeStartElement(prefix, parentQName.getLocalPart(), namespace);
        xmlWriter.writeNamespace(prefix, namespace);
        xmlWriter.setPrefix(prefix, namespace);
      }
    }
    else {
      xmlWriter.writeStartElement(parentQName.getLocalPart());
    }
    writeAttributes(parentQName, factory, xmlWriter);
    xmlWriter.writeEndElement();
  }

  private static String genPrefix(String namespace) {
    if (namespace.equals("http://schemas.microsoft.com/TeamFoundation/2005/06/VersionControl/ClientServices/03")) {
      return "";
    }

    return BeanUtil.getUniquePrefix();
  }

  protected abstract void writeAttributes(final QName parentQName, final OMFactory factory, final MTOMAwareXMLStreamWriter xmlWriter)
    throws XMLStreamException;

  /**
   * Util method to write an attribute without the ns prefix
   */
  protected static void writeVersionAttribute(String namespace, String attName, String attValue, XMLStreamWriter xmlWriter)
    throws XMLStreamException {
    if (namespace.length() == 0) {
      xmlWriter.writeAttribute(attName, attValue);
    }
    else {
      regPrefix(xmlWriter, namespace);
      xmlWriter.writeAttribute(namespace, attName, attValue);
    }
  }

  /**
   * Register a namespace prefix
   */
  private static String regPrefix(XMLStreamWriter xmlWriter, String namespace) throws XMLStreamException {
    String prefix = xmlWriter.getPrefix(namespace);

    if (prefix == null) {
      prefix = genPrefix(namespace);

      while (xmlWriter.getNamespaceContext().getNamespaceURI(prefix) != null) {
        prefix = BeanUtil.getUniquePrefix();
      }

      xmlWriter.writeNamespace(prefix, namespace);
      xmlWriter.setPrefix(prefix, namespace);
    }

    return prefix;
  }

}
