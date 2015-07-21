package com.microsoft.vso.idea.models;

import com.microsoft.vso.idea.utils.VSOConnection;

/**
 * Created by madhurig on 7/21/2015.
 */
public class VSOAuthData extends java.util.Observable {

    private VSOConnection vsoConnection;

    public VSOAuthData() {

    }

    public void setVsoConnection(VSOConnection connection) {
        vsoConnection = connection;
        setChanged();
        notifyObservers(vsoConnection);
    }
}
