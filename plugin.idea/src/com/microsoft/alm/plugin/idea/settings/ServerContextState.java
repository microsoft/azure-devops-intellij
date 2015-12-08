// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.settings;

import com.microsoft.alm.common.utils.UrlHelper;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.context.ServerContextBuilder;
import com.microsoft.teamfoundation.core.webapi.model.TeamProjectCollectionReference;
import com.microsoft.teamfoundation.core.webapi.model.TeamProjectReference;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitRepository;

import java.io.IOException;

public class ServerContextState {
    public ServerContextState() {
    }

    public ServerContextState(final ServerContext context) {
        this();
        this.type = context.getType();
        this.uri = UrlHelper.asString(context.getUri());
        this.teamProjectCollectionReference = JsonHelper.write(restrict(context.getTeamProjectCollectionReference()));
        this.teamProjectReference = JsonHelper.write(restrict(context.getTeamProjectReference()));
        this.gitRepository = JsonHelper.write(context.getGitRepository());
    }

    public ServerContextBuilder createBuilder() throws IOException {
        return new ServerContextBuilder()
                .type(this.type)
                .collection(JsonHelper.read(this.teamProjectCollectionReference, TeamProjectCollectionReference.class))
                .teamProject(JsonHelper.read(this.teamProjectReference, TeamProjectReference.class))
                .repository(JsonHelper.read(this.gitRepository, GitRepository.class));
    }

    //fields have to be public, so IntelliJ can write them to the persistent store
    public ServerContext.Type type = null;
    public String uri = null;
    public String teamProjectCollectionReference = null;
    public String teamProjectReference = null;
    public String gitRepository = null;

    // This method exists to make sure we can deserialize the collection reference.
    private TeamProjectCollectionReference restrict(final TeamProjectCollectionReference reference) {
        final TeamProjectCollectionReference newReference = new TeamProjectCollectionReference();
        if (reference != null) {
            newReference.setName(reference.getName());
            newReference.setId(reference.getId());
            newReference.setUrl(reference.getUrl());
        }
        return newReference;
    }

    // This method exists to make sure we can deserialize the project reference.
    private TeamProjectReference restrict(final TeamProjectReference reference) {
        final TeamProjectReference newReference = new TeamProjectReference();
        if (reference != null) {
            newReference.setName(reference.getName());
            newReference.setId(reference.getId());
            newReference.setUrl(reference.getUrl());
            newReference.setAbbreviation(reference.getAbbreviation());
            newReference.setDescription(reference.getDescription());
            newReference.setRevision(reference.getRevision());
            newReference.setState(reference.getState());
        }
        return newReference;
    }
}
