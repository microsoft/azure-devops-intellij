package com.microsoft.tf.idea.resources;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

/**
 * Created by madhurig on 7/18/2015.
 */
public class Icons {

    private static final Icon load(String path) {
        return IconLoader.getIcon(path);
    }

    public static final Icon VSLogo = load("/vs-logo.png");

}
