// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context;

public class RepositoryContext {
    public enum Type {
        GIT,
        TFVC
    }
    private final String localRootFolder;
    private final Type type;
    private final String name;
    private final String branch;
    private final String url;
    private final String teamProjectName;

    public static RepositoryContext createGitContext(final String localRootFolder, final String repoName,
                                                     final String currentBranch, final String remoteUrl) {
        return new RepositoryContext(localRootFolder, Type.GIT, repoName, currentBranch, remoteUrl, null);
    }

    public static RepositoryContext createTfvcContext(final String localRootFolder, final String workspaceName,
                                                      final String teamProjectName, final String serverUrl) {
        return new RepositoryContext(localRootFolder, Type.TFVC, workspaceName, null, serverUrl, teamProjectName);
    }

    private RepositoryContext(final String localRootFolder, final Type type, final String name, final String branch,
                              final String url, final String teamProjectName) {
        this.localRootFolder = localRootFolder;
        this.type = type;
        this.name = name;
        this.branch = branch;
        this.url = url;
        this.teamProjectName = teamProjectName;
    }

    public String getLocalRootFolder() {
        return localRootFolder;
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getBranch() {
        return branch;
    }

    public String getUrl() {
        return url;
    }

    public String getTeamProjectName() {
        return teamProjectName;
    }
}