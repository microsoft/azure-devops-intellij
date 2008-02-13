package org.jetbrains.tfsIntegration.core.tfs.version;

import org.apache.axiom.om.OMFactory;
import org.apache.axis2.databinding.utils.BeanUtil;
import org.apache.axis2.databinding.utils.writer.MTOMAwareXMLStreamWriter;
import org.jetbrains.tfsIntegration.stubs.versioncontrol.repository.VersionSpec;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Created by IntelliJ IDEA.
 * Date: 08.02.2008
 * Time: 13:51:25
 */
public abstract class VersionSpecBase extends VersionSpec {
  private static VersionSpec latestVersionSpec = new LatestVersionSpec();

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

  public static VersionSpec getLatest() {
    return latestVersionSpec;
  }
}
