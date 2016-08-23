// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.L2;

import com.google.common.util.concurrent.SettableFuture;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.VcsKey;
import com.microsoft.alm.helpers.Path;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.idea.common.ui.checkout.CheckoutModel;
import com.microsoft.alm.plugin.idea.common.ui.checkout.VsoCheckoutPageModel;
import com.microsoft.alm.plugin.idea.common.ui.common.ServerContextTableModel;
import com.microsoft.alm.plugin.idea.git.ui.checkout.GitCheckoutModel;
import org.junit.Assert;
import org.junit.Test;
import sun.security.util.Debug;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;

public class CheckoutTest extends L2Test {

    public static final String GIT_FOLDER = ".git";
    public static final String README_FILE = "readme.md";

    @Test //TODO (timeout = 30000) - right now the test is interactive
    public void checkout_VSO() throws InterruptedException, NoSuchAlgorithmException, IOException, ExecutionException {
        final SettableFuture<Boolean> checkoutCompleted = SettableFuture.create();
        CheckoutModel checkoutModel = new CheckoutModel(ProjectManager.getInstance().getDefaultProject(), new CheckoutProvider.Listener() {
            @Override
            public void directoryCheckedOut(File directory, VcsKey vcs) {
            }

            @Override
            public void checkoutCompleted() {
                checkoutCompleted.set(true);
            }
        }, new GitCheckoutModel());

        // Create a temp folder for the clone
        File tempFolder = createTempDirectory();
        Debug.println("tempFolder=" + tempFolder, null);

        // Create the model and set fields appropriately
        VsoCheckoutPageModel model = new VsoCheckoutPageModel(checkoutModel);
        // To avoid the test loading all the accounts for the user, we set the account server we care about
        model.setServerName(getServerUrl());
        model.setUserName(getUser());
        model.setParentDirectory(tempFolder.getPath());
        model.setDirectoryName(getTeamProject());

        // Loadrepositories should now load just the repos for this account
        model.loadRepositories();
        while (model.isAuthenticating() || model.isLoading()) {
            Thread.sleep(100);
        }
        // Now we need to find ours and select it
        final ServerContextTableModel table = model.getTableModel();
        int index = -1;
        for (int i = 0; i < table.getRowCount(); i++) {
            final ServerContext context = table.getServerContext(i);
            if (context.getGitRepository().getRemoteUrl().equalsIgnoreCase(getRepoUrl()) ||
                    context.getGitRepository().getRemoteUrl().equalsIgnoreCase(getLegacyRepoUrl())) {
                index = i;
                break;
            }
        }

        // verify that we found it
        Assert.assertTrue(index >= 0);
        // select it
        model.getTableSelectionModel().setSelectionInterval(index, index);
        // clone it
        model.cloneSelectedRepo();
        // run the clone task that was queued up
        runProgressManagerTask();

        // verify that it got cloned
        File gitFolder = new File(tempFolder, Path.combine(getTeamProject(), GIT_FOLDER));
        Assert.assertTrue(gitFolder.exists());

        // verify that the readme was downloaded
        File readme = new File(tempFolder, Path.combine(getTeamProject(), README_FILE));
        Assert.assertTrue(readme.exists());

        // Clean up the folder now that the test has passed
        // TODO: this delete seems to be failing
        tempFolder.delete();
    }
}
