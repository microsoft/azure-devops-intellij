// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcs.log.Hash;
import com.intellij.vcs.log.VcsUser;
import com.intellij.vcs.log.impl.HashImpl;
import git4idea.GitCommit;
import git4idea.GitLocalBranch;
import git4idea.GitRemoteBranch;
import git4idea.history.GitLogStatusInfo;
import git4idea.repo.GitRemote;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

public class PRGitObjectMockHelper {

    public static GitLocalBranch createLocalBranch(final String name) {
        return new GitLocalBranch(name, HashImpl.build(getSha1(name)));
    }

    public static GitRemoteBranch createRemoteBranch(final String name, final GitRemote remote) {
        Hash hash = HashImpl.build(getSha1(name));
        return new GitRemoteBranch("refs/remotes/" + name, hash) {
            @Override
            public boolean isRemote() {
                return true;
            }

            @NotNull
            @Override
            public String getNameForRemoteOperations() {
                return name;
            }

            @NotNull
            @Override
            public String getNameForLocalOperations() {
                return name;
            }

            @NotNull
            @Override
            public GitRemote getRemote() {
                return remote;
            }
        };
    }

    private static String getSha1(final String name) {
        // I don't want to generate sha1, I need crypto libs for that
        if ("origin/test1".equals(name)) {
            return "cfd40ea42910161c368956a93b623b1a8a519241";
        }

        if ("origin/test2".equals(name)) {
            return "b0169783481296ee97d7a2061d3f93c079194";
        }

        if ("origin/master".equals(name)) {
            return "9afa081effdaeafdff089b2aa3543415f6cdb1fb";
        }

        return "935b168d0601bd05d57489fae04d5c6ec439cfea";
    }

    public static GitCommit getCommit(final Project project, final VirtualFile root) {
        return getCommit(project, root, "subject", "message");
    }

    public static GitCommit getCommit(final Project project, final VirtualFile root,
                                      final String subject, final String message) {
        long date = new Date().getTime();
        VcsUser user = Mockito.mock(VcsUser.class);
        return new GitCommit(project,
                HashImpl.build("935b168d0601bd05d57489fae04d5c6ec439cfea"),
                Arrays.asList(HashImpl.build("9afa081effdaeafdff089b2aa3543415f6cdb1fb")),
                date,
                root,
                subject, user, message, user, date, Collections.<GitLogStatusInfo>emptyList());
    }
}
