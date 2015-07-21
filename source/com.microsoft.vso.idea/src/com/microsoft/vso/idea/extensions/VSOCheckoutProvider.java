package com.microsoft.vso.idea.extensions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.microsoft.vso.idea.ui.VSOLoginDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by madhurig on 7/21/2015.
 */
public class VSOCheckoutProvider implements CheckoutProvider {

    @Override
    public String getVcsName() {
        return "VSO";
    }

    @Override
    public void doCheckout(@NotNull final Project project, @Nullable final Listener listener) {
        //TODO: Temporary implementation to verify views
        final VSOLoginDialog dlg = new VSOLoginDialog(project);
        dlg.show();
    }
}
