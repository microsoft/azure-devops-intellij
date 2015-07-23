package com.microsoft.tf.core.controllers;

import com.microsoft.teamfoundation.core.webapi.CoreHttpClient;
import com.microsoft.tf.common.utils.AuthenticationType;
import com.microsoft.tf.common.utils.Connection;
import com.microsoft.tf.common.utils.UrlHelper;
import com.microsoft.tf.core.models.ConnectionsModel;

import javax.ws.rs.client.Client;
import java.util.Observer;


/**
 * Created by madhurig on 7/21/2015.
 */
public class ConnectionsManager {

    private final ConnectionsModel connectionsModel;

    private ConnectionsManager() {
        connectionsModel = new ConnectionsModel();
    }

    private static final ConnectionsManager myInstance = new ConnectionsManager();

    public static final ConnectionsManager getInstance() {
        return myInstance;
    }

    public final void registerObserver(Observer observer) {
        connectionsModel.addObserver(observer);
    }

    public final void addConnection(final String serverUrl, final AuthenticationType authenticationType, final String userName, final String password) {
        final Connection connection = new Connection(serverUrl, authenticationType, userName, password);
        final Client c = connection.getClient();
        final CoreHttpClient cc = new CoreHttpClient(c, UrlHelper.getBaseUri(serverUrl));
        connectionsModel.addConnection(connection);
    }
}
