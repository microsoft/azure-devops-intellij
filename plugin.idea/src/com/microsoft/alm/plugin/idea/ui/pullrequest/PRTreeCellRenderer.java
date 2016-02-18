// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.pullrequest;


import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.microsoft.alm.plugin.idea.resources.Icons;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.GitPullRequest;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.IdentityRefWithVote;
import com.microsoft.teamfoundation.sourcecontrol.webapi.model.PullRequestAsyncStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.Component;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Custom rendering of Pull Requests
 */
public class PRTreeCellRenderer extends DefaultTreeCellRenderer {
    private final static Logger logger = LoggerFactory.getLogger(PRTreeCellRenderer.class);

    private final static String GIT_REFS_HEADS = "refs/heads/";
    private final static short REVIEWER_VOTE_NO_RESPONSE = 0;
    private final static short REVIEWER_VOTE_APPROVED_WITH_SUGGESTIONS = 5;
    private final static short REVIEWER_VOTE_APPROVED = 10;
    private final static short REVIEWER_VOTE_WAITING_FOR_AUTHOR = -5;
    private final static short REVIEWER_VOTE_REJECTED = -10;

    private PRTreeNodeForm prViewForm;

    public PRTreeCellRenderer() {

    }

    @Override
    public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean selected,
                                                  final boolean expanded, final boolean leaf,
                                                  final int row, final boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        setIcon(null);
        if (value instanceof PRTreeNode && ((PRTreeNode) value).getGitPullRequest() != null) {
            final GitPullRequest pullRequest = ((PRTreeNode) value).getGitPullRequest();
            prViewForm = new PRTreeNodeForm(selected, hasFocus);
            prViewForm.setSummary(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_SUMMARY,
                    pullRequest.getCreatedBy().getDisplayName(),
                    pullRequest.getPullRequestId(),
                    getFriendlyDateTimeString(pullRequest.getCreationDate()),
                    pullRequest.getSourceRefName().replace(GIT_REFS_HEADS, ""),
                    pullRequest.getTargetRefName().replace(GIT_REFS_HEADS, "")));
            setStatus(prViewForm, pullRequest);

            final Component component = prViewForm.getPanel();
            return component;
        } else if (value instanceof PRTreeNode && ((PRTreeNode) value).getGitPullRequest() == null) {
            final PRTreeNode rootNode = ((PRTreeNode) value);
            final SimpleColoredComponent component = new SimpleColoredComponent();
            final SimpleTextAttributes regularTextAttributes = selected && hasFocus ?
                    new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, JBColor.WHITE)
                    : SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;
            component.append(rootNode.toString(), regularTextAttributes);
            component.append(" ");
            final SimpleTextAttributes italicTextAttributes = selected && hasFocus ?
                    new SimpleTextAttributes(SimpleTextAttributes.STYLE_ITALIC, JBColor.WHITE) :
                    SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES;
            component.append(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_VIEW_DETAILS_COUNT, rootNode.getChildCount()),
                    italicTextAttributes);
            return component;
        }
        return this;
    }

    //TODO: create a model for the rendering view logic
    private void setStatus(final PRTreeNodeForm prView, final GitPullRequest pullRequest) {
        //first check merge status, if there are conflicts, show that to the user
        final PullRequestAsyncStatus mergeStatus = pullRequest.getMergeStatus();
        if (mergeStatus == PullRequestAsyncStatus.CONFLICTS ||
                mergeStatus == PullRequestAsyncStatus.FAILURE ||
                mergeStatus == PullRequestAsyncStatus.REJECTED_BY_POLICY) {
            //merge failed
            prView.setStatus(pullRequest.getTitle(),
                    TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_MERGE_FAILED),
                    Icons.PR_STATUS_FAILED);
            return;
        }

        //no merge errors, so look at reviewer response
        short lowestVote = 0;
        short highestVote = 0;

        if (pullRequest.getReviewers() != null && pullRequest.getReviewers().length > 0) {
            for (final IdentityRefWithVote reviewer : pullRequest.getReviewers()) {
                final short vote = reviewer.getVote();
                if (vote < lowestVote) {
                    lowestVote = vote;
                }
                if (vote > highestVote) {
                    highestVote = vote;
                }
            }
        }

        short finalVote = REVIEWER_VOTE_NO_RESPONSE;
        if (lowestVote < REVIEWER_VOTE_NO_RESPONSE) {
            //use negative comments if any
            finalVote = lowestVote;
        } else if (highestVote > REVIEWER_VOTE_NO_RESPONSE) {
            //use positive comments
            finalVote = highestVote;
        }

        //set message and icon based on aggregate reviewers response
        String voteString = TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_REVIEWER_NO_RESPONSE);
        Icon voteIcon = Icons.PR_STATUS_NO_RESPONSE;
        if (finalVote == REVIEWER_VOTE_APPROVED_WITH_SUGGESTIONS) {
            voteString = TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_REVIEWER_APPROVED_SUGGESTIONS);
            voteIcon = Icons.PR_STATUS_SUCCEEDED;
        }
        if (finalVote == REVIEWER_VOTE_APPROVED) {
            voteString = TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_REVIEWER_APPROVED);
            voteIcon = Icons.PR_STATUS_SUCCEEDED;
        }
        if (finalVote == REVIEWER_VOTE_WAITING_FOR_AUTHOR) {
            voteString = TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_REVIEWER_WAITING);
            voteIcon = Icons.PR_STATUS_WAITING;
        }
        if (finalVote == REVIEWER_VOTE_REJECTED) {
            voteString = TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_REVIEWER_REJECTED);
            voteIcon = Icons.PR_STATUS_FAILED;
        }

        prView.setStatus(pullRequest.getTitle(), voteString, voteIcon);
    }

    private String getFriendlyDateTimeString(final Date date) {
        if (date == null) {
            return "";
        }

        try {
            final Date now = new Date();
            final long diff = now.getTime() - date.getTime(); //in milliseconds
            if (diff < 0) {
                return date.toString(); //input date is not in the past
            }

            final long diffMinutes = diff / (1000 * 60);
            if (diffMinutes < 1) {
                return TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_DATE_LESS_THAN_A_MINUTE_AGO);
            } else if (diffMinutes == 1) {
                return TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_DATE_ONE_MINUTE_AGO);
            } else if (diffMinutes < 60) {
                return TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_DATE_MINUTES_AGO, diffMinutes);
            }

            final long diffHours = diff / (1000 * 60 * 60);
            if (diffHours <= 1) {
                return TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_DATE_ONE_HOUR_AGO);
            } else if (diffHours <= 24) {
                return TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_DATE_HOURS_AGO, diffHours);
            }

            final long diffDays = diff / (1000 * 60 * 60 * 24);
            if (diffDays <= 2) {
                return TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_DATE_YESTERDAY);
            } else if (diffDays < 7) {
                return TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_DATE_DAYS_AGO, diffDays);
            } else {
                final SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
                return format.format(date);
            }

        } catch (Throwable t) {
            logger.warn("getFriendlyDateTimeString unexpected error with input date {}", date.toString(), t);
            return date.toString();
        }
    }
}
