// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.ui.pullrequest;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcs.log.VcsUser;
import com.intellij.vcs.log.impl.HashImpl;
import git4idea.GitCommit;
import git4idea.GitLocalBranch;
import git4idea.GitRemoteBranch;
import git4idea.history.GitLogStatusInfo;
import git4idea.repo.GitRemote;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PRGitObjectMockHelper {

    public static GitLocalBranch createLocalBranch(final String name) {
        GitLocalBranch branch = mock(GitLocalBranch.class);
        when(branch.getName()).thenReturn(name);
        when(branch.getFullName()).thenReturn(name);
        when(branch.isRemote()).thenReturn(false);
        return branch;
    }

    public static GitRemoteBranch createRemoteBranch(final String name, final GitRemote remote) {
        GitRemoteBranch branch = mock(GitRemoteBranch.class);
        when(branch.getName()).thenReturn("refs/remotes/" + name);
        when(branch.getFullName()).thenReturn(name);
        when(branch.isRemote()).thenReturn(true);
        when(branch.getRemote()).thenReturn(remote);
        when(branch.getNameForLocalOperations()).thenReturn(name);
        when(branch.getNameForRemoteOperations()).thenReturn(name);
        return branch;
    }

    public static GitCommit getCommit(final Project project, final VirtualFile root) {
        return getCommit(project, root, "subject", "message");
    }

    public static GitCommit getCommit(final Project project, final VirtualFile root,
                                      final String subject, final String message) {
        return getCommit(project, root, subject, message, "935b168d0601bd05d57489fae04d5c6ec439cfea");
    }

    public static GitCommit getCommit(final Project project, final VirtualFile root,
                                      final String subject, final String message, final String hash) {
        long date = new Date().getTime();
        VcsUser user = mock(VcsUser.class);
        return new GitCommit(project,
                HashImpl.build(hash),
                Arrays.asList(HashImpl.build("9afa081effdaeafdff089b2aa3543415f6cdb1fb")),
                date,
                root,
                subject, user, message, user, date, Collections.<GitLogStatusInfo>emptyList());
    }
}
