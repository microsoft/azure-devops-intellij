package com.microsoft.tf.common.utils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Created by madhurig on 7/21/2015.
 */
public class UrlHelper {

    //TODO: how much validation should we do, should we handle other exceptions here?
    public static final boolean isValidServerUrl(final String serverUrl) {
        try {
            final URL url = new URL(serverUrl);
            return true;
        }
        catch(MalformedURLException e) {
            //URL is not in a valid form
        }
        return false;
    }

    public static final URI getBaseUri(final String uri) {
        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            //TODO: log details? resurface to caller?
        }
        return null;
    }
}
