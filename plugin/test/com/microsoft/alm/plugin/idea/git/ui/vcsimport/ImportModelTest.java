// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.git.ui.vcsimport;

import com.microsoft.alm.plugin.authentication.TfsAuthenticationProvider;
import com.microsoft.alm.plugin.context.ServerContextManager;
import com.microsoft.alm.plugin.idea.IdeaAbstractTest;
import com.microsoft.alm.plugin.idea.common.ui.common.mocks.MockObserver;
import org.junit.Assert;
import org.junit.Test;

public class ImportModelTest extends IdeaAbstractTest {

    /**
     * Basic test of the constructors
     * It checks the values of properties are correctly initialized
     */
    @Test
    public void testConstructor() {
        ImportModel model = new ImportModel(null);
        Assert.assertNotNull("Tfs import page model is not initialized",
                model.getTfsImportPageModel());
        Assert.assertNotNull("Vso import page model is not initialized",
                model.getVsoImportPageModel());
    }

    /**
     * This test verifies all the setters on the import model notify the observers only if the value changes
     */
    @Test
    public void testObservable() {
        // Precondition: no authentication info is saved for TFS, otherwise the model will become connected immediately
        // and won't generate the events properly.
        Assert.assertNull(
                ServerContextManager.getInstance().getBestAuthenticationInfo(
                        TfsAuthenticationProvider.TFS_LAST_USED_URL,
                        false));

        ImportModel model = new ImportModel(null);
        MockObserver observer = new MockObserver(model);

        //change selection from vso to tfs, verify observer is notified since default is vso
        model.setVsoSelected(false);
        observer.assertAndClearLastUpdate(model, ImportModel.PROP_VSO_SELECTED);
        Assert.assertFalse("TFS is not selected on the model as expected",
                model.isVsoSelected());
        //verify selecting same tab doesn't notify observer
        model.setVsoSelected(false);
        observer.assertAndClearLastUpdate(null, null);
        Assert.assertFalse("TFS is not selected on the model as expected",
                model.isVsoSelected());

        // Change import enabled for Vso and make sure that we get notified
        model.setImportEnabledForVso(true);
        observer.assertAndClearLastUpdate(model, ImportModel.PROP_IMPORT_ENABLED);

        // Set import enabled to the exact same value and make sure we don't get notified
        model.setImportEnabledForVso(true);
        observer.assertAndClearLastUpdate(null, null);

        //change import enabled for Tfs and make sure that we get notified
        model.setImportEnabledForTfs(true);
        observer.assertAndClearLastUpdate(model, ImportModel.PROP_IMPORT_ENABLED);

        // Set import enabled to the exact same value and make sure we don't get notified
        model.setImportEnabledForTfs(true);
        observer.assertAndClearLastUpdate(null, null);
    }
}
