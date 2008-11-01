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

package org.jetbrains.tfsIntegration.core.tfs;

import com.intellij.util.net.HttpConfigurable;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.configuration.TFSConfigurationManager;

public class HTTPProxyInfo {

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

  public static HTTPProxyInfo getCurrent() {
    if (TFSConfigurationManager.getInstance().useIdeaHttpProxy()) {
      final HttpConfigurable httpConfigurable = HttpConfigurable.getInstance();
      if (httpConfigurable.USE_HTTP_PROXY) {
        if (httpConfigurable.PROXY_AUTHENTICATION) {
          return new HTTPProxyInfo(httpConfigurable.PROXY_HOST, httpConfigurable.PROXY_PORT, httpConfigurable.PROXY_LOGIN,
                                   httpConfigurable.getPlainProxyPassword());
        }
        else {
          return new HTTPProxyInfo(httpConfigurable.PROXY_HOST, httpConfigurable.PROXY_PORT, null, null);
        }
      }
    }

    return new HTTPProxyInfo(null, -1, null, null);
  }
}
