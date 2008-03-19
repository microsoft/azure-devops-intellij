package org.jetbrains.tfsIntegration.exceptions;

public class UnknownException extends TfsException {

  public UnknownException(Exception e) {
    super(e);
  }

  private static final long serialVersionUID = 1L;

}
