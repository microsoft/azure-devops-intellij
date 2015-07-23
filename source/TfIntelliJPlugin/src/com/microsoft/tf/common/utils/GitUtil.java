package com.microsoft.tf.common.utils;

import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;

/**
 * Created by jasholl on 7/21/2015.
 */
public class GitUtil {

    /**
     * Returns <code>true</code> if the specified GitRepository is a VSO GitRepository.
     *
     * @param gitRepository must not be <code>null</code>
     * @return
     */
    public static boolean isVSOGitRepository(GitRepository gitRepository) {
        if(gitRepository == null){
            throw new NullPointerException();
        }
        for (GitRemote gitRemote : gitRepository.getRemotes()) {
            for (String remoteUrl : gitRemote.getUrls()) {
                if (remoteUrl.contains(".visualstudio.com/") || remoteUrl.contains("/_git/")) {
                    return true; //TODO: how to detect VSO URLs for onprem
                }
            }
        }
        return false;
    }

    /**
     * Returns the VSO URL for the specified GitRepository if it is a VSO GitRepository.
     *
     * @param gitRepository must not be <code>null</code>
     * @return
     */
    public static String getVSOUrl(GitRepository gitRepository) {
        String url = "https://www.visualstudio.com/";
        for (GitRemote gitRemote : gitRepository.getRemotes()) {
            for (String remoteUrl : gitRemote.getUrls()) {
                url = remoteUrl;
            }
        }
        return url;
    }
}
