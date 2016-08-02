package org.jetbrains.tfsIntegration.exceptions;

import org.jetbrains.tfsIntegration.core.TFSBundle;
import org.jetbrains.tfsIntegration.core.tfs.TfsUtil;

import java.net.URI;

public class AuthCancelledException extends UserCancelledException {

  private final URI myServerUri;

  public AuthCancelledException(URI serverUri) {
    myServerUri = serverUri;
  }

  @Override
  public String getMessage() {
    return TFSBundle.message("authentication.canceled", TfsUtil.getPresentableUri(myServerUri));
  }
}
