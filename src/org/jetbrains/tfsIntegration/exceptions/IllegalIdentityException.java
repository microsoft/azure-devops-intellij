package org.jetbrains.tfsIntegration.exceptions;

import org.apache.axis2.AxisFault;

import javax.xml.namespace.QName;

public class IllegalIdentityException extends TfsException {

  private static final long serialVersionUID = 1L;

  public static final String CODE = "IllegalIdentityException";

  private final String myIdentityName;

  public IllegalIdentityException(AxisFault cause) {
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
