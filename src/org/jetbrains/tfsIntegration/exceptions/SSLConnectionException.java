package org.jetbrains.tfsIntegration.exceptions;

import javax.net.ssl.SSLHandshakeException;

public class SSLConnectionException extends TfsException {

  public SSLConnectionException(final SSLHandshakeException cause) {
    super(cause);
  }

  public String getMessage() {
    Throwable cause = this;
    while (cause.getCause() != null && cause.getCause() != cause) {
      cause = cause.getCause();
    }
    return "SSL connection failed: " + cause.getMessage();
  }
}
