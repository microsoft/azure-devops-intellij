// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.ui.common.helpers;


import com.microsoft.alm.core.webapi.model.TeamProjectCollectionReference;
import com.microsoft.alm.core.webapi.model.TeamProjectReference;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.common.ui.common.mocks.MockServerContext;
import com.microsoft.alm.sourcecontrol.webapi.model.GitRepository;

import java.net.URI;

public class ServerContextHelper {

    public static ServerContext getNewServerContext(String name, boolean isVso) {
        //remove spaces in the name
        final String nameNoSpaces = name.replace(" ", "");
        final String serverUri = isVso ? String.format("https://%s.visualstudio.com", name) : String.format("http://%s:8080/tfs", nameNoSpaces);
        final TeamProjectCollectionReference collection = new TeamProjectCollectionReference();
        collection.setName(String.format("Collection_%s", name));
        final TeamProjectReference teamProject = new TeamProjectReference();
        teamProject.setName(name);
        final GitRepository gitRepo = new GitRepository();
        gitRepo.setName(name);
        gitRepo.setRemoteUrl(serverUri + "/Collection/Project/_git/" + nameNoSpaces);
        gitRepo.setProjectReference(teamProject);
        final ServerContext context = new MockServerContext(isVso ? ServerContext.Type.VSO_DEPLOYMENT : ServerContext.Type.TFS, null, URI.create(serverUri), collection, teamProject, gitRepo);

        return context;
    }
}
