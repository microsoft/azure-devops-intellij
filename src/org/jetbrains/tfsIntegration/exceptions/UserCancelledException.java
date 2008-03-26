package org.jetbrains.tfsIntegration.exceptions;

public class UserCancelledException extends TfsException {

  public String getMessage() {
    return "Operation cancelled by user";
  }

}
