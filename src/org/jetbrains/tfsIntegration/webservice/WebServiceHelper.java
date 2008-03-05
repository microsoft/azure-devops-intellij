package org.jetbrains.tfsIntegration.webservice;

import com.intellij.openapi.diagnostic.Logger;
import org.apache.axis2.Constants;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.Stub;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.tfsIntegration.core.TFSConstants;
import org.jetbrains.tfsIntegration.core.TFSVcs;
import org.jetbrains.tfsIntegration.core.credentials.Credentials;
import org.jetbrains.tfsIntegration.core.credentials.CredentialsManager;
import org.jetbrains.tfsIntegration.stubs.RegistrationRegistrationSoapStub;
import org.jetbrains.tfsIntegration.stubs.ServerStatusServerStatusSoapStub;
import org.jetbrains.tfsIntegration.stubs.compatibility.CustomSOAPBuilder;
import org.jetbrains.tfsIntegration.stubs.org.jetbrains.tfsIntegration.stubs.exceptions.ConnectionFailedException;
import org.jetbrains.tfsIntegration.stubs.org.jetbrains.tfsIntegration.stubs.exceptions.TfsException;
import org.jetbrains.tfsIntegration.stubs.org.jetbrains.tfsIntegration.stubs.exceptions.TfsExceptionManager;
import org.jetbrains.tfsIntegration.stubs.org.jetbrains.tfsIntegration.stubs.exceptions.UnauthorizedException;
import org.jetbrains.tfsIntegration.stubs.services.registration.ArrayOfRegistrationEntry;
import org.jetbrains.tfsIntegration.stubs.services.registration.RegistrationEntry;
import org.jetbrains.tfsIntegration.stubs.services.registration.RegistrationExtendedAttribute;
import org.jetbrains.tfsIntegration.stubs.services.serverstatus.CheckAuthentication;
import org.jetbrains.tfsIntegration.ui.LoginDialog;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.rmi.RemoteException;

public class WebServiceHelper {

  @NonNls private static final String SOAP_BUILDER_KEY = "application/soap+xml";

  public interface NoReturnDelegate {
    void executeRequest() throws RemoteException;
  }

  public interface Delegate<T> {
    @Nullable
    T executeRequest() throws RemoteException;
  }

  static {
    System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
    System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
    System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header", "debug");
    System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "debug");

    System.setProperty("http.proxyHost", "127.0.0.1");
    System.setProperty("http.proxyPort", "8888");
  }

  @NonNls private static final String TFS_TOOL_ID = "vstfs";
  @NonNls private static final String INSTANCE_ID_ATTRIBUTE = "InstanceId";

  private static final Logger LOG = Logger.getInstance(WebServiceHelper.class.getName());

  public static String authenticate(final URI serverUri, final Credentials credentials) throws TfsException {
    // TODO: refactor: use executeRequest()
    try {
      ServerStatusServerStatusSoapStub serverStatusStub =
        new ServerStatusServerStatusSoapStub(serverUri.toString() + TFSConstants.SERVER_STATUS_ASMX);
      setupStub(serverStatusStub);
      setCredentials(serverStatusStub, credentials, serverUri);
      String result = serverStatusStub.CheckAuthentication(new CheckAuthentication());
      String expected = credentials.getDomain() + "\\" + credentials.getUserName();
      if (!expected.equalsIgnoreCase(result)) {
        throw new UnauthorizedException("Returned credentials: " + result + " differ from expected: " + expected);
      }
      RegistrationRegistrationSoapStub registrationStub =
        new RegistrationRegistrationSoapStub(serverUri.toString() + TFSConstants.REGISTRATION_ASMX);
      setupStub(registrationStub);
      setCredentials(registrationStub, credentials, serverUri);
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

  private static void setCredentials(Stub stub, Credentials credentials, URI serverUri) {
    Options options = stub._getServiceClient().getOptions();
    HttpTransportProperties.Authenticator auth = new HttpTransportProperties.Authenticator();
    auth.setUsername(credentials.getUserName());
    auth.setPassword(credentials.getPassword());
    auth.setDomain(credentials.getDomain());
    auth.setHost(serverUri.getHost());
    options.setProperty(HTTPConstants.AUTHENTICATE, auth);
  }

  public static void executeRequest(Stub stub, final NoReturnDelegate delegate) throws TfsException {
    executeRequest(stub, new Delegate<Object>() {

      @Nullable
      public Object executeRequest() throws RemoteException {
        delegate.executeRequest();
        return null;
      }
    });
  }

  public static <T> T executeRequest(Stub stub, Delegate<T> delegate) throws TfsException {
    URI serverUri = null;
    try {
      URI targetEndpoint = new URI(stub._getServiceClient().getOptions().getTo().getAddress());
      serverUri =
        new URI(targetEndpoint.getScheme(), null, targetEndpoint.getHost(), targetEndpoint.getPort(), null, null, null); // TODO: trim?
    }
    catch (URISyntaxException e) {
      TFSVcs.LOG.error(e);
    }

    assert serverUri != null;

    Credentials credentialsToConnect = CredentialsManager.getInstance().getCredentials(serverUri);
    Credentials credentialsToStore = null;
    boolean forcePrompt = false;
    while (true) {
      if (credentialsToConnect.getPassword() == null || forcePrompt) {
        final LoginDialog d = new LoginDialog(serverUri, credentialsToConnect, false);
        Runnable runnable = new Runnable() {
          public void run() {
            d.show();
          }
        };
        if (SwingUtilities.isEventDispatchThread()) {
          runnable.run();
        }
        else {
          try {
            SwingUtilities.invokeAndWait(runnable);
          }
          catch (InterruptedException e) {
            // ignore
          }
          catch (InvocationTargetException e) {
            // ignore
          }
        }
        if (d.isOK()) {
          credentialsToConnect = d.getCredentialsToConnect();
          credentialsToStore = d.getCredentialsToStore();
        }
        else {
          throw new UserCancelledException();
        }
      }

      try {
        setCredentials(stub, credentialsToConnect, serverUri);
        T result = delegate.executeRequest();
        if (credentialsToStore != null) {
          CredentialsManager.getInstance().storeCredentials(serverUri, credentialsToStore);
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
          if (!(tfsException instanceof ConnectionFailedException) && credentialsToStore != null) {
            CredentialsManager.getInstance().storeCredentials(serverUri, credentialsToStore);
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

  public static void setupStub(Stub stub) {
    Options options = stub._getServiceClient().getOptions();
    options.setProperty(HTTPConstants.CHUNKED, Constants.VALUE_FALSE);
  }

}