// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitCommit;
import git4idea.GitRevisionNumber;
import git4idea.changes.GitChangeUtils;
import git4idea.history.GitHistoryUtils;
import git4idea.repo.GitRepository;
import git4idea.util.GitCommitCompareInfo;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Compare information provider
 *
 * Calculate commits and diff information from git4idea utilities
 */
public class DiffCompareInfoProvider {

    private GitUtilWrapper utilWrapper;

    public DiffCompareInfoProvider() {
        this.utilWrapper = new GitUtilWrapper();
    }

    public GitCommitCompareInfo getBranchCompareInfo(final Project project, final GitRepository gitRepository,
                                                     final String source, final String target)
            throws VcsException {
        final GitRevisionNumber commonParentRevision = getUtilWrapper().getMergeBase(project,
                gitRepository.getRoot(), target, source);

        final String commonParentHash = (commonParentRevision != null) ? commonParentRevision.getRev() : null;

        if (commonParentHash == null) {
            return getEmptyDiff(gitRepository);
        }

        return getCompareInfo(project, gitRepository, source, commonParentHash);
    }

    private GitCommitCompareInfo getCompareInfo(final Project project, final GitRepository gitRepository,
                                                final String source, final String target)
            throws VcsException {
        final VirtualFile root = gitRepository.getRoot();
        final List<GitCommit> commits1 = getUtilWrapper().history(project, root, ".." + target);
        final List<GitCommit> commits2 = getUtilWrapper().history(project, root, target + "..");

        final Collection<Change> diff = getUtilWrapper().getDiff(project, root, target, source);
        final GitCommitCompareInfo info = new GitCommitCompareInfo(GitCommitCompareInfo.InfoType.BRANCH_TO_HEAD);

        info.put(gitRepository, diff);
        info.put(gitRepository, new Pair<List<GitCommit>, List<GitCommit>>(commits1, commits2));

        return info;
    }

    /**
     * Return zero-length list of commits and diffs
     *
     * This doesn't mean the GitCommitCompareInfo's isEmpty() method will return true, it considers an zero-length
     * list as a diff still
     * @param gitRepository
     * @return compare info which contains empty commits and diff lists
     */
    public GitCommitCompareInfo getEmptyDiff(final GitRepository gitRepository) {
        final GitCommitCompareInfo emptyCompareInfo = new GitCommitCompareInfo(GitCommitCompareInfo.InfoType.BRANCH_TO_HEAD);
        emptyCompareInfo.put(gitRepository,
                new Pair<List<GitCommit>, List<GitCommit>>(Collections.<GitCommit>emptyList(), Collections.<GitCommit>emptyList()));
        emptyCompareInfo.put(gitRepository, Collections.<Change>emptyList());

        return emptyCompareInfo;
    }

    /* wrap around static method for better unit testing */
    /* default */
    static class GitUtilWrapper {
        public List<GitCommit> history(final Project project, final VirtualFile root, String parameters)
                throws VcsException {
            return GitHistoryUtils.history(project, root, parameters);
        }

        public GitRevisionNumber getMergeBase(final Project project, final VirtualFile root,
                                              String target, String source)
                throws VcsException {
            return GitHistoryUtils.getMergeBase(project, root, target, source);
        }

        public Collection<Change> getDiff(final Project project, final VirtualFile root,
                                          String target, String source)
                throws VcsException {
            return GitChangeUtils.getDiff(project, root, target, source, null);
        }
    }

    public GitUtilWrapper getUtilWrapper() {
        return utilWrapper;
    }

    public void setUtilWrapper(GitUtilWrapper utilWrapper) {
        this.utilWrapper = utilWrapper;
    }
}
