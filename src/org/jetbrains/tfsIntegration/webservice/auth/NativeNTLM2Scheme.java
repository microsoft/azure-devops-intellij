package org.jetbrains.tfsIntegration.webservice.auth;

import com.intellij.openapi.diagnostic.Logger;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.auth.AuthenticationException;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.webservice.WebServiceHelper;
import sun.net.www.protocol.http.NTLMAuthSequence;

import java.io.IOException;
import java.lang.reflect.Constructor;

public class NativeNTLM2Scheme extends NTLM2Scheme {

  private static final Logger LOG = Logger.getInstance(NativeNTLM2Scheme.class.getName());

  @Nullable
  private final NTLMAuthSequence mySequence;

  public NativeNTLM2Scheme() {
    mySequence = createSequence();
  }

  @Nullable
  private static NTLMAuthSequence createSequence() {
    try {
      Constructor<NTLMAuthSequence> constructor = NTLMAuthSequence.class.getDeclaredConstructor(String.class, String.class, String.class);
      constructor.setAccessible(true);
      return constructor.newInstance(new Object[]{null, null, null});
    }
    catch (Throwable t) {
      LOG.debug(t);
      return null;
    }
  }

  @Override
  protected String getType1MessageResponse(NTCredentials ntcredentials, HttpMethodParams params) {
    if (!params.getBooleanParameter(WebServiceHelper.USE_NATIVE_CREDENTIALS, false) || mySequence == null) {
      return super.getType1MessageResponse(ntcredentials, params);
    }

    try {
      return mySequence.getAuthHeader(null);
    }
    catch (IOException e) {
      LOG.warn("Native authentication failed", e);
      return "";
    }
  }

  @Override
  protected String getType3MessageResponse(String type2message, NTCredentials ntcredentials, HttpMethodParams params)
    throws AuthenticationException {
    if (!params.getBooleanParameter(WebServiceHelper.USE_NATIVE_CREDENTIALS, false) || mySequence == null) {
      return super.getType3MessageResponse(type2message, ntcredentials, params);
    }

    try {
      return mySequence.getAuthHeader(type2message);
    }
    catch (IOException e) {
      LOG.warn("Native authentication failed", e);
      return "";
    }
  }

  public static boolean isAvailable() {
    return createSequence() != null;
  }
}
