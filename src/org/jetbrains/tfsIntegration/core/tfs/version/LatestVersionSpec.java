package org.jetbrains.tfsIntegration.core.tfs.version;

import org.apache.axiom.om.OMFactory;
import org.apache.axis2.databinding.utils.writer.MTOMAwareXMLStreamWriter;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

public class LatestVersionSpec extends VersionSpecBase {

  public static final LatestVersionSpec INSTANCE = new LatestVersionSpec(); 

  private LatestVersionSpec() {}

  protected void writeAttributes(final QName parentQName, final OMFactory factory, final MTOMAwareXMLStreamWriter xmlWriter) throws
                                                                                                                             XMLStreamException {
    writeVersionAttribute("", "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance", xmlWriter);
    writeVersionAttribute("", "xsi:type", "LatestVersionSpec", xmlWriter);
  }
}
