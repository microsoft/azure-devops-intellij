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

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;
import org.jetbrains.tfsIntegration.core.tfs.Workstation;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

@State(
  name = "org.jetbrains.tfsIntegration.core.configuration.TFSConfigurationManager",
  storages = {@Storage(
    id = "other",
    file = "$APP_CONFIG$/tfs.xml")})
public class TFSConfigurationManager implements PersistentStateComponent<TFSConfigurationManager.State> {

  public static class State {
    @MapAnnotation(entryTagName = "server", keyAttributeName = "uri", surroundValueWithTag = false)
    public Map<String, ServerConfiguration> config = new HashMap<String, ServerConfiguration>();

    public boolean useIdeaHttpProxy = true;

    public State() {
      this(new HashMap<String, ServerConfiguration>(), true);
    }

    public State(final Map<String, ServerConfiguration> config, boolean useIdeaHttpProxy) {
      this.config = config;
      this.useIdeaHttpProxy = useIdeaHttpProxy;
    }
  }

  private Map<String, ServerConfiguration> myServersConfig = new HashMap<String, ServerConfiguration>();
  private boolean myUseIdeaHttpProxy = true;

  @NotNull
  public static synchronized TFSConfigurationManager getInstance() {
    return ServiceManager.getService(TFSConfigurationManager.class);
  }

  /**
   * @return null if not found
   */
  @Nullable
  public synchronized Credentials getCredentials(@NotNull URI serverUri) {
    final ServerConfiguration serverConfiguration = myServersConfig.get(serverUri.toString());
    return serverConfiguration != null ? serverConfiguration.getCredentials() : null;
  }

  @Nullable
  public URI getProxyUri(@NotNull URI serverUri) {
    final ServerConfiguration serverConfiguration = myServersConfig.get(serverUri.toString());
    try {
      return serverConfiguration != null && serverConfiguration.getProxyUri() != null ? new URI(serverConfiguration.getProxyUri()) : null;
    }
    catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean shouldTryProxy(@NotNull URI serverUri) {
    final ServerConfiguration serverConfiguration = myServersConfig.get(serverUri.toString());
    return serverConfiguration != null && serverConfiguration.getProxyUri() != null && !serverConfiguration.isProxyInaccessible();
  }

  public void setProxyInaccessible(@NotNull URI serverUri) {
    myServersConfig.get(serverUri.toString()).setProxyInaccessible();
  }

  public void setProxyUri(@NotNull URI serverUri, @Nullable URI proxyUri) {
    String proxyUriString = proxyUri != null ? proxyUri.toString() : null;
    getOrCreateServerConfiguration(serverUri).setProxyUri(proxyUriString);
  }

  public synchronized void storeCredentials(@NotNull URI serverUri, final @NotNull Credentials credentials) {
    getOrCreateServerConfiguration(serverUri).setCredentials(credentials);
  }

  public synchronized void resetStoredPasswords() {
    for (ServerConfiguration serverConfiguration : myServersConfig.values()) {
      final Credentials credentials = serverConfiguration.getCredentials();
      if (credentials != null) {
        credentials.resetPassword();
      }
    }
  }

  public void loadState(final State state) {
    myServersConfig = state.config;
    myUseIdeaHttpProxy = state.useIdeaHttpProxy;
  }

  public State getState() {
    //try {
    //  Element element = XmlSerializer.serialize(myServersConfig.values().iterator().next());
    //  Document d = new Document(element);
    //  try {
    //    JDOMUtil.writeDocument(d, "C:\\test.xml", "\n");
    //  }
    //  catch (IOException e) {
    //    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    //  }
    //}
    //catch (Exception e) {
    //  int tt = 0;
    //}
    return new State(myServersConfig, myUseIdeaHttpProxy);
  }

  @NotNull
  private ServerConfiguration getOrCreateServerConfiguration(@NotNull URI serverUri) {
    ServerConfiguration config = myServersConfig.get(serverUri.toString());
    if (config == null) {
      config = new ServerConfiguration();
      myServersConfig.put(serverUri.toString(), config);
    }
    return config;
  }

  // TODO this class should take Workstation responsibilities!!!
  public boolean serverKnown(final @NotNull URI uri) {
    for (ServerInfo server : Workstation.getInstance().getServers()) {
      if (server.getUri().getHost().equalsIgnoreCase(uri.getHost())) {
        return true;
      }
    }
    return false;
  }

  public void remove(final @NotNull URI serverUri) {
    myServersConfig.remove(serverUri.toString());
  }

  public void setUseIdeaHttpProxy(boolean useIdeaHttpProxy) {
    myUseIdeaHttpProxy = useIdeaHttpProxy;
  }

  public boolean useIdeaHttpProxy() {
    return myUseIdeaHttpProxy;
  }

}
