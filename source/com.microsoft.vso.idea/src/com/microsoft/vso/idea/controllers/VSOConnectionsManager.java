package com.microsoft.vso.idea.controllers;

import com.microsoft.teamfoundation.core.webapi.CoreHttpClient;
import com.microsoft.vso.idea.models.VSOConnectionsModel;
import com.microsoft.vso.idea.ui.VSOConnectionsDialog;
import com.microsoft.vso.idea.ui.VSOLoginDialog;
import com.microsoft.vso.idea.utils.AuthenticationType;
import com.microsoft.vso.idea.utils.URLHelper;
import com.microsoft.vso.idea.utils.VSOConnection;

import javax.jnlp.ServiceManager;
import javax.ws.rs.client.Client;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * Created by madhurig on 7/21/2015.
 */
public class VSOConnectionsManager {

    VSOConnectionsModel connectionsModel;
    VSOConnectionsDialog connectionsDialog;
    VSOLoginDialog vsoLoginDialog;

    public VSOConnectionsManager() {
        connectionsModel = new VSOConnectionsModel();

        //register the observers for the model
        connectionsDialog = new VSOConnectionsDialog();
        vsoLoginDialog = new VSOLoginDialog();
        connectionsModel.addObserver(connectionsDialog);
        connectionsModel.addObserver(vsoLoginDialog);
    }

    public void addConnection(String serverUrl, AuthenticationType authenticationType, String userName, String password) {
        VSOConnection connection = new VSOConnection(serverUrl, authenticationType, userName, password);
        Client c = connection.getClient();
        CoreHttpClient cc = new CoreHttpClient(c, URLHelper.getBaseUri(serverUrl));
        connectionsModel.addConnection(connection);
    }
}
