// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.common.helpers;


import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.ui.common.mocks.MockServerContext;
import com.microsoft.teamfoundation.core.webapi.model.TeamProjectCollectionReference;
import com.microsoft.teamfoundation.core.webapi.model.TeamProjectReference;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitRepository;

import java.net.URI;

public class ServerContextHelper {

    public static ServerContext getNewServerContext(String name, boolean isVso) {
        //remove spaces in the name
        final String nameNoSpaces = name.replace(" ", "");
        final String serverUri = isVso? String.format("https://%s.visualstudio.com", name) : String.format("http://%s:8080/tfs", nameNoSpaces);
        final ServerContext context = new MockServerContext(null, URI.create(serverUri));

        final TeamProjectCollectionReference collection = new TeamProjectCollectionReference();
        collection.setName(String.format("Collection_%s", name));
        context.setTeamProjectCollectionReference(collection);

        final TeamProjectReference teamProject = new TeamProjectReference();
        teamProject.setName(name);
        context.setTeamProjectReference(teamProject);

        final GitRepository gitRepo = new GitRepository();
        gitRepo.setName(name);
        gitRepo.setRemoteUrl(serverUri + "/Collection/Project/_git/" + nameNoSpaces);
        gitRepo.setProjectReference(teamProject);
        context.setGitRepository(gitRepo);

        return context;
    }
}
