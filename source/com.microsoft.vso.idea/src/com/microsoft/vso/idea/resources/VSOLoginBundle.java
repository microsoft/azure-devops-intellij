package com.microsoft.vso.idea.resources;

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
public class VSOLoginBundle {
    @NonNls private static final String BUNDLE_NAME = "com/microsoft/vso/idea/ui/VSOLogin";
    private static Reference<ResourceBundle> thisBundle;

    private static ResourceBundle getBundle() {
        ResourceBundle bundle = com.intellij.reference.SoftReference.dereference(thisBundle);
        if(bundle == null) {
            bundle = ResourceBundle.getBundle(BUNDLE_NAME);
            thisBundle = new SoftReference<ResourceBundle>(bundle);
        }
        return bundle;
    }

    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE_NAME) String key, @NotNull Object... params) {
        return CommonBundle.message(getBundle(), key, params);
    }

    //Keys from properties file
    public static String Auth_Windows="Auth_Windows";
    public static String Auth_Alternate="Auth_Alternate";
    public static String Auth_PAT="Auth_PAT";

    public static String AddVSOAccount="AddVSOAccount";
    public static String SetupAlternateCredentials="SetupAlternateCredentials";
    public static String GenerateToken="GenerateToken";
    public static String GetStarted="GetStarted";

}
