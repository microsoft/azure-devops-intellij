// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.settings;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.ide.passwordSafe.PasswordSafeException;
import com.intellij.openapi.components.ServiceManager;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContext;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class TeamServicesSecrets {
    private static final Logger logger = LoggerFactory.getLogger(TeamServicesSecrets.class);

    public static TeamServicesSecrets getInstance() {
        return ServiceManager.getService(TeamServicesSecrets.class);
    }

    public static void forget(final String key) {
        forgetPassword(key);
    }

    public AuthenticationInfo load(final String key) throws IOException {
        final String authInfoSerialized = readPassword(key);

        AuthenticationInfo info = null;
        if (StringUtils.isNotEmpty(authInfoSerialized)) {
            info = JsonHelper.read(authInfoSerialized, AuthenticationInfo.class);
        }

        if (info == null) {
            forget(key);
            logger.warn("getServerContextSecrets: info was null for key: ", key);
            return null;
        }
        return info;
    }

    public void save(final ServerContext context) {
        if (context == null) {
            return;
        }

        final String key = context.getKey();
        final AuthenticationInfo authenticationInfo = context.getAuthenticationInfo();
        final String stringValue = JsonHelper.write(authenticationInfo);

        writePassword(key, stringValue);
    }

    /**
     * Forget password by overwriting existing password with null
     *
     * @param key
     */
    private static void forgetPassword(final String key) {
        writePassword(key, null);
    }

    /**
     * Writing password to the IDEA credential store
     * <p>
     * Attempt to write the password using the new IDEA method
     * If that doesn't exist it will fall back to the old way which doesn't store multiple passwords and is deprecated
     *
     * @param key
     * @param value
     */
    private static void writePassword(final String key, final String value) {
        // try writing password using the new IDEA implementation
        try {
            // use key as the serviceName (not username) for CredentialAttributes because that is the the unique identifier used to save it in the store
            final CredentialAttributes attributes = new CredentialAttributes(key);
            final Credentials credentials = new Credentials(key, value);
            PasswordSafe.getInstance().set(attributes, credentials);
        } catch (final NoClassDefFoundError error) {
            logger.warn("Failed to write password using new implementation so attempting old way", error);
            writePasswordOldWay(key, value);
        } catch (final NoSuchMethodError error) {
            logger.warn("Failed to write password using new implementation so attempting old way", error);
            writePasswordOldWay(key, value);
        }
    }

    /**
     * Old way to write passwords which is deprecated and should only be used in older version of IDEA
     *
     * @param key
     * @param value
     */
    private static void writePasswordOldWay(final String key, final String value) {
        try {
            PasswordSafe.getInstance().storePassword(null, TeamServicesSecrets.class, key, value);
        } catch (PasswordSafeException e) {
            logger.warn("Failed to write password", e);
        } catch (Throwable t) {
            logger.warn("Failed to write password", t);
        }
    }

    /**
     * Reading password from the IDEA credential store
     * <p>
     * Attempt to read the password using the new IDEA method
     * If that doesn't exist it will fall back to the old way which is deprecated
     *
     * @param key
     * @return unencrypted password or and empty string if no password is found
     */
    private static String readPassword(final String key) {
        String password = StringUtils.EMPTY;
        // try reading password using the new IDEA implementation
        try {
            final CredentialAttributes attributes = new CredentialAttributes(key);
            final Credentials credentials = PasswordSafe.getInstance().get(attributes);
            password = credentials != null ? credentials.getPasswordAsString() : password;
        } catch (final NoClassDefFoundError error) {
            logger.warn("Failed to get password using new implementation so attempting old way", error);
            password = readPasswordOldWay(key);
        } catch (final NoSuchMethodError error) {
            logger.warn("Failed to get password using new implementation so attempting old way", error);
            password = readPasswordOldWay(key);
        }
        return password;
    }

    /**
     * Old way to read passwords which is deprecated and should only be used in older version of IDEA
     *
     * @param key
     */
    private static String readPasswordOldWay(final String key) {
        try {
            return PasswordSafe.getInstance().getPassword(null, TeamServicesSecrets.class, key);
        } catch (final PasswordSafeException e) {
            logger.warn("Failed to read password", e);
        } catch (Throwable t) {
            logger.warn("Failed to read password", t);
        }
        return StringUtils.EMPTY;
    }
}
