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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.StreamUtil;
import org.apache.axis2.Constants;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.Stub;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSConstants;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.core.configuration.Credentials;
import org.jetbrains.tfsIntegration.core.configuration.TFSConfigurationManager;
import org.jetbrains.tfsIntegration.core.tfs.HTTPProxyInfo;
import org.jetbrains.tfsIntegration.core.tfs.TfsUtil;
import org.jetbrains.tfsIntegration.core.tfs.Workstation;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;
import org.jetbrains.tfsIntegration.exceptions.*;
import org.jetbrains.tfsIntegration.stubs.RegistrationRegistrationSoapStub;
import org.jetbrains.tfsIntegration.stubs.ServerStatusServerStatusSoapStub;
import org.jetbrains.tfsIntegration.stubs.compatibility.CustomSOAPBuilder;
import org.jetbrains.tfsIntegration.stubs.services.registration.ArrayOfRegistrationEntry;
import org.jetbrains.tfsIntegration.stubs.services.registration.RegistrationEntry;
import org.jetbrains.tfsIntegration.stubs.services.registration.RegistrationExtendedAttribute;
import org.jetbrains.tfsIntegration.stubs.services.serverstatus.CheckAuthentication;
import org.jetbrains.tfsIntegration.ui.LoginDialog;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class WebServiceHelper {

  @NonNls private static final String SOAP_BUILDER_KEY = "application/soap+xml";
  @NonNls private static final String CONTENT_TYPE_GZIP = "application/gzip";

  private static final Object STATIC_LOCK = new Object();
  private static final Map<URI, Object> ourLocks = Collections.synchronizedMap(new HashMap<URI, Object>());

  private static final Map<URI, String> ourErrorMessages = Collections.synchronizedMap(new HashMap<URI, String>());

  public interface VoidDelegate {

    void executeRequest() throws RemoteException;
  }

  public interface Delegate<T> {
    @Nullable
    T executeRequest() throws RemoteException;
  }

  private interface InnerDelegate<T> {
    @Nullable
    T executeRequest(@NotNull URI serverUri, @NotNull Credentials credentials) throws Exception;
  }

  static {
    //System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
    //System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
    //System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header", "debug");
    //System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "debug");
  }

  @NonNls private static final String TFS_TOOL_ID = "vstfs";
  @NonNls private static final String INSTANCE_ID_ATTRIBUTE = "InstanceId";

  @NotNull
  public static Pair<URI, String/*guid*/> authenticate(final @Nullable URI serverUri) throws TfsException {
    return executeRequest(serverUri, new InnerDelegate<Pair<URI, String>>() {
      public Pair<URI, String> executeRequest(@NotNull final URI serverUri, @NotNull final Credentials credentials) throws Exception {
        ServerStatusServerStatusSoapStub serverStatusStub =
          new ServerStatusServerStatusSoapStub(serverUri.toString() + TFSConstants.SERVER_STATUS_ASMX);
        setupStub(serverStatusStub, credentials, serverUri);

        String connectionCredentials = serverStatusStub.CheckAuthentication(new CheckAuthentication());
        if (!credentials.getQualifiedUsername().equalsIgnoreCase(connectionCredentials)) {
          throw new WrongConnectionException(connectionCredentials);
        }

        RegistrationRegistrationSoapStub registrationStub =
          new RegistrationRegistrationSoapStub(serverUri.toString() + TFSConstants.REGISTRATION_ASMX);
        setupStub(registrationStub, credentials, serverUri);
        ArrayOfRegistrationEntry registrationEntries = registrationStub.GetRegistrationEntries(TFS_TOOL_ID);
        for (RegistrationEntry entry : registrationEntries.getRegistrationEntry()) {
          if (TFS_TOOL_ID.equals(entry.getType())) {
            for (RegistrationExtendedAttribute attribute : entry.getRegistrationExtendedAttributes().getRegistrationExtendedAttribute()) {
              if (INSTANCE_ID_ATTRIBUTE.equals(attribute.getName())) {
                final String instanceId = attribute.getValue();
                ServerInfo existingServer = Workstation.getInstance().getServerByInstanceId(instanceId);
                if (existingServer != null) {
                  throw new DuplicateServerInstanceIdException(existingServer.getUri());
                }
                return Pair.create(serverUri, instanceId);
              }
            }
          }
        }
        throw new ConnectionFailedException("Failed to obtain server instance.");
      }
    });
  }

  public static void executeRequest(Stub stub, final VoidDelegate delegate) throws TfsException {
    executeRequest(stub, new Delegate<Object>() {

      @Nullable
      public Object executeRequest() throws RemoteException {
        delegate.executeRequest();
        return null;
      }
    });
  }

  public static <T> T executeRequest(final Stub stub, final Delegate<T> delegate) throws TfsException {
    URI serverUri = TfsUtil.getHostUri(stub._getServiceClient().getOptions().getTo().getAddress(), false);

    return executeRequest(serverUri, new InnerDelegate<T>() {
      public T executeRequest(final @NotNull URI serverUri, final @NotNull Credentials credentials) throws Exception {
        setupStub(stub, credentials, serverUri);
        return delegate.executeRequest();
      }
    });
  }

  public static void httpGet(final URI serverUri, final String downloadUrl, final OutputStream outputStream) throws TfsException {
    TFSVcs.assertTrue(downloadUrl != null);

    executeRequest(serverUri, new InnerDelegate<Object>() {
      public Object executeRequest(final @NotNull URI serverUri, final @NotNull Credentials credentials) throws Exception {
        HttpClient httpClient = new HttpClient();
        setCredentials(httpClient, credentials, serverUri);
        setProxy(httpClient);

        HttpMethod method = new GetMethod(downloadUrl);
        int statusCode = httpClient.executeMethod(method);
        if (statusCode == HttpStatus.SC_OK) {
          StreamUtil.copyStreamContent(getInputStream(method), outputStream);
        }
        else if (statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
          throw new OperationFailedException(method.getResponseBodyAsString());
        }
        else {
          throw TfsExceptionManager.createHttpTransportErrorException(statusCode, null);
        }
        return null;
      }
    });
  }

  public static void httpPost(final @NotNull String uploadUrl, final @NotNull Part[] parts, final @Nullable OutputStream outputStream)
    throws TfsException {
    final URI serverUri = TfsUtil.getHostUri(uploadUrl, false);

    executeRequest(serverUri, new InnerDelegate<Object>() {
      public Object executeRequest(final @NotNull URI serverUri, final @NotNull Credentials credentials) throws Exception {
        HttpClient httpClient = new HttpClient();
        setCredentials(httpClient, credentials, serverUri);
        setProxy(httpClient);

        PostMethod method = new PostMethod(uploadUrl);
        method.setRequestHeader("X-TFS-Version", "1.0.0.0");
        method.setRequestHeader("accept-language", "en-US");
        method.setRequestEntity(new MultipartRequestEntity(parts, method.getParams()));

        int statusCode = httpClient.executeMethod(method);
        if (statusCode == HttpStatus.SC_OK) {
          if (outputStream != null) {
            StreamUtil.copyStreamContent(getInputStream(method), outputStream);
          }
        }
        else if (statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
          throw new OperationFailedException(method.getResponseBodyAsString());
        }
        else {
          throw TfsExceptionManager.createHttpTransportErrorException(statusCode, null);
        }
        return null;
      }
    });
  }

  private static <T> T executeRequest(@Nullable final URI initialUri, InnerDelegate<T> delegate) throws TfsException {
    @Nullable final Credentials originalStoredCredentials =
      initialUri != null ? TFSConfigurationManager.getInstance().getCredentials(initialUri) : null;
    trace("entering: initial uri={0}, original stored creds={1}", initialUri, originalStoredCredentials);
    final Ref<URI> uri = Ref.create(initialUri);
    final Ref<Credentials> credentials = Ref.create(originalStoredCredentials);
    final long callerThreadId = Thread.currentThread().getId();

    while (true) {
      trace("looping: uri={0}, creds={1}", uri.get(), credentials.get());
      if (uri.isNull() || credentials.isNull() || credentials.get().getPassword() == null) {
        Runnable runnable = new Runnable() {
          public void run() {
            trace(callerThreadId, "got to UI thread");
            trace(callerThreadId, "modality state: {0}", ApplicationManager.getApplication().getCurrentModalityState());
            trace(callerThreadId, "waiting for UI lock [{0}]...", getLock(uri.get()));
            synchronized (getLock(uri.get())) {
              trace(callerThreadId, "got UI lock");
              // if another thread was pending to prompt for credentials with the same server, it may already succeed and there's no need to ask again
              boolean shouldPrompt = true;
              if (initialUri != null) {
                // if originally stored credentials not null, URI was specified and therefore isn't changed
                Credentials recentlyStoredCredentials = TFSConfigurationManager.getInstance().getCredentials(uri.get());
                trace(callerThreadId, "recently stored credentials for {0}: {1}", uri.get(), recentlyStoredCredentials);
                if (recentlyStoredCredentials != null &&
                    originalStoredCredentials != null &&
                    !recentlyStoredCredentials.equalsTo(originalStoredCredentials)) {
                  credentials.set(recentlyStoredCredentials);
                  shouldPrompt = false;
                }
              }

              if (shouldPrompt) {
                trace(callerThreadId, "showing dialog uri={0}, creds={1}, msg={2}", uri.get(), credentials.get(),
                      uri.isNull() ? "null" : "\"" + ourErrorMessages.get(uri.get()) + "\"");
                final LoginDialog d = new LoginDialog(uri.get(), credentials.get(), initialUri == null);
                if (!uri.isNull()) {
                  d.setMessage(ourErrorMessages.get(uri.get()));
                }
                d.show();
                trace(callerThreadId, "login dialog finished");
                if (d.isOK()) {
                  // if uri changed, clear error message for old uri
                  if (!uri.isNull() && !d.getUri().equals(uri.get())) {
                    trace(callerThreadId, "dialog uri={0}, clearing msg for {1}", d.getUri(), uri.get());
                    ourErrorMessages.remove(uri.get());
                  }
                  uri.set(d.getUri());
                  credentials.set(d.getCredentials());
                }
                else {
                  if (!uri.isNull()) {
                    trace(callerThreadId, "user cancelled, clearing msg for {0}", uri.get());
                    ourErrorMessages.remove(uri.get());
                  }
                  credentials.set(null);
                }
              }
              trace(callerThreadId, "leaving UI lock");
            }
            trace(callerThreadId, "leaving UI thread");
          }
        };

        trace("waiting for UI thread...");
        try {
          TfsUtil.runOrInvokeAndWaitNonModal(runnable);
        }
        catch (InvocationTargetException e) {
          trace("UI thread interrupted {0}, throwing out", e.getMessage());
          throw new OperationFailedException(e.getMessage());
        }
        catch (InterruptedException e) {
          trace("UI thread interrupted {0}, throwing out", e.getMessage());
          throw new OperationFailedException(e.getMessage());
        }

        if (credentials.isNull()) {
          throw new UserCancelledException();
        }

        TFSVcs.assertTrue(uri.get() != null);
        if (initialUri == null && TFSConfigurationManager.getInstance().serverKnown(uri.get())) {
          trace("msg=\"duplicate server uri {0}\"", uri.get());
          ourErrorMessages.put(uri.get(), "Duplicate server address");
          credentials.get().resetPassword(); // continue with prompt
          continue;
        }
      }

      trace("waiting for request lock [{0}]...", getLock(uri.get()));
      synchronized (getLock(uri.get())) {
        trace("got request lock");
        try {
          TFSVcs.assertTrue(credentials.get().getPassword() != null);
          trace("making server call...");
          T result = delegate.executeRequest(uri.get(), credentials.get());
          trace("request succeesed, clearing msg for {0}, storing credentials", uri.get());
          ourErrorMessages.remove(uri.get());
          TFSConfigurationManager.getInstance().storeCredentials(uri.get(), credentials.get());
          trace("leaving request lock - success !");
          return result;
        }
        catch (Exception e) {
          final TfsException tfsException = TfsExceptionManager.processException(e);
          if (initialUri == null || tfsException instanceof UnauthorizedException) {
            ourErrorMessages.put(uri.get(), tfsException.getMessage());
            credentials.get().resetPassword(); // continue with prompt
            trace("failed: \"{0}\", setting msg for {1} and loop again", tfsException.getMessage(), uri.get());
          }
          else {
            if (!(tfsException instanceof ConnectionFailedException)) {
              // operation failed but connection was succsessful
              TFSConfigurationManager.getInstance().storeCredentials(uri.get(), credentials.get());
            }
            trace("failed: \"{0}\", throwing out of request lock, clearing msg", tfsException.getMessage(), uri.get());
            ourErrorMessages.remove(uri.get());
            throw tfsException;
          }
        }
        trace("leaving request lock");
      }
    }
  }

  private synchronized static Object getLock(@Nullable URI serverUri) {
    if (serverUri == null) {
      return STATIC_LOCK;
    }
    else {
      Object lock = ourLocks.get(serverUri);
      if (lock == null) {
        lock = new Object();
        ourLocks.put(serverUri, lock);
      }
      return lock;
    }
  }

  // TODO move to stubs
  public static ConfigurationContext getStubConfigurationContext() throws Exception {
    ConfigurationContext configContext = ConfigurationContextFactory.createDefaultConfigurationContext();
    configContext.getAxisConfiguration().addMessageBuilder(SOAP_BUILDER_KEY, new CustomSOAPBuilder());
    return configContext;
  }

  private static void setProxy(HttpClient httpClient) {
    final HTTPProxyInfo proxy = HTTPProxyInfo.getCurrent();
    if (proxy.host != null) {
      httpClient.getHostConfiguration().setProxy(proxy.host, proxy.port);
      if (proxy.user != null) {
        httpClient.getState().setProxyCredentials(AuthScope.ANY, new UsernamePasswordCredentials(proxy.user, proxy.password));
      }
    }
    else {
      httpClient.getHostConfiguration().setProxyHost(null);
    }
  }

  private static void setupStub(final @NotNull Stub stub, final @NotNull Credentials credentials, final @NotNull URI serverUri) {
    Options options = stub._getServiceClient().getOptions();

    // http params
    options.setProperty(HTTPConstants.CHUNKED, Constants.VALUE_FALSE);
    options.setProperty(HTTPConstants.MC_ACCEPT_GZIP, Boolean.TRUE);

    // credentials
    HttpTransportProperties.Authenticator auth = new HttpTransportProperties.Authenticator();
    auth.setUsername(credentials.getUserName());
    auth.setPassword(credentials.getPassword() != null ? credentials.getPassword() : "");
    auth.setDomain(credentials.getDomain());
    auth.setHost(serverUri.getHost());
    options.setProperty(HTTPConstants.AUTHENTICATE, auth);

    // proxy
    final HttpTransportProperties.ProxyProperties proxyProperties;
    final HTTPProxyInfo proxy = HTTPProxyInfo.getCurrent();
    if (proxy.host != null) {
      proxyProperties = new HttpTransportProperties.ProxyProperties();
      proxyProperties.setProxyName(proxy.host);
      proxyProperties.setProxyPort(proxy.port);
      proxyProperties.setUserName(proxy.user);
      proxyProperties.setPassWord(proxy.password);
    }
    else {
      proxyProperties = null;
    }

    // TODO FIXME axis2 will ignore our proxy settings and will use System property "http.proxyHost" instead if it is set to any value.
    // This system property may be set if any other Idea plugin (like IDE Talk) invokes HTTPConfigurable.prepareURL() or HTTPConfigurable.setAuthenticator()
    options.setProperty(HTTPConstants.PROXY, proxyProperties);
  }

  private static void setCredentials(final @NotNull HttpClient httpClient,
                                     final @NotNull Credentials credentials,
                                     final @NotNull URI serverUri) {
    final NTCredentials ntCreds =
      new NTCredentials(credentials.getUserName(), credentials.getPassword(), serverUri.getHost(), credentials.getDomain());
    httpClient.getState().setCredentials(AuthScope.ANY, ntCreds);
  }

  private static InputStream getInputStream(HttpMethod method) throws Exception {
    // TODO: find proper way to determine gzip compression
    Header contentType = method.getResponseHeader(HTTPConstants.HEADER_CONTENT_TYPE);
    if (contentType != null && CONTENT_TYPE_GZIP.equalsIgnoreCase(contentType.getValue())) {
      return new GZIPInputStream(method.getResponseBodyAsStream());
    }
    else {
      return method.getResponseBodyAsStream();
    }
  }

  @SuppressWarnings({"UnusedDeclaration"})
  private static void trace(long threadId, @NonNls String msg) {
    // you may need this for debugging
    //String dispatch = ApplicationManager.getApplication().isDispatchThread() ? " [d]" : "";
    //System.out.println(String.valueOf(System.currentTimeMillis()) +
    //                   ", thread=" +
    //                   String.valueOf(threadId) +
    //                   ", cur thread=" +
    //                   String.valueOf(Thread.currentThread().getId()) +
    //                   dispatch +
    //                   ": " +
    //                   msg);
  }

  private static void trace(@NonNls String msg) {
    trace(Thread.currentThread().getId(), msg);
  }

  private static void trace(long threadId, @NonNls String pattern, @NonNls Object... params) {
    trace(threadId, MessageFormat.format(pattern, params));
  }

  private static void trace(@NonNls String pattern, @NonNls Object... params) {
    trace(Thread.currentThread().getId(), pattern, params);
  }


}