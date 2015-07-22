package com.microsoft.vso.idea.utils;

import com.microsoft.teamfoundation.core.webapi.CoreHttpClient;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

/**
 * Created by madhurig on 7/21/2015.
 */
public class VSOConnection {

    private String serverUrl;
    private String collectionName;
    private String teamProjectName;
    private AuthenticationType authenticationType;
    private String userName;
    private String password;

    public VSOConnection(String serverUrl, AuthenticationType authenticationType, String userName, String password) {
        this.serverUrl = serverUrl;
        this.authenticationType = authenticationType;
        this.userName = userName;
        this.password = password;
    }

    public Client getClient() {
        //validate connection
        try {
            Client c = null;
            Credentials credentials = null;
            if (authenticationType == AuthenticationType.WINDOWS) {
                credentials = new NTCredentials(userName + ":" + password);
            } else if (authenticationType == AuthenticationType.ALTERNATE_CREDENTIALS || authenticationType == AuthenticationType.PAT) {
                credentials = new UsernamePasswordCredentials(userName, password);
            } else {
                //credentials not setup
            }
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, credentials);

            ClientConfig clientConfig = new ClientConfig().connectorProvider(new ApacheConnectorProvider());
            clientConfig.property(ApacheClientProperties.CREDENTIALS_PROVIDER, credentialsProvider);
            clientConfig.property(ApacheClientProperties.PREEMPTIVE_BASIC_AUTHENTICATION, true);

            c = ClientBuilder.newClient(clientConfig);

            return c;
        }
        catch (Exception e) {
            String message = e.getLocalizedMessage(); //TODO: how to surface connection errors to UI?
        }
        return null;

    }
}
