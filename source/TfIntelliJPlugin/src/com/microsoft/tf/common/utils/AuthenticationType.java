package com.microsoft.tf.common.utils;

/**
 * Created by madhurig on 7/21/2015.
 */
public enum AuthenticationType {
    WINDOWS, //NTLM
    ALTERNATE_CREDENTIALS, //basic auth with user name, password
    PERSONAL_ACCESS_TOKEN, //Personal Access Token
}
