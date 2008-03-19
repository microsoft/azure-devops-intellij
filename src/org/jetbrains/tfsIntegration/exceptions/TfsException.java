package org.jetbrains.tfsIntegration.exceptions;

public abstract class TfsException extends Exception {

  private static final long serialVersionUID = 1L;

  public TfsException(final Throwable cause) {
    super(cause != null ? cause.getMessage() : null, cause);
  }

  public TfsException(final String message) {
    super(message);
  }

  public TfsException() {
    super();
  }

}
