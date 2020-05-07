// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.L2.tfvc;

import com.google.common.util.concurrent.SettableFuture;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.VcsKey;
import com.microsoft.alm.L2.L2Test;
import com.microsoft.alm.helpers.Path;
import com.microsoft.alm.plugin.context.ServerContext;
import com.microsoft.alm.plugin.external.commands.DeleteWorkspaceCommand;
import com.microsoft.alm.plugin.idea.common.ui.checkout.CheckoutModel;
import com.microsoft.alm.plugin.idea.common.ui.checkout.VsoCheckoutPageModel;
import com.microsoft.alm.plugin.idea.common.ui.common.ServerContextTableModel;
import com.microsoft.alm.plugin.idea.tfvc.ui.checkout.TfvcCheckoutModel;
import org.junit.Assert;
import org.junit.Test;
import sun.security.util.Debug;

import java.io.File;
import java.io.IOException;

public class TfvcCheckoutTest extends L2Test {

    public static final String TFVC_FOLDER = ".tf";
    public static final String TFVC_FOLDER_WIN = "$tf";
    public static final String README_FILE = "readme.txt";

    private String getWorkspaceName() {
        return generateUniqueName(getTeamProject());
    }

    private void deleteWorkspaceIfExists(ServerContext context, String workspaceName) {
        DeleteWorkspaceCommand deleteWorkspaceCommand = new DeleteWorkspaceCommand(context, workspaceName);
        deleteWorkspaceCommand.runSynchronously();
    }

    @Test(timeout = 60000)
    public void testCheckout_VSO() throws InterruptedException, IOException {
        final SettableFuture<Boolean> checkoutCompleted = SettableFuture.create();
        CheckoutModel checkoutModel = new CheckoutModel(ProjectManager.getInstance().getDefaultProject(), new CheckoutProvider.Listener() {
            @Override
            public void directoryCheckedOut(File directory, VcsKey vcs) {
            }

            @Override
            public void checkoutCompleted() {
                checkoutCompleted.set(true);
            }
        }, new TfvcCheckoutModel(), null, null, false);

        // Create a temp folder for the clone
        File tempFolder = createTempDirectory();
        Debug.println("tempFolder", tempFolder.toString());

        String workspaceName = getWorkspaceName();
        Debug.println("workspaceName", workspaceName);

        // Create the model and set fields appropriately
        VsoCheckoutPageModel model = (VsoCheckoutPageModel) checkoutModel.getVsoModel();
        // To avoid the test loading all the accounts for the user, we set the account server we care about
        model.setServerName(getServerUrl());
        model.setUserName(getUser());
        model.setParentDirectory(tempFolder.getPath());

        // loadRepositories should now load just the repos for this account
        model.loadRepositories();
        while (model.isAuthenticating() || model.isLoading()) {
            Thread.sleep(100);
        }
        // Now we need to find ours and select it
        final ServerContextTableModel table = model.getTableModel();
        int index = -1;
        for (int i = 0; i < table.getRowCount(); i++) {
            final ServerContext context = table.getServerContext(i);
            if (context.getTeamProjectReference().getName().equalsIgnoreCase(getTeamProject())) {
                index = i;
                break;
            }
        }

        // verify that we found it
        Assert.assertTrue(index >= 0);
        // select it
        model.getTableSelectionModel().setSelectionInterval(index, index);
        model.setDirectoryName(workspaceName);

        deleteWorkspaceIfExists(model.getSelectedContext(), workspaceName);

        try {
            // clone it
            // Everything happens synchronously, so no need to worry
            model.cloneSelectedRepo();

            // verify that it got cloned
            File tfvcFolder = new File(tempFolder, Path.combine(workspaceName, TFVC_FOLDER));
            File tfvcFolderWin = new File(tempFolder, Path.combine(workspaceName, TFVC_FOLDER_WIN));
            Assert.assertTrue(tfvcFolder.exists() || tfvcFolderWin.exists());

            // verify that the readme was downloaded
            File readme = new File(tempFolder, Path.combine(workspaceName, README_FILE));
            Assert.assertTrue(readme.exists());

            // Clean up the folder now that the test has passed
            tempFolder.delete();
        } finally {
            // Clean up the workspace
            deleteWorkspaceIfExists(model.getSelectedContext(), workspaceName);
        }
    }
}
