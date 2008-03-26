package org.jetbrains.tfsIntegration.exceptions;

import java.net.UnknownHostException;
import java.rmi.RemoteException;

// TODO: determine the correct error message for each type of exception

public class WebServiceException extends Exception {

  public WebServiceException(String message) {
    super(message);
  }


  public WebServiceException(Exception exception) {
    super(exception);
  }

  public WebServiceException(RemoteException e) {
    super(e);
  }

  public WebServiceException(String message, UnknownHostException cause) {
    super(message, cause);
  }
}
