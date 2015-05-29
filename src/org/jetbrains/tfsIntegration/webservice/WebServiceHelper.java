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
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.ClassLoaderUtil;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.util.containers.ContainerUtil;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axis2.Constants;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.Stub;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.auth.BasicScheme;
import org.apache.commons.httpclient.auth.DigestScheme;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.params.HostParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.core.configuration.Credentials;
import org.jetbrains.tfsIntegration.exceptions.OperationFailedException;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.exceptions.TfsExceptionManager;
import org.jetbrains.tfsIntegration.webservice.auth.NativeNTLM2Scheme;
import org.jetbrains.tfsIntegration.webservice.compatibility.CustomSOAP12Factory;
import org.jetbrains.tfsIntegration.webservice.compatibility.CustomSOAPBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class WebServiceHelper {

  private static final Logger LOG = Logger.getInstance(WebServiceHelper.class.getName());

  @NonNls private static final String SOAP_BUILDER_KEY = "application/soap+xml";
  @NonNls private static final String CONTENT_TYPE_GZIP = "application/gzip";

  public static final String USE_NATIVE_CREDENTIALS = WebServiceHelper.class.getName() + ".overrideCredentials";

  @SuppressWarnings("UseOfArchaicSystemPropertyAccessors")
  private static final int SOCKET_TIMEOUT = Integer.getInteger("org.jetbrains.tfsIntegration.socketTimeout", 30000);

  static {
    // keep NTLM scheme first
    AuthPolicy.unregisterAuthScheme(AuthPolicy.NTLM);
    AuthPolicy.unregisterAuthScheme(AuthPolicy.DIGEST);
    AuthPolicy.unregisterAuthScheme(AuthPolicy.BASIC);

    AuthPolicy.registerAuthScheme(AuthPolicy.NTLM, NativeNTLM2Scheme.class);
    AuthPolicy.registerAuthScheme(AuthPolicy.DIGEST, DigestScheme.class);
    AuthPolicy.registerAuthScheme(AuthPolicy.BASIC, BasicScheme.class);

    System.setProperty(OMAbstractFactory.SOAP12_FACTORY_NAME_PROPERTY, CustomSOAP12Factory.class.getName());
  }

  public interface Delegate<T> {
    @Nullable
    T executeRequest() throws RemoteException;
  }

  static {
    //System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
    //System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
    //System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header", "debug");
    //System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "debug");
  }


  public static void httpGet(final URI serverUri,
                             final String downloadUrl,
                             final OutputStream outputStream,
                             Credentials credentials,
                             final HttpClient httpClient)
    throws TfsException, IOException {
    TFSVcs.assertTrue(downloadUrl != null);
    setupHttpClient(credentials, serverUri, httpClient);

    HttpMethod method = new GetMethod(downloadUrl);
    try {
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
    }
    finally {
      // enforce connection release since GZipInputStream may not trigger underlying AutoCloseInputStream.close()
      method.releaseConnection();
    }
  }

  public static void httpPost(final @NotNull String uploadUrl,
                              final @NotNull Part[] parts,
                              final @Nullable OutputStream outputStream,
                              Credentials credentials,
                              URI serverUri,
                              final HttpClient httpClient)
    throws IOException, TfsException {
    setupHttpClient(credentials, serverUri, httpClient);

    PostMethod method = new PostMethod(uploadUrl);
    try {
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
    }
    finally {
      method.releaseConnection();
    }
  }

  public static ConfigurationContext getStubConfigurationContext() {
    return ClassLoaderUtil.runWithClassLoader(TFSVcs.class.getClassLoader(), new Computable<ConfigurationContext>() {
      public ConfigurationContext compute() {
        try {
          ConfigurationContext configContext = ConfigurationContextFactory.createDefaultConfigurationContext();
          configContext.getAxisConfiguration().addMessageBuilder(SOAP_BUILDER_KEY, new CustomSOAPBuilder());
          return configContext;
        }
        catch (Exception e) {
          LOG.error("Axis2 configuration error", e);
          return null;
        }
      }
    });
  }

  private static void setProxy(HttpClient httpClient) {
    final HTTPProxyInfo proxy = HTTPProxyInfo.getCurrent();
    if (proxy.host != null) {
      httpClient.getHostConfiguration().setProxy(proxy.host, proxy.port);
      if (proxy.user != null) {
        Pair<String, String> domainAndUser = getDomainAndUser(proxy.user);
        UsernamePasswordCredentials creds = new NTCredentials(domainAndUser.second, proxy.password, proxy.host, domainAndUser.first);
        httpClient.getState().setProxyCredentials(AuthScope.ANY, creds);
      }
    }
    else {
      httpClient.getHostConfiguration().setProxyHost(null);
    }
  }

  private static void setupHttpClient(Credentials credentials, URI serverUri, HttpClient httpClient) {
    setCredentials(httpClient, credentials, serverUri);
    setProxy(httpClient);
    httpClient.getParams().setSoTimeout(SOCKET_TIMEOUT);
    if (Registry.is("tfs.set.connection.timeout")) {
      httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(SOCKET_TIMEOUT);
      httpClient.getHttpConnectionManager().getParams().setSoTimeout(SOCKET_TIMEOUT);
    }
  }

  public static void setupStub(final @NotNull Stub stub, final @NotNull Credentials credentials, final @NotNull URI serverUri) {
    Options options = stub._getServiceClient().getOptions();

    // http params
    options.setProperty(HTTPConstants.CHUNKED, Constants.VALUE_FALSE);
    options.setProperty(HTTPConstants.MC_ACCEPT_GZIP, Boolean.TRUE);
    options.setProperty(HTTPConstants.SO_TIMEOUT, SOCKET_TIMEOUT);
    if (Registry.is("tfs.set.connection.timeout")) {
      options.setProperty(HTTPConstants.CONNECTION_TIMEOUT, SOCKET_TIMEOUT);
    }

    // credentials
    if (credentials.getType() == Credentials.Type.Alternate) {
      String basicAuth =
        BasicScheme.authenticate(new UsernamePasswordCredentials(credentials.getUserName(), credentials.getPassword()), "UTF-8");
      Map<String, String> headers = new HashMap<String, String>();
      headers.put(HTTPConstants.HEADER_AUTHORIZATION, basicAuth);
      options.setProperty(HTTPConstants.HTTP_HEADERS, headers);
    }
    else {
      HttpTransportProperties.Authenticator auth = new HttpTransportProperties.Authenticator();
      auth.setUsername(credentials.getUserName());
      auth.setPassword(credentials.getPassword() != null ? credentials.getPassword() : "");
      auth.setDomain(credentials.getDomain());
      auth.setHost(serverUri.getHost());
      options.setProperty(HTTPConstants.AUTHENTICATE, auth);

      HttpMethodParams params = new HttpMethodParams();
      params.setBooleanParameter(USE_NATIVE_CREDENTIALS, credentials.getType() == Credentials.Type.NtlmNative);
      options.setProperty(HTTPConstants.HTTP_METHOD_PARAMS, params);
    }

    // proxy
    final HttpTransportProperties.ProxyProperties proxyProperties;
    final HTTPProxyInfo proxy = HTTPProxyInfo.getCurrent();
    if (proxy.host != null) {
      proxyProperties = new HttpTransportProperties.ProxyProperties();
      Pair<String, String> domainAndUser = getDomainAndUser(proxy.user);
      proxyProperties.setProxyName(proxy.host);
      proxyProperties.setProxyPort(proxy.port);
      proxyProperties.setDomain(domainAndUser.first);
      proxyProperties.setUserName(domainAndUser.second);
      proxyProperties.setPassWord(proxy.password);
    }
    else {
      proxyProperties = null;
    }

    options.setProperty(HTTPConstants.PROXY, proxyProperties);
  }

  private static void setCredentials(final @NotNull HttpClient httpClient,
                                     final @NotNull Credentials credentials,
                                     final @NotNull URI serverUri) {
    if (credentials.getType() == Credentials.Type.Alternate) {
      HostParams parameters = httpClient.getHostConfiguration().getParams();
      Collection<Header> headers = (Collection<Header>)parameters.getParameter(HostParams.DEFAULT_HEADERS);

      if (headers == null) {
        headers = new ArrayList<Header>();
        parameters.setParameter(HostParams.DEFAULT_HEADERS, headers);
      }

      Header authHeader = ContainerUtil.find(headers, new Condition<Header>() {
        @Override
        public boolean value(Header header) {
          return header.getName().equals(HTTPConstants.HEADER_AUTHORIZATION);
        }
      });

      if (authHeader == null) {
        authHeader = new Header(HTTPConstants.HEADER_AUTHORIZATION, "");
        headers.add(authHeader);
      }

      authHeader
        .setValue(BasicScheme.authenticate(new UsernamePasswordCredentials(credentials.getUserName(), credentials.getPassword()), "UTF-8"));
    }
    else {
      final NTCredentials ntCreds =
        new NTCredentials(credentials.getUserName(), credentials.getPassword(), serverUri.getHost(), credentials.getDomain());
      httpClient.getState().setCredentials(AuthScope.ANY, ntCreds);
      httpClient.getParams().setBooleanParameter(USE_NATIVE_CREDENTIALS, credentials.getType() == Credentials.Type.NtlmNative);
    }
  }

  private static InputStream getInputStream(HttpMethod method) throws IOException {
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
    String dispatch = ApplicationManager.getApplication().isDispatchThread() ? " [d]" : "";
    @NonNls String message = String.valueOf(System.currentTimeMillis()) +
                             ", thread=" +
                             String.valueOf(threadId) +
                             ", cur thread=" +
                             String.valueOf(Thread.currentThread().getId()) +
                             dispatch +
                             ": " +
                             msg;
    //System.out.println(message);
  }

  private static Pair<String, String> getDomainAndUser(@Nullable String s) {
    if (s == null) {
      return Pair.create(null, null);
    }
    int slashPos = s.indexOf('\\');
    String domain = slashPos >= 0 ? s.substring(0, slashPos) : "";
    String user = slashPos >= 0 ? s.substring(slashPos + 1) : s;
    return Pair.create(domain, user);
  }

}
