package com.microsoft.tf.common.utils;

import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.client.Client;

import static org.junit.Assert.*;

/**
 * Created by dastahel on 7/23/2015.
 */
public class ConnectionTest {

    @Test(expected = IllegalArgumentException.class)
    public void testConnectionCtor_NullServerUrl() throws Exception {
        new Connection(
                null,
                AuthenticationType.WINDOWS,
                null,
                "myPassword");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConnectionCtor_EmptyServerUrl() throws Exception {
        new Connection(
                "",
                AuthenticationType.WINDOWS,
                null,
                "myPassword");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConnectionCtor_WhiteSpaceServerUrl() throws Exception {
        new Connection(
                "  ",
                AuthenticationType.WINDOWS,
                null,
                "myPassword");
    }

    @Test
    public void testConnectionCtor_WindowsCredentials_NullCreds() throws Exception {
        new Connection(
                "http://test.test.com",
                AuthenticationType.WINDOWS,
                null,
                null);
    }

    @Test
    public void testConnectionCtor_WindowsCredentials_NonNullCreds() throws Exception {
        new Connection(
                "http://test.test.com",
                AuthenticationType.WINDOWS,
                "myDomain\\myUsername",
                "myPassword");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConnectionCtor_WindowsCredentials_NonNullCreds_NullUsername() throws Exception {
        new Connection(
                "http://test.test.com",
                AuthenticationType.WINDOWS,
                null,
                "myPassword");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConnectionCtor_WindowsCredentials_NonNullCreds_EmptyUsername() throws Exception {
        new Connection(
                "http://test.test.com",
                AuthenticationType.WINDOWS,
                "",
                "myPassword");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConnectionCtor_WindowsCredentials_NonNullCreds_WhiteSpaceUsername() throws Exception {
        new Connection(
                "http://test.test.com",
                AuthenticationType.WINDOWS,
                "  ",
                "myPassword");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConnectionCtor_WindowsCredentials_NonNullCreds_NullPassword() throws Exception {
        new Connection(
                "http://test.test.com",
                AuthenticationType.WINDOWS,
                "myDomain\\myUsername",
                null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConnectionCtor_WindowsCredentials_NonNullCreds_EmptyPassword() throws Exception {
        new Connection(
                "http://test.test.com",
                AuthenticationType.WINDOWS,
                "myDomain\\myUsername",
                "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConnectionCtor_WindowsCredentials_NonNullCreds_WhiteSpacePassword() throws Exception {
        new Connection(
                "http://test.test.com",
                AuthenticationType.WINDOWS,
                "myDomain\\myUsername",
                "  ");
    }

    @Test
    public void testConnectionCtor_AlternateCredentials() throws Exception {
        new Connection(
                "http://test.test.com",
                AuthenticationType.ALTERNATE_CREDENTIALS,
                "myUserName",
                "myPassword");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConnectionCtor_AlternateCredentials_NullPassword() throws Exception {
        new Connection(
                "http://test.test.com",
                AuthenticationType.ALTERNATE_CREDENTIALS,
                "myUsername",
                null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConnectionCtor_AlternateCredentials_EmptyPassword() throws Exception {
        new Connection(
                "http://test.test.com",
                AuthenticationType.ALTERNATE_CREDENTIALS,
                "myUsername",
                "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConnectionCtor_AlternateCredentials_WhiteSpacePassword() throws Exception {
        new Connection(
                "http://test.test.com",
                AuthenticationType.ALTERNATE_CREDENTIALS,
                "myUsername",
                "  ");
    }

    @Test
    public void testConnectionCtor_PersonalAccessToken_NonEmptyUsername() throws Exception {
        new Connection(
                "http://test.test.com",
                AuthenticationType.PERSONAL_ACCESS_TOKEN,
                "myUsername",
                "12345");
    }

    @Test
    public void testConnectionCtor_PersonalAccessToken_EmptyUsername() throws Exception {
        new Connection(
                "http://test.test.com",
                AuthenticationType.PERSONAL_ACCESS_TOKEN,
                "",
                "12345");
    }

    @Test
    public void testConnectionCtor_PersonalAccessToken_WhiteSpaceUsername() throws Exception {
        new Connection(
                "http://test.test.com",
                AuthenticationType.PERSONAL_ACCESS_TOKEN,
                "  ",
                "12345");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConnectionCtor_PersonalAccessToken_NullPassword() throws Exception {
        new Connection(
                "http://test.test.com",
                AuthenticationType.PERSONAL_ACCESS_TOKEN,
                "",
                null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConnectionCtor_PersonalAccessToken_EmptyPassword() throws Exception {
        new Connection(
                "http://test.test.com",
                AuthenticationType.PERSONAL_ACCESS_TOKEN,
                "",
                "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConnectionCtor_PersonalAccessToken_WhiteSpacePassword() throws Exception {
        new Connection(
                "http://test.test.com",
                AuthenticationType.PERSONAL_ACCESS_TOKEN,
                "",
                "  ");
    }

    @Test
    public void testGetClient_WindowsCredentials() throws Exception {
        Connection connection = new Connection(
                "http://test.test.com",
                AuthenticationType.WINDOWS,
                null,
                null);

        Client client = connection.getClient();

        Assert.assertNotNull(client);
    }

    @Test
    public void testGetClient_AlternateCredentials() throws Exception {
        Connection connection = new Connection(
                "http://test.test.com",
                AuthenticationType.ALTERNATE_CREDENTIALS,
                "myUsername",
                "myPassword");

        Client client = connection.getClient();

        Assert.assertNotNull(client);
    }

    @Test
    public void testGetClient_PersonalAccessToken() throws Exception {
        Connection connection = new Connection(
                "http://test.test.com",
                AuthenticationType.PERSONAL_ACCESS_TOKEN,
                "",
                "12345");

        Client client = connection.getClient();

        Assert.assertNotNull(client);
    }
}