// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.exceptions;

public class TfsException extends Exception {

  private static final long serialVersionUID = 1L;

  public TfsException(final String message, final Throwable cause) {
    super(message, cause);
  }

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
