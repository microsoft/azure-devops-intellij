package com.microsoft.tf.core.models;

import com.microsoft.tf.common.utils.Connection;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

/**
 * Created by madhurig on 7/21/2015.
 */
public class ConnectionsModel extends Observable {

    private final static List<Connection> connections = new ArrayList<Connection>();

    public ConnectionsModel() {
    }

    //example where model notifies observers with some data when changed
    public final void addConnection(final Connection connection) {
        connections.add(connection);
        setChanged();
        notifyObservers(connections);
    }

}


