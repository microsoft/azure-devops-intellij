// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.idea.starters;

import com.intellij.openapi.application.ApplicationNamesInfo;
import com.microsoft.alm.plugin.idea.resources.TfPluginBundle;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Processes VSTS commands via command-line or uri. The given arguments are parsed to determine which sub-command
 * should be called along with the remaining parameters
 */
public class VstsStarter extends ApplicationStarterBase {
    private final Logger logger = LoggerFactory.getLogger(VstsStarter.class);
    private static final String IDE_TYPE_ATTRIBUTE = "IdeType";
    private static final String IDE_EXE_ATTRIBUTE = "IdeExe";

    @Override
    public String getUsageMessage() {
        // the IDE that is being used (i.e. idea, charm, etc.)
        final String applicationType = ApplicationNamesInfo.getInstance().getScriptName();
        return TfPluginBundle.message(TfPluginBundle.STARTER_COMMAND_LINE_USAGE_MSG, VSTS_COMMAND, applicationType);
    }

    @Override
    protected void processCommand(List<String> args) throws RuntimeException {
        final String command = args.remove(0);

        // can be expanded upon if more commands are added
        StarterBase starter;
        if (StringUtils.equalsIgnoreCase(SimpleCheckoutStarter.SUB_COMMAND_NAME, command)) {
            starter = SimpleCheckoutStarter.createWithCommandLineArgs(args);
        } else {
            throw new RuntimeException(TfPluginBundle.message(TfPluginBundle.STARTER_ERRORS_SUB_COMMAND_NOT_RECOGNIZED, command));
        }
        starter.processCommand();
    }

    @Override
    protected void processUri(final String uri) throws RuntimeException, UnsupportedEncodingException {
        final String[] args = uri.split("/\\?");

        if (args.length != 2) {
            throw new RuntimeException(TfPluginBundle.message(TfPluginBundle.STARTER_ERRORS_MALFORMED_URI));
        }

        final String command = args[0];
        final Map<String, String> attributes = parseUri(args[1]);

        // can be expanded upon if more commands are added
        StarterBase starter;
        if (StringUtils.equalsIgnoreCase(SimpleCheckoutStarter.SUB_COMMAND_NAME, command)) {
            starter = SimpleCheckoutStarter.createWithUriAttributes(attributes);
        } else {
            throw new RuntimeException(TfPluginBundle.message(TfPluginBundle.STARTER_ERRORS_SUB_COMMAND_NOT_RECOGNIZED, command));
        }
        starter.processCommand();
    }

    /**
     * Parse URI to find the given key-value attributes it contains
     *
     * @param uri
     * @return Map of the key-value attributes contained in the URI
     */
    protected Map<String, String> parseUri(final String uri) {
        final List<String> args = Arrays.asList(uri.split("&"));
        Map<String, String> attributes = new HashMap<String, String>();

        for (String arg : args) {
            final int index = arg.indexOf('=');
            if (index != -1) {
                final String key = arg.substring(0, index);
                final String value = arg.length() > index + 1 ? arg.substring(index + 1) : StringUtils.EMPTY;
                attributes.put(key, value);
            }
        }

        //remove the attributes that pertain to which IDE to launch since they have already been used
        attributes.remove(IDE_TYPE_ATTRIBUTE);
        attributes.remove(IDE_EXE_ATTRIBUTE);

        logger.debug("The URI attributes found are: " + attributes.entrySet().toString());
        return attributes;
    }
}
