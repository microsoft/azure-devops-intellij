package com.microsoft.vso.idea.models;

import com.microsoft.vso.idea.utils.VSOConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

/**
 * Created by madhurig on 7/21/2015.
 */
public class VSOConnectionsModel extends Observable {

    private List<VSOConnection> vsoConnections;

    public VSOConnectionsModel() {
        vsoConnections = new ArrayList<VSOConnection>();
    }

    public synchronized void addConnection(VSOConnection connection) {
        vsoConnections.add(connection);
        setChanged();
        notifyObservers(vsoConnections);
    }

}


