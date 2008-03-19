package org.jetbrains.tfsIntegration.exceptions;

import org.apache.axis2.AxisFault;

import javax.xml.namespace.QName;

public class IdentityNotFoundException extends TfsException {

  private static final long serialVersionUID = 1L;

  public static final String CODE = "IdentityNotFoundException";

  private final String myIdentityName;

  public IdentityNotFoundException(AxisFault cause) {
    super(cause);
    if (cause.getDetail() != null) {
      myIdentityName = cause.getDetail().getAttributeValue(new QName("IdentityName"));
    }
    else {
      myIdentityName = null;
    }
  }

  public String getIdentityName() {
    return myIdentityName;
  }

}
