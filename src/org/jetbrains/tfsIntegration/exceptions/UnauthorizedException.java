package org.jetbrains.tfsIntegration.exceptions;

import org.apache.axis2.AxisFault;

public class UnauthorizedException extends TfsException {

  private static final long serialVersionUID = 1L;

  public UnauthorizedException(String message) {
    super(message);
  }

  public UnauthorizedException(AxisFault cause) {
    super(cause);
  }

}
