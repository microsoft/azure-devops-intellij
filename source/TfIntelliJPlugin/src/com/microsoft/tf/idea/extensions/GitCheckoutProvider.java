package com.microsoft.tf.idea.extensions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.microsoft.tf.idea.resources.TfPluginBundle;
import com.microsoft.tf.idea.ui.LoginDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by madhurig on 7/21/2015.
 */
public class GitCheckoutProvider implements CheckoutProvider {

    @Override
    public String getVcsName() {
        return TfPluginBundle.TfGitCheckoutProvider;
    }

    @Override
    public final void doCheckout(@NotNull final Project project, @Nullable final Listener listener) {
        //TODO: Temporary implementation to verify views
        final LoginDialog dlg = new LoginDialog(project);
        dlg.show();
    }
}
