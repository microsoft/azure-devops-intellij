// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication;

import com.microsoft.visualstudio.services.authentication.DelegatedAuthorization.webapi.model.SessionToken;
import com.microsoftopentechnologies.auth.AuthenticationResult;
import com.microsoftopentechnologies.auth.UserInfo;

/**
 * This implementation of the AuthenticationInfo interface represents the VSO authentication information.
 * It is serializable to and deserializable from Json
 */
public class VsoAuthenticationInfo implements AuthenticationInfo {
    private final AuthenticationResultInfo authenticationResultInfo;
    private final SessionToken sessionToken;
    private final String userName;
    private final String password;
    private final String bearerToken;
    private final String serverUri;
    private final String userNameForDisplay;

    /**
     * Default constructor to enable serialization and deserialization
     */
    public VsoAuthenticationInfo() {
        this.serverUri = "";
        this.authenticationResultInfo = null;
        this.sessionToken = null;
        this.userName = "";
        this.password = "";
        this.bearerToken = "";
        this.userNameForDisplay = "";
    }

    public VsoAuthenticationInfo(final String serverUri, final AuthenticationResult authenticationResult,
                                 final SessionToken sessionToken) {
        assert authenticationResult != null;
        assert serverUri != null;

        this.authenticationResultInfo = new AuthenticationResultInfo(authenticationResult);
        this.sessionToken = sessionToken;
        this.serverUri = serverUri;
        this.userName = authenticationResultInfo.uniqueName;
        this.password = sessionToken != null ? sessionToken.getToken() : "";
        this.bearerToken = authenticationResultInfo != null ? authenticationResultInfo.accessToken : "";
        this.userNameForDisplay = authenticationResultInfo != null ?
                authenticationResultInfo.givenName + " " + authenticationResultInfo.familyName : userName;
    }

    @Override
    public String getServerUri() {
        return serverUri;
    }

    @Override
    public String getUserName() {
        return userName;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public String getBearerToken() { return  bearerToken; }

    @Override
    public String getUserNameForDisplay() {
        return userNameForDisplay;
    }

    public AuthenticationResultInfo getAuthenticationResultInfo() {
        return authenticationResultInfo;
    }

    public SessionToken getSessionToken() {
        return sessionToken;
    }

    /**
     * Serializable version of AuthenticationResultInfo
     */
    class AuthenticationResultInfo {
        private final String accessTokenType;
        private final String accessToken;
        private final String refreshToken;
        private final long expiresOn;
        private final String resource;

        //UserInfo fields
        private final String userId;
        private final String givenName;
        private final String familyName;
        private final String identityProvider;
        private final String upn;
        private final String uniqueName;
        private final String tenantId;

        /**
         * Default constructor to enable serialization
         */
        public AuthenticationResultInfo() {
            this.accessTokenType = "";
            this.accessToken = "";
            this.refreshToken = "";
            this.expiresOn = 0;
            this.resource = "";
            this.userId = "";
            this.givenName = "";
            this.familyName = "";
            this.identityProvider = "";
            this.upn = "";
            this.uniqueName = "";
            this.tenantId = "";
        }

        public AuthenticationResultInfo(final AuthenticationResult authenticationResult) {
            this.accessTokenType = authenticationResult.getAccessTokenType();
            this.accessToken = authenticationResult.getAccessToken();
            this.refreshToken = authenticationResult.getRefreshToken();
            this.expiresOn = authenticationResult.getExpiresOn();
            this.resource = authenticationResult.getResource();

            //User info
            final UserInfo userInfo = authenticationResult.getUserInfo();
            if (userInfo != null) {
                this.userId = userInfo.getUserId();
                this.givenName = userInfo.getGivenName();
                this.familyName = userInfo.getFamilyName();
                this.identityProvider = userInfo.getIdentityProvider();
                this.upn = userInfo.getUpn();
                this.uniqueName = userInfo.getUniqueName();
                this.tenantId = userInfo.getTenantId();
            } else {
                this.userId = "";
                this.givenName = "";
                this.familyName = "";
                this.identityProvider = "";
                this.upn = "";
                this.uniqueName = "";
                this.tenantId = "";
            }
        }

        public String getAccessTokenType() {
            return accessTokenType;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public long getExpiresOn() {
            return expiresOn;
        }

        public String getResource() {
            return resource;
        }

        public String getUserId() {
            return userId;
        }

        public String getGivenName() {
            return givenName;
        }

        public String getFamilyName() {
            return familyName;
        }

        public String getIdentityProvider() {
            return identityProvider;
        }

        public String getUpn() {
            return upn;
        }

        public String getUniqueName() {
            return uniqueName;
        }

        public String getTenantId() {
            return  tenantId;
        }
    }
}
