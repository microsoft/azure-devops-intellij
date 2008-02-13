package org.jetbrains.tfsIntegration.core.tfs.version;

import org.apache.axiom.om.OMFactory;
import org.apache.axis2.databinding.utils.writer.MTOMAwareXMLStreamWriter;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

/**
 * Created by IntelliJ IDEA.
 * Date: 05.02.2008
 * Time: 3:05:06
 */
public class ChangesetVersionSpec extends VersionSpecBase {
  public ChangesetVersionSpec(final int changeSetId) {
    // todo: implement or drop
  }

  protected void writeAttributes(final QName parentQName, final OMFactory factory, final MTOMAwareXMLStreamWriter xmlWriter)
    throws XMLStreamException {
    //To change body of implemented methods use File | Settings | File Templates.
  }
}
