// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.ide.passwordSafe.PasswordSafeException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.authentication.AuthenticationInfo;
import com.microsoft.alm.plugin.authentication.TfsAuthenticationInfo;
import com.microsoft.alm.plugin.authentication.VsoAuthenticationInfo;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContext.Type;
import com.microsoft.teamfoundation.core.webapi.model.TeamProjectCollectionReference;
import com.microsoft.teamfoundation.core.webapi.model.TeamProjectReference;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitRepository;
import com.microsoft.visualstudio.services.account.webapi.model.Account;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

/**
 * Stores a ServerContext to file and handles writing and reading the objects
 */
@State(
        name="TfServers",
        storages = {@Storage(
                file = StoragePathMacros.APP_CONFIG + "/tf_settings.xml")}
)
public class ServerContextSettings implements PersistentStateComponent<ServerContextSettings.ServerContextStore> {

    private static final Logger logger = LoggerFactory.getLogger(ServerContextSettings.class);

    private ServerContextStore state = new ServerContextStore();

    private final ObjectMapper mapper = new ObjectMapper();

    public void loadState(final ServerContextStore state) {
        this.state = state;
    }

    public ServerContextStore getState() {
        return state;
    }

    public static class ServerContextStore {
        //fields have to be public, so IntelliJ can write them to the persistent store
        public Type type = null;
        public String uri = null;
        public String account = null;
        public String teamProjectCollectionReference = null;
        public String teamProjectReference = null;
        public String gitRepository = null;
    }

    // This default instance is only returned in the case of tests or we are somehow running outside of IntelliJ
    private static ServerContextSettings DEFAULT_INSTANCE = new ServerContextSettings();

    public static ServerContextSettings getInstance() {
        if (ApplicationManager.getApplication() != null) {
            return ServiceManager.getService(ServerContextSettings.class);
        } else {
            return DEFAULT_INSTANCE;
        }
    }

    public static String getKey(final ServerContext context) {
        if (context != null) {
            return getKey(context.getUri());
        }

        return "";
    }

    public static String getKey(final String uriString) {
        if (!StringUtils.isEmpty(uriString)) {
            return getKey(UrlHelper.getBaseUri(uriString));
        }

        return "";
    }

    public static String getKey(final URI uri) {
        if (uri != null) {
            return uri.getHost();
        }

        return "";
    }


    public void saveServerContext(final String hostKey, final ServerContext context) {
        if(context != ServerContext.NO_CONTEXT) {
            setType(context.getType());
            setUri(context.getUri().toString());
            setAccount(context.getAccount());
            setTeamProjectCollectionReference(context.getTeamProjectCollectionReference());
            setTeamProjectReference(context.getTeamProjectReference());
            setGitRepository(context.getGitRepository());
            setAuthenticationInfo(hostKey, context.getAuthenticationInfo());
        }
    }

    public ServerContext getServerContext(final String hostKey) {
        final String savedUri = getUri();
        if(savedUri != null && (StringUtils.isBlank(hostKey) ||
                StringUtils.equalsIgnoreCase(getKey(savedUri), hostKey))) {
            final Type type = getType();
            final Account account = getAccount();
            final AuthenticationInfo info = getAuthenticationInfo(getKey(savedUri));
            if(info != null) {
                ServerContext context = null;
                if (type == Type.VSO_DEPLOYMENT) {
                    context = ServerContext.createVSODeploymentContext(account, (VsoAuthenticationInfo) info);
                }
                else if (type == Type.VSO) {
                    context = ServerContext.createVSOContext(account, (VsoAuthenticationInfo) info);
                }
                else if (type == Type.TFS) {
                    context = ServerContext.createTFSContext(UrlHelper.getBaseUri(savedUri), (TfsAuthenticationInfo) info);
                }

                if(context != null) {
                    context.setTeamProjectCollectionReference(getTeamProjectCollectionReference());
                    context.setTeamProjectReference(getTeamProjectReference());
                    context.setGitRepository(getGitRepository());
                    return context;
                }
            }

        }
        return ServerContext.NO_CONTEXT;
    }

