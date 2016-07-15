// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.utils;

import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Helps with converting dates to friendly formats
 */
public class DateHelper {
    private final static Logger logger = LoggerFactory.getLogger(DateHelper.class);

    public static String getFriendlyDateTimeString(final Date date) {
        final Date now = new Date();
        return (getFriendlyDateTimeString(date, now));
    }

    protected static String getFriendlyDateTimeString(final Date date, final Date now) {
        if (date == null) {
            return "";
        }

        try {
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
