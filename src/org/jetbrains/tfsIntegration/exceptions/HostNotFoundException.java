package org.jetbrains.tfsIntegration.exceptions;

import org.apache.axis2.AxisFault;

public class HostNotFoundException extends TfsException {

  private static final long serialVersionUID = 1L;

  public HostNotFoundException(AxisFault cause) {
    super(cause);
  }

  @Override
  public String getMessage() {
    return "Specified host not found";
  }

}
