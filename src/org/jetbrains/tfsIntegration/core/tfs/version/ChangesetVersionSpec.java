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
  private int changeSetId;

    public int getChangeSetId() {
        return changeSetId;
    }

    public ChangesetVersionSpec(final int changeSetId) {
    this.changeSetId = changeSetId;
  }

  protected void writeAttributes(final QName parentQName, final OMFactory factory, final MTOMAwareXMLStreamWriter xmlWriter)
    throws XMLStreamException {
    writeVersionAttribute("", "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance", xmlWriter);
    writeVersionAttribute("", "xsi:type", "ChangesetVersionSpec", xmlWriter);
    writeVersionAttribute("", "cs", Integer.toString(changeSetId), xmlWriter);
  }
}
