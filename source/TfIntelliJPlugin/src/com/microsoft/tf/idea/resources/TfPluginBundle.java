package com.microsoft.tf.idea.resources;

import com.intellij.CommonBundle;
import com.intellij.reference.SoftReference;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.lang.ref.Reference;
import java.util.ResourceBundle;

/**
 * Created by madhurig on 7/20/2015.
 */
public class TfPluginBundle {
    @NonNls
    private static final String BUNDLE_NAME = "com.microsoft.tf.idea.ui.tfplugin";

    private static Reference<ResourceBundle> thisBundle;

    private static final ResourceBundle getBundle() {
        ResourceBundle bundle = com.intellij.reference.SoftReference.dereference(thisBundle);
        if(bundle == null) {
            bundle = ResourceBundle.getBundle(BUNDLE_NAME);
            thisBundle = new SoftReference<ResourceBundle>(bundle);
        }
        return bundle;
    }

    private static final String message(@NotNull @PropertyKey(resourceBundle = BUNDLE_NAME) String key, @NotNull Object... params) {
        return CommonBundle.message(getBundle(), key, params);
    }

    //Login dialog
    public static final String Auth_Windows= TfPluginBundle.message("Auth_Windows");
    public static final String Auth_Alternate= TfPluginBundle.message("Auth_Alternate");
    public static final String Auth_PAT= TfPluginBundle.message("Auth_PAT");


    public static final String SavePassword= TfPluginBundle.message("SavePassword");
    public static final String SaveToken= TfPluginBundle.message("SaveToken");

    public static final String AddVSOAccount= TfPluginBundle.message("AddVSOAccount");
    public static final String SetupAlternateCredentials= TfPluginBundle.message("SetupAlternateCredentials");
    public static final String GenerateToken= TfPluginBundle.message("GenerateToken");
    public static final String GetStarted= TfPluginBundle.message("GetStarted");

    //Connection dialog
    public static final String ConnectToTeamProject=TfPluginBundle.message("ConnectToTeamProject");
    public static final String TeamProject=TfPluginBundle.message("TeamProject");
    public static final String ProjectCollection=TfPluginBundle.message("ProjectCollection");
    public static final String SelectTeamProject=TfPluginBundle.message("SelectTeamProject");

    //actions
    public static final String OpenInBrowser=TfPluginBundle.message("OpenInBrowser");
    public static final String OpenInBrowserMsg=TfPluginBundle.message("OpenInBrowserMsg");

    //providers
    public static final String TfGitCheckoutProvider=TfPluginBundle.message("TfGitCheckoutProvider");

}