    public void forgetServerContext(final String hostKey) {
        if(StringUtils.isNotBlank(state.uri)) {
            final String savedKey = getKey(state.uri);
            if(StringUtils.equalsIgnoreCase(hostKey, savedKey)) {
                //clear out all fields in the State
                state.type = null;
                state.uri = null;
                state.account = null;
                state.teamProjectCollectionReference = null;
                state.teamProjectReference = null;
                state.gitRepository = null;

                try {
                    PasswordSafe.getInstance().removePassword(null, ServerContextSettings.class, hostKey);
                } catch(PasswordSafeException e) {
                    logger.warn("Failed to clear password store", e);
                }
            }
        }
    }

    private void setType(final Type type) {
        state.type = type;
    }

    private Type getType() {
        return state.type;
    }

    private void setUri(final String uri) {
        state.uri = uri;
    }

    private String getUri() {
        return state.uri;
    }

    private void setAccount(final Account account) {
        state.account = writeToJson(account);
    }

    private Account getAccount() {
        return readFromJson(state.account, Account.class);
    }

    private void setTeamProjectCollectionReference(final TeamProjectCollectionReference collectionReference) {
        state.teamProjectCollectionReference = writeToJson(collectionReference);
    }

    private TeamProjectCollectionReference getTeamProjectCollectionReference() {
        return readFromJson(state.teamProjectCollectionReference, TeamProjectCollectionReference.class);
    }

    private void setTeamProjectReference(final TeamProjectReference teamProjectReference) {
        state.teamProjectReference = writeToJson(teamProjectReference);
    }

    private TeamProjectReference getTeamProjectReference() {
        return readFromJson(state.teamProjectReference, TeamProjectReference.class);
    }

    private void setGitRepository(final GitRepository gitRepository) {
        state.gitRepository = writeToJson(gitRepository);
    }

    private GitRepository getGitRepository() {
        return readFromJson(state.gitRepository, GitRepository.class);
    }

    private void setAuthenticationInfo(final String hostKey, final AuthenticationInfo authInfo) {
        final String value;
        if(getType() == Type.VSO || getType() == Type.VSO_DEPLOYMENT) {
            value = writeToJson((VsoAuthenticationInfo) authInfo);
        } else {
            value = writeToJson((TfsAuthenticationInfo) authInfo);
        }
        writePassword(null, ServerContextSettings.class, hostKey, value);
    }

    private AuthenticationInfo getAuthenticationInfo(final String hostKey) {
        final String authInfoSerialized = readPassword(null, ServerContextSettings.class, hostKey);

        AuthenticationInfo info = null;
        if (StringUtils.isNotEmpty(authInfoSerialized)) {
            if (getType() == Type.VSO_DEPLOYMENT || getType() == Type.VSO) {
                info = readFromJson(authInfoSerialized, VsoAuthenticationInfo.class);
            }
            if (getType() == Type.TFS) {
                info = readFromJson(authInfoSerialized, TfsAuthenticationInfo.class);
            }
        }

        if (info == null) {
            forgetServerContext(hostKey);
            logger.warn("getAuthenticationInfo: info was null for hostKey ", hostKey);
            return null;
        }
        return info;
    }

    private <T> String writeToJson(final T object) {
        String json = null;
        if(object != null) {
            try {
                json = mapper.writeValueAsString(object);
            } catch (JsonProcessingException e) {
                logger.warn("Failed to convert to string", e);
            }
        }
        return json;
    }

    private <T> T readFromJson(final String json, final Class<T> valueType) {
        T object = null;
        if(json != null && valueType != null) {
            try {
                object = mapper.readValue(json, valueType);
            } catch (JsonProcessingException e) {
                logger.warn("Failed to parse object from json", e);
                //TODO: should we forget server context? was file corrupted?
            } catch (IOException e) {
                logger.warn("Failed to get object from json", e);
            }
        }
        return object;
    }

    private <T> void writePassword(final Project project, final Class<T> valueType, final String key, final String value) {
        try {
            PasswordSafe.getInstance().storePassword(project, valueType, key, value);
        } catch(PasswordSafeException e) {
            logger.warn("Failed to get password", e);
        }
    }

    private <T> String readPassword(final Project project, final Class<T> valueType, final String key) {
        String password = null;
        try {
            password = PasswordSafe.getInstance().getPassword(project, valueType, key);
        } catch(PasswordSafeException e) {
            logger.warn("Failed to read password", e);
        }
        return password;
    }

}
