/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import gnu.trove.THashMap;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axis2.Constants;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.Stub;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.util.CharArrayBuffer;
import org.apache.http.util.EncodingUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.core.configuration.Credentials;
import org.jetbrains.tfsIntegration.exceptions.OperationFailedException;
import org.jetbrains.tfsIntegration.exceptions.TfsException;
import org.jetbrains.tfsIntegration.exceptions.TfsExceptionManager;
import org.jetbrains.tfsIntegration.webservice.compatibility.CustomSOAP12Factory;
import org.jetbrains.tfsIntegration.webservice.compatibility.CustomSOAPBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class WebServiceHelper {
  private static final Logger LOG = Logger.getInstance(WebServiceHelper.class.getName());

  @NonNls private static final String SOAP_BUILDER_KEY = "application/soap+xml";
  @NonNls private static final String CONTENT_TYPE_GZIP = "application/gzip";

  static {
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
    HttpResponse response = createExecutor(credentials, serverUri, httpClient).execute(Request.Get(downloadUrl)).returnResponse();
    int statusCode = response.getStatusLine().getStatusCode();
    if (statusCode == HttpStatus.SC_OK) {
      StreamUtil.copyStreamContent(getInputStream(response), outputStream);
    }
    else if (statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
      throw new OperationFailedException(StreamUtil.readText(response.getEntity().getContent(), CharsetToolkit.UTF8_CHARSET));
    }
    else {
      throw TfsExceptionManager.createHttpTransportErrorException(response.getStatusLine(), null);
    }
  }

  public static void httpPost(final @NotNull String uploadUrl,
                              @NotNull HttpEntity body,
                              final @Nullable OutputStream outputStream,
                              Credentials credentials,
                              URI serverUri,
                              final HttpClient httpClient)
    throws IOException, TfsException {
    createExecutor(credentials, serverUri, httpClient);

    HttpResponse response = createExecutor(credentials, serverUri, httpClient).execute(Request.Post(uploadUrl)
                                                                                         .addHeader("X-TFS-Version", "1.0.0.0")
                                                                                         .addHeader("accept-language", "en-US")
                                                                                         .body(body))
      .returnResponse();
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode == HttpStatus.SC_OK) {
        if (outputStream != null) {
          StreamUtil.copyStreamContent(getInputStream(response), outputStream);
        }
      }
      else if (statusCode == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
        throw new OperationFailedException(StreamUtil.readText(response.getEntity().getContent(), CharsetToolkit.UTF8_CHARSET));
      }
      else {
        throw TfsExceptionManager.createHttpTransportErrorException(response.getStatusLine(), null);
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

  @NotNull
  private static Executor createExecutor(@NotNull Credentials credentials, @NotNull URI serverUri, @NotNull HttpClient httpClient) {
    Executor executor = Executor.newInstance(httpClient);
    if (credentials.getType() == Credentials.Type.Alternate) {
      executor.auth(credentials.getUserName(), credentials.getPassword());
    }
    else {
      executor.auth(new NTCredentials(credentials.getUserName(), credentials.getPassword(), serverUri.getHost(), credentials.getDomain()));
    }
    return executor;
  }

  public static void setupStub(final @NotNull Stub stub, final @NotNull Credentials credentials, final @NotNull URI serverUri) {
    Options options = stub._getServiceClient().getOptions();

    // http params
    options.setProperty(HTTPConstants.CHUNKED, Constants.VALUE_FALSE);
    options.setProperty(HTTPConstants.MC_ACCEPT_GZIP, Boolean.TRUE);

    // credentials
    if (credentials.getType() == Credentials.Type.Alternate) {
      UsernamePasswordCredentials credentials1 = new UsernamePasswordCredentials(credentials.getUserName(), credentials.getPassword());
      String data = credentials1.getUserPrincipal().getName() +
                    ":" + ((credentials.getPassword() == null) ? "null" : credentials.getPassword());
      byte[] base64password = new Base64(0).encode(EncodingUtils.getBytes(data, CharsetToolkit.UTF8));
      CharArrayBuffer buffer = new CharArrayBuffer(32);
      buffer.append(AUTH.WWW_AUTH_RESP);
      buffer.append(": Basic ");
      buffer.append(base64password, 0, base64password.length);

      Map<String, String> headers = new THashMap<String, String>();
      headers.put(HTTPConstants.HEADER_AUTHORIZATION, buffer.toString());
      options.setProperty(HTTPConstants.HTTP_HEADERS, headers);
    }
    else {
      HttpTransportProperties.Authenticator auth = new HttpTransportProperties.Authenticator();
      auth.setUsername(credentials.getUserName());
      auth.setPassword(credentials.getPassword() != null ? credentials.getPassword() : "");
      auth.setDomain(credentials.getDomain());
      auth.setHost(serverUri.getHost());
      options.setProperty(HTTPConstants.AUTHENTICATE, auth);
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

  @NotNull
  private static InputStream getInputStream(@NotNull HttpResponse response) throws IOException {
    HttpEntity entity = response.getEntity();
    Header contentType = entity.getContentType();
    if (contentType != null && CONTENT_TYPE_GZIP.equalsIgnoreCase(contentType.getValue())) {
      return new GZIPInputStream(entity.getContent());
    }
    else {
      return entity.getContent();
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
