package org.jetbrains.tfsIntegration.webservice.auth;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Pair;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.auth.AuthenticationException;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.webservice.WebServiceHelper;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class NativeNTLM2Scheme extends NTLM2Scheme {

  private static final Logger LOG = Logger.getInstance(NativeNTLM2Scheme.class.getName());

  @Nullable
  private final Object myAuthSequenceObject;
  @Nullable
  private final Method myGetAuthHeaderMethod;

  public NativeNTLM2Scheme() {
    Pair<Object, Method> pair = createNativeAuthSequence();
    myAuthSequenceObject = pair != null ? pair.first : null;
    myGetAuthHeaderMethod = pair != null ? pair.second : null;
  }

  @Nullable
  private static Pair<Object, Method> createNativeAuthSequence() {
    try {
      Class clazz = Class.forName("sun.net.www.protocol.http.NTLMAuthSequence");
      if (clazz == null) {
        return null;
      }
      Constructor constructor = clazz.getDeclaredConstructor(String.class, String.class, String.class);
      constructor.setAccessible(true);
      Object sequence = constructor.newInstance(new Object[]{null, null, null});
      Method method = clazz.getMethod("getAuthHeader", String.class);
      if (method == null) {
        return null;
      }
      return Pair.create(sequence, method);
    }
    catch (Throwable t) {
      LOG.debug(t);
      return null;
    }
  }

  @Override
  protected String getType1MessageResponse(NTCredentials ntcredentials, HttpMethodParams params) {
    if (!params.getBooleanParameter(WebServiceHelper.USE_NATIVE_CREDENTIALS, false) || myAuthSequenceObject == null) {
      return super.getType1MessageResponse(ntcredentials, params);
    }

    try {
      return (String)myGetAuthHeaderMethod.invoke(myAuthSequenceObject, new Object[]{null});
    }
    catch (Throwable t) {
      LOG.warn("Native authentication failed", t);
      return "";
    }
  }

  @Override
  protected String getType3MessageResponse(String type2message, NTCredentials ntcredentials, HttpMethodParams params)
    throws AuthenticationException {
    if (!params.getBooleanParameter(WebServiceHelper.USE_NATIVE_CREDENTIALS, false) || myAuthSequenceObject == null) {
      return super.getType3MessageResponse(type2message, ntcredentials, params);
    }

    try {
      return (String)myGetAuthHeaderMethod.invoke(myAuthSequenceObject, new Object[]{type2message});
    }
    catch (Throwable t) {
      LOG.warn("Native authentication failed", t);
      return "";
    }
  }

  public static boolean isAvailable() {
    return createNativeAuthSequence() != null;
  }
}
