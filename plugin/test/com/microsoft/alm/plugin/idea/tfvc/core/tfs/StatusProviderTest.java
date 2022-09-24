// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.tfvc.core.tfs;

import com.intellij.openapi.vcs.FilePath;
import com.microsoft.alm.plugin.external.models.PendingChange;
import com.microsoft.alm.plugin.idea.tfvc.exceptions.TfsException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class StatusProviderTest {

    @Mock
    private StatusVisitor myStatusVisitor;

    @Mock
    private FilePath myFilePath;

    @Mock
    private MockedStatic<VersionControlPath> myVersionControlPath;

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

        myVersionControlPath.when(() -> VersionControlPath.getFilePath(any(String.class), any(Boolean.class)))
                .thenReturn(myFilePath);
        StatusProvider.visitByStatus(myStatusVisitor, candidateChange);

        verify(myStatusVisitor).unversioned(myFilePath, false, ServerStatus.Unversioned.INSTANCE);
    }
}
