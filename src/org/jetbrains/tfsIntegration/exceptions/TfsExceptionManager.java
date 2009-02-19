/*
 * Copyright 2000-2008 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.tfsIntegration.exceptions;

import org.apache.axiom.soap.SOAPFaultCode;
import org.apache.axiom.soap.SOAPFaultSubCode;
import org.apache.axiom.soap.SOAPFaultValue;
import org.apache.axiom.soap.SOAPProcessingException;
import org.apache.axis2.AxisFault;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NoHttpResponseException;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.SSLHandshakeException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

public class TfsExceptionManager {

  private static final String TRANSPORT_ERROR_MESSAGE = "Transport error: ";

  private static final Map<String, Class<?>> ourExceptionsBySubcodes;

  static {
    ourExceptionsBySubcodes = new HashMap<String, Class<?>>();
    ourExceptionsBySubcodes.put(WorkspaceNotFoundException.CODE, WorkspaceNotFoundException.class);
    ourExceptionsBySubcodes.put(IllegalIdentityException.CODE, IllegalIdentityException.class);
    ourExceptionsBySubcodes.put(IdentityNotFoundException.CODE, IdentityNotFoundException.class);
    ourExceptionsBySubcodes.put(ItemNotFoundException.CODE, ItemNotFoundException.class);
    ourExceptionsBySubcodes.put(InvalidPathException.CODE, InvalidPathException.class);
  }

  @Nullable
  private static TfsException createTfsExceptionFromThrowable(Throwable throwable) {
    if (throwable instanceof ConnectException) {
      return new ConnectionFailedException(throwable);
    }
    if (throwable instanceof UnknownHostException) {
      return new HostNotFoundException(throwable);
    }
    if (throwable instanceof NoHttpResponseException) {
      return new HostNotFoundException(throwable);
    }
    if (throwable instanceof SSLHandshakeException) {
      return new SSLConnectionException((SSLHandshakeException)throwable);
    }
    if (throwable instanceof SOAPProcessingException) {
      return new ConnectionFailedException("Invalid server response.");
    }
    if (throwable instanceof SocketTimeoutException) {
      return new ConnectionTimeoutException(throwable);
    }
    return null;
  }

  @Nullable
  private static TfsException createTfsExceptionFromAxisFault(AxisFault axisFault) {
    TfsException result = createTfsExceptionFromThrowable(axisFault.getCause());
    if (result != null) {
      return result;
    }

    SOAPFaultCode code = axisFault.getFaultCodeElement();
    if (code != null) {
      SOAPFaultSubCode subCode = code.getSubCode();
      if (subCode != null) {
        SOAPFaultValue subcodeValue = subCode.getValue();
        if (subcodeValue != null) {
          String subcodeText = subcodeValue.getText();
          if (subcodeText != null) {
            Class<?> exceptionClass = ourExceptionsBySubcodes.get(subcodeText);
            if (exceptionClass != null) {
              try {
                return (TfsException)exceptionClass.getConstructor(AxisFault.class).newInstance(axisFault);
              }
              catch (Exception e) {
                // skip
              }
            }
          }
        }
      }
    }

    // TODO: is there any way to recognize http transport errors?
    if (axisFault.getMessage() != null && axisFault.getMessage().startsWith(TRANSPORT_ERROR_MESSAGE)) {
      String[] tokens = axisFault.getMessage().substring(TRANSPORT_ERROR_MESSAGE.length()).split(" ");
      if (tokens.length > 0) {
        try {
          int errorCode = Integer.parseInt(tokens[0]);
          return createHttpTransportErrorException(errorCode, axisFault);
        }
        catch (NumberFormatException e) {
          // skip
        }
      }
    }
    return null;
  }

  public static TfsException processException(Exception e) {
    TfsException result;
    if (e instanceof AxisFault) {
      result = createTfsExceptionFromAxisFault((AxisFault)e);
    }
    else {
      result = createTfsExceptionFromThrowable(e);
    }
    if (e instanceof TfsException) {
      result = (TfsException)e;
    }
    if (result == null) {
      result = new UnknownException(e);
    }
    return result;
  }

  public static TfsException createHttpTransportErrorException(int errorCode, AxisFault axisFault) {
    switch (errorCode) {
      case HttpStatus.SC_UNAUTHORIZED:
        return new UnauthorizedException(axisFault);
      case HttpStatus.SC_BAD_GATEWAY:
        return new HostNotFoundException(axisFault);
      case HttpStatus.SC_NOT_FOUND:
        return new HostNotApplicableException(axisFault);
      case HttpStatus.SC_FORBIDDEN:
        return new ForbiddenException(axisFault);
      default:
        return new ConnectionFailedException(axisFault, errorCode);
    }
  }

}
