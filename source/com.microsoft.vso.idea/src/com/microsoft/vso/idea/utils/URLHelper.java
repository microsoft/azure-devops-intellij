package com.microsoft.vso.idea.utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by madhurig on 7/21/2015.
 */
public class URLHelper {
    
    public static boolean isValidServerUrl(String serverUrl) {
        boolean isValid = false;
        try {
            URL url = new URL(serverUrl);
            URLConnection conn = url.openConnection();
            isValid = true;
        }
        catch(MalformedURLException e) {
            //URL is not in a valid form
        }
        catch(IOException e) {
            //connection couldn't be established
        }
        return isValid;
    }
}
