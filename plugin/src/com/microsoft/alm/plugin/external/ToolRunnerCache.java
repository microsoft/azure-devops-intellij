// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.plugin.external;

import com.microsoft.alm.common.utils.ArgumentHelper;
import com.microsoft.alm.plugin.external.models.ToolVersion;
import com.microsoft.alm.plugin.external.tools.TfTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ToolRunnerCache {
    private static final Logger logger = LoggerFactory.getLogger(ToolRunnerCache.class);

    private static final ConcurrentMap<String, ToolRunner> cache = new ConcurrentHashMap<>(3);

    /**
     * Creates and returns a running tool runner instance for TFVC client.
     *
     * @param toolLocation              location of the tool to start.
     * @param argumentBuilder           an object that defined the tool arguments.
     * @param listener                  tool execution listener.
     * @param shouldPrepareCachedRunner whether to prepare a new cached runner for the same location in advance: for
     *                                  cases when new calls of the same tool in the same working directory are
     *                                  likely. See {@link #getKey(String, ToolRunner.ArgumentBuilder)} for cache key
     *                                  calculation algorithm.
     * @return a started tool runner object.
     */
    public static ToolRunner getRunningToolRunner(
            String toolLocation,
            ToolRunner.ArgumentBuilder argumentBuilder,
            ToolRunner.Listener listener,
            boolean shouldPrepareCachedRunner,
            boolean skipVersionCheck) {
        logger.info("getRunningToolRunner: toolLocation={}", toolLocation);
        ToolRunner toolRunner;

        // Check the version
        final ToolVersion version = skipVersionCheck ? null : TfTool.getToolVersion();
        if (version == null || version.compare(TfTool.TF_MIN_VERSION) < 0) {
            // If it is older than the min version then just return a new ToolRunner and start it
            logger.info("getRunningToolRunner: slow version - " + (skipVersionCheck ? "(version check skipped)" : version));
            toolRunner = startToolRunner(toolLocation, argumentBuilder, listener);
        } else {
            // check the cache and try to get one that is already running
            logger.info("getRunningToolRunner: fast version - " + version);
            final String key = getKey(toolLocation, argumentBuilder);
            logger.info("getRunningToolRunner: key=" + key);

            // If there is already one cached, then remove it and use it
            toolRunner = cache.remove(key);
            if (toolRunner == null) {
                // Cache miss, so create a new one
                logger.info("getRunningToolRunner: cache miss.");
                toolRunner = startToolRunner(toolLocation, getStartAndWaitArguments(argumentBuilder), listener);
            } else {
                // Cache hit, but we need to add the listener
                toolRunner.addListener(listener);
            }

            // The toolRunner should already be started, we just need to send the args in
            toolRunner.sendArgsViaStandardInput(argumentBuilder);

            // Add another instance to the cache for later
            //TODO: clear all or part of the cache to make sure we aren't leaking memory by never cleaning up
            if (shouldPrepareCachedRunner)
                updateCachedInstance(key, toolLocation, argumentBuilder);
        }

        return toolRunner;
    }

    private static ToolRunner startToolRunner(String toolLocation, ToolRunner.ArgumentBuilder argumentBuilder, ToolRunner.Listener listener) {
        final ToolRunner toolRunner = new ToolRunner(toolLocation, argumentBuilder.getWorkingDirectory());
        toolRunner.addListener(listener);
        toolRunner.start(argumentBuilder);
        return toolRunner;
    }

    /**
     * This method returns an ArgumentBuilder with the working directory set and arguments
     * that tell the CLC to start up and then wait for arguments to be passed via
     * standard input.
     * @param originalArgumentBuilder
     * @return
     */
    private static ToolRunner.ArgumentBuilder getStartAndWaitArguments(final ToolRunner.ArgumentBuilder originalArgumentBuilder) {
        return new ToolRunner.ArgumentBuilder()
                .setWorkingDirectory(originalArgumentBuilder.getWorkingDirectory())
                .add("@");
    }

    private static void updateCachedInstance(final String key, final String toolLocation, final ToolRunner.ArgumentBuilder argumentBuilder) {
        // Start up a new instance and cache it for later use
        logger.info("updateCachedInstance: caching a new runner: key=" + key);
        final ToolRunner toolRunnerToCache = new ToolRunner(toolLocation, argumentBuilder.getWorkingDirectory());
        // The args for the cached instance are just working dir and "@" which tells the CLC to wait
        toolRunnerToCache.start(getStartAndWaitArguments(argumentBuilder));
        final ToolRunner cachedToolRunner = cache.put(key, toolRunnerToCache);

        // If there was already one waiting (some other thread started it up) then dispose of it
        if (cachedToolRunner != null) {
            logger.info("updateCachedInstance: disposing of cached tool runner.");
            cachedToolRunner.dispose();
        }
    }

    private static String getKey(final String toolLocation, final ToolRunner.ArgumentBuilder argumentBuilder) {
        ArgumentHelper.checkNotEmptyString(toolLocation, "toolLocation");
        if (argumentBuilder != null && argumentBuilder.getWorkingDirectory() != null) {
            return toolLocation.toLowerCase() + "|" + argumentBuilder.getWorkingDirectory().toLowerCase();
        } else {
            return toolLocation.toLowerCase() + "|";
        }
    }
}
