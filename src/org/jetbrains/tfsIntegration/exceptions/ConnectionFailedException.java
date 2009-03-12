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

public class ConnectionFailedException extends TfsException {

  private static final long serialVersionUID = 1L;

  private static final int CODE_UNDEFINED = 0;

  private final int myHttpStatusCode;
  private final String myMessage;

  public ConnectionFailedException(Throwable cause, int httpStatusCode) {
    super(cause);
    myHttpStatusCode = httpStatusCode;
    myMessage = null;
  }

  public ConnectionFailedException(Throwable cause) {
    this(cause, CODE_UNDEFINED);
  }

  public ConnectionFailedException(String message) {
    super((Throwable)null);
    myHttpStatusCode = CODE_UNDEFINED;
    myMessage = message;
  }

  public ConnectionFailedException(Throwable cause, String message) {
    super(cause);
    myHttpStatusCode = CODE_UNDEFINED;
    myMessage = message;
  }

  public int getHttpStatusCode() {
    return myHttpStatusCode;
  }

  public String getMessage() {
    return myMessage != null ? myMessage : super.getMessage();
  }
}
