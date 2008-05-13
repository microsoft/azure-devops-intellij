package org.jetbrains.tfsIntegration.core.tfs.version;

import org.apache.axiom.om.OMFactory;
import org.apache.axis2.databinding.utils.writer.MTOMAwareXMLStreamWriter;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

public class LabelVersionSpec extends VersionSpecBase {

    private String myLabel;
    private String myScope;

    public LabelVersionSpec(String myLabel, String myScope) {
        this.myLabel = myLabel;
        this.myScope = myScope;
    }

    protected void writeAttributes(QName parentQName, OMFactory factory, MTOMAwareXMLStreamWriter xmlWriter) throws XMLStreamException {
        writeVersionAttribute("", "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance", xmlWriter);
        writeVersionAttribute("", "xsi:type", "LabelVersionSpec", xmlWriter);
        writeVersionAttribute("", "label", myLabel, xmlWriter);
        if (myScope != null) {
            writeVersionAttribute("", "scope", myScope, xmlWriter);
        }
    }

    public String getLabel() {
        return myLabel;
    }

    public String getScope() {
        return myScope;
    }

    public static String getLabel(String labelString) {
        int atPos = labelString.indexOf('@');
        if (atPos != -1) {
            return labelString.substring(0, atPos);
        }
        return labelString;
    }

    public static String getScope(String labelString) {
        int atPos = labelString.indexOf('@');
        if (atPos != -1) {
            return labelString.substring(atPos + 1);
        }
        return null;
    }
}
