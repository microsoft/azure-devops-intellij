package org.jetbrains.tfsIntegration.core;

import com.intellij.openapi.vcs.CheckoutProvider;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NonNls;

/**
 * Created by IntelliJ IDEA.
 * Date: 10.03.2008
 * Time: 20:33:27
 */
public class TFSCheckoutProvider implements CheckoutProvider {
  public void doCheckout(@Nullable final Listener listener) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @NonNls
  public String getVcsName() {
    return TFSVcs.TFS_NAME;
  }
}
