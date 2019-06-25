// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.common.utils;

import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.resources.TfPluginBundle;
import org.junit.Assert;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateHelperTest extends IdeaAbstractTest {
    @Test
    public void testGetFriendlyDateTimeString() {
        Assert.assertEquals("", DateHelper.getFriendlyDateTimeString(null));
        Date now = new Date();
        Assert.assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_DATE_LESS_THAN_A_MINUTE_AGO),
                DateHelper.getFriendlyDateTimeString(now, now));
        Date dt0 = getTime(now, +1000L);
        Assert.assertEquals(dt0.toString(),
                DateHelper.getFriendlyDateTimeString(dt0, now));
        Date dt1 = getTime(now, -1000L);
        Assert.assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_DATE_LESS_THAN_A_MINUTE_AGO),
                DateHelper.getFriendlyDateTimeString(dt1, now));
        Date dt2 = getTime(now, -1000L * 60);
        Assert.assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_DATE_ONE_MINUTE_AGO),
                DateHelper.getFriendlyDateTimeString(dt2, now));
        Date dt3 = getTime(now, -1000L * 60 * 2);
        Assert.assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_DATE_MINUTES_AGO, 2),
                DateHelper.getFriendlyDateTimeString(dt3, now));
        Date dt4 = getTime(now, -1000L * 60 * 60);
        Assert.assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_DATE_ONE_HOUR_AGO),
                DateHelper.getFriendlyDateTimeString(dt4, now));
        Date dt5 = getTime(now, -1000L * 60 * 60 * 2);
        Assert.assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_DATE_HOURS_AGO, 2),
                DateHelper.getFriendlyDateTimeString(dt5, now));
        Date dt6 = getTime(now, -1000L * 60 * 60 * 25);
        Assert.assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_DATE_YESTERDAY),
                DateHelper.getFriendlyDateTimeString(dt6, now));
        Date dt7 = getTime(now, -1000L * 60 * 60 * 24 * 3);
        Assert.assertEquals(TfPluginBundle.message(TfPluginBundle.KEY_VCS_PR_DATE_DAYS_AGO, 3),
                DateHelper.getFriendlyDateTimeString(dt7, now));
        Date dt8 = getTime(now, -1000L * 60 * 60 * 24 * 7);
        final SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");
        Assert.assertEquals(format.format(dt8),
                DateHelper.getFriendlyDateTimeString(dt8, now));

        // make sure the catch works
        Assert.assertEquals(dt8.toString(),
                DateHelper.getFriendlyDateTimeString(dt8, null));
    }

    private Date getTime(Date now, long millisecondsToAdd) {
        Date d2 = new Date(now.getTime() + millisecondsToAdd);
        return d2;
    }
}
