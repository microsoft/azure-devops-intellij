// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.tfs;

import com.intellij.openapi.vcs.FilePath;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.idea.tfvc.exceptions.TfsException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(VersionControlPath.class)
public class StatusProviderTest {

    @Mock
    private StatusVisitor myStatusVisitor;

    @Mock
    private FilePath myFilePath;

    public StatusProviderTest() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(VersionControlPath.class);
    }

    @Test
    public void itemExistenceShouldBeProperlyCalculated() throws TfsException {
        PendingChange candidateChange = new PendingChange(
                "serverItem",
                "localItem.nonexistent-file",
                "version",
                "owner",
                "date",
                "lock",
                "changeType",
                "workspace",
                "computer",
                true,
                "sourceItem");

        when(VersionControlPath.getFilePath(any(String.class), any(Boolean.class))).thenReturn(myFilePath);
        StatusProvider.visitByStatus(myStatusVisitor, candidateChange);

        verify(myStatusVisitor).unversioned(myFilePath, false, ServerStatus.Unversioned.INSTANCE);
    }
}
