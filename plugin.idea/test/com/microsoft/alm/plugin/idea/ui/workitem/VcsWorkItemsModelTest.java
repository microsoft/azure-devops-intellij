// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.ui.workitem;

import com.intellij.openapi.project.Project;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.utils.TfGitHelper;
import com.microsoft.alm.plugin.operations.WorkItemLookupOperation;
import com.microsoft.alm.workitemtracking.webapi.models.WorkItem;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(PowerMockRunner.class)
@PrepareForTest(TfGitHelper.class)
public class VcsWorkItemsModelTest extends IdeaAbstractTest {
    private VcsWorkItemsModel model;
    private WorkItemLookupOperation operation;

    @Mock
    private Project mockProject;

    @Before
    public void setUp() {
        model = new VcsWorkItemsModel(mockProject);
        operation = new WorkItemLookupOperation(StringUtils.EMPTY);
    }

    @Test
    public void testAppendData() {
        model.appendData(createResults(5, 0));
        Assert.assertEquals(5, model.getModelForView().getRowCount());
        for (int i = 0; i < 5; i++) {
            Assert.assertEquals(i, model.getModelForView().getWorkItem(i).getId());
        }

        model.appendData(createResults(5, 5));
        Assert.assertEquals(10, model.getModelForView().getRowCount());
        for (int i = 0; i < 10; i++) {
            Assert.assertEquals(i, model.getModelForView().getWorkItem(i).getId());
        }
    }

    @Test
    public void testClearData() {
        model.appendData(createResults(5, 0));
        Assert.assertEquals(5, model.getModelForView().getRowCount());
        model.clearData();
        Assert.assertEquals(0, model.getModelForView().getRowCount());
    }

    private WorkItemLookupOperation.WitResults createResults(final int numberOfItems, final int startingIndex) {
        final List<WorkItem> list = new ArrayList<WorkItem>();
        for (int i = startingIndex; i < numberOfItems + startingIndex; i++) {
            final WorkItem item = new WorkItem();
            item.setId(i);
            list.add(item);
        }
        return operation.new WitResults(null, list);
    }
}
