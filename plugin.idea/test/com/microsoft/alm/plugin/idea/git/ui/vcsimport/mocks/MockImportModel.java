// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.ui.vcsimport.mocks;

import com.microsoft.alm.plugin.idea.git.ui.vcsimport.ImportModel;

public class MockImportModel extends ImportModel {

    public MockImportModel() {
        super(null, new MockImportPageModel(null, false), new MockImportPageModel(null, true));
        ((MockImportPageModel) getVsoImportPageModel()).initialize(this);
        ((MockImportPageModel) getTfsImportPageModel()).initialize(this);
    }

    public MockImportPageModel getMockVsoImportPageModel() {
        return ((MockImportPageModel) getVsoImportPageModel());
    }

    public MockImportPageModel getMockTfsImportPageModel() {
        return ((MockImportPageModel) getTfsImportPageModel());
    }
}
