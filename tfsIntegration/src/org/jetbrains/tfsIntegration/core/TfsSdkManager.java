package org.jetbrains.tfsIntegration.core;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.PluginPathManager;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.ObjectUtils;
import com.microsoft.tfs.core.httpclient.DefaultNTCredentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.jni.loader.NativeLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.tfsIntegration.core.configuration.Credentials;
import org.jetbrains.tfsIntegration.core.configuration.TFSConfigurationManager;
import org.jetbrains.tfsIntegration.core.tfs.ServerInfo;

import java.io.File;

public class TfsSdkManager {

  public static void activate() {
    setupNativeLibrariesPath();
  }

  private static void setupNativeLibrariesPath() {
    File nativeLibrariesPath = new File(getPluginDirectory(), FileUtil.toSystemDependentName("lib/native"));

    System.setProperty(NativeLoader.NATIVE_LIBRARY_BASE_DIRECTORY_PROPERTY, nativeLibrariesPath.getPath());
  }

  @NotNull
  private static File getPluginDirectory() {
    PluginId pluginId = PluginId.getId("TFS");
    IdeaPluginDescriptor pluginDescriptor = ObjectUtils.assertNotNull(PluginManager.getPlugin(pluginId));

    return pluginDescriptor.isBundled()
           ? PluginPathManager.getPluginHome("tfsIntegration")
           : ObjectUtils.assertNotNull(pluginDescriptor.getPath());
  }

  @NotNull
  public static com.microsoft.tfs.core.httpclient.Credentials getCredentials(@NotNull ServerInfo server) {
    Credentials credentials = ObjectUtils.assertNotNull(TFSConfigurationManager.getInstance().getCredentials(server.getUri()));
    com.microsoft.tfs.core.httpclient.Credentials result;

    switch (credentials.getType()) {
      case NtlmNative:
        result = new DefaultNTCredentials();
        break;
      case NtlmExplicit:
      case Alternate:
        result = new UsernamePasswordCredentials(credentials.getQualifiedUsername(), credentials.getPassword());
        break;
      default:
        throw new IllegalArgumentException("Unknown credentials type " + credentials.getType());
    }

    return result;
  }
}
