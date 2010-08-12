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
    public boolean supportTfsCheckinPolicies = true;
    public boolean supportStatefulCheckinPolicies = true;
    public boolean reportNotInstalledCheckinPolicies = true;

  }

  private Map<String, ServerConfiguration> myServersConfig = new HashMap<String, ServerConfiguration>();
  private boolean myUseIdeaHttpProxy = true;
  private boolean mySupportTfsCheckinPolicies = true;
  private boolean mySupportStatefulCheckinPolicies = true;
  private boolean myReportNotInstalledCheckinPolicies = true;

  @NotNull
  public static synchronized TFSConfigurationManager getInstance() {
    return ServiceManager.getService(TFSConfigurationManager.class);
  }

  /**
   * @return null if not found
   */
  @Nullable
  public synchronized Credentials getCredentials(@NotNull URI serverUri) {
    final ServerConfiguration serverConfiguration = getConfiguration(serverUri);
    return serverConfiguration != null ? serverConfiguration.getCredentials() : null;
  }

  @Nullable
  public URI getProxyUri(@NotNull URI serverUri) {
    final ServerConfiguration serverConfiguration = getConfiguration(serverUri);
    try {
      return serverConfiguration != null && serverConfiguration.getProxyUri() != null ? new URI(serverConfiguration.getProxyUri()) : null;
    }
    catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean shouldTryProxy(@NotNull URI serverUri) {
    final ServerConfiguration serverConfiguration = getConfiguration(serverUri);
    return serverConfiguration != null && serverConfiguration.getProxyUri() != null && !serverConfiguration.isProxyInaccessible();
  }

  public void setProxyInaccessible(@NotNull URI serverUri) {
    getConfiguration(serverUri).setProxyInaccessible();
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
    mySupportTfsCheckinPolicies = state.supportTfsCheckinPolicies;
    mySupportStatefulCheckinPolicies = state.supportStatefulCheckinPolicies;
    myReportNotInstalledCheckinPolicies = state.reportNotInstalledCheckinPolicies;
  }

  public State getState() {
    final State state = new State();
    state.config = myServersConfig;
    state.supportStatefulCheckinPolicies = mySupportStatefulCheckinPolicies;
    state.supportTfsCheckinPolicies = mySupportTfsCheckinPolicies;
    state.useIdeaHttpProxy = myUseIdeaHttpProxy;
    state.reportNotInstalledCheckinPolicies = myReportNotInstalledCheckinPolicies;
    return state;
  }

  private static String getConfigKey(URI serverUri) {
    String uriString = serverUri.toString();
    if (!uriString.endsWith("/")) {
      // backward compatibility
      uriString += "/";
    }
    return uriString;
  }

  @Nullable
  private ServerConfiguration getConfiguration(URI serverUri) {
    return myServersConfig.get(getConfigKey(serverUri));
  }

  @NotNull
  private ServerConfiguration getOrCreateServerConfiguration(@NotNull URI serverUri) {
    ServerConfiguration config = myServersConfig.get(getConfigKey(serverUri));
    if (config == null) {
      config = new ServerConfiguration();
      myServersConfig.put(getConfigKey(serverUri), config);
    }
    return config;
  }

  public boolean serverKnown(final @NotNull String instanceId) {
    for (ServerInfo server : Workstation.getInstance().getServers()) {
      if (server.getGuid().equalsIgnoreCase(instanceId)) {
        return true;
      }
    }
    return false;
  }

  public void remove(final @NotNull URI serverUri) {
    myServersConfig.remove(getConfigKey(serverUri));
  }

  public void setUseIdeaHttpProxy(boolean useIdeaHttpProxy) {
    myUseIdeaHttpProxy = useIdeaHttpProxy;
  }

  public boolean useIdeaHttpProxy() {
    return myUseIdeaHttpProxy;
  }

  public TfsCheckinPoliciesCompatibility getCheckinPoliciesCompatibility() {
    return new TfsCheckinPoliciesCompatibility(mySupportStatefulCheckinPolicies, mySupportTfsCheckinPolicies,
                                               myReportNotInstalledCheckinPolicies);
  }

  public void setSupportTfsCheckinPolicies(boolean supportTfsCheckinPolicies) {
    mySupportTfsCheckinPolicies = supportTfsCheckinPolicies;
  }

  public void setSupportStatefulCheckinPolicies(boolean supportStatefulCheckinPolicies) {
    mySupportStatefulCheckinPolicies = supportStatefulCheckinPolicies;
  }

  public void setReportNotInstalledCheckinPolicies(boolean reportNotInstalledCheckinPolicies) {
    myReportNotInstalledCheckinPolicies = reportNotInstalledCheckinPolicies;
  }
}
