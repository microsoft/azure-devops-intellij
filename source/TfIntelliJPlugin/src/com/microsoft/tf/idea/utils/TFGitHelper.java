package com.microsoft.tf.idea.utils;

import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import org.jetbrains.annotations.NotNull;

/**
 * Created by jasholl on 7/21/2015.
 */
public class TFGitHelper {

    /**
     * Returns <code>true</code> if the specified GitRepository is a TF GitRepository (VSO or OnPrem).
     *
     * @param gitRepository must not be <code>null</code>
     * @return
     */
    public static boolean isTFGitRepository(@NotNull final GitRepository gitRepository) {
        if(gitRepository == null){
            throw new NullPointerException();
        }

        final String remoteUrl = getFirstRemoteUrl(gitRepository);
        if (remoteUrl != null && (remoteUrl.contains(".visualstudio.com/") || remoteUrl.contains(".tfsallin.net/") || remoteUrl.contains("/_git/"))) {
            //TODO this is a placeholder hack a the moment until we figure out how to interrogate the server & cache the result
            return true;
        }
        return false;
    }

    /**
     * Returns the first remote URL or <code>null</code> if non exist
     *
     * @param gitRepository must not be <code>null</code>
     * @return
     */
    public static String getFirstRemoteUrl(@NotNull final GitRepository gitRepository) {
        if(gitRepository == null){
            throw new NullPointerException();
        }

        for (GitRemote gitRemote : gitRepository.getRemotes()) {
            return gitRemote.getFirstUrl();
        }

        return null;
    }
}
