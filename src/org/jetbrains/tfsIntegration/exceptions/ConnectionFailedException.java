package org.jetbrains.tfsIntegration.exceptions;

import org.apache.axis2.AxisFault;

public class ConnectionFailedException extends TfsException {

  private static final long serialVersionUID = 1L;

  private static final int CODE_UNDEFINED = 0;

  private final int myHttpStatusCode;

  public ConnectionFailedException(AxisFault cause, int httpStatusCode) {
    super(cause);
    myHttpStatusCode = httpStatusCode;
  }

  public ConnectionFailedException(AxisFault cause) {
    this(cause, CODE_UNDEFINED);
  }

  public int getHttpStatusCode() {
    return myHttpStatusCode;
  }

}
