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
import com.intellij.openapi.application.ModalityState;
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
import org.jetbrains.tfsIntegration.core.credentials.Credentials;
import org.jetbrains.tfsIntegration.core.credentials.CredentialsManager;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;
import java.util.zip.GZIPInputStream;

public class WebServiceHelper {

  @NonNls private static final String SOAP_BUILDER_KEY = "application/soap+xml";
  private static final String CONTENT_TYPE_GZIP = "application/gzip";

  public interface VoidDelegate {
    void executeRequest() throws RemoteException;
  }

  public interface Delegate<T> {
    @Nullable
    T executeRequest() throws RemoteException;
  }

  private interface InnerDelegate<T> {
    @Nullable
    T executeRequest(Credentials credentials) throws Exception;
  }


  static {
    //System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
    //System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
    //System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header", "debug");
    //System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "debug");
  }

  @NonNls private static final String TFS_TOOL_ID = "vstfs";
  @NonNls private static final String INSTANCE_ID_ATTRIBUTE = "InstanceId";

  public static String authenticate(final URI serverUri, final Credentials credentials) throws TfsException {
    // TODO: refactor: use executeRequest()
    try {
      ServerStatusServerStatusSoapStub serverStatusStub =
        new ServerStatusServerStatusSoapStub(serverUri.toString() + TFSConstants.SERVER_STATUS_ASMX);
      setupStub(serverStatusStub);
      setCredentials(serverStatusStub, credentials, serverUri);
      setProxy(serverStatusStub);
      String result = serverStatusStub.CheckAuthentication(new CheckAuthentication());
      if (!credentials.getQualifiedUsername().equalsIgnoreCase(result)) {
        throw new WrongConnectionException(result);
      }
      RegistrationRegistrationSoapStub registrationStub =
        new RegistrationRegistrationSoapStub(serverUri.toString() + TFSConstants.REGISTRATION_ASMX);
      setupStub(registrationStub);
      setCredentials(registrationStub, credentials, serverUri);
      setProxy(registrationStub);
      ArrayOfRegistrationEntry registrationEntries = registrationStub.GetRegistrationEntries(TFS_TOOL_ID);
      for (RegistrationEntry entry : registrationEntries.getRegistrationEntry()) {
        if (TFS_TOOL_ID.equals(entry.getType())) {
          for (RegistrationExtendedAttribute attribute : entry.getRegistrationExtendedAttributes().getRegistrationExtendedAttribute()) {
            if (INSTANCE_ID_ATTRIBUTE.equals(attribute.getName())) {
              return attribute.getValue();
            }
          }
        }
      }
      throw new UnauthorizedException("Failed to obtain server instance");
    }
    catch (Exception e) {
      throw TfsExceptionManager.processException(e);
    }
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
    final Ref<URI> serverUri = new Ref<URI>();
    try {
      URI targetEndpoint = new URI(stub._getServiceClient().getOptions().getTo().getAddress());
      serverUri.set(
        new URI(targetEndpoint.getScheme(), null, targetEndpoint.getHost(), targetEndpoint.getPort(), null, null, null)); // TODO: trim?
    }
    catch (URISyntaxException e) {
      TFSVcs.LOG.error(e);
    }

    return executeRequest(serverUri.get(), new InnerDelegate<T>() {
      public T executeRequest(final Credentials credentials) throws Exception {
        setCredentials(stub, credentials, serverUri.get());
        setProxy(stub);
        return delegate.executeRequest();
      }
    });
  }

