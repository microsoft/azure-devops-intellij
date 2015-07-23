package com.microsoft.tf.common.utils;

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
public class Connection {

    private final String serverUrl;
    private final AuthenticationType authenticationType;
    private final String userName;
    private final String password;

    public Connection(String serverUrl, AuthenticationType authenticationType, String userName, String password) {
        this.serverUrl = serverUrl;
        this.authenticationType = authenticationType;
        this.userName = userName;
        this.password = password;
    }

    public final Client getClient() {
        //validate connection
        try {
            final Credentials credentials;
            if (authenticationType == AuthenticationType.WINDOWS) {
                credentials = new NTCredentials(userName + ":" + password);
            } else if (authenticationType == AuthenticationType.ALTERNATE_CREDENTIALS || authenticationType == AuthenticationType.PERSONAL_ACCESS_TOKEN) {
                credentials = new UsernamePasswordCredentials(userName, password);
            } else {
                //credentials not setup
                credentials = null;
            }
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, credentials);

            final ClientConfig clientConfig = new ClientConfig().connectorProvider(new ApacheConnectorProvider());
            clientConfig.property(ApacheClientProperties.CREDENTIALS_PROVIDER, credentialsProvider);
            clientConfig.property(ApacheClientProperties.PREEMPTIVE_BASIC_AUTHENTICATION, true);

            final Client c = ClientBuilder.newClient(clientConfig);

            return c;
        }
        catch (Exception e) {
            //TODO: how to surface connection errors to UI?
        }
        return null;
    }
}
