// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.context;

public class RepositoryContext {
    public static final int GIT = 0;
    public static final int TFVC = 1;
    private final int type;
    private final String name;
    private final String branch;
    private final String url;
    private final String commonRoot;

    public static RepositoryContext createGitContext(String repoName, String currentBranch, String remoteUrl) {
        return new RepositoryContext(GIT, repoName, currentBranch, remoteUrl, null);
    }

    public static RepositoryContext createTfvcContext(String workspaceName, String commonRoot, String serverUrl) {
        return new RepositoryContext(TFVC, workspaceName, null, serverUrl, commonRoot);
    }

    private RepositoryContext(int type, String name, String branch, String url, String commonRoot) {
        this.type = type;
        this.name = name;
        this.branch = branch;
        this.url = url;
        this.commonRoot = commonRoot;
    }

    public int getType() {
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

    public String getCommonRoot() {
        return commonRoot;
    }
}