  public static void httpGet(final String downloadUrl, final OutputStream outputStream) throws TfsException {
    TFSVcs.assertTrue(downloadUrl != null);
    final Ref<URI> serverUri = new Ref<URI>();
    try {
      URI uri = new URI(downloadUrl);
      serverUri.set(new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), null, null, null)); // TODO: trim?
    }
    catch (URISyntaxException e) {
      TFSVcs.LOG.error(e);
    }

    executeRequest(serverUri.get(), new InnerDelegate<Object>() {

      public Object executeRequest(final Credentials credentials) throws Exception {
        HttpClient httpClient = new HttpClient();
        setCredentials(httpClient, credentials, serverUri.get());
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

  private static <T> T executeRequest(final URI serverUri, InnerDelegate<T> delegate) throws TfsException {
    Credentials credentials = CredentialsManager.getInstance().getCredentials(serverUri);
    boolean forcePrompt = false;
    while (true) {
      if (/*credentials.getPassword() == null ||*/ forcePrompt) {
        final Ref<Credentials> dialogCredentials = new Ref<Credentials>(credentials);
        Runnable runnable = new Runnable() {
          public void run() {
            // if another thread was pending to prompt for credentials, it may already succeed and there's no need to ask again
            Credentials actualCredentials = CredentialsManager.getInstance().getCredentials(serverUri);
            //noinspection ConstantConditions
            if (actualCredentials.equalsTo(dialogCredentials.get())) {
              final LoginDialog d = new LoginDialog(serverUri, dialogCredentials.get(), false);
              d.show();
              if (d.isOK()) {
                dialogCredentials.set(d.getCredentials());
              } else {
                dialogCredentials.set(null);
              }
            } else {
              dialogCredentials.set(actualCredentials);
            }
          }
        };
        if (ApplicationManager.getApplication().isDispatchThread()) {
          runnable.run();
        }
        else {
          ApplicationManager.getApplication().invokeAndWait(runnable, ModalityState.defaultModalityState());
        }
        if (dialogCredentials.get() != null) {
          credentials = dialogCredentials.get();
        }
        else {
          throw new UserCancelledException();
        }
      }

      try {
        T result = delegate.executeRequest(credentials);
        if (forcePrompt) {
          CredentialsManager.getInstance().storeCredentials(serverUri, credentials);
        }
        return result;
      }
      catch (Exception e) {
        TfsException tfsException = TfsExceptionManager.processException(e);
        if (tfsException instanceof UnauthorizedException) {
          // repeat with login dialog
          forcePrompt = true;
        }
        else {
          if (!(tfsException instanceof ConnectionFailedException)) {
            CredentialsManager.getInstance().storeCredentials(serverUri, credentials);
          }
          throw tfsException;
        }
      }
    }
  }


  public static ConfigurationContext getStubConfigurationContext() throws Exception {
    ConfigurationContext configContext = ConfigurationContextFactory.createDefaultConfigurationContext();
    configContext.getAxisConfiguration().addMessageBuilder(SOAP_BUILDER_KEY, new CustomSOAPBuilder());
    return configContext;
  }

  private static void setProxy(Stub stub) {
    final HttpTransportProperties.ProxyProperties proxyProperties;
    if (TFSVcs.getProxyHost() != null) {
      proxyProperties = new HttpTransportProperties.ProxyProperties();
      proxyProperties.setProxyName(TFSVcs.getProxyHost());
      proxyProperties.setProxyPort(TFSVcs.getProxyPort());
    }
    else {
      proxyProperties = null;
    }

    Options options = stub._getServiceClient().getOptions();
    options.setProperty(HTTPConstants.PROXY, proxyProperties);
  }

  private static void setProxy(HttpClient httpClient) {
    if (TFSVcs.getProxyHost() != null) {
      httpClient.getHostConfiguration().setProxy(TFSVcs.getProxyHost(), TFSVcs.getProxyPort());
    }
    else {
      httpClient.getHostConfiguration().setProxyHost(null);
    }
  }

  private static void setCredentials(final @NotNull Stub stub, final @Nullable Credentials credentials, final @NotNull URI serverUri) {
    if (credentials != null) {
      Options options = stub._getServiceClient().getOptions();
      HttpTransportProperties.Authenticator auth = new HttpTransportProperties.Authenticator();
      auth.setUsername(credentials.getUserName());
      auth.setPassword(credentials.getPassword() != null ? credentials.getPassword() : "");
      auth.setDomain(credentials.getDomain());
      auth.setHost(serverUri.getHost());
      options.setProperty(HTTPConstants.AUTHENTICATE, auth);
    }
  }

  private static void setCredentials(final @NotNull HttpClient httpClient,
                                     final @Nullable Credentials credentials,
                                     final @NotNull URI serverUri) {
    if (credentials != null) {
      httpClient.getState().setCredentials(AuthScope.ANY, new NTCredentials(credentials.getUserName(), credentials.getPassword() != null
                                                                                                       ? credentials.getPassword()
                                                                                                       : null, serverUri.getHost(),
                                                                                                               credentials.getDomain()));
    }
  }

  public static void setupStub(Stub stub) {
    Options options = stub._getServiceClient().getOptions();
    options.setProperty(HTTPConstants.CHUNKED, Constants.VALUE_FALSE);
    options.setProperty(HTTPConstants.MC_ACCEPT_GZIP, Boolean.TRUE);
  }

  public static void httpPost(final @NotNull String uploadUrl, final @NotNull Part[] parts, final @Nullable OutputStream outputStream)
    throws TfsException {
    final Ref<URI> serverUri = new Ref<URI>();
    try {
      URI uri = new URI(uploadUrl);
      serverUri.set(new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), null, null, null)); // TODO: trim?
    }
    catch (URISyntaxException e) {
      TFSVcs.LOG.error(e);
    }

    executeRequest(serverUri.get(), new InnerDelegate<Object>() {

      public Object executeRequest(final Credentials credentials) throws Exception {
        HttpClient httpClient = new HttpClient();
        setCredentials(httpClient, credentials, serverUri.get());
        setProxy(httpClient);

        PostMethod method = new PostMethod(uploadUrl);
        method.setRequestHeader("X-TFS-Version", "1.0.0.0");
        method.setRequestHeader("accept-language", "en-US");
        //method.setRequestHeader("X-VersionControl-Instance",
        //                            "ac4d8821-8927-4f07-9acf-adbf71119886, CommandCheckin");

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

}