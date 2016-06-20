// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.utils;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.common.utils.ArgumentHelper;
import git4idea.repo.GitRepository;

import java.util.Map;

/**
 * This class is used to help listeners of Server events easily get the context of events that were triggered by
 * the ProjectRepoEventManager in response to IntelliJ changes like project opening/closing or repository changes.
 */
public class EventContextHelper {
    public static final String SENDER_PROJECT_OPENED = "projectOpened";
    public static final String SENDER_PROJECT_CLOSING = "projectClosing";
    public static final String SENDER_REPO_CHANGED = "repoChanged";
    public static final String CONTEXT_SENDER = "sender";
    public static final String CONTEXT_PROJECT = "project";
    public static final String CONTEXT_REPOSITORY = "repository";

    public static void setSender(final Map<String,Object> eventContext, final String sender) {
        ArgumentHelper.checkNotNull(eventContext, "eventContext");
        eventContext.put(EventContextHelper.CONTEXT_SENDER, sender);
    }

    public static void setProject(final Map<String,Object> eventContext, final Project project) {
        ArgumentHelper.checkNotNull(eventContext, "eventContext");
        eventContext.put(EventContextHelper.CONTEXT_PROJECT, project);
    }

    public static void setRepository(final Map<String,Object> eventContext, final GitRepository repository) {
        ArgumentHelper.checkNotNull(eventContext, "eventContext");
        eventContext.put(EventContextHelper.CONTEXT_REPOSITORY, repository);
    }

    public static String getSender(final Map<String,Object> eventContext) {
        ArgumentHelper.checkNotNull(eventContext, "eventContext");
        return (String)eventContext.get(EventContextHelper.CONTEXT_SENDER);
    }

    public static Project getProject(final Map<String,Object> eventContext) {
        ArgumentHelper.checkNotNull(eventContext, "eventContext");
        return (Project)eventContext.get(EventContextHelper.CONTEXT_PROJECT);
    }

    public static GitRepository getRepository(final Map<String,Object> eventContext) {
        ArgumentHelper.checkNotNull(eventContext, "eventContext");
        return (GitRepository)eventContext.get(EventContextHelper.CONTEXT_REPOSITORY);
    }

    public static boolean isProjectOpened(final Map<String,Object> eventContext) {
        final String sender = getSender(eventContext);
        return SENDER_PROJECT_OPENED.equals(sender) && getProject(eventContext) != null;
    }

    public static boolean isProjectClosing(final Map<String,Object> eventContext) {
        final String sender = getSender(eventContext);
        return SENDER_PROJECT_CLOSING.equals(sender) && getProject(eventContext) != null;
    }

    public static boolean isRepositoryChanged(final Map<String,Object> eventContext) {
        final String sender = getSender(eventContext);
        return SENDER_REPO_CHANGED.equals(sender) && getProject(eventContext) != null;
    }
}
