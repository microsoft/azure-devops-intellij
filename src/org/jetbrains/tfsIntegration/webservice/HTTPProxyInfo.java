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

package org.jetbrains.tfsIntegration.webservice;

import com.intellij.util.net.HttpConfigurable;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.configuration.TFSConfigurationManager;

import java.net.Authenticator;

class HTTPProxyInfo {

  public final @Nullable String host;
  public final int port;
  public final @Nullable String user;
  public final @Nullable String password;

  private HTTPProxyInfo(final @Nullable String host, final int port, final @Nullable String user, final @Nullable String password) {
    this.host = host;
    this.port = port;
    this.user = user;
    this.password = password;
  }

  public static boolean shouldPromptForPassword() {
    if (TFSConfigurationManager.getInstance().useIdeaHttpProxy()) {
      final HttpConfigurable hc = HttpConfigurable.getInstance();
      return hc.USE_HTTP_PROXY && hc.PROXY_AUTHENTICATION && !hc.KEEP_PROXY_PASSWORD;
    }
    return false;
  }

  public static void promptForPassword() {
    final HttpConfigurable hc = HttpConfigurable.getInstance();
    hc.getPromptedAuthentication(hc.PROXY_HOST, "Proxy authentication");
  }

  public static HTTPProxyInfo getCurrent() {
    // axis will override the higher-level settings with system properties, so explicitly clear them
    System.clearProperty("http.proxyHost");
    System.clearProperty("http.proxyPort");
    Authenticator.setDefault(null);

    if (TFSConfigurationManager.getInstance().useIdeaHttpProxy()) {
      final HttpConfigurable hc = HttpConfigurable.getInstance();
      if (hc.USE_HTTP_PROXY) {
        if (hc.PROXY_AUTHENTICATION) {
          // here we assume proxy auth dialog was shown if needed, see promptForPassword() caller
          return new HTTPProxyInfo(hc.PROXY_HOST, hc.PROXY_PORT, hc.PROXY_LOGIN, hc.getPlainProxyPassword());
        }
        else {
          return new HTTPProxyInfo(hc.PROXY_HOST, hc.PROXY_PORT, null, null);
        }
      }
    }
    return new HTTPProxyInfo(null, -1, null, null);
  }
}
