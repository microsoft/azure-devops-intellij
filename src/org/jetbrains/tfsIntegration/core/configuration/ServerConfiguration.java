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

package org.jetbrains.tfsIntegration.core.configuration;

import com.intellij.util.xmlb.annotations.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Tag(value = "configuration")
public class ServerConfiguration {

  private @Nullable Credentials myCredentials;

  private @Nullable String myProxyUri;

  public ServerConfiguration() {
  }

  public ServerConfiguration(Credentials credentials) {
    myCredentials = credentials;
  }

  @Nullable
  @Tag(value = "credentials")
  public Credentials getCredentials() {
    return myCredentials;
  }

  public void setCredentials(@NotNull final Credentials credentials) {
    myCredentials = credentials;
  }

  @Nullable
  @Tag(value = "proxy")
  public String getProxyUri() {
    return myProxyUri;
  }

  public void setProxyUri(@Nullable final String proxyUri) {
    myProxyUri = proxyUri;
  }
}
