package org.jetbrains.tfsIntegration.webservice;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.Stub;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.tfsIntegration.core.credentials.Credentials;
import org.jetbrains.tfsIntegration.core.credentials.CredentialsManager;
import org.jetbrains.tfsIntegration.core.credentials.SimpleCredentialsProvider;
import org.jetbrains.tfsIntegration.stubs.RegistrationSoapStub;
import org.jetbrains.tfsIntegration.stubs.ServerStatusSoapStub;
import org.jetbrains.tfsIntegration.stubs.services.registration.ArrayOfRegistrationEntry;
import org.jetbrains.tfsIntegration.stubs.services.registration.RegistrationEntry;
import org.jetbrains.tfsIntegration.stubs.services.registration.RegistrationExtendedAttribute;
import org.jetbrains.tfsIntegration.stubs.services.serverstatus.CheckAuthentication;
import org.jetbrains.tfsIntegration.ui.DialogCredentialsProvider;

import java.net.URI;
import java.net.UnknownHostException;
import java.rmi.RemoteException;

// TODO: assign credentials provider to the params of the current connection

public class WebServiceHelper {

  public interface Delegate {
    void executeRequest() throws RemoteException, UnknownHostException;

    void handleError(Exception e);
  }

  static {
    System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
    System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
    System.setProperty("org.apache.commons.logging.simplelog.log.httpclient.wire.header", "debug");
    System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.commons.httpclient", "debug");

    System.setProperty("http.proxyHost", "127.0.0.1");
    System.setProperty("http.proxyPort", "8888");
  }

  @NonNls private static final String SERVER_STATUS_ENDPOINT = "/Services/v1.0/ServerStatus.asmx";
  @NonNls private static final String REGISTRATION_ENDPOINT = "/Services/v1.0/Registration.asmx";
  @NonNls private static final String TFS_TOOL_ID = "vstfs";
  @NonNls private static final String INSTANCE_ID_ATTRIBUTE = "InstanceId";

  public static String authenticate(final URI serverUri, final Credentials credentials) throws WebServiceException {
    try {
      ServerStatusSoapStub stub = new ServerStatusSoapStub(serverUri.toString() + SERVER_STATUS_ENDPOINT);
      setCredentials(stub, credentials, serverUri);
      String result = stub.CheckAuthentication(new CheckAuthentication());
      String expected = credentials.getDomain() + "\\" + credentials.getUserName();
      if (!expected.equalsIgnoreCase(result)) {
        throw new WebServiceException("Failed to login to server");
      }
      RegistrationSoapStub registrationStub = new RegistrationSoapStub(serverUri.toString() + REGISTRATION_ENDPOINT);
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
      throw new WebServiceException("Failed to obtain server instance id");
    }
    catch (AxisFault axisFault) {
      throw new WebServiceException(axisFault);
    }
    catch (RemoteException e) {
      throw new WebServiceException(e);
    }
  }

  public static void executeRequest(URI serverUri, Delegate delegate) {
    Credentials storedCredentials = CredentialsManager.getInstance().getCredentials(serverUri);

    CredentialsProvider credentialsProvider;
    if (storedCredentials == null) {
      credentialsProvider = new DialogCredentialsProvider(serverUri, null, null, null);
    }
    else if (storedCredentials.getPassword() == null) {
      credentialsProvider = new DialogCredentialsProvider(serverUri, storedCredentials.getUserName(), storedCredentials.getDomain(), null);
    }
    else {
      credentialsProvider =
        new SimpleCredentialsProvider(storedCredentials.getUserName(), storedCredentials.getDomain(), storedCredentials.getPassword());
    }

    while (true) {
      Exception exception;
      try {
        HttpClientParams.getDefaultParams().setParameter(CredentialsProvider.PROVIDER, credentialsProvider);
        delegate.executeRequest();
        if (credentialsProvider instanceof DialogCredentialsProvider) {
          DialogCredentialsProvider dcp = (DialogCredentialsProvider)credentialsProvider;
          CredentialsManager.getInstance()
            .storeCredentials(serverUri,
                              new Credentials(dcp.getUsername(), dcp.getDomain(), dcp.shouldStorePassword() ? dcp.getPassword() : null));
        }
        return;
      }
      catch (AxisFault axisFault) {
        exception = axisFault;
      }
      catch (RemoteException e) {
        exception = e;
      }
      catch (UnknownHostException e) {
        exception = e;
      }

      if (credentialsProvider instanceof DialogCredentialsProvider) {
        DialogCredentialsProvider dcp = (DialogCredentialsProvider)credentialsProvider;
        if (!dcp.wasShown() || !dcp.wasOk()) {
          return;
        }
        else {
          credentialsProvider = new DialogCredentialsProvider(serverUri, dcp.getUsername(), dcp.getDomain(), dcp.getPassword());
        }
      }
      else {
        credentialsProvider =
          new DialogCredentialsProvider(serverUri, storedCredentials.getUserName(), storedCredentials.getDomain(), null);
      }
      delegate.handleError(exception);
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


}
