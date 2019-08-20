// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.authentication.providers;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.microsoft.alm.oauth2.useragent.Provider;
import com.microsoft.alm.oauth2.useragent.UserAgent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This is a {@link UserAgent} provider that is aware of a JavaFX embedded into JetBrains Runtime.
 */
public class JbrAwareJavaFxProvider extends Provider {
    /**
     * A list that includes all of the {@link Provider#PROVIDERS} prepended by an instance of this class.
     */
    public static List<Provider> PROVIDERS_WITH_JBR_AWARE;

    static {
        List<Provider> providers = new ArrayList<>();
        providers.add(new JbrAwareJavaFxProvider());
        providers.addAll(PROVIDERS);
        PROVIDERS_WITH_JBR_AWARE = Collections.unmodifiableList(providers);
    }

    private JbrAwareJavaFxProvider() {
        super("JavaFx");
    }

    private static boolean isJava11() {
        return StringUtil.compareVersionNumbers(SystemInfo.JAVA_RUNTIME_VERSION, "11") >= 0
                && StringUtil.compareVersionNumbers(SystemInfo.JAVA_RUNTIME_VERSION, "12") < 0;
    }

    @Override
    public List<String> checkRequirements() {
        if (SystemInfo.isJetBrainsJvm && isJava11()) {
            return null;
        }

        return Collections.singletonList("JetBrains Runtime 11 or later is required");
    }

    @Override
    public void augmentProcessParameters(List<String> command, List<String> classPath) {
        // Do nothing, JavaFX is already included as a module of JetBrains Runtime.
    }
}
