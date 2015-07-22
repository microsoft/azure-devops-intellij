package com.microsoft.vso.idea.models;

import com.microsoft.vso.idea.utils.VSOConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

/**
 * Created by madhurig on 7/21/2015.
 */
public class VSOConnectionsModel extends Observable {

    private static List<VSOConnection> vsoConnections = new ArrayList<VSOConnection>();

    public VSOConnectionsModel() {
    }

    public synchronized void addConnection(VSOConnection connection) {
        vsoConnections.add(connection);
        setChanged();
        notifyObservers(vsoConnections);
    }

}


