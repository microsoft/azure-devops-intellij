package org.jetbrains.tfsIntegration.webservice.compatibility;

import org.apache.axiom.om.OMXMLParserWrapper;
import org.apache.axiom.soap.*;
import org.apache.axiom.soap.impl.llom.soap12.SOAP12Factory;
import org.apache.axiom.soap.impl.llom.soap12.SOAP12FaultImpl;

import javax.xml.namespace.QName;

public class CustomSOAP12Factory extends SOAP12Factory {

  @Override
  public SOAPFault createSOAPFault(SOAPBody parent, Exception e) throws SOAPProcessingException {
    return new CustomSOAP12FaultImpl(parent, e, this);
  }

  @Override
  public SOAPFault createSOAPFault(SOAPBody parent) throws SOAPProcessingException {
    return new CustomSOAP12FaultImpl(parent, this);
  }

  @Override
  public SOAPFault createSOAPFault() throws SOAPProcessingException {
    return new CustomSOAP12FaultImpl(this);
  }

  @Override
  public SOAPFault createSOAPFault(SOAPBody parent, OMXMLParserWrapper builder) {
    return new CustomSOAP12FaultImpl(parent, builder, this);
  }

  private static class CustomSOAP12FaultImpl extends SOAP12FaultImpl {

    public CustomSOAP12FaultImpl(SOAPFactory factory) {
      super(factory);
    }

    public CustomSOAP12FaultImpl(SOAPBody parent, Exception e, SOAPFactory factory) throws SOAPProcessingException {
      super(parent, e, factory);
    }

    public CustomSOAP12FaultImpl(SOAPBody parent, OMXMLParserWrapper builder, SOAPFactory factory) {
      super(parent, builder, factory);
    }

    public CustomSOAP12FaultImpl(SOAPBody parent, SOAPFactory factory) throws SOAPProcessingException {
      super(parent, factory);
    }

    @Override
    public SOAPFaultDetail getDetail() {
      SOAPFaultDetail detail = super.getDetail();

      if (detail == null) {
        // try with no namespace
        detail = (SOAPFaultDetail)getFirstChildWithName(new QName(
          SOAP12Constants.SOAP_FAULT_DETAIL_LOCAL_NAME));
      }

      if (detail != null) {
        // Axis takes the first child and ignores attributes -> make
        // a copy and append it as a child
        detail.addChild(detail.cloneOMElement());
      }

      return detail;
    }
  }

}
